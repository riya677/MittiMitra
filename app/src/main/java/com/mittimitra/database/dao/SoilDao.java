package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.mittimitra.database.entity.SoilAnalysis;
import java.util.List;

@Dao
public interface SoilDao {

    @Insert
    void insertAnalysis(SoilAnalysis analysis);

    @Delete
    void deleteAnalysis(SoilAnalysis analysis);

    @Query("SELECT * FROM soil_history ORDER BY timestamp DESC")
    List<SoilAnalysis> getAllHistory();

    @Query("SELECT * FROM soil_history ORDER BY timestamp DESC LIMIT 1")
    SoilAnalysis getLatestReport();

    // --- NEW ---
    @Query("DELETE FROM soil_history")
    void clearAllHistory();
}