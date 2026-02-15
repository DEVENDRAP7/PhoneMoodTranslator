package com.devendrap7.phonemoodtranslator.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DailyStats.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {

    public abstract StatsDao statsDao();

    private static volatile AppDatabase INSTANCE;

    // ✅ Migration from version 1 to 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN dayOfMonth INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN dateTimestamp INTEGER NOT NULL DEFAULT 0");
        }
    };

    // ✅ Migration from version 2 to 3
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // ✅ Add all 3 missing columns
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN dayOfMonth INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN dateTimestamp INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN hourlyDataJson TEXT");
        }
    };

    // ✅ Migration from version 1 to 3 directly
    // (for users who never had version 2)
    static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN dayOfMonth INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN dateTimestamp INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE daily_stats ADD COLUMN hourlyDataJson TEXT");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "mood_database")
                            .allowMainThreadQueries()
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_1_3  // ✅ handles direct 1→3 jump
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}