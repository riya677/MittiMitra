package com.mittimitra.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Centralized error handling utility.
 * Provides consistent error logging and user feedback across the app.
 */
public final class ErrorHandler {

    private static final String TAG = "MittiMitra";

    private ErrorHandler() {
        // Prevent instantiation
    }

    /**
     * Log an error with the class name as a sub-tag.
     */
    public static void logError(@NonNull String className, @NonNull String message, @Nullable Throwable throwable) {
        String fullTag = TAG + "." + className;
        if (throwable != null) {
            Log.e(fullTag, message, throwable);
        } else {
            Log.e(fullTag, message);
        }
    }

    /**
     * Log an error without throwable.
     */
    public static void logError(@NonNull String className, @NonNull String message) {
        logError(className, message, null);
    }

    /**
     * Show a Toast error message to the user.
     */
    public static void showToast(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a Toast error message using string resource.
     */
    public static void showToast(@NonNull Context context, @StringRes int messageResId) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a Snackbar error message with optional action.
     */
    public static void showSnackbar(@NonNull View view, @NonNull String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Show a Snackbar with a retry action.
     */
    public static void showSnackbarWithRetry(@NonNull View view, @NonNull String message, 
                                              @NonNull Runnable retryAction) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> retryAction.run())
                .show();
    }

    /**
     * Handle network errors with appropriate user feedback.
     */
    public static void handleNetworkError(@NonNull Context context, @NonNull String className, 
                                          @Nullable Throwable throwable) {
        String userMessage = "Network error. Please check your connection.";
        logError(className, "Network request failed", throwable);
        showToast(context, userMessage);
    }

    /**
     * Handle API errors with error code.
     */
    public static void handleApiError(@NonNull Context context, @NonNull String className, 
                                       int errorCode, @Nullable String errorBody) {
        String userMessage;
        switch (errorCode) {
            case 401:
                userMessage = "Authentication failed. Please login again.";
                break;
            case 403:
                userMessage = "Access denied.";
                break;
            case 404:
                userMessage = "Resource not found.";
                break;
            case 429:
                userMessage = "Too many requests. Please try again later.";
                break;
            case 500:
            case 502:
            case 503:
                userMessage = "Server error. Please try again later.";
                break;
            default:
                userMessage = "Request failed (Error " + errorCode + ")";
        }
        logError(className, "API Error " + errorCode + ": " + errorBody);
        showToast(context, userMessage);
    }

    /**
     * Handle parsing errors gracefully.
     */
    public static void handleParseError(@NonNull Context context, @NonNull String className, 
                                         @NonNull Exception e) {
        logError(className, "Failed to parse response", e);
        showToast(context, "Unable to process response");
    }
}
