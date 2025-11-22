package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private SessionManager sessionManager;
    private RecyclerView recyclerRecent;
    private TextView tvNoHistory;
    private FirebaseFirestore db;
    private TextView tvName, tvPhone, tvEmail, tvJoinDate;
    private String currentPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // Clean title for this design
        }

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();

        tvName = findViewById(R.id.tv_profile_name);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvJoinDate = findViewById(R.id.tv_profile_join_date);
        recyclerRecent = findViewById(R.id.recycler_recent_history);
        tvNoHistory = findViewById(R.id.tv_no_recent_history);
        FloatingActionButton fabEdit = findViewById(R.id.fab_edit_profile);

        // Load basic info
        tvName.setText(sessionManager.getUserName());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            tvEmail.setText(email != null && !email.isEmpty() ? email : "Email: Not Linked");

            if (user.getMetadata() != null) {
                long created = user.getMetadata().getCreationTimestamp();
                tvJoinDate.setText("Member Since: " + new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(created)));
            }

            // Load advanced info (Updated Name & Phone) from Firestore
            loadFirestoreData(user.getUid());
        }

        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        loadRecentHistory();

        // Edit Button Logic
        fabEdit.setOnClickListener(v -> showEditDialog());

        // Logout Logic
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sessionManager.clearSession();
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    private void loadFirestoreData(String uid) {
        db.collection("farmers").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (doc.contains("firstName")) {
                    String name = doc.getString("firstName");
                    tvName.setText(name);
                    // Sync session
                    sessionManager.saveUser(uid, name);
                }
                if (doc.contains("phone")) {
                    currentPhone = doc.getString("phone");
                    tvPhone.setText(currentPhone);
                } else {
                    tvPhone.setText("Add Phone Number");
                }
            }
        });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        TextInputEditText etName = view.findViewById(R.id.et_edit_name);
        TextInputEditText etPhone = view.findViewById(R.id.et_edit_phone);
        Button btnSave = view.findViewById(R.id.btn_save_edit);
        Button btnCancel = view.findViewById(R.id.btn_cancel_edit);

        // Pre-fill data
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}