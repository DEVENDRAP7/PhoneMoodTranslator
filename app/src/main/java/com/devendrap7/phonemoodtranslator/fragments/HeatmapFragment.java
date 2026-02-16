package com.devendrap7.phonemoodtranslator.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    // ── Views ──
    private LinearLayout samsungHeatmapGrid;
    private TextView tvCurrentMonth, tvCurrentWeek;
    private android.widget.ImageButton tvPrevMonth, tvNextMonth;
    private android.widget.ImageButton tvPrevWeek, tvNextWeek;
    private TextView tvWeekTotal, tvWeekTopApp;
    private TextView tvTotalDays, tvPeakDay, tvAvgDaily;

    // ── Colors ──
    private static final int COLOR_EMPTY  = Color.parseColor("#E8E8E8");
    private static final int COLOR_LEVEL1 = Color.parseColor("#FFE0B2");
    private static final int COLOR_LEVEL2 = Color.parseColor("#FFB74D");
    private static final int COLOR_LEVEL3 = Color.parseColor("#FF8C00");
    private static final int COLOR_LEVEL4 = Color.parseColor("#E65100");

    private static final String[] DAY_LABELS  = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
    private static final String[] MONTH_NAMES = {
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
    };

    // ── Data ──
    private Map<String, DailyStats> statsMap = new HashMap<>();
    private List<List<String>> allWeeks      = new ArrayList<>();
    private List<String> monthList           = new ArrayList<>();

    // ── State ──
    private int currentMonthIndex = 0;
    private int currentWeekIndex  = 0; // index within current month's weeks
    private List<List<String>> currentMonthWeeks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heatmap, container, false);

        // Find views
        samsungHeatmapGrid = view.findViewById(R.id.samsungHeatmapGrid);
        tvCurrentMonth     = view.findViewById(R.id.tvCurrentMonth);
        tvPrevMonth        = view.findViewById(R.id.tvPrevMonth);
        tvNextMonth        = view.findViewById(R.id.tvNextMonth);
        tvCurrentWeek      = view.findViewById(R.id.tvCurrentWeek);
        tvPrevWeek         = view.findViewById(R.id.tvPrevWeek);
        tvNextWeek         = view.findViewById(R.id.tvNextWeek);
        tvWeekTotal        = view.findViewById(R.id.tvWeekTotal);
        tvWeekTopApp       = view.findViewById(R.id.tvWeekTopApp);
        tvTotalDays        = view.findViewById(R.id.tvTotalDays);
        tvPeakDay          = view.findViewById(R.id.tvPeakDay);
        tvAvgDaily         = view.findViewById(R.id.tvAvgDaily);

        // Month nav
        tvPrevMonth.setOnClickListener(v -> navigateMonth(-1));
        tvNextMonth.setOnClickListener(v -> navigateMonth(1));

        // Week nav
        tvPrevWeek.setOnClickListener(v -> navigateWeek(-1));
        tvNextWeek.setOnClickListener(v -> navigateWeek(1));

        loadAllData();

        return view;
    }

    // ─────────────────────────────────────────
    // LOAD ALL DATA
    // ─────────────────────────────────────────
    private void loadAllData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            List<DailyStats> allStats = db.statsDao().getYearlyStats();

            // Build stats map
            statsMap.clear();
            for (DailyStats s : allStats) statsMap.put(s.date, s);

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getDefault());

            // Find Monday of the week containing earliest record
            Calendar start = Calendar.getInstance(
                    TimeZone.getDefault());
            if (!allStats.isEmpty()) {
                try { start.setTime(sdf.parse(allStats.get(0).date)); }
                catch (Exception e) { e.printStackTrace(); }
            }
// ✅ Roll back to Monday
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
                start.add(Calendar.DAY_OF_YEAR, -1);

// Find Monday of current week then go to Sunday
            Calendar end = Calendar.getInstance(
                    TimeZone.getDefault());
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
// ✅ Roll forward to Sunday
            while (end.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
                end.add(Calendar.DAY_OF_YEAR, 1);

// ✅ Build weeks Mon→Sun
            allWeeks.clear();
            List<String> currentWeek = new ArrayList<>();
            Calendar cursor = (Calendar) start.clone();
            while (!cursor.after(end)) {
                currentWeek.add(sdf.format(cursor.getTime()));
                // ✅ Sunday = end of week
                if (cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    allWeeks.add(new ArrayList<>(currentWeek));
                    currentWeek.clear();
                }
                cursor.add(Calendar.DAY_OF_YEAR, 1);
            }
            if (!currentWeek.isEmpty()) allWeeks.add(currentWeek);

            // Build month list
            SimpleDateFormat monthFmt = new SimpleDateFormat(
                    "MMMM yyyy", Locale.ENGLISH);
            SimpleDateFormat dateFmt  = new SimpleDateFormat(
                    "dd MMM yyyy", Locale.ENGLISH);
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

            // ✅ Start on current month
            String nowMonth = monthFmt.format(
                    Calendar.getInstance().getTime());
            currentMonthIndex = monthList.indexOf(nowMonth);
            if (currentMonthIndex < 0)
                currentMonthIndex = monthList.size() - 1;

// ✅ Find current week index within current month
            String todayStr = sdf.format(Calendar.getInstance(
                    TimeZone.getDefault()).getTime());
            currentMonthWeeks = getWeeksForMonth(nowMonth);
            currentWeekIndex = 0; // default to first week
            for (int i = 0; i < currentMonthWeeks.size(); i++) {
                List<String> week = currentMonthWeeks.get(i);
                if (week.contains(todayStr)) {
                    currentWeekIndex = i;
                    break;
                }
            }

            // All time stats
            long totalUsage = 0, peakUsage = 0;
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
            final long avgDaily        = trackedDays > 0
                    ? totalUsage / trackedDays : 0;
            final String finalPeakDate = peakDate;
            final long finalPeakUsage  = peakUsage;
            final int finalTracked     = trackedDays;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateStatsCard(finalTracked,
                            finalPeakDate, finalPeakUsage, avgDaily);
                    refreshMonthView();
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────
    // MONTH NAVIGATION
    // ─────────────────────────────────────────
    private void navigateMonth(int direction) {
        int newIndex = currentMonthIndex + direction;
        if (newIndex < 0 || newIndex >= monthList.size()) return;
        currentMonthIndex = newIndex;
        currentWeekIndex  = 0;
        refreshMonthView();
    }

    private void refreshMonthView() {
        if (monthList.isEmpty()) return;

        String monthKey = monthList.get(currentMonthIndex);
        tvCurrentMonth.setText(monthKey);

        // Get weeks for this month
        currentMonthWeeks = getWeeksForMonth(monthKey);

        // Clamp week index
        if (currentWeekIndex >= currentMonthWeeks.size())
            currentWeekIndex = currentMonthWeeks.size() - 1;
        if (currentWeekIndex < 0) currentWeekIndex = 0;

        // Arrow visibility
        tvPrevMonth.setAlpha(currentMonthIndex == 0 ? 0.3f : 1f);
        tvNextMonth.setAlpha(
                currentMonthIndex == monthList.size() - 1 ? 0.3f : 1f);

        refreshWeekView();
    }

    // ─────────────────────────────────────────
    // WEEK NAVIGATION
    // ─────────────────────────────────────────
    private void navigateWeek(int direction) {
        int newIndex = currentWeekIndex + direction;

        if (newIndex < 0) {
            // Go to previous month last week
            if (currentMonthIndex > 0) {
                currentMonthIndex--;
                currentMonthWeeks = getWeeksForMonth(
                        monthList.get(currentMonthIndex));
                currentWeekIndex = currentMonthWeeks.size() - 1;
                tvCurrentMonth.setText(monthList.get(currentMonthIndex));
                tvPrevMonth.setAlpha(currentMonthIndex == 0 ? 0.3f : 1f);
                tvNextMonth.setAlpha(
                        currentMonthIndex == monthList.size()-1 ? 0.3f : 1f);
            } else return;
        } else if (newIndex >= currentMonthWeeks.size()) {
            // Go to next month first week
            if (currentMonthIndex < monthList.size() - 1) {
                currentMonthIndex++;
                currentMonthWeeks = getWeeksForMonth(
                        monthList.get(currentMonthIndex));
                currentWeekIndex = 0;
                tvCurrentMonth.setText(monthList.get(currentMonthIndex));
                tvPrevMonth.setAlpha(currentMonthIndex == 0 ? 0.3f : 1f);
                tvNextMonth.setAlpha(
                        currentMonthIndex == monthList.size()-1 ? 0.3f : 1f);
            } else return;
        } else {
            currentWeekIndex = newIndex;
        }

        refreshWeekView();
    }

    private void refreshWeekView() {
        if (currentMonthWeeks.isEmpty()) return;

        List<String> week = currentMonthWeeks.get(currentWeekIndex);

        // ✅ Always show Mon – Sun of this week
        SimpleDateFormat inFmt  = new SimpleDateFormat(
                "dd MMM yyyy", Locale.ENGLISH);
        SimpleDateFormat outFmt = new SimpleDateFormat(
                "d MMM", Locale.ENGLISH);

        String first = "", last = "";
        try {
            // First day is always index 0 (Monday)
            if (!week.isEmpty() && !week.get(0).isEmpty())
                first = outFmt.format(inFmt.parse(week.get(0)));
            // Last day is always index 6 (Sunday)
            if (week.size() == 7 && !week.get(6).isEmpty())
                last = outFmt.format(inFmt.parse(week.get(6)));
            else if (!week.isEmpty())
                last = outFmt.format(inFmt.parse(
                        week.get(week.size()-1)));
        } catch (Exception e) { e.printStackTrace(); }

        tvCurrentWeek.setText(first + " – " + last);
        // Week arrows
        tvPrevWeek.setAlpha(
                (currentWeekIndex == 0 && currentMonthIndex == 0)
                        ? 0.3f : 1f);
        tvNextWeek.setAlpha(
                (currentWeekIndex == currentMonthWeeks.size()-1
                        && currentMonthIndex == monthList.size()-1)
                        ? 0.3f : 1f);

        // Build Samsung grid
        buildSamsungGrid(week);

        // Build week summary
        buildWeekSummary(week);
    }

    // ─────────────────────────────────────────
    // GET WEEKS FOR MONTH
    // ─────────────────────────────────────────
    private List<List<String>> getWeeksForMonth(String monthKey) {
        List<List<String>> result = new ArrayList<>();
        SimpleDateFormat monthFmt = new SimpleDateFormat(
                "MMMM yyyy", Locale.ENGLISH);
        SimpleDateFormat dateFmt  = new SimpleDateFormat(
                "dd MMM yyyy", Locale.ENGLISH);

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
    // SAMSUNG STYLE GRID
    // ─────────────────────────────────────────
    private void buildSamsungGrid(List<String> week) {
        samsungHeatmapGrid.removeAllViews();

        float density   = getResources().getDisplayMetrics().density;
        int blockHeight = (int) (18 * density);
        int blockMargin = (int) (1.5f * density);

        // Pad to 7 days
        List<String> paddedWeek = new ArrayList<>(week);
        while (paddedWeek.size() < 7) paddedWeek.add("");

        Gson gson = new Gson();

        for (int day = 0; day < 7; day++) {
            String dateStr = paddedWeek.get(day);
            DailyStats stats = dateStr.isEmpty()
                    ? null : statsMap.get(dateStr);

            // Get hourly data
            long[] hourlyMins = new long[24];
            if (stats != null) {
                if (stats.hourlyDataJson != null) {
                    try {
                        long[] parsed = gson.fromJson(
                                stats.hourlyDataJson, long[].class);
                        if (parsed != null && parsed.length == 24)
                            hourlyMins = parsed;
                        else hourlyMins = estimateHourlyDistribution(stats);
                    } catch (Exception e) {
                        hourlyMins = estimateHourlyDistribution(stats);
                    }
                } else {
                    hourlyMins = estimateHourlyDistribution(stats);
                }
            }

            // Build row
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, blockMargin, 0, blockMargin);
            row.setLayoutParams(rowParams);

            // Day label
            TextView dayLabel = new TextView(getContext());
            LinearLayout.LayoutParams labelParams =
                    new LinearLayout.LayoutParams(
                            (int) (32 * density), blockHeight);
            dayLabel.setLayoutParams(labelParams);
            dayLabel.setText(DAY_LABELS[day]);
            dayLabel.setTextSize(9f);
            dayLabel.setTextColor(Color.parseColor("#555555"));
            dayLabel.setTypeface(Typeface.DEFAULT_BOLD);
            dayLabel.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(dayLabel);

            // 24 hour blocks
            for (int hour = 0; hour < 24; hour++) {
                View block = new View(getContext());
                LinearLayout.LayoutParams blockParams =
                        new LinearLayout.LayoutParams(0, blockHeight, 1f);
                blockParams.setMargins(blockMargin, 0, blockMargin, 0);
                block.setLayoutParams(blockParams);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(3f);
                bg.setColor(stats != null
                        ? getHeatColor(hourlyMins[hour])
                        : COLOR_EMPTY);
                block.setBackground(bg);
                row.addView(block);
            }

            samsungHeatmapGrid.addView(row);
        }
    }

    // ─────────────────────────────────────────
    // WEEK SUMMARY
    // ─────────────────────────────────────────
    private void buildWeekSummary(List<String> week) {
        long weekTotal = 0;
        Map<String, Long> appTotals = new HashMap<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
        long[] dayMins = new long[7];

        List<String> paddedWeek = new ArrayList<>(week);
        while (paddedWeek.size() < 7) paddedWeek.add("");

        for (int i = 0; i < 7; i++) {
            String dateStr = paddedWeek.get(i);
            if (dateStr.isEmpty()) continue;
            DailyStats stats = statsMap.get(dateStr);
            if (stats == null) continue;

            weekTotal  += stats.totalUsageTime;
            dayMins[i]  = stats.totalUsageTime / 60000;

            if (stats.topAppsJson != null) {
                try {
                    List<MainActivity.AppUsageInfo> apps =
                            gson.fromJson(stats.topAppsJson, listType);
                    for (MainActivity.AppUsageInfo app : apps) {
                        appTotals.put(app.name,
                                appTotals.getOrDefault(app.name, 0L)
                                        + app.usageTime);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        // Total
        long totalMins = weekTotal / 60000;
        tvWeekTotal.setText((totalMins/60) + "h " + (totalMins%60) + "m");

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
        tvWeekTopApp.setText(topApp + "\n"
                + (topMins/60 > 0 ? topMins/60 + "h " : "")
                + topMins%60 + "m");
    }

    // ─────────────────────────────────────────
    // STATS CARD
    // ─────────────────────────────────────────
    private void updateStatsCard(int trackedDays, String peakDate,
                                 long peakUsage, long avgDaily) {
        tvTotalDays.setText(trackedDays + " days");

        if (!peakDate.isEmpty()) {
            long peakMins = peakUsage / 60000;
            tvPeakDay.setText(peakDate + "  —  "
                    + peakMins/60 + "h " + peakMins%60 + "m");
        } else {
            tvPeakDay.setText("No data yet");
        }

        long avgMins = avgDaily / 60000;
        tvAvgDaily.setText(avgMins/60 + "h " + avgMins%60 + "m");
    }

    // ─────────────────────────────────────────
    // HOURLY ESTIMATE FALLBACK
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
    // COLOR SCALE
    // ─────────────────────────────────────────
    private int getHeatColor(long minutes) {
        if (minutes <= 0)   return COLOR_EMPTY;
        if (minutes <= 15)  return COLOR_LEVEL1;
        if (minutes <= 30)  return COLOR_LEVEL2;
        if (minutes <= 45)  return COLOR_LEVEL3;
        return COLOR_LEVEL4;
    }
    @Override
    public void onResume() {
        super.onResume();
        // ✅ Always snap back to current week when fragment is opened
        if (!monthList.isEmpty()) {
            SimpleDateFormat monthFmt = new SimpleDateFormat(
                    "MMMM yyyy", Locale.ENGLISH);
            SimpleDateFormat dateFmt  = new SimpleDateFormat(
                    "dd MMM yyyy", Locale.ENGLISH);
            dateFmt.setTimeZone(TimeZone.getDefault());

            String nowMonth = monthFmt.format(
                    Calendar.getInstance().getTime());
            String todayStr = dateFmt.format(
                    Calendar.getInstance().getTime());

            int monthIdx = monthList.indexOf(nowMonth);
            if (monthIdx >= 0) {
                currentMonthIndex = monthIdx;
                currentMonthWeeks = getWeeksForMonth(nowMonth);
                currentWeekIndex  = 0;
                for (int i = 0; i < currentMonthWeeks.size(); i++) {
                    if (currentMonthWeeks.get(i).contains(todayStr)) {
                        currentWeekIndex = i;
                        break;
                    }
                }
                if (getView() != null) refreshMonthView();
            }
        }
    }
}