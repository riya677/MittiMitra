package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GroqApiService {
    @POST("chat/completions")
    Call<JsonObject> chatCompletion(
        @Header("Authorization") String token,
        @Body JsonObject body
    );
}
