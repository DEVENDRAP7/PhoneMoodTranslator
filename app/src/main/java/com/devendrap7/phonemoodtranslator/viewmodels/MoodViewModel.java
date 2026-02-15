package com.devendrap7.phonemoodtranslator.viewmodels;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MoodViewModel extends ViewModel {

    private AppDatabase db;
    private LiveData<DailyStats> todayStats;

    public void init(Context context) {
        if (db == null) {
            db = AppDatabase.getDatabase(context.getApplicationContext());
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
            String todayDate = sdf.format(java.util.Calendar.getInstance(
                    java.util.TimeZone.getTimeZone("Asia/Kolkata")).getTime());
            todayStats = db.statsDao().getStatsByDateLive(todayDate);
        }
    }
    public LiveData<DailyStats> getTodayStats() {
        return todayStats;
    }
    public void readUsageDataAndRefresh(Context context) {
        init(context);
    }
}