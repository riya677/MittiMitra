package com.mittimitra.network;

import com.mittimitra.config.ApiConfig;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit soilRetrofit;
    private static Retrofit meteoRetrofit;
    private static Retrofit hfRetrofit;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build();

    public static SoilApiService getSoilService() {
        if (soilRetrofit == null) {
            soilRetrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.SOIL_GRIDS_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return soilRetrofit.create(SoilApiService.class);
    }

    public static OpenMeteoService getWeatherService() {
        if (meteoRetrofit == null) {
            meteoRetrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.OPEN_METEO_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return meteoRetrofit.create(OpenMeteoService.class);
    }

    public static HuggingFaceService getHuggingFaceService() {
        if (hfRetrofit == null) {
            hfRetrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.HF_API_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return hfRetrofit.create(HuggingFaceService.class);
    }

    private static Retrofit groqRetrofit;
    public static GroqApiService getGroqService() {
        if (groqRetrofit == null) {
            groqRetrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.GROQ_API_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return groqRetrofit.create(GroqApiService.class);
    }

    private static Retrofit geocodingRetrofit;
    public static GeocodingService getGeocodingService() {
        if (geocodingRetrofit == null) {
            geocodingRetrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.GEOCODING_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return geocodingRetrofit.create(GeocodingService.class);
    }

    // --- DATA.GOV.IN (Mandi Prices) ---
    private static Retrofit mandiRetrofit;
    public static MandiApiService getMandiService() {
        if (mandiRetrofit == null) {
            mandiRetrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.DATA_GOV_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return mandiRetrofit.create(MandiApiService.class);
    }
}
