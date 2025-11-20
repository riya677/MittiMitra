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

    private TextView tvDate, tvLocation, tvWeather, tvAiAdvice;
    private TextView valN, ratN, valP, ratP, valK, ratK, valPh, ratPh;
    private View reportContainer;

    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private int analysisId = -1;
    private TextToSpeech tts;

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

        reportContainer = findViewById(R.id.report_container);
        tvDate = findViewById(R.id.tv_report_date);
        tvLocation = findViewById(R.id.tv_rep_location);
        tvWeather = findViewById(R.id.tv_rep_weather);
        tvAiAdvice = findViewById(R.id.tv_ai_advice);

        valN = findViewById(R.id.tv_val_n); ratN = findViewById(R.id.tv_rat_n);
        valP = findViewById(R.id.tv_val_p); ratP = findViewById(R.id.tv_rat_p);
        valK = findViewById(R.id.tv_val_k); ratK = findViewById(R.id.tv_rat_k);
        valPh = findViewById(R.id.tv_val_ph); ratPh = findViewById(R.id.tv_rat_ph);

        findViewById(R.id.btn_save_pdf).setOnClickListener(v -> generatePdf());
        tvAiAdvice.setOnClickListener(v -> speakAdvice());

        loadData();
    }

    // --- Data Loading ---
    private void loadData() {
        new Thread(() -> {
            SoilAnalysis analysis;
            if (analysisId != -1) {
                analysis = MittiMitraDatabase.getDatabase(this).soilDao().getAnalysisById(analysisId);
            } else {
                analysis = MittiMitraDatabase.getDatabase(this).soilDao().getLatestReport();
            }

            runOnUiThread(() -> {
                if (analysis != null) populateForm(analysis);
                else tvAiAdvice.setText("No data found.");
            });
        }).start();
    }

    private void populateForm(SoilAnalysis analysis) {
        try {
            JSONObject json = new JSONObject(analysis.soilReportJson);

            tvDate.setText("Date: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(analysis.timestamp)));
            tvLocation.setText("Location: " + json.optString("location", "Unknown"));
            tvWeather.setText("Weather: " + json.optString("weather", "N/A"));

            int n = json.optInt("N");
            int p = json.optInt("P");
            int k = json.optInt("K");
            double ph = 6.5;
            if (json.has("pH")) ph = json.getDouble("pH");

            setRow(valN, ratN, n, 280, 560, "kg/ha");
            setRow(valP, ratP, p, 10, 25, "kg/ha");
            setRow(valK, ratK, k, 108, 280, "kg/ha");

            valPh.setText(String.format(Locale.US, "%.1f", ph));
            if(ph < 6.0) { ratPh.setText("Acidic"); ratPh.setTextColor(Color.parseColor("#FF5722")); }
            else if(ph > 7.5) { ratPh.setText("Alkaline"); ratPh.setTextColor(Color.parseColor("#3F51B5")); }
            else { ratPh.setText("Neutral"); ratPh.setTextColor(Color.parseColor("#4CAF50")); }

            fetchAiAdvice(json, n, p, k, ph);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setRow(TextView vVal, TextView vRat, int val, int low, int high, String unit) {
        vVal.setText(val + " " + unit);
        if (val < low) {
            vRat.setText("LOW");
            vRat.setTextColor(Color.parseColor("#FF5722"));
        } else if (val > high) {
            vRat.setText("HIGH");
            vRat.setTextColor(Color.parseColor("#2196F3"));
        } else {
            vRat.setText("NORMAL");
            vRat.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    // --- AI & TTS ---
    private void fetchAiAdvice(JSONObject data, int n, int p, int k, double ph) {
        String prompt = String.format(Locale.US,
                "Act as an Indian agricultural expert. Results: N=%d kg/ha, P=%d kg/ha, K=%d kg/ha, pH=%.1f. Location: %s. Weather: %s. \n" +
                        "Provide a concise recommendation plan in these 3 sections:\n" +
                        "1. Fertilizer Dosage (Urea, DAP, MOP)\n" +
                        "2. Organic Manure\n" +
                        "3. Crop Care Tips based on weather.",
                n, p, k, ph, data.optString("location"), data.optString("weather"));

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
                runOnUiThread(() -> tvAiAdvice.setText("AI Service Unavailable. Check connection."));
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
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("en", "IN"));
        }
    }

    private void speakAdvice() {
        String text = tvAiAdvice.getText().toString();
        if (!text.isEmpty()) {
            Toast.makeText(this, "Reading Advice...", Toast.LENGTH_SHORT).show();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // --- PDF Generation ---
    private void generatePdf() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(reportContainer.getWidth(), reportContainer.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);
        reportContainer.draw(canvas);
        document.finishPage(page);

        String fileName = "MittiMitra_Report_" + System.currentTimeMillis() + ".pdf";
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
                    Toast.makeText(this, "PDF Saved to Documents!", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "PDF saving requires Android 10+", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Force Navigate to Home, clearing back stack
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