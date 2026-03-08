package com.mittimitra;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "MittiMitraPrefs";
    private static final String KEY_FONT_SCALE = "font_scale";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_DYSLEXIC_FONT = "dyslexic_font";
    private static final String KEY_THEME = "app_theme";
    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LON = "last_lon";
    private static final String KEY_LAST_LOC_NAME = "last_loc_name";
    
    // Session Context Keys
    private static final String KEY_LAST_CROP = "last_crop";
    private static final String KEY_LAST_SOIL_TYPE = "last_soil_type";
    private static final String KEY_LAST_FIELD_SIZE = "last_field_size";

    // Auto dark mode (sunrise/sunset based)
    private static final String KEY_AUTO_DARK_MODE = "auto_dark_mode";
    private static final String KEY_LAST_SUNRISE = "last_sunrise";
    private static final String KEY_LAST_SUNSET = "last_sunset";

    // Onboarding
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";

    private final SharedPreferences sharedPreferences;

    public AppPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setFontScale(float scale) {
        sharedPreferences.edit().putFloat(KEY_FONT_SCALE, scale).apply();
    }
    public float getFontScale() { return sharedPreferences.getFloat(KEY_FONT_SCALE, 1.0f); }

    public void setDyslexicFontEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DYSLEXIC_FONT, enabled).apply();
    }
    public boolean isDyslexicFontEnabled() { return sharedPreferences.getBoolean(KEY_DYSLEXIC_FONT, false); }

    public void setLanguage(String lang) { sharedPreferences.edit().putString(KEY_APP_LANGUAGE, lang).apply(); }
    public String getLanguage() { return sharedPreferences.getString(KEY_APP_LANGUAGE, null); }

    public void setTheme(String theme) { sharedPreferences.edit().putString(KEY_THEME, theme).apply(); }
    public String getTheme() { return sharedPreferences.getString(KEY_THEME, "system"); }

    public void setLastLocation(double lat, double lon, String name) {
        // Store as String to preserve full double precision (float truncates decimals)
        sharedPreferences.edit()
                .putString(KEY_LAST_LAT, String.valueOf(lat))
                .putString(KEY_LAST_LON, String.valueOf(lon))
                .putString(KEY_LAST_LOC_NAME, name)
                .apply();
    }

    // Returns array [lat, lon] or null if not set.
    // Handles migration from legacy Float storage (pre-fix #10) to String storage.
    public double[] getLastLocation() {
        try {
            String latStr = sharedPreferences.getString(KEY_LAST_LAT, null);
            String lonStr = sharedPreferences.getString(KEY_LAST_LON, null);
            if (latStr == null || lonStr == null) return null;
            return new double[]{Double.parseDouble(latStr), Double.parseDouble(lonStr)};
        } catch (ClassCastException e) {
            // Legacy data stored as Float — migrate to String and return value
            float lat = sharedPreferences.getFloat(KEY_LAST_LAT, Float.MIN_VALUE);
            float lon = sharedPreferences.getFloat(KEY_LAST_LON, Float.MIN_VALUE);
            if (lat == Float.MIN_VALUE || lon == Float.MIN_VALUE) return null;
            setLastLocation(lat, lon, getLastLocationName());
            return new double[]{lat, lon};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLastLocationName() {
        return sharedPreferences.getString(KEY_LAST_LOC_NAME, "Unknown Location");
    }

    // --- Session Data Methods ---
    public void setLastCrop(String crop) { sharedPreferences.edit().putString(KEY_LAST_CROP, crop).apply(); }
    public String getLastCrop() { return sharedPreferences.getString(KEY_LAST_CROP, null); }

    public void setLastSoilType(String soilType) { sharedPreferences.edit().putString(KEY_LAST_SOIL_TYPE, soilType).apply(); }
    public String getLastSoilType() { return sharedPreferences.getString(KEY_LAST_SOIL_TYPE, null); }

    public void setLastFieldSize(String size) { sharedPreferences.edit().putString(KEY_LAST_FIELD_SIZE, size).apply(); }
    public String getLastFieldSize() { return sharedPreferences.getString(KEY_LAST_FIELD_SIZE, ""); }

    // --- Auto Dark Mode ---
    public void setAutoDarkMode(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_DARK_MODE, enabled).apply();
    }
    public boolean isAutoDarkMode() {
        return sharedPreferences.getBoolean(KEY_AUTO_DARK_MODE, false);
    }

    public void setLastSunrise(long epochMillis) {
        sharedPreferences.edit().putLong(KEY_LAST_SUNRISE, epochMillis).apply();
    }
    public long getLastSunrise() {
        return sharedPreferences.getLong(KEY_LAST_SUNRISE, 0L);
    }

    public void setLastSunset(long epochMillis) {
        sharedPreferences.edit().putLong(KEY_LAST_SUNSET, epochMillis).apply();
    }
    public long getLastSunset() {
        return sharedPreferences.getLong(KEY_LAST_SUNSET, 0L);
    }

    // --- Onboarding ---
    public boolean hasSeenOnboarding() {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false);
    }
    public void setOnboardingComplete() {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }
}