package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// UPDATED: Removed the foreignKey and user ID
@Entity(tableName = "soil_history")
public class SoilAnalysis {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "analysis_id")
    public long analysisId;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "soil_report_json")
    public String soilReportJson;
    
    @ColumnInfo(name = "user_id")
    public String userId;
}