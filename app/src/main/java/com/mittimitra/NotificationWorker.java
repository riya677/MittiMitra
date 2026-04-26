package com.mittimitra;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mittimitra.data.repository.RoomTaskRepository;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.Document;
import com.mittimitra.database.entity.FarmTask;
import com.mittimitra.database.entity.TaskReminder;
import com.mittimitra.network.RetrofitClient;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.success();
        }

        SharedPreferences prefs = context.getSharedPreferences("MittiMitra_Notifications", Context.MODE_PRIVATE);
        SharedPreferences scanCache = context.getSharedPreferences("scan_cache", Context.MODE_PRIVATE);
        String locName = scanCache.getString("loc", null);

        if (prefs.getBoolean("notif_weather", true)) {
            checkWeatherAndNotify(context);
        }

        if (prefs.getBoolean("notif_mandi", true)) {
            showMandiPriceNotification(context, locName);
        }

        checkExpiringDocuments(context, user.getUid());
        checkTaskReminders(context, user.getUid());

        return Result.success();
    }

    private void showMandiPriceNotification(Context context, String locName) {
        try {
            SharedPreferences mandiCache = context.getSharedPreferences("mandi_price_cache", Context.MODE_PRIVATE);
            String cachedJson = mandiCache.getString("last_prices", null);

            if (cachedJson != null && !cachedJson.isEmpty()) {
                Type listType = new TypeToken<List<MandiActivity.MandiPrice>>() {
                }.getType();
                List<MandiActivity.MandiPrice> prices = new Gson().fromJson(cachedJson, listType);

                if (prices != null && !prices.isEmpty()) {
                    StringBuilder priceMsg = new StringBuilder();
                    int count = Math.min(prices.size(), 3);
                    for (int i = 0; i < count; i++) {
                        MandiActivity.MandiPrice p = prices.get(i);
                        if (i > 0) priceMsg.append(" - ");
                        priceMsg.append(p.market).append(": Rs ").append(p.modal);
                    }

                    String lastCommodity = mandiCache.getString("last_commodity", "");
                    String title = context.getString(R.string.notif_mandi_title)
                            + (!lastCommodity.isEmpty() ? " - " + lastCommodity : "");
                    triggerNotification(2, title, priceMsg.toString());
                    return;
                }
            }

            triggerNotification(2, context.getString(R.string.notif_mandi_title),
                    context.getString(R.string.notif_mandi_check_prices));
        } catch (Exception e) {
            android.util.Log.e("NotificationWorker", "Error showing mandi notification", e);
        }
    }

    private void checkExpiringDocuments(Context context, String userId) {
        long now = System.currentTimeMillis();
        long nextWeek = now + (7 * 24 * 60 * 60 * 1000L);

        List<Document> expiringDocs = MittiMitraDatabase.getDatabase(context)
                .documentDao()
                .getExpiringDocumentsForUser(userId, now, nextWeek);

        if (expiringDocs == null || expiringDocs.isEmpty()) return;

        for (Document doc : expiringDocs) {
            int notifId = (int) (doc.documentId % (Integer.MAX_VALUE - 100)) + 100;
            triggerNotification(notifId,
                    context.getString(R.string.notif_doc_expiring_title),
                    context.getString(R.string.notif_doc_expiring_body, doc.documentName));
        }
    }

    private void checkTaskReminders(Context context, String userId) {
        long now = System.currentTimeMillis();
        long nextDay = now + (24 * 60 * 60 * 1000L);

        RoomTaskRepository taskRepository = new RoomTaskRepository(MittiMitraDatabase.getDatabase(context));
        List<TaskReminder> reminders = taskRepository.getUpcomingReminders(userId, now, nextDay);
        if (reminders == null || reminders.isEmpty()) return;

        for (TaskReminder reminder : reminders) {
            FarmTask task = MittiMitraDatabase.getDatabase(context).farmTaskDao().getById(reminder.taskId);
            if (task == null || "COMPLETED".equalsIgnoreCase(task.status)) {
                MittiMitraDatabase.getDatabase(context).taskReminderDao().markSent(reminder.id);
                continue;
            }

            int id = (int) ((reminder.id % 100000) + 2000);
            triggerNotification(id,
                    context.getString(R.string.task_reminder_title),
                    context.getString(R.string.task_reminder_body, task.title));
            MittiMitraDatabase.getDatabase(context).taskReminderDao().markSent(reminder.id);
        }
    }

    private void checkWeatherAndNotify(Context context) {
        try {
            SharedPreferences locPrefs = context.getSharedPreferences("user_location_cache", Context.MODE_PRIVATE);
            double lat = Double.parseDouble(locPrefs.getString("last_lat", "20.5937"));
            double lon = Double.parseDouble(locPrefs.getString("last_lon", "78.9629"));

            com.google.gson.JsonObject response = RetrofitClient.getWeatherService()
                    .getAgroWeather(lat, lon)
                    .execute()
                    .body();

            if (response == null) return;

            double temp = response.getAsJsonObject("current").get("temperature_2m").getAsDouble();
            int humidity = response.getAsJsonObject("current").get("relative_humidity_2m").getAsInt();

            try {
                if (response.has("daily")) {
                    com.google.gson.JsonObject daily = response.getAsJsonObject("daily");
                    if (daily.has("sunrise") && daily.has("sunset")) {
                        String sunriseStr = daily.getAsJsonArray("sunrise").get(0).getAsString();
                        String sunsetStr = daily.getAsJsonArray("sunset").get(0).getAsString();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
                        AppPreferences appPrefs = new AppPreferences(context);
                        java.util.Date sunrise = sdf.parse(sunriseStr);
                        java.util.Date sunset = sdf.parse(sunsetStr);
                        if (sunrise != null) {
                            appPrefs.setLastSunrise(sunrise.getTime());
                        }
                        if (sunset != null) {
                            appPrefs.setLastSunset(sunset.getTime());
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("NotificationWorker", "Failed to parse sunrise/sunset", e);
            }

            String msg = "Current: " + Math.round(temp) + " C, " + humidity + "% humidity.";
            if (humidity > 80) msg += " " + context.getString(R.string.weather_high_humidity);
            else if (temp > 35) msg += " " + context.getString(R.string.weather_high_heat);

            triggerNotification(1, context.getString(R.string.weather_alert_title), msg);
        } catch (Exception e) {
            android.util.Log.e("NotificationWorker", "Weather check failed", e);
        }
    }

    private void triggerNotification(int id, String title, String message) {
        Context context = getApplicationContext();
        if (!canPostNotifications(context)) {
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        boolean isDarkMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Alerts about weather, market prices, documents, and tasks");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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

    private boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
