package com.mittimitra;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.radiobutton.MaterialRadioButton;

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

        // Set the currently selected theme
        String currentTheme = appPreferences.getTheme();
        if (currentTheme.equals("light")) {
            rbLight.setChecked(true);
        } else if (currentTheme.equals("dark")) {
            rbDark.setChecked(true);
        } else {
            rbSystem.setChecked(true);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}