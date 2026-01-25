package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomeActivity extends BaseActivity {

    private SessionManager sessionManager;
    private ViewPager2 viewPagerCarousel;
    private final Handler carouselHandler = new Handler(Looper.getMainLooper());

    // Auto-scroll runnable
    private final Runnable carouselRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerCarousel != null) {
                int currentItem = viewPagerCarousel.getCurrentItem();
                int totalItems = viewPagerCarousel.getAdapter().getItemCount();
                viewPagerCarousel.setCurrentItem((currentItem + 1) % totalItems);
                carouselHandler.postDelayed(this, 3000); // Scroll every 3 seconds
            }
        }
    };

    private TextView tvWelcome; // Moved to field level

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        tvWelcome = findViewById(R.id.tv_home_welcome);
        
        // Initial set (onResume will handle updates)
        updateWelcomeMessage();

        viewPagerCarousel = findViewById(R.id.viewPagerCarousel);
        List<Integer> images = new ArrayList<>();

        images.add(R.drawable.banner_pm_kisan);
        images.add(R.drawable.banner_soil_health);
        images.add(R.drawable.banner_fasal_bima);

        // FIX: Use getString() for ALL titles so language switching works
        List<String> titles = new ArrayList<>();
        titles.add(getString(R.string.title_pm_kisan));
        titles.add(getString(R.string.title_soil_health));
        titles.add(getString(R.string.title_fasal_bima));

        CarouselAdapter adapter = new CarouselAdapter(images, titles);
        viewPagerCarousel.setAdapter(adapter);

        carouselHandler.postDelayed(carouselRunnable, 3000);

        setupGridNavigation();
        setupBottomNavigation();

        // FIX: Ask for Notification Permission (Required for Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
            }
        // Schedule Smart Reminders (Periodic Work)
        androidx.work.PeriodicWorkRequest weatherWork =
                new androidx.work.PeriodicWorkRequest.Builder(NotificationWorker.class, 12, java.util.concurrent.TimeUnit.HOURS)
                        .build();
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SmartWeatherWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                weatherWork
        );
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh name every time activity comes to foreground (e.g. back from Profile)
        updateWelcomeMessage();
        refreshUserNameFromFirestore();
    }

    private void updateWelcomeMessage() {
        if (tvWelcome != null) {
            tvWelcome.setText(getString(R.string.greeting_format, sessionManager.getUserName()));
        }
    }
    
    // Updated to use class member tvWelcome
    private void refreshUserNameFromFirestore() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("farmers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("firstName");
                        if (name != null && !name.isEmpty()) {
                            // Update session and UI
                            sessionManager.saveUser(user.getUid(), name);
                            updateWelcomeMessage();
                        }
                    }
                });
    }

    private void setupGridNavigation() {
        findViewById(R.id.card_scan).setOnClickListener(v -> startActivity(new Intent(this, ScanActivity.class)));
        findViewById(R.id.card_history).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.card_documents).setOnClickListener(v -> startActivity(new Intent(this, DocumentsActivity.class)));
        findViewById(R.id.card_tips).setOnClickListener(v -> startActivity(new Intent(this, TipActivity.class)));
        findViewById(R.id.card_recommendation).setOnClickListener(v -> startActivity(new Intent(this, RecommendationActivity.class)));
        findViewById(R.id.card_help).setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));
        findViewById(R.id.card_plant_doctor).setOnClickListener(v -> startActivity(new Intent(this, PlantScanActivity.class)));
        
        // Scheme Promo Banner
        findViewById(R.id.card_scheme_promo).setOnClickListener(v -> startActivity(new Intent(this, SchemeActivity.class)));

        // New Features
        findViewById(R.id.card_weather).setOnClickListener(v -> startActivity(new Intent(this, WeatherAlertsActivity.class)));
        findViewById(R.id.card_calendar).setOnClickListener(v -> startActivity(new Intent(this, CropCalendarActivity.class)));
        findViewById(R.id.card_irrigation).setOnClickListener(v -> startActivity(new Intent(this, IrrigationActivity.class)));
        findViewById(R.id.card_mandi).setOnClickListener(v -> startActivity(new Intent(this, MandiActivity.class)));
    }

    private void setupBottomNavigation() {
        findViewById(R.id.nav_scan).setOnClickListener(v -> startActivity(new Intent(this, ScanActivity.class)));

        // RE-ENABLED profile navigation
        findViewById(R.id.nav_profile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.nav_settings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        carouselHandler.removeCallbacks(carouselRunnable);
    }
}