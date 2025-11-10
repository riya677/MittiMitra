package com.mittimitra;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppPreferences prefs = new AppPreferences(this);
        // Apply theme *before* setContentView
        applyAppTheme(prefs.getTheme());
        // Apply language
        applyAppLanguage();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
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

    // --- NEW: Apply Theme ---
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
}