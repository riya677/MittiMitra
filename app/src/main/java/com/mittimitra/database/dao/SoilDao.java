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
    List<SoilAnalysis> getAllSoilAnalysis();

    @Query("SELECT * FROM soil_history ORDER BY timestamp DESC LIMIT 1")
    SoilAnalysis getLatestReport();

    @Query("SELECT * FROM soil_history WHERE user_id = :userId ORDER BY timestamp DESC LIMIT 1")
    SoilAnalysis getLatestReportForUser(String userId);

    // NEW: Fetch specific report by ID
    @Query("SELECT * FROM soil_history WHERE analysis_id = :id")
    SoilAnalysis getAnalysisById(int id);

    @Query("SELECT * FROM soil_history WHERE user_id = :userId ORDER BY timestamp DESC")
    List<SoilAnalysis> getAnalysisForUser(String userId);

    @Query("SELECT * FROM soil_history WHERE user_id=:uid AND timestamp >= :since ORDER BY timestamp DESC")
    List<SoilAnalysis> getAnalysisSince(String uid, long since);

    @Query("DELETE FROM soil_history")
    void clearAllHistory();

    @Query("DELETE FROM soil_history WHERE user_id = :userId")
    void clearHistoryForUser(String userId);

    @Query("SELECT COUNT(*) FROM soil_history WHERE user_id = :userId")
    int getCountForUser(String userId);
}
