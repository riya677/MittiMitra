package com.mittimitra.config;

/**
 * Centralized API and ML model configuration.
 * All API endpoints and model identifiers should be defined here.
 */
public final class ApiConfig {

    private ApiConfig() {
        // Prevent instantiation
    }

    // AI provider calls are intentionally removed from client-side networking.
    // All advisory/diagnosis/schedule requests must go through authenticated backend functions.

    // ========== OPEN METEO ==========
    public static final String OPEN_METEO_BASE = "https://api.open-meteo.com/";
    public static final String GEOCODING_BASE = "https://geocoding-api.open-meteo.com/";

    // ========== SOIL DATA ==========
    public static final String SOIL_GRIDS_BASE = "https://rest.isric.org/soilgrids/v2.0/";

    // ========== OPTIONAL DEBUG AI FALLBACK ==========
    public static final String GROQ_BASE = "https://api.groq.com/openai/v1/";

    // ========== LOCAL ML MODELS ==========
    public static final String TFLITE_SOIL_CLASSIFIER = "soil_classifier.tflite";

    // ========== NETWORK TIMEOUTS (seconds) ==========
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    // ========== DATA.GOV.IN (Mandi Prices) ==========
    public static final String DATA_GOV_BASE = "https://api.data.gov.in/";
    public static final String MANDI_RESOURCE_ID = "9ef84268-d588-465a-a308-a864a43d0070";
}
