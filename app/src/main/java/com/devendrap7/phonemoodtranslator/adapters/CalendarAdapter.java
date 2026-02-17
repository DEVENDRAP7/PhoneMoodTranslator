package com.devendrap7.phonemoodtranslator.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.database.DailyStats;
import com.devendrap7.phonemoodtranslator.views.MoodPetView;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    // ✅ Click listener interface
    public interface OnDayClickListener {
        void onDayClick(DailyStats stats);
    }

    private final List<DailyStats> monthlyStats;
    private final int daysInMonth;
    private final int firstDayOfWeek;
    private OnDayClickListener listener;

    public CalendarAdapter(List<DailyStats> monthlyStats,
                           int daysInMonth, int firstDayOfWeek) {
        this.monthlyStats  = monthlyStats;
        this.daysInMonth   = daysInMonth;
        this.firstDayOfWeek = firstDayOfWeek;
    }

    // ✅ Set click listener from fragment
    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        int dayOfMonth = position - firstDayOfWeek + 1;

        if (dayOfMonth <= 0 || dayOfMonth > daysInMonth) {
            holder.itemView.setVisibility(View.INVISIBLE);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.tvDate.setText(String.valueOf(dayOfMonth));

            DailyStats statsForDay = findStatsForDay(dayOfMonth);

            if (statsForDay != null) {
                // ✅ Has data — warm card
                holder.cardDayRoot.setCardBackgroundColor(
                        Color.parseColor("#FFF8E1"));
                holder.cardDayRoot.setStrokeColor(
                        Color.parseColor("#FFB74D"));
                holder.tvDate.setTextColor(Color.parseColor("#1c1554"));

                // ✅ Pop animation
                holder.itemView.setScaleX(0.8f);
                holder.itemView.setScaleY(0.8f);
                holder.itemView.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(350)
                        .setInterpolator(
                                new android.view.animation.OvershootInterpolator())
                        .start();

                holder.petView.setLoading(false);
                holder.petView.setMoodData(
                        (int)(statsForDay.totalUsageTime / 60000));

                // ✅ Tap → open detail popup
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onDayClick(statsForDay);
                });

            } else {
                // ✅ No data — empty card
                holder.cardDayRoot.setCardBackgroundColor(Color.WHITE);
                holder.cardDayRoot.setStrokeColor(
                        Color.parseColor("#F0F0F0"));
                holder.tvDate.setTextColor(Color.parseColor("#CCCCCC"));
                holder.petView.setVisibility(View.INVISIBLE);
                holder.itemView.setOnClickListener(null);
            }
        }
    }

    private DailyStats findStatsForDay(int day) {
        for (DailyStats s : monthlyStats) {
            try {
                if (Integer.parseInt(s.date.split(" ")[0]) == day)
                    return s;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() { return 42; }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        MoodPetView petView;
        MaterialCardView cardDayRoot;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate      = itemView.findViewById(R.id.tvDateNumber);
            petView     = itemView.findViewById(R.id.calendarPet);
            cardDayRoot = itemView.findViewById(R.id.cardDayRoot);
        }
    }
}