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
        sharedPreferences.edit()
                .putFloat(KEY_LAST_LAT, (float) lat)
                .putFloat(KEY_LAST_LON, (float) lon)
                .putString(KEY_LAST_LOC_NAME, name)
                .apply();
    }

    // Returns array [lat, lon] or null if not set
    public double[] getLastLocation() {
        if (!sharedPreferences.contains(KEY_LAST_LAT)) return null;
        float lat = sharedPreferences.getFloat(KEY_LAST_LAT, 0);
        float lon = sharedPreferences.getFloat(KEY_LAST_LON, 0);
        return new double[]{lat, lon};
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
}