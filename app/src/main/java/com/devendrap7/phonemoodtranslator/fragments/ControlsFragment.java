package com.devendrap7.phonemoodtranslator.fragments;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class ControlsFragment extends Fragment {

    private TextInputEditText etFutureNote;
    private Button btnSaveNote;
    private SwitchMaterial switchSocialLimit;
    private SwitchMaterial switchTotalLimit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);

        etFutureNote = view.findViewById(R.id.etFutureNote);
        btnSaveNote = view.findViewById(R.id.btnSaveNote);
        switchSocialLimit = view.findViewById(R.id.switchSocialLimit);
        switchTotalLimit = view.findViewById(R.id.switchTotalLimit);

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // Load UI state
        etFutureNote.setText(prefs.getString("future_note", ""));
        switchSocialLimit.setChecked(prefs.getBoolean("limit_social_enabled", true));
        switchTotalLimit.setChecked(prefs.getBoolean("limit_total_enabled", true));

        btnSaveNote.setOnClickListener(v -> {
            String note = etFutureNote.getText().toString();
            prefs.edit().putString("future_note", note).apply();

            // Save to Room for the intervention banner
            new Thread(() -> {
                AppDatabase db = AppDatabase.getDatabase(getContext());
                String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

                // 1. Try to update the existing row for today
                int rowsUpdated = db.statsDao().updateNote(todayDate, note);

                // 2. If no row exists yet (fresh day), create one
                if (rowsUpdated == 0) {
                    // 1. Get current date info to satisfy the new V2 requirements
                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH) + 1; // February = 2
                    int currentYear = cal.get(Calendar.YEAR);
                    int currentDay   = cal.get(Calendar.DAY_OF_MONTH); // ✅ ADD THIS
                    long timestamp   = cal.getTimeInMillis();

// 2. Update the constructor to the 10-parameter version
                    DailyStats newDay = new DailyStats(
                            todayDate,      // 1. date
                            currentMonth,   // 2. month
                            currentYear,    // 3. year
                            currentDay,     // 4. ✅ dayOfMonth
                            timestamp,      // 5. ✅ dateTimestamp
                            0,              // 6. totalCount
                            0L,             // 7. totalUsageTime
                            0,              // 8. unlockCount
                            "🧘",           // 9. moodEmoji
                            "Starting Day", // 10. moodTitle
                            "[]",           // 11. topAppsJson
                            note            // 12. selfNote
                    );
                    db.statsDao().insert(newDay);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Note Saved for Intervention!", Toast.LENGTH_SHORT).show();
                        etFutureNote.clearFocus();
                    });
                }
            }).start();
        });

        switchSocialLimit.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("limit_social_enabled", isChecked).apply());

        switchTotalLimit.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("limit_total_enabled", isChecked).apply());

        return view;
    }
}