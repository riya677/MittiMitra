package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Remove default title to use our custom one
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize Views
        ImageView profileIcon = findViewById(R.id.profile_icon);
        MaterialCardView btnTip = findViewById(R.id.btn_tip);
        MaterialCardView btnRecommendation = findViewById(R.id.btn_recommendation);
        MaterialCardView btnHistory = findViewById(R.id.btn_history);
        MaterialCardView btnScan = findViewById(R.id.btn_scan);

        // --- Setup Click Listeners (Routing) ---

        // Profile Icon Navigation
        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ProfileActivity
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Tip Button Navigation
        btnTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to TipActivity
                Intent intent = new Intent(HomeActivity.this, TipActivity.class);
                startActivity(intent);
            }
        });

        // Recommendation Button Navigation
        btnRecommendation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to RecommendationActivity
                Intent intent = new Intent(HomeActivity.this, RecommendationActivity.class);
                startActivity(intent);
            }
        });

        // History Button Navigation
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to HistoryActivity
                Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        // Scan Button Navigation
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ScanActivity
                Intent intent = new Intent(HomeActivity.this, ScanActivity.class);
                startActivity(intent);
            }
        });
    }

    // --- Toolbar Menu (Sandwich Icon) ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Handle Settings click
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            // Example: startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_help) {
            // Handle Help click
            Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
            // Example: startActivity(new Intent(this, HelpActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            // Handle Logout click
            Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show();
            // Add your logout logic here
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}