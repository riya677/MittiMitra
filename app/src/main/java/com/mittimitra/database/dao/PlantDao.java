package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.mittimitra.database.entity.PlantHealth;
import java.util.List;

@Dao
public interface PlantDao {
    @Insert
    void insert(PlantHealth health);

    @Delete
    void delete(PlantHealth health);

    @Query("SELECT * FROM plant_health WHERE user_id = :userId ORDER BY timestamp DESC")
    List<PlantHealth> getAllByUserId(String userId);

    @Query("SELECT * FROM plant_health WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    List<PlantHealth> getRecentByUserId(String userId, int limit);

    @Query("SELECT * FROM plant_health WHERE id = :id")
    PlantHealth getById(long id);
}
