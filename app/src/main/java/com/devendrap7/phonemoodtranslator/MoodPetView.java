package com.devendrap7.phonemoodtranslator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.Calendar;

public class MoodPetView extends View {

    private Paint paintFace, paintFeatures, paintEars, paintGlasses;
    private int usageMinutes = 0;
    private int appOpens = 0;
    private boolean isLateNight = false; // New Trigger

    // Geometry Paths
    private Path mouthPath = new Path();
    private Path earPath = new Path();

    public MoodPetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintFace = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFace.setStyle(Paint.Style.FILL);

        paintEars = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEars.setStyle(Paint.Style.FILL);

        paintFeatures = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFeatures.setColor(Color.WHITE);
        paintFeatures.setStyle(Paint.Style.FILL);
        paintFeatures.setStrokeCap(Paint.Cap.ROUND);

        // New Paint for Glasses
        paintGlasses = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGlasses.setColor(Color.parseColor("#333333")); // Dark Grey frames
        paintGlasses.setStyle(Paint.Style.STROKE);
        paintGlasses.setStrokeWidth(8f);
    }

    public void setMoodData(int minutes, int opens) {
        this.usageMinutes = minutes;
        this.appOpens = opens;

        // Check for Late Night (Auto-detect if current time is late, or pass it in)
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        this.isLateNight = (hour >= 23 || hour < 5);

        // --- DETERMINE COLOR & MOOD ---

        if (isLateNight) {
            // MODE: ZOMBIE (Late Night)
            paintFace.setColor(Color.parseColor("#7E57C2")); // Deep Purple
            paintEars.setColor(Color.parseColor("#512DA8"));
        }
        else if (minutes > 150 && opens < 15) {
            // MODE: PROFESSOR (Hyperfocused / Work Mode)
            paintFace.setColor(Color.parseColor("#42A5F5")); // Intellect Blue
            paintEars.setColor(Color.parseColor("#1E88E5"));
        }
        else if (minutes > 300) {
            // MODE: OVERDOSE (Sick)
            paintFace.setColor(Color.parseColor("#EF5350")); // Red
            paintEars.setColor(Color.parseColor("#C62828"));
        }
        else if (opens > 50) {
            // MODE: ANXIOUS (High Switching)
            paintFace.setColor(Color.parseColor("#FFCA28")); // Amber
            paintEars.setColor(Color.parseColor("#FF8F00"));
        }
        else {
            // MODE: HAPPY (Balanced)
            paintFace.setColor(Color.parseColor("#66BB6A")); // Green
            paintEars.setColor(Color.parseColor("#2E7D32"));
        }

        invalidate(); // Redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2;
        float cy = h / 2;
        float radius = Math.min(w, h) / 2.8f;

        drawEars(canvas, cx, cy, radius);
        canvas.drawCircle(cx, cy, radius, paintFace); // Face Base

        // Special: Draw Glasses if in Professor Mode
        if (!isLateNight && usageMinutes > 150 && appOpens < 15) {
            drawGlasses(canvas, cx, cy, radius);
        } else {
            drawEyes(canvas, cx, cy, radius);
        }

        // Nose (Always cute)
        paintFeatures.setColor(Color.BLACK);
        paintFeatures.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy + radius * 0.1f, radius * 0.15f, paintFeatures);

        drawMouth(canvas, cx, cy, radius);
    }

    private void drawGlasses(Canvas canvas, float cx, float cy, float r) {
        float eyeOffset = r * 0.35f;
        float eyeY = cy - r * 0.2f;
        float glassRadius = r * 0.25f;

        // 1. Draw White Lenses
        paintFeatures.setColor(Color.WHITE);
        paintFeatures.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx - eyeOffset, eyeY, glassRadius, paintFeatures);
        canvas.drawCircle(cx + eyeOffset, eyeY, glassRadius, paintFeatures);

        // 2. Draw Frames (Rim)
        canvas.drawCircle(cx - eyeOffset, eyeY, glassRadius, paintGlasses);
        canvas.drawCircle(cx + eyeOffset, eyeY, glassRadius, paintGlasses);

        // 3. Draw Bridge
        canvas.drawLine(cx - eyeOffset + glassRadius, eyeY, cx + eyeOffset - glassRadius, eyeY, paintGlasses);

        // 4. Draw Pupils (Small & Smart)
        paintFeatures.setColor(Color.BLACK);
        canvas.drawCircle(cx - eyeOffset, eyeY, 8f, paintFeatures);
        canvas.drawCircle(cx + eyeOffset, eyeY, 8f, paintFeatures);
    }

    private void drawEyes(Canvas canvas, float cx, float cy, float r) {
        float eyeOffset = r * 0.35f;
        float eyeY = cy - r * 0.2f;
        float eyeSize = r * 0.12f;

        paintFeatures.setColor(Color.WHITE);
        paintFeatures.setStyle(Paint.Style.FILL);

        if (isLateNight) {
            // SLEEPY EYES: Just two lines (- -)
            paintFeatures.setColor(Color.parseColor("#311B92")); // Dark Purple Lines
            paintFeatures.setStrokeWidth(12f);
            canvas.drawLine(cx - eyeOffset - 20, eyeY, cx - eyeOffset + 20, eyeY, paintFeatures);
            canvas.drawLine(cx + eyeOffset - 20, eyeY, cx + eyeOffset + 20, eyeY, paintFeatures);
            paintFeatures.setStrokeWidth(0f); // Reset
            return;
        }

        // Standard Eyes Background
        canvas.drawCircle(cx - eyeOffset, eyeY, eyeSize * 2, paintFeatures);
        canvas.drawCircle(cx + eyeOffset, eyeY, eyeSize * 2, paintFeatures);

        // Pupils
        paintFeatures.setColor(Color.BLACK);
        if (usageMinutes > 300) {
            // TIRED X EYES
            paintFeatures.setStrokeWidth(10f);
            canvas.drawLine(cx - eyeOffset - 15, eyeY - 15, cx - eyeOffset + 15, eyeY + 15, paintFeatures);
            canvas.drawLine(cx - eyeOffset + 15, eyeY - 15, cx - eyeOffset - 15, eyeY + 15, paintFeatures);
            canvas.drawLine(cx + eyeOffset - 15, eyeY - 15, cx + eyeOffset + 15, eyeY + 15, paintFeatures);
            canvas.drawLine(cx + eyeOffset + 15, eyeY - 15, cx + eyeOffset - 15, eyeY + 15, paintFeatures);
            paintFeatures.setStrokeWidth(0f);
        } else {
            // HAPPY DOT EYES
            canvas.drawCircle(cx - eyeOffset, eyeY, eyeSize, paintFeatures);
            canvas.drawCircle(cx + eyeOffset, eyeY, eyeSize, paintFeatures);
        }
    }

    private void drawMouth(Canvas canvas, float cx, float cy, float r) {
        mouthPath.reset();
        paintFeatures.setColor(Color.BLACK);
        paintFeatures.setStyle(Paint.Style.STROKE);
        paintFeatures.setStrokeWidth(10f);

        float mouthY = cy + r * 0.3f;
        float mouthW = r * 0.25f;

        if (isLateNight) {
            // Sleepy Drool (Small o)
            canvas.drawCircle(cx, mouthY, 15f, paintFeatures);
        }
        else if (usageMinutes > 300) { // Frown
            mouthPath.moveTo(cx - mouthW, mouthY + 15);
            mouthPath.quadTo(cx, mouthY - 15, cx + mouthW, mouthY + 15);
        }
        else if (usageMinutes > 150 && appOpens < 15) { // Smart Smirk (Small line)
            canvas.drawLine(cx - 20, mouthY, cx + 20, mouthY, paintFeatures);
        }
        else { // Smile
            mouthPath.moveTo(cx - mouthW, mouthY);
            mouthPath.quadTo(cx - mouthW/2, mouthY + 20, cx, mouthY);
            mouthPath.quadTo(cx + mouthW/2, mouthY + 20, cx + mouthW, mouthY);
        }
        canvas.drawPath(mouthPath, paintFeatures); // Draw the path if used
        paintFeatures.setStyle(Paint.Style.FILL);
    }

    // ... (Keep drawEars the same) ...
    private void drawEars(Canvas canvas, float cx, float cy, float r) {
        earPath.reset();
        earPath.moveTo(cx - r * 0.8f, cy - r * 0.5f);
        earPath.lineTo(cx - r * 1.4f, cy - r * 1.2f);
        earPath.lineTo(cx - r * 0.2f, cy - r * 0.9f);
        earPath.moveTo(cx + r * 0.8f, cy - r * 0.5f);
        earPath.lineTo(cx + r * 1.4f, cy - r * 1.2f);
        earPath.lineTo(cx + r * 0.2f, cy - r * 0.9f);
        canvas.drawPath(earPath, paintEars);
    }
}