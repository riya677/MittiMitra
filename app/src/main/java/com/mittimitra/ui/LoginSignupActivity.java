package com.mittimitra.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import com.mittimitra.databinding.ActivityLoginSignupBinding;

public class LoginSignupActivity extends AppCompatActivity {

    private ActivityLoginSignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityLoginSignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> handleLogin());
        binding.signupButton.setOnClickListener(v -> handleSignup());
    }

    private void handleLogin() {
        String username = binding.usernameEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString();

        if (TextUtils.isEmpty(username)) {
            binding.usernameInputLayout.setError("Username/Email is required.");
            return;
        } else {
            binding.usernameInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordInputLayout.setError("Password is required.");
            return;
        } else {
            binding.passwordInputLayout.setError(null);
        }

        Toast.makeText(this, "Login successful! Welcome.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void handleSignup() {
        Toast.makeText(this, "Navigating to Account Creation...", Toast.LENGTH_SHORT).show();
    }
}
