package com.mittimitra.config;

/**
 * Centralized API and ML model configuration.
 * All API endpoints and model identifiers should be defined here.
 */
public final class ApiConfig {

    private ApiConfig() {
        // Prevent instantiation
    }

    // ========== GROQ API ==========
    public static final String GROQ_API_BASE = "https://api.groq.com/openai/v1/";
    public static final String GROQ_CHAT_ENDPOINT = GROQ_API_BASE + "chat/completions";
    
    // Groq Model IDs
    public static final String GROQ_MODEL_CHAT = "llama-3.3-70b-versatile";
    public static final String GROQ_MODEL_VISION = "meta-llama/llama-4-scout-17b-16e-instruct";

    // ========== HUGGING FACE ==========
    public static final String HF_API_BASE = "https://api-inference.huggingface.co/";
    public static final String HF_MODEL_PLANT_DISEASE = 
            "https://api-inference.huggingface.co/models/linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification";

    // ========== OPEN METEO ==========
    public static final String OPEN_METEO_BASE = "https://api.open-meteo.com/";
    public static final String GEOCODING_BASE = "https://geocoding-api.open-meteo.com/";

    // ========== SOIL DATA ==========
    public static final String SOIL_GRIDS_BASE = "https://rest.isric.org/soilgrids/v2.0/";

    // ========== LOCAL ML MODELS ==========
    public static final String TFLITE_SOIL_CLASSIFIER = "soil_classifier.tflite";

    // ========== NETWORK TIMEOUTS (seconds) ==========
    public static final int CONNECT_TIMEOUT = 60;
    public static final int READ_TIMEOUT = 60;
    public static final int WRITE_TIMEOUT = 60;
}
