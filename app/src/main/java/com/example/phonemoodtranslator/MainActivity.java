package com.example.phonemoodtranslator;

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

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnStart, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnHistory = findViewById(R.id.btnHistory);

        btnStart.setOnClickListener(v -> {
            if (!hasUsageAccessPermission()) {
                Toast.makeText(
                        this,
                        "Please allow usage access to translate your mood",
                        Toast.LENGTH_LONG
                ).show();
                openUsageAccessSettings();
            } else {
                readUsageData();
            }
        });

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class))
        );
    }

    // ==============================
    // PERMISSION CHECK
    // ==============================
    private boolean hasUsageAccessPermission() {
        try {
            AppOpsManager appOps =
                    (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);

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
            return false;
        }
    }

    private void openUsageAccessSettings() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    // ==============================
    // READ USAGE DATA
    // ==============================

    private boolean isUserApp(String packageName) {
        try {
            ApplicationInfo appInfo =
                    getPackageManager().getApplicationInfo(packageName, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 🔥 ADD STEP 1 METHOD HERE 👇
    private boolean isLaunchableApp(String packageName) {
        Intent intent =
                getPackageManager().getLaunchIntentForPackage(packageName);
        return intent != null;
    }


    private String getAppName(String packageName) {
        try {
            ApplicationInfo appInfo =
                    getPackageManager().getApplicationInfo(packageName, 0);

            return getPackageManager()
                    .getApplicationLabel(appInfo)
                    .toString();

        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown App";
        }
    }

    private void readUsageData() {

        UsageStatsManager usageStatsManager =
                (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        if (usageStatsManager == null) {
            Toast.makeText(this, "Usage data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> stats =
                usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        startTime,
                        endTime
                );

        if (stats == null || stats.isEmpty()) {
            Toast.makeText(this, "No usage data yet. Try later.", Toast.LENGTH_SHORT).show();
            return;
        }

        long totalUsageTime = 0;
        int appOpenCount = 0;


        String mostUsedApp = "";
        long maxAppTime = 0;

        for (UsageStats usage : stats) {
            long time = usage.getTotalTimeInForeground();
            String myPackageName = getPackageName();

            String pkg=usage.getPackageName();
            if (pkg.equals(myPackageName)) {
                continue;
            }
            if (time > 0) {
                totalUsageTime += time;
                appOpenCount++;

                if (time > maxAppTime) {
                    maxAppTime = time;
                    mostUsedApp = usage.getPackageName();
                }
            }
        }
        String mostUsedAppName = getAppName(mostUsedApp);
        int mostUsedMinutes = (int) (maxAppTime / (1000 * 60));


        long usageMinutes = totalUsageTime / (1000 * 60);
        boolean usedAtNight = isLateNightUsage(stats);

        MoodResult mood = translateMood(usageMinutes, appOpenCount, usedAtNight);

        saveTodayMood(mood);

        // 🔗 Open Result Screen
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
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
    }

    // ==============================
    // LATE NIGHT CHECK
    // ==============================
    private boolean isLateNightUsage(List<UsageStats> stats) {
        Calendar calendar = Calendar.getInstance();

        for (UsageStats usage : stats) {
            calendar.setTimeInMillis(usage.getLastTimeUsed());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour >= 23 || hour <= 4) {
                return true;
            }
        }
        return false;
    }

    // ==============================
    // MOOD LOGIC (TUNED)
    // ==============================
    private MoodResult translateMood(long usageMinutes, int appOpenCount, boolean usedAtNight) {

        if (usedAtNight && usageMinutes > 180) {
            return new MoodResult("🌙", "Late-Night Thinker",
                    new String[]{
                            "Your phone stayed active late into the night.",
                            "The screen glow stretched past bedtime.",
                            "A mind that didn’t fully switch off."
                    });
        }

        if (usageMinutes > 240 && appOpenCount < 20) {
            return new MoodResult("🔥", "Hyperfocused",
                    new String[]{
                            "Long sessions with few interruptions.",
                            "Deep focus defined your day.",
                            "Sustained attention was present."
                    });
        }

        if (usageMinutes < 240 && appOpenCount > 35) {
            return new MoodResult("😵", "Distracted Mind",
                    new String[]{
                            "Frequent short checks scattered attention.",
                            "Your focus jumped often.",
                            "Moments of distraction dominated."
                    });
        }

        if (usageMinutes > 240 && appOpenCount > 40) {
            return new MoodResult("🧠", "Restless Energy",
                    new String[]{
                            "High usage with frequent switching.",
                            "Stimulation was often sought.",
                            "Restlessness colored the day."
                    });
        }

        return new MoodResult("🧘", "Calm & Grounded",
                new String[]{
                        "A balanced digital rhythm.",
                        "Moments felt intentional.",
                        "Calm awareness was present."
                });
    }

    // ==============================
    // SAVE DAILY MOOD
    // ==============================
    private void saveTodayMood(MoodResult mood) {
        SharedPreferences prefs =
                getSharedPreferences("mood_history", MODE_PRIVATE);

        String today =
                java.text.DateFormat.getDateInstance().format(
                        Calendar.getInstance().getTime()
                );

        prefs.edit()
                .putString(today, mood.emoji + "|" + mood.title)
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnStart.setText(
                hasUsageAccessPermission()
                        ? "Translate My Mood"
                        : "Grant Usage Access"
        );
    }
}
