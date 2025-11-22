package com.mittimitra;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    private TextView tvDate, tvName, tvPhone, tvState, tvDistrict, tvSeason, tvAiAdvice;
    private TextView tvWeather, tvSolar, tvMoisture;
    private TextView valN, ratN, valP, ratP, valK, ratK, valPh, ratPh;
    private View reportContainer;

    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private int analysisId = -1;
    private TextToSpeech tts;
    private SessionManager sessionManager;
    private String currentSeason = "Unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Soil Health Report");
        }

        analysisId = getIntent().getIntExtra("analysis_id", -1);
        tts = new TextToSpeech(this, this);
        sessionManager = new SessionManager(this);

        // Initialize Views
        reportContainer = findViewById(R.id.report_container);
        tvDate = findViewById(R.id.tv_report_date);
        tvName = findViewById(R.id.tv_rep_name);
        tvPhone = findViewById(R.id.tv_rep_phone);
        tvState = findViewById(R.id.tv_rep_state);
        tvDistrict = findViewById(R.id.tv_rep_district);
        tvSeason = findViewById(R.id.tv_rep_season);
        tvWeather = findViewById(R.id.tv_rep_weather);
        tvSolar = findViewById(R.id.tv_rep_solar);
        tvMoisture = findViewById(R.id.tv_rep_moisture);
        tvAiAdvice = findViewById(R.id.tv_ai_advice);

        valN = findViewById(R.id.tv_val_n); ratN = findViewById(R.id.tv_rat_n);
        valP = findViewById(R.id.tv_val_p); ratP = findViewById(R.id.tv_rat_p);
        valK = findViewById(R.id.tv_val_k); ratK = findViewById(R.id.tv_rat_k);
        valPh = findViewById(R.id.tv_val_ph); ratPh = findViewById(R.id.tv_rat_ph);

        findViewById(R.id.btn_save_pdf).setOnClickListener(v -> generatePdf());

        // THIS LINE CAUSED THE ERROR IF THE METHOD WAS MISSING
        tvAiAdvice.setOnClickListener(v -> speakAdvice());

        loadFarmerDetails();
        calculateSeason();
        loadData();
    }

    private void loadFarmerDetails() {
        String name = sessionManager.getUserName();
        tvName.setText(name != null ? name : "Farmer");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String phone = user.getPhoneNumber();
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "N/A");
        }
    }

    private void calculateSeason() {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH);
        if (month >= 5 && month <= 8) currentSeason = "Kharif";
        else if (month >= 9 || month <= 2) currentSeason = "Rabi";
        else currentSeason = "Zaid";
        tvSeason.setText(currentSeason);
    }

    private void loadData() {
        new Thread(() -> {
            SoilAnalysis analysis;
            if (analysisId != -1) analysis = MittiMitraDatabase.getDatabase(this).soilDao().getAnalysisById(analysisId);
            else analysis = MittiMitraDatabase.getDatabase(this).soilDao().getLatestReport();

            runOnUiThread(() -> {
                if (analysis != null) populateForm(analysis);
            });
        }).start();
    }

    private void populateForm(SoilAnalysis analysis) {
        try {
            JSONObject json = new JSONObject(analysis.soilReportJson);

            tvDate.setText("Date: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(analysis.timestamp)));
            tvWeather.setText(json.optString("weather", "N/A"));
            tvSolar.setText(json.optString("solar", "N/A"));
            tvMoisture.setText(json.optString("soil_dynamic", "N/A"));

            String rawLoc = json.optString("location", "Unknown, Unknown");
            String[] parts = rawLoc.split(",");
            if (parts.length >= 2) {
                tvDistrict.setText(parts[0].trim());
                tvState.setText(parts[1].trim());
            } else {
                tvDistrict.setText(rawLoc);
            }

            int n = json.optInt("N");
            int p = json.optInt("P");
            int k = json.optInt("K");
            double ph = 6.5;
            if (json.has("pH")) ph = json.getDouble("pH");

            setRow(valN, ratN, n, 280, 560, "kg/ha");
            setRow(valP, ratP, p, 10, 25, "kg/ha");
            setRow(valK, ratK, k, 108, 280, "kg/ha");

            valPh.setText(String.format(Locale.US, "%.1f", ph));
            if(ph < 6.0) { ratPh.setText("Acidic"); ratPh.setTextColor(Color.RED); }
            else if(ph > 7.5) { ratPh.setText("Alkaline"); ratPh.setTextColor(Color.BLUE); }
            else { ratPh.setText("Neutral"); ratPh.setTextColor(Color.parseColor("#4CAF50")); }

            fetchAiAdvice(json, n, p, k, ph);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setRow(TextView vVal, TextView vRat, int val, int low, int high, String unit) {
        vVal.setText(val + " " + unit);
        if (val < low) {
            vRat.setText("LOW 🔴"); vRat.setTextColor(Color.RED);
        } else if (val > high) {
            vRat.setText("HIGH 🔴"); vRat.setTextColor(Color.RED);
        } else {
            vRat.setText("OPTIMAL 🟢"); vRat.setTextColor(Color.parseColor("#2E7D32"));
        }
    }

    private void fetchAiAdvice(JSONObject data, int n, int p, int k, double ph) {
        String prompt = String.format(Locale.US,
                "Act as an Indian agricultural scientist. State: %s. Season: %s. Soil: N=%d, P=%d, K=%d, pH=%.1f. \n" +
                        "Provide a recommendation in 3 sections:\n" +
                        "1. Best Crops\n2. Fertilizer Dosage\n3. Crop Care.",
                tvState.getText(), currentSeason, n, p, k, ph);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "llama3-8b-8192");
            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.put(msg);
            jsonBody.put("messages", messages);
        } catch (Exception e) {}

        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder().url(API_URL).addHeader("Authorization", "Bearer " + GROQ_API_KEY).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> tvAiAdvice.setText("AI Service Unavailable."));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() != null) {
                    try {
                        JSONObject res = new JSONObject(response.body().string());
                        String content = res.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                        runOnUiThread(() -> tvAiAdvice.setText(content));
                    } catch (Exception e) {}
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) { tts.setLanguage(new Locale("en", "IN")); }
    }

    // --- THIS WAS THE MISSING METHOD ---
    private void speakAdvice() {
        String text = tvAiAdvice.getText().toString();
        if (!text.isEmpty()) {
            Toast.makeText(this, "Reading...", Toast.LENGTH_SHORT).show();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    // -----------------------------------

    private void generatePdf() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(reportContainer.getWidth(), reportContainer.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);
        reportContainer.draw(canvas);
        document.finishPage(page);
        String fileName = "Report_" + System.currentTimeMillis() + ".pdf";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    document.writeTo(out);
                    if (out != null) out.close();
                    Toast.makeText(this, "PDF Saved!", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {}
        document.close();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}