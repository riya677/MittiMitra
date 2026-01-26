package com.mittimitra.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service interface for data.gov.in Agricultural Market Prices API.
 * Provides access to real-time commodity prices from Indian mandis.
 */
public interface MandiApiService {

    /**
     * Fetch current daily commodity prices from agricultural markets.
     * API Resource: Current Daily Price of Various Commodities from Various Markets (Mandi)
     * 
     * @param apiKey    Your data.gov.in API key
     * @param format    Response format (json recommended)
     * @param state     State filter (e.g., "Maharashtra")
     * @param commodity Commodity filter (e.g., "Onion")
     * @param limit     Number of results to return
     * @return JsonObject containing records array with price data
     */
    @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
    Call<JsonObject> getCommodityPrices(
        @Query("api-key") String apiKey,
        @Query("format") String format,
        @Query("filters[state]") String state,
        @Query("filters[commodity]") String commodity,
        @Query("limit") int limit
    );

    /**
     * Fetch prices for a specific date range.
     */
    @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
    Call<JsonObject> getCommodityPricesWithDate(
        @Query("api-key") String apiKey,
        @Query("format") String format,
        @Query("filters[state]") String state,
        @Query("filters[commodity]") String commodity,
        @Query("filters[arrival_date]") String date,
        @Query("limit") int limit
    );
}
