package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.karateslicer.state.GameState;
import com.example.karateslicer.util.Constants;

/**
 * Draws the gameplay area's top bar: kill count, wave timer, lives, and escape flash.
 * Sub-layout per spec §2 — top bar is ~8% of gameplay height.
 */
public class GameHUD {

    private final GameState gameState;

    private RectF topBarRect = new RectF();
    private float killsTextY;
    private float livesTextY;
    private float centerTextY;
    private float mainTextSize;
    private float rightTextInset;

    private final Paint bgPaint    = new Paint();
    private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint();
    private final Paint borderPaint = new Paint();

    // Escape flash
    private float flashTimer = 0f;
    private static final float FLASH_DURATION = 0.4f;
    private String penaltyText = "";

    // Floating kill/penalty texts
    private final java.util.List<FloatingText> floatingTexts = new java.util.ArrayList<>();

    public GameHUD(GameState gameState) {
        this.gameState = gameState;

        bgPaint.setColor(Color.parseColor("#202630"));

        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(true);

        flashPaint.setColor(Color.parseColor("#88FF0000"));
        borderPaint.setColor(Color.parseColor("#F6D06F"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
    }

    public void layout(RectF gameAreaRect) {
        float barH = gameAreaRect.height() * Constants.HUD_TOP_BAR_RATIO;
        topBarRect.set(gameAreaRect.left, gameAreaRect.top,
                       gameAreaRect.right, gameAreaRect.top + barH);
        mainTextSize = barH * 0.32f;
        killsTextY = topBarRect.top + barH * 0.62f;
        livesTextY = killsTextY;
        centerTextY = killsTextY;
    }

    public void update(float dt) {
        if (flashTimer > 0f) flashTimer -= dt;

        // Tick floating texts
        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.update(dt);
            if (ft.isDone()) floatingTexts.remove(i);
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(topBarRect, bgPaint);
        canvas.drawRect(topBarRect, borderPaint);

        float cx = topBarRect.centerX();
        float pad = 12f;
        float safeRight = topBarRect.right - rightTextInset - pad;
        float third = topBarRect.width() / 3f;

        textPaint.setTextAlign(Paint.Align.LEFT);
        drawFittedText(canvas, "Kills: " + gameState.getKillCount(),
                topBarRect.left + pad, killsTextY, third - pad * 2f, Paint.Align.LEFT);

        textPaint.setTextAlign(Paint.Align.CENTER);
        int sec = (int) Math.ceil(gameState.getWaveTimer());
        drawFittedText(canvas, "Wave " + gameState.getWaveNumber() + "  " + sec + "s",
                cx, centerTextY, third - pad * 2f, Paint.Align.CENTER);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        drawFittedText(canvas, "Lives: " + gameState.getLives() + "/" + gameState.getMaxLives(),
                safeRight, livesTextY, Math.max(40f, safeRight - (topBarRect.left + third * 2f + pad)),
                Paint.Align.RIGHT);

        // Red flash on escape
        if (flashTimer > 0f) {
            canvas.drawRect(topBarRect, flashPaint);
        }

        // Floating texts
        for (FloatingText ft : floatingTexts) ft.draw(canvas);
    }

    /** Flash the top bar and show a penalty label when an enemy escapes. */
    public void triggerEscapeFlash(int deducted, float x, float bottomY) {
        flashTimer = FLASH_DURATION;
        if (deducted > 0) {
            floatingTexts.add(new FloatingText("-" + deducted, x, bottomY,
                    Color.RED, 30));
        }
    }

    /** Show a "+N KILL" floating text above the killed enemy position. */
    public void triggerKillText(int amount, float x, float y) {
        floatingTexts.add(new FloatingText("+" + amount + " KILL",
                x, y, Color.parseColor("#FFD700"), 30));
    }

    public void setRightTextInset(float inset) {
        rightTextInset = inset;
    }

    private void drawFittedText(Canvas canvas, String text, float x, float y,
                                float maxWidth, Paint.Align align) {
        textPaint.setTextAlign(align);
        textPaint.setTextSize(mainTextSize);
        while (textPaint.measureText(text) > maxWidth && textPaint.getTextSize() > 12f) {
            textPaint.setTextSize(textPaint.getTextSize() - 1f);
        }
        canvas.drawText(text, x, y, textPaint);
    }
}
