package com.mittimitra;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private SessionManager sessionManager;
    private RecyclerView recyclerRecent;
    private TextView tvNoHistory;
    
    // Stats Views
    private TextView tvBadgeIcon, tvBadgeName, tvLandSize, tvCropName;

    // Firebase (Only Firestore now, NO Storage)
    private FirebaseFirestore db;

    // UI Elements
    private TextView tvName, tvPhone, tvEmail, tvJoinDate;
    private CircleImageView imgProfile;
    private String currentPhone = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Image Picker Launcher
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    saveImageLocally(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Initialize
        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();

        // Bind Views
        tvName = findViewById(R.id.tv_profile_name);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvJoinDate = findViewById(R.id.tv_profile_join_date);
        imgProfile = findViewById(R.id.img_profile);
        recyclerRecent = findViewById(R.id.recycler_recent_history);
        tvNoHistory = findViewById(R.id.tv_no_recent_history);
        FloatingActionButton fabEdit = findViewById(R.id.fab_edit_profile);
        ImageView btnChangePhoto = findViewById(R.id.btn_change_photo);

        // Stats
        tvBadgeIcon = findViewById(R.id.tv_stat_badge_icon);
        tvBadgeName = findViewById(R.id.tv_stat_badge);
        tvLandSize = findViewById(R.id.tv_stat_land);
        tvCropName = findViewById(R.id.tv_stat_crop);

        // Setup Data
        tvName.setText(sessionManager.getUserName());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String email = user.getEmail();
            tvEmail.setText(email != null && !email.isEmpty() ? email : getString(R.string.profile_email_not_linked));

            if (user.getMetadata() != null) {
                long created = user.getMetadata().getCreationTimestamp();
                tvJoinDate.setText(getString(R.string.profile_member_since) + " " + new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(created)));
            }

            // Load data from Firestore
            loadFirestoreData(user.getUid());

            // Load Local Image immediately
            loadLocalProfileImage(user.getUid());
        }

        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        loadRecentHistory();
        loadFarmStats();

        // Listeners
        fabEdit.setOnClickListener(v -> showEditDialog());
        btnChangePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        imgProfile.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sessionManager.clearSession();
            startActivity(new Intent(this, WelcomeActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    // --- NEW: SAVE IMAGE TO PHONE STORAGE ---
    private void saveImageLocally(Uri sourceUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            File file = new File(getFilesDir(), "profile_" + user.getUid() + ".jpg");
            try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            Toast.makeText(this, getString(R.string.profile_photo_saved), Toast.LENGTH_SHORT).show();
            loadLocalProfileImage(user.getUid());
        } catch (Exception e) {
            android.util.Log.e("ProfileActivity", "Failed to save profile image", e);
            Toast.makeText(this, R.string.profile_photo_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // --- NEW: LOAD IMAGE FROM PHONE STORAGE ---
    private void loadLocalProfileImage(String uid) {
        File file = new File(getFilesDir(), "profile_" + uid + ".jpg");

        if (file.exists()) {
            Glide.with(this)
                    .load(file) // Load directly from file
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache, so updates show instantly
                    .skipMemoryCache(true)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(imgProfile);
        } else {
            // If no file exists, show default
            imgProfile.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }

    private void loadFirestoreData(String uid) {
        android.content.SharedPreferences prefs = getSharedPreferences("profile_cache", MODE_PRIVATE);
        
        db.collection("farmers").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    if (doc.contains("firstName")) {
                        String name = doc.getString("firstName");
                        tvName.setText(name);
                        sessionManager.saveUser(uid, name);
                        // Cache for offline
                        prefs.edit().putString("cached_name", name).apply();
                    }
                    if (doc.contains("phone")) {
                        currentPhone = doc.getString("phone");
                        tvPhone.setText(currentPhone);
                        // Cache for offline
                        prefs.edit().putString("cached_phone", currentPhone).apply();
                    } else {
                        tvPhone.setText(getString(R.string.profile_add_phone));
                    }
                    // FIX: Load Email from Firestore (Phone Auth users have null email in FirebaseUser)
                    if (doc.contains("email")) {
                        String email = doc.getString("email");
                        if (email != null && !email.isEmpty()) {
                            tvEmail.setText(email);
                            // Cache for offline
                            prefs.edit().putString("cached_email", email).apply();
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                // Load from cache when offline
                loadCachedProfile(prefs);
            });
    }

    private void loadCachedProfile(android.content.SharedPreferences prefs) {
        String cachedName = prefs.getString("cached_name", null);
        String cachedPhone = prefs.getString("cached_phone", null);
        
        if (cachedName != null) {
            tvName.setText(cachedName);
        }
        if (cachedPhone != null) {
            currentPhone = cachedPhone;
            tvPhone.setText(cachedPhone);
        } else {
            tvPhone.setText(getString(R.string.profile_offline_add_phone));
        }
        
        // FIX: Load cached email
        String cachedEmail = prefs.getString("cached_email", null);
        if (cachedEmail != null && !cachedEmail.isEmpty()) {
            tvEmail.setText(cachedEmail);
        }
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etName = view.findViewById(R.id.et_edit_name);
        TextInputEditText etPhone = view.findViewById(R.id.et_edit_phone);
        TextInputEditText etEmail = view.findViewById(R.id.et_edit_email);
        TextInputEditText etDob = view.findViewById(R.id.et_edit_dob);
        TextInputEditText etAadhaar = view.findViewById(R.id.et_edit_aadhaar);
        TextInputEditText etKisanId = view.findViewById(R.id.et_edit_kisan_id);
        Button btnSave = view.findViewById(R.id.btn_save_edit);
        Button btnCancel = view.findViewById(R.id.btn_cancel_edit);

        // Pre-fill existing values - SMART HANDLING
        String currentName = tvName.getText().toString();
        if (!currentName.contains("Farmer") && !currentName.isEmpty()) {
            etName.setText(currentName);
        }
        
        // Phone - only set if we have a real number
        if (currentPhone != null && !currentPhone.isEmpty() && !currentPhone.contains("Add")) {
            etPhone.setText(currentPhone);
        }
        
        // Email - DON'T pre-fill placeholder text
        if (tvEmail != null && tvEmail.getText() != null) {
            String email = tvEmail.getText().toString();
            if (email.contains("@") && !email.contains("Not") && !email.contains("Linked")) {
                etEmail.setText(email);
            }
        }
        
        // Load saved Aadhaar and Kisan ID from Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("farmers").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String aadhaar = doc.getString("aadhaarId");
                        String kisanId = doc.getString("kisanId");
                        String dob = doc.getString("dob");
                        if (aadhaar != null && !aadhaar.isEmpty()) {
                            etAadhaar.setText(aadhaar);
                        }
                        if (kisanId != null && !kisanId.isEmpty()) {
                            etKisanId.setText(kisanId);
                        }
                        if (dob != null && !dob.isEmpty()) {
                            etDob.setText(dob);
                        }
                    }
                });
        }

        // Date picker for DOB
        etDob.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    this,
                    (datePicker, year, month, day) -> {
                        String date = String.format(java.util.Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                        etDob.setText(date);
                    },
                    calendar.get(java.util.Calendar.YEAR) - 25,
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newDob = etDob.getText().toString().trim();
            String newAadhaar = etAadhaar.getText().toString().trim();
            String newKisanId = etKisanId.getText().toString().trim();

            // Validation
            if (newName.isEmpty()) {
                etName.setError(getString(R.string.error_name_required));
                etName.requestFocus();
                return;
            }
            if (!com.mittimitra.utils.ValidationUtils.isValidIndianPhone(newPhone)) {
                etPhone.setError(com.mittimitra.utils.ValidationUtils.getPhoneValidationError(newPhone));
                etPhone.requestFocus();
                return;
            }
            if (!newEmail.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                etEmail.setError(getString(R.string.error_valid_email));
                etEmail.requestFocus();
                return;
            }
            // Aadhaar validation: must be exactly 12 digits if provided
            if (!newAadhaar.isEmpty() && newAadhaar.length() != 12) {
                etAadhaar.setError(getString(R.string.error_aadhaar_digits));
                etAadhaar.requestFocus();
                return;
            }

            // Show loading state
            btnSave.setEnabled(false);
            btnSave.setText(getString(R.string.profile_saving));
            
            updateProfile(newName, newPhone, newEmail, newDob, newAadhaar, newKisanId, dialog, btnSave);
        });

        dialog.show();
    }

    private void updateProfile(String name, String phone, String email, String dob,
                               String aadhaar, String kisanId, AlertDialog dialog, Button btnSave) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (!phone.isEmpty()) {
            // Enforce one phone number per account: check no other user owns this phone
            db.collection("farmers")
                    .whereEqualTo("phone", phone)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        boolean conflict = false;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (!doc.getId().equals(user.getUid())) {
                                conflict = true;
                                break;
                            }
                        }
                        if (conflict) {
                            btnSave.setEnabled(true);
                            btnSave.setText(getString(R.string.btn_save_changes));
                            Toast.makeText(this, getString(R.string.phone_already_registered), Toast.LENGTH_LONG).show();
                        } else {
                            doFirestoreSave(name, phone, email, dob, aadhaar, kisanId, dialog, btnSave, user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ProfileActivity", "Phone uniqueness check failed", e);
                        // PERMISSION_DENIED: client-side list queries on farmers are not allowed.
                        // Fall through to save — uniqueness is enforced server-side via Cloud Functions.
                        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                            com.google.firebase.firestore.FirebaseFirestoreException fe =
                                    (com.google.firebase.firestore.FirebaseFirestoreException) e;
                            if (fe.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                doFirestoreSave(name, phone, email, dob, aadhaar, kisanId, dialog, btnSave, user);
                                return;
                            }
                        }
                        btnSave.setEnabled(true);
                        btnSave.setText(getString(R.string.btn_save_changes));
                        Toast.makeText(this, getString(R.string.phone_check_failed), Toast.LENGTH_SHORT).show();
                    });
        } else {
            doFirestoreSave(name, phone, email, dob, aadhaar, kisanId, dialog, btnSave, user);
        }
    }

    private void doFirestoreSave(String name, String phone, String email, String dob,
                                  String aadhaar, String kisanId, AlertDialog dialog, Button btnSave,
                                  FirebaseUser user) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", name);
        updates.put("phone", phone);
        if (!email.isEmpty()) updates.put("email", email);
        if (!dob.isEmpty()) updates.put("dob", dob);
        if (!aadhaar.isEmpty()) updates.put("aadhaarId", aadhaar);
        if (!kisanId.isEmpty()) updates.put("kisanId", kisanId);

        db.collection("farmers").document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    tvName.setText(name);
                    tvPhone.setText(phone);
                    currentPhone = phone;
                    if (!email.isEmpty() && tvEmail != null) tvEmail.setText(email);
                    sessionManager.saveUser(user.getUid(), name);
                    Toast.makeText(this, getString(R.string.profile_updated_success), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText(getString(R.string.btn_save_changes));
                    Toast.makeText(this, getString(R.string.profile_update_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRecentHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        
        executor.execute(() -> {
            MittiMitraDatabase db = MittiMitraDatabase.getDatabase(this);

            // 1. Soil Analysis
            List<SoilAnalysis> soilHistory = db.soilDao().getAnalysisForUser(user.getUid());
            
            // 2. Plant Analysis (Fetch last 5)
            List<com.mittimitra.database.entity.PlantHealth> plantHistory = 
                    db.plantDao().getRecentByUserId(user.getUid(), 5);

            // 3. Crop Calendar (Fetch last 5)
            List<com.mittimitra.database.entity.CropSchedule> cropHistory = 
                    db.cropDao().getRecentByUserId(user.getUid(), 5);

            runOnUiThread(() -> {
                // Soil
                if (soilHistory != null && !soilHistory.isEmpty()) {
                    int size = Math.min(soilHistory.size(), 2);
                    List<SoilAnalysis> recentItems = soilHistory.subList(0, size);
                    RecentAnalysisAdapter adapter = new RecentAnalysisAdapter(recentItems);
                    recyclerRecent.setAdapter(adapter);
                    recyclerRecent.setVisibility(View.VISIBLE);
                    tvNoHistory.setVisibility(View.GONE);
                } else {
                    recyclerRecent.setVisibility(View.GONE);
                    tvNoHistory.setVisibility(View.VISIBLE);
                }

                // Plant
                RecyclerView recyclerPlant = findViewById(R.id.recycler_plant_history);
                TextView tvNoPlant = findViewById(R.id.tv_no_plant_history);
                if (recyclerPlant != null) {
                    recyclerPlant.setLayoutManager(new LinearLayoutManager(this));
                    if (plantHistory != null && !plantHistory.isEmpty()) {
                        com.mittimitra.ui.adapters.PlantHistoryAdapter adapter = 
                                new com.mittimitra.ui.adapters.PlantHistoryAdapter(this, plantHistory);
                        recyclerPlant.setAdapter(adapter);
                        recyclerPlant.setVisibility(View.VISIBLE);
                        tvNoPlant.setVisibility(View.GONE);
                    } else {
                        recyclerPlant.setVisibility(View.GONE);
                        tvNoPlant.setVisibility(View.VISIBLE);
                    }
                }

                // Crop
                RecyclerView recyclerCrop = findViewById(R.id.recycler_crop_history);
                TextView tvNoCrop = findViewById(R.id.tv_no_crop_history);
                if (recyclerCrop != null) {
                    recyclerCrop.setLayoutManager(new LinearLayoutManager(this));
                    if (cropHistory != null && !cropHistory.isEmpty()) {
                        com.mittimitra.ui.adapters.CropHistoryAdapter adapter = 
                                new com.mittimitra.ui.adapters.CropHistoryAdapter(cropHistory);
                        recyclerCrop.setAdapter(adapter);
                        recyclerCrop.setVisibility(View.VISIBLE);
                        tvNoCrop.setVisibility(View.GONE);
                    } else {
                        recyclerCrop.setVisibility(View.GONE);
                        tvNoCrop.setVisibility(View.VISIBLE);
                    }
                }
            });
        });
    }

    private void loadFarmStats() {
        // 1. Load Session Data
        AppPreferences prefs = new AppPreferences(this);
        
        String landSize = prefs.getLastFieldSize();
        tvLandSize.setText(landSize.isEmpty() ? "--" : landSize + " " + getString(R.string.unit_acres));
        
        String crop = prefs.getLastCrop();
        if (crop != null) {
             tvCropName.setText(crop.split(" ")[0]); // "Wheat" from "Wheat (Gehu)"
        } else {
            tvCropName.setText(getString(R.string.status_none));
        }

        // 2. Load Badge based on Scan History Count
        executor.execute(() -> {
            int scanCount = MittiMitraDatabase.getDatabase(this)
                    .soilDao().getAllSoilAnalysis().size();
            runOnUiThread(() -> updateBadge(scanCount));
        });
    }

    private void updateBadge(int scanCount) {
        if (scanCount >= 50) {
            tvBadgeIcon.setText("👑");
            tvBadgeName.setText(getString(R.string.profile_rank_master));
        } else if (scanCount >= 20) {
            tvBadgeIcon.setText("🌳");
            tvBadgeName.setText(getString(R.string.profile_rank_guardian));
        } else if (scanCount >= 5) {
            tvBadgeIcon.setText("🌿");
            tvBadgeName.setText(getString(R.string.profile_rank_expert));
        } else {
            tvBadgeIcon.setText("🌱");
            tvBadgeName.setText(getString(R.string.profile_rank_novice));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}