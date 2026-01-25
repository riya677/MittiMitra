package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "chat_messages")
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "message_id")
    public long messageId;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "is_user")
    public boolean isUser;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
    
    @ColumnInfo(name = "user_id")
    public String userId;

    public ChatMessage() {}

    @Ignore
    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }
}
