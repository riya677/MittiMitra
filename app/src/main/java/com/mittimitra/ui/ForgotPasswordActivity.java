package com.mittimitra.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mittimitra.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button sendButton;
    private TextView backLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.emailInput);
        sendButton = findViewById(R.id.sendButton);
        backLink = findViewById(R.id.backLink);

        sendButton.setOnClickListener(v -> handleSendReset());

        backLink.setOnClickListener(v -> finish());
    }

    private void handleSendReset() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);

        // TODO: Send reset link to backend
        Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();

        sendButton.setEnabled(true);
        finish();
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }
}