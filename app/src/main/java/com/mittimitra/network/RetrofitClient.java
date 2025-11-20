package com.mittimitra.network;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit soilRetrofit;
    private static Retrofit meteoRetrofit;

    // Create a custom client with 60s timeout for slow APIs (like ISRIC)
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Connect fast
            .readTimeout(60, TimeUnit.SECONDS)    // Wait up to 60s for data
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public static SoilApiService getSoilService() {
        if (soilRetrofit == null) {
            soilRetrofit = new Retrofit.Builder()
                    .baseUrl("https://rest.isric.org/soilgrids/v2.0/")
                    .client(client) // Apply the custom client here
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return soilRetrofit.create(SoilApiService.class);
    }

    public static OpenMeteoService getAgroService() {
        if (meteoRetrofit == null) {
            meteoRetrofit = new Retrofit.Builder()
                    .baseUrl("https://api.open-meteo.com/")
                    .client(client) // Apply here too for stability
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return meteoRetrofit.create(OpenMeteoService.class);
    }
}