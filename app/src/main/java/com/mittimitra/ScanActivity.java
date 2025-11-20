package com.mittimitra;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;
import com.mittimitra.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {

    private View initialScanGroup, confirmGroup, analysisGroup;
    private ImageView imageViewPlaceholder;
    private TextView tvAiStatus, tvSoilContext, tvWeatherContext;
    private ProgressBar progressBarContext;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // Image Launchers
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    imageViewPlaceholder.setImageBitmap(result);
                    showConfirmUI(true);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) cameraLauncher.launch(null);
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), result -> {
                if (result != null) {
                    imageViewPlaceholder.setImageURI(result);
                    showConfirmUI(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.scan_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Views
        imageViewPlaceholder = findViewById(R.id.image_view_placeholder);
        initialScanGroup = findViewById(R.id.initial_scan_group);
        confirmGroup = findViewById(R.id.confirm_group);
        analysisGroup = findViewById(R.id.analysis_group);

        // Context Views (Add these IDs to activity_scan.xml if missing, or ignore if just for AI)
        // For now, we assume they might not exist in your layout yet, so we check for null
        tvAiStatus = findViewById(R.id.tv_analysing); // Reusing analysis text for status

        // Buttons
        MaterialButton btnCamera = findViewById(R.id.btn_camera);
        MaterialButton btnUpload = findViewById(R.id.btn_upload);
        FloatingActionButton btnShutter = findViewById(R.id.btn_shutter);
        MaterialButton btnAnalyze = findViewById(R.id.btn_analyze);
        MaterialButton btnReset = findViewById(R.id.btn_reset);
        MaterialButton btnCancel = findViewById(R.id.btn_cancel);

        // Listeners
        btnCamera.setOnClickListener(v -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
        btnShutter.setOnClickListener(v -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
        btnUpload.setOnClickListener(v -> galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));

        btnAnalyze.setOnClickListener(v -> startAnalysis());
        btnReset.setOnClickListener(v -> resetUI());
        btnCancel.setOnClickListener(v -> resetUI());

        resetUI();

        // Start AI Context Gathering immediately
        checkLocationAndFetchData();
    }

    private void startAnalysis() {
        showAnalysisUI(true);
        // Simulate Analysis Delay
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this, "Analysis Complete! (Mock)", Toast.LENGTH_SHORT).show();
            resetUI();
        }, 3000);
    }

    private void checkLocationAndFetchData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                fetchContextData(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void fetchContextData(double lat, double lon) {
        // Weather
        RetrofitClient.getWeatherService().getCurrentWeather(lat, lon, "metric", BuildConfig.OPENWEATHER_API_KEY)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        // Data fetched successfully - store for AI model
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });

        // Soil
        String[] props = {"phh2o", "ocd", "clay"};
        RetrofitClient.getSoilService().getSoilProperties(lat, lon, props, "0-5cm", "mean")
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
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
        imageViewPlaceholder.setImageResource(android.R.color.darker_gray);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationAndFetchData();
        }
    }
}