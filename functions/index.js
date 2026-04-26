const crypto = require("node:crypto");
const admin = require("firebase-admin");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");

// Set once with: firebase functions:secrets:set GROQ_API_KEY
const GROQ_API_KEY = defineSecret("GROQ_API_KEY");
// Set once with: firebase functions:secrets:set GEMINI_API_KEY
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

admin.initializeApp();
const db = admin.firestore();

const REGION = "asia-south1";
const MAX_CALLS_PER_HOUR = 60;

function traceId() {
  return crypto.randomUUID();
}

function success(data, message = "Success", code = "OK", t = traceId()) {
  return {status: "success", code, message, data, traceId: t};
}

function failure(code, message, t = traceId(), data = null) {
  return {status: "error", code, message, data, traceId: t};
}

function requireAuth(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication required");
  }
  return request.auth.uid;
}

async function enforceRateLimit(uid, endpoint, t) {
  const key = `${uid}_${endpoint}`;
  const ref = db.collection("rate_limits").doc(key);
  const now = Date.now();
  const windowStart = now - 60 * 60 * 1000;

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) {
      tx.set(ref, {uid, endpoint, count: 1, firstAt: now, updatedAt: now});
      return;
    }

    const row = snap.data();
    const firstAt = row.firstAt || now;
    const count = row.count || 0;

    if (firstAt < windowStart) {
      tx.update(ref, {count: 1, firstAt: now, updatedAt: now});
      return;
    }

    if (count >= MAX_CALLS_PER_HOUR) {
      throw new HttpsError("resource-exhausted", `Rate limit exceeded. traceId=${t}`);
    }

    tx.update(ref, {count: count + 1, updatedAt: now});
  });
}

function validateString(value, field, min = 0, max = 4000) {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${field} must be a string`);
  }
  const cleaned = value.trim();
  if (cleaned.length < min || cleaned.length > max) {
    throw new HttpsError("invalid-argument", `${field} length out of range`);
  }
  return cleaned;
}

function validateNumber(value, field, min = -1e6, max = 1e6) {
  if (typeof value !== "number" || Number.isNaN(value) || value < min || value > max) {
    throw new HttpsError("invalid-argument", `${field} must be a valid number`);
  }
  return value;
}

function maskPhone(phone) {
  if (!phone || phone.length < 6) return phone || "";
  return `${phone.slice(0, 3)}****${phone.slice(-3)}`;
}

function maskEmail(email) {
  if (!email || !email.includes("@")) return email || "";
  const [name, domain] = email.split("@");
  if (name.length <= 2) return `**@${domain}`;
  return `${name.slice(0, 2)}***@${domain}`;
}

async function callGroqChat(messages, options = {}) {
  const apiKey = GROQ_API_KEY.value();
  if (!apiKey) {
    throw new Error("GROQ_API_KEY secret is not set. Run: firebase functions:secrets:set GROQ_API_KEY");
  }

  const payload = {
    model: options.model || "llama-3.3-70b-versatile",
    temperature: options.temperature ?? 0.2,
    max_tokens: options.maxTokens ?? 900,
    messages,
  };

  const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Groq call failed: ${response.status} ${text}`);
  }

  const json = await response.json();
  const content = json?.choices?.[0]?.message?.content;
  if (!content || typeof content !== "string") {
    throw new Error("Invalid Groq response");
  }
  return content;
}

async function callGeminiVision(imageDataUrl, systemPrompt, userPromptText) {
  const apiKey = GEMINI_API_KEY.value();
  if (!apiKey) throw new Error("GEMINI_API_KEY secret not set");

  // Split "data:image/jpeg;base64,XXXX" into mimeType + raw base64
  const commaIdx = imageDataUrl.indexOf(",");
  if (commaIdx < 0) throw new Error("Invalid image data URL");
  const meta = imageDataUrl.substring(5, commaIdx); // e.g. "image/jpeg;base64"
  const mimeType = meta.split(";")[0] || "image/jpeg";
  const base64Data = imageDataUrl.substring(commaIdx + 1);

  const body = {
    systemInstruction: {parts: [{text: systemPrompt}]},
    contents: [{
      parts: [
        {text: userPromptText},
        {inlineData: {mimeType, data: base64Data}},
      ],
    }],
    generationConfig: {temperature: 0.1, maxOutputTokens: 900},
  };

  const model = "gemini-2.0-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
  const response = await fetch(url, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Gemini call failed: ${response.status} ${text}`);
  }

  const json = await response.json();
  const content = json?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!content || typeof content !== "string") {
    throw new Error("Invalid Gemini response structure");
  }
  return content;
}

function parseFirstJson(rawText) {
  const raw = (rawText || "").trim();
  const codeFence = raw.match(/```(?:json)?\\s*([\\s\\S]*?)```/i);
  const candidate = codeFence ? codeFence[1].trim() : raw;
  const firstBrace = candidate.indexOf("{");
  const lastBrace = candidate.lastIndexOf("}");
  if (firstBrace < 0 || lastBrace <= firstBrace) {
    throw new Error("No JSON object found in model output");
  }
  return JSON.parse(candidate.substring(firstBrace, lastBrace + 1));
}

function normalizeConfidence(value, fallback = 55) {
  let numeric = Number.NaN;
  if (typeof value === "number") {
    numeric = value;
  } else if (typeof value === "string") {
    const match = value.match(/-?\d+(\.\d+)?/);
    if (match) numeric = Number(match[0]);
  }

  if (!Number.isFinite(numeric)) numeric = fallback;
  if (numeric > 0 && numeric <= 1) numeric = numeric * 100;

  let normalized = Math.round(numeric);
  if (!Number.isFinite(normalized)) normalized = fallback;
  if (normalized < 0) normalized = 0;
  if (normalized > 100) normalized = 100;
  if (normalized === 0) normalized = fallback;
  return normalized;
}

function fallbackSoilAdvisory(data) {
  const actions = [];
  if (data.nitrogen < 200) actions.push("Nitrogen is low. Apply split urea doses with irrigation.");
  if (data.phosphorus < 12) actions.push("Phosphorus is low. Add SSP/DAP at sowing.");
  if (data.potassium < 120) actions.push("Potassium is low. Add MOP in two splits.");
  if (data.ph < 6) actions.push("Soil is acidic. Apply lime based on local recommendation.");
  if (data.ph > 7.8) actions.push("Soil is alkaline. Apply gypsum and increase organic matter.");
  if (actions.length === 0) actions.push("Soil indicators are broadly stable. Maintain balanced fertilization.");

  return {
    advisoryMarkdown: actions.map((a) => `- ${a}`).join("\n"),
    contextSummary: `Soil: ${data.detectedSoil || "Unknown"} | Weather: ${data.weather || "N/A"}`,
    confidence: 35,
    uncertaintyMessage: "Fallback advisory generated due temporary AI unavailability.",
  };
}

function fallbackCropSchedule(cropName) {
  return {
    crop: cropName,
    bestPlantingMonth: "June",
    bestHarvestMonth: "October",
    durationDays: 120,
    fertilizerSchedule: "Use balanced NPK based on soil test in split doses.",
    irrigationTips: "Irrigate early morning. Adjust by rainfall and soil moisture.",
    pestWatch: "Scout field weekly for early pest symptoms.",
    confidence: 30,
    uncertaintyMessage: "Fallback schedule generated due temporary AI unavailability.",
    schedule: [
      {week: 1, activity: "Land preparation", tips: "Prepare seedbed and compost incorporation"},
      {week: 2, activity: "Sowing/Transplanting", tips: "Maintain spacing and seed quality checks"},
      {week: 4, activity: "Nutrient top-up", tips: "Apply first split N dose"},
    ],
  };
}

function fallbackPlantDiagnosis() {
  return {
    cropIdentified: "Unknown",
    healthStatus: "Unknown",
    confidence: 20,
    issuesDetected: ["Unable to infer from current image input."],
    recommendations: [
      "Capture image in daylight with one affected leaf in focus.",
      "Avoid blurred shots and include close-up lesions.",
    ],
    uncertaintyMessage: "Fallback diagnosis generated due temporary AI unavailability.",
    rawJson: "{}",
  };
}

exports.checkDuplicateAccount = onCall({region: REGION}, async (request) => {
  const t = traceId();
  try {
    const uid = requireAuth(request);
    await enforceRateLimit(uid, "checkDuplicateAccount", t);

    const phone = request.data?.phone ? validateString(request.data.phone, "phone", 0, 24) : "";
    const email = request.data?.email ? validateString(request.data.email, "email", 0, 160).toLowerCase() : "";
    const currentUid = request.data?.currentUid ? validateString(request.data.currentUid, "currentUid", 1, 128) : uid;

    let foundDoc = null;
    if (phone) {
      const byPhone = await db.collection("farmers").where("phone", "==", phone).limit(5).get();
      foundDoc = byPhone.docs.find((d) => d.id !== currentUid) || null;
    }
    if (!foundDoc && email) {
      const byEmail = await db.collection("farmers").where("email", "==", email).limit(5).get();
      foundDoc = byEmail.docs.find((d) => d.id !== currentUid) || null;
    }

    if (!foundDoc) {
      return success({duplicate: false});
    }

    const row = foundDoc.data() || {};
    return success({
      duplicate: true,
      existingUid: foundDoc.id,
      maskedName: row.firstName || "Farmer",
      maskedPhone: maskPhone(row.phone || ""),
      maskedEmail: maskEmail(row.email || ""),
      reason: "phone_or_email_conflict",
    }, "Duplicate account found", "DUPLICATE_FOUND", t);
  } catch (err) {
    console.error("[checkDuplicateAccount]", err);
    return failure("CHECK_FAILED", err.message || "Duplicate check failed", t);
  }
});

exports.linkAccountIdentity = onCall({region: REGION}, async (request) => {
  const t = traceId();
  try {
    const uid = requireAuth(request);
    await enforceRateLimit(uid, "linkAccountIdentity", t);

    const targetUid = validateString(request.data?.targetUid || "", "targetUid", 1, 128);
    const phone = request.data?.phone ? validateString(request.data.phone, "phone", 0, 24) : "";
    const email = request.data?.email ? validateString(request.data.email, "email", 0, 160).toLowerCase() : "";

    if (!phone && !email) {
      throw new HttpsError("invalid-argument", "At least one identity field is required");
    }

    // Verify caller owns the identity fields they are trying to link.
    const authUser = await admin.auth().getUser(uid);
    const authPhone = authUser.phoneNumber || "";
    const authEmail = (authUser.email || "").toLowerCase();
    if (phone && authPhone !== phone) {
      throw new HttpsError("permission-denied", "Phone does not belong to authenticated user");
    }
    if (email && authEmail !== email) {
      throw new HttpsError("permission-denied", "Email does not belong to authenticated user");
    }

    // Verify the target account is actually the conflicting account for this identity.
    if (phone) {
      const byPhone = await db.collection("farmers").where("phone", "==", phone).limit(5).get();
      const ownsPhone = byPhone.docs.some((d) => d.id === targetUid);
      if (!ownsPhone) {
        throw new HttpsError("failed-precondition", "Target account does not own provided phone");
      }
    }
    if (email) {
      const byEmail = await db.collection("farmers").where("email", "==", email).limit(5).get();
      const ownsEmail = byEmail.docs.some((d) => d.id === targetUid);
      if (!ownsEmail) {
        throw new HttpsError("failed-precondition", "Target account does not own provided email");
      }
    }

    const targetRef = db.collection("farmers").doc(targetUid);
    const targetSnap = await targetRef.get();
    if (!targetSnap.exists) {
      throw new HttpsError("not-found", "Target account not found");
    }

    const targetData = targetSnap.data() || {};
    const updates = {updatedAt: Date.now()};

    // Never overwrite existing identity values with different values.
    if (phone) {
      const current = targetData.phone || "";
      if (!current) {
        updates.phone = phone;
      } else if (current !== phone) {
        throw new HttpsError("failed-precondition", "Target phone mismatch");
      }
    }
    if (email) {
      const current = (targetData.email || "").toLowerCase();
      if (!current) {
        updates.email = email;
      } else if (current !== email) {
        throw new HttpsError("failed-precondition", "Target email mismatch");
      }
    }

    await targetRef.set(updates, {merge: true});
    return success({linked: true, status: "linked"}, "Identity linked", "LINKED", t);
  } catch (err) {
    console.error("[linkAccountIdentity]", err);
    return failure("LINK_FAILED", err.message || "Failed to link account identity", t);
  }
});

exports.getSoilAdvisory = onCall({region: REGION, timeoutSeconds: 60, secrets: [GROQ_API_KEY]}, async (request) => {
  const t = traceId();
  try {
    const uid = requireAuth(request);
    await enforceRateLimit(uid, "getSoilAdvisory", t);

    const data = {
      nitrogen: validateNumber(request.data?.nitrogen, "nitrogen", 0, 5000),
      phosphorus: validateNumber(request.data?.phosphorus, "phosphorus", 0, 5000),
      potassium: validateNumber(request.data?.potassium, "potassium", 0, 5000),
      ph: validateNumber(request.data?.ph, "ph", 0, 14),
      userNotes: request.data?.userNotes ? validateString(request.data.userNotes, "userNotes", 0, 600) : "",
      weather: request.data?.weather ? validateString(request.data.weather, "weather", 0, 200) : "",
      moisture: request.data?.moisture ? validateString(request.data.moisture, "moisture", 0, 80) : "",
      detectedSoil: request.data?.detectedSoil ? validateString(request.data.detectedSoil, "detectedSoil", 0, 120) : "",
      languageCode: request.data?.languageCode ? validateString(request.data.languageCode, "languageCode", 0, 20) : "en",
      location: request.data?.location ? validateString(request.data.location, "location", 0, 120) : "",
    };

    try {
      const schemaInstruction = `Return ONLY valid JSON with keys: advisoryMarkdown, contextSummary, confidence, uncertaintyMessage.`;
      const content = await callGroqChat([
        {
          role: "system",
          content: "You are an Indian agronomy advisor. Keep practical and safe recommendations. " + schemaInstruction,
        },
        {
          role: "user",
          content: `Generate a soil advisory.\nN=${data.nitrogen}, P=${data.phosphorus}, K=${data.potassium}, pH=${data.ph}\nWeather=${data.weather}\nMoisture=${data.moisture}\nSoil=${data.detectedSoil}\nNotes=${data.userNotes}\nLocation=${data.location}`,
        },
      ], {temperature: 0.2, maxTokens: 900});
      const parsed = parseFirstJson(content);
      return success({
        advisoryMarkdown: parsed.advisoryMarkdown || "",
        contextSummary: parsed.contextSummary || "",
        confidence: normalizeConfidence(parsed.confidence, 60),
        uncertaintyMessage: parsed.uncertaintyMessage || "",
      }, "Advisory generated", "ADVISORY_OK", t);
    } catch (aiErr) {
      console.error("[getSoilAdvisory][fallback]", aiErr);
      return success(fallbackSoilAdvisory(data), "Fallback advisory generated", "FALLBACK", t);
    }
  } catch (err) {
    console.error("[getSoilAdvisory]", err);
    return failure("ADVISORY_FAILED", err.message || "Soil advisory failed", t);
  }
});

exports.getCropSchedule = onCall({region: REGION, timeoutSeconds: 60, secrets: [GROQ_API_KEY]}, async (request) => {
  const t = traceId();
  try {
    const uid = requireAuth(request);
    await enforceRateLimit(uid, "getCropSchedule", t);

    const cropName = validateString(request.data?.cropName || "", "cropName", 1, 80);
    const location = request.data?.location ? validateString(request.data.location, "location", 0, 120) : "";
    const currentMonth = request.data?.currentMonth ? validateString(request.data.currentMonth, "currentMonth", 0, 40) : "";

    try {
      const schemaInstruction = "Return ONLY valid JSON with keys: crop,bestPlantingMonth,bestHarvestMonth,durationDays,schedule(array of week/activity/tips),fertilizerSchedule,irrigationTips,pestWatch,confidence,uncertaintyMessage.";
      const content = await callGroqChat([
        {role: "system", content: "You are an agriculture planner for Indian farmers. " + schemaInstruction},
        {
          role: "user",
          content: `Create crop schedule for ${cropName}. location=${location}, currentMonth=${currentMonth}.`,
        },
      ], {temperature: 0.25, maxTokens: 950});
      const parsed = parseFirstJson(content);
      return success({
        crop: parsed.crop || cropName,
        bestPlantingMonth: parsed.bestPlantingMonth || "N/A",
        bestHarvestMonth: parsed.bestHarvestMonth || "N/A",
        durationDays: Number(parsed.durationDays || 90),
        schedule: Array.isArray(parsed.schedule) ? parsed.schedule : [],
        fertilizerSchedule: parsed.fertilizerSchedule || "",
        irrigationTips: parsed.irrigationTips || "",
        pestWatch: parsed.pestWatch || "",
        confidence: normalizeConfidence(parsed.confidence, 60),
        uncertaintyMessage: parsed.uncertaintyMessage || "",
      }, "Schedule generated", "SCHEDULE_OK", t);
    } catch (aiErr) {
      console.error("[getCropSchedule][fallback]", aiErr);
      return success(fallbackCropSchedule(cropName), "Fallback schedule generated", "FALLBACK", t);
    }
  } catch (err) {
    console.error("[getCropSchedule]", err);
    return failure("SCHEDULE_FAILED", err.message || "Crop schedule failed", t);
  }
});

exports.getPlantDiagnosis = onCall({region: REGION, timeoutSeconds: 60, secrets: [GROQ_API_KEY, GEMINI_API_KEY]}, async (request) => {
  const t = traceId();
  try {
    const uid = requireAuth(request);
    await enforceRateLimit(uid, "getPlantDiagnosis", t);

    const image = validateString(request.data?.imageBase64DataUrl || "", "imageBase64DataUrl", 100, 8_000_000);
    const location = request.data?.location ? validateString(request.data.location, "location", 0, 120) : "";

    try {
      const schemaInstruction = `Return ONLY valid JSON, no markdown. Schema:
{
  "cropIdentified": "string (crop/plant name)",
  "healthStatus": "Healthy | Diseased | Stressed | Unknown",
  "confidence": number (0-100),
  "issuesDetected": ["array", "of", "issue", "strings"],
  "recommendations": ["array", "of", "actionable", "remedy", "strings"],
  "uncertaintyMessage": "string or empty"
}
issuesDetected and recommendations MUST be JSON arrays, never strings.`;

      const systemPrompt = `You are an expert plant pathologist specializing in Indian agriculture. Carefully examine the image and:
1. Identify the crop or plant from visible leaf shape, texture, colour, and structure. Use the common Indian name (e.g. Tomato, Chilli, Brinjal, Mango, Paddy, Wheat). If the exact species is unclear, use a descriptive category like "Broad-leaf vegetable", "Leafy green crop", "Cereal crop seedling", or "Fruit tree sapling" — never output "Unidentified Plant". Only use "No plant detected" if there is literally no plant in the image.
2. Set healthStatus to: Healthy, Diseased, Stressed, or Unknown.
3. List ONLY issues that are VISUALLY EVIDENT in the image (spots, lesions, yellowing, wilting, pest damage, etc.). Do not invent issues.
4. Give practical remedies for Indian farmers (organic first, then chemical if needed).
5. Set confidence based on how certain you are: well-known crop in clear image = 75-95, recognisable type but not exact species = 45-65, blurry or very unclear = below 35.
6. If the image does not show a plant or leaf at all, set cropIdentified to "No plant detected", healthStatus to "Unknown", confidence to 0.
7. Express uncertainty in uncertaintyMessage (e.g. "Exact species unclear — diagnosis based on visible symptoms"). Keep cropIdentified always farmer-friendly and descriptive.
${schemaInstruction}`;
      const userText = `Diagnose this plant image. ${location ? "Farm location: " + location + "." : ""} Give remedies appropriate for Indian farming conditions.`;

      let content;
      try {
        content = await callGeminiVision(image, systemPrompt, userText);
        console.log("[getPlantDiagnosis] Gemini responded OK");
      } catch (geminiErr) {
        console.warn("[getPlantDiagnosis][gemini-fallback]", geminiErr.message);
        content = await callGroqChat([
          {role: "system", content: systemPrompt},
          {
            role: "user",
            content: [
              {type: "text", text: userText},
              {type: "image_url", image_url: {url: image}},
            ],
          },
        ], {model: "meta-llama/llama-4-scout-17b-16e-instruct", temperature: 0.1, maxTokens: 900});
        console.log("[getPlantDiagnosis] Scout fallback responded OK");
      }

      const parsed = parseFirstJson(content);

      // Normalise issuesDetected — AI sometimes returns a string despite instructions
      let issuesDetected = parsed.issuesDetected;
      if (!Array.isArray(issuesDetected)) {
        issuesDetected = (typeof issuesDetected === "string" && issuesDetected.trim())
          ? [issuesDetected.trim()]
          : [];
      }

      // Normalise recommendations
      let recommendations = parsed.recommendations;
      if (!Array.isArray(recommendations)) {
        recommendations = (typeof recommendations === "string" && recommendations.trim())
          ? [recommendations.trim()]
          : [];
      }

      return success({
        cropIdentified: parsed.cropIdentified || "Unknown",
        healthStatus: parsed.healthStatus || "Unknown",
        confidence: normalizeConfidence(parsed.confidence, 55),
        issuesDetected,
        recommendations,
        uncertaintyMessage: parsed.uncertaintyMessage || "",
        rawJson: JSON.stringify(parsed),
      }, "Diagnosis generated", "DIAGNOSIS_OK", t);
    } catch (aiErr) {
      console.error("[getPlantDiagnosis][fallback]", aiErr);
      return success(fallbackPlantDiagnosis(), "Fallback diagnosis generated", "FALLBACK", t);
    }
  } catch (err) {
    console.error("[getPlantDiagnosis]", err);
    return failure("DIAGNOSIS_FAILED", err.message || "Plant diagnosis failed", t);
  }
});

exports.getFarmerChatResponse = onCall({region: REGION, timeoutSeconds: 45, secrets: [GROQ_API_KEY]}, async (request) => {
  const t = traceId();
  try {
    const uid = requireAuth(request);
    await enforceRateLimit(uid, "getFarmerChatResponse", t);

    const prompt = validateString(request.data?.prompt || "", "prompt", 1, 6000);
    const languageCode = request.data?.languageCode ? validateString(request.data.languageCode, "languageCode", 0, 20) : "en";

    try {
      const reply = await callGroqChat([
        {
          role: "system",
          content: `You are Kisan Sahayak, an AI assistant exclusively for Indian farmers. You ONLY answer questions related to:
- Farming, agriculture, horticulture, and crop cultivation
- Soil health, fertilizers, NPK, pH, composting
- Pest and disease management for crops
- Irrigation, water management, and drainage
- Weather and its effect on farming
- Seeds, crop varieties, and sowing seasons
- Government agricultural schemes (PM-KISAN, Fasal Bima, etc.)
- Market prices, selling crops, and mandi information
- Farm equipment and tools
- Animal husbandry, dairy, and poultry related to farming
- Post-harvest storage and processing

If a user asks ANYTHING outside these topics (coding, math, jokes, general knowledge, politics, entertainment, etc.), respond with ONLY this exact message:
"🌾 I am Kisan Sahayak, your farming assistant. I can only help with farming, soil, crops, pests, weather, and agriculture topics. Please ask me something related to farming!"

Keep replies concise, practical, in simple language. Respond in the user's language (language code: ${languageCode}). Never give unsafe advice on pesticide doses — always recommend consulting a local Krishi officer for chemical applications.`,
        },
        {
          role: "user",
          content: prompt,
        },
      ], {temperature: 0.3, maxTokens: 600});

      return success({
        replyMarkdown: reply,
        confidence: 65,
        uncertaintyMessage: "",
      }, "Chat reply generated", "CHAT_OK", t);
    } catch (aiErr) {
      console.error("[getFarmerChatResponse][fallback]", aiErr);
      return success({
        replyMarkdown: "I could not reach the advisory model right now. Please retry in a moment, or use offline guidance.",
        confidence: 20,
        uncertaintyMessage: "Fallback response generated due temporary AI unavailability.",
      }, "Fallback chat response", "FALLBACK", t);
    }
  } catch (err) {
    console.error("[getFarmerChatResponse]", err);
    return failure("CHAT_FAILED", err.message || "Chat response failed", t);
  }
});
