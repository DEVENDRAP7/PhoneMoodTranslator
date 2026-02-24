package com.devendrap7.phonemoodtranslator.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PulseBackgroundView extends View {
    private int particleColor = Color.parseColor("#4A148C");
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Heartbeat> pulses = new ArrayList<>();
    private Random random = new Random();

    public PulseBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeWidth(3f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#80FFFFFF")); // Semi-transparent white

        // Initialize 15-20 random pulses
        for (int i = 0; i < 20; i++) {
            pulses.add(new Heartbeat());
        }
    }
    // Add this method to the class
    public void setThemeColor(int themeColor) {
        if (isColorDark(themeColor)) {
            this.particleColor = Color.argb(120, 255, 255, 255); // Soft white for dark theme
        } else {
            // Red or Purple for light/soft themes
            this.particleColor = Color.parseColor("#FF1744"); // Vibrant Red
        }
        invalidate();
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Heartbeat h : pulses) {
            h.update(getWidth(), getHeight());
            h.draw(canvas, paint);
        }
        invalidate(); // Keeps the animation running
    }

    private class Heartbeat {
        float x, y, speed, alpha;
        double angle; // ✅ For multi-direction
        Path path = new Path();

        Heartbeat() { reset(); }

        void reset() {
            // Random start position anywhere on screen
            x = random.nextInt(getWidth() > 0 ? getWidth() : 1000);
            y = random.nextInt(getHeight() > 0 ? getHeight() : 2000);

            speed = 1 + random.nextFloat() * 4;
            angle = random.nextDouble() * 2 * Math.PI; // ✅ Random 360-degree direction
            alpha = 80 + random.nextInt(100);

            // Spike shape
            path.reset();
            path.moveTo(-25, 0); path.lineTo(-10, 0);
            path.lineTo(-5, -20); path.lineTo(0, 20);
            path.lineTo(5, 0); path.lineTo(25, 0);
        }

        void update(int w, int h) {
            // ✅ Calculate movement based on angle
            x += Math.cos(angle) * speed;
            y += Math.sin(angle) * speed;

            // Reset if it goes off-screen
            if (x < -50 || x > w + 50 || y < -50 || y > h + 50) {
                reset();
            }
        }

        void draw(Canvas canvas, Paint p) {
            p.setColor(particleColor);
            p.setAlpha((int) alpha);
            canvas.save();
            canvas.translate(x, y);
            // ✅ Rotate the heartbeat to face the direction it's moving
            canvas.rotate((float) Math.toDegrees(angle));
            canvas.drawPath(path, p);
            canvas.restore();
        }
    }
}