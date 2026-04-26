package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "fields")
public class Field {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "field_name")
    public String fieldName;

    @ColumnInfo(name = "crop_name")
    public String cropName;

    @ColumnInfo(name = "area_hectares")
    public double areaHectares;

    @ColumnInfo(name = "location_label")
    public String locationLabel;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
