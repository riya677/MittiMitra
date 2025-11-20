package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private EditText etPhone, etOtp;
    private LinearLayout layoutOtpVerify;
    private ProgressBar progressBar;
    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etPhone = findViewById(R.id.et_phone_auth);
        etOtp = findViewById(R.id.et_otp_code);
        layoutOtpVerify = findViewById(R.id.layout_otp_verify);
        progressBar = findViewById(R.id.progress_login);
        TextView tvForgotPass = findViewById(R.id.tv_forgot_password);

        // Google Sign In Config
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Listeners
        findViewById(R.id.btn_google_signin).setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.btn_send_otp).setOnClickListener(v -> sendOtp());
        findViewById(R.id.btn_verify_otp).setOnClickListener(v -> verifyOtp());

        // Requirement 5: Forgot Password
        tvForgotPass.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    // --- GOOGLE ---
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                checkAndCreateUser(mAuth.getCurrentUser());
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- PHONE OTP ---
    private void sendOtp() {
        String mobile = etPhone.getText().toString().trim();
        if (mobile.length() < 10) {
            etPhone.setError("Valid Number Required");
            return;
        }
        if (!mobile.startsWith("+")) mobile = "+91" + mobile; // Default India

        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(mobile)
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
                    signInWithCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "OTP Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    progressBar.setVisibility(View.GONE);
                    mVerificationId = verificationId;
                    layoutOtpVerify.setVisibility(View.VISIBLE);
                    Toast.makeText(LoginActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyOtp() {
        String code = etOtp.getText().toString().trim();
        if (code.isEmpty()) return;
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                checkAndCreateUser(task.getResult().getUser());
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- COMMON USER CHECK ---
    private void checkAndCreateUser(FirebaseUser user) {
        db.collection("farmers").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        navigateToHome(doc.getString("firstName"));
                    } else {
                        createUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> navigateToHome("Farmer"));
    }

    private void createUserProfile(FirebaseUser user) {
        Map<String, Object> map = new HashMap<>();
        String name = user.getDisplayName() != null ? user.getDisplayName() : "Farmer";
        map.put("firstName", name);
        map.put("phone", user.getPhoneNumber());
        map.put("email", user.getEmail());
        map.put("createdAt", System.currentTimeMillis());

        db.collection("farmers").document(user.getUid()).set(map)
                .addOnSuccessListener(aVoid -> navigateToHome(name));
    }

    private void navigateToHome(String name) {
        progressBar.setVisibility(View.GONE);
        SessionManager session = new SessionManager(this);
        session.saveUser(mAuth.getUid(), name != null ? name : "Farmer");

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}