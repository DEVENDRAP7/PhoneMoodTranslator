package com.devendrap7.phonemoodtranslator.activities;

import java.util.*;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.devendrap7.phonemoodtranslator.models.MoodResult;
import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.viewmodels.MoodViewModel;
import com.devendrap7.phonemoodtranslator.views.PulseBackgroundView;
import com.devendrap7.phonemoodtranslator.views.PulseCoreView;
import com.devendrap7.phonemoodtranslator.workers.UsageWorker;
import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnHistory;

    private View rootLayout;
    private TextView tvTitle, tvSubtitle, tvEmoji, tvSwipeHint;

    private int mDefaultColor;
    private MoodViewModel moodViewModel;
    private GestureDetector gestureDetector;
    private Map<String, String> installedAppsCache = new HashMap<>();
    private PulseBackgroundView pulseBackground;
    private PulseCoreView pulseCoreView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Use android.R.id.content to get the root view without needing an ID in XML
        View rootView = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // This adds padding so your Heatmap doesn't hide behind the status/nav bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize DB and fix today's record for Version 2 (The Missing Fix)
        AppDatabase db = AppDatabase.getDatabase(this);

        moodViewModel = new ViewModelProvider(this).get(MoodViewModel.class);
        moodViewModel.init(this);

        // Inside MainActivity.java onCreate
        moodViewModel.getTodayStats().observe(this, stats -> {
            if (stats != null) {
                // Update your Main Screen Mascot/Text here!
                int mins = (int) (stats.totalUsageTime / 60000);
                // e.g., mascotView.setMood(mins);
            }
        });

        initializeViews();
        // setupTheme();
        cacheInstalledApps();
        setupClickListeners();

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        // 2. WorkManager Setup
        androidx.work.PeriodicWorkRequest usageWorkRequest = new androidx.work.PeriodicWorkRequest.Builder(
                UsageWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "MoodUsageMonitor",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                usageWorkRequest);
        // After WorkManager scheduling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                // Show one-time dialog asking user to disable battery optimization
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                boolean askedBefore = prefs.getBoolean("battery_opt_asked", false);

                if (!askedBefore) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Enable Background Tracking")
                            .setMessage(
                                    "To track your usage even when the app is closed, please disable battery optimization for this app.")
                            .setPositiveButton("Settings", (dialog, which) -> {
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(android.net.Uri.parse("package:" + packageName));
                                startActivity(intent);
                            })
                            .setNegativeButton("Later", null)
                            .show();

                    prefs.edit().putBoolean("battery_opt_asked", true).apply();
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(
                "app_prefs", MODE_PRIVATE);
        mDefaultColor = prefs.getInt("bg_color",
                Color.parseColor("#4A148C"));
        applyTheme(mDefaultColor);
        // Peek at the data the moment the app comes to the foreground
        if (hasUsageAccessPermission()) {
            moodViewModel.readUsageDataAndRefresh(this);
        }
    }

    private void initializeViews() {
        btnStart = findViewById(R.id.btnStart);
        btnHistory = findViewById(R.id.btnHistory);
        rootLayout = findViewById(R.id.mainRootLayout);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvEmoji = findViewById(R.id.tvEmoji);
        tvSwipeHint = findViewById(R.id.tvSwipeHint);
        pulseCoreView = findViewById(R.id.pulseCore);

        pulseBackground = findViewById(R.id.pulseBackground);

        SharedPreferences prefs = getSharedPreferences(
                "app_prefs", MODE_PRIVATE);
        mDefaultColor = prefs.getInt("bg_color",
                Color.parseColor("#4A148C"));
        applyTheme(mDefaultColor);
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> handleStartButtonClick());
        btnHistory.setOnClickListener(v -> {
            readUsageDataSilent(); // ✅ saves only, no navigation
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void applyTheme(int themeColor) {
        if (rootLayout != null)
            rootLayout.setBackgroundColor(themeColor);
        boolean isDark = isColorDark(themeColor);
        int contrastColor = isDark ? Color.WHITE : Color.BLACK;

        if (tvTitle != null)
            tvTitle.setTextColor(contrastColor);
        if (tvSubtitle != null)
            tvSubtitle.setTextColor(contrastColor);
        if (tvSwipeHint != null) {
            tvSwipeHint.setTextColor(contrastColor);
            tvSwipeHint.setAlpha(0.7f);
        }
        if (btnStart != null) {
            btnStart.setBackgroundTintList(ColorStateList.valueOf(contrastColor));
            btnStart.setTextColor(themeColor);
        }
        if (btnHistory != null) {
            btnHistory.setBackgroundTintList(ColorStateList.valueOf(contrastColor));
            btnHistory.setTextColor(themeColor);
        }
        if (pulseBackground != null) {
            pulseBackground.setThemeColor(themeColor);
        }
        if (pulseCoreView != null) {
            // If background is light, make the core Red/Purple. If dark, make it glow.
            pulseCoreView.setupTheme(this);
        }
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private void cacheInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo app : apps) {
            String packageName = app.activityInfo.packageName;
            String appName = app.loadLabel(pm).toString();
            installedAppsCache.put(packageName, appName);
        }
    }

    private String getAppName(String packageName) {
        if (installedAppsCache.containsKey(packageName)) {
            return installedAppsCache.get(packageName);
        }
        return formatPackageName(packageName);
    }

    private String formatPackageName(String packageName) {
        if (packageName == null)
            return "Unknown";
        String[] parts = packageName.split("\\.");
        String rawName = packageName;
        if (parts.length > 1) {
            rawName = parts[1];
            if ((rawName.equals("google") || rawName.equals("android")) && parts.length > 2) {
                rawName = parts[2];
            }
        }
        return rawName.length() > 0 ? rawName.substring(0, 1).toUpperCase() + rawName.substring(1) : rawName;
    }

    private void handleStartButtonClick() {
        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please allow usage access", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            // REMOVED: moodViewModel.readUsageDataAndRefresh(this); <- was firing before
            // data ready
            // The real calculation happens inside readUsageData() -> processUsageEvents()
            // -> navigateToResult()
            readUsageData(); // This already calls navigateToResult() at the end with all the data
        }
    }

    private boolean hasUsageAccessPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),
                    getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private void readUsageData() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // STRICT MIDNIGHT RESET
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        // If for some reason the clock is weird, ensure startTime is today
        if (startTime >= endTime) {
            startTime = endTime - (1000 * 60); // Default to 1 min ago if at exactly midnight
        }

        // ✅ MIDNIGHT FIX: Query from 6 hours before midnight to catch running apps
        long queryStart = startTime - (6 * 60 * 60 * 1000);

        UsageEvents events = usageStatsManager.queryEvents(queryStart, endTime);
        if (events != null) {
            processUsageEvents(events, startTime, endTime, true);
        }
    }

    private void readUsageDataSilent() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        // ✅ MIDNIGHT FIX: Query from 6 hours before midnight to catch running apps
        long queryStart = startTime - (6 * 60 * 60 * 1000);

        UsageEvents events = usageStatsManager.queryEvents(queryStart, endTime);
        if (events != null) {
            processUsageEvents(events, startTime, endTime, false); // ✅ false = no navigation
        }
    }

    private String getLauncherPackageName() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (resolveInfo != null && resolveInfo.activityInfo != null) ? resolveInfo.activityInfo.packageName : "";
    }

    private static final List<String> IGNORED_PACKAGES = Arrays.asList(
            "com.android.systemui",
            "android",
            "com.google.android.gms",
            "com.android.settings",
            "com.samsung");

    private void processUsageEvents(UsageEvents events, long startTime, long endTime, boolean shouldNavigate) {
        HashMap<String, Long> finalAppDurations = new HashMap<>();
        HashMap<String, Long> appStartTimes = new HashMap<>();
        long[] hourlyMillis = new long[24];
        HashSet<String> uniqueAppsOpened = new HashSet<>();

        UsageEvents.Event currentEvent = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(currentEvent);
            long time = currentEvent.getTimeStamp();
            int type = currentEvent.getEventType();
            String pkg = currentEvent.getPackageName();

            if (IGNORED_PACKAGES.contains(pkg))
                continue;

            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appStartTimes.put(pkg, time);
                if (time >= startTime)
                    uniqueAppsOpened.add(pkg);
            } else if (type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (appStartTimes.containsKey(pkg)) {
                    long start = appStartTimes.get(pkg);
                    long effectiveStart = Math.max(start, startTime);
                    long duration = time - effectiveStart;

                    if (duration > 0) {
                        finalAppDurations.put(pkg,
                                finalAppDurations.getOrDefault(pkg, 0L) + duration);
                        distributeToHourlyBuckets(hourlyMillis,
                                effectiveStart, time);
                    }
                    appStartTimes.remove(pkg);
                }
            }
        }

        // Handle apps still in foreground
        for (Map.Entry<String, Long> entry : appStartTimes.entrySet()) {
            if (!IGNORED_PACKAGES.contains(entry.getKey())) {
                long start = entry.getValue();
                long effectiveStart = Math.max(start, startTime);
                long duration = endTime - effectiveStart;

                if (duration > 0) {
                    finalAppDurations.put(entry.getKey(),
                            finalAppDurations.getOrDefault(entry.getKey(), 0L) + duration);
                    distributeToHourlyBuckets(hourlyMillis, effectiveStart, endTime);
                }
            }
        }

        // Calculate totals
        long filteredTotalScreenTime = 0;
        long maxAppTime = 0;
        String mostUsedApp = "";
        List<AppUsageInfo> detailedList = new ArrayList<>();
        String launcherPkg = getLauncherPackageName();

        for (Map.Entry<String, Long> entry : finalAppDurations.entrySet()) {
            String pkg = entry.getKey();
            long duration = entry.getValue();
            boolean isLauncher = pkg.equals(launcherPkg) || pkg.contains("launcher");

            if (!isLauncher) {
                filteredTotalScreenTime += duration;
            }

            if (duration > 1000) {
                if (duration > maxAppTime) {
                    maxAppTime = duration;
                    mostUsedApp = pkg;
                }
                String name = getAppName(pkg);
                if (isLauncher)
                    name = "Home Screen";
                detailedList.add(new AppUsageInfo(name, duration));
            }
        }

        detailedList.sort((a, b) -> Long.compare(b.usageTime, a.usageTime));
        String topAppsJson = new Gson().toJson(detailedList);

        long[] hourlyMinutes = new long[24];
        for (int i = 0; i < 24; i++) {
            hourlyMinutes[i] = hourlyMillis[i] / 60000;
        }
        String hourlyDataJson = new Gson().toJson(hourlyMinutes);

        int usageMinutes = (int) (filteredTotalScreenTime / (1000 * 60));
        int mostUsedMinutes = (int) (maxAppTime / (1000 * 60));
        boolean usedAtNight = com.devendrap7.phonemoodtranslator.utils.MoodCalculator.isLateNightUsage();
        String mostUsedName = getAppName(mostUsedApp.isEmpty() ? "General Usage" : mostUsedApp);

        int uniqueAppCount = uniqueAppsOpened.size();

        MoodResult mood = com.devendrap7.phonemoodtranslator.utils.MoodCalculator.calculateMood(
                usageMinutes, uniqueAppCount, usedAtNight);

        saveToDatabase(mood, filteredTotalScreenTime, uniqueAppCount, topAppsJson, hourlyDataJson);

        if (shouldNavigate) {
            navigateToResult(mood, usageMinutes, uniqueAppCount, usedAtNight, mostUsedName, mostUsedMinutes);
        }
    }

    // ✅ NEW METHOD — distributes usage time into hourly buckets
    private void distributeToHourlyBuckets(long[] hourlyMillis,
            long startMs, long endMs) {
        Calendar cal = Calendar.getInstance();

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

    private void navigateToResult(MoodResult mood, long usageMinutes, int appOpenCount, boolean usedAtNight,
            String mostUsedAppName, int mostUsedMinutes) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("emoji", mood.emoji);
        intent.putExtra("title", mood.title);
        intent.putExtra("description", mood.getRandomDescription());
        intent.putExtra("usageMinutes", (int) usageMinutes);
        intent.putExtra("appOpens", appOpenCount);
        intent.putExtra("lateNight", usedAtNight);
        intent.putExtra("topAppName", mostUsedAppName);
        intent.putExtra("topAppMinutes", mostUsedMinutes);

        // ✅ THIS is what notifies the AnalysisFragment to update
        moodViewModel.readUsageDataAndRefresh(this);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void saveToDatabase(MoodResult mood, long totalUsageTime,
            int unlockCount, String topAppsJson,
            String hourlyDataJson) { // ✅ new param
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "dd MMM yyyy", Locale.ENGLISH);
                dateFormat.setTimeZone(TimeZone.getDefault());
                String todayDate = dateFormat.format(cal.getTime());

                int currentMonth = cal.get(Calendar.MONTH) + 1;
                int currentYear = cal.get(Calendar.YEAR);
                int currentDay = cal.get(Calendar.DAY_OF_MONTH);
                long timestamp = cal.getTimeInMillis();

                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                DailyStats existing = db.statsDao().getStatsByDate(todayDate);

                if (existing != null) {
                    existing.totalUsageTime = totalUsageTime;
                    existing.unlockCount = unlockCount;
                    existing.moodEmoji = mood.emoji;
                    existing.moodTitle = mood.title;
                    existing.topAppsJson = topAppsJson;
                    existing.dayOfMonth = currentDay;
                    existing.dateTimestamp = timestamp;
                    existing.hourlyDataJson = hourlyDataJson; // ✅
                    db.statsDao().update(existing);
                } else {
                    DailyStats todayStats = new DailyStats(
                            todayDate, currentMonth, currentYear,
                            currentDay, timestamp,
                            unlockCount, totalUsageTime,
                            unlockCount, mood.emoji, mood.title,
                            topAppsJson, "");
                    todayStats.hourlyDataJson = hourlyDataJson; // ✅
                    db.statsDao().insert(todayStats);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null)
                return false;
            float diffY = e2.getY() - e1.getY();
            if (Math.abs(diffY) > 100 && Math.abs(velocityY) > 100 && diffY < 0) {
                onSwipeUp();
                return true;
            }
            return false;
        }
    }

    private void onSwipeUp() {
        startActivity(new Intent(MainActivity.this, MoodGalleryActivity.class));
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
    }

    public static class AppUsageInfo {
        public String name;
        public long usageTime;

        public AppUsageInfo(String name, long usageTime) {
            this.name = name;
            this.usageTime = usageTime;
        }
    }
}