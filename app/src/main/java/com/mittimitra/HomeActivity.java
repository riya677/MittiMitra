package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private ImageView imgProfileToolbar;
    private SessionManager sessionManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        tvWelcome = findViewById(R.id.tv_welcome_user);
        imgProfileToolbar = findViewById(R.id.img_profile_toolbar);

        setupGridNavigation();

        // Profile Icon -> Profile Page
        findViewById(R.id.btn_profile_toolbar).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        fetchUserDetails();
    }

    private void setupGridNavigation() {
        setClick(R.id.card_scan, ScanActivity.class);
        setClick(R.id.card_documents, DocumentsActivity.class);
        setClick(R.id.card_history, HistoryActivity.class);
        setClick(R.id.card_tips, TipActivity.class);
        setClick(R.id.card_recommendations, RecommendationActivity.class);
        setClick(R.id.card_settings, SettingsActivity.class);
    }

    private void setClick(int id, Class<?> cls) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(view -> startActivity(new Intent(this, cls)));
    }

    private void fetchUserDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String localName = sessionManager.getUserName();
        tvWelcome.setText("Namaste, " + (localName != null ? localName : "Farmer"));

        db.collection("farmers").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("firstName");
                String pic = doc.getString("profilePic");
                if (name != null) {
                    tvWelcome.setText("Namaste, " + name);
                    sessionManager.saveUser(uid, name);
                }
                if (pic != null && !pic.isEmpty()) {
                    Glide.with(this).load(pic).circleCrop().into(imgProfileToolbar);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_contact) {
            startActivity(new Intent(this, ContactActivity.class)); // Requirement 1
            return true;
        } else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            sessionManager.clearSession();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}