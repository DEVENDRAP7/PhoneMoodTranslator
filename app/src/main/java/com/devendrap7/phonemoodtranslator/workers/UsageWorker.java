package com.devendrap7.phonemoodtranslator.workers;

import com.devendrap7.phonemoodtranslator.models.MoodResult;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            "com.snapchat.android");

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

        boolean alertSocialEnabled = prefs.getBoolean("limit_social_enabled", true);
        boolean alertTotalEnabled = prefs.getBoolean("limit_total_enabled", true);
        float limitTotal = prefs.getFloat("limit_total_hours", (float) DEFAULT_LIMIT_TOTAL);
        float limitSocial = prefs.getFloat("limit_social_hours", (float) DEFAULT_LIMIT_SOCIAL);
        long lastAlertTime = prefs.getLong("last_alert_timestamp", 0);

        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        // ✅ MIDNIGHT FIX: Query from 6 hours before midnight to catch running apps
        long queryStart = startTime - (6 * 60 * 60 * 1000);

        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = usm.queryEvents(queryStart, endTime);

        long[] hourlyMillis = new long[24];
        int[] appSwitchCount = new int[] { 0 };

        Map<String, Long> appDurations = calculateAppDurations(events, startTime, endTime, hourlyMillis,
                appSwitchCount);

        List<AppUsageInfo> appList = new ArrayList<>();
        long totalUsageMillis = 0;
        long socialUsageMillis = 0;

        for (Map.Entry<String, Long> entry : appDurations.entrySet()) {
            String pkg = entry.getKey();
            long duration = entry.getValue();
            totalUsageMillis += duration;

            if (SOCIAL_PACKAGES.contains(pkg)) {
                socialUsageMillis += duration;
            }

            if (duration > 1000) {
                String appName = getAppName(context, pkg);
                appList.add(new AppUsageInfo(appName, duration));
            }
        }

        appList.sort((a, b) -> Long.compare(b.usageTime, a.usageTime));
        String topAppsJson = new com.google.gson.Gson().toJson(appList);

        long[] hourlyMinutes = new long[24];
        for (int i = 0; i < 24; i++) {
            hourlyMinutes[i] = hourlyMillis[i] / 60000;
        }
        String hourlyDataJson = new com.google.gson.Gson().toJson(hourlyMinutes);

        int usageMinutes = (int) (totalUsageMillis / (1000 * 60));
        boolean usedAtNight = com.devendrap7.phonemoodtranslator.utils.MoodCalculator.isLateNightUsage();

        com.devendrap7.phonemoodtranslator.models.MoodResult mood = com.devendrap7.phonemoodtranslator.utils.MoodCalculator
                .calculateMood(
                        usageMinutes, appSwitchCount[0], usedAtNight);

        double totalHours = totalUsageMillis / (1000.0 * 60 * 60);
        double socialHours = socialUsageMillis / (1000.0 * 60 * 60);

        updateDatabase(context, totalUsageMillis, currentMonth, currentYear,
                topAppsJson, hourlyDataJson, mood);

        // Alerts
        if (System.currentTimeMillis() - lastAlertTime >= ALERT_COOLDOWN) {
            if (alertSocialEnabled && socialHours > limitSocial) {
                sendAlert("📱 Social Media Warning",
                        "You've spent " + formatHoursForNotification(socialHours) +
                                " on social media. Your pet is getting worried!");
                prefs.edit().putLong("last_alert_timestamp",
                        System.currentTimeMillis()).apply();
            } else if (alertTotalEnabled && totalHours > limitTotal) {
                sendAlert("⚠️ High Screen Time",
                        "Total usage is " + formatHoursForNotification(totalHours) +
                                ". Time for a real-world break?");
                prefs.edit().putLong("last_alert_timestamp",
                        System.currentTimeMillis()).apply();
            }
        }
    }

    // ✅ Add this class inside UsageWorker
    public static class AppUsageInfo {
        public String name;
        public long usageTime;

        public AppUsageInfo(String name, long usageTime) {
            this.name = name;
            this.usageTime = usageTime;
        }
    }

    // ✅ Add this method
    private String getAppName(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            // Fallback formatting
            String[] parts = packageName.split("\\.");
            if (parts.length > 1) {
                String name = parts[parts.length - 1];
                return name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            return packageName;
        }
    }

    private Map<String, Long> calculateAppDurations(UsageEvents events,
            long startTime,
            long endTime,
            long[] hourlyMillis,
            int[] appSwitchCount) {
        Map<String, Long> durations = new HashMap<>();
        Map<String, Long> startTimes = new HashMap<>();
        java.util.HashSet<String> uniqueApps = new java.util.HashSet<>();
        UsageEvents.Event event = new UsageEvents.Event();

        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(event);
            String pkg = event.getPackageName();
            long time = event.getTimeStamp();
            int type = event.getEventType();

            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                startTimes.put(pkg, time);
                if (time >= startTime)
                    uniqueApps.add(pkg);
            } else if (type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (startTimes.containsKey(pkg)) {
                    long start = startTimes.get(pkg);
                    long effectiveStart = Math.max(start, startTime);
                    long duration = time - effectiveStart;

                    if (duration > 0) {
                        durations.put(pkg, durations.getOrDefault(pkg, 0L) + duration);
                        distributeToHourlyBuckets(hourlyMillis,
                                effectiveStart, time);
                    }
                    startTimes.remove(pkg);
                }
            }
        }

        // Handle apps still in foreground
        for (String pkg : startTimes.keySet()) {
            long start = startTimes.get(pkg);
            long effectiveStart = Math.max(start, startTime);
            long duration = endTime - effectiveStart;

            if (duration > 0) {
                durations.put(pkg, durations.getOrDefault(pkg, 0L) + duration);
                distributeToHourlyBuckets(hourlyMillis, effectiveStart, endTime);
            }
        }

        appSwitchCount[0] = uniqueApps.size();
        return durations;
    }

    private String formatHoursForNotification(double hours) {
        if (hours < 1.0) {
            int mins = (int) (hours * 60);
            return mins + " minutes";
        }

        int wholeHours = (int) hours;
        int mins = (int) ((hours - wholeHours) * 60);

        if (mins == 0) {
            return wholeHours + " hour" + (wholeHours > 1 ? "s" : "");
        } else {
            return wholeHours + "h " + mins + "min";
        }
    }

    private void updateDatabase(Context context, long totalTime,
            int month, int year,
            String topAppsJson,
            String hourlyDataJson,
            com.devendrap7.phonemoodtranslator.models.MoodResult mood) {
        AppDatabase db = AppDatabase.getDatabase(context);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        String today = sdf.format(cal.getTime());

        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        long timestamp = cal.getTimeInMillis();

        DailyStats stats = db.statsDao().getStatsByDate(today);

        if (stats != null) {
            stats.totalUsageTime = totalTime;
            stats.month = month;
            stats.year = year;
            stats.dayOfMonth = currentDay;
            stats.dateTimestamp = timestamp;
            stats.topAppsJson = topAppsJson;
            stats.hourlyDataJson = hourlyDataJson;
            // ✅ Update mood
            stats.moodEmoji = mood.emoji;
            stats.moodTitle = mood.title;
            db.statsDao().update(stats);
        } else {
            DailyStats newStats = new DailyStats(
                    today, month, year, currentDay, timestamp,
                    0, totalTime, 0,
                    mood.emoji, mood.title, // ✅ Real mood now!
                    topAppsJson,
                    "");
            newStats.hourlyDataJson = hourlyDataJson;
            db.statsDao().insert(newStats);
        }
    }

    private void distributeToHourlyBuckets(long[] hourlyMillis, long startMs, long endMs) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());

        long cursor = startMs;
        while (cursor < endMs) {
            cal.setTimeInMillis(cursor);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            // Find end of this hour slot
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            long hourEnd = Math.min(cal.getTimeInMillis(), endMs);

            // Add duration for this hour slot
            hourlyMillis[hour] += (hourEnd - cursor);

            // Move to next hour
            cursor = hourEnd + 1;
        }
    }

    private void sendAlert(String title, String message) {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "DigiPulse Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            if (manager != null)
                manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // ENSURE THIS EXISTS
                .setContentTitle(title)
                .setContentText(message)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        if (manager != null)
            manager.notify(1001, builder.build());
    }
}