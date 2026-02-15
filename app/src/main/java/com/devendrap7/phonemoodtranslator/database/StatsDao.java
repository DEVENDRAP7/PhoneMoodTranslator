package com.devendrap7.phonemoodtranslator.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyStats stats);

    @Update
    void update(DailyStats stats);

    // ── SINGLE DAY ──
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    DailyStats getStatsByDate(String date);

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    LiveData<DailyStats> getStatsByDateLive(String date);

    // ── WEEKLY (last 7 days) — sorted by real timestamp ──
    @Query("SELECT * FROM daily_stats ORDER BY dateTimestamp DESC LIMIT 7")
    List<DailyStats> getWeeklyStats();

    // ── MONTHLY (last 30 days) ──
    @Query("SELECT * FROM daily_stats ORDER BY dateTimestamp DESC LIMIT 30")
    List<DailyStats> getMonthlyStats();

    // ── YEARLY / ALL TIME ──
    @Query("SELECT * FROM daily_stats ORDER BY dateTimestamp ASC LIMIT 365")
    List<DailyStats> getYearlyStats();

    // ── BY MONTH ──
    @Query("SELECT * FROM daily_stats WHERE month = :m AND year = :y ORDER BY dayOfMonth ASC")
    List<DailyStats> getStatsForMonth(int m, int y);

    // ── DELETE ──
    @Query("DELETE FROM daily_stats WHERE date = :dateStr")
    void deleteByDate(String dateStr);

    // ── NOTES ──
    @Query("UPDATE daily_stats SET selfNote = :note WHERE date = :date")
    int updateNote(String date, String note);

    // ✅ Fallback weekly query using date string sorting
    @Query("SELECT * FROM daily_stats ORDER BY year DESC, month DESC, dayOfMonth DESC LIMIT 7")
    List<DailyStats> getWeeklyStatsFallback();
}