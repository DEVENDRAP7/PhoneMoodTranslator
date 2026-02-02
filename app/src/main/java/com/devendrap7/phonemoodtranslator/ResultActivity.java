package com.devendrap7.phonemoodtranslator;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {

    // Views
    private TextView tvEmoji, tvTitle, tvDescription, tvUsageDetails, tvReflection, tvMoodCriteria;
    private BarChart barChart;
    private Button btnDone;
    private ViewGroup rootLayout; // The main container

    // NEW: The Mood Pet View
    private MoodPetView moodPet;

    // State
    private boolean isShowingCriteria = false;

    // Data
    private String moodTitle;
    private int usageMinutes, appOpens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // 1. Initialize Views First
        initializeViews();

        // 2. Apply the User's Custom Theme (Background & Text)
        applySavedTheme();
    }

    private void applySavedTheme() {
        // Get the color saved from MainActivity (Color Picker)
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int savedColor = prefs.getInt("bg_color", Color.parseColor("#4A148C")); // Default Purple

        // Find root layout (Make sure your XML has ID: rootLayout)
        View root = findViewById(R.id.rootLayout);
        if (root == null) {
            // Fallback if ID is missing in XML
            root = findViewById(android.R.id.content);
        }
        root.setBackgroundColor(savedColor);

        // Smart Text Contrast: Check if background is Dark or Light
        boolean isDark = isColorDark(savedColor);
        int textColor = isDark ? Color.WHITE : Color.BLACK;

        // Apply text color to all text views
        tvTitle.setTextColor(textColor);
        tvDescription.setTextColor(textColor);
        tvUsageDetails.setTextColor(textColor);
        tvReflection.setTextColor(textColor);
        tvMoodCriteria.setTextColor(textColor);

        // Emoji looks good in both, but we can update it too
        tvEmoji.setTextColor(textColor);
        Button btnDone = findViewById(R.id.btnDone);
        if (btnDone != null) {
            int contrastColor = isDark ? Color.WHITE : Color.BLACK;
            btnDone.setBackgroundTintList(android.content.res.ColorStateList.valueOf(contrastColor));
            btnDone.setTextColor(savedColor);
        }
    }

    // Helper: Returns true if the color is dark
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private void initializeViews() {
        // We need the root layout for animations
        rootLayout = findViewById(android.R.id.content);

        // Get views
        tvEmoji = findViewById(R.id.tvEmoji);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvUsageDetails = findViewById(R.id.tvUsageDetails);
        tvReflection = findViewById(R.id.tvReflection);
        tvMoodCriteria = findViewById(R.id.tvMoodCriteria);
        btnDone = findViewById(R.id.btnDone);
        barChart = findViewById(R.id.barChart);

        // NEW: Initialize the Dog
        moodPet = findViewById(R.id.moodPet);

        // Get data from intent
        String emoji = getIntent().getStringExtra("emoji");
        moodTitle = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        usageMinutes = getIntent().getIntExtra("usageMinutes", 0);
        appOpens = getIntent().getIntExtra("appOpens", 0);
        String topAppName = getIntent().getStringExtra("topAppName");
        int topAppMinutes = getIntent().getIntExtra("topAppMinutes", 0);
        boolean lateNight = getIntent().getBooleanExtra("lateNight", false);

        // NEW: Feed Data to the Dog
        if (moodPet != null) {
            moodPet.setMoodData(usageMinutes, appOpens);
            // Ensure it starts hidden (We only show it in Criteria Mode)
            moodPet.setVisibility(View.GONE);
        }

        // Set text with animations
        setTextWithAnimation(tvEmoji, emoji != null ? emoji : "📱");
        setTextWithAnimation(tvTitle, moodTitle != null ? moodTitle : "Your Mood");
        setTextWithAnimation(tvDescription, description != null ? description : "");

        // Format usage details
        String usageText = formatUsageDetails(usageMinutes, appOpens, topAppName, topAppMinutes, lateNight);
        setTextWithAnimation(tvUsageDetails, usageText);

        // Set reflection
        String reflection = getReflectionLine(moodTitle, usageMinutes, appOpens, lateNight);
        setTextWithAnimation(tvReflection, reflection);

        // Setup chart (Pass topAppName for dynamic coloring)
        setupBarChart(barChart, usageMinutes, appOpens, topAppMinutes, topAppName);

        // ---------------------------------------------------------
        // INTERACTIVE CLICK LISTENERS
        // ---------------------------------------------------------
        View.OnClickListener toggleListener = v -> toggleCriteriaMode();

        // Tap Emoji or Title to switch modes
        tvEmoji.setOnClickListener(toggleListener);
        tvTitle.setOnClickListener(toggleListener);

        // Done button
        btnDone.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    // =========================================================
    // SMOOTH TRANSITION LOGIC (UPDATED WITH DOG)
    // =========================================================
    private void toggleCriteriaMode() {
        TransitionManager.beginDelayedTransition(rootLayout, new AutoTransition());

        if (isShowingCriteria) {
            // --- GOING BACK TO STATS ---
            tvUsageDetails.setVisibility(View.VISIBLE);
            barChart.setVisibility(View.VISIBLE);
            tvReflection.setVisibility(View.VISIBLE);

            // Hide Criteria & Dog
            tvMoodCriteria.setVisibility(View.GONE);
            if (moodPet != null) moodPet.setVisibility(View.GONE); // HIDE DOG

            tvDescription.setText(getIntent().getStringExtra("description"));
            isShowingCriteria = false;
        } else {
            // --- SHOWING CRITERIA (WHY) ---
            tvUsageDetails.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
            tvReflection.setVisibility(View.GONE);

            tvMoodCriteria.setText(getCriteriaExplanation(moodTitle));
            tvMoodCriteria.setVisibility(View.VISIBLE);

            // Show Dog
            if (moodPet != null) moodPet.setVisibility(View.VISIBLE); // SHOW DOG

            tvDescription.setText("(Tap again to see stats)");
            isShowingCriteria = true;
        }
    }

    private String getCriteriaExplanation(String title) {
        if (title == null) return "Criteria unknown.";
        String userStats = String.format("\n\nYour Stats:\n⏱ %dh %dm\n📱 %d unlocks",
                usageMinutes/60, usageMinutes%60, appOpens);

        switch (title) {
            case "Overdose": return "Why Overdose?\n\n• Usage > 5 hours\n• Extremely high consumption." + userStats;
            case "Tethered": return "Why Tethered?\n\n• Usage between 4 - 5 hours\n• Borderline heavy usage." + userStats;
            case "Late-Night Thinker": return "Why Late-Night?\n\n• Active between 11 PM - 4 AM\n• Regardless of total duration." + userStats;
            case "Hyperfocused": return "Why Hyperfocused?\n\n• Usage > 2.5 hours\n• Very low switching (< 15 opens)." + userStats;
            case "Restless Energy": return "Why Restless?\n\n• Usage > 2.5 hours\n• High switching (> 45 opens)." + userStats;
            case "Distracted Mind": return "Why Distracted?\n\n• Lower usage (< 2.5h)\n• High switching (> 40 opens)." + userStats;
            case "Serious Mode": return "Why Serious?\n\n• Moderate usage\n• Low switching (< 30 opens)." + userStats;
            case "Light-hearted": return "Why Light-hearted?\n\n• Moderate usage\n• Casual switching (30+ opens)." + userStats;
            case "Slick": return "Why Slick?\n\n• Usage < 1.5 hours\n• Efficient interactions." + userStats;
            case "Unplugged": return "Why Unplugged?\n\n• Usage < 30 minutes\n• Almost zero screen time." + userStats;
            case "Calm & Grounded": return "Why Calm?\n\n• Usage < 4 hours\n• Balanced switching stats." + userStats;
            default: return "Calculated based on a balance of screen time and app switching frequency." + userStats;
        }
    }

    private void setTextWithAnimation(TextView textView, String text) {
        textView.setText(text);
        textView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private String formatUsageDetails(int usageMinutes, int appOpens, String topAppName, int topAppMinutes, boolean lateNight) {
        StringBuilder details = new StringBuilder();
        details.append("📊 Today's Phone Usage\n\n");
        if (usageMinutes >= 60) {
            details.append(String.format("⏱ Total time: %dh %dm\n", usageMinutes / 60, usageMinutes % 60));
        } else {
            details.append(String.format("⏱ Total time: %d minutes\n", usageMinutes));
        }
        details.append(String.format("📱 Apps opened: %d\n", appOpens));
        if (topAppName != null && !topAppName.isEmpty()) {
            if (topAppMinutes >= 60) {
                details.append(String.format("🏆 Most used: %s (%dh %dm)", topAppName, topAppMinutes / 60, topAppMinutes % 60));
            } else {
                details.append(String.format("🏆 Most used: %s (%d min)", topAppName, topAppMinutes));
            }
        }
        if (lateNight) details.append("\n🌙 Late night activity detected");
        return details.toString();
    }

    // =========================================================
    // BAR CHART SETUP (With Brand Colors)
    // =========================================================
    private void setupBarChart(BarChart barChart, int usageMinutes, int appOpens, int topAppMinutes, String topAppName) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, usageMinutes));
        entries.add(new BarEntry(1f, appOpens * 5));
        entries.add(new BarEntry(2f, topAppMinutes));

        final String[] labels = new String[]{
                "Total (min)", "App Opens (x5)",
                topAppName != null ? topAppName.substring(0, Math.min(topAppName.length(), 15)) : "Top App"
        };

        BarDataSet dataSet = new BarDataSet(entries, "");

        // IMPORTANT: Use the App Name to get the color
        dataSet.setColors(getBarColors(topAppName));

        // Ensure chart text color matches the theme (we use the same logic as TextViews)
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int savedColor = prefs.getInt("bg_color", Color.parseColor("#4A148C"));
        int chartTextColor = isColorDark(savedColor) ? Color.WHITE : Color.BLACK;

        dataSet.setValueTextColor(chartTextColor);
        dataSet.setValueTextSize(14f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < labels.length ? labels[index] : "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(chartTextColor);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);

        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawBorders(false);
        barChart.setDrawGridBackground(false);
        barChart.setDescription(new Description(){{setText("");}});
        barChart.setTouchEnabled(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private ArrayList<Integer> getBarColors(String topAppName) {
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#60A5FA")); // Blue
        colors.add(Color.parseColor("#34D399")); // Teal
        colors.add(getBrandColor(topAppName));   // Dynamic Brand Color
        return colors;
    }

    private int getBrandColor(String appName) {
        if (appName == null) return Color.parseColor("#FBBF24"); // Default Amber
        String lowerName = appName.toLowerCase();

        if (lowerName.contains("youtube")) return Color.parseColor("#FF0000");
        else if (lowerName.contains("instagram")) return Color.parseColor("#C13584");
        else if (lowerName.contains("whatsapp")) return Color.parseColor("#25D366");
        else if (lowerName.contains("spotify")) return Color.parseColor("#1DB954");
        else if (lowerName.contains("snapchat")) return Color.parseColor("#FFFC00");
        else if (lowerName.contains("twitter") || lowerName.contains(" x ")) return Color.parseColor("#000000");
        else if (lowerName.contains("facebook")) return Color.parseColor("#1877F2");
        else if (lowerName.contains("chrome") || lowerName.contains("google")) return Color.parseColor("#4285F4");

        return Color.parseColor("#FBBF24"); // Default
    }

    private String getReflectionLine(String moodTitle, int usageMinutes, int appOpens, boolean lateNight) {
        if (moodTitle == null) return "Take a moment to reflect.";
        switch (moodTitle) {
            case "Hyperfocused": return "Your attention stayed with one thing longer than usual.";
            case "Late-Night Thinker": return "Some thoughts chose the night instead of rest.";
            case "Restless Energy": return "Today felt full, but not always settled.";
            case "Distracted Mind": return "Your attention moved faster than your intentions.";
            case "Unplugged": return "A day lived beyond the screen. Balance found.";
            case "Calm & Grounded": return "Nothing extreme today. And that's a kind of balance.";
            case "Overdose": return "Digital noise drowned out the quiet. Time to recharge offline.";
            case "Tethered": return "You were connected more than necessary today. A break is due.";
            case "Serious Mode": return "Purposeful and steady. You used your device as a tool, not a toy.";
            case "Light-hearted": return "Just a casual day of browsing. Nothing heavy, just exploring.";
            case "Slick": return "Fast, efficient, and done. You got what you needed and left.";
            default: return "Your digital day was unique.";
        }
    }
}