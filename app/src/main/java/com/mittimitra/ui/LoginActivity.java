package com.mittimitra.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mittimitra.MainActivity;
import com.mittimitra.R;
import com.mittimitra.api.ApiService;
import com.mittimitra.api.AuthResponse;
import com.mittimitra.api.LoginRequest;
import com.mittimitra.api.RetrofitClient;
import com.mittimitra.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton;
    private TextView signupLink, forgotPasswordLink;
    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupLink = findViewById(R.id.signupLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);

        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getApiService();

        loginButton.setOnClickListener(v -> handleLogin());

        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });

        forgotPasswordLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void handleLogin() {
        String usernameOrPhone = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // ===== VALIDATION 1: USERNAME/PHONE =====
        if (usernameOrPhone.isEmpty()) {
            Toast.makeText(this, "Username or phone is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== VALIDATION 2: PASSWORD =====
        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== ALL VALIDATIONS PASSED - LOGIN =====
        loginButton.setEnabled(false);
        LoginRequest request = new LoginRequest(usernameOrPhone, password);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                loginButton.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    if (authResponse.success && authResponse.token != null) {
                        tokenManager.saveToken(authResponse.token);
                        tokenManager.saveUserId(authResponse.userId);
                        tokenManager.saveUsername(authResponse.username);

                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid username/phone or password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed - check credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                loginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}