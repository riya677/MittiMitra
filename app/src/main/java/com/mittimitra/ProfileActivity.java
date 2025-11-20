package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // Fixed missing import
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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

        tvName = findViewById(R.id.tv_profile_name);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvJoinDate = findViewById(R.id.tv_profile_join_date);
        recyclerRecent = findViewById(R.id.recycler_recent_history);
        tvNoHistory = findViewById(R.id.tv_no_recent_history);

        tvName.setText(sessionManager.getUserName());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String phone = user.getPhoneNumber();
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Phone: Not Linked");

            String email = user.getEmail();
            tvEmail.setText(email != null && !email.isEmpty() ? email : "Email: Not Linked");

            if (user.getMetadata() != null) {
                long created = user.getMetadata().getCreationTimestamp();
                tvJoinDate.setText("Member Since: " + new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(created)));
            }
            loadFirestoreData(user.getUid());
        }

        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        loadRecentHistory(); // Only one call

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
            if (doc.exists() && doc.contains("firstName")) {
                tvName.setText(doc.getString("firstName"));
            }
        });
    }

    // This is the ONLY definition of loadRecentHistory
    private void loadRecentHistory() {
        new Thread(() -> {
            List<SoilAnalysis> history = MittiMitraDatabase.getDatabase(this)
                    .soilDao().getAllSoilAnalysis();

            runOnUiThread(() -> {
                if (history != null && !history.isEmpty()) {
                    // Show Last 2 Reports as requested
                    int size = Math.min(history.size(), 2);
                    List<SoilAnalysis> recentItems = history.subList(0, size);

                    RecentAnalysisAdapter adapter = new RecentAnalysisAdapter(recentItems);
                    recyclerRecent.setAdapter(adapter);
                    recyclerRecent.setVisibility(View.VISIBLE); // Fixed import
                    tvNoHistory.setVisibility(View.GONE);     // Fixed import
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