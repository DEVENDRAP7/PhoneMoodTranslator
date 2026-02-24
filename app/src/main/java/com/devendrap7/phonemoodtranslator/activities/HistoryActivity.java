package com.devendrap7.phonemoodtranslator.activities;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.fragments.AnalysisFragment;
import com.devendrap7.phonemoodtranslator.fragments.ControlsFragment;
import com.devendrap7.phonemoodtranslator.fragments.HeatmapFragment;
import com.devendrap7.phonemoodtranslator.fragments.HistoryFragment;
import com.google.android.material.appbar.AppBarLayout;

public class HistoryActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private AppBarLayout appBarLayout;

    // Tab containers
    private LinearLayout tab0, tab1, tab2, tab3;
    // Tab texts
    private TextView text0, text1, text2, text3 ,tvdigiInsights;

    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        // ✅ Make status bar match your app background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#F7E7CE"));
            // ✅ Make status bar icons dark (since background is light)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 1. Initialize Views
        viewPager = findViewById(R.id.viewPager);
        appBarLayout = findViewById(R.id.appBarLayout);
        tvdigiInsights = appBarLayout.findViewById(R.id.tvdigiInsights);


        // 2. Initialize Tabs
        tab0 = findViewById(R.id.tab0);
        tab1 = findViewById(R.id.tab1);
        tab2 = findViewById(R.id.tab2);
        tab3 = findViewById(R.id.tab3);
        text0 = findViewById(R.id.text0);
        text1 = findViewById(R.id.text1);
        text2 = findViewById(R.id.text2);
        text3 = findViewById(R.id.text3);

        // 3. Setup Theme
        setupTheme();

        // 4. Setup ViewPager
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4);

        // 5. Setup Tab Click Listeners
        tab0.setOnClickListener(v -> selectTab(0));
        tab1.setOnClickListener(v -> selectTab(1));
        tab2.setOnClickListener(v -> selectTab(2));
        tab3.setOnClickListener(v -> selectTab(3));

        // 6. Sync tabs when user swipes ViewPager
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectTab(position);
            }
        });

        // 7. Set default selected tab
        selectTab(0);
    }


    private void selectTab(int index) {
        // Deselect all tabs first
        deselectTab(tab0, text0);
        deselectTab(tab1, text1);
        deselectTab(tab2, text2);
        deselectTab(tab3, text3);

        // Select the clicked tab
        switch (index) {
            case 0: selectTab(tab0, text0); break;
            case 1: selectTab(tab1, text1); break;
            case 2: selectTab(tab2, text2); break;
            case 3: selectTab(tab3, text3); break;
        }

        // Navigate ViewPager only if tab clicked (not swiped)
        if (viewPager.getCurrentItem() != index) {
            viewPager.setCurrentItem(index, true);
        }

        currentTab = index;
    }

    private void selectTab(LinearLayout tab, TextView text) {
        // Show text with animation
        text.setVisibility(View.VISIBLE);
        text.setAlpha(0f);
        text.animate().alpha(1f).setDuration(200).start();

        // Expand tab weight with animation
        animateWeight(tab, 1f, 2.5f);

        // Highlight background
        tab.setBackgroundResource(R.drawable.tab_selected_bg);
    }

    private void deselectTab(LinearLayout tab, TextView text) {
        // Hide text
        text.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> text.setVisibility(View.GONE))
                .start();

        // Shrink tab weight
        animateWeight(tab, 2.5f, 1f);

        // Remove highlight
        tab.setBackgroundResource(R.drawable.tab_unselected_bg);
    }

    private void animateWeight(LinearLayout tab, float fromWeight, float toWeight) {
        ValueAnimator animator = ValueAnimator.ofFloat(fromWeight, toWeight);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            float weight = (float) animation.getAnimatedValue();
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tab.getLayoutParams();
            params.weight = weight;
            tab.setLayoutParams(params);
        });
        animator.start();
    }

    private void setupTheme() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        // User's purple/dark color
        int topColor = prefs.getInt("bg_color", Color.parseColor("#4A148C"));
        // Your fixed beige color
        int bottomColor = Color.parseColor("#F7E7CE");

        if (appBarLayout != null) {
            appBarLayout.setBackgroundColor(topColor);
            // ✅ 1. Kill the elevation shadow that creates the "white line"
            appBarLayout.setElevation(0f);
            appBarLayout.setOutlineProvider(null);
        }

        // ✅ 2. Match Status Bar to the Purple Top
        Window window = getWindow();
        window.setStatusBarColor(topColor);


        // ✅ 3. Ensure ViewPager/Activity Root is the Beige Bottom

        // ✅ 4. Dynamic Text Color (Purple background needs light text)
        int tabTextColor = isColorDark(topColor) ? Color.WHITE : Color.BLACK;

        tvdigiInsights.setTextColor(tabTextColor);
        text0.setTextColor(tabTextColor);
        text1.setTextColor(tabTextColor);
        text2.setTextColor(tabTextColor);
        text3.setTextColor(tabTextColor);
    }

    // Helper to determine contrast
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull AppCompatActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new AnalysisFragment();
                case 1: return new HeatmapFragment();
                case 2: return new HistoryFragment();
                default: return new ControlsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}