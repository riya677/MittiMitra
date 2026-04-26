package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mittimitra.database.entity.TaskReminder;

import java.util.List;

@Dao
public interface TaskReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(TaskReminder reminder);

    @Query("SELECT * FROM task_reminders WHERE user_id = :userId AND is_sent = 0 AND remind_at BETWEEN :now AND :until ORDER BY remind_at ASC")
    List<TaskReminder> getUpcomingReminders(String userId, long now, long until);

    @Query("UPDATE task_reminders SET is_sent = 1 WHERE id = :reminderId")
    void markSent(long reminderId);

    @Query("DELETE FROM task_reminders WHERE task_id = :taskId")
    void deleteForTask(long taskId);
}
