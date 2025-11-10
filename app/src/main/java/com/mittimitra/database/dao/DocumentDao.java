package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.mittimitra.database.entity.Document;
import java.util.List;

@Dao
public interface DocumentDao {

    @Insert
    void insertDocument(Document document);

    @Delete
    void deleteDocument(Document document);

    @Query("SELECT * FROM documents")
    List<Document> getAllDocuments();

    // --- NEW ---
    @Query("DELETE FROM documents")
    void clearAllDocuments();
}