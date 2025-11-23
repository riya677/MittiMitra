package com.mittimitra.network;

import com.google.gson.JsonObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SoilApiService {
    @GET("properties/query")
    Call<JsonObject> getSoilProperties(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("property") List<String> properties,
            @Query("depth") String depth,
            @Query("value") String value
    );
}