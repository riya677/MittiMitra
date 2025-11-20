package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoService {
    // CHANGED: soil_moisture_1_to_3cm -> soil_moisture_9_to_27cm (Root Zone)
    @GET("v1/forecast?current=temperature_2m,relative_humidity_2m,rain,soil_temperature_0cm,soil_moisture_0_to_1cm,soil_moisture_9_to_27cm,shortwave_radiation,uv_index&daily=sunrise,sunset&timezone=auto")
    Call<JsonObject> getAgroWeather(
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );

    @GET("v1/elevation")
    Call<JsonObject> getElevation(
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );
}