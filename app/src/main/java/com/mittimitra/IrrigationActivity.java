package com.mittimitra;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonObject;
import com.mittimitra.network.RetrofitClient;
import com.mittimitra.utils.WeatherUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Irrigation Calculator Activity.
 * Calculates daily water requirements based on crop, field size, and weather.
 */
public class IrrigationActivity extends AppCompatActivity {

    private Spinner spinnerCrop, spinnerSoilType;
    private EditText etFieldSize;
    private MaterialButton btnCalculate;
    private ProgressBar progressBar;
    private MaterialCardView cardResult;
    private TextView tvWaterRequired, tvIrrigationAdvice, tvWeatherNote;
    private AppPreferences prefs;

    private String[] crops;
    private String[] soilTypes;

    // Water requirement in mm/day for each crop (approximate)
    private final double[] CROP_WATER_NEEDS = {
        8.0, 4.5, 5.5, 6.0, 7.0,  // Rice, Wheat, Maize, Cotton, Sugarcane
        5.0, 5.5, 4.0, 4.5, 5.0   // Tomato, Potato, Onion, Chilli, Soybean
    };

    // Soil water retention factor (1.0 = normal, <1 = needs more water)
    private final double[] SOIL_FACTORS = {
        0.8,  // Clay - retains water well
        1.3,  // Sandy - drains fast, needs more
        1.0,  // Loamy - ideal
        0.9,  // Black - good retention
        1.1   // Red - moderate drainage
    };

    private double currentTemp = 30;
    private double currentHumidity = 60;
    private double currentPrecipitation = 0;
    private double currentSoilMoisture = 0.2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_irrigation);

        Toolbar toolbar = findViewById(R.id.toolbar_irrigation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.irrigation_title);
        }

        prefs = new AppPreferences(this);

        initViews();
        setupSpinners();
        setupListeners();
        fetchCurrentWeather();
    }

    private void initViews() {
        spinnerCrop = findViewById(R.id.spinner_irrigation_crop);
        spinnerSoilType = findViewById(R.id.spinner_soil_type);
        etFieldSize = findViewById(R.id.et_field_size);
        btnCalculate = findViewById(R.id.btn_calculate);
        progressBar = findViewById(R.id.progress_irrigation);
        cardResult = findViewById(R.id.card_irrigation_result);
        tvWaterRequired = findViewById(R.id.tv_water_required);
        tvIrrigationAdvice = findViewById(R.id.tv_irrigation_advice);
        tvWeatherNote = findViewById(R.id.tv_weather_note);
    }

    private void setupSpinners() {
        crops = getResources().getStringArray(R.array.irrigation_crops);
        soilTypes = getResources().getStringArray(R.array.irrigation_soil_types);

        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, crops);
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCrop.setAdapter(cropAdapter);

        // Session: Pre-fill Crop
        String lastCrop = prefs.getLastCrop();
        if (lastCrop != null) {
            String cropName = lastCrop.split(" ")[0]; // Match by first word
            for (int i = 0; i < crops.length; i++) {
                if (crops[i].startsWith(cropName)) {
                    spinnerCrop.setSelection(i);
                    break;
                }
            }
        }

        ArrayAdapter<String> soilAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, soilTypes);
        soilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSoilType.setAdapter(soilAdapter);

        // Session: Pre-fill Soil Type
        String lastSoil = prefs.getLastSoilType();
        if (lastSoil != null) {
            for (int i = 0; i < soilTypes.length; i++) {
                if (soilTypes[i].startsWith(lastSoil)) {
                    spinnerSoilType.setSelection(i);
                    break;
                }
            }
        }

        // Session: Pre-fill Field Size
        String lastSize = prefs.getLastFieldSize();
        if (!lastSize.isEmpty()) {
            etFieldSize.setText(lastSize);
        }
    }

    private void setupListeners() {
        btnCalculate.setOnClickListener(v -> calculateWaterRequirement());
    }

    private void fetchCurrentWeather() {
        double lat = 20.5937; // Default (India Center)
        double lon = 78.9629;
        String locName = "Default Location";

        double[] savedLoc = prefs.getLastLocation();
        if (savedLoc != null) {
            lat = savedLoc[0];
            lon = savedLoc[1];
            locName = prefs.getLastLocationName();
        }

        final String finalLocName = locName;

        RetrofitClient.getWeatherService().get7DayForecast(lat, lon)
                .enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject current = response.body().getAsJsonObject("current");
                        currentTemp = current.get("temperature_2m").getAsDouble();
                        currentHumidity = current.get("relative_humidity_2m").getAsDouble();
                        currentPrecipitation = current.has("precipitation") ? 
                                current.get("precipitation").getAsDouble() : 0;
                    } catch (Exception e) {
                        // Use defaults
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Use default values
            }
        });
    }

    private void calculateWaterRequirement() {
        String fieldSizeStr = etFieldSize.getText().toString().trim();
        if (fieldSizeStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.irrigation_hint_size), Toast.LENGTH_SHORT).show();
            return;
        }

        // Save for next time
        prefs.setLastFieldSize(fieldSizeStr);

        double fieldSizeAcres;
        try {
            fieldSizeAcres = Double.parseDouble(fieldSizeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.irrigation_error_size), Toast.LENGTH_SHORT).show();
            return;
        }

        int cropIndex = spinnerCrop.getSelectedItemPosition();
        int soilIndex = spinnerSoilType.getSelectedItemPosition();

        // Base water requirement (mm/day)
        double baseWater = CROP_WATER_NEEDS[cropIndex];
        
        // Adjust for soil type
        double soilFactor = SOIL_FACTORS[soilIndex];
        
        // Adjust for temperature (higher temp = more evaporation)
        double tempFactor = 1.0;
        if (currentTemp > 35) tempFactor = 1.3;
        else if (currentTemp > 30) tempFactor = 1.15;
        else if (currentTemp < 20) tempFactor = 0.85;

        // Adjust for humidity (lower humidity = more evaporation)
        double humidityFactor = 1.0;
        if (currentHumidity < 40) humidityFactor = 1.2;
        else if (currentHumidity > 80) humidityFactor = 0.8;

        // Subtract recent precipitation
        double effectiveWater = baseWater * soilFactor * tempFactor * humidityFactor;
        effectiveWater = Math.max(0, effectiveWater - (currentPrecipitation * 0.8));

        // Convert mm to liters per acre
        // 1 mm over 1 acre = 4046.86 liters
        double litersPerAcre = effectiveWater * 4046.86;
        double totalLiters = litersPerAcre * fieldSizeAcres;

        // Display results
        String waterText = getString(R.string.irrigation_result_water, totalLiters, effectiveWater);
        tvWaterRequired.setText(waterText);

        // Get irrigation advice
        String advice = WeatherUtils.getIrrigationAdvice(currentTemp, currentHumidity, currentPrecipitation, currentSoilMoisture);
        tvIrrigationAdvice.setText(advice);

        // Weather note
        String weatherNote = getString(R.string.irrigation_weather_note, prefs.getLastLocationName(), currentTemp, currentHumidity);
        if (currentPrecipitation > 0) {
            weatherNote += getString(R.string.irrigation_rain_append, String.format("%.1f", currentPrecipitation));
        }
        tvWeatherNote.setText(weatherNote);

        cardResult.setVisibility(View.VISIBLE);
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
