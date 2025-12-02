package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mittimitra.utils.TokenManager;

public class MainActivity extends BaseActivity {

    private Button logoutButton;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoutButton = findViewById(R.id.logoutButton);
        tokenManager = new TokenManager(this);

        // Check if user is logged in
        String token = tokenManager.getToken();
        if (token == null) {
            // No token, go to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        // Logout button click
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        // Clear token and all user data
        tokenManager.clearToken();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Go back to login
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}