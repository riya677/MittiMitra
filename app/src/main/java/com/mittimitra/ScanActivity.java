package com.mittimitra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;
import com.mittimitra.network.RetrofitClient;
import com.mittimitra.utils.SoilDataManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";

    // UI Components
    private ImageView imageViewPlaceholder;
    private View layoutScanHint;
    private Chip chipLocation;
    private TextView tvCardWeather, tvCardMoisture, tvHiddenSolar, tvHiddenSoilStatic;
    private TextInputEditText etUserNotes; // UPDATED
    private MaterialButton btnAnalyze;
    private ProgressBar progressAnalysis;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String PREF_CACHE = "scan_cache";

    // Data Variables
    private String weatherSummary = "N/A";
    private String soilDynamic = "N/A";
    private String soilStatic = "N/A";
    private String locationName = "Unknown";
    private String districtName = "Unknown";
    private double currentLat = 0.0, currentLon = 0.0;

    // --- Image Launchers ---
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    imageViewPlaceholder.setImageBitmap(result);
                    onImageCaptured();
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), result -> {
                if (result != null) {
                    imageViewPlaceholder.setImageURI(result);
                    onImageCaptured();
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) cameraLauncher.launch(null);
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Toolbar toolbar = findViewById(R.id.scan_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Bind Views
        imageViewPlaceholder = findViewById(R.id.image_view_placeholder);
        layoutScanHint = findViewById(R.id.layout_scan_hint);
        chipLocation = findViewById(R.id.chip_location);
        tvCardWeather = findViewById(R.id.tv_card_weather);
        tvCardMoisture = findViewById(R.id.tv_card_moisture);
        tvHiddenSolar = findViewById(R.id.tv_hidden_solar);
        tvHiddenSoilStatic = findViewById(R.id.tv_hidden_soil_static);
        etUserNotes = findViewById(R.id.et_user_notes); // UPDATED BINDING
        btnAnalyze = findViewById(R.id.btn_analyze);
        progressAnalysis = findViewById(R.id.progress_analysis);

        // Listeners
        findViewById(R.id.btn_camera).setOnClickListener(v -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));

        findViewById(R.id.btn_upload).setOnClickListener(v ->
                galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        btnAnalyze.setOnClickListener(v -> startAnalysis());

        // Initial State
        btnAnalyze.setEnabled(false);

        // Load Data
        loadCachedData();
        checkLocationAndFetchData();
    }

    private void onImageCaptured() {
        imageViewPlaceholder.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageViewPlaceholder.setColorFilter(null);
        imageViewPlaceholder.setBackground(null);
        layoutScanHint.setVisibility(View.GONE);
        btnAnalyze.setEnabled(true);
    }

    // --- Caching & Data Loading (Same as before) ---
    private void loadCachedData() {
        SharedPreferences prefs = getSharedPreferences(PREF_CACHE, Context.MODE_PRIVATE);
        if (prefs.contains("weather")) {
            weatherSummary = prefs.getString("weather", "N/A");
            tvCardWeather.setText(weatherSummary);
        }
        if (prefs.contains("soil_dyn")) {
            soilDynamic = prefs.getString("soil_dyn", "N/A");
            tvCardMoisture.setText(soilDynamic + " m³/m³");
        }
        if (prefs.contains("loc")) {
            locationName = prefs.getString("loc", "Unknown");
            chipLocation.setText(locationName);
        }
        if (prefs.contains("soil_stat")) {
            soilStatic = prefs.getString("soil_stat", "");
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
        chipLocation.setText("Locating...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
                fetchAddress(currentLat, currentLon);
                fetchAgroData(currentLat, currentLon);
                fetchSoilBaseline(currentLat, currentLon);
            } else {
                chipLocation.setText("Location Not Found");
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
                    locationName = addr.getLocality() + ", " + addr.getAdminArea();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        chipLocation.setText(locationName);
                        cacheData("loc", locationName);
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
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
                        int hum = current.get("relative_humidity_2m").getAsInt();
                        weatherSummary = String.format("%.1f°C\n%d%% Hum", temp, hum);
                        tvCardWeather.setText(weatherSummary);
                        cacheData("weather", weatherSummary);

                        if (current.has("soil_moisture_0_to_1cm")) {
                            double soilMoist = current.get("soil_moisture_0_to_1cm").getAsDouble();
                            soilDynamic = String.format("%.2f", soilMoist);
                            tvCardMoisture.setText(soilDynamic + " m³/m³");
                            cacheData("soil_dyn", soilDynamic);
                        }
                    } catch (Exception e) { Log.e(TAG, "Weather Parse Error", e); }
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void fetchSoilBaseline(double lat, double lon) {
        String[] props = {"phh2o", "nitrogen", "soc", "clay"};
        RetrofitClient.getSoilService().getSoilProperties(lat, lon, props, new String[]{"0-5cm"}, "mean")
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.body() != null) {
                            try {
                                JsonObject properties = response.body().getAsJsonObject("properties");
                                JsonArray layers = properties.getAsJsonArray("layers");
                                StringBuilder sb = new StringBuilder();
                                for (JsonElement layer : layers) {
                                    JsonObject obj = layer.getAsJsonObject();
                                    String name = obj.get("name").getAsString();
                                    JsonArray depths = obj.getAsJsonArray("depths");
                                    if (depths.size() > 0) {
                                        int val = depths.get(0).getAsJsonObject().getAsJsonObject("values").get("mean").getAsInt();
                                        if (name.equals("phh2o")) sb.append("pH:").append(val/10.0).append(" ");
                                        if (name.equals("nitrogen")) sb.append("N:").append(val/100.0).append("g ");
                                    }
                                }
                                soilStatic = sb.toString();
                                cacheData("soil_stat", soilStatic);
                                if(tvHiddenSoilStatic != null) tvHiddenSoilStatic.setText(soilStatic);
                            } catch (Exception e) { Log.e(TAG, "Soil Parse Error", e); }
                        }
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void startAnalysis() {
        btnAnalyze.setVisibility(View.INVISIBLE);
        progressAnalysis.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(this::generateSmartReport, 1500);
    }

    private void generateSmartReport() {
        new Thread(() -> {
            try {
                SoilDataManager.SoilProfile csvData = SoilDataManager.getDistrictAverage(this, districtName);

                JSONObject report = new JSONObject();
                report.put("N", csvData.isFound ? csvData.N : 150);
                report.put("P", csvData.isFound ? csvData.P : 20);
                report.put("K", csvData.isFound ? csvData.K : 100);
                report.put("pH", csvData.isFound ? csvData.pH : 6.5);

                // UPDATED: Capture User Input instead of Spinner
                String notes = etUserNotes.getText().toString().trim();
                report.put("user_notes", notes.isEmpty() ? "None" : notes);

                report.put("weather", weatherSummary);
                report.put("soil_dynamic", soilDynamic);
                report.put("location", locationName);
                report.put("satellite_data", soilStatic);

                SoilAnalysis analysis = new SoilAnalysis();
                analysis.timestamp = System.currentTimeMillis();
                analysis.soilReportJson = report.toString();
                MittiMitraDatabase.getDatabase(this).soilDao().insertAnalysis(analysis);

                runOnUiThread(() -> {
                    startActivity(new Intent(this, RecommendationActivity.class));
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
}