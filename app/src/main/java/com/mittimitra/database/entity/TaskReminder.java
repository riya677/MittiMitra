package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_reminders")
public class TaskReminder {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "task_id")
    public long taskId;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "remind_at")
    public long remindAt;

    @ColumnInfo(name = "channel")
    public String channel;

    @ColumnInfo(name = "is_sent")
    public boolean isSent;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
