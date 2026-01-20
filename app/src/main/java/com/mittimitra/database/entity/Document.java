package com.mittimitra.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// UPDATED: Removed the foreignKey and user ID
@Entity(tableName = "documents")
public class Document {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "document_id")
    public long documentId;

    @ColumnInfo(name = "document_name")
    public String documentName;

    @ColumnInfo(name = "internal_file_path")
    public String internalFilePath;

    @ColumnInfo(name = "document_type")
    public String documentType;
    
    @ColumnInfo(name = "expiry_date")
    public Long expiryDate; // Timestamp in milliseconds, null if no expiry
}