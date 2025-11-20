package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize Session Manager just in case
        new SessionManager(getApplicationContext());

        MaterialButton btnGoToLogin = findViewById(R.id.btn_go_to_login);
        MaterialButton btnGoToSignup = findViewById(R.id.btn_go_to_signup);

        btnGoToLogin.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );

        btnGoToSignup.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, SignupActivity.class))
        );
    }
}