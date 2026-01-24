package com.devendrap7.phonemoodtranslator;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.devendrap7.phonemoodtranslator.R;

public class DisclaimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);

        Button btnAgree = findViewById(R.id.btnAgree);

        btnAgree.setOnClickListener(v -> {
            Intent intent = new Intent(DisclaimerActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
