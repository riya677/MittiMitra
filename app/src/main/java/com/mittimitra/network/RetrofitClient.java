package com.mittimitra.network;

import com.mittimitra.config.ApiConfig;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build();

    // Eagerly initialized - thread-safe with no synchronization overhead
    private static final Retrofit soilRetrofit = new Retrofit.Builder()
            .baseUrl(ApiConfig.SOIL_GRIDS_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final Retrofit meteoRetrofit = new Retrofit.Builder()
            .baseUrl(ApiConfig.OPEN_METEO_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final Retrofit geocodingRetrofit = new Retrofit.Builder()
            .baseUrl(ApiConfig.GEOCODING_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final Retrofit mandiRetrofit = new Retrofit.Builder()
            .baseUrl(ApiConfig.DATA_GOV_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final Retrofit groqRetrofit = new Retrofit.Builder()
            .baseUrl(ApiConfig.GROQ_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public static SoilApiService getSoilService() {
        return soilRetrofit.create(SoilApiService.class);
    }

    public static OpenMeteoService getWeatherService() {
        return meteoRetrofit.create(OpenMeteoService.class);
    }

    public static GeocodingService getGeocodingService() {
        return geocodingRetrofit.create(GeocodingService.class);
    }

    public static MandiApiService getMandiService() {
        return mandiRetrofit.create(MandiApiService.class);
    }

    public static GroqApiService getGroqService() {
        return groqRetrofit.create(GroqApiService.class);
    }
}

