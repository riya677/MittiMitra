package com.mittimitra.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mittimitra.backend.model.AiModels;
import com.mittimitra.data.repository.RoomTaskRepository;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.FarmTask;
import com.mittimitra.database.entity.TaskReminder;

import java.util.List;

public final class TaskSuggestionEngine {

    private TaskSuggestionEngine() {
    }

    public static void suggestFromCropSchedule(@NonNull Context context,
                                               @NonNull String userId,
                                               @NonNull String cropName,
                                               @Nullable AiModels.CropScheduleData scheduleData) {
        RoomTaskRepository repo = new RoomTaskRepository(MittiMitraDatabase.getDatabase(context));
        long now = System.currentTimeMillis();

        if (scheduleData != null && scheduleData.schedule != null && !scheduleData.schedule.isEmpty()) {
            for (AiModels.ScheduleItem item : scheduleData.schedule) {
                if (item == null || item.activity == null || item.activity.trim().isEmpty()) continue;
                FarmTask task = new FarmTask();
                task.userId = userId;
                task.cropName = cropName;
                task.title = item.activity;
                task.description = item.tips != null ? item.tips : "";
                task.stage = "schedule";
                task.source = "crop_schedule";
                long dueAt = now + (Math.max(1, item.week != null ? item.week : 1) * 7L * 24L * 60L * 60L * 1000L);
                task.dueAt = dueAt;
                task.status = "PENDING";
                task.priority = 2;
                task.confidence = scheduleData.confidence;
                task.createdAt = now;

                long taskId = repo.createTask(task);
                createReminder(repo, userId, taskId, dueAt - (6L * 60L * 60L * 1000L));
            }
            return;
        }

        List<FarmTask> defaults = FarmTaskTemplates.forCrop(userId, cropName, now);
        for (FarmTask task : defaults) {
            long taskId = repo.createTask(task);
            createReminder(repo, userId, taskId, task.dueAt - (6L * 60L * 60L * 1000L));
        }
    }

    public static void suggestFromPlantDiagnosis(@NonNull Context context,
                                                 @NonNull String userId,
                                                 @NonNull String cropName,
                                                 @Nullable String healthStatus,
                                                 @Nullable String issues,
                                                 @Nullable Integer confidence) {
        if (healthStatus == null) return;
        if ("Healthy".equalsIgnoreCase(healthStatus)) return;

        RoomTaskRepository repo = new RoomTaskRepository(MittiMitraDatabase.getDatabase(context));
        long now = System.currentTimeMillis();

        FarmTask task = new FarmTask();
        task.userId = userId;
        task.cropName = cropName;
        task.title = "Disease Follow-up Check";
        task.description = issues != null && !issues.isEmpty()
                ? "Inspect affected plants and apply remedy: " + issues
                : "Inspect plants and apply recommended treatment.";
        task.stage = "health";
        task.source = "plant_diagnosis";
        task.dueAt = now + (24L * 60L * 60L * 1000L);
        task.status = "PENDING";
        task.priority = 1;
        task.confidence = confidence;
        task.createdAt = now;

        long taskId = repo.createTask(task);
        createReminder(repo, userId, taskId, now + (12L * 60L * 60L * 1000L));
    }

    public static void suggestFromSoilAdvisory(@NonNull Context context,
                                               @NonNull String userId,
                                               @Nullable String cropName,
                                               int nitrogen,
                                               int phosphorus,
                                               int potassium,
                                               double ph,
                                               @Nullable Integer confidence) {
        RoomTaskRepository repo = new RoomTaskRepository(MittiMitraDatabase.getDatabase(context));
        long now = System.currentTimeMillis();
        String crop = (cropName == null || cropName.trim().isEmpty()) ? "General" : cropName;

        if (nitrogen < 200 || phosphorus < 12 || potassium < 120 || ph < 6.0 || ph > 7.8) {
            FarmTask task = new FarmTask();
            task.userId = userId;
            task.cropName = crop;
            task.title = "Soil Correction Task";
            task.description = "Apply recommended nutrients and pH correction from advisory report.";
            task.stage = "soil";
            task.source = "soil_advisory";
            task.dueAt = now + (48L * 60L * 60L * 1000L);
            task.status = "PENDING";
            task.priority = 1;
            task.confidence = confidence;
            task.createdAt = now;

            long taskId = repo.createTask(task);
            createReminder(repo, userId, taskId, task.dueAt - (12L * 60L * 60L * 1000L));
        }
    }

    private static void createReminder(RoomTaskRepository repo, String userId, long taskId, long remindAt) {
        TaskReminder reminder = new TaskReminder();
        reminder.taskId = taskId;
        reminder.userId = userId;
        reminder.remindAt = Math.max(remindAt, System.currentTimeMillis() + (60L * 1000L));
        reminder.channel = "app_notification";
        reminder.isSent = false;
        reminder.createdAt = System.currentTimeMillis();
        repo.upsertReminder(reminder);
    }
}
