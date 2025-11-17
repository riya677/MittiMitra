package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

// This is a BaseActivity to ensure language/theme is applied
public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DELAY = 1500; // 1.5 seconds
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(getApplicationContext());

        // Use a Handler to delay the next action
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Check if the user is logged in
            if (sessionManager.isLoggedIn()) {
                // User is logged in, go to Home
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                // User is not logged in, go to the new Welcome screen
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            }

            // Finish this activity so the user can't press "back" to it
            finish();

        }, SPLASH_DELAY);
    }
}