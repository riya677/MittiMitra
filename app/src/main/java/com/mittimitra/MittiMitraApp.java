package com.mittimitra;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import androidx.work.ExistingPeriodicWorkPolicy;

public class MittiMitraApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Firebase
        FirebaseApp.initializeApp(this);

        // 2. Enable App Check
        // CRITICAL FIX: Use Debug Factory for development to allow OTPs to work
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();

        // Check if we are in Debug mode (simplified check)
        // For production, you would switch this to PlayIntegrity
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());

        // 3. Enable Offline Persistence for Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // 4. Schedule Notifications (Runs every 15 minutes for demo purposes)
        PeriodicWorkRequest notifRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyAlerts",
                ExistingPeriodicWorkPolicy.KEEP,
                notifRequest);
    }
}