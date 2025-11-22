package com.mittimitra;

import android.content.ContentValues;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class RecommendationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "RecommendationActivity";

    // UI Components
    private TextView tvName, tvPhone, tvEmail, tvLocation, tvDate;
    private TextView valN, ratN, valP, ratP, valK, ratK, valPh, ratPh;
    private TextView tvAiContext, tvAiAdvice;
    private View reportContainer;
    private ExtendedFloatingActionButton btnDownload;

    // Services
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL_ID = "llama-3.3-70b-versatile";

    private int analysisId = -1;
    private TextToSpeech tts;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        Toolbar toolbar = findViewById(R.id.toolbar_recommendation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        tts = new TextToSpeech(this, this);
        analysisId = getIntent().getIntExtra("analysis_id", -1);

        reportContainer = findViewById(R.id.report_container);
        btnDownload = findViewById(R.id.btn_download_pdf);

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

        btnDownload.setOnClickListener(v -> savePdf());
        btnDownload.setVisibility(View.GONE); // Hide until ready

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
            }
        }).start();
    }

    private void populateForm(SoilAnalysis analysis) {
        try {
            JSONObject json = new JSONObject(analysis.soilReportJson);

            tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(analysis.timestamp)));
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

            // --- DYNAMIC CONTEXT ---
            String userNotes = json.optString("user_notes", "None"); // Use Notes
            String weather = json.optString("weather", "N/A");
            String moisture = json.optString("soil_dynamic", "N/A");

            String contextText = String.format("Input: %s | Weather: %s | Moisture: %s", userNotes, weather, moisture);
            tvAiContext.setText(contextText);

            fetchProfessionalAdvice(n, p, k, ph, userNotes, weather, moisture);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setRow(TextView vVal, TextView vRat, int val, int low, int high, String unit) {
        vVal.setText(val + " " + unit);
        if (val < low) { vRat.setText("LOW"); vRat.setTextColor(Color.RED); }
        else if (val > high) { vRat.setText("HIGH"); vRat.setTextColor(Color.RED); }
        else { vRat.setText("OK"); vRat.setTextColor(Color.parseColor("#2E7D32")); }
    }

    private void fetchProfessionalAdvice(int n, int p, int k, double ph, String userNotes, String weather, String moisture) {

        String rawKey = BuildConfig.GROQ_API_KEY.replace("\"", "").trim();

        // DYNAMIC PROMPT
        String taskInstruction;
        if (userNotes.equalsIgnoreCase("None") || userNotes.isEmpty()) {
            // Case A: No Crop -> Recommend
            taskInstruction =
                    "1. **Crop Recommendation:** Suggest top 3 crops for this Soil NPK/pH.\n" +
                            "2. **Why:** Explain suitability briefly.\n" +
                            "3. **Soil Correction:** Advise on pH correction.";
        } else {
            // Case B: Specific Input -> Analyze
            taskInstruction =
                    "1. **Analysis for '" + userNotes + "':** Is this crop/issue relevant? Give specific advice.\n" +
                            "2. **Fertilizer Schedule:** Exact Urea/DAP/MOP dosage (kg/acre) for " + userNotes + ".\n" +
                            "3. **Care:** Address the user note directly (disease/irrigation).";
        }

        String systemPrompt = "You are a Senior Indian Agronomist. Provide strict, actionable advice in clean Markdown.";
        String userPrompt = String.format(
                "Generate Soil Health Advisory.\n\n" +
                        "DATA:\n- User Input: %s\n- Soil: N=%d P=%d K=%d pH=%.1f\n- Weather: %s\n- Moisture: %s\n\n" +
                        "TASKS:\n%s\n\nNote: Be precise.",
                userNotes, n, p, k, ph, weather, moisture, taskInstruction
        );

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", MODEL_ID);
            jsonBody.put("temperature", 0.3);
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
                runOnUiThread(() -> tvAiAdvice.setText("Network Error."));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> tvAiAdvice.setText("AI Error: " + response.code()));
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
                    runOnUiThread(() -> tvAiAdvice.setText("Error parsing response."));
                }
            }
        });
    }

    private static Spanned parseMarkdown(String markdown) {
        String html = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("####\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("###\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("##\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("^-\\s+(.*)", "• $1<br>");
        html = html.replaceAll("\\n-\\s+(.*)", "<br>• $1");
        html = html.replace("\n", "<br>");
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    }

    private void savePdf() {
        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(reportContainer.getWidth(), reportContainer.getHeight(), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            reportContainer.draw(canvas);
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
                Toast.makeText(this, "PDF Saved!", Toast.LENGTH_LONG).show();
            }
            document.close();
        } catch (Exception e) {
            Toast.makeText(this, "PDF Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("en", "IN"));
    }

    private void speakAdvice() {
        if (tts != null) tts.speak(tvAiAdvice.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Read Aloud").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        if (item.getItemId() == 1) speakAdvice();
        return true;
    }

    @Override
    protected void onDestroy() {
        if(tts != null) tts.shutdown();
        super.onDestroy();
    }
}