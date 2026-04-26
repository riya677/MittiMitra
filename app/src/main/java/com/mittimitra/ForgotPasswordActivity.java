package com.mittimitra;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class ForgotPasswordActivity extends BaseActivity {
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
                            Toast.makeText(this, resolveResetError(ex), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private String resolveResetError(Exception ex) {
        if (ex instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) ex).getErrorCode();
            if ("ERROR_USER_NOT_FOUND".equals(code) || "ERROR_INVALID_EMAIL".equals(code)) {
                return getString(R.string.error_invalid_email);
            }
        }
        return getString(R.string.forgot_reset_error);
    }
}
