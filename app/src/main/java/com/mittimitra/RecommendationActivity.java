package com.mittimitra;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecommendationActivity extends BaseActivity implements TextToSpeech.OnInitListener {

    private TextView tvName, tvPhone, tvEmail, tvLocation, tvDate;
    private TextView valN, ratN, valP, ratP, valK, ratK, valPh, ratPh;
    private TextView tvAiContext, tvAiAdvice;
    
    // New Gauge Views
    private android.widget.ProgressBar gaugeSoilHealth;
    private TextView tvHealthStatus;
    
    private View reportContainer;
    private ExtendedFloatingActionButton btnDownload;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL_ID = "llama-3.3-70b-versatile";

    private int analysisId = -1;
    private TextToSpeech tts;
    private FirebaseFirestore db;
    private String passedSoilType = null;
    private Uri savedPdfUri = null; // Store saved PDF for sharing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        Toolbar toolbar = findViewById(R.id.toolbar_recommendation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        tts = new TextToSpeech(this, this);

        // Get data from intent or database
        analysisId = getIntent().getIntExtra("analysis_id", -1);
        passedSoilType = getIntent().getStringExtra("DETECTED_SOIL_TYPE");

        reportContainer = findViewById(R.id.report_container);
        btnDownload = findViewById(R.id.btn_download_pdf);
        btnDownload.setVisibility(View.GONE);

        tvName = findViewById(R.id.tv_rep_name);
        tvPhone = findViewById(R.id.tv_rep_phone);
        tvEmail = findViewById(R.id.tv_rep_email);
        tvLocation = findViewById(R.id.tv_rep_location);
        tvDate = findViewById(R.id.tv_report_date);

        valN = findViewById(R.id.tv_val_n); ratN = findViewById(R.id.tv_rat_n);
        valP = findViewById(R.id.tv_val_p); ratP = findViewById(R.id.tv_rat_p);
        valK = findViewById(R.id.tv_val_k); ratK = findViewById(R.id.tv_rat_k);
        valPh = findViewById(R.id.tv_val_ph); ratPh = findViewById(R.id.tv_rat_ph);

        tvAiContext = findViewById(R.id.tv_ai_context);
        tvAiAdvice = findViewById(R.id.tv_ai_advice);

        // Init Gauge
        gaugeSoilHealth = findViewById(R.id.gauge_soil_health);
        tvHealthStatus = findViewById(R.id.tv_health_status);

        btnDownload.setOnClickListener(v -> savePdf());

        loadFarmerProfile();
        loadAnalysisData();
    }

    private void loadFarmerProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            tvEmail.setText((email != null && !email.isEmpty()) ? email : "No Email Linked");

            db.collection("farmers").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("firstName");
                            String phone = doc.getString("phone");
                            tvName.setText((name != null && !name.isEmpty()) ? name : "Mitti Mitra Farmer");
                            tvPhone.setText((phone != null && !phone.isEmpty()) ? phone : "Not Registered");
                        } else {
                            tvName.setText("Mitti Mitra User");
                        }
                    })
                    .addOnFailureListener(e -> tvName.setText("Offline User"));
        }
    }

    private void loadAnalysisData() {
        new Thread(() -> {
            SoilAnalysis analysis;
            if (analysisId != -1) analysis = MittiMitraDatabase.getDatabase(this).soilDao().getAnalysisById(analysisId);
            else analysis = MittiMitraDatabase.getDatabase(this).soilDao().getLatestReport();

            if (analysis != null) {
                SoilAnalysis finalAnalysis = analysis;
                runOnUiThread(() -> populateForm(finalAnalysis));
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "No Report Found", Toast.LENGTH_SHORT).show();
                    // Set current date anyway so UI doesn't look broken
                    tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));
                });
            }
        }).start();
    }

    private void populateForm(SoilAnalysis analysis) {
        try {
            JSONObject json = new JSONObject(analysis.soilReportJson);

            // UPDATED DATE LOGIC: Use timestamp if valid (>0), else use current time
            long reportTime = analysis.timestamp > 0 ? analysis.timestamp : System.currentTimeMillis();
            tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(reportTime)));

            tvLocation.setText(json.optString("location", "Unknown Location"));

            int n = json.optInt("N");
            int p = json.optInt("P");
            int k = json.optInt("K");
            double ph = json.optDouble("pH", 6.5);

            setRow(valN, ratN, n, 280, 560, "kg/ha");
            setRow(valP, ratP, p, 10, 25, "kg/ha");
            setRow(valK, ratK, k, 108, 280, "kg/ha");

            valPh.setText(String.format("%.1f", ph));
            if(ph < 6.0) { ratPh.setText("ACIDIC"); ratPh.setTextColor(Color.RED); }
            else if(ph > 7.5) { ratPh.setText("ALKALINE"); ratPh.setTextColor(Color.BLUE); }
            else { ratPh.setText("NEUTRAL"); ratPh.setTextColor(Color.parseColor("#2E7D32")); }

            // --- CALCULATE SOIL HEALTH SCORE ---
            // Score Logic: 100 is perfect (pH 6.5-7.5). Deduct 15 points for every 1.0 deviation from 7.0
            float deviation = (float) Math.abs(ph - 7.0);
            int healthScore = Math.max(0, 100 - (int)(deviation * 15));
            gaugeSoilHealth.setProgress(healthScore);
            
            String statusText;
            int statusColor;
            
            if (healthScore >= 80) {
                statusText = "Excellent Condition 🌿";
                statusColor = Color.parseColor("#2E7D32"); // Green
            } else if (healthScore >= 60) {
                statusText = getString(R.string.health_good);
                statusColor = Color.DKGRAY;
            } else if (healthScore >= 40) {
                statusText = getString(R.string.health_attention);
                statusColor = Color.parseColor("#FF9800"); // Orange
            } else {
                statusText = getString(R.string.health_poor);
                statusColor = Color.RED;
            }
            
            tvHealthStatus.setText(statusText);
            tvHealthStatus.setTextColor(statusColor);
            // -----------------------------------



            String userNotes = json.optString("user_notes", "None");
            String weather = json.optString("weather", "N/A");
            String moisture = json.optString("soil_dynamic", "N/A");

            // Prefer the JSON data, but fallback to intent data if needed
            String detectedSoil = json.optString("detected_soil", passedSoilType != null ? passedSoilType : "Not Scanned");

            String contextText = String.format("Input: %s | Weather: %s | Type: %s", userNotes, weather, detectedSoil);
            tvAiContext.setText(contextText);

            fetchProfessionalAdvice(n, p, k, ph, userNotes, weather, moisture, detectedSoil);

        } catch (Exception e) {
            android.util.Log.e("RecommendationActivity", "Error parsing soil report", e);
            tvAiAdvice.setText("Error loading report data. Please try a new scan.");
        }
    }

    private void setRow(TextView vVal, TextView vRat, int val, int low, int high, String unit) {
        vVal.setText(val + " " + unit);
        if (val < low) { vRat.setText(getString(R.string.soil_status_low)); vRat.setTextColor(Color.RED); }
        else if (val > high) { vRat.setText(getString(R.string.soil_status_high)); vRat.setTextColor(Color.RED); }
        else { vRat.setText(getString(R.string.soil_status_ok)); vRat.setTextColor(Color.parseColor("#2E7D32")); }
    }

    private void fetchProfessionalAdvice(int n, int p, int k, double ph, String userNotes, String weather, String moisture, String detectedSoil) {

        String rawKey = BuildConfig.GROQ_API_KEY.replace("\"", "").trim();
        String systemPrompt =
                "You are a Senior Scientist at the Indian Council of Agricultural Research (ICAR). " +
                        "Don't mention the name of the organisation , mention only Mittimitra. " +
                        "Your task is to generate a 'Soil Health Card (SHC) Advisory' adhering to Government of India guidelines. " +
                        "Focus on Integrated Nutrient Management (INM) and long-term soil sustainability. " +
                        "Output must be formal, scientific, and strictly formatted in Markdown.";

        String taskInstruction =
                "### 1. 搭 **Soil Health Status Report**\n" +
                        "- **Diagnosis:** Classify nutrient levels (Low/Medium/High) based on Indian standards.\n" +
                        "- **Suitability:** Assess if **" + detectedSoil + "** soil is suitable for cultivation given the current status.\n\n" +

                        "### 2. 言 **Crop Planning (Agro-Climatic approach)**\n" +
                        "- Recommend 3 crops aligned with **" + detectedSoil + "** soil and local climate (" + weather + ").\n" +
                        "- **Variety Selection:** Suggest specific **ICAR/State University certified varieties** (e.g., 'Pusa Basmati', 'Co-86032').\n" +
                        "- **ECONOMIC REASONING:** For each crop, estimate current **Mandi Market Prices (INR/Quintal)** for the region. Highlight the most profitable option.\n\n" +

                        "### 3. 抽 **Balanced Fertilization Schedule (RDF)**\n" +
                        "- **Goal:** Achieve Recommended Dose of Fertilizer (RDF) for the priority crop.\n" +
                        "- **Chemical:** Prescribe exact **Neem Coated Urea**, **DAP/SSP**, and **MOP** dosage in **kg/acre**.\n" +
                        "- **Bio-Fertilizer:** Mandate use of PSB/Azotobacter or Rhizobium cultures.\n" +
                        "- **INM:** Suggest FYM or Vermicompost quantity per acre.\n\n" +

                        "### 4. 屏 **Soil Amelioration & Reclamation**\n" +
                        "- **pH Correction:** Current pH is " + String.format("%.1f", ph) + ". " +
                        (ph < 6.0 ? "Prescribe agricultural Lime/Dolomite dosage." :
                                ph > 7.5 ? "Prescribe Gypsum requirement." :
                                        "Soil reaction is neutral; advise on maintaining organic carbon.") + "\n" +
                        "- **Micro-Nutrients:** Suggest Zinc/Boron soil application if typical for " + detectedSoil + " soil.\n\n" +

                        "**Note:** Address the farmer's observation: '" + userNotes + "' with a scientific control measure.";

        String userPrompt = String.format(
                "Generate Official Soil Health Advisory.\n\n" +
                        "**FARMER DATA:**\n" +
                        "- **Observation:** %s\n" +
                        "- **Soil Texture:** %s\n" +
                        "- **Nutrient Status:** N=%d, P=%d, K=%d (kg/ha)\n" +
                        "- **pH Value:** %.1f\n" +
                        "- **Weather:** %s (Moisture: %s)\n\n" +
                        "**MANDATE:**\n%s",
                userNotes, detectedSoil, n, p, k, ph, weather, moisture, taskInstruction
        );

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", MODEL_ID);
            jsonBody.put("temperature", 0.3);
            jsonBody.put("max_tokens", 1100);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
            jsonBody.put("messages", messages);
        } catch (Exception e) { return; }

        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).build();
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + rawKey)
                .post(RequestBody.create(jsonBody.toString(), MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> tvAiAdvice.setText("Server Connection Failed."));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> tvAiAdvice.setText("Advisory Generation Failed: " + response.code()));
                        return;
                    }
                    JSONObject res = new JSONObject(response.body().string());
                    String rawContent = res.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    Spanned styledText = parseMarkdown(rawContent);
                    runOnUiThread(() -> {
                        tvAiAdvice.setText(styledText);
                        btnDownload.setVisibility(View.VISIBLE);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvAiAdvice.setText("Error formatting advisory."));
                }
            }
        });
    }

    private static Spanned parseMarkdown(String markdown) {
        String html = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("####\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("###\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("##\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("^-\\s+(.*)", "窶｢ $1<br>");
        html = html.replaceAll("\\n-\\s+(.*)", "<br>窶｢ $1");
        html = html.replace("\n", "<br>");
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    }

    private void savePdf() {
        try {
            PdfDocument document = new PdfDocument();
            // A4 Size in points (595 x 842)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            drawAgriPass(canvas); // Custom drawing

            document.finishPage(page);

            String fileName = "MittiMitra_Report_" + System.currentTimeMillis() + ".pdf";
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                document.writeTo(out);
                out.close();
                savedPdfUri = uri; // Store for sharing
                
                // Offer to share
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.share_report)
                    .setMessage("PDF saved! Would you like to share it?")
                    .setPositiveButton(R.string.share_via, (dialog, which) -> shareReport())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            }
            document.close();
        } catch (Exception e) {
            android.util.Log.e("RecommendationActivity", "PDF save error", e);
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReport() {
        if (savedPdfUri == null) {
            Toast.makeText(this, "No report to share. Save it first.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, savedPdfUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mitti Mitra Soil Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is my soil health report from Mitti Mitra app.");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("en", "IN"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Read Aloud").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, 2, 1, R.string.share_report).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == 1 && tts != null) {
            tts.speak(tvAiAdvice.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
            return true;
        }
        if (item.getItemId() == 2) {
            if (savedPdfUri != null) {
                shareReport();
            } else {
                Toast.makeText(this, "Download the report first", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void drawAgriPass(Canvas canvas) {
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.Paint titlePaint = new android.graphics.Paint();
        
        // Background
        canvas.drawColor(Color.WHITE);
        
        // Header
        titlePaint.setColor(getColor(R.color.brand_green));
        titlePaint.setTextSize(24);
        titlePaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        titlePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText("MITTI MITRA - SOIL HEALTH CARD", 595/2, 50, titlePaint);
        
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText("Official Agri-Pass | Govt. of India Guidelines Compliant", 595/2, 70, paint);
        
        // Border
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(getColor(R.color.brand_green));
        canvas.drawRect(20, 20, 575, 822, paint);
        
        // Reset Paint
        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setTextAlign(android.graphics.Paint.Align.LEFT);
        paint.setTextSize(14);
        
        int y = 120;
        int x = 40;
        
        // Farmer Details
        paint.setFakeBoldText(true);
        canvas.drawText("FARMER DETAILS", x, y, paint);
        paint.setFakeBoldText(false);
        y += 20;
        canvas.drawText("Name: " + tvName.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Location: " + tvLocation.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Date: " + tvDate.getText().toString(), x, y, paint);
        
        // Divider
        y += 30;
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(40, y, 555, y, paint);
        paint.setColor(Color.BLACK);
        
        // Soil Metrics
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("SOIL ANALYSIS RESULTS", x, y, paint);
        paint.setFakeBoldText(false);
        y += 25;
        
        // Header Row
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(40, y-15, 555, y+10, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText("PARAMETER", x+10, y, paint);
        canvas.drawText("VALUE", x+200, y, paint);
        canvas.drawText("STATUS", x+350, y, paint);
        paint.setColor(Color.BLACK);
        
        y += 30;
        drawRow(canvas, "Nitrogen (N)", valN.getText().toString(), ratN.getText().toString(), x, y);
        y += 25;
        drawRow(canvas, "Phosphorus (P)", valP.getText().toString(), ratP.getText().toString(), x, y);
        y += 25;
        drawRow(canvas, "Potassium (K)", valK.getText().toString(), ratK.getText().toString(), x, y);
        y += 25;
        drawRow(canvas, "pH Level", valPh.getText().toString(), ratPh.getText().toString(), x, y);
        
        // Advisory
        y += 50;
        paint.setFakeBoldText(true);
        canvas.drawText("SCIENTIFIC ADVISORY", x, y, paint);
        paint.setFakeBoldText(false);
        y += 25;
        
        String advisory = tvAiAdvice.getText().toString();
        // Simple text wrap (very basic)
        android.text.TextPaint textPaint = new android.text.TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12);
        
        android.text.StaticLayout staticLayout = android.text.StaticLayout.Builder.obtain(
                advisory, 0, advisory.length(), textPaint, 515)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .build();
        
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
        
        // QR Code Placeholder
        y += staticLayout.getHeight() + 50;
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(595/2 - 40, y, 595/2 + 40, y + 80, paint);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText("Scan to Verify", 595/2, y + 95, paint);
    }
    
    private void drawRow(Canvas canvas, String param, String val, String status, int x, int y) {
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        canvas.drawText(param, x+10, y, paint);
        canvas.drawText(val, x+200, y, paint);
        
        if(status.contains("LOW") || status.contains("ACIDIC")) paint.setColor(Color.RED);
        else if(status.contains("OK") || status.contains("NEUTRAL")) paint.setColor(Color.parseColor("#2E7D32"));
        else paint.setColor(Color.BLUE);
        
        canvas.drawText(status, x+350, y, paint);
    }

    @Override
    protected void onDestroy() {
        if(tts != null) tts.shutdown();
        super.onDestroy();
    }
}