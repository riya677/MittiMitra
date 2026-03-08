package com.mittimitra.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.mittimitra.database.dao.ChatDao;
import com.mittimitra.database.dao.CropDao;
import com.mittimitra.database.dao.DocumentDao;
import com.mittimitra.database.dao.PlantDao;
import com.mittimitra.database.dao.SoilDao;
import com.mittimitra.database.entity.ChatMessage;
import com.mittimitra.database.entity.CropSchedule;
import com.mittimitra.database.entity.Document;
import com.mittimitra.database.entity.PlantHealth;
import com.mittimitra.database.entity.SoilAnalysis;

/**
 * Room Database for MittiMitra app.
 * 
 * Migration History:
 * - v1 → v2: Removed user ID and foreign key constraints (destructive migration)
 * - v2 → v3: Added chat_messages table
 * - v3 → v4: Added expiry_date to documents
 * - v4 → v5: Added user_id to soil_history, documents, chat_messages
 * - v5 → v6: Added crop_schedules table (CropCalendarActivity)
 * - v6 → v7: Added plant_health table (PlantScanActivity)
 *
 * For future migrations, add a new Migration object and include it in addMigrations().
 */
@Database(entities = {SoilAnalysis.class, Document.class, ChatMessage.class, CropSchedule.class, PlantHealth.class}, version = 7, exportSchema = false)
public abstract class MittiMitraDatabase extends RoomDatabase {

    public abstract SoilDao soilDao();
    public abstract DocumentDao documentDao();
    public abstract ChatDao chatDao();
    public abstract CropDao cropDao();
    public abstract PlantDao plantDao();

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
     * Adds user_id column to soil_history, documents, and chat_messages for data isolation.
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `soil_history` ADD COLUMN `user_id` TEXT");
            database.execSQL("ALTER TABLE `documents` ADD COLUMN `user_id` TEXT");
            database.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `user_id` TEXT");
        }
    };

    /**
     * Migration from version 5 to 6.
     * Adds crop_schedules table for CropCalendarActivity.
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `crop_schedules` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`user_id` TEXT, " +
                    "`crop_name` TEXT, " +
                    "`planting_date` TEXT, " +
                    "`harvest_date` TEXT, " +
                    "`full_json` TEXT, " +
                    "`timestamp` INTEGER NOT NULL DEFAULT 0)");
        }
    };

    /**
     * Migration from version 6 to 7.
     * Adds plant_health table for PlantScanActivity.
     */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `plant_health` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`user_id` TEXT, " +
                    "`image_path` TEXT, " +
                    "`crop_name` TEXT, " +
                    "`health_status` TEXT, " +
                    "`diagnosis` TEXT, " +
                    "`confidence` INTEGER NOT NULL DEFAULT 0, " +
                    "`full_json` TEXT, " +
                    "`timestamp` INTEGER NOT NULL DEFAULT 0)");
        }
    };

    public static MittiMitraDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MittiMitraDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MittiMitraDatabase.class, "mitti_mitra_database")
                            .addMigrations(
                                    MIGRATION_2_3, MIGRATION_3_4,
                                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
