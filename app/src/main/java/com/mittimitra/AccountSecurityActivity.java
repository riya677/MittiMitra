package com.mittimitra;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Account Security screen - shows linked accounts and allows users to link additional login methods.
 * Phone display checks both Firebase Auth providers AND the Firestore profile phone field.
 */
public class AccountSecurityActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvEmail, tvPhone, tvProvider;
    private MaterialButton btnLinkGoogle, btnLinkPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        Toolbar toolbar = findViewById(R.id.toolbar_account);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvEmail = findViewById(R.id.tv_account_email);
        tvPhone = findViewById(R.id.tv_account_phone);
        tvProvider = findViewById(R.id.tv_account_provider);
        btnLinkGoogle = findViewById(R.id.btn_link_google);
        btnLinkPhone = findViewById(R.id.btn_link_phone);

        loadAccountInfo();

        btnLinkGoogle.setOnClickListener(v -> linkGoogleAccount());
        btnLinkPhone.setOnClickListener(v -> linkPhoneNumber());
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> loadAccountInfo());
        }
    }

    private void loadAccountInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        String email = user.getEmail();
        String authPhone = user.getPhoneNumber();
        boolean isGoogleLinked = false;
        boolean isPhoneLinked = false;

        StringBuilder providersBuilder = new StringBuilder();

        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            String providerId = profile.getProviderId();
            switch (providerId) {
                case "google.com":
                    isGoogleLinked = true;
                    if (email == null || email.isEmpty()) email = profile.getEmail();
                    break;
                case "phone":
                    isPhoneLinked = true;
                    if (authPhone == null || authPhone.isEmpty()) authPhone = profile.getPhoneNumber();
                    break;
                case "password":
                    if (email == null || email.isEmpty()) email = profile.getEmail();
                    break;
            }
            if (!providerId.equals("firebase")) {
                if (providersBuilder.length() > 0) providersBuilder.append(", ");
                switch (providerId) {
                    case "google.com": providersBuilder.append(getString(R.string.provider_google)); break;
                    case "phone":     providersBuilder.append(getString(R.string.provider_phone));  break;
                    case "password":  providersBuilder.append(getString(R.string.provider_email));  break;
                    default:          providersBuilder.append(providerId); break;
                }
            }
        }

        tvEmail.setText(email != null && !email.isEmpty() ? email : getString(R.string.status_not_linked));
        tvProvider.setText(providersBuilder.length() > 0 ? providersBuilder.toString() : getString(R.string.status_unknown));

        if (isGoogleLinked) {
            btnLinkGoogle.setEnabled(false);
            btnLinkGoogle.setText(R.string.status_google_linked);
        } else {
            btnLinkGoogle.setEnabled(true);
            btnLinkGoogle.setText(R.string.link_google_account);
        }

        if (isPhoneLinked && authPhone != null && !authPhone.isEmpty()) {
            // Phone is a real Firebase Auth provider
            tvPhone.setText(authPhone);
            btnLinkPhone.setEnabled(false);
            btnLinkPhone.setText(R.string.status_phone_linked);
        } else {
            // Not auth-linked — check Firestore profile for a saved phone number
            tvPhone.setText(getString(R.string.status_not_linked));
            btnLinkPhone.setEnabled(true);
            btnLinkPhone.setText(R.string.link_phone_number);
            loadPhoneFromFirestore(user.getUid());
        }
    }

    /**
     * Fetches the phone number saved in the user's Firestore profile.
     * This covers users who saved their phone via Profile but haven't done OTP auth linking.
     */
    private void loadPhoneFromFirestore(String uid) {
        db.collection("farmers").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String savedPhone = doc.getString("phone");
                        if (savedPhone != null && !savedPhone.isEmpty()) {
                            tvPhone.setText(savedPhone);
                            btnLinkPhone.setEnabled(false);
                            btnLinkPhone.setText(R.string.status_phone_saved);
                        }
                    }
                });
        // On failure, leave the UI as-is (shows "Not linked")
    }

    private void linkGoogleAccount() {
        Toast.makeText(this, getString(R.string.account_link_soon), Toast.LENGTH_SHORT).show();
    }

    private void linkPhoneNumber() {
        Toast.makeText(this, getString(R.string.account_link_soon), Toast.LENGTH_SHORT).show();
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
