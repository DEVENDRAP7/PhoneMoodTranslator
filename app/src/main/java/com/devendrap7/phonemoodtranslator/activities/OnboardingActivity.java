package com.devendrap7.phonemoodtranslator.activities;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.onboarding.OnboardingPage;
import com.devendrap7.phonemoodtranslator.onboarding.OnboardingPagerAdapter;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity
        implements OnboardingPagerAdapter.ActionListener {

    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final int REQ_NOTIFICATIONS = 101;

    // Page indices
    private static final int PAGE_WELCOME = 0;
    private static final int PAGE_USAGE = 1;
    private static final int PAGE_NOTIF = 2;
    private static final int PAGE_BATTERY = 3;

    private ViewPager2 viewPager;
    private OnboardingPagerAdapter adapter;
    private Button btnNext, btnSkip;
    private LinearLayout dotsLayout;

    private final List<OnboardingPage> pages = Arrays.asList(
            new OnboardingPage(
                    "🐾",
                    "Welcome to DigiPulse",
                    "Your digital wellbeing companion that translates your screen time into moods through a living, breathing pet.\n\nEvery time you tap Translate, your pet reacts to your phone habits — from 🌿 Unplugged to 🤯 Overdose!",
                    null,
                    OnboardingPage.TYPE_INFO),
            new OnboardingPage(
                    "📊",
                    "Allow Usage Access",
                    "DigiPulse needs to see which apps you use and for how long. This is the core data that powers your mood analysis.\n\nYour data never leaves your phone — it's 100% private and stored only on your device.",
                    "Grant Usage Access",
                    OnboardingPage.TYPE_USAGE),
            new OnboardingPage(
                    "🔔",
                    "Enable Notifications",
                    "Get timely alerts when you're spending too much time on social media or your screen time exceeds your limit.\n\nYou'll also receive your personal note — a message from present-you to future-you! 📝",
                    "Allow Notifications",
                    OnboardingPage.TYPE_NOTIF),
            new OnboardingPage(
                    "🔋",
                    "Enable Background Tracking",
                    "To automatically track your usage even when the app is closed, DigiPulse needs to run in the background.\n\nWithout this, tracking only works manually when you open the app.",
                    "Enable Background Tracking",
                    OnboardingPage.TYPE_BATTERY));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isOnboardingCompleted()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_onboarding_simple);

        // Status bar styling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#F7E7CE"));
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        dotsLayout = findViewById(R.id.dotsLayout);

        adapter = new OnboardingPagerAdapter(pages, this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // manual navigation only

        buildDots(pages.size(), 0);
        updateButtonState(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                buildDots(pages.size(), position);
                updateButtonState(position);
                // Refresh granted state when page becomes visible
                refreshGrantedState(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current == pages.size() - 1) {
                // Last page — finish onboarding
                saveOnboardingCompleted();
                navigateToMain();
            } else {
                viewPager.setCurrentItem(current + 1, true);
            }
        });

        btnSkip.setOnClickListener(v -> {
            saveOnboardingCompleted();
            navigateToMain();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the current page's granted state after returning from Settings
        int current = viewPager != null ? viewPager.getCurrentItem() : 0;
        refreshGrantedState(current);
    }

    // ── Permission button tapped ──────────────────────────────────────────────

    @Override
    public void onActionButtonClicked(int pageType) {
        switch (pageType) {
            case OnboardingPage.TYPE_USAGE:
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                break;

            case OnboardingPage.TYPE_NOTIF:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(
                            new String[] { android.Manifest.permission.POST_NOTIFICATIONS },
                            REQ_NOTIFICATIONS);
                } else {
                    // Pre-13: notifications are on by default
                    adapter.setGranted(PAGE_NOTIF, true);
                }
                break;

            case OnboardingPage.TYPE_BATTERY:
                try {
                    Intent intent = new Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            adapter.setGranted(PAGE_NOTIF, granted);
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private void refreshGrantedState(int page) {
        switch (page) {
            case PAGE_USAGE:
                adapter.setGranted(PAGE_USAGE, hasUsageAccess());
                break;
            case PAGE_NOTIF:
                adapter.setGranted(PAGE_NOTIF, hasNotificationPermission());
                break;
            case PAGE_BATTERY:
                adapter.setGranted(PAGE_BATTERY, isBatteryOptimizationDisabled());
                break;
        }
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Pre-13 always on
    }

    private boolean isBatteryOptimizationDisabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true; // Pre-M, no restriction
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void updateButtonState(int position) {
        boolean isLast = position == pages.size() - 1;
        btnNext.setText(isLast ? "Get Started 🚀" : "Next →");
        btnSkip.setVisibility(isLast ? View.GONE : View.VISIBLE);
    }

    private void buildDots(int count, int activeIndex) {
        dotsLayout.removeAllViews();
        for (int i = 0; i < count; i++) {
            TextView dot = new TextView(this);
            dot.setText(i == activeIndex ? "●" : "○");
            dot.setTextSize(18f);
            dot.setTextColor(i == activeIndex
                    ? Color.parseColor("#1c1554")
                    : Color.parseColor("#BBBBBB"));
            dot.setPadding(6, 0, 6, 0);
            dotsLayout.addView(dot);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private boolean isOnboardingCompleted() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    private void saveOnboardingCompleted() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, true)
                .apply();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Allow going back within onboarding pages
        if (viewPager != null && viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        }
        // Block going back from the first page
    }
}