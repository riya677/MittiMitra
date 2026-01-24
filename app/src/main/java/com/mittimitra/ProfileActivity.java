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
            tvEmail.setText(email != null && !email.isEmpty() ? email : "Email: Not Linked");

            if (user.getMetadata() != null) {
                long created = user.getMetadata().getCreationTimestamp();
                tvJoinDate.setText("Member Since: " + new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(created)));
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
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    // --- NEW: SAVE IMAGE TO PHONE STORAGE ---
    private void saveImageLocally(Uri sourceUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            // 1. Open the input stream from the selected gallery image
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);

            // 2. Create a file in the app's internal storage
            // Naming it "profile_{uid}.jpg" ensures every user has their own unique file
            File file = new File(getFilesDir(), "profile_" + user.getUid() + ".jpg");

            // 3. Create output stream to write to that file
            FileOutputStream outputStream = new FileOutputStream(file);

            // 4. Copy the bytes
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 5. Close streams
            outputStream.close();
            inputStream.close();

            Toast.makeText(this, "Profile Photo Saved!", Toast.LENGTH_SHORT).show();

            // 6. Load it immediately
            loadLocalProfileImage(user.getUid());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image locally", Toast.LENGTH_SHORT).show();
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
                        tvPhone.setText("Add Phone Number");
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
            tvPhone.setText("Offline - Add phone later");
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
        Button btnSave = view.findViewById(R.id.btn_save_edit);
        Button btnCancel = view.findViewById(R.id.btn_cancel_edit);

        etName.setText(tvName.getText().toString());
        etPhone.setText(currentPhone);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                etName.setError("Name required");
                return;
            }
            if (newPhone.length() != 10) {
                etPhone.setError("Enter valid 10-digit number");
                return;
            }

            updateProfile(newName, newPhone, dialog);
        });

        dialog.show();
    }

    private void updateProfile(String name, String phone, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", name);
        updates.put("phone", phone);

        db.collection("farmers").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    tvName.setText(name);
                    tvPhone.setText(phone);
                    currentPhone = phone;
                    sessionManager.saveUser(user.getUid(), name);
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRecentHistory() {
        new Thread(() -> {
            List<SoilAnalysis> history = MittiMitraDatabase.getDatabase(this)
                    .soilDao().getAllSoilAnalysis();

            runOnUiThread(() -> {
                if (history != null && !history.isEmpty()) {
                    int size = Math.min(history.size(), 2);
                    List<SoilAnalysis> recentItems = history.subList(0, size);
                    RecentAnalysisAdapter adapter = new RecentAnalysisAdapter(recentItems);
                    recyclerRecent.setAdapter(adapter);
                    recyclerRecent.setVisibility(View.VISIBLE);
                    tvNoHistory.setVisibility(View.GONE);
                } else {
                    recyclerRecent.setVisibility(View.GONE);
                    tvNoHistory.setVisibility(View.VISIBLE);
                }
            });
        }).start();
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
        new Thread(() -> {
            int scanCount = MittiMitraDatabase.getDatabase(this)
                    .soilDao().getAllSoilAnalysis().size();
            
            runOnUiThread(() -> updateBadge(scanCount));
        }).start();
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}