package com.mittimitra.utils;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Helper class for logging errors to Firebase Crashlytics.
 * Use for non-fatal exceptions and custom logging.
 */
public class CrashlyticsHelper {
    
    private static final String TAG = "CrashlyticsHelper";
    
    /**
     * Log a non-fatal exception to Crashlytics.
     * Use this for handled errors that you want to track.
     */
    public static void logException(@NonNull Throwable throwable) {
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable);
            Log.e(TAG, "Exception logged to Crashlytics", throwable);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log to Crashlytics", e);
        }
    }

    /**
     * Log a non-fatal exception with custom message.
     */
    public static void logException(@NonNull String message, @NonNull Throwable throwable) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.log(message);
            crashlytics.recordException(throwable);
            Log.e(TAG, message, throwable);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log to Crashlytics", e);
        }
    }

    /**
     * Log a custom message/breadcrumb.
     */
    public static void log(@NonNull String message) {
        try {
            FirebaseCrashlytics.getInstance().log(message);
            Log.d(TAG, message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log to Crashlytics", e);
        }
    }

    /**
     * Set user identifier for crash reports.
     */
    public static void setUserId(String userId) {
        try {
            if (userId != null && !userId.isEmpty()) {
                FirebaseCrashlytics.getInstance().setUserId(userId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set user ID in Crashlytics", e);
        }
    }

    /**
     * Set a custom key-value pair for crash reports.
     */
    public static void setCustomKey(String key, String value) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key in Crashlytics", e);
        }
    }

    /**
     * Set custom key with int value.
     */
    public static void setCustomKey(String key, int value) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key in Crashlytics", e);
        }
    }

    /**
     * Set custom key with boolean value.
     */
    public static void setCustomKey(String key, boolean value) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key in Crashlytics", e);
        }
    }

    /**
     * Log API call result for debugging.
     */
    public static void logApiCall(String endpoint, boolean success, String errorMessage) {
        try {
            Bundle params = new Bundle();
            params.putString("endpoint", endpoint);
            params.putBoolean("success", success);
            if (errorMessage != null) {
                params.putString("error", errorMessage);
            }
            
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.log("API: " + endpoint + " | Success: " + success + 
                (errorMessage != null ? " | Error: " + errorMessage : ""));
        } catch (Exception e) {
            Log.e(TAG, "Failed to log API call", e);
        }
    }
}
