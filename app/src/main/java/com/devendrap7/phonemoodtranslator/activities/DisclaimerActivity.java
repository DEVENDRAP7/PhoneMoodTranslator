package com.devendrap7.phonemoodtranslator.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.devendrap7.phonemoodtranslator.R;

public class DisclaimerActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if disclaimer was already accepted
        if (isDisclaimerAccepted()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_disclaimer);

        Button btnAgree = findViewById(R.id.btnAgree);
        btnAgree.setOnClickListener(v -> {
            saveDisclaimerAccepted();
            navigateToMain();
        });
    }

    private boolean isDisclaimerAccepted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false);
    }

    private void saveDisclaimerAccepted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, true).apply();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Prevent going back from disclaimer
        // User must accept to proceed
    }
}