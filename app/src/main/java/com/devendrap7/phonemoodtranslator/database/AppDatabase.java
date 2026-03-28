package com.devendrap7.phonemoodtranslator.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DailyStats.class}, version = 7)  // ✅ Changed to version 7
public abstract class AppDatabase extends RoomDatabase {

    public abstract StatsDao statsDao();

    private static AppDatabase instance;

    // ✅ NEW MIGRATION
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE daily_stats ADD COLUMN hourlyAppsJson TEXT");
        }
    };

    public static AppDatabase getDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, AppDatabase.class, "stats_db")
                    .addMigrations(MIGRATION_6_7)  // ✅ ADD THIS
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
