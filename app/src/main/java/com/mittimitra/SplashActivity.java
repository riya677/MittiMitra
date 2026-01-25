package com.mittimitra;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Splash screen that handles both normal launch and deep link navigation.
 * 
 * Deep Link Scheme: mittimitra://[path]
 * Supported paths:
 *   - mittimitra://scan     → Opens ScanActivity
 *   - mittimitra://history  → Opens HistoryActivity
 *   - mittimitra://chat     → Opens TipActivity (Kisan Sahayak)
 *   - mittimitra://weather  → Opens WeatherAlertsActivity
 */
public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DELAY = 1500; // 1.5 seconds
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(getApplicationContext());

        // Check for deep link
        Intent intent = getIntent();
        Uri deepLink = intent.getData();

        // Use a Handler to delay the next action
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Check if the user is logged in
            if (sessionManager.isLoggedIn()) {
                // Handle deep link navigation
                if (deepLink != null) {
                    handleDeepLink(deepLink);
                } else {
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                }
            } else {
                // User is not logged in, go to Welcome screen
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            }

            finish();

        }, SPLASH_DELAY);
    }

    /**
     * Handle deep link routing to specific screens.
     */
    private void handleDeepLink(Uri uri) {
        String host = uri.getHost();
        
        if (host == null) {
            // Default to home
            startActivity(new Intent(this, HomeActivity.class));
            return;
        }

        Intent targetIntent;
        switch (host) {
            case "scan":
                targetIntent = new Intent(this, ScanActivity.class);
                break;
            case "history":
                targetIntent = new Intent(this, HistoryActivity.class);
                break;
            case "chat":
            case "tip":
                targetIntent = new Intent(this, TipActivity.class);
                break;
            case "weather":
                targetIntent = new Intent(this, WeatherAlertsActivity.class);
                break;
            case "plant":
            case "disease":
                targetIntent = new Intent(this, PlantScanActivity.class);
                break;
            default:
                targetIntent = new Intent(this, HomeActivity.class);
                break;
        }

        startActivity(targetIntent);
    }
}