package com.devendrap7.phonemoodtranslator.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_stats")
public class DailyStats {

    @PrimaryKey
    @NonNull
    public String date; // Format: "11 Feb 2026"

    public int month;
    public int year;
    public int dayOfMonth;       // ✅ NEW — for proper sorting
    public long dateTimestamp;   // ✅ NEW — for proper sorting
    public int totalCount;
    public long totalUsageTime;
    public int unlockCount;
    public String moodEmoji;
    public String moodTitle;
    public String topAppsJson;
    public String selfNote;
    public String hourlyDataJson; // ✅ 24 values, mins per hour
    public String hourlyAppsJson; // ✅ NEW — {"0": [{"name":"Instagram","mins":15},...], "1": [...], ...}

    public DailyStats(@NonNull String date, int month, int year,
                      int dayOfMonth, long dateTimestamp,
                      int totalCount, long totalUsageTime,
                      int unlockCount, String moodEmoji, String moodTitle,
                      String topAppsJson, String selfNote) {
        this.date          = date;
        this.month         = month;
        this.year          = year;
        this.dayOfMonth    = dayOfMonth;
        this.dateTimestamp = dateTimestamp;
        this.totalCount    = totalCount;
        this.totalUsageTime = totalUsageTime;
        this.unlockCount   = unlockCount;
        this.moodEmoji     = moodEmoji;
        this.moodTitle     = moodTitle;
        this.topAppsJson   = topAppsJson;
        this.selfNote      = selfNote;
        this.hourlyDataJson = null; // filled later
        this.hourlyAppsJson = null; // ✅ NEW — filled later
    }
}