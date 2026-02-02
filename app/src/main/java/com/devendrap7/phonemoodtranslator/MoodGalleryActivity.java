package com.devendrap7.phonemoodtranslator;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;

public class MoodGalleryActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_gallery);

        // 1. Theme Logic: Calculate colors first
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int bgColor = prefs.getInt("bg_color", Color.parseColor("#4A148C")); // Default Purple

        // Smart Contrast: Is the background Dark?
        boolean isDark = (1 - (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255) >= 0.5;
        int textColor = isDark ? Color.WHITE : Color.BLACK;

        // Apply Background & Text Colors to UI
        findViewById(R.id.galleryRoot).setBackgroundColor(bgColor);
        ((TextView)findViewById(R.id.tvHeader)).setTextColor(textColor);
        ((TextView)findViewById(R.id.btnClose)).setTextColor(textColor);

        // 2. Initialize Gesture Detector (For Swipe Down)
        gestureDetector = new GestureDetector(this, new SwipeDownListener());

        // 3. Setup ViewPager (The Card Carousel)
        ViewPager2 viewPager = findViewById(R.id.moodPager);
        List<MoodGalleryAdapter.MoodItem> items = getMoodItems();

        // **CRITICAL:** Pass textColor and isDark flag to Adapter for neat cards
        viewPager.setAdapter(new MoodGalleryAdapter(items, textColor, isDark));

        // 4. Carousel Animation (Scale & Fade)
        viewPager.setOffscreenPageLimit(3);
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(40)); // Spacing
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f); // Center card is bigger
            page.setAlpha(0.5f + r * 0.5f);   // Side cards are faded
        });
        viewPager.setPageTransformer(transformer);

        // 5. Close Button (Backup if swipe fails)
        findViewById(R.id.btnClose).setOnClickListener(v -> finishWithAnimation());
    }

    // ==========================================
    // SWIPE DOWN DETECTION
    // ==========================================
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Check for Swipe Down FIRST
        if (gestureDetector != null) {
            if (gestureDetector.onTouchEvent(ev)) {
                return true; // We handled the swipe!
            }
        }
        // Otherwise, let standard touch events happen (like scrolling cards)
        return super.dispatchTouchEvent(ev);
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
    }

    private class SwipeDownListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return false; // Let the ViewPager capture the initial touch
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (e1 == null || e2 == null) return false;

                float diffY = e2.getY() - e1.getY(); // Vertical movement
                float diffX = e2.getX() - e1.getX(); // Horizontal movement

                // Check: Is this a Vertical Swipe? (More Up/Down than Left/Right)
                if (Math.abs(diffY) > Math.abs(diffX)) {
                    // Check: Is it fast enough and long enough?
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) { // Positive Y means DOWN
                            finishWithAnimation();
                            return true;
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }
    }

    // ==========================================
    // DATA GENERATION
    // ==========================================
    private List<MoodGalleryAdapter.MoodItem> getMoodItems() {
        List<MoodGalleryAdapter.MoodItem> items = new ArrayList<>();
        items.add(new MoodGalleryAdapter.MoodItem("🤯", "Overdose", "> 5 Hours", "System overload. You've lived more online than offline today."));
        items.add(new MoodGalleryAdapter.MoodItem("🔗", "Tethered", "4 - 5 Hours", "Not quite an overdose, but the phone feels glued to your hand."));
        items.add(new MoodGalleryAdapter.MoodItem("🔥", "Hyperfocused", "3+ Hrs (Low Switching)", "Deep work defined your day. Long sessions, zero interruptions."));
        items.add(new MoodGalleryAdapter.MoodItem("🧠", "Restless Energy", "3+ Hrs (High Switching)", "Your mind was running sprints. High usage with constant app switching."));
        items.add(new MoodGalleryAdapter.MoodItem("😵", "Distracted Mind", "< 2.5 Hrs (Extreme Switching)", "Focus was impossible. A butterfly flitting between apps."));
        items.add(new MoodGalleryAdapter.MoodItem("🧐", "Serious Mode", "Moderate Usage (Low Switch)", "Usage was purposeful, not random. You meant business today."));
        items.add(new MoodGalleryAdapter.MoodItem("🎡", "Light-hearted", "Moderate Usage (Avg Switch)", "Just browsing and chatting. Nothing too heavy today."));
        items.add(new MoodGalleryAdapter.MoodItem("😎", "Slick", "< 1.5 Hours", "In and out. Efficient. You rule the phone, it doesn't rule you."));
        items.add(new MoodGalleryAdapter.MoodItem("🌙", "Late-Night Thinker", "11 PM - 4 AM", "Sleep was sacrificed for scrolling. A midnight mind wandering."));
        items.add(new MoodGalleryAdapter.MoodItem("🌿", "Unplugged", "< 30 Mins", "A rare day of digital silence. Real life took priority."));
        items.add(new MoodGalleryAdapter.MoodItem("🧘", "Calm & Grounded", "< 4 Hours (Balanced)", "A balanced digital rhythm. Neither obsessed nor absent."));
        return items;
    }
}