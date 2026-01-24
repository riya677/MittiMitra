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
}