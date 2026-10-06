package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.karateslicer.util.ScoreManager;

/**
 * Title screen drawn on the SurfaceView (spec §3.2).
 * Drifting enemies are decorative only; no interaction.
 */
public class TitleScreen {

    private RectF screenRect = new RectF();

    private final Paint bgPaint    = new Paint();
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tapPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Tap-to-start blink timer
    private float blinkTimer = 0f;
    private boolean blinkOn  = true;

    private final ScoreManager scoreManager;

    public TitleScreen(ScoreManager scoreManager) {
        this.scoreManager = scoreManager;

        bgPaint.setColor(Color.parseColor("#202630"));

        titlePaint.setColor(Color.parseColor("#F6D06F"));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setFakeBoldText(true);

        subPaint.setColor(Color.WHITE);
        subPaint.setTextAlign(Paint.Align.CENTER);

        infoPaint.setColor(Color.parseColor("#CCCCCC"));
        infoPaint.setTextAlign(Paint.Align.CENTER);

        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextAlign(Paint.Align.CENTER);

        tapPaint.setColor(Color.parseColor("#FFD700"));
        tapPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void layout(RectF screen) {
        this.screenRect.set(screen);
        float w = screen.width();
        titlePaint.setTextSize(w * 0.10f);
        subPaint.setTextSize(w * 0.055f);
        infoPaint.setTextSize(w * 0.038f);
        hintPaint.setTextSize(w * 0.046f);
        tapPaint.setTextSize(w * 0.065f);
    }

    public void update(float dt) {
        blinkTimer += dt;
        if (blinkTimer > 0.6f) { blinkTimer = 0f; blinkOn = !blinkOn; }
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(screenRect, bgPaint);

        float cx = screenRect.centerX();
        float cy = screenRect.centerY();
        float h  = screenRect.height();

        canvas.drawText("KARATE SLICER", cx, cy - h * 0.22f, titlePaint);
        canvas.drawText("Use unique gestures to defeat different enemies", cx, cy - h * 0.14f, infoPaint);
        canvas.drawText("and purchase upgrades using acquired kills.", cx, cy - h * 0.10f, infoPaint);
        canvas.drawText("Highest Wave: " + scoreManager.getHighWave(), cx, cy - h * 0.04f, subPaint);
        canvas.drawText("Max Kills: " + scoreManager.getHighScore(), cx, cy + h * 0.00f, subPaint);
        canvas.drawText("Swipe Right -> Circle", cx, cy + h * 0.07f, hintPaint);
        canvas.drawText("Draw a Loop -> Square", cx, cy + h * 0.12f, hintPaint);
        canvas.drawText("Swipe Down -> Triangle", cx, cy + h * 0.17f, hintPaint);
        canvas.drawText("Swipe Left -> Star", cx, cy + h * 0.22f, hintPaint);
        canvas.drawText("Up, then Down -> Lightning", cx, cy + h * 0.27f, hintPaint);
        if (blinkOn) canvas.drawText("TAP TO START", cx, cy + h * 0.38f, tapPaint);
    }
}
