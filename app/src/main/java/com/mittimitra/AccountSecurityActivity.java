package com.mittimitra;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mittimitra.utils.ValidationUtils;

import java.util.concurrent.TimeUnit;

public class AccountSecurityActivity extends BaseActivity {

    private static final String TAG = "AccountSecurityActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    private TextView tvEmail, tvPhone, tvProvider;
    private MaterialButton btnLinkGoogle, btnLinkPhone, btnLinkEmail;
    private ProgressBar progressBar;

    private LinearLayout layoutLinkPhoneOtp;
    private View tilLinkPhoneOtp;
    private TextInputEditText etLinkPhone, etLinkOtp;
    private MaterialButton btnSendLinkOtp, btnVerifyLinkPhone;
    private String mLinkVerificationId;

    private final ActivityResultLauncher<Intent> googleLinkLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    com.google.android.gms.tasks.Task<GoogleSignInAccount> task =
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        performGoogleLink(account.getIdToken());
                    } catch (ApiException e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Google sign-in failed", e);
                        Toast.makeText(this, R.string.google_signin_failed, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            });

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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tvEmail = findViewById(R.id.tv_account_email);
        tvPhone = findViewById(R.id.tv_account_phone);
        tvProvider = findViewById(R.id.tv_account_provider);
        btnLinkGoogle = findViewById(R.id.btn_link_google);
        btnLinkPhone = findViewById(R.id.btn_link_phone);
        btnLinkEmail = findViewById(R.id.btn_link_email);
        progressBar = findViewById(R.id.progress_account_security);
        layoutLinkPhoneOtp = findViewById(R.id.layout_link_phone_otp);
        tilLinkPhoneOtp = findViewById(R.id.til_link_phone_otp);
        etLinkPhone = findViewById(R.id.et_link_phone_number);
        etLinkOtp = findViewById(R.id.et_link_phone_otp);
        btnSendLinkOtp = findViewById(R.id.btn_send_link_otp);
        btnVerifyLinkPhone = findViewById(R.id.btn_verify_link_phone);

        loadAccountInfo();

        btnLinkGoogle.setOnClickListener(v -> linkGoogleAccount());
        btnLinkPhone.setOnClickListener(v -> showPhoneLinkSection());
        btnLinkEmail.setOnClickListener(v -> startActivity(new Intent(this, LinkEmailActivity.class)));
        btnSendLinkOtp.setOnClickListener(v -> sendLinkOtp());
        btnVerifyLinkPhone.setOnClickListener(v -> verifyAndLinkPhone());
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
        if (user == null) { finish(); return; }

        String email = user.getEmail();
        String authPhone = user.getPhoneNumber();
        boolean isGoogleLinked = false;
        boolean isPhoneLinked = false;
        boolean isEmailLinked = false;

        StringBuilder providers = new StringBuilder();
        for (UserInfo profile : user.getProviderData()) {
            String id = profile.getProviderId();
            switch (id) {
                case "google.com":
                    isGoogleLinked = true;
                    if (email == null || email.isEmpty()) email = profile.getEmail();
                    break;
                case "phone":
                    isPhoneLinked = true;
                    if (authPhone == null || authPhone.isEmpty()) authPhone = profile.getPhoneNumber();
                    break;
                case "password":
                    isEmailLinked = true;
                    if (email == null || email.isEmpty()) email = profile.getEmail();
                    break;
            }
            if (!id.equals("firebase")) {
                if (providers.length() > 0) providers.append(", ");
                switch (id) {
                    case "google.com": providers.append(getString(R.string.provider_google)); break;
                    case "phone":      providers.append(getString(R.string.provider_phone));  break;
                    case "password":   providers.append(getString(R.string.provider_email));  break;
                    default:           providers.append(getString(R.string.status_unknown));  break;
                }
            }
        }

        tvEmail.setText(email != null && !email.isEmpty() ? email : getString(R.string.status_not_linked));
        tvProvider.setText(providers.length() > 0 ? providers.toString() : getString(R.string.status_unknown));

        btnLinkGoogle.setEnabled(!isGoogleLinked);
        btnLinkGoogle.setText(isGoogleLinked ? R.string.status_google_linked : R.string.link_google_account);

        btnLinkEmail.setEnabled(!isEmailLinked);
        btnLinkEmail.setText(isEmailLinked ? R.string.status_email_linked : R.string.link_email_account);

        if (isPhoneLinked && authPhone != null && !authPhone.isEmpty()) {
            tvPhone.setText(authPhone);
            btnLinkPhone.setEnabled(false);
            btnLinkPhone.setText(R.string.status_phone_linked);
        } else {
            tvPhone.setText(getString(R.string.status_not_linked));
            btnLinkPhone.setEnabled(true);
            btnLinkPhone.setText(R.string.link_phone_number);
            loadPhoneFromFirestore(user.getUid());
        }
    }

    private void loadPhoneFromFirestore(String uid) {
        db.collection("farmers").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String saved = doc.getString("phone");
                        if (saved != null && !saved.isEmpty()) {
                            tvPhone.setText(saved);
                            btnLinkPhone.setEnabled(false);
                            btnLinkPhone.setText(R.string.status_phone_saved);
                        }
                    }
                });
    }

    private void linkGoogleAccount() {
        progressBar.setVisibility(View.VISIBLE);
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            googleLinkLauncher.launch(mGoogleSignInClient.getSignInIntent());
        });
    }

    private void performGoogleLink(String idToken) {
        if (idToken == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.google_signin_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { finish(); return; }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        user.linkWithCredential(credential)
                .addOnSuccessListener(result -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.google_linked_success, Toast.LENGTH_SHORT).show();
                    loadAccountInfo();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, R.string.link_credential_in_use, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Google link failed", e);
                        Toast.makeText(this, R.string.link_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showPhoneLinkSection() {
        btnLinkPhone.setVisibility(View.GONE);
        layoutLinkPhoneOtp.setVisibility(View.VISIBLE);
    }

    private void sendLinkOtp() {
        String phone = etLinkPhone.getText() != null ? etLinkPhone.getText().toString().trim() : "";
        if (!ValidationUtils.isValidIndianPhone(phone)) {
            etLinkPhone.setError(getString(R.string.signup_phone_invalid));
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        btnSendLinkOtp.setEnabled(false);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(ValidationUtils.formatIndianPhone(phone))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(linkPhoneCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks linkPhoneCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    performPhoneLink(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    progressBar.setVisibility(View.GONE);
                    btnSendLinkOtp.setEnabled(true);
                    Log.e(TAG, "Phone verification failed", e);
                    Toast.makeText(AccountSecurityActivity.this,
                            getString(R.string.otp_failed_format, e.getMessage()), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    progressBar.setVisibility(View.GONE);
                    mLinkVerificationId = verificationId;
                    tilLinkPhoneOtp.setVisibility(View.VISIBLE);
                    btnVerifyLinkPhone.setVisibility(View.VISIBLE);
                    btnSendLinkOtp.setEnabled(true);
                    Toast.makeText(AccountSecurityActivity.this, R.string.otp_sent, Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyAndLinkPhone() {
        String code = etLinkOtp.getText() != null ? etLinkOtp.getText().toString().trim() : "";
        if (!ValidationUtils.isValidOtp(code)) {
            etLinkOtp.setError(getString(R.string.otp_invalid));
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        performPhoneLink(PhoneAuthProvider.getCredential(mLinkVerificationId, code));
    }

    private void performPhoneLink(PhoneAuthCredential credential) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { finish(); return; }
        user.linkWithCredential(credential)
                .addOnSuccessListener(result -> {
                    progressBar.setVisibility(View.GONE);
                    layoutLinkPhoneOtp.setVisibility(View.GONE);
                    btnLinkPhone.setVisibility(View.VISIBLE);
                    Toast.makeText(this, R.string.phone_linked_success, Toast.LENGTH_SHORT).show();
                    loadAccountInfo();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, R.string.link_credential_in_use, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Phone link failed", e);
                        Toast.makeText(this, R.string.link_failed, Toast.LENGTH_SHORT).show();
                    }
                });
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
