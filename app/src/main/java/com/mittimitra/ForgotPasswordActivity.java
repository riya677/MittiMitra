package com.mittimitra;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        EditText etEmail = findViewById(R.id.et_reset_email);
        Button btnSend = findViewById(R.id.btn_reset_pass);

        btnSend.setOnClickListener(v -> {
            String mail = etEmail.getText().toString().trim();

            // Validate: empty
            if (mail.isEmpty()) {
                etEmail.setError(getString(R.string.forgot_enter_email));
                etEmail.requestFocus();
                return;
            }
            // Validate: format
            if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                etEmail.setError(getString(R.string.error_invalid_email));
                etEmail.requestFocus();
                return;
            }

            // Disable button during request to prevent double-tap
            btnSend.setEnabled(false);

            FirebaseAuth.getInstance().sendPasswordResetEmail(mail)
                    .addOnCompleteListener(task -> {
                        btnSend.setEnabled(true);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, getString(R.string.forgot_reset_sent), Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Exception ex = task.getException();
                            String msg = ex != null ? ex.getMessage() : getString(R.string.forgot_reset_error);
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
