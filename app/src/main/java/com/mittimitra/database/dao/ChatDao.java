package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.mittimitra.database.entity.ChatMessage;
import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    void insertMessage(ChatMessage message);

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAllMessages();

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    List<ChatMessage> getRecentMessages(int limit);

    @Query("SELECT * FROM chat_messages WHERE user_id = :userId ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesForUser(String userId);

    @Query("DELETE FROM chat_messages")
    void clearAllMessages();

    @Query("SELECT COUNT(*) FROM chat_messages")
    int getMessageCount();
}
