package com.devendrap7.phonemoodtranslator.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DailyStats.class}, version = 3,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract StatsDao statsDao();

    private static volatile AppDatabase INSTANCE;
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "mood_database")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()  // ✅ Keep this as safety net
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}