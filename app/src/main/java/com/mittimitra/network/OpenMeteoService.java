package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoService {
    // Fetches Weather, Soil Moisture, Solar Radiation, UV, AND Elevation in ONE call
    @GET("v1/forecast?current=temperature_2m,relative_humidity_2m,rain,precipitation,weather_code,wind_speed_10m,soil_temperature_0cm,soil_moisture_0_to_1cm,soil_moisture_3_to_9cm,shortwave_radiation,uv_index&daily=sunrise,sunset&timezone=auto")
    Call<JsonObject> getAgroWeather(
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );

    @GET("v1/forecast?current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=7")
    Call<JsonObject> get7DayForecast(
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );
}