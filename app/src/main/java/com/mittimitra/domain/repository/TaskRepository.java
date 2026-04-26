package com.mittimitra.domain.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mittimitra.database.entity.FarmTask;
import com.mittimitra.database.entity.Field;
import com.mittimitra.database.entity.TaskReminder;

import java.util.List;

public interface TaskRepository {
    long createField(@NonNull Field field);

    long createTask(@NonNull FarmTask task);

    void updateTask(@NonNull FarmTask task);

    void completeTask(long taskId, long completedAt);

    void upsertReminder(@NonNull TaskReminder reminder);

    @NonNull
    List<FarmTask> getTasksForUser(@NonNull String userId);

    @NonNull
    List<FarmTask> getUpcomingTasks(@NonNull String userId, long now, long until);

    @NonNull
    List<TaskReminder> getUpcomingReminders(@NonNull String userId, long now, long until);

    @NonNull
    List<FarmTask> getTaskTemplatesByCropStage(@NonNull String cropName, @Nullable String stage, @NonNull String userId);
}
