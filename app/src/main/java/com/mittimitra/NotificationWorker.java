package com.mittimitra;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("MittiMitra_Notifications", Context.MODE_PRIVATE);

        // Check user settings
        boolean showWeather = prefs.getBoolean("notif_weather", true);
        boolean showMandi = prefs.getBoolean("notif_mandi", true);

        if (showWeather) {
            triggerNotification(1, "Weather Alert", "Heavy rain expected tomorrow. Secure your crops.");
        }

        if (showMandi) {
            // Slight delay so they don't pop at exact same millisecond
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            triggerNotification(2, "Mandi Prices", "Wheat prices are up by 5% in your district today!");
        }

        return Result.success();
    }

    private void triggerNotification(int id, String title, String message) {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "mitti_mitra_alerts";

        // Create Channel (Required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Farming Alerts", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Use your app icon here ideally
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(id, builder.build());
    }
}