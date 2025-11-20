package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SoilApiService {
    @GET("properties/query")
    Call<JsonObject> getSoilProperties(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("property") String[] properties,
            @Query("depth") String[] depths,
            @Query("value") String value
    );
}