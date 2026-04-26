package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mittimitra.database.entity.FarmTask;

import java.util.List;

@Dao
public interface FarmTaskDao {
    @Insert
    long insert(FarmTask task);

    @Update
    void update(FarmTask task);

    @Query("SELECT * FROM farm_tasks WHERE id = :taskId")
    FarmTask getById(long taskId);

    @Query("SELECT * FROM farm_tasks WHERE user_id = :userId ORDER BY due_at ASC")
    List<FarmTask> getAllForUser(String userId);

    @Query("SELECT * FROM farm_tasks WHERE user_id = :userId AND status != 'COMPLETED' AND due_at BETWEEN :now AND :until ORDER BY due_at ASC")
    List<FarmTask> getUpcomingForUser(String userId, long now, long until);

    @Query("UPDATE farm_tasks SET status = 'COMPLETED', completed_at = :completedAt WHERE id = :taskId")
    void markCompleted(long taskId, long completedAt);

    @Query("SELECT * FROM farm_tasks WHERE user_id = :userId AND crop_name = :cropName AND (:stage IS NULL OR stage = :stage) ORDER BY due_at ASC")
    List<FarmTask> getTaskTemplatesByCropStage(String userId, String cropName, String stage);
}
