package com.mittimitra;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.mittimitra.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;

public class LinkEmailActivity extends BaseActivity {

    private static final String TAG = "LinkEmailActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_email);

        Toolbar toolbar = findViewById(R.id.toolbar_link_email);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.et_link_email);
        etPassword = findViewById(R.id.et_link_password);
        etConfirmPassword = findViewById(R.id.et_link_confirm_password);
        progressBar = findViewById(R.id.progress_link_email);

        // Pre-fill email if already in Firestore but not linked at Auth level
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("farmers").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        String saved = doc.getString("email");
                        if (saved != null && !saved.isEmpty() && etEmail.getText().toString().isEmpty()) {
                            etEmail.setText(saved);
                        }
                    });
        }

        findViewById(R.id.btn_link_email_submit).setOnClickListener(v -> attemptLink());
    }

    private void attemptLink() {
        String email = text(etEmail);
        String password = text(etPassword);
        String confirm = text(etConfirmPassword);

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError(getString(R.string.signup_email_invalid));
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_too_short));
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            etConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            etConfirmPassword.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.linkWithCredential(credential)
                .addOnSuccessListener(result -> {
                    saveEmailToFirestore(user.getUid(), email);
                    // Send verification email
                    if (result.getUser() != null) {
                        result.getUser().sendEmailVerification()
                                .addOnSuccessListener(v ->
                                        Log.i(TAG, "Verification email sent to " + email))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Verification email failed", e));
                    }
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.email_linked_success, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, R.string.link_credential_in_use, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Email link failed", e);
                        Toast.makeText(this, R.string.link_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveEmailToFirestore(String uid, String email) {
        Map<String, Object> update = new HashMap<>();
        update.put("email", email);
        db.collection("farmers").document(uid)
                .set(update, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save email to Firestore", e));
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
