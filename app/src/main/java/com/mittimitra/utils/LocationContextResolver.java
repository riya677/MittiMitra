package com.mittimitra.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mittimitra.AppPreferences;

public final class LocationContextResolver {

    private static final String PREF_SCAN_CACHE = "scan_cache";
    private static final String PREF_USER_LOCATION_CACHE = "user_location_cache";
    private static final String KEY_SCAN_LOCATION_NAME = "loc";
    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LON = "last_lon";

    private LocationContextResolver() {
    }

    @Nullable
    public static String getBestLocationName(@NonNull Context context) {
        SharedPreferences scanCache = context.getSharedPreferences(PREF_SCAN_CACHE, Context.MODE_PRIVATE);
        String fromScan = scanCache.getString(KEY_SCAN_LOCATION_NAME, null);
        if (fromScan != null && !fromScan.trim().isEmpty()) {
            return fromScan;
        }

        String fromApp = new AppPreferences(context).getLastLocationName();
        if (fromApp != null && !fromApp.trim().isEmpty() && !"Unknown Location".equalsIgnoreCase(fromApp)) {
            return fromApp;
        }
        return null;
    }

    @Nullable
    public static double[] getBestCoordinates(@NonNull Context context) {
        SharedPreferences cache = context.getSharedPreferences(PREF_USER_LOCATION_CACHE, Context.MODE_PRIVATE);
        String latRaw = cache.getString(KEY_LAST_LAT, null);
        String lonRaw = cache.getString(KEY_LAST_LON, null);
        if (latRaw != null && lonRaw != null) {
            try {
                double lat = Double.parseDouble(latRaw);
                double lon = Double.parseDouble(lonRaw);
                if (!(lat == 0.0 && lon == 0.0)) {
                    return new double[]{lat, lon};
                }
            } catch (NumberFormatException ignored) {
                // fall through to AppPreferences fallback
            }
        }

        return new AppPreferences(context).getLastLocation();
    }

    public static void saveCoordinates(@NonNull Context context, double lat, double lon) {
        context.getSharedPreferences(PREF_USER_LOCATION_CACHE, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_LAT, String.valueOf(lat))
                .putString(KEY_LAST_LON, String.valueOf(lon))
                .apply();
    }

    public static void saveResolvedLocation(@NonNull Context context, double lat, double lon, @Nullable String locationName) {
        saveCoordinates(context, lat, lon);
        String safeName = locationName == null ? "" : locationName.trim();

        context.getSharedPreferences(PREF_SCAN_CACHE, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SCAN_LOCATION_NAME, safeName)
                .apply();

        new AppPreferences(context).setLastLocation(lat, lon, safeName.isEmpty() ? "Unknown Location" : safeName);
    }
}
