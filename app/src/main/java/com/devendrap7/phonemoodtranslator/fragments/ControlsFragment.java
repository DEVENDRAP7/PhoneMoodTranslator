package com.devendrap7.phonemoodtranslator.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.devendrap7.phonemoodtranslator.R;
import com.devendrap7.phonemoodtranslator.database.AppDatabase;
import com.devendrap7.phonemoodtranslator.database.DailyStats;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ControlsFragment extends Fragment {

    private TextInputEditText etFutureNote;
    private Button btnSaveNote;
    private SwitchMaterial switchSocialLimit;
    private SwitchMaterial switchTotalLimit;
    private View viewColorPreview;
    private Button btnPickColor;
    private int currentColor;
    private TextView tvAboutDesc;

    // ✅ Battery optimization status views
    private CardView cardTrackingStatus;
    private TextView tvStatusIcon;
    private TextView tvStatusLabel;
    private TextView tvStatusMessage;
    private Button btnEnableTracking;
    TextView tvSocialSubtitle,tvTotalSubtitle;
    Button btnChangeSocial, btnChangeTotal;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_controls, container, false);

        etFutureNote     = view.findViewById(R.id.etFutureNote);
        btnSaveNote      = view.findViewById(R.id.btnSaveNote);
        switchSocialLimit = view.findViewById(R.id.switchSocialLimit);
        switchTotalLimit  = view.findViewById(R.id.switchTotalLimit);
        viewColorPreview  = view.findViewById(R.id.viewColorPreview);
        btnPickColor      = view.findViewById(R.id.btnPickColor);
        tvAboutDesc       = view.findViewById(R.id.tvAboutDesc);
        btnChangeSocial = view.findViewById(R.id.btnChangeSocial);
        btnChangeTotal = view.findViewById(R.id.btnChangeTotal);

        // ✅ Find battery optimization status views
        cardTrackingStatus = view.findViewById(R.id.cardTrackingStatus);
        tvStatusIcon       = view.findViewById(R.id.tvStatusIcon);
        tvStatusLabel      = view.findViewById(R.id.tvStatusLabel);
        tvStatusMessage    = view.findViewById(R.id.tvStatusMessage);
        btnEnableTracking  = view.findViewById(R.id.btnEnableTracking);

        // Inside onCreateView(), after finding all views:
         tvSocialSubtitle = view.findViewById(R.id.tvSocialSubtitle);
         tvTotalSubtitle = view.findViewById(R.id.tvTotalSubtitle);
        updateLimitSubtitles(tvSocialSubtitle, tvTotalSubtitle);

        androidx.cardview.widget.CardView cardSocialLimit = view.findViewById(R.id.cardSocialLimit);
        androidx.cardview.widget.CardView cardTotalLimit = view.findViewById(R.id.cardTotalLimit);

        btnChangeSocial.setOnClickListener(v -> showCustomizeLimitsDialog());
        btnChangeTotal.setOnClickListener(v -> showCustomizeLimitsDialog());

        cardSocialLimit.setOnClickListener(v -> showCustomizeLimitsDialog());
        cardTotalLimit.setOnClickListener(v -> showCustomizeLimitsDialog());

        String desc = "Digi Pulse helps you understand your screen time habits through mood-based insights. ";

        SpannableString spannable = new SpannableString(desc + "Read More →");

        spannable.setSpan(new android.text.style.ForegroundColorSpan(
                        Color.parseColor("#1c1554")),
                desc.length(), spannable.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(new android.text.style.StyleSpan(
                        android.graphics.Typeface.BOLD),
                desc.length(), spannable.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(new android.text.style.ClickableSpan() {
                              @Override
                              public void onClick(@NonNull View widget) {
                                  Dialog dialog = new Dialog(requireContext(),
                                          com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar);
                                  dialog.setContentView(R.layout.dialog_about_fullscreen);
                                  dialog.findViewById(R.id.btnCloseAbout)
                                          .setOnClickListener(cv -> dialog.dismiss());

                                  dialog.findViewById(R.id.btnRateApp)
                                          .setOnClickListener(rv -> {
                                              String packageName = requireContext().getPackageName();
                                              try {
                                                  startActivity(new Intent(Intent.ACTION_VIEW,
                                                          android.net.Uri.parse(
                                                                  "market://details?id=" + packageName)));
                                              } catch (android.content.ActivityNotFoundException e) {
                                                  startActivity(new Intent(Intent.ACTION_VIEW,
                                                          android.net.Uri.parse(
                                                                  "https://play.google.com/store/apps/details?id="
                                                                          + packageName)));
                                              }
                                          });
                                  dialog.show();
                              }

                              @Override
                              public void updateDrawState(@NonNull android.text.TextPaint ds) {
                                  ds.setColor(Color.parseColor("#1c1554"));
                                  ds.setUnderlineText(false);
                              }
                          }, desc.length(), spannable.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvAboutDesc.setText(spannable);
        tvAboutDesc.setMovementMethod(
                android.text.method.LinkMovementMethod.getInstance());
        tvAboutDesc.setHighlightColor(Color.TRANSPARENT);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        etFutureNote.setText(prefs.getString("future_note", ""));
        switchSocialLimit.setChecked(
                prefs.getBoolean("limit_social_enabled", true));
        switchTotalLimit.setChecked(
                prefs.getBoolean("limit_total_enabled", true));

        currentColor = prefs.getInt("bg_color",
                Color.parseColor("#4A148C"));
        viewColorPreview.setBackground(
                ContextCompat.getDrawable(requireContext(), R.drawable.circle_preview));
        viewColorPreview.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(currentColor));

        btnSaveNote.setOnClickListener(v -> {
            String note = etFutureNote.getText().toString();
            prefs.edit().putString("future_note", note).apply();

            new Thread(() -> {
                AppDatabase db = AppDatabase.getDatabase(getContext());
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "dd MMM yyyy", Locale.ENGLISH);
                String todayDate = sdf.format(
                        Calendar.getInstance().getTime());

                int rowsUpdated = db.statsDao()
                        .updateNote(todayDate, note);

                if (rowsUpdated == 0) {
                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH) + 1;
                    int currentYear  = cal.get(Calendar.YEAR);
                    int currentDay   = cal.get(Calendar.DAY_OF_MONTH);
                    long timestamp   = cal.getTimeInMillis();

                    DailyStats newDay = new DailyStats(
                            todayDate, currentMonth, currentYear,
                            currentDay, timestamp,
                            0, 0L, 0,
                            "🧘", "Starting Day",
                            "[]", note
                    );
                    db.statsDao().insert(newDay);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Note Saved! ✅",
                                Toast.LENGTH_SHORT).show();
                        etFutureNote.clearFocus();
                    });
                }
            }).start();
        });

        switchSocialLimit.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("limit_social_enabled", isChecked).apply();
            Toast.makeText(getContext(),
                    isChecked ? "Social media limit enabled" : "Social media limit disabled",
                    Toast.LENGTH_SHORT).show();
        });

        switchTotalLimit.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("limit_total_enabled", isChecked).apply();
            Toast.makeText(getContext(),
                    isChecked ? "Total usage limit enabled" : "Total usage limit disabled",
                    Toast.LENGTH_SHORT).show();
        });

        btnPickColor.setOnClickListener(v -> {
            AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(
                    requireContext(), currentColor,
                    new AmbilWarnaDialog.OnAmbilWarnaListener() {
                        @Override
                        public void onCancel(AmbilWarnaDialog dialog) {}

                        @Override
                        public void onOk(AmbilWarnaDialog dialog,
                                         int color) {
                            currentColor = color;
                            prefs.edit().putInt("bg_color", color).apply();
                            viewColorPreview.setBackground(
                                    ContextCompat.getDrawable(requireContext(), R.drawable.circle_preview));
                            viewColorPreview.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(currentColor));
                            Toast.makeText(getContext(),
                                    "Theme saved! Restart app to apply.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
            colorPicker.show();
        });

        // ✅ Update battery optimization status
        updateBatteryOptimizationStatus();

        // ✅ Enable tracking button click
        btnEnableTracking.setOnClickListener(v -> {
            openBatteryOptimizationSettings();
        });

        return view;
    }
    private void updateLimitSubtitles(TextView tvSocial, TextView tvTotal) {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        float socialHours = prefs.getFloat("limit_social_hours", 2.0f);
        float totalHours = prefs.getFloat("limit_total_hours", 6.0f);

        tvSocial.setText("Alert after " + formatHours(socialHours));
        tvTotal.setText("Alert after " + formatHours(totalHours));
    }
    // Add this method to ControlsFragment.java

    private void showCustomizeLimitsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_customize_limits);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        SeekBar seekBarSocial = dialog.findViewById(R.id.seekBarSocial);
        SeekBar seekBarTotal = dialog.findViewById(R.id.seekBarTotal);
        TextView tvSocialValue = dialog.findViewById(R.id.tvSocialValue);
        TextView tvTotalValue = dialog.findViewById(R.id.tvTotalValue);
        Button btnSave = dialog.findViewById(R.id.btnSaveLimits);

        // Load current values
        float currentSocial = prefs.getFloat("limit_social_hours", 2.0f);
        float currentTotal = prefs.getFloat("limit_total_hours", 6.0f);

        // Convert hours to SeekBar progress (0 = 30min, 1 = 1h, 2 = 1.5h, etc)
        int socialProgress = (int)((currentSocial - 0.5f) / 0.5f);
        int totalProgress = (int)((currentTotal - 0.5f) / 0.5f);

        seekBarSocial.setProgress(socialProgress);
        seekBarTotal.setProgress(totalProgress);
        tvSocialValue.setText(formatHours(currentSocial));
        tvTotalValue.setText(formatHours(currentTotal));



        // SeekBar listeners
        seekBarSocial.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float hours = 0.5f + (progress * 0.5f); // 0=30min, 1=1h, 2=1.5h, etc
                tvSocialValue.setText(formatHours(hours));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarTotal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float hours = 0.5f + (progress * 0.5f);
                tvTotalValue.setText(formatHours(hours));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Save button
        btnSave.setOnClickListener(v -> {
            float socialHours = 0.5f + (seekBarSocial.getProgress() * 0.5f);
            float totalHours = 0.5f + (seekBarTotal.getProgress() * 0.5f);

            prefs.edit()
                    .putFloat("limit_social_hours", socialHours)
                    .putFloat("limit_total_hours", totalHours)
                    .apply();

            updateLimitSubtitles(tvSocialSubtitle, tvTotalSubtitle);

            Toast.makeText(getContext(), "Limits updated! ✅", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // Helper method to format hours
    private String formatHours(float hours) {
        if (hours < 1.0f) {
            return "30 min";
        }

        int wholeHours = (int) hours;
        float remainder = hours - wholeHours;

        if (remainder == 0) {
            return wholeHours + "h";
        } else {
            return wholeHours + "h 30min";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // ✅ Refresh status when fragment becomes visible
        updateBatteryOptimizationStatus();
    }

    // ✅ Check battery optimization status and update UI
    private void updateBatteryOptimizationStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager)
                    requireContext().getSystemService(Context.POWER_SERVICE);
            String packageName = requireContext().getPackageName();

            boolean isOptimizationDisabled = pm != null &&
                    pm.isIgnoringBatteryOptimizations(packageName);

            if (isOptimizationDisabled) {
                // ✅ Tracking is ACTIVE
                tvStatusIcon.setText("🟢");
                tvStatusLabel.setText("Active");
                tvStatusMessage.setText("Collecting usage data automatically");
                btnEnableTracking.setVisibility(View.GONE);
            } else {
                // ❌ Tracking is DISABLED
                tvStatusIcon.setText("🔴");
                tvStatusLabel.setText("Inactive");
                tvStatusMessage.setText("Battery optimization is blocking background tracking");
                btnEnableTracking.setVisibility(View.VISIBLE);
            }
        } else {
            // Pre-Marshmallow — no battery optimization exists
            tvStatusIcon.setText("🟢");
            tvStatusLabel.setText("Active");
            tvStatusMessage.setText("Collecting usage data automatically");
            btnEnableTracking.setVisibility(View.GONE);
        }
    }

    // ✅ Open battery optimization settings
    private void openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse(
                        "package:" + requireContext().getPackageName()));
                startActivity(intent);

                Toast.makeText(getContext(),
                        "Please allow to enable background tracking",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                // Fallback to general battery settings
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        }
    }
}