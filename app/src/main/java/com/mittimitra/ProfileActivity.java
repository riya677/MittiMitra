package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProfileActivity extends BaseActivity {

    private SessionManager sessionManager;
    private RecyclerView recyclerRecent;
    private TextView tvNoHistory;
    private FirebaseFirestore db;
    private TextView tvName, tvPhone, tvEmail, tvJoinDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();

        // Bind Views
        tvName = findViewById(R.id.tv_profile_name);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvJoinDate = findViewById(R.id.tv_profile_join_date);
        recyclerRecent = findViewById(R.id.recycler_recent_history);
        tvNoHistory = findViewById(R.id.tv_no_recent_history);

        // Set Initial Data
        tvName.setText(sessionManager.getUserName());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Show Phone
            String phone = user.getPhoneNumber();
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Phone: Not Linked");

            // Show Email
            String email = user.getEmail();
            tvEmail.setText(email != null && !email.isEmpty() ? email : "Email: Not Linked");

            // Show Join Date
            if (user.getMetadata() != null) {
                long creationTimestamp = user.getMetadata().getCreationTimestamp();
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                tvJoinDate.setText("Member Since: " + sdf.format(new Date(creationTimestamp)));
            }

            // Fetch extra fields from Firestore
            loadFirestoreData(user.getUid());
        }

        // Recent History List
        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        loadRecentHistory();

        // Logout
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sessionManager.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadFirestoreData(String uid) {
        db.collection("farmers").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (doc.contains("firstName")) tvName.setText(doc.getString("firstName"));
            }
        });
    }

    private void loadRecentHistory() {
        new Thread(() -> {
            // Use the correct DAO method we fixed earlier
            List<SoilAnalysis> history = MittiMitraDatabase.getDatabase(this)
                    .soilDao()
                    .getAllSoilAnalysis();

            runOnUiThread(() -> {
                if (history != null && !history.isEmpty()) {
                    int size = Math.min(history.size(), 3);
                    RecentAnalysisAdapter adapter = new RecentAnalysisAdapter(history.subList(0, size));
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