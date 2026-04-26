package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import com.google.firebase.firestore.SetOptions;
import com.mittimitra.backend.ApiEnvelope;
import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.model.AccountModels;
import com.mittimitra.data.repository.FirebaseUserProfileRepository;
import com.mittimitra.domain.repository.UserProfileRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private EditText etPhone, etOtp, etLoginEmail, etLoginPassword;
    private LinearLayout layoutOtpVerify, layoutPhoneSection, layoutEmailLogin;
    private ProgressBar progressBar;
    private String mVerificationId;
    private boolean isEmailMode = false;

    private String prefilledName;
    private String prefilledEmail;

    private final UserProfileRepository userProfileRepository = new FirebaseUserProfileRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etPhone = findViewById(R.id.et_phone_auth);
        etOtp = findViewById(R.id.et_otp_code);
        layoutOtpVerify = findViewById(R.id.layout_otp_verify);
        layoutPhoneSection = findViewById(R.id.layout_phone_section);
        layoutEmailLogin = findViewById(R.id.layout_email_login);
        etLoginEmail = layoutEmailLogin.findViewById(R.id.et_login_email);
        etLoginPassword = layoutEmailLogin.findViewById(R.id.et_login_password);
        progressBar = findViewById(R.id.progress_login);
        TextView tvForgotPass = findViewById(R.id.tv_forgot_password);
        TextView tvToggle = findViewById(R.id.tv_toggle_email_login);

        findViewById(R.id.btn_google_signin).setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.btn_send_otp).setOnClickListener(v -> sendOtp());
        findViewById(R.id.btn_verify_otp).setOnClickListener(v -> verifyOtp());
        findViewById(R.id.btn_email_login).setOnClickListener(v -> signInWithEmail());

        tvToggle.setOnClickListener(v -> {
            isEmailMode = !isEmailMode;
            layoutPhoneSection.setVisibility(isEmailMode ? View.GONE : View.VISIBLE);
            layoutOtpVerify.setVisibility(View.GONE);
            layoutEmailLogin.setVisibility(isEmailMode ? View.VISIBLE : View.GONE);
            tvToggle.setText(isEmailMode
                    ? getString(R.string.login_use_phone_instead)
                    : getString(R.string.login_use_email_instead));
        });

        tvForgotPass.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        findViewById(R.id.tv_go_to_signup).setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));

        Intent intent = getIntent();
        if (intent.hasExtra("prefill_phone")) {
            etPhone.setText(intent.getStringExtra("prefill_phone"));
            prefilledName = intent.getStringExtra("prefill_name");
            prefilledEmail = intent.getStringExtra("prefill_email");
        }
    }

    private void signInWithGoogle() {
        int playServicesStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (playServicesStatus != ConnectionResult.SUCCESS) {
            Toast.makeText(this, getString(R.string.google_play_services_required), Toast.LENGTH_LONG).show();
            return;
        }

        if (mGoogleSignInClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        }

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
                        Log.e(TAG, "Google sign-in failed. status=" + e.getStatusCode(), e);
                        if (e.getStatusCode() == ConnectionResult.DEVELOPER_ERROR) {
                            Toast.makeText(this, getString(R.string.google_signin_config_error), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, getString(R.string.google_signin_failed), Toast.LENGTH_SHORT).show();
                        }
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
                Toast.makeText(this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendOtp() {
        String mobile = etPhone.getText().toString().trim();

        if (!com.mittimitra.utils.ValidationUtils.isValidIndianPhone(mobile)) {
            etPhone.setError(com.mittimitra.utils.ValidationUtils.getPhoneValidationError(mobile));
            return;
        }

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
                    Toast.makeText(LoginActivity.this, getString(R.string.otp_failed_format, e.getMessage()), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    progressBar.setVisibility(View.GONE);
                    mVerificationId = verificationId;
                    layoutOtpVerify.setVisibility(View.VISIBLE);
                    Toast.makeText(LoginActivity.this, getString(R.string.otp_sent), Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyOtp() {
        String code = etOtp.getText().toString().trim();
        if (!com.mittimitra.utils.ValidationUtils.isValidOtp(code)) {
            etOtp.setError(getString(R.string.otp_invalid));
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithEmail() {
        String email = etLoginEmail.getText() != null ? etLoginEmail.getText().toString().trim() : "";
        String password = etLoginPassword.getText() != null ? etLoginPassword.getText().toString().trim() : "";
        if (!com.mittimitra.utils.ValidationUtils.isValidEmail(email)) {
            etLoginEmail.setError(getString(R.string.signup_email_invalid));
            etLoginEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etLoginPassword.setError(getString(R.string.login_hint_password));
            etLoginPassword.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                checkAndCreateUser(mAuth.getCurrentUser());
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.login_email_failed), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Email login failed", task.getException());
            }
        });
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                checkAndCreateUser(task.getResult().getUser());
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.otp_invalid), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndCreateUser(FirebaseUser user) {
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("farmers").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        applyPendingPhoneLink(user.getUid(), doc);
                        navigateToHome(doc.getString("firstName"));
                    } else {
                        checkForDuplicateAndCreate(user);
                    }
                })
                .addOnFailureListener(e -> navigateToHome("Farmer"));
    }

    private void applyPendingPhoneLink(String uid, com.google.firebase.firestore.DocumentSnapshot doc) {
        android.content.SharedPreferences pendingPrefs = getSharedPreferences("pending_link", MODE_PRIVATE);
        String pendingPhone = pendingPrefs.getString("phone", null);
        String pendingEmail = pendingPrefs.getString("email", null);
        String targetUid = pendingPrefs.getString("target_uid", null);
        if (pendingPhone == null || !uid.equals(targetUid)) return;

        pendingPrefs.edit().clear().apply();

        Map<String, Object> updates = new HashMap<>();
        String existingPhone = doc.getString("phone");
        String existingEmail = doc.getString("email");

        if ((existingPhone == null || existingPhone.isEmpty()) && !pendingPhone.isEmpty()) {
            updates.put("phone", pendingPhone);
        }
        if ((existingEmail == null || existingEmail.isEmpty()) && pendingEmail != null && !pendingEmail.isEmpty()) {
            updates.put("email", pendingEmail);
        }

        if (!updates.isEmpty()) {
            db.collection("farmers").document(uid).set(updates, SetOptions.merge())
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, getString(R.string.dup_phone_linked), Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to apply pending phone/email link", e));
        }
    }

    private void checkForDuplicateAndCreate(FirebaseUser user) {
        String phone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
        String email = user.getEmail() != null ? user.getEmail() : "";

        if (phone.isEmpty() && email.isEmpty()) {
            createUserProfile(user);
            return;
        }

        AccountModels.DuplicateAccountRequest request = new AccountModels.DuplicateAccountRequest();
        request.currentUid = user.getUid();
        request.phone = phone;
        request.email = email;

        userProfileRepository.checkDuplicateAccount(request, new BackendCallback<AccountModels.DuplicateAccountData>() {
            @Override
            public void onSuccess(@NonNull ApiEnvelope<AccountModels.DuplicateAccountData> envelope) {
                AccountModels.DuplicateAccountData data = envelope.data;
                if (data != null && data.duplicate && data.existingUid != null && !data.existingUid.isEmpty()) {
                    showDuplicateWarning(user, data);
                } else {
                    createUserProfile(user);
                }
            }

            @Override
            public void onFailure(@NonNull ApiEnvelope<AccountModels.DuplicateAccountData> envelope, Throwable throwable) {
                createUserProfile(user);
            }
        });
    }

    private void showDuplicateWarning(FirebaseUser currentUser, AccountModels.DuplicateAccountData existing) {
        progressBar.setVisibility(View.GONE);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_duplicate_account, null);
        builder.setView(view);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCancelable(false);

        TextView tvName = view.findViewById(R.id.tv_existing_name);
        TextView tvPhone = view.findViewById(R.id.tv_existing_phone);
        TextView tvEmail = view.findViewById(R.id.tv_existing_email);

        tvName.setText(getString(R.string.label_name_value, safe(existing.maskedName)));
        tvPhone.setText(getString(R.string.label_phone_value, safe(existing.maskedPhone)));
        tvEmail.setText(getString(R.string.label_email_value, safe(existing.maskedEmail)));

        view.findViewById(R.id.btn_use_existing).setOnClickListener(v -> {
            dialog.dismiss();
            progressBar.setVisibility(View.VISIBLE);
            currentUser.delete().addOnCompleteListener(t -> {
                progressBar.setVisibility(View.GONE);
                if (!t.isSuccessful()) {
                    Toast.makeText(this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to delete duplicate signed-in user", t.getException());
                    return;
                }
                Toast.makeText(this, getString(R.string.dup_toast_login), Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                recreate();
            });
        });

        view.findViewById(R.id.btn_link_accounts).setOnClickListener(v -> {
            dialog.dismiss();
            progressBar.setVisibility(View.VISIBLE);

            String phoneToLink = currentUser.getPhoneNumber();
            String emailToLink = currentUser.getEmail();

            getSharedPreferences("pending_link", MODE_PRIVATE).edit()
                    .putString("phone", phoneToLink != null ? phoneToLink : "")
                    .putString("email", emailToLink != null ? emailToLink : "")
                    .putString("target_uid", existing.existingUid)
                    .apply();

            AccountModels.LinkIdentityRequest linkReq = new AccountModels.LinkIdentityRequest();
            linkReq.targetUid = existing.existingUid;
            linkReq.phone = phoneToLink;
            linkReq.email = emailToLink;

            userProfileRepository.linkAccountIdentity(linkReq, new BackendCallback<AccountModels.LinkIdentityData>() {
                @Override
                public void onSuccess(@NonNull ApiEnvelope<AccountModels.LinkIdentityData> envelope) {
                    // Keep user flow identical even if backend already linked.
                }

                @Override
                public void onFailure(@NonNull ApiEnvelope<AccountModels.LinkIdentityData> envelope, Throwable throwable) {
                    // Keep fallback path through pending_link to avoid blocking sign-in.
                }
            });

            currentUser.delete().addOnCompleteListener(t -> {
                progressBar.setVisibility(View.GONE);
                if (!t.isSuccessful()) {
                    Toast.makeText(this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to delete user after account-link flow", t.getException());
                    return;
                }
                Toast.makeText(this, getString(R.string.dup_link_instructions), Toast.LENGTH_LONG).show();
                mAuth.signOut();
                recreate();
            });
        });

        view.findViewById(R.id.btn_cancel_duplicate).setOnClickListener(v -> {
            dialog.dismiss();
            mAuth.signOut();
            finish();
        });

        dialog.show();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }

    private void createUserProfile(FirebaseUser user) {
        Map<String, Object> map = new HashMap<>();

        String name = prefilledName != null ? prefilledName :
                (user.getDisplayName() != null ? user.getDisplayName() : "Farmer");

        map.put("firstName", name);
        String phone = user.getPhoneNumber();
        if (phone != null && !phone.trim().isEmpty()) {
            map.put("phone", phone);
        }

        String email = prefilledEmail != null ? prefilledEmail : user.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            map.put("email", email);
        }

        map.put("createdAt", System.currentTimeMillis());

        db.collection("farmers").document(user.getUid()).set(map)
                .addOnSuccessListener(aVoid -> navigateToHome(name))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user profile", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                });
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
