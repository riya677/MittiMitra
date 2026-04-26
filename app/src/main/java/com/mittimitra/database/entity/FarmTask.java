package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "farm_tasks")
public class FarmTask {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "field_id")
    public Long fieldId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "crop_name")
    public String cropName;

    @ColumnInfo(name = "stage")
    public String stage;

    @ColumnInfo(name = "source")
    public String source;

    @ColumnInfo(name = "due_at")
    public long dueAt;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "priority")
    public int priority;

    @ColumnInfo(name = "confidence")
    public Integer confidence;

    @ColumnInfo(name = "metadata_json")
    public String metadataJson;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "completed_at")
    public Long completedAt;
}
