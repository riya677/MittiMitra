package com.mittimitra.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wrapper for Firebase Analytics with predefined events for Mitti Mitra.
 * Provides type-safe event logging and consistent parameter naming.
 */
public class AnalyticsHelper {

    private static FirebaseAnalytics analytics;

    // ========== EVENT NAMES ==========
    public static final String EVENT_SCAN_STARTED = "scan_started";
    public static final String EVENT_SCAN_COMPLETED = "scan_completed";
    public static final String EVENT_PLANT_SCAN_STARTED = "plant_scan_started";
    public static final String EVENT_PLANT_SCAN_COMPLETED = "plant_scan_completed";
    public static final String EVENT_CHAT_MESSAGE_SENT = "chat_message_sent";
    public static final String EVENT_REPORT_GENERATED = "report_generated";
    public static final String EVENT_REPORT_SHARED = "report_shared";
    public static final String EVENT_SCHEME_VIEWED = "scheme_viewed";
    public static final String EVENT_WEATHER_CHECKED = "weather_checked";
    public static final String EVENT_LANGUAGE_CHANGED = "language_changed";

    // ========== PARAMETER NAMES ==========
    public static final String PARAM_SOIL_TYPE = "soil_type";
    public static final String PARAM_DISEASE_TYPE = "disease_type";
    public static final String PARAM_LANGUAGE = "language";
    public static final String PARAM_SCHEME_NAME = "scheme_name";
    public static final String PARAM_SOURCE = "source";

    private AnalyticsHelper() {
        // Prevent instantiation
    }

    /**
     * Initialize analytics. Call this from Application class.
     */
    public static void init(@NonNull Context context) {
        analytics = FirebaseAnalytics.getInstance(context);
    }

    /**
     * Log a simple event without parameters.
     */
    public static void logEvent(@NonNull String eventName) {
        if (analytics != null) {
            analytics.logEvent(eventName, null);
        }
    }

    /**
     * Log an event with a bundle of parameters.
     */
    public static void logEvent(@NonNull String eventName, @Nullable Bundle params) {
        if (analytics != null) {
            analytics.logEvent(eventName, params);
        }
    }

    /**
     * Log a scan completion event with soil type.
     */
    public static void logScanCompleted(@NonNull String soilType) {
        Bundle params = new Bundle();
        params.putString(PARAM_SOIL_TYPE, soilType);
        logEvent(EVENT_SCAN_COMPLETED, params);
    }

    /**
     * Log a plant scan completion event with disease type.
     */
    public static void logPlantScanCompleted(@NonNull String diseaseType) {
        Bundle params = new Bundle();
        params.putString(PARAM_DISEASE_TYPE, diseaseType);
        logEvent(EVENT_PLANT_SCAN_COMPLETED, params);
    }

    /**
     * Log a language change event.
     */
    public static void logLanguageChanged(@NonNull String newLanguage) {
        Bundle params = new Bundle();
        params.putString(PARAM_LANGUAGE, newLanguage);
        logEvent(EVENT_LANGUAGE_CHANGED, params);
    }

    /**
     * Log a scheme view event.
     */
    public static void logSchemeViewed(@NonNull String schemeName) {
        Bundle params = new Bundle();
        params.putString(PARAM_SCHEME_NAME, schemeName);
        logEvent(EVENT_SCHEME_VIEWED, params);
    }

    /**
     * Log screen view (use for manual tracking if needed).
     */
    public static void logScreenView(@NonNull String screenName, @NonNull String screenClass) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
        }
    }

    /**
     * Set user property for segmentation.
     */
    public static void setUserProperty(@NonNull String name, @Nullable String value) {
        if (analytics != null) {
            analytics.setUserProperty(name, value);
        }
    }

    /**
     * Set user ID for cross-device tracking.
     */
    public static void setUserId(@Nullable String userId) {
        if (analytics != null) {
            analytics.setUserId(userId);
        }
    }
}
