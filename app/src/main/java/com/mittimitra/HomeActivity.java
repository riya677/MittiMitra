package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity {

    private SessionManager sessionManager;
    private ViewPager2 viewPagerCarousel;
    private final Handler carouselHandler = new Handler(Looper.getMainLooper());
    private final Runnable carouselRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerCarousel != null) {
                int currentItem = viewPagerCarousel.getCurrentItem();
                int totalItems = viewPagerCarousel.getAdapter().getItemCount();
                viewPagerCarousel.setCurrentItem((currentItem + 1) % totalItems);
                carouselHandler.postDelayed(this, 3000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        TextView tvWelcome = findViewById(R.id.tv_home_welcome);
        tvWelcome.setText(getString(R.string.greeting_format, sessionManager.getUserName()));

        // Carousel
        viewPagerCarousel = findViewById(R.id.viewPagerCarousel);
        List<Integer> images = new ArrayList<>();
        images.add(android.R.color.holo_green_light);
        images.add(android.R.color.holo_orange_light);
        images.add(android.R.color.holo_blue_light);
        List<String> titles = new ArrayList<>();
        titles.add("Instant Soil Analysis");
        titles.add("Track Your Farming History");
        titles.add("Expert Agricultural Tips");

        CarouselAdapter adapter = new CarouselAdapter(images, titles);
        viewPagerCarousel.setAdapter(adapter);
        carouselHandler.postDelayed(carouselRunnable, 3000);

        // Navigation
        findViewById(R.id.card_scan).setOnClickListener(v -> startActivity(new Intent(this, ScanActivity.class)));
        findViewById(R.id.card_history).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.card_documents).setOnClickListener(v -> startActivity(new Intent(this, DocumentsActivity.class)));
        findViewById(R.id.card_tips).setOnClickListener(v -> startActivity(new Intent(this, TipActivity.class)));
        findViewById(R.id.card_recommendation).setOnClickListener(v -> startActivity(new Intent(this, RecommendationActivity.class)));
        findViewById(R.id.card_profile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.nav_scan).setOnClickListener(v -> startActivity(new Intent(this, ScanActivity.class)));
        findViewById(R.id.nav_profile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.nav_settings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        carouselHandler.removeCallbacks(carouselRunnable);
    }
}