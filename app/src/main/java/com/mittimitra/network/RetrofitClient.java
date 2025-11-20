package com.mittimitra.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit soilRetrofit;
    private static Retrofit weatherRetrofit;

    public static SoilApiService getSoilService() {
        if (soilRetrofit == null) {
            soilRetrofit = new Retrofit.Builder()
                    .baseUrl("https://rest.isric.org/soilgrids/v2.0/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return soilRetrofit.create(SoilApiService.class);
    }

    public static WeatherApiService getWeatherService() {
        if (weatherRetrofit == null) {
            weatherRetrofit = new Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return weatherRetrofit.create(WeatherApiService.class);
    }
}