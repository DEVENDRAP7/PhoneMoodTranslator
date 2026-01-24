package com.devendrap7.phonemoodtranslator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    Button back;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView tvHistory = findViewById(R.id.tvHistory);
        Button back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        SharedPreferences prefs =
                getSharedPreferences("mood_history", MODE_PRIVATE);

        Map<String, ?> all = prefs.getAll();

        if (all.isEmpty()) {
            tvHistory.setText("No history yet.\nReflect again tomorrow 🌙");
            return;
        }

        StringBuilder builder = new StringBuilder();

        for (String date : all.keySet()) {
            String value = all.get(date).toString();
            String[] parts = value.split("\\|");

            builder.append(date)
                    .append("\n")
                    .append(parts[0]).append(" ").append(parts[1])
                    .append("\n\n");
        }

        tvHistory.setText(builder.toString());
    }
}
