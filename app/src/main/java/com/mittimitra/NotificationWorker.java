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
        
        // Load cached location from user's last scan
        SharedPreferences scanCache = context.getSharedPreferences("scan_cache", Context.MODE_PRIVATE);
        String locName = scanCache.getString("loc", null);
        
        // Load user's actual cached coordinates (cached during ScanActivity)
        SharedPreferences locPrefs = context.getSharedPreferences("user_location_cache", Context.MODE_PRIVATE);
        double lat = Double.parseDouble(locPrefs.getString("last_lat", "20.5937")); // Delhi fallback
        double lon = Double.parseDouble(locPrefs.getString("last_lon", "78.9629")); // Delhi fallback
        
        // Check permissions/settings
        boolean showWeather = prefs.getBoolean("notif_weather", true);

        if (showWeather) {
            checkWeatherAndNotify(context);
        }

        // Mandi notification with real cached prices
        if (prefs.getBoolean("notif_mandi", false)) {
            showMandiPriceNotification(context, locName);
        }

        // CHECK EXPIRING DOCUMENTS
        checkExpiringDocuments(context);

        return Result.success();
    }
    
    private void showMandiPriceNotification(Context context, String locName) {
        try {
            // Try to load cached mandi prices
            SharedPreferences mandiCache = context.getSharedPreferences("mandi_price_cache", Context.MODE_PRIVATE);
            String cachedJson = mandiCache.getString("last_prices", null);
            
            if (cachedJson != null && !cachedJson.isEmpty()) {
                // Parse cached prices
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<com.mittimitra.MandiActivity.MandiPrice>>(){}.getType();
                java.util.List<com.mittimitra.MandiActivity.MandiPrice> prices = new com.google.gson.Gson().fromJson(cachedJson, listType);
                
                if (prices != null && !prices.isEmpty()) {
                    // Show first 2-3 prices in notification
                    StringBuilder priceMsg = new StringBuilder();
                    int count = Math.min(prices.size(), 3);
                    for (int i = 0; i < count; i++) {
                        com.mittimitra.MandiActivity.MandiPrice p = prices.get(i);
                        if (i > 0) priceMsg.append(" • ");
                        priceMsg.append(p.market).append(": ₹").append(p.modal);
                    }
                    
                    String lastCommodity = mandiCache.getString("last_commodity", "");
                    String title = context.getString(R.string.notif_mandi_title) + (!lastCommodity.isEmpty() ? " - " + lastCommodity : "");
                    triggerNotification(2, title, priceMsg.toString());
                    return;
                }
            }
            
            // Fallback: if no cache, show prompt to check prices
            triggerNotification(2, context.getString(R.string.notif_mandi_title), 
                "Tap to check today's mandi prices for " + (locName != null ? locName : "your area"));
        } catch (Exception e) {
            android.util.Log.e("NotificationWorker", "Error showing mandi notification", e);
        }
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
            // Load user's cached location
            SharedPreferences locPrefs = context.getSharedPreferences("user_location_cache", Context.MODE_PRIVATE);
            double lat = Double.parseDouble(locPrefs.getString("last_lat", "20.5937"));
            double lon = Double.parseDouble(locPrefs.getString("last_lon", "78.9629"));
            
            com.google.gson.JsonObject response = com.mittimitra.network.RetrofitClient.getWeatherService()
                .getAgroWeather(lat, lon) // Use cached location
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
