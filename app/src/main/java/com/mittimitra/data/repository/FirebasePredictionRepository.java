package com.mittimitra.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mittimitra.BuildConfig;
import com.mittimitra.backend.ApiEnvelope;
import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.BackendFunctionsClient;
import com.mittimitra.backend.model.AiModels;
import com.mittimitra.domain.repository.PredictionRepository;
import com.mittimitra.network.GroqApiService;
import com.mittimitra.network.RetrofitClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirebasePredictionRepository implements PredictionRepository {

    private static final String TAG = "FirebasePredictionRepo";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private final BackendFunctionsClient functionsClient;

    public FirebasePredictionRepository() {
        this.functionsClient = new BackendFunctionsClient();
    }

    @Override
    public void fetchSoilAdvisory(@NonNull AiModels.SoilAdvisoryRequest request,
                                  @NonNull BackendCallback<AiModels.SoilAdvisoryData> callback) {
        if (shouldPreferLocalFallbackForSoilAdvisory()) {
            Log.i(TAG, "Using local fallback for Soil Advisory in debug mode.");
            callback.onSuccess(ApiEnvelope.success(buildLocalSoilAdvisoryFallback(request)));
            return;
        }

        functionsClient.getSoilAdvisory(request, callback);
    }

    @Override
    public void fetchPlantDiagnosis(@NonNull AiModels.PlantDiagnosisRequest request,
                                    @NonNull BackendCallback<AiModels.PlantDiagnosisData> callback) {
        if (shouldPreferLocalFallbackForPlantDiagnosis()) {
            Log.i(TAG, "Using local-first Plant Diagnosis fallback in debug mode.");
            ApiEnvelope<AiModels.PlantDiagnosisData> preferredLocalEnvelope =
                    ApiEnvelope.error("LOCAL_FALLBACK_MODE", "Using local API fallback", null);
            fetchPlantDiagnosisWithLocalGroq(request, callback, preferredLocalEnvelope, null);
            return;
        }

        functionsClient.getPlantDiagnosis(request, new BackendCallback<AiModels.PlantDiagnosisData>() {
            @Override
            public void onSuccess(@NonNull ApiEnvelope<AiModels.PlantDiagnosisData> envelope) {
                callback.onSuccess(envelope);
            }

            @Override
            public void onFailure(@NonNull ApiEnvelope<AiModels.PlantDiagnosisData> envelope, Throwable throwable) {
                if (shouldUseLocalFallback(envelope)) {
                    Log.w(TAG, "Functions unavailable for plant diagnosis. Falling back to local Groq API key path. code=" + envelope.code);
                    fetchPlantDiagnosisWithLocalGroq(request, callback, envelope, throwable);
                    return;
                }
                callback.onFailure(envelope, throwable);
            }
        });
    }

    private boolean shouldPreferLocalFallbackForPlantDiagnosis() {
        // Backend diagnosis is higher quality and should remain the primary source.
        // Local path is retained strictly as outage fallback.
        return false;
    }

    private boolean shouldPreferLocalFallbackForSoilAdvisory() {
        // Always try backend first. Local rule-based fallback only triggers on
        // actual network/function failure via shouldUseLocalFallback().
        return false;
    }

    @Override
    public void fetchCropSchedule(@NonNull AiModels.CropScheduleRequest request,
                                  @NonNull BackendCallback<AiModels.CropScheduleData> callback) {
        functionsClient.getCropSchedule(request, callback);
    }

    @Override
    public void fetchChatResponse(@NonNull AiModels.ChatRequest request,
                                  @NonNull BackendCallback<AiModels.ChatResponseData> callback) {
        functionsClient.getFarmerChatResponse(request, callback);
    }

    private boolean shouldUseLocalFallback(@NonNull ApiEnvelope<?> envelope) {
        if (!BuildConfig.ENABLE_LOCAL_AI_FALLBACK) {
            return false;
        }
        if (BuildConfig.GROQ_API_KEY == null || BuildConfig.GROQ_API_KEY.trim().isEmpty()) {
            return false;
        }

        String code = envelope.code == null ? "" : envelope.code.toUpperCase(Locale.ROOT);
        return "NOT_FOUND".equals(code)
                || "UNAVAILABLE".equals(code)
                || "DEADLINE_EXCEEDED".equals(code)
                || "INTERNAL".equals(code)
                || "UNKNOWN_ERROR".equals(code);
    }

    private void fetchPlantDiagnosisWithLocalGroq(@NonNull AiModels.PlantDiagnosisRequest request,
                                                  @NonNull BackendCallback<AiModels.PlantDiagnosisData> callback,
                                                  @Nullable ApiEnvelope<AiModels.PlantDiagnosisData> originalEnvelope,
                                                  @Nullable Throwable originalThrowable) {
        try {
            JsonObject body = buildPlantDiagnosisPayload(request);
            GroqApiService groqService = RetrofitClient.getGroqService();
            groqService.chatCompletion("Bearer " + BuildConfig.GROQ_API_KEY, body)
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                            if (!response.isSuccessful() || response.body() == null) {
                                try (ResponseBody errorBody = response.errorBody()) {
                                    if (errorBody != null) {
                                        Log.w(TAG, "Local fallback HTTP error " + response.code() + ": " + errorBody.string());
                                    }
                                } catch (Exception io) {
                                    Log.w(TAG, "Failed reading local fallback error body", io);
                                }
                                callback.onFailure(resolveFallbackEnvelope(originalEnvelope), originalThrowable);
                                return;
                            }

                            try {
                                AiModels.PlantDiagnosisData data = parseGroqDiagnosisResponse(response.body());
                                callback.onSuccess(ApiEnvelope.success(data));
                            } catch (Exception parseError) {
                                Log.e(TAG, "Local fallback parse failed", parseError);
                                callback.onFailure(resolveFallbackEnvelope(originalEnvelope), originalThrowable);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                            Log.e(TAG, "Local fallback call failed", t);
                            callback.onFailure(resolveFallbackEnvelope(originalEnvelope), t);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Local fallback setup failed", e);
            callback.onFailure(resolveFallbackEnvelope(originalEnvelope), e);
        }
    }

    @NonNull
    private ApiEnvelope<AiModels.PlantDiagnosisData> resolveFallbackEnvelope(
            @Nullable ApiEnvelope<AiModels.PlantDiagnosisData> envelope) {
        if (envelope != null) return envelope;
        return ApiEnvelope.error("LOCAL_FALLBACK_FAILED", "Local fallback diagnosis failed", null);
    }

    private JsonObject buildPlantDiagnosisPayload(@NonNull AiModels.PlantDiagnosisRequest request) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "meta-llama/llama-4-scout-17b-16e-instruct");
        payload.addProperty("temperature", 0.1);
        payload.addProperty("max_tokens", 700);

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
                "You are a plant pathologist for Indian agriculture. " +
                        "Return ONLY valid JSON with keys: cropIdentified,healthStatus,confidence,issuesDetected,recommendations,uncertaintyMessage,rawJson.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        JsonArray content = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", "Diagnose this crop image. Location=" + safe(request.location)
                + ". Respond in language code: " + safe(request.languageCode));
        content.add(textPart);

        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");
        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", request.imageBase64DataUrl);
        imagePart.add("image_url", imageUrl);
        content.add(imagePart);

        userMessage.add("content", content);
        messages.add(userMessage);

        payload.add("messages", messages);
        return payload;
    }

    private AiModels.PlantDiagnosisData parseGroqDiagnosisResponse(@NonNull JsonObject response) throws Exception {
        if (!response.has("choices") || !response.get("choices").isJsonArray()) {
            throw new IllegalArgumentException("Invalid model response: missing choices");
        }
        if (response.getAsJsonArray("choices").size() == 0) {
            throw new IllegalArgumentException("Invalid model response: empty choices");
        }

        JsonObject choice = response.getAsJsonArray("choices").get(0).getAsJsonObject();
        JsonObject message = choice.has("message") && choice.get("message").isJsonObject()
                ? choice.getAsJsonObject("message") : null;
        if (message == null || !message.has("content")) {
            throw new IllegalArgumentException("Invalid model response: missing content");
        }
        String content = message.get("content").getAsString();
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid model response: empty content");
        }

        String jsonText = extractFirstJsonObject(content);
        JSONObject parsed = new JSONObject(jsonText);

        AiModels.PlantDiagnosisData data = new AiModels.PlantDiagnosisData();
        data.cropIdentified = firstNonEmpty(
                parsed.optString("cropIdentified", null),
                parsed.optString("crop_identified", null),
                "Unknown");
        data.healthStatus = firstNonEmpty(
                parsed.optString("healthStatus", null),
                parsed.optString("health_status", null),
                "Unknown");
        data.confidence = parseConfidence(parsed);
        // issuesDetected may be a JSON array or a plain string — handle both
        JSONArray issuesArr = parsed.optJSONArray("issuesDetected");
        if (issuesArr == null) issuesArr = parsed.optJSONArray("issues_detected");
        if (issuesArr != null) {
            for (int ii = 0; ii < issuesArr.length(); ii++) {
                String item = issuesArr.optString(ii, "").trim();
                if (!item.isEmpty()) data.issuesDetected.add(item);
            }
        } else {
            String issueStr = firstNonEmpty(
                    parsed.optString("issuesDetected", null),
                    parsed.optString("issues_detected", null), "");
            if (!issueStr.isEmpty()) data.issuesDetected.add(issueStr);
        }
        data.uncertaintyMessage = firstNonEmpty(
                parsed.optString("uncertaintyMessage", null),
                parsed.optString("uncertainty_message", null),
                "");
        if (data.uncertaintyMessage == null || data.uncertaintyMessage.trim().isEmpty()) {
            data.uncertaintyMessage = "Local fallback model response (debug mode).";
        }
        JSONArray recs = parsed.optJSONArray("recommendations");
        if (recs != null) {
            for (int i = 0; i < recs.length(); i++) {
                String item = recs.optString(i, "");
                if (!item.trim().isEmpty()) {
                    data.recommendations.add(item.trim());
                }
            }
        } else {
            String recText = parsed.optString("recommendations", "");
            if (!recText.trim().isEmpty()) {
                data.recommendations.add(recText.trim());
            }
        }
        data.rawJson = parsed.toString();
        return data;
    }

    private int parseConfidence(@NonNull JSONObject parsed) {
        Object raw = parsed.opt("confidence");
        double confidence = 50;
        if (raw instanceof Number) {
            confidence = ((Number) raw).doubleValue();
        } else if (raw instanceof String) {
            Matcher matcher = NUMBER_PATTERN.matcher((String) raw);
            if (matcher.find()) {
                try {
                    confidence = Double.parseDouble(matcher.group());
                } catch (NumberFormatException ignored) {
                    confidence = 50;
                }
            }
        }
        if (confidence > 0 && confidence <= 1.0) {
            confidence = confidence * 100.0;
        }
        int normalized = (int) Math.round(confidence);
        if (normalized < 0) return 0;
        if (normalized > 100) return 100;
        if (normalized == 0) return 50;
        return normalized;
    }

    private String extractFirstJsonObject(@NonNull String rawText) {
        String raw = rawText.trim();
        if (raw.startsWith("```")) {
            int firstNewline = raw.indexOf('\n');
            int lastFence = raw.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                raw = raw.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int firstBrace = raw.indexOf('{');
        int lastBrace = raw.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw new IllegalArgumentException("No JSON object found in model response");
        }
        return raw.substring(firstBrace, lastBrace + 1);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonEmpty(String a, String b, String fallback) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return fallback;
    }

    @NonNull
    private AiModels.SoilAdvisoryData buildLocalSoilAdvisoryFallback(@NonNull AiModels.SoilAdvisoryRequest request) {
        AiModels.SoilAdvisoryData data = new AiModels.SoilAdvisoryData();
        StringBuilder advice = new StringBuilder();

        if (request.nitrogen < 280) {
            advice.append("- Nitrogen is low. Apply split urea doses with irrigation.\n");
        } else if (request.nitrogen > 560) {
            advice.append("- Nitrogen is high. Reduce additional urea and monitor crop vigor.\n");
        }

        if (request.phosphorus < 10) {
            advice.append("- Phosphorus is low. Add SSP/DAP near root zone at recommended dose.\n");
        } else if (request.phosphorus > 25) {
            advice.append("- Phosphorus is high. Reduce phosphate application in the next cycle.\n");
        }

        if (request.potassium < 108) {
            advice.append("- Potassium is low. Apply MOP in split doses.\n");
        } else if (request.potassium > 280) {
            advice.append("- Potassium is high. Hold potash application and re-test later.\n");
        }

        if (request.ph < 6.0) {
            advice.append("- Soil is acidic. Add lime as per local agronomy recommendation.\n");
        } else if (request.ph > 7.5) {
            advice.append("- Soil is alkaline. Add gypsum and improve organic matter.\n");
        } else {
            advice.append("- Soil pH is in acceptable range. Maintain balanced nutrients.\n");
        }

        if (advice.length() == 0) {
            advice.append("- Soil indicators are broadly stable. Maintain balanced fertilization.\n");
        }

        data.advisoryMarkdown = advice.toString().trim();
        data.contextSummary = "Fallback mode | Weather: " + safe(request.weather)
                + " | Soil: " + safe(request.detectedSoil);
        data.confidence = 45;
        data.uncertaintyMessage = "Local fallback advisory (debug mode). Verify with local agronomy experts.";
        return data;
    }
}
