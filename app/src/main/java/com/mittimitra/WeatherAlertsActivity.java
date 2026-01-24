package com.mittimitra;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mittimitra.network.RetrofitClient;
import com.mittimitra.utils.WeatherUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Weather Alerts Activity - Shows 7-day forecast with agricultural recommendations.
 * Uses Open-Meteo API for weather data.
 */
public class WeatherAlertsActivity extends AppCompatActivity {

    private EditText etLocation;
    private MaterialButton btnSearch;
    private ProgressBar progressBar;
    private MaterialCardView cardCurrent;
    private RecyclerView rvForecast;
    private TextView tvLocationName, tvCurrentTemp, tvCurrentCondition, tvHumidity, tvWind;
    private TextView tvRecommendations;

    private double currentLat, currentLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_alerts);

        Toolbar toolbar = findViewById(R.id.toolbar_weather);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.weather_alerts_title);
        }

        initViews();
        setupListeners();

        // Load weather for current location on start
        getCurrentLocationWeather();
    }

    private void initViews() {
        etLocation = findViewById(R.id.et_location);
        btnSearch = findViewById(R.id.btn_search_location);
        progressBar = findViewById(R.id.progress_weather);
        cardCurrent = findViewById(R.id.card_current_weather);
        rvForecast = findViewById(R.id.rv_forecast);
        tvLocationName = findViewById(R.id.tv_location_name);
        tvCurrentTemp = findViewById(R.id.tv_current_temp);
        tvCurrentCondition = findViewById(R.id.tv_current_condition);
        tvHumidity = findViewById(R.id.tv_humidity);
        tvWind = findViewById(R.id.tv_wind);
        tvRecommendations = findViewById(R.id.tv_recommendations);

        rvForecast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> searchLocation());
        
        etLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation();
                return true;
            }
            return false;
        });
    }

    private void searchLocation() {
        String location = etLocation.getText().toString().trim();
        if (location.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_location), Toast.LENGTH_SHORT).show();
            return;
        }
        geocodeAndFetch(location);
    }

    private void getCurrentLocationWeather() {
        // Use GPS location - for now use default coordinates (India center)
        currentLat = 20.5937;
        currentLon = 78.9629;
        tvLocationName.setText(getString(R.string.location_default));
        fetchWeatherData(currentLat, currentLon);
    }

    private void geocodeAndFetch(String locationName) {
        progressBar.setVisibility(View.VISIBLE);
        
        RetrofitClient.getGeocodingService().geocode(locationName, 1).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("results") && data.getAsJsonArray("results").size() > 0) {
                        JsonObject result = data.getAsJsonArray("results").get(0).getAsJsonObject();
                        currentLat = result.get("latitude").getAsDouble();
                        currentLon = result.get("longitude").getAsDouble();
                        
                        String name = result.get("name").getAsString();
                        if (result.has("admin1")) name += ", " + result.get("admin1").getAsString();
                        if (result.has("country")) name += ", " + result.get("country").getAsString();
                        
                        tvLocationName.setText(name);
                         
                         // Save to preferences for other apps to use
                         new AppPreferences(WeatherAlertsActivity.this).setLastLocation(currentLat, currentLon, name);

                        fetchWeatherData(currentLat, currentLon);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(WeatherAlertsActivity.this, getString(R.string.error_location_not_found), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(WeatherAlertsActivity.this, getString(R.string.error_geocoding), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WeatherAlertsActivity.this, getString(R.string.alert_network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWeatherData(double lat, double lon) {
        progressBar.setVisibility(View.VISIBLE);
        
        RetrofitClient.getWeatherForecastService().get7DayForecast(lat, lon).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    parseAndDisplayWeather(response.body());
                } else {
                    Toast.makeText(WeatherAlertsActivity.this, getString(R.string.error_fetch_weather), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WeatherAlertsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void parseAndDisplayWeather(JsonObject data) {
        try {
            // Current weather
            JsonObject current = data.getAsJsonObject("current");
            double temp = current.get("temperature_2m").getAsDouble();
            double humidity = current.get("relative_humidity_2m").getAsDouble();
            double windSpeed = current.get("wind_speed_10m").getAsDouble();
            int weatherCode = current.has("weather_code") ? current.get("weather_code").getAsInt() : 0;
            double precipitation = current.has("precipitation") ? current.get("precipitation").getAsDouble() : 0;

            String[] weatherDesc = WeatherUtils.getWeatherDescription(weatherCode);
            
            tvCurrentTemp.setText(String.format("%.1f°C", temp));
            tvCurrentCondition.setText(weatherDesc[0] + " " + weatherDesc[1]);
            tvHumidity.setText(String.format(getString(R.string.label_humidity_format), humidity));
            tvWind.setText(String.format(getString(R.string.label_wind_format), windSpeed));

            cardCurrent.setVisibility(View.VISIBLE);

            // Agricultural Recommendations
            List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(temp, humidity, windSpeed, precipitation);
            
            // Add pest warning
            String pestRisk = WeatherUtils.getPestRiskLevel(temp, humidity);
            if (!pestRisk.equals("Low")) {
                recommendations.add(0, String.format(getString(R.string.pest_risk_format), pestRisk));
            }
            
            StringBuilder recBuilder = new StringBuilder();
            for (String rec : recommendations) {
                recBuilder.append("• ").append(rec).append("\n");
            }
            tvRecommendations.setText(recBuilder.toString().trim());

            // 7-day forecast
            if (data.has("daily")) {
                JsonObject daily = data.getAsJsonObject("daily");
                List<ForecastItem> forecastItems = new ArrayList<>();
                
                JsonArray times = daily.getAsJsonArray("time");
                JsonArray maxTemps = daily.getAsJsonArray("temperature_2m_max");
                JsonArray minTemps = daily.getAsJsonArray("temperature_2m_min");
                JsonArray precipProb = daily.has("precipitation_probability_max") ? 
                    daily.getAsJsonArray("precipitation_probability_max") : null;
                
                for (int i = 0; i < Math.min(7, times.size()); i++) {
                    ForecastItem item = new ForecastItem();
                    item.date = times.get(i).getAsString();
                    item.maxTemp = maxTemps.get(i).getAsDouble();
                    item.minTemp = minTemps.get(i).getAsDouble();
                    item.precipProb = precipProb != null ? precipProb.get(i).getAsInt() : 0;
                    forecastItems.add(item);
                }
                
                rvForecast.setAdapter(new ForecastAdapter(forecastItems));
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Inner class for forecast items
    static class ForecastItem {
        String date;
        double maxTemp;
        double minTemp;
        int precipProb;
    }

    // Simple adapter for forecast
    class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {
        private final List<ForecastItem> items;

        ForecastAdapter(List<ForecastItem> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_forecast, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ForecastItem item = items.get(position);
            holder.tvDate.setText(item.date.substring(5)); // MM-DD
            holder.tvMaxTemp.setText(String.format("↑%.0f°", item.maxTemp));
            holder.tvMinTemp.setText(String.format("↓%.0f°", item.minTemp));
            holder.tvRain.setText(String.format("💧%d%%", item.precipProb));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvMaxTemp, tvMinTemp, tvRain;

            ViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_forecast_date);
                tvMaxTemp = itemView.findViewById(R.id.tv_forecast_max);
                tvMinTemp = itemView.findViewById(R.id.tv_forecast_min);
                tvRain = itemView.findViewById(R.id.tv_forecast_rain);
            }
        }
    }
}
