package com.mittimitra;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.mittimitra.utils.AnalyticsHelper;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import androidx.work.ExistingPeriodicWorkPolicy;

public class MittiMitraApp extends Application {
    private static final String TAG = "MittiMitraApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Firebase
        FirebaseApp.initializeApp(this);

        // 2. Initialize Analytics
        AnalyticsHelper.init(this);

        // 3. Enable App Check (strict in release, optional in debug).
        if (BuildConfig.ENABLE_APP_CHECK) {
            try {
                FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
                if (BuildConfig.DEBUG) {
                    firebaseAppCheck.installAppCheckProviderFactory(
                            DebugAppCheckProviderFactory.getInstance());
                } else {
                    firebaseAppCheck.installAppCheckProviderFactory(
                            PlayIntegrityAppCheckProviderFactory.getInstance());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Firebase App Check", e);
            }
        } else {
            Log.i(TAG, "Firebase App Check disabled for this build variant.");
        }

        // 4. Enable Offline Persistence for Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings.Builder settingsBuilder = new FirebaseFirestoreSettings.Builder();
        try {
            Class<?> localCacheClass = Class.forName("com.google.firebase.firestore.LocalCacheSettings");
            Class<?> persistentCacheClass = Class.forName("com.google.firebase.firestore.PersistentCacheSettings");
            Object cacheBuilder = persistentCacheClass.getMethod("newBuilder").invoke(null);
            Object persistentCache = cacheBuilder.getClass().getMethod("build").invoke(cacheBuilder);
            settingsBuilder.getClass()
                    .getMethod("setLocalCacheSettings", localCacheClass)
                    .invoke(settingsBuilder, persistentCache);
        } catch (Throwable ignored) {
            // Backward-compatible fallback for older Firestore SDKs.
            settingsBuilder.setPersistenceEnabled(true);
        }
        db.setFirestoreSettings(settingsBuilder.build());

        // 5. Schedule Notifications
        PeriodicWorkRequest notifRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyAlerts",
                ExistingPeriodicWorkPolicy.KEEP,
                notifRequest);
    }
}
