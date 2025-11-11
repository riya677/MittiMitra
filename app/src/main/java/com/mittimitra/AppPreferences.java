package com.mittimitra;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREF_NAME = "MittiMitraPrefs";
    private static final String KEY_FONT_SCALE = "font_scale";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_APP_THEME = "app_theme";
    private static final String KEY_DYSLEXIC_FONT = "dyslexic_font"; // NEW

    private final SharedPreferences sharedPreferences;

    public AppPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- Font Scale ---
    public void setFontScale(float scale) {
        sharedPreferences.edit().putFloat(KEY_FONT_SCALE, scale).apply();
    }
    public float getFontScale() {
        return sharedPreferences.getFloat(KEY_FONT_SCALE, 1.0f);
    }

    // --- Language ---
    public void setLanguage(String languageCode) {
        sharedPreferences.edit().putString(KEY_APP_LANGUAGE, languageCode).apply();
    }
    public String getLanguage() {
        return sharedPreferences.getString(KEY_APP_LANGUAGE, null);
    }

    // --- App Theme ---
    public void setTheme(String theme) {
        sharedPreferences.edit().putString(KEY_APP_THEME, theme).apply();
    }
    public String getTheme() {
        return sharedPreferences.getString(KEY_APP_THEME, "system");
    }

    // --- NEW: Dyslexic Font ---
    public void setDyslexicFontEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DYSLEXIC_FONT, enabled).apply();
    }
    public boolean isDyslexicFontEnabled() {
        return sharedPreferences.getBoolean(KEY_DYSLEXIC_FONT, false); // Default is false
    }
}