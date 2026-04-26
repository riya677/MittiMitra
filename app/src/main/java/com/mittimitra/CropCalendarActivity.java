package com.mittimitra;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.mittimitra.backend.ApiEnvelope;
import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.model.AiModels;
import com.mittimitra.data.repository.FirebasePredictionRepository;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.CropSchedule;
import com.mittimitra.domain.repository.PredictionRepository;
import com.mittimitra.tasks.TaskSuggestionEngine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Crop Calendar Activity - AI generated planting and harvesting schedules.
 */
public class CropCalendarActivity extends BaseActivity {

    private static final String TAG = "CropCalendarActivity";

    private Spinner spinnerCrop;
    private MaterialButton btnGenerate;
    private ProgressBar progressBar;
    private MaterialCardView cardSchedule;
    private TextView tvScheduleTitle;
    private TextView tvScheduleContent;
    private TextView tvPlantingDate;
    private TextView tvHarvestDate;
    private TextView tvDuration;

    private final String[] crops = {
            "Rice", "Wheat", "Maize", "Cotton", "Sugarcane", "Soybean", "Groundnut",
            "Tomato", "Potato", "Onion", "Chilli", "Brinjal", "Cabbage", "Cauliflower", "Mustard"
    };

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final PredictionRepository predictionRepository = new FirebasePredictionRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_calendar);

        Toolbar toolbar = findViewById(R.id.toolbar_calendar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.crop_calendar_title);
        }

        initViews();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        spinnerCrop = findViewById(R.id.spinner_crop);
        btnGenerate = findViewById(R.id.btn_generate_schedule);
        progressBar = findViewById(R.id.progress_calendar);
        cardSchedule = findViewById(R.id.card_schedule);
        tvScheduleTitle = findViewById(R.id.tv_schedule_title);
        tvScheduleContent = findViewById(R.id.tv_schedule_content);
        tvPlantingDate = findViewById(R.id.tv_planting_date);
        tvHarvestDate = findViewById(R.id.tv_harvest_date);
        tvDuration = findViewById(R.id.tv_duration);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, crops);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCrop.setAdapter(adapter);

        String lastCrop = new AppPreferences(this).getLastCrop();
        if (lastCrop == null) return;

        for (int i = 0; i < crops.length; i++) {
            if (crops[i].equalsIgnoreCase(lastCrop.split(" ")[0])) {
                spinnerCrop.setSelection(i);
                break;
            }
        }
    }

    private void setupListeners() {
        btnGenerate.setOnClickListener(v -> generateSchedule());
    }

    private void generateSchedule() {
        String selectedCrop = crops[spinnerCrop.getSelectedItemPosition()];
        new AppPreferences(this).setLastCrop(selectedCrop);

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnGenerate.setEnabled(false);
        cardSchedule.setVisibility(android.view.View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        String currentMonth = sdf.format(new Date());

        AiModels.CropScheduleRequest request = new AiModels.CropScheduleRequest();
        request.cropName = selectedCrop;
        request.currentMonth = currentMonth;
        request.location = getSharedPreferences("scan_cache", MODE_PRIVATE).getString("loc", "Central India");
        request.languageCode = new AppPreferences(this).getLanguage();

        predictionRepository.fetchCropSchedule(request, new BackendCallback<AiModels.CropScheduleData>() {
            @Override
            public void onSuccess(@NonNull ApiEnvelope<AiModels.CropScheduleData> envelope) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnGenerate.setEnabled(true);

                    AiModels.CropScheduleData data = envelope.data;
                    if (data == null) {
                        showError(getString(R.string.crop_error_ai));
                        return;
                    }

                    displaySchedule(data, selectedCrop);
                    persistSchedule(data, selectedCrop);
                });
            }

            @Override
            public void onFailure(@NonNull ApiEnvelope<AiModels.CropScheduleData> envelope, Throwable throwable) {
                Log.e(TAG, "Crop schedule fetch failed", throwable);
                runOnUiThread(() -> showError(getString(R.string.crop_error_network)));
            }
        });
    }

    private void displaySchedule(AiModels.CropScheduleData data, String cropName) {
        tvScheduleTitle.setText(getString(R.string.crop_schedule_title_format, cropName));
        tvPlantingDate.setText(getString(R.string.crop_plant_label_format, nonEmpty(data.bestPlantingMonth, "N/A")));
        tvHarvestDate.setText(getString(R.string.crop_harvest_label_format, nonEmpty(data.bestHarvestMonth, "N/A")));
        tvDuration.setText(getString(R.string.crop_duration_format, data.durationDays != null ? data.durationDays : 90));

        StringBuilder scheduleText = new StringBuilder();
        if (data.schedule != null && !data.schedule.isEmpty()) {
            scheduleText.append(getString(R.string.crop_weekly_activities)).append("\n\n");
            for (AiModels.ScheduleItem item : data.schedule) {
                if (item == null) continue;
                int week = item.week != null ? item.week : 0;
                scheduleText.append(getString(R.string.crop_week_item_format,
                        week,
                        nonEmpty(item.activity, getString(R.string.status_unknown)),
                        nonEmpty(item.tips, "")));
                scheduleText.append("\n");
            }
        }

        if (data.fertilizerSchedule != null && !data.fertilizerSchedule.isEmpty()) {
            scheduleText.append("\n").append(getString(R.string.crop_fertilizer_label)).append(data.fertilizerSchedule).append("\n");
        }
        if (data.irrigationTips != null && !data.irrigationTips.isEmpty()) {
            scheduleText.append("\n").append(getString(R.string.crop_irrigation_label)).append(data.irrigationTips).append("\n");
        }
        if (data.pestWatch != null && !data.pestWatch.isEmpty()) {
            scheduleText.append("\n").append(getString(R.string.crop_pest_watch_label)).append(data.pestWatch).append("\n");
        }
        if (data.confidence != null && data.confidence < 45) {
            scheduleText.append("\n").append(nonEmpty(data.uncertaintyMessage, getString(R.string.prediction_low_confidence_hint)));
        }

        tvScheduleContent.setText(scheduleText.toString().trim());
        cardSchedule.setVisibility(android.view.View.VISIBLE);
    }

    private void persistSchedule(AiModels.CropScheduleData data, String cropName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        final android.content.Context appContext = getApplicationContext();
        final String uid = user.getUid();

        dbExecutor.execute(() -> {
            CropSchedule entry = new CropSchedule();
            entry.userId = uid;
            entry.cropName = cropName;
            entry.plantingDate = nonEmpty(data.bestPlantingMonth, "N/A");
            entry.harvestDate = nonEmpty(data.bestHarvestMonth, "N/A");
            entry.fullJson = new Gson().toJson(data);
            entry.timestamp = System.currentTimeMillis();

            MittiMitraDatabase.getDatabase(appContext).cropDao().insert(entry);
            TaskSuggestionEngine.suggestFromCropSchedule(appContext, uid, cropName, data);
        });
    }

    private String nonEmpty(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    private void showError(String message) {
        progressBar.setVisibility(android.view.View.GONE);
        btnGenerate.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdownNow();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
