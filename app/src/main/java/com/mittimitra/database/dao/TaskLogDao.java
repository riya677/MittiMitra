package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mittimitra.database.entity.TaskLog;

import java.util.List;

@Dao
public interface TaskLogDao {
    @Insert
    long insert(TaskLog taskLog);

    @Query("SELECT * FROM task_logs WHERE user_id = :userId ORDER BY timestamp DESC")
    List<TaskLog> getLogsForUser(String userId);
}
