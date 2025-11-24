package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class AccessibilityActivity extends BaseActivity {

    private AppPreferences appPreferences;
    private Slider slider;
    private SwitchMaterial fontSwitch;
    private TextView tvSample;

    private float currentScale = 1.0f;
    private boolean currentDyslexicEnabled = false;
    private boolean hasChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessibility);

        appPreferences = new AppPreferences(this);

        // Load preferences
        currentScale = appPreferences.getFontScale();
        currentDyslexicEnabled = appPreferences.isDyslexicFontEnabled();

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.accessibility_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.setting_accessibility);
        }

        // Bind Views
        slider = findViewById(R.id.slider_font_size);
        tvSample = findViewById(R.id.tv_font_sample);
        fontSwitch = findViewById(R.id.switch_dyslexic_font);

        // Clamp Slider Value
        if (currentScale < 0.8f) currentScale = 0.8f;
        if (currentScale > 1.5f) currentScale = 1.5f;

        slider.setValue(currentScale);
        fontSwitch.setChecked(currentDyslexicEnabled);

        updateSampleText(currentScale);

        // Listeners
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            if (fromUser) {
                updateSampleText(value);
                currentScale = value;
                hasChanges = true;
            }
        });

        fontSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentDyslexicEnabled = isChecked;
            hasChanges = true;
            saveAndRestart();
        });
    }

    private void updateSampleText(float scale) {
        float baseSizeSp = 18f;
        tvSample.setTextSize(baseSizeSp * scale);
    }

    private void saveAndRestart() {
        appPreferences.setFontScale(currentScale);
        appPreferences.setDyslexicFontEnabled(currentDyslexicEnabled);

        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (hasChanges) saveAndRestart();
            else finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (hasChanges) saveAndRestart();
        else super.onBackPressed();
    }
}