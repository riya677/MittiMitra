package com.mittimitra.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/auth/signup")
    Call<AuthResponse> signup(@Body SignupRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("api/auth/health")
    Call<String> health();
}