package com.devendrap7.phonemoodtranslator.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import java.util.Calendar;

public class MoodPetView extends View {

    private Paint paintFace, paintFeatures, paintEars, paintGlasses;
    public int usageMinutes = 0;
    public boolean isLateNight = false;
    private boolean isLoading = false;

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

        paintGlasses = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGlasses.setColor(Color.parseColor("#333333"));
        paintGlasses.setStyle(Paint.Style.STROKE);
        paintGlasses.setStrokeWidth(8f);
    }

    // This is called by your CalendarAdapter for days with no data
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        // Force a redraw immediately
        invalidate();
    }

    public void setMoodData(int minutes) {
        this.isLoading = false;
        this.usageMinutes = minutes;

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        this.isLateNight = (hour >= 23 || hour < 5);

        if (isLateNight) {
            paintFace.setColor(Color.parseColor("#7E57C2"));
            paintEars.setColor(Color.parseColor("#512DA8"));
        } else if (minutes < 120) {
            paintFace.setColor(Color.parseColor("#66BB6A"));
            paintEars.setColor(Color.parseColor("#2E7D32"));
        } else if (minutes < 180) {
            paintFace.setColor(Color.parseColor("#42A5F5"));
            paintEars.setColor(Color.parseColor("#1E88E5"));
        } else if (minutes < 240) {
            paintFace.setColor(Color.parseColor("#FFCA28"));
            paintEars.setColor(Color.parseColor("#FF8F00"));
        } else if (minutes < 300) {
            paintFace.setColor(Color.parseColor("#FB8C00"));
            paintEars.setColor(Color.parseColor("#E65100"));
        } else if (minutes < 420) {
            paintFace.setColor(Color.parseColor("#F4511E"));
            paintEars.setColor(Color.parseColor("#BF360C"));
        } else {
            paintFace.setColor(Color.parseColor("#EF5350"));
            paintEars.setColor(Color.parseColor("#C62828"));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2;
        float cy = h / 2;
        float radius = Math.min(w, h) / 2.8f;
        if (!isLoading) {
            // This creates a soft glow using the same color as the face
            paintFace.setShadowLayer(15, 0, 0, paintFace.getColor());
            setLayerType(LAYER_TYPE_SOFTWARE, paintFace); // Required for ShadowLayer
        } else {
            paintFace.clearShadowLayer();
        }

        // 1. FORCE THE CORRECT COLORS


        // 2. ENFORCE SOLID FILL
        paintFace.setStyle(Paint.Style.FILL);
        paintEars.setStyle(Paint.Style.FILL);

        // 3. DRAW THE BASE BODY
        drawEars(canvas, cx, cy, radius);
        canvas.drawCircle(cx, cy, radius, paintFace);

        // 4. DRAW THE FEATURES
        float eyeOffset = radius * 0.35f;
        float eyeY = cy - radius * 0.2f;

        if (isLoading) {
            drawLoadingFeatures(canvas, cx, cy, radius, eyeOffset, eyeY);
            paintFace.setColor(Color.parseColor("#E0E0E0")); // Light Grey
            paintEars.setColor(Color.parseColor("#BDBDBD"));
        } else {
            drawMoodFeatures(canvas, cx, cy, radius);
            applyMoodColors(); // Sets the color based on usageMinutes
        }
    }

    // --- NEW METHOD 1: MOOD FEATURES ---
    private void drawMoodFeatures(Canvas canvas, float cx, float cy, float radius) {
        // Logic for Glasses vs Eyes
        if (!isLateNight && usageMinutes >= 120 && usageMinutes < 180) {
            drawGlasses(canvas, cx, cy, radius);
        } else {
            drawEyes(canvas, cx, cy, radius);
        }
        // Nose
        paintFeatures.setColor(Color.BLACK);
        paintFeatures.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy + radius * 0.1f, radius * 0.15f, paintFeatures);

        // Mouth
        drawMouth(canvas, cx, cy, radius);
    }

    // --- NEW METHOD 2: LOADING FEATURES ---
    private void drawLoadingFeatures(Canvas canvas, float cx, float cy, float r, float offset, float eyeY) {
        paintFeatures.setColor(Color.parseColor("#757575")); // Soft Grey
        paintFeatures.setStyle(Paint.Style.STROKE);
        paintFeatures.setStrokeWidth(8f);
        paintFeatures.setStrokeCap(Paint.Cap.ROUND);
        drawMoodFeatures(canvas , cx, cy, r);

        // Sleepy Eyes
        canvas.drawLine(cx - offset - 20, eyeY, cx - offset + 20, eyeY, paintFeatures);
        canvas.drawLine(cx + offset - 20, eyeY, cx + offset + 20, eyeY, paintFeatures);

        // Neutral Mouth
        canvas.drawLine(cx - 20, cy + r * 0.3f, cx + 20, cy + r * 0.3f, paintFeatures);
    }

    // --- NEW METHOD 3: MOOD COLOR LOGIC ---
    public void applyMoodColors() {
        if (isLateNight) {
            paintFace.setColor(Color.parseColor("#7E57C2"));
            paintEars.setColor(Color.parseColor("#512DA8"));
        } else if (usageMinutes < 120) {
            paintFace.setColor(Color.parseColor("#66BB6A"));
            paintEars.setColor(Color.parseColor("#2E7D32"));
        } else if (usageMinutes < 180) {
            paintFace.setColor(Color.parseColor("#42A5F5"));
            paintEars.setColor(Color.parseColor("#1E88E5"));
        } else if (usageMinutes < 240) {
            paintFace.setColor(Color.parseColor("#FFCA28"));
            paintEars.setColor(Color.parseColor("#FF8F00"));
        } else if (usageMinutes < 300) {
            paintFace.setColor(Color.parseColor("#FB8C00"));
            paintEars.setColor(Color.parseColor("#E65100"));
        } else if (usageMinutes < 420) {
            paintFace.setColor(Color.parseColor("#F4511E"));
            paintEars.setColor(Color.parseColor("#BF360C"));
        } else {
            paintFace.setColor(Color.parseColor("#EF5350"));
            paintEars.setColor(Color.parseColor("#C62828"));
        }
    }


    private void drawGlasses(Canvas canvas, float cx, float cy, float r) {
        float eyeOffset = r * 0.35f;
        float eyeY = cy - r * 0.2f;
        float glassRadius = r * 0.25f;

        paintFeatures.setColor(Color.WHITE);
        paintFeatures.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx - eyeOffset, eyeY, glassRadius, paintFeatures);
        canvas.drawCircle(cx + eyeOffset, eyeY, glassRadius, paintFeatures);

        canvas.drawCircle(cx - eyeOffset, eyeY, glassRadius, paintGlasses);
        canvas.drawCircle(cx + eyeOffset, eyeY, glassRadius, paintGlasses);
        canvas.drawLine(cx - eyeOffset + glassRadius, eyeY, cx + eyeOffset - glassRadius, eyeY, paintGlasses);

        paintFeatures.setColor(Color.BLACK);
        canvas.drawCircle(cx - eyeOffset, eyeY, 8f, paintFeatures);
        canvas.drawCircle(cx + eyeOffset, eyeY, 8f, paintFeatures);
    }

    private void drawEyes(Canvas canvas, float cx, float cy, float r) {
        float eyeOffset = r * 0.35f;
        float eyeY = cy - r * 0.2f;

        if (isLateNight) {
            drawSleepyEyes(canvas, cx, cy, r, eyeOffset, eyeY);
        } else if (usageMinutes > 420) {
            drawTiredXEyes(canvas, cx, cy, r, eyeOffset, eyeY);
        } else if (usageMinutes > 240) {
            drawConcernedEyes(canvas, cx, cy, r, eyeOffset, eyeY);
        } else {
            paintFeatures.setColor(Color.WHITE);
            paintFeatures.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx - eyeOffset, eyeY, r * 0.24f, paintFeatures);
            canvas.drawCircle(cx + eyeOffset, eyeY, r * 0.24f, paintFeatures);
            paintFeatures.setColor(Color.BLACK);
            canvas.drawCircle(cx - eyeOffset, eyeY, r * 0.12f, paintFeatures);
            canvas.drawCircle(cx + eyeOffset, eyeY, r * 0.12f, paintFeatures);
        }
    }

    private void drawSleepyEyes(Canvas canvas, float cx, float cy, float r, float offset, float eyeY) {
        paintFeatures.setColor(Color.parseColor("#311B92"));
        paintFeatures.setStyle(Paint.Style.STROKE);
        paintFeatures.setStrokeWidth(12f);
        canvas.drawLine(cx - offset - 25, eyeY, cx - offset + 25, eyeY, paintFeatures);
        canvas.drawLine(cx + offset - 25, eyeY, cx + offset + 25, eyeY, paintFeatures);
    }

    private void drawTiredXEyes(Canvas canvas, float cx, float cy, float r, float offset, float eyeY) {
        paintFeatures.setColor(Color.BLACK);
        paintFeatures.setStyle(Paint.Style.STROKE);
        paintFeatures.setStrokeWidth(10f);
        float xSize = 20f;
        canvas.drawLine(cx - offset - xSize, eyeY - xSize, cx - offset + xSize, eyeY + xSize, paintFeatures);
        canvas.drawLine(cx - offset + xSize, eyeY - xSize, cx - offset - xSize, eyeY + xSize, paintFeatures);
        canvas.drawLine(cx + offset - xSize, eyeY - xSize, cx + offset + xSize, eyeY + xSize, paintFeatures);
        canvas.drawLine(cx + offset + xSize, eyeY - xSize, cx + offset - xSize, eyeY + xSize, paintFeatures);
    }

    private void drawConcernedEyes(Canvas canvas, float cx, float cy, float r, float offset, float eyeY) {
        paintFeatures.setColor(Color.WHITE);
        paintFeatures.setStyle(Paint.Style.FILL);
        float eyeSize = r * 0.15f;
        canvas.drawCircle(cx - offset, eyeY, eyeSize, paintFeatures);
        canvas.drawCircle(cx + offset, eyeY, eyeSize, paintFeatures);
        canvas.drawRect(cx - offset - eyeSize, eyeY, cx - offset + eyeSize, eyeY + eyeSize, paintFace);
        canvas.drawRect(cx + offset - eyeSize, eyeY, cx + offset + eyeSize, eyeY + eyeSize, paintFace);
        paintFeatures.setColor(Color.BLACK);
        canvas.drawCircle(cx - offset, eyeY - 5, 8f, paintFeatures);
        canvas.drawCircle(cx + offset, eyeY - 5, 8f, paintFeatures);
    }

    private void drawMouth(Canvas canvas, float cx, float cy, float r) {
        mouthPath.reset();
        paintFeatures.setColor(Color.BLACK);
        paintFeatures.setStyle(Paint.Style.STROKE);
        paintFeatures.setStrokeWidth(10f);
        float mouthY = cy + r * 0.3f;
        float mouthW = r * 0.25f;

        if (usageMinutes < 120) {
            mouthPath.moveTo(cx - mouthW, mouthY);
            mouthPath.quadTo(cx, mouthY + 30, cx + mouthW, mouthY);
        } else if (usageMinutes < 240) {
            canvas.drawLine(cx - 30, mouthY, cx + 30, mouthY, paintFeatures);
        } else if (usageMinutes < 420) {
            canvas.drawCircle(cx, mouthY, 10f, paintFeatures);
        } else {
            mouthPath.moveTo(cx - mouthW, mouthY + 20);
            mouthPath.quadTo(cx, mouthY - 20, cx + mouthW, mouthY + 20);
        }
        canvas.drawPath(mouthPath, paintFeatures);
    }

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
    public Drawable getMascotAsDrawable(MoodPetView view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
    }
}