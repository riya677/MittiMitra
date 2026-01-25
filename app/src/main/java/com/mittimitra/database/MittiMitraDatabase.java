package com.mittimitra.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.mittimitra.database.dao.ChatDao;
import com.mittimitra.database.dao.DocumentDao;
import com.mittimitra.database.dao.SoilDao;
import com.mittimitra.database.entity.ChatMessage;
import com.mittimitra.database.entity.Document;
import com.mittimitra.database.entity.SoilAnalysis;

/**
 * Room Database for MittiMitra app.
 * 
 * Migration History:
 * - v1 → v2: Removed user ID and foreign key constraints from tables
 *            (This was a breaking change, used destructive migration)
 * - v2 → v3: Added chat_messages table for chat history persistence
 * 
 * For future migrations, add proper migration objects below.
 */
@Database(entities = {SoilAnalysis.class, Document.class, ChatMessage.class}, version = 5, exportSchema = false)
public abstract class MittiMitraDatabase extends RoomDatabase {

    public abstract SoilDao soilDao();
    public abstract DocumentDao documentDao();
    public abstract ChatDao chatDao();

    private static volatile MittiMitraDatabase INSTANCE;

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                    "`message_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`content` TEXT, " +
                    "`is_user` INTEGER NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `documents` ADD COLUMN `expiry_date` INTEGER");
        }
    };
    
    /**
     * Migration from version 4 to 5.
     * Adds user_id column to soil_history table for data isolation.
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `soil_history` ADD COLUMN `user_id` TEXT");
        }
    };

    public static MittiMitraDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MittiMitraDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MittiMitraDatabase.class, "mitti_mitra_database")
                            .fallbackToDestructiveMigrationFrom(1)
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
