package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.mittimitra.database.MittiMitraDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// UPDATED: Must extend BaseActivity
public class HomeActivity extends BaseActivity {

    private MittiMitraDatabase db;
    private SessionManager sessionManager;
    private ExecutorService databaseExecutor;
    private Handler mainThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // --- Initialize DB, Session, & Threading ---
        db = MittiMitraDatabase.getDatabase(getApplicationContext());
        sessionManager = new SessionManager(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        // --- Find Views ---
        TextView tvGreeting = findViewById(R.id.tv_greeting);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        MaterialCardView btnTip = findViewById(R.id.btn_tip);
        MaterialCardView btnRecommendation = findViewById(R.id.btn_recommendation);
        MaterialCardView btnHistory = findViewById(R.id.btn_history);
        MaterialCardView btnScan = findViewById(R.id.btn_scan);
        MaterialCardView btnDocuments = findViewById(R.id.btn_documents);

        // --- Set Greeting ---
        tvGreeting.setText(String.format(getString(R.string.greeting_format), sessionManager.getUserName()));

        // --- Set Listeners ---
        profileIcon.setOnClickListener(v -> {
            // TODO: This should open your teammate's Profile Activity
            Toast.makeText(this, "Profile feature in development.", Toast.LENGTH_SHORT).show();
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
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        } else if (id == R.id.action_contact) {
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            // --- UPDATED Logout Logic ---

            // 1. Clear the Session Token
            sessionManager.clearSession();

            // 2. Clear the local database cache
            databaseExecutor.execute(() -> {
                db.clearAllTables();

                mainThreadHandler.post(() -> {
                    // 3. Relaunch the app to the Welcome screen
                    Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, WelcomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                });
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}