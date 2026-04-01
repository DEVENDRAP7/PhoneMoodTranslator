package com.devendrap7.phonemoodtranslator.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devendrap7.phonemoodtranslator.activities.MainActivity;
import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;
import com.devendrap7.phonemoodtranslator.viewmodels.MoodViewModel;
import com.devendrap7.phonemoodtranslator.views.MoodPetView;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AnalysisFragment extends Fragment {


    private BarChart barChart;

    private PieChart pieChart;
    private TextView tvHeader,trend;
    private LinearLayout llAppAnalysisList;
    private MaterialButtonToggleGroup toggleGroup;
    private boolean isFirstLoad = true;

    private boolean isMonthly = false;
    private List<DailyStats> historyStatsList = new ArrayList<>();
    private DailyStats todayStats = null;
    private Map<String, Drawable> iconCache = new HashMap<>();
    private View loadingContainer; // The FrameLayout with the pet
    private View mainContentScroll; // The ScrollView with charts
    private MoodPetView loadingPet; // Your custom pet view


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis, container, false);

        // A. FIND ALL VIEWS FIRST (Stops the NullPointerException crash)
        loadingContainer = view.findViewById(R.id.loading_container);
        mainContentScroll = view.findViewById(R.id.mainContentLayout);
        loadingPet = view.findViewById(R.id.loading_pet);
        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChartApps);
        tvHeader = view.findViewById(R.id.tvChartHeader);
        llAppAnalysisList = view.findViewById(R.id.llAppAnalysisList);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        // If your method looks like: public void onViewCreated(View v, ...)


        // B. SETUP LISTENERS
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isMonthly = (checkedId == R.id.btnMonthly);
                tvHeader.setText(isMonthly ? "Monthly Average (Jan - Dec)" : "This Week (Mon-Sun)");
                loadChartData();
            }
        });
        // 1. Initialize the ViewModel using 'requireActivity()'
// This ensures it uses the EXACT SAME brain as the MainActivity
        MoodViewModel moodViewModel = new ViewModelProvider(requireActivity()).get(MoodViewModel.class);
        moodViewModel.init(requireContext());
        // Force a refresh when fragment opens



// 2. Set up the Observer
        moodViewModel.getTodayStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                // This block runs every time the Translate button is clicked!
                setupPieChartToday(stats);
                updateAppList(Collections.singletonList(stats));

                // Update the mascot/dashboard details
                int mins = (int) (stats.totalUsageTime / 60000);
            }
        });
        loadChartData();

        // C. START THE PROCESS (Only called once now)
        startPetPulseAnimation();
        return view;
    }
    private void startPetPulseAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(loadingPet, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(loadingPet, "scaleY", 1f, 1.05f, 1f);

        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setDuration(1200);
        scaleY.setDuration(1200);

        pulse.start();
    }

    // Pass your calculated variables into this method
//    private void revealInsightsDashboard(int totalMinutes, int totalOpens) {
//        // 1. Immediately update the Pet's color/face
//        loadingPet.setMoodData(totalMinutes, totalOpens);
//
//        // 2. Prepare the Chart Content
//        mainContentScroll.setAlpha(0f);
//        mainContentScroll.setVisibility(View.INVISIBLE);
//        barChart.setVisibility(View.INVISIBLE);
//
//        loadingContainer.animate()
//                .alpha(0f)
//                .setDuration(2500)
//                .withEndAction(() -> {
//                    loadingContainer.setVisibility(View.GONE);
//                    loadingPet.animate().cancel();
//                });
//
//        barChart.setVisibility(View.VISIBLE);
//        mainContentScroll.setVisibility(View.VISIBLE);
//        // 3. Slow Fade In for Charts (2 Seconds)
//        mainContentScroll.animate()
//                .alpha(1f)
//                .setDuration(500)
//                .start();
//    }
    private void revealInsightsDashboard(int totalMinutes) {
        loadingPet.setMoodData(totalMinutes);

        // If already revealed, don't do it again
        if (loadingContainer.getVisibility() == View.GONE) return;

        // Fast reveal if we are refreshing, slow reveal only for first-load
        loadingContainer.animate()
                .alpha(0f)
                .setDuration(2000) // Reduced from 2500 to stop the "waiting" feel
                .withEndAction(() -> {
                    loadingPet.setMoodData(totalMinutes);
                    loadingContainer.setVisibility(View.GONE);
                    mainContentScroll.setVisibility(View.VISIBLE);
                    mainContentScroll.setAlpha(1f);
                });

        mainContentScroll.animate().alpha(1f).setDuration(500).start();

    }

    private void loadChartData() {
        boolean isContentAlreadyVisible = (mainContentScroll.getVisibility() == View.VISIBLE);

        if (!isContentAlreadyVisible) {
            loadingContainer.setVisibility(View.VISIBLE);
            loadingContainer.setAlpha(1f);
            mainContentScroll.setVisibility(View.INVISIBLE);
            barChart.setVisibility(View.INVISIBLE);
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());

            if (isMonthly) {
                historyStatsList = db.statsDao().getMonthlyStats(); // ✅ Need monthly query!
            } else {
                historyStatsList = db.statsDao().getWeeklyStats();

                // ✅ Debug log
                android.util.Log.d("WEEKLY_DEBUG", "=== WEEKLY STATS ===");
                android.util.Log.d("WEEKLY_DEBUG", "Count: " + historyStatsList.size());
                for (DailyStats s : historyStatsList) {
                    android.util.Log.d("WEEKLY_DEBUG", "Date: " + s.date
                            + " | timestamp: " + s.dateTimestamp
                            + " | dayOfMonth: " + s.dayOfMonth
                            + " | usage: " + s.totalUsageTime);
                }
// Change this
                boolean allZero = true;
                for (DailyStats s : historyStatsList) {
                    if (s.dateTimestamp > 0) { allZero = false; break; }
                }

// To this ✅ — update ALL old records with correct timestamps
                for (DailyStats s : historyStatsList) {
                    if (s.dateTimestamp == 0 && s.date != null) {
                        try {
                            SimpleDateFormat pSdf = new SimpleDateFormat(
                                    "dd MMM yyyy", Locale.ENGLISH);
                            pSdf.setTimeZone(TimeZone.getDefault());
                            Calendar c = Calendar.getInstance(
                                    TimeZone.getDefault());
                            c.setTime(pSdf.parse(s.date));
                            s.dateTimestamp = c.getTimeInMillis();
                            s.dayOfMonth    = c.get(Calendar.DAY_OF_MONTH);
                            db.statsDao().update(s); // ✅ fix old records in DB
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
                if (allZero) {
                    android.util.Log.d("WEEKLY_DEBUG", "All timestamps zero — using fallback query");
                    historyStatsList = db.statsDao().getWeeklyStatsFallback();
                    android.util.Log.d("WEEKLY_DEBUG", "Fallback count: " + historyStatsList.size());
                    for (DailyStats s : historyStatsList) {
                        android.util.Log.d("WEEKLY_DEBUG", "Fallback Date: " + s.date
                                + " | usage: " + s.totalUsageTime);
                    }
                }
            }

            // ✅ Always use Locale.ENGLISH to match DB storage format
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getDefault());
            String todayDate = sdf.format(Calendar.getInstance(
                    TimeZone.getDefault()).getTime());

            android.util.Log.d("WEEKLY_DEBUG", "Today string: " + todayDate);

            todayStats = db.statsDao().getStatsByDate(todayDate);

            android.util.Log.d("WEEKLY_DEBUG", "Today stats: "
                    + (todayStats != null ? todayStats.totalUsageTime : "NULL"));

            if (!isAdded()) return;
            preloadIcons(historyStatsList);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (historyStatsList != null && !historyStatsList.isEmpty()) {
                        if (isMonthly) setupBarChartMonthlyGrouped(barChart, historyStatsList);
                        else setupBarChartWeekly(barChart, historyStatsList);
                    } else {
                        android.util.Log.d("WEEKLY_DEBUG", "⚠️ historyStatsList is EMPTY — no bars to show!");
                    }

                    setupPieChartToday(todayStats);
                    updateAppList(historyStatsList);

                    int mins  = 0;
                    int opens = 0;
                    if (todayStats != null) {
                        mins  = (int) (todayStats.totalUsageTime / 60000);
                        opens = todayStats.unlockCount;
                    }

                    loadingPet.setMoodData(mins);

                    if (!isContentAlreadyVisible) {
                        loadingContainer.animate()
                                .alpha(0f)
                                .setDuration(800)
                                .withEndAction(() -> {
                                    loadingContainer.setVisibility(View.GONE);
                                    barChart.setVisibility(View.VISIBLE);
                                    mainContentScroll.setVisibility(View.VISIBLE);
                                    mainContentScroll.animate()
                                            .alpha(1f)
                                            .setDuration(400)
                                            .start();
                                }).start();
                    } else {
                        barChart.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }
    private void setupBarChartWeekly(BarChart chart, List<DailyStats> stats) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xLabels  = new ArrayList<>();

        // ✅ Start from Monday of current week
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // ✅ Always use Locale.ENGLISH to match DB storage
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());

        SimpleDateFormat dayLabelFmt = new SimpleDateFormat("E", Locale.ENGLISH);
        dayLabelFmt.setTimeZone(TimeZone.getDefault());

        for (int i = 0; i < 7; i++) {
            String dateString = sdf.format(cal.getTime());
            xLabels.add(dayLabelFmt.format(cal.getTime()));

            // ✅ Default to 0 — shows bar slot even if no DB record
            float hours = 0f;

            for (DailyStats stat : stats) {
                // ✅ Normalize stat.date to English for comparison
                String normalizedDate = stat.date;
                try {
                    // Re-parse and re-format in English to handle locale issues
                    SimpleDateFormat parser = new SimpleDateFormat(
                            "dd MMM yyyy", Locale.ENGLISH);
                    parser.setTimeZone(TimeZone.getDefault());
                    normalizedDate = parser.format(parser.parse(stat.date));
                } catch (Exception e) {
                    // If parsing fails keep original
                }

                if (normalizedDate.equals(dateString)) {
                    hours = stat.totalUsageTime / (1000f * 60 * 60);
                    break;
                }
            }

            entries.add(new BarEntry(i, hours));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        renderBarChart(chart, entries, xLabels,true,true);
    }
    private void setupBarChartMonthlyGrouped(BarChart chart, List<DailyStats> stats) {
        // 1. Define all 12 months in order
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        // 2. Maps to store usage and day counts
        Map<String, Long> monthTotalUsage = new HashMap<>();
        Map<String, Integer> monthDayCount = new HashMap<>();

        // Initialize maps with 0s for every month
        for (String m : months) {
            monthTotalUsage.put(m, 0L);
            monthDayCount.put(m, 0);
        }

        SimpleDateFormat dateParser = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
        // 3. Process the data you actually have
        for (DailyStats stat : stats) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateParser.parse(stat.date));
                String monthName = monthFormat.format(cal.getTime());

                if (monthTotalUsage.containsKey(monthName)) {
                    monthTotalUsage.put(monthName, monthTotalUsage.get(monthName) + stat.totalUsageTime);
                    monthDayCount.put(monthName, monthDayCount.get(monthName) + 1);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 4. Build entries for all 12 months
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();

        for (int i = 0; i < months.length; i++) {
            String month = months[i];
            long total = monthTotalUsage.get(month);
            int count = monthDayCount.get(month);

            float avgHours = 0f;
            if (count > 0) {
                avgHours = (total / (float) count) / (1000f * 60 * 60); // Convert to Avg Hours
            }

            entries.add(new BarEntry(i, avgHours));
            xLabels.add(month);
        }

        renderBarChart(chart, entries, xLabels,true,true);
    }

    private void renderBarChart(BarChart chart, ArrayList<BarEntry> entries, ArrayList<String> xLabels, boolean showYLabels , boolean showBarValues) {
        BarDataSet dataSet = new BarDataSet(entries, "Usage");
        dataSet.setDrawValues(showBarValues);
        int textColor = Color.BLACK;
        // Inside renderBarChart, replace setColors with this:
        dataSet.setGradientColor(Color.parseColor("#43c197"), Color.parseColor("#1c1554"));
        // dataSet.setColors(ColorTemplate.LIBERTY_COLORS);
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(10f);

        // Formatter for values on TOP of the bars
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                int h = (int) value;
                int m = (int) Math.round((value - h) * 60);
                if (h > 0 && m > 0) return h + "h " + m + "m";
                if (h > 0) return h + "h";
                return m + "m";
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        chart.setData(data);

        // Chart styling
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setPinchZoom(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setGridBackgroundColor(Color.TRANSPARENT);

        // 1. ADDED: Padding so wide labels (e.g., 2h 30m) aren't cut off
        chart.setExtraLeftOffset(20f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColor);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12); // Forces all 12 labels to show


        com.github.mikephil.charting.components.YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#20000000"));
        leftAxis.setAxisMinimum(0f);
        // 2. THE FIX: Calculate Axis Max and Label Count to prevent skipping 2h 30m
        float maxY = 0;
        for (BarEntry entry : entries) {
            if (entry.getY() > maxY) maxY = entry.getY();
        }
        // Round up to nearest 0.5 for a clean scale top
        float axisMax = (float) (Math.ceil(maxY * 2) / 2.0);
        if (axisMax < 1) axisMax = 1; // Minimum view of 1 hour

        leftAxis.setAxisMaximum(axisMax);

        // 3. FORCE EVERY STEP: (Total Hours / 0.5) + 1 for zero
        int labelCount = (int) (axisMax / 0.5f) + 1;

        LimitLine ll = new LimitLine(6f, "Daily Limit");
        ll.setLineColor(Color.RED);
        ll.setLineWidth(2f);
        ll.enableDashedLine(10f, 10f, 0f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll.setTextSize(10f);

        // Inside renderBarChart
        chart.setBackgroundColor(Color.parseColor("#F7E7CE")); // Soft "Cloud" Blue

        leftAxis.addLimitLine(ll);

        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(0.5f);
        leftAxis.setLabelCount(labelCount, true);

        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                float snappedValue = Math.round(value * 2) / 2f;
                if (snappedValue <= 0) return "0";
                int hours = (int) snappedValue;
                int minutes = (int) Math.round((snappedValue - hours) * 60);

                // ✅ Half hours — return empty string (grid line shows, no text)
                if (minutes >= 25 && minutes <= 35) return "";

                // ✅ Whole hours — show label
                if (minutes < 5) return hours + "h";

                return hours + "h " + minutes + "m";
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.animateY(12000, Easing.EaseOutBack);
        // ✅ Control Y axis label visibility
        leftAxis.setDrawLabels(showYLabels);
        leftAxis.setDrawAxisLine(showYLabels);
        leftAxis.setDrawGridLines(showYLabels);
        chart.invalidate();
    }

//    private void setupAppDetailMonthlyChart(BarChart chart, String appName, List<DailyStats> stats) {
//        ArrayList<BarEntry> entries = new ArrayList<>();
//        ArrayList<String> xLabels = new ArrayList<>();
//        Gson gson = new Gson();
//        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
//
//        // 1. Get only the last 30 days of data
//        int count = Math.min(stats.size(), 30);
//
//        // 2. Loop BACKWARDS to show chronological Left-to-Right (Oldest to Newest)
//        for (int i = count - 1; i >= 0; i--) {
//            DailyStats day = stats.get(i);
//            long usage = getAppUsageForDay(day, appName, gson, listType);
//
//            // chartPosition starts at 0 and goes up
//            int chartPosition = (count - 1) - i;
//            entries.add(new BarEntry(chartPosition, usage / 3600000f));
//
//            // Label with just the Day Number (e.g., "12")
//            xLabels.add(day.date.substring(0, 2));
//        }
//
//        renderBarChart(chart, entries, xLabels,false,true);
//    }
    private void setupPieChartToday(DailyStats todayStats) {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        ArrayList<Integer> customColors = new ArrayList<>();

        long totalMsForMascot = 0;
        List<MainActivity.AppUsageInfo> apps = null;

        if (todayStats != null && todayStats.topAppsJson != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
            apps = gson.fromJson(todayStats.topAppsJson, listType);

            for (MainActivity.AppUsageInfo app : apps) {
                totalMsForMascot += app.usageTime;
            }
        }
        int totalMinutes = (int) (totalMsForMascot / 60000);



        customColors.add(Color.parseColor("#cdb4db")); // Deep Purple
        customColors.add(Color.parseColor("#ffc8dd")); //cream
        customColors.add(Color.parseColor("#ffafcc")); // Teal
        customColors.add(Color.parseColor("#a2d2ff")); // Orange
        customColors.add(Color.parseColor("#bde0fe")); // Pink
        customColors.add(Color.parseColor("#b8e0d2")); // Grey (For "Others")

        if (todayStats != null && todayStats.topAppsJson != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
            //List<MainActivity.AppUsageInfo> appslist = gson.fromJson(todayStats.topAppsJson, listType);
            List<MainActivity.AppUsageInfo> cleanApps = new ArrayList<>();

            for (MainActivity.AppUsageInfo app : apps) {
                String name = app.name.toLowerCase();
                if (!name.contains("home screen") &&
                        !name.contains("launcher") &&
                        !name.contains("system ui") &&
                        !name.contains("one ui") &&
                        !name.contains("samsung")) {
                    cleanApps.add(app);
                }
            }
            cleanApps.sort((a, b) -> Long.compare(b.usageTime, a.usageTime));
            long othersTime = 0;
            for (int i = 0; i < cleanApps.size(); i++) {
                MainActivity.AppUsageInfo app = cleanApps.get(i);
                float minutes = app.usageTime / 60000f;
                if (i < 5) {
                    if (minutes > 1) pieEntries.add(new PieEntry(minutes, app.name));
                } else {
                    othersTime += app.usageTime;
                }
            }
            if (othersTime > 0) {
                pieEntries.add(new PieEntry(othersTime / 60000f, "Others"));
            }
        }
        if (pieEntries.isEmpty()) {
            pieEntries.add(new PieEntry(1, "No Usage Yet"));
            customColors.clear();
            customColors.add(Color.LTGRAY); // Grey for empty state
        }

        Context ctx = getContext();
        if (ctx == null) return;
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        int bgColor = prefs.getInt("bg_color", Color.parseColor("#4A148C"));
        double darkness = 1 - (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255;
        int dynamicTextColor = (darkness >= 0.5) ? Color.WHITE : Color.BLACK;

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(customColors);

        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);

        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Screen Time\n stats");
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        pieChart.setCenterTextColor(Color.parseColor("#333333"));
        pieChart.setHoleRadius(55f);
        pieChart.setHoleColor(Color.parseColor("#F7E7CE"));
        pieChart.setTransparentCircleRadius(60f);

        // Match the chart background to your overall layout
        pieChart.setBackgroundColor(Color.parseColor("#F7E7CE"));

        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(14f);
        pieDataSet.setValueTypeface(Typeface.DEFAULT_BOLD); // Bold text
        pieDataSet.setValueFormatter(new ValueFormatter() {

            @Override
            public String getFormattedValue(float value) {
                long totalMinutes = (long) value;
                if (value < 5) return "";
                if (totalMinutes < 60) return totalMinutes + "m";
                return (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m";
            }
        });
        pieChart.setData(new PieData(pieDataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        //pieChart.animateXY(800, 800);
        pieChart.setHoleColor(Color.parseColor("#F7E7CE"));

        pieChart.setBackgroundColor(Color.parseColor("#F7E7CE"));
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTypeface(Typeface.DEFAULT_BOLD);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(5000, Easing.EaseInOutQuad);
        //pieChart.spin(1000,0,360f,Easing.EaseInOutQuad);




        pieChart.invalidate();

    }
    private void updatePopupChart(BarChart detailChart, String appName,
                                  boolean showMonthly,
                                  TextView tvWeekly, TextView tvMonthly,
                                  TextView tvTotal,
                                  TextView tvLabelWeekly,
                                  TextView tvLabelMonthly,
                                  TextView tvLabelTotal,
                                  List<DailyStats> dataToUse) { // ✅ New param {
        if (showMonthly) {
            setupAppDetailMonthlyChart(detailChart, appName, dataToUse, // ✅ Use passed data
                    tvWeekly, tvMonthly, tvTotal,
                    tvLabelWeekly, tvLabelMonthly, tvLabelTotal);
        } else {
            setupAppDetailWeeklyChart(detailChart, appName, dataToUse, // ✅ Use passed data
                    tvWeekly, tvMonthly, tvTotal,
                    tvLabelWeekly, tvLabelMonthly, tvLabelTotal);
        }
    }
    private void updateAppList(List<DailyStats> statsList) {
        llAppAnalysisList.removeAllViews();
        Map<String, Long> appTotalMap = new HashMap<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());
        String todayDate = sdf.format(Calendar.getInstance(TimeZone.getDefault()).getTime());

        for (DailyStats day : statsList) {
            // Only sum usage if it matches today's date for the progress bars
            if (day.date.equals(todayDate) && day.topAppsJson != null) {
                List<MainActivity.AppUsageInfo> apps = gson.fromJson(day.topAppsJson, listType);
                for (MainActivity.AppUsageInfo app : apps) {
                    appTotalMap.put(app.name, appTotalMap.getOrDefault(app.name, 0L) + app.usageTime);
                }
            }
        }

        int daysCount = statsList.isEmpty() ? 1 : statsList.size();
        List<Map.Entry<String, Long>> sortedApps = new ArrayList<>(appTotalMap.entrySet());
        Collections.sort(sortedApps, (a, b) -> Long.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Long> entry : sortedApps) {
            if (entry.getValue() > 1000) {
                long avg = entry.getValue() / daysCount;
                llAppAnalysisList.addView(createAppRow(entry.getKey(), entry.getValue(), avg));
                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                llAppAnalysisList.addView(divider);
            }
        }
    }

    private View createAppRow(String appName, long total, long avg) {
        // 1. The Main Card
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(32, 16, 32, 16);
        card.setLayoutParams(cardParams);
        card.setRadius(28f);
        card.setCardElevation(6f);
        card.setCardBackgroundColor(Color.parseColor("#F7E7CE"));

        // 2. The Horizontal Layout
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setPadding(32, 32, 32, 32);
        mainLayout.setGravity(Gravity.CENTER_VERTICAL);

        // 3. Icon with Circular Background
        ImageView icon = new ImageView(getContext());
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        icon.setPadding(4, 4, 4, 4);
        if (iconCache.containsKey(appName)) icon.setImageDrawable(iconCache.get(appName));

        // 4. Text and Progress Bar Vertical Layout
        LinearLayout infoLayout = new LinearLayout(getContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        infoLayout.setPadding(32, 0, 0, 0);

        // App Name
        TextView name = new TextView(getContext());
        name.setText(appName);
        name.setTextColor(Color.BLACK);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextSize(16f);

        // ✅ ADDED: The Time Duration Label
        TextView timeText = new TextView(getContext());
        timeText.setText(formatTime(total)); // Uses your existing formatting logic
        timeText.setTextColor(Color.parseColor("#424242")); // Subtle dark gray for contrast
        timeText.setTextSize(13f);
        timeText.setPadding(0, 4, 0, 8); // Add spacing between name and bar

        int usageMinutes = (int) (total / 60000);
        // Subtle Progress Bar
        ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8));
        progressBar.setMax(120);
        if(usageMinutes==0){
            progressBar.setProgress(0);
        }
        else{
            progressBar.setProgress(usageMinutes);
        }
        progressBar.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.progress_gradient));

        // Add views in order: Name -> Time -> Progress Bar
        infoLayout.addView(name);
        infoLayout.addView(timeText); // New view added here
        infoLayout.addView(progressBar);

        // Adds a "springy" touch feel
        card.setClickable(true);
        card.setFocusable(true);

        // 5. Assembly
        mainLayout.addView(icon);
        mainLayout.addView(infoLayout);
        card.addView(mainLayout);

        card.setOnClickListener(v -> showDetailedAppPopup(appName, icon.getDrawable()));
        return card;
    }

    private void showDetailedAppPopup(String appName, Drawable icon) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_details);

        // 1. Setup Window
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.75));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
        }

        // 2. Find Views
        MaterialButtonToggleGroup popupToggle =
                dialog.findViewById(R.id.popupToggleGroup);
        BarChart detailChart   = dialog.findViewById(R.id.detailBarChart);
        TextView tvWeekly      = dialog.findViewById(R.id.tvWeeklyAvg);
        TextView tvMonthly     = dialog.findViewById(R.id.tvMonthlyAvg);
        TextView tvTotal       = dialog.findViewById(R.id.tvTotalUsage);
        TextView tvLabelWeekly  = dialog.findViewById(R.id.tvLabelWeekly);
        TextView tvLabelMonthly = dialog.findViewById(R.id.tvLabelMonthly);
        TextView tvLabelTotal   = dialog.findViewById(R.id.tvLabelTotal);
        trend = dialog.findViewById(R.id.trend);

        ((ImageView) dialog.findViewById(R.id.imgDetailIcon))
                .setImageDrawable(icon);
        ((TextView) dialog.findViewById(R.id.tvDetailTitle))
                .setText(appName);

        Button btnWeek  = dialog.findViewById(R.id.btnPopupWeek);
        Button btnMonth = dialog.findViewById(R.id.btnPopupMonth);

        btnMonth.setBackgroundColor(Color.parseColor("#F7E7CE"));
        btnWeek.setBackgroundColor(Color.parseColor("#1c1554"));
        btnWeek.setTextColor(Color.WHITE);

        // 3. Handle Toggle Listener — single listener only
        popupToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            // Update trend label
            trend.setText(checkedId == R.id.btnPopupWeek
                    ? "Weekly Trend" : "Monthly Trend");

            // Reset both buttons
            btnWeek.setBackgroundColor(Color.parseColor("#F7E7CE"));
            btnWeek.setTextColor(Color.parseColor("#333333"));
            btnMonth.setBackgroundColor(Color.parseColor("#F7E7CE"));
            btnMonth.setTextColor(Color.parseColor("#333333"));

            // Highlight selected
            Button selected = dialog.findViewById(checkedId);
            selected.setBackgroundColor(Color.parseColor("#1c1554"));
            selected.setTextColor(Color.WHITE);

            boolean showMonthly = (checkedId == R.id.btnPopupMonth);
            if (showMonthly) {
                // Fetch fresh monthly data for popup
                new Thread(() -> {
                    AppDatabase db = AppDatabase.getDatabase(requireContext());
                    List<DailyStats> monthlyData = db.statsDao().getMonthlyStats();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updatePopupChart(detailChart, appName, true,
                                    tvWeekly, tvMonthly, tvTotal,
                                    tvLabelWeekly, tvLabelMonthly, tvLabelTotal,
                                    monthlyData); // ✅ Pass fresh data
                        });
                    }
                }).start();
            } else {
                updatePopupChart(detailChart, appName, false,
                        tvWeekly, tvMonthly, tvTotal,
                        tvLabelWeekly, tvLabelMonthly, tvLabelTotal,
                        historyStatsList); // ✅ Use cached weekly data
            }
        });

        // 4. Initial Load
        popupToggle.check(R.id.btnPopupWeek);
        updatePopupChart(detailChart, appName, false,
                tvWeekly, tvMonthly, tvTotal,
                tvLabelWeekly, tvLabelMonthly, tvLabelTotal,
                historyStatsList); // ✅ Add data param

        dialog.findViewById(R.id.btnCloseDetail)
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private long getAppUsageForDay(DailyStats day, String appName, Gson gson, Type listType) {
        if (day.topAppsJson == null) return 0;
        List<MainActivity.AppUsageInfo> apps = gson.fromJson(day.topAppsJson, listType);
        for (MainActivity.AppUsageInfo app : apps) { if (app.name.equals(appName)) return app.usageTime; }
        return 0;
    }


    private void preloadIcons(List<DailyStats> stats) {
        if (!isAdded()) return;
        Context ctx = getContext();
        if (ctx == null) return;
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> installedApps;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            installedApps = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA));
        } else {
            installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        }
        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
        for (DailyStats day : stats) {
            if (day.topAppsJson == null) continue;
            List<MainActivity.AppUsageInfo> apps = new Gson().fromJson(day.topAppsJson, listType);
            for (MainActivity.AppUsageInfo app : apps) {
                if (!iconCache.containsKey(app.name)) {
                    Drawable icon = null;
                    for (ApplicationInfo info : installedApps) { if (pm.getApplicationLabel(info).toString().equals(app.name)) { icon = pm.getApplicationIcon(info); break; } }
                    iconCache.put(app.name, icon != null ? icon : ContextCompat.getDrawable(ctx, android.R.drawable.sym_def_app_icon));
                }
            }
        }
    }
//    private void setupAppDetailWeeklyChart(BarChart detailChart, String appName,
//                                           List<DailyStats> stats,
//                                           TextView tvWeekly, TextView tvMonthly, TextView tvTotal,
//                                           TextView tvLabelWeekly, TextView tvLabelMonthly, TextView tvLabelTotal) {
//        ArrayList<BarEntry> entries = new ArrayList<>();
//        ArrayList<String> xLabels = new ArrayList<>();
//        long totalAppUsage = 0;
//        Gson gson = new Gson();
//        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
//
//        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
//        cal.set(Calendar.HOUR_OF_DAY, 0);
//        cal.set(Calendar.MINUTE, 0);
//        cal.set(Calendar.SECOND, 0);
//        cal.set(Calendar.MILLISECOND, 0);
//        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
//            cal.add(Calendar.DAY_OF_YEAR, -1);
//
//        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
//        String todayStr = sdf.format(Calendar.getInstance(TimeZone.getDefault()).getTime());
//        long todayUsage = 0;
//        int daysWithData = 0;
//
//        for (int i = 0; i < 7; i++) {
//            String target = sdf.format(cal.getTime());
//            long usage = 0;
//            for (DailyStats day : stats) {
//                if (day.date.equals(target)) {
//                    usage = getAppUsageForDay(day, appName, gson, listType);
//                    if (target.equals(todayStr)) todayUsage = usage;
//                    break;
//                }
//            }
//            totalAppUsage += usage;
//            if (usage > 0) daysWithData++;
//            entries.add(new BarEntry(i, usage / 3600000f));
//            xLabels.add(new SimpleDateFormat("E", Locale.ENGLISH).format(cal.getTime()));
//            cal.add(Calendar.DAY_OF_YEAR, 1);
//        }
//
//        if (tvLabelWeekly != null) tvLabelWeekly.setText("Today's Usage");
//        if (tvLabelMonthly != null) tvLabelMonthly.setText("Weekly Avg");
//        if (tvLabelTotal != null) tvLabelTotal.setText("Weekly Total");
//
//        long weeklyAvg = daysWithData > 0 ? totalAppUsage / daysWithData : 0;
//        if (tvWeekly != null) tvWeekly.setText(formatTime(todayUsage));
//        if (tvMonthly != null) tvMonthly.setText(formatTime(weeklyAvg));
//        if (tvTotal != null) tvTotal.setText(formatTime(totalAppUsage));
//
//        renderBarChart(detailChart, entries, xLabels, false);
//    }
//
//    private void setupAppDetailMonthlyChart(BarChart detailChart, String appName,
//                                            List<DailyStats> stats,
//                                            TextView tvWeekly, TextView tvMonthly, TextView tvTotal,
//                                            TextView tvLabelWeekly, TextView tvLabelMonthly, TextView tvLabelTotal) {
//        ArrayList<BarEntry> entries = new ArrayList<>();
//        ArrayList<String> xLabels = new ArrayList<>();
//        long totalAppUsage = 0;
//        Gson gson = new Gson();
//        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
//
//        // Get current month details
//        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
//        int currentMonth = cal.get(Calendar.MONTH);
//        int currentYear = cal.get(Calendar.YEAR);
//        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
//
//        // Set to first day of month
//        cal.set(Calendar.DAY_OF_MONTH, 1);
//        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
//        sdf.setTimeZone(TimeZone.getDefault());
//
//        int daysWithData = 0;
//
//        // Loop through ALL days of the month
//        for (int day = 1; day <= daysInMonth; day++) {
//            cal.set(Calendar.DAY_OF_MONTH, day);
//            String dateString = sdf.format(cal.getTime());
//
//            long usage = 0;
//            for (DailyStats stat : stats) {
//                if (stat.date.equals(dateString)) {
//                    usage = getAppUsageForDay(stat, appName, gson, listType);
//                    break;
//                }
//            }
//
//            totalAppUsage += usage;
//            if (usage > 0) daysWithData++;
//
//            entries.add(new BarEntry(day - 1, usage / 3600000f));
//            xLabels.add(String.valueOf(day));
//        }
//        // Update labels
//        if (tvLabelWeekly != null) tvLabelWeekly.setText("Days Used");
//        if (tvLabelMonthly != null) tvLabelMonthly.setText("Monthly Avg");
//        if (tvLabelTotal != null) tvLabelTotal.setText("Monthly Total");
//
//        // Update values
//        long monthlyAvg = daysWithData > 0 ? totalAppUsage / daysWithData : 0;
//        if (tvWeekly != null) tvWeekly.setText(daysWithData + "d");
//        if (tvMonthly != null) tvMonthly.setText(formatTime(monthlyAvg));
//        if (tvTotal != null) tvTotal.setText(formatTime(totalAppUsage));
//
//        renderBarChart(detailChart, entries, xLabels, true);
//
//    }
    private void setupAppDetailWeeklyChart(BarChart detailChart, String appName,
                                           List<DailyStats> stats,
                                           TextView tvWeekly, TextView tvMonthly, TextView tvTotal,
                                           TextView tvLabelWeekly, TextView tvLabelMonthly, TextView tvLabelTotal) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();
        long totalAppUsage = 0;

        // 1. Move GSON out of the loop
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();

        // 2. Pre-index stats by date for O(1) lookup speed
        Map<String, DailyStats> statsMap = new HashMap<>();
        for (DailyStats s : stats) statsMap.put(s.date, s);

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
            cal.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String todayStr = sdf.format(Calendar.getInstance(TimeZone.getDefault()).getTime());
        long todayUsage = 0;
        int daysWithData = 0;

        for (int i = 0; i < 7; i++) {
            String target = sdf.format(cal.getTime());
            long usage = 0;

            // 3. Instant lookup instead of nested for-loop
            DailyStats day = statsMap.get(target);
            if (day != null) {
                usage = getAppUsageForDay(day, appName, gson, listType);
                if (target.equals(todayStr)) todayUsage = usage;
            }

            totalAppUsage += usage;
            if (usage > 0) daysWithData++;
            entries.add(new BarEntry(i, usage / 3600000f));
            xLabels.add(new SimpleDateFormat("E", Locale.ENGLISH).format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // [Rest of your label and render logic remains exactly the same]
        if (tvLabelWeekly != null) tvLabelWeekly.setText("Today's Usage");
        if (tvLabelMonthly != null) tvLabelMonthly.setText("Weekly Avg");
        if (tvLabelTotal != null) tvLabelTotal.setText("Weekly Total");

        long weeklyAvg = daysWithData > 0 ? totalAppUsage / daysWithData : 0;
        if (tvWeekly != null) tvWeekly.setText(formatTime(todayUsage));
        if (tvMonthly != null) tvMonthly.setText(formatTime(weeklyAvg));
        if (tvTotal != null) tvTotal.setText(formatTime(totalAppUsage));

        renderBarChart(detailChart, entries, xLabels, false,true);
    }
    private void setupAppDetailMonthlyChart(BarChart detailChart, String appName,
                                            List<DailyStats> stats,
                                            TextView tvWeekly, TextView tvMonthly, TextView tvTotal,
                                            TextView tvLabelWeekly, TextView tvLabelMonthly, TextView tvLabelTotal) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();
        long totalAppUsage = 0;

        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();

        // Indexing for speed
        Map<String, DailyStats> statsMap = new HashMap<>();
        for (DailyStats s : stats) statsMap.put(s.date, s);

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        int daysWithData = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateString = sdf.format(cal.getTime());

            long usage = 0;
            DailyStats stat = statsMap.get(dateString); // Optimized lookup
            if (stat != null) {
                usage = getAppUsageForDay(stat, appName, gson, listType);
            }

            totalAppUsage += usage;
            if (usage > 0) daysWithData++;

            entries.add(new BarEntry(day - 1, usage / 3600000f));
            xLabels.add(String.valueOf(day));
        }

        // [Rest of your label and render logic remains exactly the same]
        if (tvLabelWeekly != null) tvLabelWeekly.setText("Days Used");
        if (tvLabelMonthly != null) tvLabelMonthly.setText("Monthly Avg");
        if (tvLabelTotal != null) tvLabelTotal.setText("Monthly Total");

        long monthlyAvg = daysWithData > 0 ? totalAppUsage / daysWithData : 0;
        if (tvWeekly != null) tvWeekly.setText(daysWithData + "d");
        if (tvMonthly != null) tvMonthly.setText(formatTime(monthlyAvg));
        if (tvTotal != null) tvTotal.setText(formatTime(totalAppUsage));

        renderBarChart(detailChart, entries, xLabels, true,false);
    }

    private String formatTime(long millis) {
        long hours = millis / (1000 * 60 * 60); long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }
}