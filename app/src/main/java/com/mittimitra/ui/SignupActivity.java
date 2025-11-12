package com.mittimitra.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mittimitra.R;
import com.mittimitra.api.ApiService;
import com.mittimitra.api.AuthResponse;
import com.mittimitra.api.RetrofitClient;
import com.mittimitra.api.SignupRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private EditText fullNameInput, phoneInput;
    private Button signupButton;
    private TextView loginLink;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize all views
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        fullNameInput = findViewById(R.id.fullNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        signupButton = findViewById(R.id.signupButton);
        loginLink = findViewById(R.id.loginLink);

        apiService = RetrofitClient.getApiService();

        signupButton.setOnClickListener(v -> handleSignup());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void handleSignup() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String fullName = fullNameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        // ===== VALIDATION 1: USERNAME =====
        if (username.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.length() < 3) {
            Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== VALIDATION 2: EMAIL =====
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Email format invalid (use: name@domain.com)", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== VALIDATION 3: FULL NAME =====
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Full name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fullName.length() < 3) {
            Toast.makeText(this, "Full name must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== VALIDATION 4: PHONE =====
        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidIndianPhone(phone)) {
            Toast.makeText(this, "Phone must be 10 digits starting with 6-9 (Indian format)", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== VALIDATION 5: PASSWORD =====
        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasUpperCase(password)) {
            Toast.makeText(this, "Password must have at least 1 UPPERCASE letter", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasSpecialChar(password)) {
            Toast.makeText(this, "Password must have at least 1 special character (@#$%^&*)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasLowerCase(password)) {
            Toast.makeText(this, "Password must have at least 1 lowercase letter", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== VALIDATION 6: CONFIRM PASSWORD =====
        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Confirm password is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== ALL VALIDATIONS PASSED - SIGNUP =====
        signupButton.setEnabled(false);
        SignupRequest request = new SignupRequest(username, email, password,
                confirmPassword, fullName, phone);

        apiService.signup(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                signupButton.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.success) {
                        Toast.makeText(SignupActivity.this, "Signup successful! Please login", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, authResponse.message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SignupActivity.this, "Signup failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                signupButton.setEnabled(true);
                Toast.makeText(SignupActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== VALIDATION HELPER METHODS =====

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private boolean isValidIndianPhone(String phone) {
        String phonePattern = "^[6-9]\\d{9}$";
        return phone.matches(phonePattern);
    }

    private boolean hasUpperCase(String password) {
        return password.matches(".*[A-Z].*");
    }

    private boolean hasLowerCase(String password) {
        return password.matches(".*[a-z].*");
    }

    private boolean hasSpecialChar(String password) {
        return password.matches(".*[@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?].*");
    }
}