package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.mittimitra.database.entity.CropSchedule;
import java.util.List;

@Dao
public interface CropDao {
    @Insert
    void insert(CropSchedule schedule);

    @Delete
    void delete(CropSchedule schedule);

    @Query("SELECT * FROM crop_schedules WHERE user_id = :userId ORDER BY timestamp DESC")
    List<CropSchedule> getAllByUserId(String userId);

    @Query("SELECT * FROM crop_schedules WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    List<CropSchedule> getRecentByUserId(String userId, int limit);

    @Query("SELECT * FROM crop_schedules WHERE id = :id")
    CropSchedule getById(long id);
}
