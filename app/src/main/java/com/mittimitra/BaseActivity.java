package com.mittimitra;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    // Variables to store the state when the activity is created
    private String lastTheme;
    private boolean lastDyslexic;
    private float lastFontScale;
    private String lastLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences prefs = new AppPreferences(this);

        // 1. Capture the settings at the moment of creation
        lastTheme = prefs.getTheme();
        lastDyslexic = prefs.isDyslexicFontEnabled();
        lastFontScale = prefs.getFontScale();
        lastLanguage = prefs.getLanguage();

        // 2. Apply Theme Logic (Night Mode)
        applyAppTheme(lastTheme);

        // 3. Apply Accessibility Theme (Dyslexic Font)
        if (lastDyslexic) {
            setTheme(R.style.Theme_MittiMitra_Dyslexic);
        } else {
            setTheme(R.style.Theme_MittiMitra);
        }

        applyAppLanguage();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 4. Check if settings changed while we were away (e.g., in Settings screen)
        AppPreferences prefs = new AppPreferences(this);

        boolean themeChanged = !prefs.getTheme().equals(lastTheme);
        boolean dyslexicChanged = prefs.isDyslexicFontEnabled() != lastDyslexic;
        boolean fontChanged = prefs.getFontScale() != lastFontScale;

        String currentLang = prefs.getLanguage();
        boolean langChanged = (lastLanguage == null && currentLang != null) ||
                (lastLanguage != null && !lastLanguage.equals(currentLang));

        // 5. If anything changed, RECREATE this activity to apply the new look
        if (themeChanged || dyslexicChanged || fontChanged || langChanged) {
            recreate();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        AppPreferences prefs = new AppPreferences(newBase);
        Context context = applyConfig(newBase, prefs.getFontScale(), prefs.getLanguage());
        super.attachBaseContext(context);
    }

    private void applyAppLanguage() {
        AppPreferences prefs = new AppPreferences(this);
        String lang = prefs.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            LocaleListCompat locales = LocaleListCompat.forLanguageTags(lang);
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }

    private Context applyConfig(Context context, float fontScale, String languageCode) {
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.fontScale = fontScale;
        if (languageCode != null && !languageCode.isEmpty()) {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            config.setLocale(locale);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(new LocaleList(locale));
            }
        }
        return context.createConfigurationContext(config);
    }

    private void applyAppTheme(String theme) {
        switch (theme) {
            case "light": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case "dark": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    /**
     * Apply enter transition animation when activity starts.
     * Call this after setContentView() for smooth slide-in effect.
     */
    protected void applyEnterTransition() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Override finish to apply exit transition animation.
     */
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /**
     * Helper method to start activity with transition animation.
     */
    protected void startActivityWithTransition(android.content.Intent intent) {
        startActivity(intent);
        applyEnterTransition();
    }
}