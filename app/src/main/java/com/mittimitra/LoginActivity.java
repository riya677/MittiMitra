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
    
    // Prefilled data from SignupActivity
    private String prefilledName;
    private String prefilledEmail;

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

        findViewById(R.id.tv_go_to_signup).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
        
        // Check for prefilled data from SignupActivity
        Intent intent = getIntent();
        if (intent.hasExtra("prefill_phone")) {
            etPhone.setText(intent.getStringExtra("prefill_phone"));
            prefilledName = intent.getStringExtra("prefill_name");
            prefilledEmail = intent.getStringExtra("prefill_email");
        }
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
        
        // Use ValidationUtils for proper Indian phone validation
        if (!com.mittimitra.utils.ValidationUtils.isValidIndianPhone(mobile)) {
            etPhone.setError(com.mittimitra.utils.ValidationUtils.getPhoneValidationError(mobile));
            return;
        }
        
        // Format to +91XXXXXXXXXX
        String formattedPhone = com.mittimitra.utils.ValidationUtils.formatIndianPhone(mobile);

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
                        // 🔍 Check for duplicate accounts before creating
                        checkForDuplicateAndCreate(user);
                    }
                })
                .addOnFailureListener(e -> navigateToHome("Farmer"));
    }
    
    // 🔒 DUPLICATE DETECTION - Check if phone/email already exists in another account
    private void checkForDuplicateAndCreate(FirebaseUser user) {
        String phone = user.getPhoneNumber();
        String email = user.getEmail();
        
        // Check by phone first
        if (phone != null && !phone.isEmpty()) {
            db.collection("farmers")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(docs -> {
                    if (!docs.isEmpty()) {
                        // ⚠️ Duplicate found by phone!
                        showDuplicateWarning(user, docs.getDocuments().get(0));
                    } else if (email != null && !email.isEmpty()) {
                        // Check by email
                        checkDuplicateByEmail(user, email);
                    } else {
                        // No duplicates, create profile
                        createUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> createUserProfile(user));
        } else if (email != null && !email.isEmpty()) {
            checkDuplicateByEmail(user, email);
        } else {
            createUserProfile(user);
        }
    }
    
    private void checkDuplicateByEmail(FirebaseUser user, String email) {
        db.collection("farmers")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener(docs -> {
                if (!docs.isEmpty()) {
                    // ⚠️ Duplicate found by email!
                    showDuplicateWarning(user, docs.getDocuments().get(0));
                } else {
                    createUserProfile(user);
                }
            })
            .addOnFailureListener(e -> createUserProfile(user));
    }
    
    private void showDuplicateWarning(FirebaseUser currentUser, com.google.firebase.firestore.DocumentSnapshot existingDoc) {
        progressBar.setVisibility(View.GONE);
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_duplicate_account, null);
        builder.setView(view);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCancelable(false);
        
        // Populate existing account info
        android.widget.TextView tvName = view.findViewById(R.id.tv_existing_name);
        android.widget.TextView tvPhone = view.findViewById(R.id.tv_existing_phone);
        android.widget.TextView tvEmail = view.findViewById(R.id.tv_existing_email);
        
        String existingName = existingDoc.getString("firstName");
        String existingPhone = existingDoc.getString("phone");
        String existingEmail = existingDoc.getString("email");
        
        tvName.setText("Name: " + (existingName != null ? existingName : "--"));
        tvPhone.setText("Phone: " + (existingPhone != null ? maskPhone(existingPhone) : "--"));
        tvEmail.setText("Email: " + (existingEmail != null ? existingEmail : "--"));
        
        // Use existing account button - Log out and guide user
        view.findViewById(R.id.btn_use_existing).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, getString(R.string.dup_toast_login), Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            recreate();
        });
        
        // Link accounts button - Show instructions for merging
        view.findViewById(R.id.btn_link_accounts).setOnClickListener(v -> {
            dialog.dismiss();
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dup_merge_title))
                .setMessage(getString(R.string.dup_merge_msg))
                .setPositiveButton(getString(R.string.dup_btn_got_it), (d, w) -> {
                    mAuth.signOut();
                    recreate();
                })
                .show();
        });
        
        // Cancel button
        view.findViewById(R.id.btn_cancel_duplicate).setOnClickListener(v -> {
            dialog.dismiss();
            mAuth.signOut();
            finish();
        });
        
        dialog.show();
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    private void createUserProfile(FirebaseUser user) {
        Map<String, Object> map = new HashMap<>();
        
        // Use prefilled name from signup, or fallback to Google display name, or "Farmer"
        String name = prefilledName != null ? prefilledName :
                      (user.getDisplayName() != null ? user.getDisplayName() : "Farmer");
        
        map.put("firstName", name);
        map.put("phone", user.getPhoneNumber());
        
        // Use prefilled email from signup, or fallback to Firebase email
        String email = prefilledEmail != null ? prefilledEmail : user.getEmail();
        map.put("email", email);
        
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