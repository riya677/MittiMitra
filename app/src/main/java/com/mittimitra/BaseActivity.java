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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences prefs = new AppPreferences(this);

        // Apply Theme Logic
        applyAppTheme(prefs.getTheme());

        // Accessibility Themes
        if (prefs.isHighContrastEnabled()) {
            setTheme(R.style.Theme_MittiMitra_HighContrast);
        } else if (prefs.isDyslexicFontEnabled()) {
            setTheme(R.style.Theme_MittiMitra_Dyslexic);
        } else {
            setTheme(R.style.Theme_MittiMitra);
        }

        applyAppLanguage();
        super.onCreate(savedInstanceState);
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
}