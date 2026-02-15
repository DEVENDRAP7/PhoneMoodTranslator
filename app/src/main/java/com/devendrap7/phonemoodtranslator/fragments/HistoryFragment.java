package com.devendrap7.phonemoodtranslator.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.devendrap7.phonemoodtranslator.adapters.HistoryAdapter;
import com.devendrap7.phonemoodtranslator.adapters.CalendarAdapter;
import com.devendrap7.phonemoodtranslator.activities.MainActivity;
import com.devendrap7.phonemoodtranslator.R;

import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;
import com.devendrap7.phonemoodtranslator.database.MoodHistoryItem;
import com.devendrap7.phonemoodtranslator.views.MoodPetView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<MoodHistoryItem> historyList;

    private RecyclerView rvCalendar;
    private CardView cvCalendarWrapper;
    private TextView tvCurrentMonth,tvTotalUsage;
    private Calendar currentCalendarInstance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // 1. RecyclerView Setup
        recyclerView = view.findViewById(R.id.rvHistoryList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList, item -> showDetailDialog(item));
        recyclerView.setAdapter(adapter);

        // 2. Calendar Setup
        setupCalendarUI(view);

        // 3. Initial Load
        loadHistoryFromDatabase();



        return view;
    }

    private void setupCalendarUI(View view) {
        cvCalendarWrapper = view.findViewById(R.id.cvCalendarWrapper);
        rvCalendar = view.findViewById(R.id.rvCalendar);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        ImageButton btnToggle = view.findViewById(R.id.btnToggleCalendar);
        ImageButton btnPrev = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = view.findViewById(R.id.btnNextMonth);

        currentCalendarInstance = Calendar.getInstance();

        btnToggle.setOnClickListener(v -> {
            if (cvCalendarWrapper.getVisibility() == View.VISIBLE) {
                cvCalendarWrapper.setVisibility(View.GONE);
            } else {
                cvCalendarWrapper.setVisibility(View.VISIBLE);
                updateCalendarGrid();
            }
        });

        btnPrev.setOnClickListener(v -> {
            currentCalendarInstance.add(Calendar.MONTH, -1);
            updateCalendarGrid();
        });

        btnNext.setOnClickListener(v -> {
            currentCalendarInstance.add(Calendar.MONTH, 1);
            updateCalendarGrid();
        });

        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
    }

    private void updateCalendarGrid() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCurrentMonth.setText(sdf.format(currentCalendarInstance.getTime()));

        final int month = currentCalendarInstance.get(Calendar.MONTH) + 1;
        final int year = currentCalendarInstance.get(Calendar.YEAR);

        final Calendar tempCal = (Calendar) currentCalendarInstance.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);

        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        final int firstDayOfWeek = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - 2;
        final int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(getContext());
            List<DailyStats> monthlyStats = db.statsDao().getStatsForMonth(month, year);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (getContext() != null) {
                    CalendarAdapter calAdapter = new CalendarAdapter(monthlyStats, daysInMonth, firstDayOfWeek);
                    rvCalendar.setAdapter(calAdapter);
                }
            });
        }).start();
    }

    private void loadHistoryFromDatabase() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(getContext());
            List<DailyStats> dbData = db.statsDao().getYearlyStats();
            List<MoodHistoryItem> uiItems = new ArrayList<>();

            for (DailyStats stat : dbData) {
                String usageStr = formatTime(stat.totalUsageTime);
                uiItems.add(new MoodHistoryItem(
                        stat.date, stat.moodEmoji, stat.moodTitle, usageStr,
                        stat.totalUsageTime, stat.topAppsJson, stat.selfNote
                ));
            }
            Collections.reverse(uiItems);

            new Handler(Looper.getMainLooper()).post(() -> {
                historyList.clear();
                historyList.addAll(uiItems);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private String formatTime(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
        return (hours > 0) ? hours + "h " + minutes + "m" : minutes + "m";
    }

    private void showDetailDialog(MoodHistoryItem item) {
        if (item.topAppsJson == null || item.topAppsJson.isEmpty()) return;

        // 1. Parse the JSON data
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MainActivity.AppUsageInfo>>(){}.getType();
        List<MainActivity.AppUsageInfo> apps = gson.fromJson(item.topAppsJson, listType);

        // 2. Setup the Dialog
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(R.layout.dialog_history_details); // This is your "Second XML"

        TextView tvHeader = dialog.findViewById(R.id.tvDetailHeader);
         // The ID we added in Step 1
        TextView tvTotalUsage = dialog.findViewById(R.id.tvTotalUsageTime);
        PieChart pieChart = dialog.findViewById(R.id.detailPieChart); // The ID we added in Step 1

        if (tvHeader != null) tvHeader.setText("Usage on " + item.date);
        if (tvTotalUsage != null) tvTotalUsage.setText("Total Time: " + item.usageStr);

        LinearLayout statsContainer = dialog.findViewById(R.id.statsContainer);
        // 3. Populate the Pie Chart
        if (pieChart != null && apps != null) {
            setupPieChart(pieChart, apps,statsContainer,item,dialog);
        }
        com.google.android.material.bottomsheet.BottomSheetBehavior behavior = dialog.getBehavior();

// 2. Calculate 60% of the screen height
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int peekHeight = (int) (screenHeight * 0.60);

// 3. Set the Peek Height and initial state
        behavior.setPeekHeight(peekHeight);
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);

// Optional: If you want it to hide completely when swiped down
        behavior.setHideable(true);

        dialog.show();
        // Force the BottomSheet to expand fully so it doesn't hide half the chart
        //dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
    }
    private void setupPieChart(PieChart pieChart, List<MainActivity.AppUsageInfo> apps, LinearLayout statsContainer ,MoodHistoryItem item, BottomSheetDialog dialog) {
        List<PieEntry> entries = new ArrayList<>();

        // 1. Sort apps and take top 5
        Collections.sort(apps, (a, b) -> Long.compare(b.usageTime, a.usageTime));

        for (int i = 0; i < Math.min(apps.size(), 5); i++) {
            MainActivity.AppUsageInfo app = apps.get(i);
            float minutes = app.usageTime / 60000f;
            if (minutes > 0) {
                // We pass the name here; the 'minutes' will be formatted by the ValueFormatter below
                entries.add(new PieEntry(minutes, app.name));
            }
        }
        if (statsContainer != null) {
            statsContainer.removeAllViews();
            for (MainActivity.AppUsageInfo app : apps) {
                // Create a horizontal layout for each row
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 20, 0, 20);

                // App Name (Left side)
                TextView tvName = new TextView(getContext());
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                tvName.setText(app.name);
                tvName.setTextSize(14f);
                tvName.setTextColor(Color.parseColor("#000000")); // Dark Grey
                tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                // Usage Time (Right side)
                TextView tvTime = new TextView(getContext());
                long mins = app.usageTime / 60000;
                String timeStr = (mins >= 60) ? (mins / 60) + "h " + (mins % 60) + "m" : mins + "m";
                tvTime.setText(timeStr);
                tvTime.setTextColor(Color.parseColor("#6366F1")); // Modern Indigo
                tvTime.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                row.addView(tvName);
                row.addView(tvTime);

                // Add a subtle divider line
                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(-1, 2));
                divider.setBackgroundColor(Color.parseColor("#F3F4F6"));

                statsContainer.addView(row);
                statsContainer.addView(divider);
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Modern "Soft Material" Color Palette
        int[] modernColors = {
                Color.parseColor("#cdb4db"), // Indigo
                Color.parseColor("#ffc8dd"), // Violet
                Color.parseColor("#bde0fe"), // Pink
                Color.parseColor("#a2d2ff"), // Amber
                Color.parseColor("#ffafcc")  // Emerald
        };

        dataSet.setColors(modernColors);
         // Adds a small gap between slices for a cleaner look
        dataSet.setSelectionShift(10f); // Slices "pop out" more when clicked

        // 2. Custom Value Formatter to show "1h 19m" instead of "79.0"
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry entry) {
                int totalMinutes = (int) value;
                if (totalMinutes >= 60) {
                    return (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m";
                } else {
                    return totalMinutes + "m";
                }
            }
        });

        PieData data = new PieData(dataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        data.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // 3. Modern Styling (The "Donut" Look)
        pieChart.setData(data);
        pieChart.setUsePercentValues(false); // We want real time, not %
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);

        // Stylish Center Hole
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(53f); // Soft outer glow
        pieChart.setHoleRadius(50f); // Large hole for a modern "ring" look


        // 1. Find the Mascot View from the XML
        MoodPetView mascotView = dialog.findViewById(R.id.dialogMascot);

        if (mascotView != null && item != null) {
            // 2. Convert usageTime (ms) to minutes for the mascot
            int totalMins = (int) (item.usageMillis / 60000);
            mascotView.usageMinutes = totalMins;
            mascotView.isLateNight = false; // You can add logic here if it's past 11 PM

            // 4. Now apply the colors and refresh the view
            mascotView.applyMoodColors();
            mascotView.invalidate(); // Redraws the pet with new colors
        }

// 5. Minimalist PieChart settings
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(50f); // Make sure the hole fits your 100dp mascot
        pieChart.setDrawCenterText(false);
        pieChart.invalidate();


        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.parseColor("#333333"));
        pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelTypeface(Typeface.DEFAULT_BOLD);

        pieChart.animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mascotView, "scaleX", 1.20f, 1.05f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mascotView, "scaleY", 1.20f, 1.05f);

        scaleX.setDuration(1000);
        scaleY.setDuration(1000);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        AnimatorSet breath = new AnimatorSet();
        breath.playTogether(scaleX, scaleY);
        breath.start();
        pieChart.invalidate();
    }
}