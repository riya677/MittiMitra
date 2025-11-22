package com.mittimitra;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsActivity extends BaseActivity {

    private SharedPreferences prefs;
    private static final String PREF_NOTIF = "notif_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        Toolbar toolbar = findViewById(R.id.notif_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences(PREF_NOTIF, MODE_PRIVATE);

        SwitchMaterial swWeather = findViewById(R.id.switch_weather);
        SwitchMaterial swMandi = findViewById(R.id.switch_mandi);
        SwitchMaterial swSchemes = findViewById(R.id.switch_schemes);

        // Load saved state (Default is true)
        swWeather.setChecked(prefs.getBoolean("weather", true));
        swMandi.setChecked(prefs.getBoolean("mandi", true));
        swSchemes.setChecked(prefs.getBoolean("schemes", true));

        // Save state on change
        swWeather.setOnCheckedChangeListener((v, isChecked) ->
                prefs.edit().putBoolean("weather", isChecked).apply());

        swMandi.setOnCheckedChangeListener((v, isChecked) ->
                prefs.edit().putBoolean("mandi", isChecked).apply());

        swSchemes.setOnCheckedChangeListener((v, isChecked) ->
                prefs.edit().putBoolean("schemes", isChecked).apply());
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