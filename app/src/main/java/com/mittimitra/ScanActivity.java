package com.mittimitra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
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
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;
import com.mittimitra.network.RetrofitClient;
import com.mittimitra.utils.SoilDataManager;
import com.mittimitra.utils.SoilNutrientMapper; // NEW IMPORT

import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";
    private static final String MODEL_PATH = "soil_classifier.tflite";

    private static final String[] SOIL_LABELS = {
            "Alluvial", "Black", "Clay", "Red", "Sandy",
            "Loam", "Laterite", "Yellow", "Peaty", "Chalky"
    };

    // UI Components
    private ImageView imageViewPlaceholder;
    private View layoutScanHint;
    private MaterialCardView cardImageContainer;
    private Chip chipLocation;
    private TextView tvCardWeather, tvCardMoisture, tvStatusLabel;
    private TextView tvValN, tvValP, tvValK, tvValPh;
    private TextInputEditText etUserNotes;
    private MaterialButton btnAnalyze;
    private ProgressBar progressAnalysis;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String PREF_CACHE = "scan_cache";

    // Data Variables
    private String weatherSummary = "Wait...";
    private String soilDynamic = "--";
    private String locationName = "Locating...";

    private double currentLat = 0.0;
    private double currentLon = 0.0;

    // Store Final Values (Defaults)
    private double finalN = 140;
    private double finalP = 25;
    private double finalK = 80;
    private double finalpH = 6.5;

    private Bitmap currentImageBitmap = null;

    // --- Launchers (Same as before) ---
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    currentImageBitmap = result;
                    imageViewPlaceholder.setImageBitmap(result);
                    onImageSuccess();
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), result -> {
                if (result != null) {
                    imageViewPlaceholder.setImageURI(result);
                    try {
                        currentImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result);
                    } catch (IOException e) { e.printStackTrace(); }
                    onImageSuccess();
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) cameraLauncher.launch(null);
                else Toast.makeText(this, "Camera permission needed.", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Toolbar toolbar = findViewById(R.id.scan_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Bind Views
        imageViewPlaceholder = findViewById(R.id.image_view_placeholder);
        layoutScanHint = findViewById(R.id.layout_scan_hint);
        cardImageContainer = findViewById(R.id.card_image);
        chipLocation = findViewById(R.id.chip_location);
        tvCardWeather = findViewById(R.id.tv_card_weather);
        tvCardMoisture = findViewById(R.id.tv_card_moisture);
        tvStatusLabel = findViewById(R.id.tv_status_label);
        etUserNotes = findViewById(R.id.et_user_notes);
        btnAnalyze = findViewById(R.id.btn_analyze);
        progressAnalysis = findViewById(R.id.progress_analysis);

        bindNutrientCard(R.id.card_n, "N");
        bindNutrientCard(R.id.card_p, "P");
        bindNutrientCard(R.id.card_k, "K");
        bindNutrientCard(R.id.card_ph, "pH");

        findViewById(R.id.btn_camera).setOnClickListener(v -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
        findViewById(R.id.btn_upload).setOnClickListener(v ->
                galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        btnAnalyze.setOnClickListener(v -> startAnalysis());

        loadCachedData();
        checkLocationAndFetchData();
    }

    private void bindNutrientCard(int cardId, String label) {
        View card = findViewById(cardId);
        if(card != null) {
            TextView tvLabel = card.findViewById(R.id.tv_nutrient_label);
            TextView tvValue = card.findViewById(R.id.tv_nutrient_value);
            if(tvLabel != null) tvLabel.setText(label);

            if(label.equals("N")) tvValN = tvValue;
            else if(label.equals("P")) tvValP = tvValue;
            else if(label.equals("K")) tvValK = tvValue;
            else if(label.equals("pH")) tvValPh = tvValue;
        }
    }

    private void onImageSuccess() {
        imageViewPlaceholder.clearColorFilter();
        imageViewPlaceholder.setScaleType(ImageView.ScaleType.CENTER_CROP);
        layoutScanHint.setVisibility(View.GONE);
        cardImageContainer.setStrokeWidth(0);
        btnAnalyze.setVisibility(View.VISIBLE);
        btnAnalyze.setAlpha(0f);
        btnAnalyze.animate().alpha(1f).setDuration(300).start();
    }

    // --- Data Loading ---
    private void loadCachedData() {
        SharedPreferences prefs = getSharedPreferences(PREF_CACHE, Context.MODE_PRIVATE);
        weatherSummary = prefs.getString("weather", "Waiting...");
        soilDynamic = prefs.getString("soil_dyn", "--");
        locationName = prefs.getString("loc", "Locating...");
        updateDashboardUI();
    }

    private void updateDashboardUI() {
        tvCardWeather.setText(weatherSummary);
        tvCardMoisture.setText(soilDynamic.equals("--") ? "--" : soilDynamic + " m³/m³");
        chipLocation.setText(locationName);

        // Update Nutrients UI
        if(tvValN != null) tvValN.setText(String.format("%.0f", finalN));
        if(tvValP != null) tvValP.setText(String.format("%.0f", finalP));
        if(tvValK != null) tvValK.setText(String.format("%.0f", finalK));
        if(tvValPh != null) tvValPh.setText(String.format("%.1f", finalpH));
    }

    private void cacheData(String key, String value) {
        getSharedPreferences(PREF_CACHE, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    private void checkLocationAndFetchData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        tvStatusLabel.setText("Acquiring Satellite Data...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
                fetchAddress(currentLat, currentLon);
                fetchAgroData(currentLat, currentLon);
                fetchLiveISRICData(currentLat, currentLon); // NEW: Live N and pH
            } else {
                tvStatusLabel.setText("Using Offline Mode");
            }
        });
    }

    private void fetchAddress(double lat, double lon) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (!addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    locationName = (addr.getLocality() != null ? addr.getLocality() : "Unknown") + ", " + addr.getAdminArea();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        chipLocation.setText(locationName);
                        cacheData("loc", locationName);
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    // 1. LIVE API: Get Weather
    private void fetchAgroData(double lat, double lon) {
        RetrofitClient.getAgroService().getAgroWeather(lat, lon).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.body() != null) {
                    try {
                        JsonObject current = response.body().getAsJsonObject("current");
                        weatherSummary = String.format(Locale.US, "%.0f°C | %d%%",
                                current.get("temperature_2m").getAsDouble(),
                                current.get("relative_humidity_2m").getAsInt());
                        if (current.has("soil_moisture_0_to_1cm")) {
                            soilDynamic = String.format(Locale.US, "%.2f", current.get("soil_moisture_0_to_1cm").getAsDouble());
                        }
                        cacheData("weather", weatherSummary);
                        cacheData("soil_dyn", soilDynamic);
                        updateDashboardUI();
                    } catch (Exception e) { }
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    // 2. LIVE API: Get Nitrogen & pH from ISRIC
    private void fetchLiveISRICData(double lat, double lon) {
        String[] props = {"nitrogen", "phh2o"};
        RetrofitClient.getSoilService().getSoilProperties(lat, lon, props, new String[]{"0-5cm"}, "mean")
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if(response.body() != null) {
                            try {
                                JsonObject layers = response.body().getAsJsonObject("properties").getAsJsonObject("layers");

                                // Parse Nitrogen (cg/kg -> ~kg/ha roughly x10 factor for surface)
                                double nVal = layers.getAsJsonObject("nitrogen").getAsJsonObject("depths")
                                        .getAsJsonObject("0-5cm").getAsJsonObject("values").get("mean").getAsDouble();
                                finalN = nVal * 2; // Simple conversion for display

                                // Parse pH (scaled by 10)
                                double phVal = layers.getAsJsonObject("phh2o").getAsJsonObject("depths")
                                        .getAsJsonObject("0-5cm").getAsJsonObject("values").get("mean").getAsDouble();
                                finalpH = phVal / 10.0;

                                runOnUiThread(() -> {
                                    updateDashboardUI();
                                    tvStatusLabel.setText("Live Soil Data Synced");
                                });
                            } catch (Exception e) { Log.e(TAG, "ISRIC Error", e); }
                        }
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    // --- ANALYSIS (Offline TFLite) ---
    private void startAnalysis() {
        btnAnalyze.setEnabled(false);
        btnAnalyze.setText("Analyzing...");
        progressAnalysis.setVisibility(View.VISIBLE);

        if (currentImageBitmap != null) {
            runLocalInference(currentImageBitmap);
        } else {
            generateSmartReport("Not Scanned");
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private void runLocalInference(Bitmap bitmap) {
        try (Interpreter interpreter = new Interpreter(loadModelFile())) {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
            inputBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[224 * 224];
            resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224);

            int pixel = 0;
            for (int i = 0; i < 224; ++i) {
                for (int j = 0; j < 224; ++j) {
                    int val = intValues[pixel++];
                    inputBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                    inputBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                    inputBuffer.putFloat((val & 0xFF) / 255.0f);
                }
            }

            float[][] output = new float[1][10];
            interpreter.run(inputBuffer, output);

            int maxIndex = getMaxIndex(output[0]);
            String detectedSoil = (maxIndex < SOIL_LABELS.length) ? SOIL_LABELS[maxIndex] : "Unknown";

            // 3. INFER P & K from Visual Soil Type
            SoilNutrientMapper.NutrientRange estimates = SoilNutrientMapper.getEstimates(detectedSoil);
            finalP = estimates.avgP;
            finalK = estimates.avgK;

            generateSmartReport(detectedSoil);

        } catch (Exception e) {
            Log.e(TAG, "Inference Error", e);
            generateSmartReport("Analysis Error");
        }
    }

    private int getMaxIndex(float[] probabilities) {
        int maxIndex = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[maxIndex]) maxIndex = i;
        }
        return maxIndex;
    }

    private void generateSmartReport(String detectedSoilType) {
        new Thread(() -> {
            try {
                JSONObject report = new JSONObject();
                report.put("N", finalN);
                report.put("P", finalP);
                report.put("K", finalK);
                report.put("pH", finalpH);
                report.put("weather", weatherSummary);
                report.put("soil_dynamic", soilDynamic);
                report.put("location", locationName);
                report.put("detected_soil", detectedSoilType);

                String notes = etUserNotes.getText().toString().trim();
                report.put("user_notes", notes.isEmpty() ? "No notes added" : notes);

                SoilAnalysis analysis = new SoilAnalysis();
                analysis.timestamp = System.currentTimeMillis();
                analysis.soilReportJson = report.toString();
                MittiMitraDatabase.getDatabase(this).soilDao().insertAnalysis(analysis);

                runOnUiThread(() -> {
                    Intent intent = new Intent(ScanActivity.this, RecommendationActivity.class);
                    intent.putExtra("DETECTED_SOIL_TYPE", detectedSoilType);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}