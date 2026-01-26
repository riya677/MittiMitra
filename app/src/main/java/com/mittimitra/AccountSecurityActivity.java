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

/**
 * Account Security screen - shows linked accounts and allows users to link additional login methods
 */
public class AccountSecurityActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private TextView tvEmail, tvPhone, tvProvider;
    private MaterialButton btnLinkGoogle, btnLinkPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_account);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mAuth = FirebaseAuth.getInstance();
        
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
            user.reload().addOnCompleteListener(task -> {
                loadAccountInfo();
            });
        }
    }

    private void loadAccountInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        String email = user.getEmail();
        String phone = user.getPhoneNumber();
        boolean isGoogleLinked = false;
        boolean isPhoneLinked = false;
        boolean isEmailLinked = false;

        StringBuilder providersBuilder = new StringBuilder();
        
        // Iterate through all linked providers to get accurate status
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            String providerId = profile.getProviderId();
            
            // Check provider type and fallback to profile data if main user data is missing
            switch (providerId) {
                case "google.com":
                    isGoogleLinked = true;
                    if (email == null || email.isEmpty()) email = profile.getEmail();
                    break;
                case "phone":
                    isPhoneLinked = true;
                    if (phone == null || phone.isEmpty()) phone = profile.getPhoneNumber();
                    break;
                case "password":
                    isEmailLinked = true;
                    if (email == null || email.isEmpty()) email = profile.getEmail();
                    break;
            }

            // Build display string
            if (!providerId.equals("firebase")) {
                if (providersBuilder.length() > 0) providersBuilder.append(", ");
                switch (providerId) {
                    case "google.com": providersBuilder.append(getString(R.string.provider_google)); break;
                    case "phone": providersBuilder.append(getString(R.string.provider_phone)); break;
                    case "password": providersBuilder.append(getString(R.string.provider_email)); break;
                    default: providersBuilder.append(providerId); break;
                }
            }
        }

        // 1. Set Email Text
        tvEmail.setText(email != null && !email.isEmpty() ? email : getString(R.string.status_not_linked));
        
        // 2. Set Phone Text
        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : getString(R.string.status_not_linked));
        
        // 3. Set Provider Text
        tvProvider.setText(providersBuilder.length() > 0 ? providersBuilder.toString() : getString(R.string.status_unknown));

        // 4. Update Google Button State
        if (isGoogleLinked) {
            btnLinkGoogle.setEnabled(false);
            btnLinkGoogle.setText(R.string.status_google_linked);
        } else {
            btnLinkGoogle.setEnabled(true);
            btnLinkGoogle.setText(R.string.link_google_account);
        }

        // 5. Update Phone Button State
        if (isPhoneLinked) {
            btnLinkPhone.setEnabled(false);
            btnLinkPhone.setText(R.string.status_phone_linked);
        } else {
            btnLinkPhone.setEnabled(true);
            btnLinkPhone.setText(R.string.link_phone_number);
        }
    }

    private void linkGoogleAccount() {
        Toast.makeText(this, "Google account linking will be available soon", Toast.LENGTH_SHORT).show();
        // Future: Implement Google credential linking using GoogleSignInClient
    }

    private void linkPhoneNumber() {
        Toast.makeText(this, "Phone linking will be available soon", Toast.LENGTH_SHORT).show();
        // Future: Implement phone linking using PhoneAuthProvider
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
