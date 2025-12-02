package com.mittimitra;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsActivity extends BaseActivity {

    private SwitchMaterial switchWeather;
    private SwitchMaterial switchMandi;
    private SwitchMaterial switchSchemes;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        // Initialize SharedPreferences to save data
        prefs = getSharedPreferences("MittiMitra_Notifications", Context.MODE_PRIVATE);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.notif_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Title handled in XML
        }

        // Bind Views
        switchWeather = findViewById(R.id.switch_weather);
        switchMandi = findViewById(R.id.switch_mandi);
        switchSchemes = findViewById(R.id.switch_schemes);

        // Load Saved States (Default to true/ON)
        switchWeather.setChecked(prefs.getBoolean("notif_weather", true));
        switchMandi.setChecked(prefs.getBoolean("notif_mandi", true));
        switchSchemes.setChecked(prefs.getBoolean("notif_schemes", true));

        // Set Listeners to Save Data on Change
        setupSwitchListener(switchWeather, "notif_weather", "Weather Alerts");
        setupSwitchListener(switchMandi, "notif_mandi", "Mandi Prices");
        setupSwitchListener(switchSchemes, "notif_schemes", "Govt Schemes");

        // --- DEMO FEATURE: TEST NOTIFICATION BUTTON ---
        // Find the button (ensure ID matches XML)
        if (findViewById(R.id.btn_test_notification) != null) {
            findViewById(R.id.btn_test_notification).setOnClickListener(v -> showDemoNotification());
        }
    }

    private void setupSwitchListener(SwitchMaterial switchView, String key, String name) {
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the new state
            prefs.edit().putBoolean(key, isChecked).apply();
        });
    }

    private void showDemoNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "mitti_mitra_alerts";

        // Create Channel (Required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Farming Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Critical alerts for weather and prices");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with R.drawable.ic_notification if you have one
                .setContentTitle("Mitti Mitra Alert")
                .setContentText("Heavy rain expected in your district tomorrow. Please cover your harvest!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show the notification
        manager.notify(999, builder.build());
        Toast.makeText(this, "Test Notification Sent!", Toast.LENGTH_SHORT).show();
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