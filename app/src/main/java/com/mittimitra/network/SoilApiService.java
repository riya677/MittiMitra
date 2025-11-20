package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SoilApiService {
    // ISRIC SoilGrids API
    // Example: /properties/query?lat=12.34&lon=76.56&property=phh2o&property=ocd&depth=0-5cm
    @GET("properties/query")
    Call<JsonObject> getSoilProperties(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("property") String[] properties, // phh2o, ocd, clay, sand, nitrogen
            @Query("depth") String depth,           // "0-5cm"
            @Query("value") String value            // "mean"
    );
}