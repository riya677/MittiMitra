package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_logs")
public class TaskLog {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "task_id")
    public long taskId;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "action")
    public String action;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
