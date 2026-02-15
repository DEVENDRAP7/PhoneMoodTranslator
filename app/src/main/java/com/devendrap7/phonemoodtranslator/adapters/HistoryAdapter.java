package com.devendrap7.phonemoodtranslator.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.database.MoodHistoryItem;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<MoodHistoryItem> historyList;
    private OnItemClickListener listener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(MoodHistoryItem item);
    }

    // Constructor
    public HistoryAdapter(List<MoodHistoryItem> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ✅ CHANGE 1: Inflate the new Card Layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_day, parent, false);
        view.setBackgroundColor(Color.parseColor("#F7E7CE"));
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoodHistoryItem item = historyList.get(position);

        // 1. Date Parsing (Fancy Box)
        try {
            String[] parts = item.date.split(" ");
            if (parts.length >= 2) {
                holder.tvDayDate.setText(parts[0]);
                holder.tvDayMonth.setText(parts[1].toUpperCase());
            } else {
                holder.tvDayDate.setText(item.date);
                holder.tvDayMonth.setText("");
            }
        } catch (Exception e) {
            holder.tvDayDate.setText("??");
            holder.tvDayMonth.setText("---");
        }

        // 2. Title & Emoji (Ensuring consistent Size)
        holder.tvMoodTitle.setText(item.title);
        holder.tvMoodTitle.setTextSize(16); // Force size to be sure
        holder.tvMoodEmoji.setText(item.emoji);


        if (item.usageStr == null || item.usageStr.isEmpty() || item.usageStr.equals("Total: ")) {
            holder.tvTotalTime.setVisibility(View.GONE);
            holder.pbDailyHealth.setVisibility(View.GONE);
        } else {
            holder.tvTotalTime.setVisibility(View.VISIBLE);
            holder.pbDailyHealth.setVisibility(View.VISIBLE);
            holder.tvTotalTime.setText(item.usageStr);
            double maxMillis = 12 * 60 * 60 * 1000.0;
            int usageMinutes = (int) (item.usageMillis / (1000 * 60));

            // 2. Set the Progress
            // Since XML max is 1000 (roughly 16 hours), 440 mins will be 44% full.
            holder.pbDailyHealth.setMax(720);
            int usageStr = (int) (item.usageMillis / (1000 * 60));
            holder.pbDailyHealth.setProgress(usageMinutes);
            //holder.pbDailyHealth.setProgress(usageMinutes);

            // 3. Force Color Logic
            int color;
            if (usageMinutes > 420) { // Over 7 hours
                color = Color.parseColor("#FF0800"); // Startup Red
            } else if (usageMinutes < 180) { // Under 3 hours
                color = Color.parseColor("#388E3C"); // Green
            } else {
                color = Color.parseColor("#FFB300"); // Startup Gold
            }

            // Applying the color filter to the progress bar drawable
            holder.pbDailyHealth.setProgressTintList(android.content.res.ColorStateList.valueOf(color));

            // 4. Handle Visibility (Hide for "Today" if no data)
            if (usageMinutes <= 0) {
                holder.tvTotalTime.setVisibility(View.GONE);
                holder.pbDailyHealth.setVisibility(View.GONE);
            } else {
                holder.tvTotalTime.setVisibility(View.VISIBLE);
                holder.pbDailyHealth.setVisibility(View.VISIBLE);
                holder.tvTotalTime.setText(item.usageStr);
            }
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    // Helper to parse "4h 30m" into integer minutes
    private int parseMinutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0;
        try {
            // Remove "Total:" if it exists, and remove all spaces
            String clean = timeStr.replace("Total:", "").replace(" ", "").trim();
            int h = 0, m = 0;

            if (clean.contains("h")) {
                String[] parts = clean.split("h");
                h = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                if (parts.length > 1 && parts[1].contains("m")) {
                    m = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                }
            } else if (clean.contains("m")) {
                m = Integer.parseInt(clean.replaceAll("[^0-9]", ""));
            }
            return (h * 60) + m;
        } catch (Exception e) {
            return 0; // If parsing fails, bar stays at 0
        }
    }
    // ✅ CHANGE 4: Update ViewHolder to match new IDs in item_history_day.xml
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayDate, tvDayMonth, tvMoodTitle, tvTotalTime, tvMoodEmoji;
        ProgressBar pbDailyHealth;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayDate = itemView.findViewById(R.id.tvDayDate);
            tvDayMonth = itemView.findViewById(R.id.tvDayMonth);
            tvMoodTitle = itemView.findViewById(R.id.tvMoodTitle);
            tvTotalTime = itemView.findViewById(R.id.tvTotalTime);
            tvMoodEmoji = itemView.findViewById(R.id.tvMoodEmoji);
            pbDailyHealth = itemView.findViewById(R.id.pbDailyHealth);
        }
    }
}