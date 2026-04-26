package com.mittimitra.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mittimitra.database.entity.Field;

import java.util.List;

@Dao
public interface FieldDao {
    @Insert
    long insert(Field field);

    @Update
    void update(Field field);

    @Delete
    void delete(Field field);

    @Query("SELECT * FROM fields WHERE user_id = :userId ORDER BY updated_at DESC")
    List<Field> getAllForUser(String userId);

    @Query("SELECT * FROM fields WHERE id = :id")
    Field getById(long id);
}
