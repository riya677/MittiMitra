package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;

public class HomeActivity extends BaseActivity {

    private TextView tvGreeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // --- Find Views ---
        tvGreeting = findViewById(R.id.tv_greeting);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        MaterialCardView btnTip = findViewById(R.id.btn_tip);
        MaterialCardView btnRecommendation = findViewById(R.id.btn_recommendation);
        MaterialCardView btnHistory = findViewById(R.id.btn_history);
        MaterialCardView btnScan = findViewById(R.id.btn_scan);
        MaterialCardView btnDocuments = findViewById(R.id.btn_documents);

        tvGreeting.setText(getString(R.string.greeting_default));

        // --- Set Listeners ---
        profileIcon.setOnClickListener(v -> {
            Toast.makeText(this, "Profile feature is in development.", Toast.LENGTH_SHORT).show();
        });

        btnTip.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, TipActivity.class));
        });

        btnRecommendation.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RecommendationActivity.class));
        });

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, HistoryActivity.class));
        });

        btnScan.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ScanActivity.class));
        });

        btnDocuments.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, DocumentsActivity.class));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_help) {
            // UPDATED: Go to the new HelpActivity
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        } else if (id == R.id.action_contact) {
            // UPDATED: Also go to the new HelpActivity
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            Toast.makeText(this, "Logout feature is in development.", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}