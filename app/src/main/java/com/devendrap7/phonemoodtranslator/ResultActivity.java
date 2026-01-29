package com.devendrap7.phonemoodtranslator;

import android.graphics.Color;
import android.os.Bundle;
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
import java.util.Collections;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initializeViews();
    }

    private void initializeViews() {
        // Get views
        TextView tvEmoji = findViewById(R.id.tvEmoji);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvUsageDetails = findViewById(R.id.tvUsageDetails);
        TextView tvReflection = findViewById(R.id.tvReflection);
        Button btnDone = findViewById(R.id.btnDone);
        BarChart barChart = findViewById(R.id.barChart);

        // Get data from intent
        String emoji = getIntent().getStringExtra("emoji");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        int usageMinutes = getIntent().getIntExtra("usageMinutes", 0);
        int appOpens = getIntent().getIntExtra("appOpens", 0);
        String topAppName = getIntent().getStringExtra("topAppName");
        int topAppMinutes = getIntent().getIntExtra("topAppMinutes", 0);
        boolean lateNight = getIntent().getBooleanExtra("lateNight", false);

        // Set text with animations
        setTextWithAnimation(tvEmoji, emoji != null ? emoji : "📱");
        setTextWithAnimation(tvTitle, title != null ? title : "Your Mood");
        setTextWithAnimation(tvDescription, description != null ? description : "");

        // Format usage details
        String usageText = formatUsageDetails(usageMinutes, appOpens, topAppName, topAppMinutes, lateNight);
        setTextWithAnimation(tvUsageDetails, usageText);

        // Set reflection
        String reflection = getReflectionLine(title, usageMinutes, appOpens, lateNight);
        setTextWithAnimation(tvReflection, reflection);

        // Setup chart
        setupBarChart(barChart, usageMinutes, appOpens, topAppMinutes, topAppName);

        // Done button
        btnDone.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setTextWithAnimation(TextView textView, String text) {
        textView.setText(text);
        textView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private String formatUsageDetails(int usageMinutes, int appOpens,
                                      String topAppName, int topAppMinutes,
                                      boolean lateNight) {
        StringBuilder details = new StringBuilder();
        details.append("📊 Today's Phone Usage\n\n");

        // Format total time
        if (usageMinutes >= 60) {
            int hours = usageMinutes / 60;
            int minutes = usageMinutes % 60;
            details.append(String.format("⏱ Total time: %dh %dm\n", hours, minutes));
        } else {
            details.append(String.format("⏱ Total time: %d minutes\n", usageMinutes));
        }

        details.append(String.format("📱 Apps opened: %d\n", appOpens));

        // Most used app
        if (topAppName != null && !topAppName.isEmpty()) {
            if (topAppMinutes >= 60) {
                int hours = topAppMinutes / 60;
                int minutes = topAppMinutes % 60;
                details.append(String.format("🏆 Most used: %s (%dh %dm)", topAppName, hours, minutes));
            } else {
                details.append(String.format("🏆 Most used: %s (%d min)", topAppName, topAppMinutes));
            }
        }

        if (lateNight) {
            details.append("\n🌙 Late night activity detected");
        }

        return details.toString();
    }

    private void setupBarChart(BarChart barChart, int usageMinutes, int appOpens,
                               int topAppMinutes, String topAppName) {
        ArrayList<BarEntry> entries = new ArrayList<>();

        // Add entries - normalize app opens for better visualization
        entries.add(new BarEntry(0f, usageMinutes));
        entries.add(new BarEntry(1f, appOpens * 5)); // Scale for better visualization
        entries.add(new BarEntry(2f, topAppMinutes));

        final String[] labels = new String[]{
                "Total (min)",
                "App Opens (x5)",
                topAppName != null ? topAppName.substring(0, Math.min(topAppName.length(), 15)) : "Top App"
        };

        // Create dataset
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(getBarColors());
        dataSet.setValueTextColor(Color.WHITE);
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

        // Configure X-axis
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
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);

        // Disable axes
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawBorders(false);
        barChart.setDrawGridBackground(false);

        // Description
        Description desc = new Description();
        desc.setText("");
        barChart.setDescription(desc);

        // Touch and zoom
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);

        // Animate
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private ArrayList<Integer> getBarColors() {
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#60A5FA")); // Blue - total usage
        colors.add(Color.parseColor("#34D399")); // Green - app opens
        colors.add(Color.parseColor("#FBBF24")); // Amber - top app
        return colors;
    }

    private String getReflectionLine(String moodTitle, int usageMinutes,
                                     int appOpens, boolean lateNight) {
        if (moodTitle == null) {
            return "Take a moment to reflect on your digital habits.";
        }

        switch (moodTitle) {
            case "Hyperfocused":
                return "Your attention stayed with one thing longer than usual.";

            case "Late-Night Thinker":
                return "Some thoughts chose the night instead of rest.";

            case "Restless Energy":
                return "Today felt full, but not always settled.";

            case "Distracted Mind":
                return "Your attention moved faster than your intentions.";

            case "Unplugged":
                return "A day lived beyond the screen. Balance found.";

            case "Calm & Grounded":
                return "Nothing extreme today. And that's a kind of balance.";
        }

        // Fallback based on usage
        if (usageMinutes > 360) {
            return "This was a heavy day — not necessarily a bad one.";
        } else if (usageMinutes < 60) {
            return "A quiet digital day. Sometimes less is more.";
        }

        return "Every day is a new pattern. Tomorrow might look different.";
    }
}