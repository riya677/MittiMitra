package com.mittimitra.network;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit soilRetrofit;
    private static Retrofit meteoRetrofit;
    private static Retrofit hfRetrofit; // New instance for Hugging Face

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public static SoilApiService getSoilService() {
        if (soilRetrofit == null) {
            soilRetrofit = new Retrofit.Builder()
                    .baseUrl("https://rest.isric.org/soilgrids/v2.0/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return soilRetrofit.create(SoilApiService.class);
    }

    public static OpenMeteoService getAgroService() {
        if (meteoRetrofit == null) {
            meteoRetrofit = new Retrofit.Builder()
                    .baseUrl("https://api.open-meteo.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return meteoRetrofit.create(OpenMeteoService.class);
    }

    public static HuggingFaceService getHuggingFaceService() {
        if (hfRetrofit == null) {
            hfRetrofit = new Retrofit.Builder()
                    .baseUrl("https://api-inference.huggingface.co/")
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
                    .baseUrl("https://api.groq.com/openai/v1/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return groqRetrofit.create(GroqApiService.class);
    }

    // Geocoding API
    private static Retrofit geocodingRetrofit;
    public static GeocodingService getGeocodingService() {
        if (geocodingRetrofit == null) {
            geocodingRetrofit = new Retrofit.Builder()
                    .baseUrl("https://geocoding-api.open-meteo.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return geocodingRetrofit.create(GeocodingService.class);
    }

    // Weather Forecast API (7-day)
    private static Retrofit forecastRetrofit;
    public static WeatherForecastService getWeatherForecastService() {
        if (forecastRetrofit == null) {
            forecastRetrofit = new Retrofit.Builder()
                    .baseUrl("https://api.open-meteo.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return forecastRetrofit.create(WeatherForecastService.class);
    }
}