package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mittimitra.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SignupActivity extends BaseActivity {

    private static final String TAG = "SignupActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout layoutStep1;
    private LinearLayout layoutStep2;
    private TextInputEditText etName, etEmail, etPhone;
    private TextInputEditText etOtp;
    private TextView tvOtpSentTo, tvResend, tvChangeNumber;
    private ProgressBar progressBar;

    private String mVerificationId;
    private String pendingFormattedPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        layoutStep1 = findViewById(R.id.layout_signup_step1);
        layoutStep2 = findViewById(R.id.layout_signup_step2);
        etName = findViewById(R.id.et_signup_name);
        etEmail = findViewById(R.id.et_signup_email);
        etPhone = findViewById(R.id.et_signup_phone);
        etOtp = findViewById(R.id.et_signup_otp);
        tvOtpSentTo = findViewById(R.id.tv_otp_sent_to);
        tvResend = findViewById(R.id.tv_signup_resend);
        tvChangeNumber = findViewById(R.id.tv_signup_change_number);
        progressBar = findViewById(R.id.progress_signup);

        findViewById(R.id.btn_signup_continue).setOnClickListener(v -> validateAndSendOtp());
        findViewById(R.id.btn_signup_verify_create).setOnClickListener(v -> verifyAndCreate());

        tvResend.setOnClickListener(v -> {
            if (pendingFormattedPhone != null) sendOtp(pendingFormattedPhone);
        });
        tvChangeNumber.setOnClickListener(v -> {
            layoutStep1.setVisibility(View.VISIBLE);
            layoutStep2.setVisibility(View.GONE);
            mVerificationId = null;
        });

        findViewById(R.id.tv_signup_login_link).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void validateAndSendOtp() {
        String name = text(etName);
        String email = text(etEmail);
        String phone = text(etPhone);

        if (!ValidationUtils.isValidName(name)) {
            etName.setError(getString(R.string.signup_name_required));
            etName.requestFocus();
            return;
        }
        if (!email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
            etEmail.setError(getString(R.string.signup_email_invalid));
            etEmail.requestFocus();
            return;
        }
        if (!ValidationUtils.isValidIndianPhone(phone)) {
            etPhone.setError(getString(R.string.signup_phone_invalid));
            etPhone.requestFocus();
            return;
        }

        pendingFormattedPhone = ValidationUtils.formatIndianPhone(phone);
        sendOtp(pendingFormattedPhone);
    }

    private void sendOtp(String formattedPhone) {
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    signInAndCreate(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SignupActivity.this,
                            getString(R.string.otp_failed_format, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    progressBar.setVisibility(View.GONE);
                    mVerificationId = verificationId;
                    tvOtpSentTo.setText(getString(R.string.signup_otp_sent_to, pendingFormattedPhone));
                    layoutStep1.setVisibility(View.GONE);
                    layoutStep2.setVisibility(View.VISIBLE);
                    Toast.makeText(SignupActivity.this, R.string.otp_sent, Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyAndCreate() {
        String code = text(etOtp);
        if (!ValidationUtils.isValidOtp(code)) {
            etOtp.setError(getString(R.string.otp_invalid));
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInAndCreate(credential);
    }

    private void signInAndCreate(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.otp_invalid), Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = task.getResult().getUser();
            if (user == null) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            boolean isNew = task.getResult().getAdditionalUserInfo() != null
                    && Boolean.TRUE.equals(task.getResult().getAdditionalUserInfo().isNewUser());

            if (isNew) {
                createProfile(user);
            } else {
                navigateToHome(user);
            }
        });
    }

    private void createProfile(FirebaseUser user) {
        String name = text(etName);
        String email = text(etEmail);

        Map<String, Object> profile = new HashMap<>();
        profile.put("firstName", name.isEmpty() ? "Farmer" : name);
        profile.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        if (!email.isEmpty()) profile.put("email", email);
        profile.put("createdAt", System.currentTimeMillis());

        db.collection("farmers").document(user.getUid()).set(profile)
                .addOnSuccessListener(v -> navigateToHome(user))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Profile creation failed", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHome(FirebaseUser user) {
        progressBar.setVisibility(View.GONE);
        db.collection("farmers").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.exists() ? doc.getString("firstName") : "Farmer";
                    new SessionManager(this).saveUser(user.getUid(), name != null ? name : "Farmer");
                    goHome();
                })
                .addOnFailureListener(e -> {
                    new SessionManager(this).saveUser(user.getUid(), "Farmer");
                    goHome();
                });
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
