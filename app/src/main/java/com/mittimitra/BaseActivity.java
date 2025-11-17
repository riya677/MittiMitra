package com.mittimitra;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- UPDATED: Apply theme and font BEFORE super.onCreate() ---
        AppPreferences prefs = new AppPreferences(this);
        applyAppTheme(prefs.getTheme());
        applyAppFont(prefs.isDyslexicFontEnabled());

        super.onCreate(savedInstanceState);

        // Apply language (can be done after super.onCreate)
        applyAppLanguage();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // This is the magic for applying font size
        AppPreferences prefs = new AppPreferences(newBase);
        Context contextWithFont = applyFontScale(newBase, prefs.getFontScale());
        super.attachBaseContext(contextWithFont);
    }

    private void applyAppLanguage() {
        AppPreferences prefs = new AppPreferences(this);
        String lang = prefs.getLanguage();
        if (lang != null) {
            LocaleListCompat locales = LocaleListCompat.forLanguageTags(lang);
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }

    private Context applyFontScale(Context context, float fontScale) {
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.fontScale = fontScale;
        return context.createConfigurationContext(config);
    }

    private void applyAppTheme(String theme) {
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default: // "system"
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // --- NEW: Apply Font Theme ---
    private void applyAppFont(boolean isDyslexicEnabled) {
        if (isDyslexicEnabled) {
            setTheme(R.style.Theme_MittiMitra_Dyslexic);
        } else {
            setTheme(R.style.Theme_MittiMitra);
        }
    }
}