package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Open-Meteo Geocoding API Service.
 * Converts location names to coordinates.
 */
public interface GeocodingService {
    @GET("v1/search")
    Call<JsonObject> geocode(
            @Query("name") String locationName,
            @Query("count") int count
    );
}
