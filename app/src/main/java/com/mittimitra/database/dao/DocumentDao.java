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

    @Query("SELECT * FROM documents WHERE user_id = :userId")
    List<Document> getDocumentsForUser(String userId);

    // --- NEW ---
    @Query("DELETE FROM documents")
    void clearAllDocuments();

    @Query("DELETE FROM documents WHERE user_id = :userId")
    void clearDocumentsForUser(String userId);

    // --- NEW ---
    @Query("SELECT * FROM documents WHERE expiry_date BETWEEN :now AND :threshold")
    List<Document> getExpiringDocuments(long now, long threshold);

    @Query("SELECT * FROM documents WHERE user_id = :userId AND expiry_date BETWEEN :now AND :threshold")
    List<Document> getExpiringDocumentsForUser(String userId, long now, long threshold);
}
