package com.devendrap7.phonemoodtranslator.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PulseCoreView extends View {
    private Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint spikePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<InnerParticle> particles = new ArrayList<>();
    private List<Ripple> ripples = new ArrayList<>();
    private final int PARTICLE_COUNT = 6;
    private float scanProgress = 0f;
    private float pulsePhase = 0f;
    private Path spikePath = new Path();
    private int coreColor = Color.parseColor("#4A148C");
    private int accentColor = Color.WHITE; // ✅ Dynamic accent color
    private long lastBeat = 0;
    private float beatIntensity = 0f;

    public PulseCoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        for (int i = 0; i < PARTICLE_COUNT; i++) particles.add(new InnerParticle());

        spikePaint.setStyle(Paint.Style.STROKE);
        spikePaint.setStrokeWidth(12f);
        spikePaint.setStrokeCap(Paint.Cap.ROUND);

        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(4f);
    }

    public void setCoreColor(int color) {
        this.coreColor = color;
        invalidate();
    }

    private class InnerParticle {
        float xOffset, yOffset, size, speed, angle;
        int alpha;
        float orbitRadius;

        InnerParticle() {
            reset();
        }

        void reset() {
            angle = (float) (Math.random() * 2 * Math.PI);
            orbitRadius = 0.3f + (float) Math.random() * 0.4f;
            size = 4 + (float) Math.random() * 12;
            speed = 0.003f + (float) Math.random() * 0.007f;
            alpha = 80 + (int) (Math.random() * 175);
        }

        void update() {
            angle += speed;
            if (angle > 2 * Math.PI) angle -= 2 * Math.PI;

            xOffset = (float) (Math.cos(angle) * orbitRadius);
            yOffset = (float) (Math.sin(angle) * orbitRadius);

            size = (4 + (float) Math.random() * 12) * (1 + beatIntensity * 0.3f);
        }
    }

    private class Ripple {
        float radius;
        float maxRadius;
        int alpha;

        Ripple(float max) {
            radius = 0;
            maxRadius = max;
            alpha = 255;
        }

        boolean update() {
            radius += maxRadius * 0.05f;
            alpha -= 12;
            return alpha > 0 && radius < maxRadius;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.65f;

        long time = System.currentTimeMillis();

        float beatCycle = (time % 1200) / 1200f;

        if (beatCycle < 0.15f) {
            beatIntensity = (float) Math.sin(beatCycle / 0.15f * Math.PI);
            if (beatCycle < 0.02f && time - lastBeat > 200) {
                ripples.add(new Ripple(radius * 2f));
                lastBeat = time;
            }
        } else if (beatCycle > 0.2f && beatCycle < 0.35f) {
            float localPhase = (beatCycle - 0.2f) / 0.15f;
            beatIntensity = (float) Math.sin(localPhase * Math.PI) * 0.7f;
            if (beatCycle > 0.2f && beatCycle < 0.22f && time - lastBeat > 200) {
                ripples.add(new Ripple(radius * 1.8f));
                lastBeat = time;
            }
        } else {
            beatIntensity *= 0.95f;
        }

        float scale = 1.0f + beatIntensity * 0.12f;

        canvas.save();
        canvas.scale(scale, scale, cx, cy);
        canvas.restore();

        scanProgress += 0.006f;
        if (scanProgress > 1.5f) scanProgress = -0.3f;

        if (scanProgress >= 0 && scanProgress <= 1.2f) {
            drawAnimatedSpike(canvas, cx, cy, radius * 1.5f);
        }

        invalidate();
    }

    private void drawAnimatedSpike(Canvas canvas, float cx, float cy, float width) {
        spikePath.reset();
        float startX = cx - width / 2;

        spikePath.moveTo(startX, cy);
        spikePath.lineTo(startX + width * 0.15f, cy);
        spikePath.lineTo(startX + width * 0.25f, cy);
        spikePath.lineTo(startX + width * 0.32f, cy - 90);
        spikePath.lineTo(startX + width * 0.38f, cy + 100);
        spikePath.lineTo(startX + width * 0.44f, cy - 50);
        spikePath.lineTo(startX + width * 0.5f, cy + 20);
        spikePath.lineTo(startX + width * 0.56f, cy);
        spikePath.lineTo(startX + width * 0.85f, cy);
        spikePath.lineTo(startX + width, cy);

        android.graphics.PathMeasure pm = new android.graphics.PathMeasure(spikePath, false);
        float pathLength = pm.getLength();

        float tailLength = pathLength * 0.35f;
        float scanPos = scanProgress * pathLength;
        float start = Math.max(0, scanPos - tailLength);
        float end = Math.min(pathLength, scanPos);

        android.graphics.Path partialPath = new android.graphics.Path();
        pm.getSegment(start, end, partialPath, true);

        // ✅ Use dynamic accent color
        Paint fadingSpike = new Paint(spikePaint);
        fadingSpike.setColor(accentColor);
        fadingSpike.setAlpha((int) (255 * Math.min(1, scanProgress)));
        fadingSpike.setShadowLayer(25, 0, 0, accentColor);
        canvas.drawPath(partialPath, fadingSpike);

        if (scanProgress >= 0 && scanProgress <= 1.0f) {
            float[] pos = new float[2];
            pm.getPosTan(end, pos, null);

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(accentColor);
            dotPaint.setShadowLayer(30, 0, 0, accentColor);
            canvas.drawCircle(pos[0], pos[1], 10f, dotPaint);

            dotPaint.setAlpha(120);
            canvas.drawCircle(pos[0], pos[1], 20f, dotPaint);
        }
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public void setupTheme(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        int themeColor = prefs.getInt("bg_color", Color.parseColor("#4A148C"));

        if (isColorDark(themeColor)) {
            this.coreColor = themeColor;
            this.accentColor = Color.WHITE; // ✅ White on dark
        } else {
            this.coreColor = Color.parseColor("#FF1744");
            this.accentColor = Color.parseColor("#1c1554"); // ✅ Dark navy on light
        }

        // ✅ Update paint colors
        spikePaint.setColor(accentColor);
        ripplePaint.setColor(accentColor);

        invalidate();
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private int lightenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.min(1.0f, hsv[2] + 0.35f);
        return Color.HSVToColor(hsv);
    }

    private int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0f, hsv[2] - 0.3f);
        return Color.HSVToColor(hsv);
    }
}