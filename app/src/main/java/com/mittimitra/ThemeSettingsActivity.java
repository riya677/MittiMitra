package com.mittimitra;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ThemeSettingsActivity extends BaseActivity {

    private AppPreferences appPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_settings);

        appPreferences = new AppPreferences(this);

        Toolbar toolbar = findViewById(R.id.theme_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RadioGroup rgThemes = findViewById(R.id.rg_themes);
        MaterialRadioButton rbLight = findViewById(R.id.rb_theme_light);
        MaterialRadioButton rbDark = findViewById(R.id.rb_theme_dark);
        MaterialRadioButton rbSystem = findViewById(R.id.rb_theme_system);

        // Set the currently selected theme (null-safe: default to "system")
        String currentTheme = appPreferences.getTheme();
        if ("light".equals(currentTheme)) {
            rbLight.setChecked(true);
        } else if ("dark".equals(currentTheme)) {
            rbDark.setChecked(true);
        } else {
            rbSystem.setChecked(true);
        }

        // Auto dark mode switch
        SwitchMaterial switchAutoDark = findViewById(R.id.switch_auto_dark);
        if (switchAutoDark != null) {
            switchAutoDark.setChecked(appPreferences.isAutoDarkMode());
            // Disable manual theme picker when auto mode is active
            rgThemes.setEnabled(!appPreferences.isAutoDarkMode());
            rbLight.setEnabled(!appPreferences.isAutoDarkMode());
            rbDark.setEnabled(!appPreferences.isAutoDarkMode());
            rbSystem.setEnabled(!appPreferences.isAutoDarkMode());

            switchAutoDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPreferences.setAutoDarkMode(isChecked);
                rgThemes.setEnabled(!isChecked);
                rbLight.setEnabled(!isChecked);
                rbDark.setEnabled(!isChecked);
                rbSystem.setEnabled(!isChecked);
                // Immediately re-apply theme
                applyThemeFromPrefs(appPreferences);
            });
        }

        rgThemes.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_theme_light) {
                appPreferences.setTheme("light");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.rb_theme_dark) {
                appPreferences.setTheme("dark");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (checkedId == R.id.rb_theme_system) {
                appPreferences.setTheme("system");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });
    }

    /** Re-applies the current theme (including auto dark mode logic). */
    private void applyThemeFromPrefs(AppPreferences prefs) {
        if (prefs.isAutoDarkMode()) {
            long sunrise = prefs.getLastSunrise();
            long sunset = prefs.getLastSunset();
            long now = System.currentTimeMillis();
            if (sunrise > 0 && sunset > 0) {
                boolean isNight = (now < sunrise || now >= sunset);
                AppCompatDelegate.setDefaultNightMode(isNight
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        } else {
            String theme = prefs.getTheme();
            switch (theme) {
                case "light": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
                case "dark":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
                default:      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}