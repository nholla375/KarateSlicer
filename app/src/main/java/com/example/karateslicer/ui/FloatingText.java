package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

/** A piece of text that floats upward and fades out over N frames. */
public class FloatingText {

    private final String text;
    private float x, y;
    private final int lifetimeFrames;
    private int frame = 0;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public FloatingText(String text, float x, float y, int color, int lifetimeFrames) {
        this.text          = text;
        this.x             = x;
        this.y             = y;
        this.lifetimeFrames = lifetimeFrames;
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(36f);
        paint.setFakeBoldText(true);
    }

    public void update(float dt) {
        frame++;
        y -= 60f * dt; // rise at 60px/s
    }

    public void draw(Canvas canvas) {
        float alpha = 1f - (float) frame / lifetimeFrames;
        paint.setAlpha((int)(alpha * 255));
        canvas.drawText(text, x, y, paint);
    }

    public boolean isDone() { return frame >= lifetimeFrames; }
}
