package com.devendrap7.phonemoodtranslator.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.activities.MainActivity;
import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HeatmapFragment extends Fragment {

    // Views
    private LinearLayout heatmapGrid;
    private LinearLayout monthLabelsRow;
    private LinearLayout dayLabelsColumn;
    private HorizontalScrollView heatmapScrollView;
    private TextView tvTotalDays, tvPeakDay, tvAvgDaily;
    private TextView tvCurrentMonth, tvPrevMonth, tvNextMonth;
    private CardView expandedWeekCard;
    private TextView tvWeekTitle, tvWeekTotal, tvWeekTopApp;
    private LinearLayout weekBarChart, weekBarLabels;

    // Colors
    private static final int COLOR_EMPTY  = Color.parseColor("#E8E8E8");
    private static final int COLOR_LEVEL1 = Color.parseColor("#FFE0B2");
    private static final int COLOR_LEVEL2 = Color.parseColor("#FFB74D");
    private static final int COLOR_LEVEL3 = Color.parseColor("#FF8C00");
    private static final int COLOR_LEVEL4 = Color.parseColor("#E65100");

    private static final String[] DAY_LABELS  = {"M","T","W","T","F","S","S"};
    private static final String[] MONTH_NAMES = {
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
    };

    private int cellSize;
    private int cellMargin;

    // Data
    private Map<String, DailyStats> statsMap = new HashMap<>();
    private List<List<String>> allWeeks      = new ArrayList<>();

    // State
    private int currentMonthIndex = 0; // index into monthList
    private List<String> monthList = new ArrayList<>(); // "Feb 2026" etc
    private int expandedWeekIndex = -1; // which week is expanded

    private GestureDetector gestureDetector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heatmap, container, false);

        // Find views
        heatmapGrid       = view.findViewById(R.id.heatmapGrid);
        monthLabelsRow    = view.findViewById(R.id.monthLabelsRow);
        dayLabelsColumn   = view.findViewById(R.id.dayLabelsColumn);
        heatmapScrollView = view.findViewById(R.id.heatmapScrollView);
        tvTotalDays       = view.findViewById(R.id.tvTotalDays);
        tvPeakDay         = view.findViewById(R.id.tvPeakDay);
        tvAvgDaily        = view.findViewById(R.id.tvAvgDaily);
        tvCurrentMonth    = view.findViewById(R.id.tvCurrentMonth);
        tvPrevMonth       = view.findViewById(R.id.tvPrevMonth);
        tvNextMonth       = view.findViewById(R.id.tvNextMonth);
        expandedWeekCard  = view.findViewById(R.id.expandedWeekCard);
        tvWeekTitle       = view.findViewById(R.id.tvWeekTitle);
        tvWeekTotal       = view.findViewById(R.id.tvWeekTotal);
        tvWeekTopApp      = view.findViewById(R.id.tvWeekTopApp);
        weekBarChart      = view.findViewById(R.id.weekBarChart);
        weekBarLabels     = view.findViewById(R.id.weekBarLabels);

        // Cell dimensions
        float density = getResources().getDisplayMetrics().density;
        cellSize   = (int) (14 * density);
        cellMargin = (int) (2  * density);

        // Swipe gesture for month navigation
        gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float vX, float vY) {
                        if (e1 == null || e2 == null) return false;
                        float diffX = e2.getX() - e1.getX();
                        if (Math.abs(diffX) > 100 && Math.abs(vX) > 100) {
                            if (diffX < 0) navigateMonth(1);  // swipe left = next
                            else           navigateMonth(-1); // swipe right = prev
                            return true;
                        }
                        return false;
                    }
                });

        // Attach swipe to heatmap scroll view
        heatmapScrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // still allow scrolling
        });

        // Month nav buttons
        tvPrevMonth.setOnClickListener(v -> navigateMonth(-1));
        tvNextMonth.setOnClickListener(v -> navigateMonth(1));

        // Build day labels
        buildDayLabels();

        // Load data
        loadAndBuildHeatmap();

        return view;
    }

    // ─────────────────────────────────────────
    // MONTH NAVIGATION
    // ─────────────────────────────────────────
    private void navigateMonth(int direction) {
        int newIndex = currentMonthIndex + direction;
        if (newIndex < 0 || newIndex >= monthList.size()) return;
        currentMonthIndex = newIndex;
        expandedWeekIndex = -1;
        expandedWeekCard.setVisibility(View.GONE);
        renderCurrentMonth();
    }

    private void renderCurrentMonth() {
        if (monthList.isEmpty()) return;
        String monthKey = monthList.get(currentMonthIndex);
        tvCurrentMonth.setText(monthKey);

        // Filter weeks for this month
        List<List<String>> monthWeeks = getWeeksForMonth(monthKey);
        buildGrid(monthWeeks);
        buildMonthLabels(monthWeeks);

        // Disable arrows at boundaries
        tvPrevMonth.setAlpha(currentMonthIndex == 0 ? 0.3f : 1f);
        tvNextMonth.setAlpha(currentMonthIndex == monthList.size() - 1 ? 0.3f : 1f);
    }

    private List<List<String>> getWeeksForMonth(String monthKey) {
        List<List<String>> result = new ArrayList<>();
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        SimpleDateFormat dateFmt  = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (List<String> week : allWeeks) {
            for (String dateStr : week) {
                if (dateStr.isEmpty()) continue;
                try {
                    Calendar c = Calendar.getInstance();
                    c.setTime(dateFmt.parse(dateStr));
                    String m = monthFmt.format(c.getTime());
                    if (m.equals(monthKey)) {
                        result.add(week);
                        break;
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        return result;
    }

    // ─────────────────────────────────────────
    // DAY LABELS  M T W T F S S
    // ─────────────────────────────────────────
    private void buildDayLabels() {
        dayLabelsColumn.removeAllViews();
        for (String label : DAY_LABELS) {
            TextView tv = new TextView(getContext());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, cellSize);
            p.setMargins(0, cellMargin, 0, cellMargin);
            tv.setLayoutParams(p);
            tv.setText(label);
            tv.setTextSize(9f);
            tv.setTextColor(Color.parseColor("#888888"));
            tv.setGravity(Gravity.CENTER);
            dayLabelsColumn.addView(tv);
        }
    }

    // ─────────────────────────────────────────
    // LOAD ALL DATA
    // ─────────────────────────────────────────
    private void loadAndBuildHeatmap() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            List<DailyStats> allStats = db.statsDao().getYearlyStats();

            // Build stats map
            statsMap.clear();
            for (DailyStats s : allStats) statsMap.put(s.date, s);

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            // Find start (earliest Monday)
            Calendar start = Calendar.getInstance(
                    TimeZone.getTimeZone("Asia/Kolkata"));
            if (!allStats.isEmpty()) {
                try {
                    start.setTime(sdf.parse(allStats.get(0).date));
                } catch (Exception e) { e.printStackTrace(); }
            }
            while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
                start.add(Calendar.DAY_OF_YEAR, -1);

            // Find end (Sunday of current week)
            Calendar end = Calendar.getInstance(
                    TimeZone.getTimeZone("Asia/Kolkata"));
            while (end.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
                end.add(Calendar.DAY_OF_YEAR, 1);

            // Build all weeks
            allWeeks.clear();
            List<String> currentWeek = new ArrayList<>();
            Calendar cursor = (Calendar) start.clone();

            while (!cursor.after(end)) {
                currentWeek.add(sdf.format(cursor.getTime()));
                if (cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    allWeeks.add(new ArrayList<>(currentWeek));
                    currentWeek.clear();
                }
                cursor.add(Calendar.DAY_OF_YEAR, 1);
            }
            if (!currentWeek.isEmpty()) allWeeks.add(currentWeek);

            // Build month list
            SimpleDateFormat monthFmt = new SimpleDateFormat(
                    "MMMM yyyy", Locale.getDefault());
            SimpleDateFormat dateFmt = new SimpleDateFormat(
                    "dd MMM yyyy", Locale.getDefault());

            monthList.clear();
            String lastMonth = "";
            for (List<String> week : allWeeks) {
                for (String dateStr : week) {
                    if (dateStr.isEmpty()) continue;
                    try {
                        Calendar c = Calendar.getInstance();
                        c.setTime(dateFmt.parse(dateStr));
                        String m = monthFmt.format(c.getTime());
                        if (!m.equals(lastMonth)) {
                            monthList.add(m);
                            lastMonth = m;
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    break;
                }
            }

            // Start on current month
            String currentMonthStr = monthFmt.format(Calendar.getInstance().getTime());
            currentMonthIndex = monthList.indexOf(currentMonthStr);
            if (currentMonthIndex < 0)
                currentMonthIndex = monthList.size() - 1;

            // Summary stats
            long totalUsage = 0;
            long peakUsage  = 0;
            String peakDate = "";
            int trackedDays = 0;

            for (DailyStats s : allStats) {
                totalUsage += s.totalUsageTime;
                trackedDays++;
                if (s.totalUsageTime > peakUsage) {
                    peakUsage = s.totalUsageTime;
                    peakDate  = s.date;
                }
            }

            final long avgDaily        = trackedDays > 0 ? totalUsage / trackedDays : 0;
            final String finalPeakDate = peakDate;
            final long finalPeakUsage  = peakUsage;
            final int finalTracked     = trackedDays;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    renderCurrentMonth();
                    updateStatsCard(finalTracked, finalPeakDate,
                            finalPeakUsage, avgDaily);

                    // Auto scroll to end
                    heatmapScrollView.post(() ->
                            heatmapScrollView.fullScroll(View.FOCUS_RIGHT));
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────
    // BUILD GRID
    // ─────────────────────────────────────────
    private void buildGrid(List<List<String>> weeks) {
        heatmapGrid.removeAllViews();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            List<String> week = weeks.get(weekIndex);
            final int finalWeekIndex = weekIndex;

            LinearLayout col = new LinearLayout(getContext());
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            colParams.setMargins(cellMargin, 0, cellMargin, 0);
            col.setLayoutParams(colParams);

            // Pad to 7 rows
            List<String> paddedWeek = new ArrayList<>(week);
            while (paddedWeek.size() < 7) paddedWeek.add("");

            for (String dateStr : paddedWeek) {
                DailyStats stats = dateStr.isEmpty()
                        ? null : statsMap.get(dateStr);
                long minutes = stats != null
                        ? stats.totalUsageTime / 60000 : 0;
                boolean hasData = stats != null;

                View cell = new View(getContext());
                LinearLayout.LayoutParams cellParams =
                        new LinearLayout.LayoutParams(cellSize, cellSize);
                cellParams.setMargins(
                        cellMargin, cellMargin, cellMargin, cellMargin);
                cell.setLayoutParams(cellParams);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(3f);
                bg.setColor(hasData ? getHeatColor(minutes) : COLOR_EMPTY);
                cell.setBackground(bg);

                // Tap single cell → show day detail
                if (hasData) {
                    final DailyStats finalStats = stats;
                    final String finalDate = dateStr;
                    cell.setOnLongClickListener(v -> {






                        showDayDetailPopup(finalDate, finalStats);
                        return true;
                    });
                }

                col.addView(cell);
            }

            // Tap whole column (week) → expand below
            col.setOnClickListener(v -> toggleWeekExpand(finalWeekIndex, week));

            heatmapGrid.addView(col);
        }
    }

    // ─────────────────────────────────────────
    // WEEK EXPAND / COLLAPSE
    // ─────────────────────────────────────────
    private void toggleWeekExpand(int weekIndex, List<String> week) {
        TransitionManager.beginDelayedTransition(
                (ViewGroup) expandedWeekCard.getParent(),
                new AutoTransition());

        if (expandedWeekIndex == weekIndex) {
            // Collapse
            expandedWeekCard.setVisibility(View.GONE);
            expandedWeekIndex = -1;
        } else {
            // Expand
            expandedWeekIndex = weekIndex;
            expandedWeekCard.setVisibility(View.VISIBLE);
            populateExpandedWeek(week);
        }
    }

    private void populateExpandedWeek(List<String> week) {
        SimpleDateFormat inFmt  = new SimpleDateFormat(
                "dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat outFmt = new SimpleDateFormat(
                "d MMM", Locale.getDefault());

        // Week title
        String first = "", last = "";
        List<String> validDays = new ArrayList<>();
        for (String d : week) if (!d.isEmpty()) validDays.add(d);

        try {
            if (!validDays.isEmpty()) {
                first = outFmt.format(inFmt.parse(validDays.get(0)));
                last  = outFmt.format(inFmt.parse(
                        validDays.get(validDays.size() - 1)));
            }
        } catch (Exception e) { e.printStackTrace(); }
        tvWeekTitle.setText("Week of " + first + " – " + last);

        // Calculate week stats
        long weekTotal = 0;
        Map<String, Long> appTotals = new HashMap<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();

        long[] dayMins = new long[7];

        for (int i = 0; i < week.size(); i++) {
            String dateStr = week.get(i);
            if (dateStr.isEmpty()) continue;
            DailyStats stats = statsMap.get(dateStr);
            if (stats == null) continue;

            long dayMs = stats.totalUsageTime;
            weekTotal += dayMs;
            dayMins[i] = dayMs / 60000;

            // Top app aggregation
            if (stats.topAppsJson != null) {
                try {
                    List<MainActivity.AppUsageInfo> apps = gson.fromJson(stats.topAppsJson, listType);

                    for (MainActivity.AppUsageInfo app : apps){
                        appTotals.put(app.name,
                                appTotals.getOrDefault(app.name, 0L)
                                        + app.usageTime);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        // Total
        long totalMins = weekTotal / 60000;
        long totalH    = totalMins / 60;
        long totalM    = totalMins % 60;
        tvWeekTotal.setText(totalH + "h " + totalM + "m");

        // Top app
        String topApp = "None";
        long topTime  = 0;
        for (Map.Entry<String, Long> e : appTotals.entrySet()) {
            if (e.getValue() > topTime) {
                topTime = e.getValue();
                topApp  = e.getKey();
            }
        }
        long topMins = topTime / 60000;
        long topH    = topMins / 60;
        long topM    = topMins % 60;
        tvWeekTopApp.setText(topApp + "\n"
                + (topH > 0 ? topH + "h " : "") + topM + "m");

        // Build mini bar chart
        buildMiniBarChart(dayMins, week);
    }

    // ─────────────────────────────────────────
    // MINI BAR CHART
    // ─────────────────────────────────────────
    private void buildMiniBarChart(long[] dayMins, List<String> week) {
        weekBarChart.removeAllViews();
        weekBarLabels.removeAllViews();

        long maxMins = 1;
        for (long m : dayMins) if (m > maxMins) maxMins = m;

        float density = getResources().getDisplayMetrics().density;
        int chartHeight = (int) (100 * density);

        for (int i = 0; i < 7; i++) {
            // Bar column
            LinearLayout barCol = new LinearLayout(getContext());
            barCol.setOrientation(LinearLayout.VERTICAL);
            barCol.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colP = new LinearLayout.LayoutParams(
                    0, chartHeight, 1f);
            colP.setMargins(4, 0, 4, 0);
            barCol.setLayoutParams(colP);

            // Bar itself
            float ratio    = (float) dayMins[i] / maxMins;
            int barHeight  = (int) (chartHeight * ratio);
            if (barHeight < 4 && dayMins[i] > 0) barHeight = 4;

            View bar = new View(getContext());
            LinearLayout.LayoutParams barP = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barHeight);
            bar.setLayoutParams(barP);

            GradientDrawable barBg = new GradientDrawable();
            barBg.setCornerRadius(6f);
            barBg.setColor(getHeatColor(dayMins[i]));
            bar.setBackground(barBg);

            // Minutes label on top of bar
            if (dayMins[i] > 0) {
                TextView minLabel = new TextView(getContext());
                minLabel.setText(dayMins[i] >= 60
                        ? (dayMins[i] / 60) + "h"
                        : dayMins[i] + "m");
                minLabel.setTextSize(8f);
                minLabel.setTextColor(Color.parseColor("#555555"));
                minLabel.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                minLabel.setLayoutParams(lp);
                barCol.addView(minLabel);
            }

            barCol.addView(bar);
            weekBarChart.addView(barCol);

            // Day label below
            TextView dayLabel = new TextView(getContext());
            dayLabel.setText(DAY_LABELS[i]);
            dayLabel.setTextSize(10f);
            dayLabel.setTextColor(Color.parseColor("#888888"));
            dayLabel.setGravity(Gravity.CENTER);
            dayLabel.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams dlP =
                    new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            dlP.setMargins(4, 4, 4, 0);
            dayLabel.setLayoutParams(dlP);
            weekBarLabels.addView(dayLabel);
        }
    }

    // ─────────────────────────────────────────
    // MONTH LABELS
    // ─────────────────────────────────────────
    private void buildMonthLabels(List<List<String>> weeks) {
        monthLabelsRow.removeAllViews();
        SimpleDateFormat dateFmt = new SimpleDateFormat(
                "dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat mFmt = new SimpleDateFormat(
                "MMM", Locale.getDefault());

        int lastMonth = -1;
        for (List<String> week : weeks) {
            TextView label = new TextView(getContext());
            int colWidth = cellSize + cellMargin * 4;
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    colWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            label.setLayoutParams(p);
            label.setTextSize(9f);
            label.setTextColor(Color.parseColor("#888888"));

            if (!week.isEmpty() && !week.get(0).isEmpty()) {
                try {
                    Calendar c = Calendar.getInstance();
                    c.setTime(dateFmt.parse(week.get(0)));
                    int month = c.get(Calendar.MONTH);
                    if (month != lastMonth) {
                        label.setText(mFmt.format(c.getTime()));
                        lastMonth = month;
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            monthLabelsRow.addView(label);
        }
    }

    // ─────────────────────────────────────────
    // DAY DETAIL POPUP
    // ─────────────────────────────────────────
    private void showDayDetailPopup(String dateStr, DailyStats stats) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_day_detail);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDayDetailTitle);
        TextView tvTotal = dialog.findViewById(R.id.tvDayDetailTotal);
        GridLayout hourlyGrid = dialog.findViewById(R.id.hourlyGrid);

        // Format date
        try {
            SimpleDateFormat inFmt  = new SimpleDateFormat(
                    "dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat outFmt = new SimpleDateFormat(
                    "EEEE, dd MMM", Locale.getDefault());
            tvTitle.setText(outFmt.format(inFmt.parse(dateStr)));
        } catch (Exception e) { tvTitle.setText(dateStr); }

        // Total
        long totalMins = stats.totalUsageTime / 60000;
        long hours = totalMins / 60;
        long mins  = totalMins % 60;
        tvTotal.setText("Total: " + (hours > 0 ? hours + "h " : "") + mins + "m");

        // 12 blocks of 2hrs
        hourlyGrid.removeAllViews();
        hourlyGrid.setColumnCount(6);
        hourlyGrid.setRowCount(2);

        float density = getResources().getDisplayMetrics().density;
        int blockSize = (int) (48 * density);
        long[] hourlyMins = estimateHourlyDistribution(stats);

        for (int block = 0; block < 12; block++) {
            int hourStart  = block * 2;
            long blockMins = hourlyMins[hourStart] + hourlyMins[hourStart + 1];

            LinearLayout blockView = new LinearLayout(getContext());
            blockView.setOrientation(LinearLayout.VERTICAL);
            blockView.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams gp = new GridLayout.LayoutParams(
                    GridLayout.spec(block / 6),
                    GridLayout.spec(block % 6));
            gp.width  = blockSize;
            gp.height = blockSize;
            gp.setMargins(6, 6, 6, 6);
            blockView.setLayoutParams(gp);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(8f);
            bg.setColor(getHeatColor(blockMins));
            blockView.setBackground(bg);

            // Time
            int display = hourStart == 0 ? 12
                    : hourStart > 12 ? hourStart - 12 : hourStart;
            String amPm = hourStart < 12 ? "a" : "p";

            TextView timeTv = new TextView(getContext());
            timeTv.setText(display + amPm);
            timeTv.setTextSize(10f);
            timeTv.setTextColor(blockMins > 30
                    ? Color.WHITE : Color.parseColor("#888888"));
            timeTv.setGravity(Gravity.CENTER);
            blockView.addView(timeTv);

            // Mins
            if (blockMins > 0) {
                TextView minsTv = new TextView(getContext());
                minsTv.setText(blockMins + "m");
                minsTv.setTextSize(9f);
                minsTv.setTextColor(blockMins > 30
                        ? Color.WHITE : Color.parseColor("#AAAAAA"));
                minsTv.setGravity(Gravity.CENTER);
                blockView.addView(minsTv);
            }

            hourlyGrid.addView(blockView);
        }

        dialog.findViewById(R.id.btnCloseDayDetail)
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ─────────────────────────────────────────
    // HOURLY ESTIMATE
    // ─────────────────────────────────────────
    private long[] estimateHourlyDistribution(DailyStats stats) {
        long[] hourlyMins = new long[24];
        if (stats.totalUsageTime <= 0) return hourlyMins;
        long totalMins = stats.totalUsageTime / 60000;

        int[]   activeHours = {8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
        float[] weights     = {1,1,1.5f,1.5f,2,2,2,2,2.5f,2.5f,3,3,3,2.5f,2,1.5f};

        float totalWeight = 0;
        for (float w : weights) totalWeight += w;

        for (int i = 0; i < activeHours.length; i++) {
            hourlyMins[activeHours[i]] =
                    Math.round(totalMins * (weights[i] / totalWeight));
        }
        return hourlyMins;
    }

    // ─────────────────────────────────────────
    // STATS CARD
    // ─────────────────────────────────────────
    private void updateStatsCard(int trackedDays, String peakDate,
                                 long peakUsage, long avgDaily) {
        tvTotalDays.setText(trackedDays + " days");

        if (!peakDate.isEmpty()) {
            long peakMins  = peakUsage / 60000;
            long peakH     = peakMins / 60;
            long peakM     = peakMins % 60;
            tvPeakDay.setText(peakDate + "  —  " + peakH + "h " + peakM + "m");
        } else {
            tvPeakDay.setText("No data yet");
        }

        long avgMins = avgDaily / 60000;
        long avgH    = avgMins / 60;
        long avgM    = avgMins % 60;
        tvAvgDaily.setText(avgH + "h " + avgM + "m");
    }

    // ─────────────────────────────────────────
    // COLOR SCALE
    // ─────────────────────────────────────────
    private int getHeatColor(long minutes) {
        if (minutes <= 0)   return COLOR_EMPTY;
        if (minutes <= 30)  return COLOR_LEVEL1;
        if (minutes <= 90)  return COLOR_LEVEL2;
        if (minutes <= 180) return COLOR_LEVEL3;
        return COLOR_LEVEL4;
    }
}