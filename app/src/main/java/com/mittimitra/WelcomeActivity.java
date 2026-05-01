package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        MaterialButton btnGoToLogin = findViewById(R.id.btn_go_to_login);
        MaterialButton btnGoToSignup = findViewById(R.id.btn_go_to_signup);
        MaterialButton btnContinueGuest = findViewById(R.id.btn_continue_guest);

        btnGoToLogin.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );

        btnGoToSignup.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, SignupActivity.class))
        );

        btnContinueGuest.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_START_GUEST_FLOW, true);
            startActivity(intent);
        });
    }
}
