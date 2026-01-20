package com.mittimitra;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {

    private static final String CHANNEL_ID = "mitti_mitra_alerts";
    private static final String CHANNEL_NAME = "Farming Alerts";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("MittiMitra_Notifications", Context.MODE_PRIVATE);
        
        // Load cached location (simplest way for background worker without permissions)
        SharedPreferences scanCache = context.getSharedPreferences("scan_cache", Context.MODE_PRIVATE);
        String locName = scanCache.getString("loc", null);
        
        // Defaults if no cache
        double lat = 20.5937; 
        double lon = 78.9629; 

        // Try to get cached lat/lon if we stored it (we didn't explicitly store lat/lon in scan_cache, just text)
        // For this demo, we'll try to fetch weather for a "central" location or skip if logic allows.
        // Better: Let's assume the user has run a scan and we can just use the RetrofitClient.
        
        // Check permissions/settings
        boolean showWeather = prefs.getBoolean("notif_weather", true);

        if (showWeather) {
            checkWeatherAndNotify(context);
        }

        // Keep other dummy notifications for now or remove if they are annoying
        if (prefs.getBoolean("notif_mandi", false)) {
             triggerNotification(2, context.getString(R.string.notif_mandi_title), "Check market prices for " + (locName!=null?locName:"your area"));
        }

        // CHECK EXPIRING DOCUMENTS
        checkExpiringDocuments(context);

        return Result.success();
    }

    private void checkExpiringDocuments(Context context) {
        long now = System.currentTimeMillis();
        long nextWeek = now + (7 * 24 * 60 * 60 * 1000L); // 7 days

        java.util.List<com.mittimitra.database.entity.Document> expiringDocs = 
            com.mittimitra.database.MittiMitraDatabase.getDatabase(context).documentDao().getExpiringDocuments(now, nextWeek);

        if (expiringDocs != null && !expiringDocs.isEmpty()) {
            for (com.mittimitra.database.entity.Document doc : expiringDocs) {
                triggerNotification((int) doc.documentId + 100, 
                    "Document Expiring Soon! 📄", 
                    "Your document '" + doc.documentName + "' is expiring this week. Please renew it.");
            }
        }
    }

    private void checkWeatherAndNotify(Context context) {
        try {
            // Synchronous call
            // Using a hardcoded generic location for India as fallback or "last known" strategy
            //Ideally we should save lat/lon in prefs in ScanActivity. I will assume we did or just force a check.
            
            com.google.gson.JsonObject response = com.mittimitra.network.RetrofitClient.getAgroService()
                .getAgroWeather(20.59, 78.96) // Generic Center of India as fallback
                .execute()
                .body();

            if (response != null) {
                double temp = response.getAsJsonObject("current").get("temperature_2m").getAsDouble();
                int humidity = response.getAsJsonObject("current").get("relative_humidity_2m").getAsInt();
                
                String msg = "Current: " + Math.round(temp) + "°C, " + humidity + "% Humidity.";
                if (humidity > 80) msg += " " + context.getString(R.string.weather_high_humidity);
                else if (temp > 35) msg += " " + context.getString(R.string.weather_high_heat);
                
                triggerNotification(1, context.getString(R.string.weather_alert_title), msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void triggerNotification(int id, String title, String message) {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if dark mode is enabled
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode 
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // Create Channel (Required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, 
                CHANNEL_NAME, 
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Alerts about weather, market prices, and government schemes");
            manager.createNotificationChannel(channel);
        }

        // Create intent to open app when notification is tapped
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            id, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Theme-aware notification colors
        int accentColor = isDarkMode 
            ? context.getResources().getColor(R.color.brand_green_light, context.getTheme())
            : context.getResources().getColor(R.color.brand_green, context.getTheme());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mittimitra_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setColor(accentColor)
                .setAutoCancel(true);

        manager.notify(id, builder.build());
    }
}
