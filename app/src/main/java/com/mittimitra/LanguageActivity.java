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

        // Existing Views
        MaterialRadioButton rbSystem = findViewById(R.id.rb_system_default);
        MaterialRadioButton rbEnglish = findViewById(R.id.rb_english);
        MaterialRadioButton rbHindi = findViewById(R.id.rb_hindi);
        MaterialRadioButton rbTamil = findViewById(R.id.rb_tamil);
        MaterialRadioButton rbMalayalam = findViewById(R.id.rb_malayalam);
        MaterialRadioButton rbTelugu = findViewById(R.id.rb_telugu);

        // New Views
        MaterialRadioButton rbKannada = findViewById(R.id.rb_kannada);
        MaterialRadioButton rbMarathi = findViewById(R.id.rb_marathi);
        MaterialRadioButton rbBengali = findViewById(R.id.rb_bengali);
        MaterialRadioButton rbGujarati = findViewById(R.id.rb_gujarati);
        MaterialRadioButton rbPunjabi = findViewById(R.id.rb_punjabi);

        // Set the currently selected language
        String currentLang = appPreferences.getLanguage();
        if (currentLang == null) {
            rbSystem.setChecked(true);
        } else {
            switch (currentLang) {
                case "en": rbEnglish.setChecked(true); break;
                case "hi": rbHindi.setChecked(true); break;
                case "ta": rbTamil.setChecked(true); break;
                case "ml": rbMalayalam.setChecked(true); break;
                case "te": rbTelugu.setChecked(true); break;
                // New Cases
                case "kn": rbKannada.setChecked(true); break;
                case "mr": rbMarathi.setChecked(true); break;
                case "bn": rbBengali.setChecked(true); break;
                case "gu": rbGujarati.setChecked(true); break;
                case "pa": rbPunjabi.setChecked(true); break;
            }
        }

        rgLanguages.setOnCheckedChangeListener((group, checkedId) -> {
            String langCode = null;

            if (checkedId == R.id.rb_system_default) langCode = null;
            else if (checkedId == R.id.rb_english) langCode = "en";
            else if (checkedId == R.id.rb_hindi) langCode = "hi";
            else if (checkedId == R.id.rb_tamil) langCode = "ta";
            else if (checkedId == R.id.rb_malayalam) langCode = "ml";
            else if (checkedId == R.id.rb_telugu) langCode = "te";
            else if (checkedId == R.id.rb_kannada) langCode = "kn";
            else if (checkedId == R.id.rb_marathi) langCode = "mr";
            else if (checkedId == R.id.rb_bengali) langCode = "bn";
            else if (checkedId == R.id.rb_gujarati) langCode = "gu";
            else if (checkedId == R.id.rb_punjabi) langCode = "pa";

            appPreferences.setLanguage(langCode);

            if (langCode == null) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode));
            }

            // Relaunch the app to apply changes
            restartApp();
        });
    }

    private void restartApp() {
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