package com.devendrap7.phonemoodtranslator.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class UsageWorker extends Worker {

    private static final String CHANNEL_ID = "mood_alerts";
    // Default fallback limits (in hours)
    private static final double DEFAULT_LIMIT_TOTAL = 6.0;
    private static final double DEFAULT_LIMIT_SOCIAL = 2.0;
    private static final long ALERT_COOLDOWN = 4 * 60 * 60 * 1000; // 4 Hours

    // The Social Media "Watchlist"
    private static final List<String> SOCIAL_PACKAGES = Arrays.asList(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.whatsapp",
            "com.snapchat.android"
    );

    public UsageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        checkUsage();
        return Result.success();
    }

    private void checkUsage() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // Fetch User Settings
        boolean alertSocialEnabled = prefs.getBoolean("limit_social_enabled", true);
        boolean alertTotalEnabled = prefs.getBoolean("limit_total_enabled", true);
        float limitTotal = prefs.getFloat("limit_total_hours", (float) DEFAULT_LIMIT_TOTAL);
        float limitSocial = prefs.getFloat("limit_social_hours", (float) DEFAULT_LIMIT_SOCIAL);
        long lastAlertTime = prefs.getLong("last_alert_timestamp", 0);

        // 1. Midnight Calculation (IST)
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        // 2. Fetch Usage Events
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = usm.queryEvents(startTime, endTime);

        // Map to store time per package
        Map<String, Long> appDurations = calculateAppDurations(events, startTime, endTime);

        // 3. Calculate Totals
        long totalUsageMillis = 0;
        long socialUsageMillis = 0;

        for (Map.Entry<String, Long> entry : appDurations.entrySet()) {
            totalUsageMillis += entry.getValue();
            if (SOCIAL_PACKAGES.contains(entry.getKey())) {
                socialUsageMillis += entry.getValue();
            }
        }

        double totalHours = totalUsageMillis / (1000.0 * 60 * 60);
        double socialHours = socialUsageMillis / (1000.0 * 60 * 60);

        // 4. Update Database for the Pie Chart/History
        updateDatabase(context, totalUsageMillis, currentMonth, currentYear);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());
        String todayDate = sdf.format(Calendar.getInstance(
                TimeZone.getDefault()).getTime());

        AppDatabase db = AppDatabase.getDatabase(context);
        DailyStats fresh = db.statsDao().getStatsByDate(todayDate);

        // 5. Enforcement Logic (Alerts)
        if (System.currentTimeMillis() - lastAlertTime >= ALERT_COOLDOWN) {
            
            // Check Social Limit first (usually more urgent)
            if (alertSocialEnabled && socialHours > limitSocial) {
                sendAlert("📱 Social Media Warning", "You've spent " + String.format("%.1f", socialHours) + "h on social apps. Your pet is getting worried!");
                prefs.edit().putLong("last_alert_timestamp", System.currentTimeMillis()).apply();
            } 
            // Check Total Limit
            else if (alertTotalEnabled && totalHours > limitTotal) {
                sendAlert("⚠️ High Screen Time", "Total usage is " + String.format("%.1f", totalHours) + "h. Time for a real-world break?");
                prefs.edit().putLong("last_alert_timestamp", System.currentTimeMillis()).apply();
            }
        }
    }

    private Map<String, Long> calculateAppDurations(UsageEvents events, long startTime, long endTime) {
        Map<String, Long> durations = new HashMap<>();
        Map<String, Long> startTimes = new HashMap<>();
        UsageEvents.Event event = new UsageEvents.Event();

        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(event);
            String pkg = event.getPackageName();
            long time = event.getTimeStamp();
            int type = event.getEventType();

            if (time < startTime) continue;

            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                startTimes.put(pkg, time);
            } else if (type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (startTimes.containsKey(pkg)) {
                    long duration = time - Math.max(startTime, startTimes.get(pkg));
                    durations.put(pkg, durations.getOrDefault(pkg, 0L) + duration);
                    startTimes.remove(pkg);
                }
            }
        }
        
        // Handle apps currently in foreground
        for (String pkg : startTimes.keySet()) {
            long duration = endTime - Math.max(startTime, startTimes.get(pkg));
            durations.put(pkg, durations.getOrDefault(pkg, 0L) + duration);
        }
        return durations;
    }

    private void updateDatabase(Context context, long totalTime, int month, int year) {
        AppDatabase db = AppDatabase.getDatabase(context);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        String today = sdf.format(cal.getTime());

        int currentDay   = cal.get(Calendar.DAY_OF_MONTH); // ✅
        long timestamp   = cal.getTimeInMillis();           // ✅

        DailyStats stats = db.statsDao().getStatsByDate(today);

        if (stats != null) {
            // ✅ Update existing — preserves moodEmoji, moodTitle, topAppsJson
            stats.totalUsageTime = totalTime;
            stats.month          = month;
            stats.year           = year;
            stats.dayOfMonth     = currentDay;  // ✅
            stats.dateTimestamp  = timestamp;   // ✅
            db.statsDao().update(stats);
        } else {
            // ✅ Insert skeleton row
            DailyStats newStats = new DailyStats(
                    today,
                    month,
                    year,
                    currentDay,  // ✅ dayOfMonth
                    timestamp,   // ✅ dateTimestamp
                    0,           // totalCount
                    totalTime,   // totalUsageTime
                    0,           // unlockCount
                    "❓",        // moodEmoji placeholder
                    "Pending",   // moodTitle placeholder
                    null,        // topAppsJson
                    ""           // selfNote
            );
            db.statsDao().insert(newStats);
        }
    }

    private void sendAlert(String title, String message) {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Mood Alerts", NotificationManager.IMPORTANCE_HIGH);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // ENSURE THIS EXISTS
                .setContentTitle(title)
                .setContentText(message)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        if (manager != null) manager.notify(1001, builder.build());
    }
}