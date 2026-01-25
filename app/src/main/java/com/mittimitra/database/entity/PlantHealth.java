package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plant_health")
public class PlantHealth {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "image_path")
    public String imagePath; // Local URI to the saved image

    @ColumnInfo(name = "crop_name")
    public String cropName;

    @ColumnInfo(name = "health_status")
    public String healthStatus; // Healthy, Diseased, etc.

    @ColumnInfo(name = "diagnosis")
    public String diagnosis; // Short summary of issues

    @ColumnInfo(name = "confidence")
    public int confidence;

    @ColumnInfo(name = "full_json")
    public String fullJson; // Full details including detailed recommendations

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
