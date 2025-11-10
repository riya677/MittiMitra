package com.mittimitra.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.mittimitra.database.dao.DocumentDao;
import com.mittimitra.database.dao.SoilDao;
import com.mittimitra.database.entity.Document;
import com.mittimitra.database.entity.SoilAnalysis;

// UPDATED: Version is now 2
@Database(entities = {SoilAnalysis.class, Document.class}, version = 2, exportSchema = false)
public abstract class MittiMitraDatabase extends RoomDatabase {

    public abstract SoilDao soilDao();
    public abstract DocumentDao documentDao();

    private static volatile MittiMitraDatabase INSTANCE;

    public static MittiMitraDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MittiMitraDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MittiMitraDatabase.class, "mitti_mitra_database")
                            // THIS FIXES THE CRASH:
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}