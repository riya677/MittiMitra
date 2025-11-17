package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // Make sure this import is added
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends BaseActivity {

    private SessionManager sessionManager; // NEW: Added session manager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // NEW: Initialize the session manager
        sessionManager = new SessionManager(getApplicationContext());

        MaterialButton btnGoToLogin = findViewById(R.id.btn_go_to_login);
        MaterialButton btnGoToSignup = findViewById(R.id.btn_go_to_signup);

        // NEW: Create a single listener for both buttons
        View.OnClickListener listener = v -> {
            // 1. Simulate a successful login by saving a mock user
            // This is what your teammate's login screen will do on success
            sessionManager.saveUser("local_test_user_id", "Farmer"); // Using a test ID and name

            // 2. Go to Home page
            Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
            // Clear the task stack so user can't go "back" to Splash or Welcome
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        };

        // UPDATED: Set the listener for both buttons
        btnGoToLogin.setOnClickListener(listener);
        btnGoToSignup.setOnClickListener(listener);
    }
}