package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.radiobutton.MaterialRadioButton;

public class LanguageActivity extends BaseActivity {

    private AppPreferences appPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        appPreferences = new AppPreferences(this);

        Toolbar toolbar = findViewById(R.id.language_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RadioGroup rgLanguages = findViewById(R.id.rg_languages);
        MaterialRadioButton rbSystem = findViewById(R.id.rb_system_default);
        MaterialRadioButton rbEnglish = findViewById(R.id.rb_english);
        MaterialRadioButton rbHindi = findViewById(R.id.rb_hindi);
        MaterialRadioButton rbTamil = findViewById(R.id.rb_tamil);
        MaterialRadioButton rbMalayalam = findViewById(R.id.rb_malayalam);
        MaterialRadioButton rbTelugu = findViewById(R.id.rb_telugu); // NEW

        // Set the currently selected language
        String currentLang = appPreferences.getLanguage();
        if (currentLang == null) {
            rbSystem.setChecked(true);
        } else if (currentLang.equals("en")) {
            rbEnglish.setChecked(true);
        } else if (currentLang.equals("hi")) {
            rbHindi.setChecked(true);
        } else if (currentLang.equals("ta")) {
            rbTamil.setChecked(true);
        } else if (currentLang.equals("ml")) {
            rbMalayalam.setChecked(true);
        } else if (currentLang.equals("te")) { // NEW
            rbTelugu.setChecked(true);
        }

        rgLanguages.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_system_default) {
                appPreferences.setLanguage(null);
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            } else if (checkedId == R.id.rb_english) {
                appPreferences.setLanguage("en");
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
            } else if (checkedId == R.id.rb_hindi) {
                appPreferences.setLanguage("hi");
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("hi"));
            } else if (checkedId == R.id.rb_tamil) {
                appPreferences.setLanguage("ta");
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ta"));
            } else if (checkedId == R.id.rb_malayalam) {
                appPreferences.setLanguage("ml");
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ml"));
            } else if (checkedId == R.id.rb_telugu) { // NEW
                appPreferences.setLanguage("te");
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("te"));
            }

            // Relaunch the app to apply changes
            restartApp();
        });
    }

    private void restartApp() {
        // --- UPDATED: Restart the app by launching SplashActivity ---
        Intent i = new Intent(this, SplashActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
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