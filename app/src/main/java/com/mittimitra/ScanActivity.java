package com.mittimitra;

import com.mittimitra.utils.NutrientStatus;
import com.mittimitra.utils.WeatherUtils;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;
import com.mittimitra.network.RetrofitClient;
import com.mittimitra.utils.SoilDataManager;
import com.mittimitra.utils.SoilNutrientMapper;

import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.google.android.material.snackbar.Snackbar;

public class ScanActivity extends BaseActivity implements SensorEventListener {

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

    // Variables
    private String weatherSummary = "Wait...";
    private String soilDynamic = "--";
    private String locationName = ""; // Loaded in init
    private String districtName = "Unknown"; // Restored variable

    private double currentLat = 0.0;
    private double currentLon = 0.0;

    // Default values (0 means loading)
    private double finalN = 0;
    private double finalP = 0;
    private double finalK = 0;
    private double finalpH = 0;

    private Bitmap currentImageBitmap = null;

    // Sensors
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private boolean isLowLight = false;

    // --- Launchers ---
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
        
        // Sensor Setup
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

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
        
        if (locationName.isEmpty()) locationName = getString(R.string.scan_locating);
        if (weatherSummary.equals("Wait...")) weatherSummary = getString(R.string.status_calculating);
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

        if (tvValN != null) {
            NutrientStatus.Status s = NutrientStatus.getNitrogenStatus(finalN);
            tvValN.setText(String.format("%.0f (%s)", finalN, s.label));
            tvValN.setTextColor(s.color);
        }
        // Repeat for P, K, and pH using their respective status methods
        if (tvValP != null) {
            NutrientStatus.Status s = NutrientStatus.getPhosphorusStatus(finalP);
            tvValP.setText(String.format("%.0f (%s)", finalP, s.label));
            tvValP.setTextColor(s.color);
        }
        if (tvValK != null) {
            NutrientStatus.Status s = NutrientStatus.getPotassiumStatus(finalK);
            tvValK.setText(String.format("%.0f (%s)", finalK, s.label));
            tvValK.setTextColor(s.color);
        }
        if (tvValPh != null) {
            NutrientStatus.Status s = NutrientStatus.getPhStatus(finalpH);
            tvValPh.setText(String.format("%.1f (%s)", finalpH, s.label));
            tvValPh.setTextColor(s.color);
        }
    }

    private void cacheData(String key, String value) {
        getSharedPreferences(PREF_CACHE, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    private void checkLocationAndFetchData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        tvStatusLabel.setText(getString(R.string.alert_connecting));

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
                fetchAddress(currentLat, currentLon);
                fetchAgroData(currentLat, currentLon);

                // CALL LIVE SOIL DATA
                fetchLiveSoilData(currentLat, currentLon);
            } else {
                tvStatusLabel.setText(getString(R.string.alert_offline_mode));
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
                    districtName = addr.getSubAdminArea();
                    if (districtName == null) districtName = addr.getLocality();
                    locationName = (addr.getLocality() != null ? addr.getLocality() : "Unknown") + ", " + addr.getAdminArea();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        chipLocation.setText(locationName);
                        cacheData("loc", locationName);
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding failed", e);
            }
        }).start();
    }

    private void fetchAgroData(double lat, double lon) {
        RetrofitClient.getAgroService().getAgroWeather(lat, lon).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.body() != null) {
                    try {
                        JsonObject current = response.body().getAsJsonObject("current");
                        
                        double temp = current.get("temperature_2m").getAsDouble();
                        int humidity = current.get("relative_humidity_2m").getAsInt();
                        double precipitation = current.has("precipitation") ? current.get("precipitation").getAsDouble() : 0.0;
                        double windSpeed = current.has("wind_speed_10m") ? current.get("wind_speed_10m").getAsDouble() : 0.0;
                        int weatherCode = current.has("weather_code") ? current.get("weather_code").getAsInt() : 0;

                        // Weather Summary
                        String[] desc = WeatherUtils.getWeatherDescription(weatherCode);
                        weatherSummary = String.format(Locale.US, "%s %.0f°C | %d%%", desc[0], temp, humidity);

                        if (current.has("soil_moisture_0_to_1cm")) {
                            soilDynamic = String.format(Locale.US, "%.2f", current.get("soil_moisture_0_to_1cm").getAsDouble());
                        }
                        
                        // Show actionable alerts if any
                        List<String> alerts = WeatherUtils.getAgriculturalRecommendations(temp, humidity, windSpeed, precipitation);
                        if(!alerts.isEmpty() && !alerts.get(0).startsWith("✅")) {
                             String primaryAlert = alerts.get(0).split(":")[0]; // Get Title
                             runOnUiThread(() -> Snackbar.make(layoutScanHint, "⚠️ Alert: " + primaryAlert, Snackbar.LENGTH_LONG).show());
                        }

                        cacheData("weather", weatherSummary);
                        cacheData("soil_dyn", soilDynamic);
                        updateDashboardUI();
                    } catch (Exception e) {
                        Log.e(TAG, "Weather parsing error", e);
                        // Use cached data on parse error
                        updateDashboardUI();
                    }
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Weather fetch failed", t);
                runOnUiThread(() -> {
                    // Use cached data, just update UI
                    updateDashboardUI();
                });
            }
        });
    }

    // --- FIXED: CORRECT JSON ARRAY PARSING ---
    private void fetchLiveSoilData(double lat, double lon) {
        List<String> props = new ArrayList<>(Arrays.asList("nitrogen", "phh2o", "soc", "clay"));

        RetrofitClient.getSoilService().getSoilProperties(lat, lon, props, "0-5cm", "mean")
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if(response.body() != null) {
                            try {
                                // Log raw response
                                Log.d(TAG, "ISRIC Response: " + response.body().toString());

                                // FIX: 'layers' is a JsonArray, loop through it
                                JsonArray layers = response.body().getAsJsonObject("properties").getAsJsonArray("layers");

                                for (JsonElement layerElement : layers) {
                                    JsonObject layer = layerElement.getAsJsonObject();
                                    String name = layer.get("name").getAsString();
                                    double value = getMeanValue(layer);

                                    if (name.equals("nitrogen")) {
                                        // Unit: cg/kg -> Convert to kg/ha (approx)
                                        finalN = value * 2.0;
                                    }
                                    else if (name.equals("phh2o")) {
                                        // Unit: pH*10 -> Convert to real pH
                                        finalpH = value / 10.0;
                                    }
                                    else if (name.equals("soc")) {
                                        // Organic Carbon -> Calculate P
                                        finalP = 10 + (value * 0.2);
                                    }
                                    else if (name.equals("clay")) {
                                        // Clay % -> Calculate K
                                        finalK = 50 + (value * 3.5);
                                    }
                                }

                                runOnUiThread(() -> {
                                    updateDashboardUI();
                                    tvStatusLabel.setText(getString(R.string.alert_data_received));
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Parsing Error", e);
                                runOnUiThread(() -> tvStatusLabel.setText(getString(R.string.alert_data_unavailable)));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Log.e(TAG, "Network Fail", t);
                        runOnUiThread(() -> tvStatusLabel.setText(getString(R.string.alert_network_error)));
                    }
                });
    }

    private double getMeanValue(JsonObject layer) {
        try {
            return layer.getAsJsonArray("depths")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("values")
                    .get("mean").getAsDouble();
        } catch (Exception e) {
            return 0.0;
        }
    }

    // --- ANALYSIS (TFLite) ---
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

                // Save soil type for Irrigation Calculator
                new AppPreferences(ScanActivity.this).setLastSoilType(detectedSoilType);

                runOnUiThread(() -> {
                    Intent intent = new Intent(ScanActivity.this, RecommendationActivity.class);
                    intent.putExtra("DETECTED_SOIL_TYPE", detectedSoilType);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationAndFetchData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            if (lux < 50 && !isLowLight) {
                isLowLight = true;
                Snackbar.make(layoutScanHint, getString(R.string.alert_too_dark), Snackbar.LENGTH_SHORT).show();
            } else if (lux >= 50 && isLowLight) {
                isLowLight = false; // Reset if light improves
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}