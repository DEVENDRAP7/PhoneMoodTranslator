package com.devendrap7.phonemoodtranslator.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.devendrap7.phonemoodtranslator.R;

import java.util.List;

public class MoodGalleryAdapter extends RecyclerView.Adapter<MoodGalleryAdapter.MoodViewHolder> {

    private List<MoodItem> moodList;
    private int themeTextColor;
    private boolean isDarkTheme; // New flag

    // Update Constructor
    public MoodGalleryAdapter(List<MoodItem> moodList, int themeTextColor, boolean isDarkTheme) {
        this.moodList = moodList;
        this.themeTextColor = themeTextColor;
        this.isDarkTheme = isDarkTheme;
    }

    @NonNull
    @Override
    public MoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mood_card, parent, false);
        return new MoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodViewHolder holder, int position) {
        MoodItem item = moodList.get(position);

        holder.emoji.setText(item.emoji);
        holder.title.setText(item.title);
        holder.desc.setText(item.desc);
        holder.criteria.setText(item.criteria);

        // 1. DYNAMIC TEXT COLOR
        holder.title.setTextColor(themeTextColor);
        holder.desc.setTextColor(themeTextColor);

        // 2. STATIC CARD BACKGROUND (Contrast)
        // If theme is Dark -> Card is 10% White (Glass)
        // If theme is Light -> Card is 10% Black (Smoked Glass)
        int cardColor = isDarkTheme ? Color.parseColor("#1AFFFFFF") : Color.parseColor("#10000000");

        // Apply the color while keeping the rounded corners
        GradientDrawable bg = (GradientDrawable) holder.cardRoot.getBackground();
        bg.setColor(cardColor);

        // Optional: Add a subtle border that matches the text
        bg.setStroke(2, themeTextColor & 0x40FFFFFF); // 25% opacity of text color
    }

    @Override
    public int getItemCount() {
        return moodList.size();
    }

    static class MoodViewHolder extends RecyclerView.ViewHolder {
        TextView emoji, title, criteria, desc;
        ConstraintLayout cardRoot; // Reference to the card container

        MoodViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            emoji = itemView.findViewById(R.id.tvCardEmoji);
            title = itemView.findViewById(R.id.tvCardTitle);
            criteria = itemView.findViewById(R.id.tvCardCriteria);
            desc = itemView.findViewById(R.id.tvCardDesc);
        }
    }

    public static class MoodItem {
        String emoji, title, criteria, desc;
        public MoodItem(String e, String t, String c, String d) {
            this.emoji = e; this.title = t; this.criteria = c; this.desc = d;
        }
    }
}