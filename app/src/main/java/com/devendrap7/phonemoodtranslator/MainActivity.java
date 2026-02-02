package com.devendrap7.phonemoodtranslator;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnHistory;
    private FloatingActionButton fabColorPicker;
    private View rootLayout;
    private TextView tvTitle, tvSubtitle,tvEmoji;
    private int mDefaultColor;

    // Gesture Detector for Swipe Up
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupTheme();
        setupClickListeners();

        // Initialize Gesture Detector
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
    }

    // This method intercepts touch events BEFORE they reach buttons
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            if (gestureDetector.onTouchEvent(ev)) {
                return true; // Swipe handled!
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return false; // Crucial: Return false so buttons can still be clicked
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                // Check if vertical swipe > threshold
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) {
                        // Swipe UP detected
                        onSwipeUp();
                        return true;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }
    }

    private void onSwipeUp() {
        Intent intent = new Intent(MainActivity.this, MoodGalleryActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
    }

    // ==========================================
    // EXISTING LOGIC (Unchanged)
    // ==========================================

    private void initializeViews() {
        btnStart = findViewById(R.id.btnStart);
        btnHistory = findViewById(R.id.btnHistory);
        fabColorPicker = findViewById(R.id.fabColorPicker);
        rootLayout = findViewById(R.id.mainRootLayout);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvEmoji=findViewById(R.id.tvEmoji);
    }

    private void setupTheme() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        mDefaultColor = prefs.getInt("bg_color", Color.parseColor("#4A148C"));
        applyTheme(mDefaultColor);
    }

    private void applyTheme(int themeColor) {
        if (rootLayout != null) rootLayout.setBackgroundColor(themeColor);
        boolean isDark = isColorDark(themeColor);
        int contrastColor = isDark ? Color.WHITE : Color.BLACK;

        if (tvTitle != null) tvTitle.setTextColor(contrastColor);
        if (tvSubtitle != null) tvSubtitle.setTextColor(contrastColor);

        if (btnStart != null) {
            btnStart.setBackgroundTintList(ColorStateList.valueOf(contrastColor));
            btnStart.setTextColor(themeColor);
        }
        if (btnHistory != null) {
            btnHistory.setBackgroundTintList(ColorStateList.valueOf(contrastColor));
            btnHistory.setTextColor(themeColor);
        }
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> handleStartButtonClick());
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        fabColorPicker.setOnClickListener(v -> openColorPicker());
    }

    private void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, mDefaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {}
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mDefaultColor = color;
                saveTheme(color);
                applyTheme(color);
            }
        });
        colorPicker.show();
    }

    private void saveTheme(int color) {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putInt("bg_color", color).apply();
    }

    private void handleStartButtonClick() {
        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please allow usage access", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            readUsageData();
        }
    }

    private boolean hasUsageAccessPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) { return false; }
    }

    private void readUsageData() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);
        if (events == null) {
            Toast.makeText(this, "No usage data found.", Toast.LENGTH_SHORT).show();
            return;
        }
        processUsageEvents(events, startTime, endTime);
    }

    private void processUsageEvents(UsageEvents events, long startTime, long endTime) {
        HashMap<String, Long> appUsageMap = new HashMap<>();
        UsageEvents.Event currentEvent = new UsageEvents.Event();
        String lastAppPackage = null;
        long lastAppStartTime = 0;

        while (events.hasNextEvent()) {
            events.getNextEvent(currentEvent);
            String packageName = currentEvent.getPackageName();
            long timeStamp = currentEvent.getTimeStamp();
            int eventType = currentEvent.getEventType();

            if (packageName.equals("android") || packageName.contains("launcher") ||
                    packageName.contains("home") || packageName.contains("systemui")) continue;

            if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastAppPackage = packageName;
                lastAppStartTime = timeStamp;
            } else if (eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (lastAppPackage != null && lastAppPackage.equals(packageName)) {
                    long duration = timeStamp - lastAppStartTime;
                    appUsageMap.put(packageName, appUsageMap.getOrDefault(packageName, 0L) + duration);
                }
                lastAppPackage = null;
            }
        }
        if (lastAppPackage != null) {
            appUsageMap.put(lastAppPackage, appUsageMap.getOrDefault(lastAppPackage, 0L) + (endTime - lastAppStartTime));
        }

        long totalUsageTime = 0;
        int appOpenCount = 0;
        String mostUsedApp = "";
        long maxAppTime = 0;

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            String pkg = entry.getKey();
            long duration = entry.getValue();
            if (isAppLaunchable(pkg) && duration > 1000) {
                totalUsageTime += duration;
                appOpenCount++;
                if (duration > maxAppTime) { maxAppTime = duration; mostUsedApp = pkg; }
            }
        }

        if (mostUsedApp.isEmpty()) mostUsedApp = "General Usage";
        navigateToResult(totalUsageTime, appOpenCount, mostUsedApp, maxAppTime, startTime);
    }

    private void navigateToResult(long totalUsageTime, int appOpenCount, String mostUsedApp, long maxAppTime, long startTime) {
        String mostUsedAppName = getAppName(mostUsedApp);
        int mostUsedMinutes = (int) (maxAppTime / (1000 * 60));
        long usageMinutes = totalUsageTime / (1000 * 60);
        boolean usedAtNight = isLateNightUsage(startTime);

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

    private MoodResult translateMood(long usageMinutes, int appOpenCount, boolean usedAtNight) {
        if (usageMinutes > 300) return new MoodResult("🤯", "Overdose", new String[]{"System overload detected.", "Time to disconnect."});
        if (usageMinutes >= 240) return new MoodResult("🔗", "Tethered", new String[]{"The phone feels glued to your hand.", "Borderline heavy usage."});
        if (usageMinutes < 30) return new MoodResult("🌿", "Unplugged", new String[]{"Real life took priority.", "Digital distance felt natural."});
        if (usedAtNight && usageMinutes > 30) return new MoodResult("🌙", "Late-Night Thinker", new String[]{"Sleep was sacrificed for scrolling.", "A midnight mind wandering."});
        if (usageMinutes > 150 && appOpenCount < 15) return new MoodResult("🔥", "Hyperfocused", new String[]{"Deep work defined your day.", "Sustained attention."});
        if (usageMinutes > 150 && appOpenCount > 45) return new MoodResult("🧠", "Restless Energy", new String[]{"Your mind was running sprints.", "Stimulation sought."});
        if (usageMinutes < 150 && appOpenCount > 40) return new MoodResult("😵", "Distracted Mind", new String[]{"Focus was impossible.", "A butterfly flitting."});
        if (usageMinutes < 90) return new MoodResult("😎", "Slick", new String[]{"In and out. Efficient.", "You rule the phone."});
        if (appOpenCount < 30) return new MoodResult("🧐", "Serious Mode", new String[]{"Usage was purposeful.", "Disciplined session."});
        if (appOpenCount >= 30) return new MoodResult("🎡", "Light-hearted", new String[]{"Just browsing and chatting.", "Casual wandering."});
        return new MoodResult("🧘", "Calm & Grounded", new String[]{"A balanced digital rhythm.", "Stable connection."});
    }

    private boolean isAppLaunchable(String packageName) {
        try { return getPackageManager().getLaunchIntentForPackage(packageName) != null; }
        catch (Exception e) { return false; }
    }

    private String getAppName(String packageName) {
        if (packageName.equals("General Usage")) return packageName;
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0)).toString(); }
        catch (PackageManager.NameNotFoundException e) { return packageName; }
    }

    private boolean isLateNightUsage(long todayStartIST) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        int hour = now.get(Calendar.HOUR_OF_DAY);
        return (hour >= 23 || hour <= 4);
    }

    private void saveTodayMood(MoodResult mood, long usageMinutes, int appOpenCount, String topApp, int topAppMinutes) {
        SharedPreferences prefs = getSharedPreferences("mood_history", MODE_PRIVATE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        String today = dateFormat.format(Calendar.getInstance().getTime());
        String moodData = String.format(Locale.getDefault(), "%s|%s|%d|%d|%s|%d", mood.emoji, mood.title, usageMinutes, appOpenCount, topApp, topAppMinutes);
        prefs.edit().putString(today, moodData).apply();
    }
}