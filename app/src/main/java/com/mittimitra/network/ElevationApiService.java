package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ElevationApiService {
    // Example: https://api.open-meteo.com/v1/elevation?latitude=52.52&longitude=13.41
    @GET("v1/elevation")
    Call<JsonObject> getElevation(
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );
}