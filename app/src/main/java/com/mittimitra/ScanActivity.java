package com.mittimitra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;
import com.mittimitra.network.RetrofitClient;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {

    private View initialScanGroup, confirmGroup, analysisGroup;
    private ImageView imageViewPlaceholder;
    private TextView tvLocation, tvWeather, tvSoilDynamic, tvSoilStatic, tvSolar, tvAiStatus;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String PREF_CACHE = "scan_cache";

    // Data
    private String weatherSummary = "N/A";
    private String soilDynamic = "N/A";
    private String soilStatic = "N/A";
    private String solarInfo = "N/A";
    private String locationName = "Unknown";
    private double currentLat = 0.0, currentLon = 0.0;

    // Launchers
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) displayImage(result);
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), result -> {
                if (result != null) {
                    imageViewPlaceholder.setImageURI(result);
                    fixImageStyle();
                    showConfirmUI(true);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) cameraLauncher.launch(null);
                else Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
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
        initialScanGroup = findViewById(R.id.initial_scan_group);
        confirmGroup = findViewById(R.id.confirm_group);
        analysisGroup = findViewById(R.id.analysis_group);
        tvAiStatus = findViewById(R.id.tv_analysing);
        tvLocation = findViewById(R.id.tv_location_data);
        tvWeather = findViewById(R.id.tv_weather_data);
        tvSoilDynamic = findViewById(R.id.tv_soil_dynamic_data);
        tvSoilStatic = findViewById(R.id.tv_soil_static_data);
        tvSolar = findViewById(R.id.tv_solar_data);

        // Listeners
        findViewById(R.id.btn_camera).setOnClickListener(v -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
        findViewById(R.id.btn_upload).setOnClickListener(v -> galleryLauncher.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        findViewById(R.id.btn_analyze).setOnClickListener(v -> startAnalysis());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());

        resetUI();

        // 1. INSTANT LOAD: Show cached data while waiting
        loadCachedData();

        // 2. FETCH NEW: Get fresh GPS and API data
        checkLocationAndFetchData();
    }

    private void loadCachedData() {
        SharedPreferences prefs = getSharedPreferences(PREF_CACHE, Context.MODE_PRIVATE);
        if (prefs.contains("weather")) {
            tvWeather.setText("☁️ " + prefs.getString("weather", "--"));
            tvSoilDynamic.setText("💧 " + prefs.getString("soil_dyn", "--"));
            tvSolar.setText("☀️ " + prefs.getString("solar", "--"));
            tvSoilStatic.setText("🪨 " + prefs.getString("soil_stat", "--"));
            tvLocation.setText("📍 " + prefs.getString("loc", "Detecting..."));

            // Pre-fill variables
            weatherSummary = prefs.getString("weather", "N/A");
            soilDynamic = prefs.getString("soil_dyn", "N/A");
            soilStatic = prefs.getString("soil_stat", "N/A");
            solarInfo = prefs.getString("solar", "N/A");
        }
    }

    private void cacheData(String key, String value) {
        getSharedPreferences(PREF_CACHE, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    // --- Logic ---
    private void displayImage(Bitmap bitmap) {
        imageViewPlaceholder.setImageBitmap(bitmap);
        fixImageStyle();
        showConfirmUI(true);
    }

    private void fixImageStyle() {
        // CRITICAL: Removes tint/background so image is visible
        imageViewPlaceholder.setImageTintList(null);
        imageViewPlaceholder.setBackground(null);
        imageViewPlaceholder.setPadding(0, 0, 0, 0);
        imageViewPlaceholder.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    private void checkLocationAndFetchData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
                fetchAddress(currentLat, currentLon);
                fetchAgroData(currentLat, currentLon);
                fetchSoilBaseline(currentLat, currentLon);
            }
        });
    }

    private void fetchAddress(double lat, double lon) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (!addresses.isEmpty()) {
                    locationName = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        tvLocation.setText("📍 " + locationName);
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
                        weatherSummary = String.format("%.1f°C, %d%% Hum", temp, hum);
                        tvWeather.setText("☁️ " + weatherSummary);
                        cacheData("weather", weatherSummary);

                        double soilTemp = current.get("soil_temperature_0cm").getAsDouble();
                        double soilMoist = current.get("soil_moisture_0_to_1cm").getAsDouble();
                        soilDynamic = String.format("Temp: %.1f°C | Moist: %.2f", soilTemp, soilMoist);
                        tvSoilDynamic.setText("💧 " + soilDynamic);
                        cacheData("soil_dyn", soilDynamic);

                        double uv = current.get("uv_index").getAsDouble();
                        solarInfo = String.format("UV: %.1f", uv);
                        tvSolar.setText("☀️ " + solarInfo);
                        cacheData("solar", solarInfo);
                    } catch (Exception e) {}
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void fetchSoilBaseline(double lat, double lon) {
        String[] props = {"phh2o", "nitrogen", "soc", "clay", "cec"};
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
                                tvSoilStatic.setText("🪨 " + soilStatic);
                                cacheData("soil_stat", soilStatic);
                            } catch (Exception e) {}
                        }
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void startAnalysis() {
        showAnalysisUI(true);
        new Handler(Looper.getMainLooper()).postDelayed(this::saveAndFinish, 2000);
    }

    private void saveAndFinish() {
        new Thread(() -> {
            try {
                JSONObject report = new JSONObject();
                report.put("N", new Random().nextInt(50) + 100);
                report.put("P", new Random().nextInt(30) + 10);
                report.put("K", new Random().nextInt(40) + 100);
                report.put("weather", weatherSummary);
                report.put("soil_dynamic", soilDynamic);
                report.put("soil_static", soilStatic);
                report.put("solar", solarInfo);
                report.put("location", locationName);

                SoilAnalysis analysis = new SoilAnalysis();
                analysis.timestamp = System.currentTimeMillis();
                analysis.soilReportJson = report.toString();
                MittiMitraDatabase.getDatabase(this).soilDao().insertAnalysis(analysis);

                runOnUiThread(() -> {
                    startActivity(new Intent(this, RecommendationActivity.class));
                    finish();
                });
            } catch (Exception e) {}
        }).start();
    }

    private void showAnalysisUI(boolean show) {
        analysisGroup.setVisibility(show ? View.VISIBLE : View.GONE);
        confirmGroup.setVisibility(View.GONE);
    }

    private void showConfirmUI(boolean show) {
        confirmGroup.setVisibility(show ? View.VISIBLE : View.GONE);
        initialScanGroup.setVisibility(View.GONE);
    }

    private void resetUI() {
        analysisGroup.setVisibility(View.GONE);
        confirmGroup.setVisibility(View.GONE);
        initialScanGroup.setVisibility(View.VISIBLE);
        imageViewPlaceholder.setImageResource(android.R.drawable.ic_menu_camera);
        imageViewPlaceholder.setImageTintList(ColorStateList.valueOf(Color.parseColor("#757575")));
        imageViewPlaceholder.setBackgroundColor(Color.LTGRAY);
        imageViewPlaceholder.setPadding(150, 150, 150, 150);
        imageViewPlaceholder.setScaleType(ImageView.ScaleType.CENTER);
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