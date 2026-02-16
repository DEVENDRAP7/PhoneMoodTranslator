package com.devendrap7.phonemoodtranslator.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // ── Load saved state ──
        etFutureNote.setText(prefs.getString("future_note", ""));
        switchSocialLimit.setChecked(
                prefs.getBoolean("limit_social_enabled", true));
        switchTotalLimit.setChecked(
                prefs.getBoolean("limit_total_enabled", true));

        // ── Load saved theme color ──
        currentColor = prefs.getInt("bg_color",
                Color.parseColor("#4A148C"));
        viewColorPreview.setBackgroundColor(currentColor);

        // ── Save Note ──
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

        // ── Switch listeners ──
        switchSocialLimit.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(
                        "limit_social_enabled", isChecked).apply());

        switchTotalLimit.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(
                        "limit_total_enabled", isChecked).apply());

        // ── Theme Color Picker ──
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
                            // ✅ Save to prefs
                            prefs.edit().putInt("bg_color", color).apply();
                            viewColorPreview.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(currentColor));
                            // ✅ Update preview circle
                            viewColorPreview.setBackgroundColor(color);
                            Toast.makeText(getContext(),
                                    "Theme saved! Restart app to apply.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
            colorPicker.show();
        });
        return view;
    }
}