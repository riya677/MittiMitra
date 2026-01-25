package com.mittimitra.config;

/**
 * Application-wide constants.
 * Notification channels, preference keys, and other configuration values.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // ========== NOTIFICATION ==========
    public static final String NOTIFICATION_CHANNEL_ID = "mitti_mitra_alerts";
    public static final String NOTIFICATION_CHANNEL_NAME = "Farming Alerts";

    // ========== SOIL CLASSIFICATION LABELS ==========
    public static final String[] SOIL_LABELS = {
            "Alluvial Soil",
            "Black Soil",
            "Cinder Soil",
            "Clay Soil",
            "Laterite Soil",
            "Peat Soil",
            "Red Soil",
            "Sandy Soil",
            "Yellow Soil"
    };

    // ========== SHARED PREFERENCES NAMES ==========
    public static final String PREF_MITTI_MITRA = "MittiMitraPrefs";
    public static final String PREF_SESSION = "MittiMitraSession";
    public static final String PREF_AUTH = "auth_prefs";
    public static final String PREF_SCAN_CACHE = "scan_cache";

    // ========== REQUEST CODES ==========
    public static final int REQUEST_LOCATION_PERMISSION = 100;
    public static final int REQUEST_CAMERA_PERMISSION = 101;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 102;
    public static final int REQUEST_AUDIO_PERMISSION = 103;
    public static final int REQUEST_STORAGE_PERMISSION = 104;

    // ========== WORK MANAGER TAGS ==========
    public static final String WORK_TAG_WEATHER = "SmartWeatherWork";
    public static final int WORK_INTERVAL_HOURS = 12;

    // ========== IMAGE PROCESSING ==========
    public static final int IMAGE_CLASSIFICATION_SIZE = 224;
    public static final int JPEG_QUALITY_HIGH = 90;
    public static final int JPEG_QUALITY_MEDIUM = 70;
}
