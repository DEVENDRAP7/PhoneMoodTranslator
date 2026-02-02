package com.devendrap7.phonemoodtranslator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private int themeTextColor; // Black or White
    private int themeCardColor; // Glassy background color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 1. Initialize Views
        LinearLayout container = findViewById(R.id.llHistoryContainer);
        Button back = findViewById(R.id.back);
        TextView tvTitle = findViewById(R.id.tvPageTitle);
        View rootLayout = findViewById(R.id.historyRootLayout);

        // 2. Calculate Theme Colors FIRST
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int savedColor = prefs.getInt("bg_color", Color.parseColor("#4A148C"));

        // Is the background dark?
        boolean isDark = (1 - (0.299 * Color.red(savedColor) + 0.587 * Color.green(savedColor) + 0.114 * Color.blue(savedColor)) / 255) >= 0.5;

        // Set Colors based on Theme
        themeTextColor = isDark ? Color.WHITE : Color.BLACK;
        themeCardColor = isDark ? Color.parseColor("#20FFFFFF") : Color.parseColor("#10000000"); // White Glass vs Black Glass

        // 3. Apply to Main Layout
        if (rootLayout != null) rootLayout.setBackgroundColor(savedColor);
        if (tvTitle != null) tvTitle.setTextColor(themeTextColor);
        if (back != null) {
            back.setBackgroundTintList(ColorStateList.valueOf(themeTextColor));
            back.setTextColor(savedColor); // Button text matches background
        }

        // 4. Load & Build Cards (Now using the correct colors)
        loadHistoryCards(container);

        back.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void loadHistoryCards(LinearLayout container) {
        SharedPreferences prefs = getSharedPreferences("mood_history", MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        if (all.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No history yet.\nReflect tomorrow! 🌙");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 100, 0, 0);
            emptyView.setTextColor(themeTextColor); // Adapted color
            container.addView(emptyView);
            return;
        }

        List<MoodHistoryEntry> entries = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String dateString = entry.getKey();
            Object valObj = entry.getValue();
            if (valObj == null) continue;

            try {
                Date date = dateFormat.parse(dateString);
                entries.add(new MoodHistoryEntry(dateString, valObj.toString(), date));
            } catch (ParseException e) {
                entries.add(new MoodHistoryEntry(dateString, valObj.toString(), null));
            }
        }

        Collections.sort(entries, (e1, e2) -> {
            if (e1.date == null) return 1;
            if (e2.date == null) return -1;
            return e2.date.compareTo(e1.date);
        });

        for (MoodHistoryEntry entry : entries) {
            View card = createHistoryCard(entry);
            container.addView(card);
        }
    }

    private View createHistoryCard(MoodHistoryEntry entry) {
        String[] parts = entry.value.split("\\|");
        if (parts.length < 2) return new View(this);

        String emoji = parts[0];
        String title = parts[1];
        int usageMin = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 0;
        int appOpens = (parts.length >= 4) ? Integer.parseInt(parts[3]) : 0;
        String topApp = (parts.length >= 5) ? parts[4] : "";
        int topAppMin = (parts.length >= 6) ? Integer.parseInt(parts[5]) : 0;

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 32);
        cardLayout.setLayoutParams(params);
        cardLayout.setPadding(40, 40, 40, 40);

        // DYNAMIC CARD BACKGROUND
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(themeCardColor); // Uses calculated glass color
        bg.setCornerRadius(32);
        // Subtle border matching text color (25% opacity)
        bg.setStroke(2, themeTextColor & 0x40FFFFFF);
        cardLayout.setBackground(bg);

        // 1. Date Header
        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + entry.dateString);
        tvDate.setTextSize(12);
        // Set date to 70% opacity of main text color
        tvDate.setTextColor(themeTextColor & 0xB3FFFFFF);
        cardLayout.addView(tvDate);

        // 2. Row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(32);
        tvEmoji.setPadding(0, 0, 24, 0);
        tvEmoji.setTextColor(themeTextColor); // Ensure Emoji is visible

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(themeTextColor); // Dynamic Color

        row.addView(tvEmoji);
        row.addView(tvTitle);
        cardLayout.addView(row);

        // 3. Stats
        TextView tvStats = new TextView(this);
        tvStats.setText(String.format(Locale.getDefault(), "%dh %dm • %d unlocks",
                usageMin/60, usageMin%60, appOpens));
        // Set stats to 80% opacity of main text color
        tvStats.setTextColor(themeTextColor & 0xCCFFFFFF);
        tvStats.setPadding(0, 16, 0, 0);
        cardLayout.addView(tvStats);

        cardLayout.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, ResultActivity.class);
            intent.putExtra("emoji", emoji);
            intent.putExtra("title", title);
            intent.putExtra("description", "Historical Data from " + entry.dateString);
            intent.putExtra("usageMinutes", usageMin);
            intent.putExtra("appOpens", appOpens);
            intent.putExtra("topAppName", topApp);
            intent.putExtra("topAppMinutes", topAppMin);
            intent.putExtra("lateNight", title.equals("Late-Night Thinker"));

            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        return cardLayout;
    }

    private static class MoodHistoryEntry {
        String dateString, value;
        Date date;
        MoodHistoryEntry(String d, String v, Date dt) { this.dateString = d; this.value = v; this.date = dt; }
    }
}