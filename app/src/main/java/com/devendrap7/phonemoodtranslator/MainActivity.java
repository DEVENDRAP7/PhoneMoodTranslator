package com.devendrap7.phonemoodtranslator;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button btnStart, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnStart = findViewById(R.id.btnStart);
        btnHistory = findViewById(R.id.btnHistory);
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> handleStartButtonClick());
        btnHistory.setOnClickListener(v -> navigateToHistory());
    }

    private void handleStartButtonClick() {
        if (!hasUsageAccessPermission()) {
            showToast("Please allow usage access to translate your mood");
            openUsageAccessSettings();
        } else {
            readUsageData();
        }
    }

    private void navigateToHistory() {
        startActivity(new Intent(this, HistoryActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ==============================
    // PERMISSION CHECK
    // ==============================
    private boolean hasUsageAccessPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;

            int mode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        getPackageName()
                );
            } else {
                mode = appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        getPackageName()
                );
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking usage permission", e);
            return false;
        }
    }

    private void openUsageAccessSettings() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    // ==============================
    // READ USAGE DATA - UPDATED LOGIC
    // ==============================
    // =========================================================
    // NEW: EVENT-BASED TRACKING (Instant & Accurate)
    // =========================================================
    private void readUsageData() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // 1. Get Midnight IST
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        Log.d(TAG, "Reading Events from: " + new SimpleDateFormat("HH:mm:ss").format(startTime));

        // 2. Query EVENTS (Not Stats)
        android.app.usage.UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);

        if (events == null) {
            showToast("No usage events found.");
            return;
        }

        // 3. Process the Event Stream
        processUsageEvents(events, startTime, endTime);
    }
    private void processUsageEvents(android.app.usage.UsageEvents events, long startTime, long endTime) {
        // Map to store duration per app
        java.util.HashMap<String, Long> appUsageMap = new java.util.HashMap<>();

        android.app.usage.UsageEvents.Event currentEvent = new android.app.usage.UsageEvents.Event();
        String lastAppPackage = null;
        long lastAppStartTime = 0;

        // Iterate through every single event today
        while (events.hasNextEvent()) {
            events.getNextEvent(currentEvent);
            String packageName = currentEvent.getPackageName();
            long timeStamp = currentEvent.getTimeStamp();
            int eventType = currentEvent.getEventType();

            // 1. Filter out System Junk immediately
            if (packageName.equals("android") ||
                    packageName.contains("launcher") ||
                    packageName.contains("home") ||
                    packageName.contains("systemui")) {
                continue;
            }

            // 2. Logic: Add time when app MOVES TO BACKGROUND
            if (eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastAppPackage = packageName;
                lastAppStartTime = timeStamp;
            }
            else if (eventType == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                // Only calculate if we saw the start of this session
                if (lastAppPackage != null && lastAppPackage.equals(packageName)) {
                    long duration = timeStamp - lastAppStartTime;

                    // Add to existing total for this app
                    long currentTotal = appUsageMap.getOrDefault(packageName, 0L);
                    appUsageMap.put(packageName, currentTotal + duration);
                }
                lastAppPackage = null; // Reset
            }
        }

        // Handle the app currently on screen (it hasn't "ended" yet)
        if (lastAppPackage != null) {
            long duration = endTime - lastAppStartTime;
            long currentTotal = appUsageMap.getOrDefault(lastAppPackage, 0L);
            appUsageMap.put(lastAppPackage, currentTotal + duration);
        }

        // ==========================================
        // AGGREGATE RESULTS
        // ==========================================
        long totalUsageTime = 0;
        int appOpenCount = 0;
        String mostUsedApp = "";
        long maxAppTime = 0;

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            String pkg = entry.getKey();
            long duration = entry.getValue();

            // Only count "Real" apps with significant usage (> 1 second)
            if (isAppLaunchable(pkg) && duration > 1000) {
                totalUsageTime += duration;
                appOpenCount++;

                if (duration > maxAppTime) {
                    maxAppTime = duration;
                    mostUsedApp = pkg;
                }

                // Debug log
                Log.d(TAG, "App: " + getAppName(pkg) + " | Time: " + (duration/60000) + "m");
            }
        }

        Log.d(TAG, "FINAL TOTAL TIME: " + (totalUsageTime/60000) + " mins");

        if (mostUsedApp.isEmpty()) mostUsedApp = "Unknown";

        // You'll need to pass an empty list for 'stats' since we used events
        navigateToResult(totalUsageTime, appOpenCount, mostUsedApp, maxAppTime, new ArrayList<>(), startTime);
    }

    private void processUsageStats(List<UsageStats> stats, long startTime, long endTime) {
        long totalUsageTime = 0;
        int appOpenCount = 0;
        String mostUsedApp = "";
        long maxAppTime = 0;

        String myPackageName = getPackageName();
        long timeSinceMidnight = endTime - startTime;

        // Debug lists to help us see what's happening
        ArrayList<String> debugTopApps = new ArrayList<>();

        for (UsageStats usage : stats) {
            String packageName = usage.getPackageName();
            long timeInForeground = usage.getTotalTimeInForeground();
            long lastTimeUsed = usage.getLastTimeUsed();

            // ------------------------------------------------------------
            // STRICT FILTERING RULES
            // ------------------------------------------------------------

            // 1. Remove the "Android OS" uptime bug
            if (packageName.equals("android")) continue;

            // 2. Remove data from yesterday
            if (lastTimeUsed < startTime) continue;

            // 3. Remove "Ghost" usage (Time > Time Since Midnight)
            // If an app says it ran for 5 hours, but only 2 hours have passed since midnight, it's a bug.
            if (timeInForeground > timeSinceMidnight) {
                Log.e(TAG, "Skipping Buggy App (Time > Reality): " + packageName);
                continue;
            }

            // 4. STRICTLY REMOVE SYSTEM UI & LAUNCHERS
            // This is the only way to fix the "11h" bug on your device.
            if (packageName.equals("com.android.systemui") ||
                    packageName.equals("com.android.settings") ||
                    packageName.equals("com.google.android.googlequicksearchbox") || // Google App (often background)
                    packageName.contains("launcher") ||
                    packageName.contains("home") ||
                    packageName.contains("nexuslauncher") ||
                    packageName.contains("miui")) {
                continue;
            }

            // 5. Must be a "Real" App (YouTube, Insta, etc.)
            if (!isAppLaunchable(packageName)) {
                continue;
            }

            // ------------------------------------------------------------
            // VALID APP - CALCULATE
            // ------------------------------------------------------------

            totalUsageTime += timeInForeground;
            appOpenCount++;

            if (timeInForeground > maxAppTime) {
                maxAppTime = timeInForeground;
                mostUsedApp = packageName;
            }

            // Add to debug list
            if (timeInForeground > 10 * 60 * 1000) { // Only log apps with > 10 mins
                debugTopApps.add(getAppName(packageName) + ": " + (timeInForeground / 60000) + "m");
            }
        }

        // Final Logic
        long totalMinutes = totalUsageTime / 60000;
        Log.d(TAG, "Final Calculated Screen Time: " + totalMinutes + " mins");
        Log.d(TAG, "Contributors: " + debugTopApps.toString());

        if (totalMinutes == 0) {
            showToast("No usage detected yet today. (System apps hidden)");
            return;
        }

        if (mostUsedApp.isEmpty()) mostUsedApp = "Unknown";

        navigateToResult(totalUsageTime, appOpenCount, mostUsedApp, maxAppTime, stats, startTime);
    }

    private void navigateToResult(long totalUsageTime, int appOpenCount,
                                  String mostUsedApp, long maxAppTime,
                                  List<UsageStats> stats, long startTime) {

        String mostUsedAppName = getAppName(mostUsedApp);
        int mostUsedMinutes = (int) (maxAppTime / (1000 * 60));
        long usageMinutes = totalUsageTime / (1000 * 60);
        boolean usedAtNight = isLateNightUsage(stats, startTime);

        MoodResult mood = translateMood(usageMinutes, appOpenCount, usedAtNight);
        saveTodayMood(mood, usageMinutes, appOpenCount, mostUsedAppName, mostUsedMinutes);

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("emoji", mood.emoji);
        intent.putExtra("title", mood.title);
        intent.putExtra("description", mood.getRandomDescription());
        intent.putExtra("usageMinutes", (int) usageMinutes);
        intent.putExtra("appOpens", appOpenCount);
        intent.putExtra("lateNight", usedAtNight);
        intent.putExtra("topAppName", mostUsedAppName);
        intent.putExtra("topAppMinutes", mostUsedMinutes);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ==============================
    // HELPER METHODS
    // ==============================

    private boolean isAppLaunchable(String packageName) {
        if (packageName.equals("android")) return false;
        try {
            return getPackageManager().getLaunchIntentForPackage(packageName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String getAppName(String packageName) {
        if (packageName.equals("General Usage")) return packageName;
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(packageName, 0);
            return getPackageManager().getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName.substring(packageName.lastIndexOf('.') + 1);
        }
    }

    private boolean isLateNightUsage(List<UsageStats> stats, long todayStartIST) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));

        for (UsageStats usage : stats) {
            long lastTimeUsed = usage.getLastTimeUsed();
            if (lastTimeUsed >= todayStartIST) {
                calendar.setTimeInMillis(lastTimeUsed);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                // Late night: 11 PM to 4 AM IST
                if (hour >= 23 || hour <= 4) {
                    return true;
                }
            }
        }
        return false;
    }

    private MoodResult translateMood(long usageMinutes, int appOpenCount, boolean usedAtNight) {
        // Late night heavy usage
        if (usedAtNight && usageMinutes > 120) {
            return new MoodResult("🌙", "Late-Night Thinker", new String[]{
                    "Your phone stayed active late into the night.", "The screen glow stretched past bedtime.",
                    "A mind that didn't fully switch off.", "Night thoughts kept you scrolling."});
        }
        // Deep focus - long sessions, few switches
        if (usageMinutes > 180 && appOpenCount < 15) {
            return new MoodResult("🔥", "Hyperfocused", new String[]{
                    "Long sessions with few interruptions.", "Deep focus defined your day.",
                    "Sustained attention was present.", "You stayed locked in for extended periods."});
        }
        // Distracted - many switches, moderate time
        if (appOpenCount > 20 && usageMinutes < 180) {
            return new MoodResult("😵", "Distracted Mind", new String[]{
                    "Frequent short checks scattered attention.", "Your focus jumped often.",
                    "Moments of distraction dominated.", "Apps kept pulling you in different directions."});
        }
        // Restless - heavy usage AND many switches
        if (usageMinutes > 240 && appOpenCount > 25) {
            return new MoodResult("🧠", "Restless Energy", new String[]{
                    "High usage with frequent switching.", "Stimulation was often sought.",
                    "Restlessness colored the day.", "Your mind was constantly in motion."});
        }
        // Very low usage
        if (usageMinutes < 45) {
            return new MoodResult("🌿", "Unplugged", new String[]{
                    "A quiet day away from screens.", "The phone stayed mostly dormant.",
                    "Presence lived elsewhere today.", "Digital distance felt natural."});
        }
        // Balanced usage
        return new MoodResult("🧘", "Calm & Grounded", new String[]{
                "A balanced digital rhythm.", "Moments felt intentional.",
                "Calm awareness was present.", "Nothing excessive, nothing lacking."});
    }

    private void saveTodayMood(MoodResult mood, long usageMinutes, int appOpenCount,
                               String topApp, int topAppMinutes) {
        SharedPreferences prefs = getSharedPreferences("mood_history", MODE_PRIVATE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        String today = dateFormat.format(Calendar.getInstance().getTime());

        String moodData = String.format(Locale.getDefault(), "%s|%s|%d|%d|%s|%d",
                mood.emoji, mood.title, usageMinutes, appOpenCount, topApp, topAppMinutes);

        prefs.edit().putString(today, moodData).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonText();
    }

    private void updateButtonText() {
        btnStart.setText(hasUsageAccessPermission() ? "Translate My Mood" : "Grant Usage Access");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}