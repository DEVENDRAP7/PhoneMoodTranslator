package com.devendrap7.phonemoodtranslator.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.views.MoodPetView;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding already completed
        if (isOnboardingCompleted()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_onboarding_simple);

        // Status bar styling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#F7E7CE"));
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        MoodPetView petView = findViewById(R.id.onboardingPet);
        Button btnGetStarted = findViewById(R.id.btnGetStarted);

        // Animate pet
        petView.setMoodData(120);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(petView, "scaleX", 0.5f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(petView, "scaleY", 0.5f, 1.2f, 1f);
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.start();
        scaleY.start();

        btnGetStarted.setOnClickListener(v -> {
            saveOnboardingCompleted();
            navigateToMain();
        });
    }

    private boolean isOnboardingCompleted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    private void saveOnboardingCompleted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Prevent going back from onboarding
    }
}