package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Open-Meteo Forecast API Service.
 * Fetches 7-day weather forecast with agricultural parameters.
 */
public interface WeatherForecastService {
    @GET("v1/forecast?current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=7")
    Call<JsonObject> get7DayForecast(
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );
}
