package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crop_schedules")
public class CropSchedule {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "crop_name")
    public String cropName;

    @ColumnInfo(name = "planting_date")
    public String plantingDate;

    @ColumnInfo(name = "harvest_date")
    public String harvestDate;

    // Storing the full JSON allows us to reconstruct the detailed view later
    @ColumnInfo(name = "full_json")
    public String fullJson;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
