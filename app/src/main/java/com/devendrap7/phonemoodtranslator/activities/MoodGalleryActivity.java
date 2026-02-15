package com.devendrap7.phonemoodtranslator.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.devendrap7.phonemoodtranslator.R;

import java.util.ArrayList;
import java.util.List;

public class MoodGalleryActivity extends AppCompatActivity {

    private ViewPager2 moodPager;
    private TextView btnClose;
    private View rootLayout;
    private GestureDetector gestureDetector;
    private int themeTextColor; // The adaptive color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_gallery);

        moodPager = findViewById(R.id.moodPager);
        btnClose = findViewById(R.id.btnClose);
        rootLayout = findViewById(R.id.galleryRoot);

        // 1. Theme & Contrast Setup
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int savedColor = prefs.getInt("bg_color", Color.parseColor("#4A148C"));

        if (rootLayout != null) rootLayout.setBackgroundColor(savedColor);

        // Calculate if background is Dark or Light
        boolean isDark = (1 - (0.299 * Color.red(savedColor) + 0.587 * Color.green(savedColor) + 0.114 * Color.blue(savedColor)) / 255) >= 0.5;

        // Set Text Color: White for Dark themes, Black for Light themes
        themeTextColor = isDark ? Color.WHITE : Color.BLACK;

        // Apply color to the "Close" button immediately
        btnClose.setTextColor(themeTextColor);
        btnClose.setAlpha(0.6f); // Keep the transparency style

        // 2. Load Data
        List<MoodItem> allMoods = loadAllMoods();

        // 3. Setup Adapter (PASS THE COLOR HERE)
        MoodAdapter adapter = new MoodAdapter(allMoods, themeTextColor);
        moodPager.setAdapter(adapter);
        moodPager.setPageTransformer(new ZoomOutPageTransformer());

        // 4. Gesture Logic (Swipe Down to Close)
        gestureDetector = new GestureDetector(this, new SwipeDownListener());
        View child = moodPager.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        }

        // 5. Close Actions
        btnClose.setOnClickListener(v -> closeGallery());
        rootLayout.setOnClickListener(v -> closeGallery());
    }

    private void closeGallery() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
    }

    private class SwipeDownListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) { return false; }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) { closeGallery(); return true; }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            return false;
        }
    }

    private List<MoodItem> loadAllMoods() {
        List<MoodItem> moods = new ArrayList<>();
        moods.add(new MoodItem("🤯", "Overdose", "Usage > 7 Hours", "System overload detected. You've lived more online than offline today."));
        moods.add(new MoodItem("🔗", "Tethered", "Usage 6 - 7 Hours", "The phone feels glued to your hand. Borderline heavy usage pattern."));
        moods.add(new MoodItem("🌿", "Unplugged", "Usage < 30 Mins", "Real life took priority today. Digital distance felt natural and healthy."));
        moods.add(new MoodItem("🌙", "Late-Night Thinker", "11 PM - 4 AM Activity", "Sleep was sacrificed for scrolling. A midnight mind wandering through pixels."));
        moods.add(new MoodItem("🔥", "Hyperfocused", "Usage > 3h • Low Unlocks", "Deep work defined your day. Sustained attention on a single task."));
        moods.add(new MoodItem("🧠", "Restless Energy", "Usage > 3h • High Unlocks", "Your mind was running sprints. Constant switching seeking stimulation."));
        moods.add(new MoodItem("😵", "Distracted Mind", "Moderate Usage • High Unlocks", "Focus was impossible today. A digital butterfly flitting from app to app."));
        moods.add(new MoodItem("😎", "Slick", "Usage < 1.5 Hours", "In and out. Efficient. You used the tool, the tool didn't use you."));
        moods.add(new MoodItem("🧐", "Serious Mode", "Moderate Time • Low Unlocks", "Usage was purposeful. You came for a reason and stayed for it."));
        moods.add(new MoodItem("🎡", "Light-hearted", "Balanced Time • High Unlocks", "Just browsing, chatting, and wandering. A casual digital stroll."));
        moods.add(new MoodItem("🧘", "Calm & Grounded", "Balanced Stats", "A perfect digital rhythm. Not too long, not too frantic. Just right."));
        return moods;
    }

    static class MoodItem {
        String emoji, title, criteria, desc;
        MoodItem(String e, String t, String c, String d) { this.emoji = e; this.title = t; this.criteria = c; this.desc = d; }
    }

    // --- ADAPTER UPDATED TO ACCEPT COLOR ---
    class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.MoodViewHolder> {
        List<MoodItem> list;
        int textColor; // New Variable

        MoodAdapter(List<MoodItem> list, int textColor) {
            this.list = list;
            this.textColor = textColor; // Store the adaptive color
        }

        @NonNull
        @Override
        public MoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mood_card, parent, false);
            return new MoodViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MoodViewHolder holder, int position) {
            MoodItem item = list.get(position);
            holder.tvEmoji.setText(item.emoji);
            holder.tvTitle.setText(item.title);
            holder.tvCriteria.setText(item.criteria);
            holder.tvDesc.setText(item.desc);

            // --- APPLY THE COLOR ---
            holder.tvTitle.setTextColor(textColor);

            holder.tvDesc.setTextColor(textColor);
            holder.tvDesc.setAlpha(0.7f); // 70% opacity for description
        }

        @Override
        public int getItemCount() { return list.size(); }

        class MoodViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvTitle, tvCriteria, tvDesc;
            MoodViewHolder(View v) {
                super(v);
                tvEmoji = v.findViewById(R.id.tvCardEmoji);
                tvTitle = v.findViewById(R.id.tvCardTitle);
                tvCriteria = v.findViewById(R.id.tvCardCriteria);
                tvDesc = v.findViewById(R.id.tvCardDesc);
            }
        }
    }

    // --- Transformer (Unchanged) ---
    public class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();
            if (position < -1) { view.setAlpha(0f); }
            else if (position <= 1) {
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) { view.setTranslationX(horzMargin - vertMargin / 2); }
                else { view.setTranslationX(-horzMargin + vertMargin / 2); }
                view.setScaleX(scaleFactor); view.setScaleY(scaleFactor);
                view.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            } else { view.setAlpha(0f); }
        }
    }
}