package com.mittimitra;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.firebase.auth.FirebaseUser;
import com.mittimitra.network.RetrofitClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Crop Calendar Activity - AI-generated planting and harvesting schedules.
 * Uses Groq API to generate personalized schedules based on crop and location.
 */
public class CropCalendarActivity extends AppCompatActivity {

    private static final String TAG = "CropCalendarActivity";
    
    private Spinner spinnerCrop;
    private MaterialButton btnGenerate;
    private ProgressBar progressBar;
    private MaterialCardView cardSchedule;
    private TextView tvScheduleTitle, tvScheduleContent;
    private TextView tvPlantingDate, tvHarvestDate, tvDuration;

    private final String[] CROPS = {
        "Rice (धान)", "Wheat (गेहूं)", "Maize (मक्का)", "Cotton (कपास)",
        "Sugarcane (गन्ना)", "Soybean (सोयाबीन)", "Groundnut (मूंगफली)",
        "Tomato (टमाटर)", "Potato (आलू)", "Onion (प्याज)",
        "Chilli (मिर्च)", "Brinjal (बैंगन)", "Cabbage (पत्तागोभी)",
        "Cauliflower (फूलगोभी)", "Mustard (सरसों)"
    };

    private OkHttpClient httpClient;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

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

        httpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

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
                android.R.layout.simple_spinner_item, CROPS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCrop.setAdapter(adapter);

        // Pre-select last used crop from Session
        String lastCrop = new AppPreferences(this).getLastCrop();
        if (lastCrop != null) {
            for (int i = 0; i < CROPS.length; i++) {
                if (CROPS[i].equals(lastCrop)) {
                    spinnerCrop.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupListeners() {
        btnGenerate.setOnClickListener(v -> generateSchedule());
    }

    private void generateSchedule() {
        String selectedCrop = CROPS[spinnerCrop.getSelectedItemPosition()];
        
        // Save to Session for Irrigation Calculator
        new AppPreferences(this).setLastCrop(selectedCrop);

        String cropName = selectedCrop.split(" ")[0]; // Get English name only
        
        progressBar.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);
        cardSchedule.setVisibility(View.GONE);

        // Get current date for context
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        String currentMonth = sdf.format(new Date());

        // Build AI prompt
        String prompt = String.format(
            "You are an expert agricultural advisor for Indian farmers. " +
            "Generate a detailed planting and harvesting schedule for %s crop. " +
            "Current month is %s. Location: Central India. " +
            "Respond ONLY with valid JSON (no markdown, no extra text):\n" +
            "{\n" +
            "  \"crop\": \"%s\",\n" +
            "  \"best_planting_month\": \"Month name\",\n" +
            "  \"best_harvest_month\": \"Month name\",\n" +
            "  \"duration_days\": 90,\n" +
            "  \"schedule\": [\n" +
            "    {\"week\": 1, \"activity\": \"Land preparation\", \"tips\": \"Plow field 2-3 times\"},\n" +
            "    {\"week\": 2, \"activity\": \"Sowing\", \"tips\": \"Maintain seed spacing\"}\n" +
            "  ],\n" +
            "  \"fertilizer_schedule\": \"NPK recommendations\",\n" +
            "  \"irrigation_tips\": \"Water requirements\",\n" +
            "  \"pest_watch\": \"Common pests to monitor\"\n" +
            "}", 
            cropName, currentMonth, cropName
        );

        callGroqAPI(prompt, selectedCrop);
    }

    private void callGroqAPI(String prompt, String cropName) {
        // Use Retrofit (Gson)
        String apiKey = BuildConfig.GROQ_API_KEY.replace("\"", "").trim();
        String token = "Bearer " + apiKey;

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(userMessage);

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "llama-3.3-70b-versatile");
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.3);
        payload.addProperty("max_tokens", 1024);

        RetrofitClient.getGroqService().chatCompletion(token, payload).enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject json = response.body();
                        String content = json.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString();

                        runOnUiThread(() -> parseAndDisplaySchedule(content, cropName));
                    } catch (Exception e) {
                        Log.e(TAG, "Groq Parse Error", e);
                        runOnUiThread(() -> showError(getString(R.string.crop_error_ai)));
                    }
                } else {
                    if (response.errorBody() != null) {
                        try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                            Log.e(TAG, "Groq API Error: " + errorBody.string());
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    runOnUiThread(() -> showError(getString(R.string.crop_error_service)));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network Failure", t);
                runOnUiThread(() -> showError(getString(R.string.crop_error_network)));
            }
        });
    }

    private void parseAndDisplaySchedule(String content, String cropName) {
        progressBar.setVisibility(View.GONE);
        btnGenerate.setEnabled(true);

        try {
            // Clean JSON if wrapped in markdown
            String cleaned = content.trim();
            if (cleaned.contains("```json")) {
                cleaned = cleaned.split("```json")[1].split("```")[0];
            } else if (cleaned.contains("```")) {
                cleaned = cleaned.split("```")[1].split("```")[0];
            }
            cleaned = cleaned.trim();

            JSONObject schedule = new JSONObject(cleaned);
            
            tvScheduleTitle.setText("📅 " + cropName + " Schedule");
            tvPlantingDate.setText("🌱 Plant: " + schedule.optString("best_planting_month", "N/A"));
            tvHarvestDate.setText("🌾 Harvest: " + schedule.optString("best_harvest_month", "N/A"));
            tvDuration.setText("⏱️ Duration: " + schedule.optInt("duration_days", 90) + " days");

            StringBuilder scheduleText = new StringBuilder();
            
            // Weekly schedule
            if (schedule.has("schedule")) {
                scheduleText.append("📋 Weekly Activities:\n\n");
                JSONArray activities = schedule.getJSONArray("schedule");
                for (int i = 0; i < activities.length(); i++) {
                    JSONObject activity = activities.getJSONObject(i);
                    scheduleText.append("Week ").append(activity.optInt("week", i+1))
                            .append(": ").append(activity.optString("activity", ""))
                            .append("\n   → ").append(activity.optString("tips", ""))
                            .append("\n\n");
                }
            }

            // Additional info
            if (schedule.has("fertilizer_schedule")) {
                scheduleText.append("🧪 Fertilizer: ").append(schedule.getString("fertilizer_schedule")).append("\n\n");
            }
            if (schedule.has("irrigation_tips")) {
                scheduleText.append("💧 Irrigation: ").append(schedule.getString("irrigation_tips")).append("\n\n");
            }
            if (schedule.has("pest_watch")) {
                scheduleText.append("🐛 Pest Watch: ").append(schedule.getString("pest_watch"));
            }

            tvScheduleContent.setText(scheduleText.toString().trim());
            cardSchedule.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing schedule", e);
            // Fallback: show raw response
            tvScheduleTitle.setText("📅 " + cropName + " Schedule");
            tvPlantingDate.setText("");
            tvHarvestDate.setText("");
            tvDuration.setText("");
            tvScheduleContent.setText(content);
            cardSchedule.setVisibility(View.VISIBLE);
        }

        // SAVE TO DATABASE
        FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            dbExecutor.execute(() -> {
                com.mittimitra.database.entity.CropSchedule entry = new com.mittimitra.database.entity.CropSchedule();
                entry.userId = user.getUid();
                entry.cropName = cropName;
                entry.fullJson = content; // Save raw JSON content
                entry.timestamp = System.currentTimeMillis();

                // Extract basic info again for easy access (redundant parsing but safer separation)
                try {
                    String cleaned = content.trim();
                    if (cleaned.contains("```json")) {
                        cleaned = cleaned.split("```json")[1].split("```")[0];
                    } else if (cleaned.contains("```")) {
                        cleaned = cleaned.split("```")[1].split("```")[0];
                    }
                    JSONObject schedule = new JSONObject(cleaned);
                    entry.plantingDate = schedule.optString("best_planting_month", "N/A");
                    entry.harvestDate = schedule.optString("best_harvest_month", "N/A");
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing schedule for DB", e);
                    entry.plantingDate = "N/A";
                    entry.harvestDate = "N/A";
                }

                com.mittimitra.database.MittiMitraDatabase.getDatabase(this).cropDao().insert(entry);
                Log.d(TAG, "Schedule saved to DB");
            });
        }

    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
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
