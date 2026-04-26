package com.mittimitra.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.FarmTask;
import com.mittimitra.database.entity.Field;
import com.mittimitra.database.entity.TaskLog;
import com.mittimitra.database.entity.TaskReminder;
import com.mittimitra.domain.repository.TaskRepository;

import java.util.List;

public class RoomTaskRepository implements TaskRepository {

    private final MittiMitraDatabase database;

    public RoomTaskRepository(@NonNull MittiMitraDatabase database) {
        this.database = database;
    }

    @Override
    public long createField(@NonNull Field field) {
        return database.fieldDao().insert(field);
    }

    @Override
    public long createTask(@NonNull FarmTask task) {
        long taskId = database.farmTaskDao().insert(task);
        log(task.userId, taskId, "CREATED", task.title);
        return taskId;
    }

    @Override
    public void updateTask(@NonNull FarmTask task) {
        database.farmTaskDao().update(task);
        log(task.userId, task.id, "UPDATED", task.title);
    }

    @Override
    public void completeTask(long taskId, long completedAt) {
        FarmTask existing = database.farmTaskDao().getById(taskId);
        if (existing == null) return;
        database.farmTaskDao().markCompleted(taskId, completedAt);
        log(existing.userId, taskId, "COMPLETED", existing.title);
    }

    @Override
    public void upsertReminder(@NonNull TaskReminder reminder) {
        database.taskReminderDao().upsert(reminder);
    }

    @Override
    @NonNull
    public List<FarmTask> getTasksForUser(@NonNull String userId) {
        return database.farmTaskDao().getAllForUser(userId);
    }

    @Override
    @NonNull
    public List<FarmTask> getUpcomingTasks(@NonNull String userId, long now, long until) {
        return database.farmTaskDao().getUpcomingForUser(userId, now, until);
    }

    @Override
    @NonNull
    public List<TaskReminder> getUpcomingReminders(@NonNull String userId, long now, long until) {
        return database.taskReminderDao().getUpcomingReminders(userId, now, until);
    }

    @Override
    @NonNull
    public List<FarmTask> getTaskTemplatesByCropStage(@NonNull String cropName, @Nullable String stage, @NonNull String userId) {
        return database.farmTaskDao().getTaskTemplatesByCropStage(userId, cropName, stage);
    }

    private void log(String userId, long taskId, String action, String notes) {
        TaskLog log = new TaskLog();
        log.userId = userId;
        log.taskId = taskId;
        log.action = action;
        log.notes = notes;
        log.timestamp = System.currentTimeMillis();
        database.taskLogDao().insert(log);
    }
}
