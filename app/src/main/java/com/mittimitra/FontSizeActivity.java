package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.slider.Slider;

public class FontSizeActivity extends BaseActivity {

    private AppPreferences appPreferences;
    private float currentScale = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_size);

        appPreferences = new AppPreferences(this);
        currentScale = appPreferences.getFontScale();

        Toolbar toolbar = findViewById(R.id.font_size_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Slider slider = findViewById(R.id.slider_font_size);
        TextView tvSample = findViewById(R.id.tv_font_sample);

        slider.setValue(currentScale);

        float baseSizeSp = 18f;
        tvSample.setTextSize(baseSizeSp * currentScale);

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            tvSample.setTextSize(baseSizeSp * value);
            currentScale = value;
        });

        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                saveAndRestart();
            }
        });
    }

    private void saveAndRestart() {
        appPreferences.setFontScale(currentScale);

        // --- UPDATED: Restart the app by launching HomeActivity ---
        Intent i = new Intent(this, HomeActivity.class);
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