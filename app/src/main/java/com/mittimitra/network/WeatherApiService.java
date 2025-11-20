package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    @GET("data/2.5/weather")
    Call<JsonObject> getCurrentWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("units") String units, // "metric"
            @Query("appid") String apiKey
    );
}