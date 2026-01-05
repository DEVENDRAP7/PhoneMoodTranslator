package com.example.phonemoodtranslator;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // ================= UI =================
        TextView tvEmoji = findViewById(R.id.tvEmoji);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvUsageDetails = findViewById(R.id.tvUsageDetails);
        TextView tvReflection = findViewById(R.id.tvReflection);
        Button btnDone = findViewById(R.id.btnDone);
        BarChart barChart = findViewById(R.id.barChart);

        // ================= DATA =================
        String emoji = getIntent().getStringExtra("emoji");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");

        int usageMinutes = getIntent().getIntExtra("usageMinutes", 0);
        int appOpens = getIntent().getIntExtra("appOpens", 0);

        String topAppName = getIntent().getStringExtra("topAppName");
        int topAppMinutes = getIntent().getIntExtra("topAppMinutes", 0);

        // ================= SET TEXT =================
        tvEmoji.setText(emoji != null ? emoji : "📱");
        tvTitle.setText(title != null ? title : "Your Mood");
        tvDescription.setText(description != null ? description : "");

        tvUsageDetails.setText(
                "📊 Today’s Phone Usage\n\n" +
                        "⏱ Total time: " + usageMinutes + " minutes\n" +
                        "📱 Apps opened: " + appOpens + "\n" +
                        "🏆 Most used app: " + topAppName + " (" + topAppMinutes + " min)"
        );

        tvReflection.setText(getReflectionLine(title, usageMinutes));

        // ================= BAR CHART =================
        ArrayList<BarEntry> entries = new ArrayList<>();

        entries.add(new BarEntry(0f, usageMinutes));
        entries.add(new BarEntry(1f, appOpens));
        entries.add(new BarEntry(2f, topAppMinutes));

        final String[] labels = new String[]{
                "Usage",
                "App Opens",
                topAppName != null ? topAppName : "Top App"
        };

        BarDataSet dataSet = new BarDataSet(entries, "Usage");
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        dataSet.setColors(getRandomBarColors(entries.size()));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);

        // X-axis labels
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < labels.length ? labels[index] : "";
            }
        });

        // ================= CLEAN CHART STYLE =================
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getXAxis().setEnabled(false);

        barChart.getLegend().setEnabled(false);
        barChart.setDrawBorders(false);

        Description desc = new Description();
        desc.setText("");
        barChart.setDescription(desc);

        barChart.animateY(700);
        barChart.invalidate();

        // ================= DONE =================
        btnDone.setOnClickListener(v -> finish());
    }

    // ================= REFLECTION =================
    private String getReflectionLine(String moodTitle, int usageMinutes) {

        if ("Hyperfocused".equals(moodTitle)) {
            return "Your attention stayed with one thing longer than usual.";
        }

        if ("Late-Night Thinker".equals(moodTitle)) {
            return "Some thoughts chose the night instead of rest.";
        }

        if ("Restless Energy".equals(moodTitle)) {
            return "Today felt full, but not always settled.";
        }

        if ("Distracted Mind".equals(moodTitle)) {
            return "Your attention moved faster than your intentions.";
        }

        if (usageMinutes > 360) {
            return "This was a heavy day — not necessarily a bad one.";
        }

        return "Nothing extreme today. And that’s a kind of balance.";
    }
    private ArrayList<Integer> getColorPool() {
        ArrayList<Integer> pool = new ArrayList<>();

        // Calm / Premium colors
        pool.add(Color.parseColor("#60A5FA")); // soft blue
        pool.add(Color.parseColor("#34D399")); // mint green
        pool.add(Color.parseColor("#FBBF24")); // amber
        pool.add(Color.parseColor("#818CF8")); // indigo
        pool.add(Color.parseColor("#2DD4BF")); // teal
        pool.add(Color.parseColor("#A78BFA")); // violet
        pool.add(Color.parseColor("#86EFAC")); // soft green
        pool.add(Color.parseColor("#FDBA74")); // warm orange
        pool.add(Color.parseColor("#38BDF8")); // sky blue
        pool.add(Color.parseColor("#06B6D4")); // cyan
        pool.add(Color.parseColor("#60A5FA")); // Usage time (blue)
        pool.add(Color.parseColor("#34D399")); // App opens (green)
        pool.add(Color.parseColor("#FBBF24")); // Late night (amber)
        pool.add(Color.parseColor("#7C3AED")); // Deep purple
        pool.add(Color.parseColor("#06B6D4")); // Cyan
        pool.add(Color.parseColor("#F59E0B")); // Gold
        pool.add(Color.parseColor("#93C5FD")); // Light blue
        pool.add(Color.parseColor("#60A5FA")); // Medium blue
        pool.add(Color.parseColor("#2563EB")); // Deep blue
        pool.add(Color.parseColor("#38BDF8")); // Sky blue
        pool.add(Color.parseColor("#4ADE80")); // Emerald
        pool.add(Color.parseColor("#FACC15")); // Yellow
        pool.add(Color.parseColor("#A78BFA")); // Soft violet
        pool.add(Color.parseColor("#86EFAC")); // Soft green
        pool.add(Color.parseColor("#FDBA74")); // Warm orange
        pool.add(Color.parseColor("#818CF8")); // Indigo – thinking
        pool.add(Color.parseColor("#2DD4BF")); // Teal – engagement
        pool.add(Color.parseColor("#F472B6"));

        return pool;
    }
    private ArrayList<Integer> getRandomBarColors(int count) {

        ArrayList<Integer> pool = getColorPool();
        ArrayList<Integer> selected = new ArrayList<>();

        Collections.shuffle(pool);

        for (int i = 0; i < count && i < pool.size(); i++) {
            selected.add(pool.get(i));
        }

        return selected;
    }
}
