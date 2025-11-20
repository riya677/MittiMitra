package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Redirect to LoginActivity which handles the auth flow for both new and existing users
        findViewById(R.id.btn_signup_google).setOnClickListener(v -> redirectToLogin());
        findViewById(R.id.btn_signup_phone).setOnClickListener(v -> redirectToLogin());

        findViewById(R.id.tv_login_link).setOnClickListener(v -> redirectToLogin());
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}