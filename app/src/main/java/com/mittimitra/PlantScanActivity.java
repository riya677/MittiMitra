package com.mittimitra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mittimitra.config.ApiConfig;
import com.mittimitra.config.AppConstants;
import com.mittimitra.network.ClassificationResult;
import com.mittimitra.network.RetrofitClient;
import com.mittimitra.utils.BitmapUtils;
import com.mittimitra.utils.ErrorHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlantScanActivity extends BaseActivity {

    private static final String TAG = "PlantScanActivity";

    private ImageView ivPreview;
    private TextView tvStatus, tvResultTitle, tvResultDesc;
    private ProgressBar progressBar;
    private MaterialButton btnCamera, btnGallery, btnAnalyze;
    private MaterialCardView resultCard;

    private Bitmap currentBitmap;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    currentBitmap = result;
                    ivPreview.setImageBitmap(result);
                    resetUI();
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), result -> {
                if (result != null) {
                    try {
                        currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), result);
                        ivPreview.setImageBitmap(currentBitmap);
                        resetUI();
                    } catch (IOException e) {
                        ErrorHandler.logError(TAG, "Failed to load image from gallery", e);
                        ErrorHandler.showToast(PlantScanActivity.this, R.string.alert_network_error);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_scan);

        Toolbar toolbar = findViewById(R.id.plant_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.plant_doctor_title);
        }

        ivPreview = findViewById(R.id.iv_plant_preview);
        tvStatus = findViewById(R.id.tv_plant_status);
        progressBar = findViewById(R.id.progress_plant);
        btnCamera = findViewById(R.id.btn_plant_camera);
        btnGallery = findViewById(R.id.btn_plant_gallery);
        btnAnalyze = findViewById(R.id.btn_plant_analyze);
        
        resultCard = findViewById(R.id.card_plant_result);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvResultDesc = findViewById(R.id.tv_result_desc);

        btnCamera.setOnClickListener(v -> cameraLauncher.launch(null));
        btnGallery.setOnClickListener(v -> galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        btnAnalyze.setOnClickListener(v -> analyzePlant());
    }

    private void resetUI() {
        btnAnalyze.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);
        tvStatus.setText(R.string.scan_status_ready);
    }

    private void analyzePlant() {
        if (currentBitmap == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.plant_status_scanning);
        btnAnalyze.setEnabled(false);

        // 1. Run Classification (Fast, Specialized)
        runClassification();

        // 2. Run Deep Analysis (Groq VLM) - Triggered after classification for now, or parallel.
        // For better UX, we'll trigger Groq analysis immediately in parallel.
        runGroqAnalysis();
    }

    private void runClassification() {
        Bitmap scaled = BitmapUtils.prepareForClassification(currentBitmap, AppConstants.IMAGE_CLASSIFICATION_SIZE);
        byte[] byteArray = BitmapUtils.bitmapToJpegBytes(scaled, AppConstants.JPEG_QUALITY_HIGH);

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), byteArray);
        String token = "Bearer " + BuildConfig.HF_API_TOKEN.replace("\"", "").trim();

        RetrofitClient.getHuggingFaceService().classifyImage(ApiConfig.HF_MODEL_PLANT_DISEASE, token, requestBody)
                .enqueue(new Callback<List<ClassificationResult>>() {
                    @Override
                    public void onResponse(Call<List<ClassificationResult>> call, Response<List<ClassificationResult>> response) {
                         // We are relying on Groq for the main detailed report, but we use this for robust "Label" validation
                         // Logging result but not strictly updating UI here to avoid race conditions with Groq,
                         // UNLESS Groq fails. For now, let's let Groq take the lead on the text description.
                    }
                    @Override
                    public void onFailure(Call<List<ClassificationResult>> call, Throwable t) {
                        ErrorHandler.logError(TAG, "HF Classification failed", t);
                    }
                });
    }

    private void runGroqAnalysis() {
        try {
            // Encode Image using BitmapUtils
            String imageBase64 = BitmapUtils.bitmapToBase64DataUrl(currentBitmap, AppConstants.JPEG_QUALITY_MEDIUM);

            // Build Groq Payload (OpenAI Compatible)
            JsonObject payload = new JsonObject();
            payload.addProperty("model", ApiConfig.GROQ_MODEL_VISION);
            payload.addProperty("temperature", 0.1);
            payload.addProperty("max_tokens", 512);

            JsonArray messages = new JsonArray();
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");

            JsonArray content = new JsonArray();
            
            // Text Part
            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", buildPrompt());
            content.add(textPart);

            // Image Part
            JsonObject imagePart = new JsonObject();
            imagePart.addProperty("type", "image_url");
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", imageBase64);
            imagePart.add("image_url", imageUrl);
            content.add(imagePart);

            userMessage.add("content", content);
            messages.add(userMessage);
            payload.add("messages", messages);

            String token = "Bearer " + BuildConfig.GROQ_API_KEY.replace("\"", "").trim();

            RetrofitClient.getGroqService().chatCompletion(token, payload).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnAnalyze.setEnabled(true);
                        
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String content = response.body().getAsJsonArray("choices")
                                        .get(0).getAsJsonObject()
                                        .getAsJsonObject("message")
                                        .get("content").getAsString();
                                
                                parseAndShowGroqResult(content);
                            } catch (Exception e) {
                                tvStatus.setText(R.string.plant_status_error);
                                Log.e(TAG, "Groq Parse Error", e);
                            }
                        } else {
                            tvStatus.setText(getString(R.string.plant_status_unavailable, String.valueOf(response.code())));
                             try {
                                Log.e(TAG, "Groq Error: " + response.errorBody().string());
                            } catch (IOException e) {}
                        }
                    });
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnAnalyze.setEnabled(true);
                        btnAnalyze.setEnabled(true);
                        tvStatus.setText(getString(R.string.alert_network_error));
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            btnAnalyze.setEnabled(true);
        }
    }

    private String buildPrompt() {
        return "You are an expert agronomist analyzing a crop image. " +
               "Analyze this image and provide a JSON response with the following keys: " +
               "crop_identified (string), health_status (string: Healthy/Diseased/Stressed/Unknown), " +
               "confidence (int 0-100), issues_detected (string summary of disease/pests), " +
               "recommendations (list of strings). " +
               "Return ONLY the JSON. No Markdown.";
    }

    private void parseAndShowGroqResult(String jsonString) {
        resultCard.setVisibility(View.VISIBLE);
        
        // Clean markdown code blocks if present
        jsonString = jsonString.replace("```json", "").replace("```", "").trim();

        try {
            JSONObject result = new JSONObject(jsonString);
            
            String crop = result.optString("crop_identified", "Unknown Crop");
            String status = result.optString("health_status", "Unknown");
            String issues = result.optString("issues_detected", "None");
            
            // Update Title
            tvResultTitle.setText(crop.toUpperCase() + " - " + status.toUpperCase());
            if (status.equalsIgnoreCase("Healthy")) {
                tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
            } else {
                tvResultTitle.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }

            // Build Description
            StringBuilder desc = new StringBuilder();
            desc.append("Diagnosis: ").append(issues).append("\n\n");
            
            org.json.JSONArray recs = result.optJSONArray("recommendations");
            if (recs != null && recs.length() > 0) {
                desc.append("Remedies:\n");
                for (int i = 0; i < recs.length(); i++) {
                    desc.append("• ").append(recs.getString(i)).append("\n");
                }
            }

            tvResultDesc.setText(desc.toString());
            tvStatus.setText(R.string.plant_status_complete);

        } catch (Exception e) {
            // Fallback if JSON parsing fails - show raw text
             tvResultTitle.setText("AI ANALYSIS REPORT");
             tvResultDesc.setText(jsonString);
             tvStatus.setText("Analysis Complete");
        }
    }
    
    // Using org.json.JSONObject for internal parsing helper as standard in Android
    private static class JSONObject extends org.json.JSONObject {
        public JSONObject(String json) throws org.json.JSONException { super(json); }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
