package com.mittimitra;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mittimitra.backend.ApiEnvelope;
import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.model.AiModels;
import com.mittimitra.config.AppConstants;
import com.mittimitra.data.repository.FirebasePredictionRepository;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.PlantHealth;
import com.mittimitra.domain.repository.PredictionRepository;
import com.mittimitra.tasks.TaskSuggestionEngine;
import com.mittimitra.utils.BitmapUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlantScanActivity extends BaseActivity {

    private static final String TAG = "PlantScanActivity";

    private ImageView ivPreview;
    private TextView tvStatus;
    private TextView tvResultTitle;
    private TextView tvResultDesc;
    private ProgressBar progressBar;
    private MaterialButton btnAnalyze;
    private MaterialCardView resultCard;

    private Bitmap currentBitmap;
    private Uri cameraImageUri;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final PredictionRepository predictionRepository = new FirebasePredictionRepository();

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (!success || cameraImageUri == null) return;
                try {
                    currentBitmap = decodeSampledBitmap(cameraImageUri, 1024);
                    ivPreview.setImageBitmap(currentBitmap);
                    resetUI();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load camera image", e);
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), result -> {
                if (result == null) return;
                try {
                    currentBitmap = decodeSampledBitmap(result, 1024);
                    ivPreview.setImageBitmap(currentBitmap);
                    resetUI();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load gallery image", e);
                    Toast.makeText(this, R.string.alert_network_error, Toast.LENGTH_SHORT).show();
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
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ivPreview = findViewById(R.id.iv_plant_preview);
        tvStatus = findViewById(R.id.tv_plant_status);
        progressBar = findViewById(R.id.progress_plant);
        MaterialButton btnCamera = findViewById(R.id.btn_plant_camera);
        MaterialButton btnGallery = findViewById(R.id.btn_plant_gallery);
        btnAnalyze = findViewById(R.id.btn_plant_analyze);
        resultCard = findViewById(R.id.card_plant_result);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvResultDesc = findViewById(R.id.tv_result_desc);

        btnCamera.setOnClickListener(v -> launchCamera());
        btnGallery.setOnClickListener(v -> galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        btnAnalyze.setOnClickListener(v -> analyzePlant());
    }

    private void launchCamera() {
        try {
            File storageDir = new File(getCacheDir(), "camera_images");
            if (!storageDir.exists()) storageDir.mkdirs();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File imageFile = new File(storageDir, "PLANT_" + timeStamp + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Log.e(TAG, "Could not create camera image file", e);
        }
    }

    private Bitmap decodeSampledBitmap(Uri uri, int maxDim) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int largest = Math.max(bounds.outWidth, bounds.outHeight);
        int sampleSize = 1;
        while (largest / (sampleSize * 2) >= maxDim) sampleSize *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(in, null, opts);
        }
    }

    private void resetUI() {
        btnAnalyze.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);
        tvStatus.setText(R.string.scan_status_ready);

        View hintLayout = findViewById(R.id.layout_plant_hint);
        if (hintLayout != null) hintLayout.setVisibility(View.GONE);
    }

    private void analyzePlant() {
        if (currentBitmap == null) {
            Toast.makeText(this, R.string.plant_select_image_first, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.plant_status_scanning);
        btnAnalyze.setEnabled(false);

        runBackendDiagnosis();
    }

    private void runBackendDiagnosis() {
        final Bitmap bitmapSnapshot = currentBitmap;
        if (bitmapSnapshot == null) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnAnalyze.setEnabled(true);
                tvStatus.setText(R.string.plant_select_image_first);
            });
            return;
        }

        dbExecutor.execute(() -> {
            try {
                // Base64 conversion is CPU/memory heavy; keep it off the main thread.
                String imageBase64 = BitmapUtils.bitmapToBase64DataUrl(bitmapSnapshot, AppConstants.JPEG_QUALITY_HIGH);
                AppPreferences prefs = new AppPreferences(this);

                AiModels.PlantDiagnosisRequest request = new AiModels.PlantDiagnosisRequest();
                request.imageBase64DataUrl = imageBase64;
                request.languageCode = prefs.getLanguage();
                request.location = getSharedPreferences("scan_cache", MODE_PRIVATE).getString("loc", "");

                predictionRepository.fetchPlantDiagnosis(request, new BackendCallback<AiModels.PlantDiagnosisData>() {
                    @Override
                    public void onSuccess(@NonNull ApiEnvelope<AiModels.PlantDiagnosisData> envelope) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnAnalyze.setEnabled(true);
                            AiModels.PlantDiagnosisData data = envelope.data;
                            if (data == null) {
                                tvStatus.setText(R.string.plant_status_error);
                                return;
                            }
                            showResult(data);
                        });
                    }

                    @Override
                    public void onFailure(@NonNull ApiEnvelope<AiModels.PlantDiagnosisData> envelope, Throwable throwable) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnAnalyze.setEnabled(true);
                            tvStatus.setText(getString(R.string.plant_status_unavailable, envelope.code));
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Backend diagnosis setup failed", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAnalyze.setEnabled(true);
                    tvStatus.setText(R.string.plant_status_error);
                });
            }
        });
    }

    private void showResult(AiModels.PlantDiagnosisData data) {
        resultCard.setVisibility(View.VISIBLE);

        String crop = nonEmpty(data.cropIdentified, "Unknown Crop");
        String health = nonEmpty(data.healthStatus, "Unknown");
        int confidence = normalizeConfidence(data.confidence);
        String issues = (data.issuesDetected != null && !data.issuesDetected.isEmpty())
                ? android.text.TextUtils.join(", ", data.issuesDetected)
                : getString(R.string.status_unknown);

        tvResultTitle.setText(crop.toUpperCase(Locale.getDefault()) + " - " + health.toUpperCase(Locale.getDefault()));
        if ("Healthy".equalsIgnoreCase(health)) {
            tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        } else {
            tvResultTitle.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }

        StringBuilder desc = new StringBuilder();
        desc.append(getString(R.string.plant_doctor_confidence, String.valueOf(confidence))).append("\n");
        desc.append("Diagnosis: ").append(issues).append("\n\n");

        if (data.recommendations != null && !data.recommendations.isEmpty()) {
            desc.append("Remedies:\n");
            for (String recommendation : data.recommendations) {
                if (recommendation == null || recommendation.trim().isEmpty()) continue;
                desc.append("- ").append(recommendation).append("\n");
            }
        }

        if (confidence < 45) {
            desc.append("\n").append(nonEmpty(data.uncertaintyMessage, getString(R.string.prediction_low_confidence_hint)));
        }

        tvResultDesc.setText(desc.toString().trim());
        tvStatus.setText(R.string.plant_status_complete);

        persistResult(data, crop, health, issues, confidence);
    }

    private void persistResult(AiModels.PlantDiagnosisData data,
                               String crop,
                               String status,
                               String issues,
                               int confidence) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final Bitmap bitmapSnapshot = currentBitmap;
        if (user == null || bitmapSnapshot == null) return;
        final String userId = user.getUid();

        dbExecutor.execute(() -> {
            try {
                String imageFileName = "plant_" + System.currentTimeMillis() + ".jpg";
                String imagePath = BitmapUtils.saveBitmapToInternalStorage(this, bitmapSnapshot, imageFileName);
                if (imagePath == null || imagePath.trim().isEmpty()) {
                    Log.e(TAG, "Failed to persist diagnosis image");
                    return;
                }

                PlantHealth entry = new PlantHealth();
                entry.userId = userId;
                entry.imagePath = imagePath;
                entry.cropName = crop;
                entry.healthStatus = status;
                entry.diagnosis = issues;
                entry.confidence = confidence;
                entry.fullJson = buildSafeRawJson(data, crop, status, issues, confidence);
                entry.timestamp = System.currentTimeMillis();

                MittiMitraDatabase.getDatabase(this).plantDao().insert(entry);
                TaskSuggestionEngine.suggestFromPlantDiagnosis(this, userId, crop, status, issues, confidence);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save diagnosis history", e);
            }
        });
    }

    private int normalizeConfidence(Integer rawConfidence) {
        if (rawConfidence == null) return 50;
        int value = rawConfidence;
        if (value < 0) return 0;
        if (value > 100) return 100;
        if (value == 0) return 50;
        return value;
    }

    private String buildSafeRawJson(AiModels.PlantDiagnosisData data,
                                    String crop,
                                    String status,
                                    String issues,
                                    int confidence) {
        if (data.rawJson != null && !data.rawJson.trim().isEmpty()) {
            return data.rawJson;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("crop_identified", crop);
            json.put("health_status", status);
            json.put("issues_detected", issues);
            json.put("confidence", confidence);
            JSONArray recs = new JSONArray();
            if (data.recommendations != null) {
                for (String item : data.recommendations) {
                    recs.put(item);
                }
            }
            json.put("recommendations", recs);
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private String nonEmpty(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdownNow();
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
    }
}
