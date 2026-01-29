package com.devendrap7.phonemoodtranslator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView tvHistory = findViewById(R.id.tvHistory);
        Button back = findViewById(R.id.back);

        back.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        loadAndDisplayHistory(tvHistory);
    }

    private void loadAndDisplayHistory(TextView tvHistory) {
        SharedPreferences prefs = getSharedPreferences("mood_history", MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        if (all.isEmpty()) {
            tvHistory.setText("No history yet.\nReflect again tomorrow 🌙");
            return;
        }

        // Sort entries by date (most recent first)
        List<MoodHistoryEntry> entries = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String dateString = entry.getKey();
            String value = entry.getValue().toString();

            try {
                Date date = dateFormat.parse(dateString);
                entries.add(new MoodHistoryEntry(dateString, value, date));
            } catch (ParseException e) {
                // If parsing fails, add with null date (will be sorted last)
                entries.add(new MoodHistoryEntry(dateString, value, null));
            }
        }

        // Sort by date (most recent first)
        Collections.sort(entries, new Comparator<MoodHistoryEntry>() {
            @Override
            public int compare(MoodHistoryEntry e1, MoodHistoryEntry e2) {
                if (e1.date == null) return 1;
                if (e2.date == null) return -1;
                return e2.date.compareTo(e1.date); // Descending order
            }
        });

        // Build display string
        StringBuilder builder = new StringBuilder();
        builder.append("📅 Your Mood History\n");
        builder.append("━━━━━━━━━━━━━━━━━━━━\n\n");

        for (MoodHistoryEntry entry : entries) {
            String[] parts = entry.value.split("\\|");

            if (parts.length >= 2) {
                String emoji = parts[0];
                String title = parts[1];

                // If we have extended data (new format)
                if (parts.length >= 6) {
                    long usageMinutes = Long.parseLong(parts[2]);
                    int appOpens = Integer.parseInt(parts[3]);
                    String topApp = parts[4];
                    int topAppMinutes = Integer.parseInt(parts[5]);

                    builder.append("📆 ").append(entry.dateString).append("\n");
                    builder.append(emoji).append(" ").append(title).append("\n");
                    builder.append(formatUsageTime(usageMinutes))
                            .append(" • ")
                            .append(appOpens).append(" apps\n");
                    if (topApp != null && !topApp.isEmpty() && !topApp.equals("Unknown App")) {
                        builder.append("🏆 ").append(topApp).append("\n");
                    }
                    builder.append("\n");
                } else {
                    // Old format (just emoji and title)
                    builder.append("📆 ").append(entry.dateString).append("\n");
                    builder.append(emoji).append(" ").append(title).append("\n\n");
                }
            }
        }

        tvHistory.setText(builder.toString());
    }

    private String formatUsageTime(long minutes) {
        if (minutes >= 60) {
            long hours = minutes / 60;
            long mins = minutes % 60;
            return String.format(Locale.getDefault(), "⏱ %dh %dm", hours, mins);
        } else {
            return String.format(Locale.getDefault(), "⏱ %dm", minutes);
        }
    }

    // Helper class for sorting
    private static class MoodHistoryEntry {
        String dateString;
        String value;
        Date date;

        MoodHistoryEntry(String dateString, String value, Date date) {
            this.dateString = dateString;
            this.value = value;
            this.date = date;
        }
    }
}