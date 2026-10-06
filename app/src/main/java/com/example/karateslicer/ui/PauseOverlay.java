package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Semi-transparent overlay drawn over the gameplay area when paused (spec §3.4).
 */
public class PauseOverlay {

    private RectF gameAreaRect = new RectF();

    private final Paint dimPaint    = new Paint();
    private final Paint titlePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint btnBgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint();
    private final Paint btnTxtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF resumeRect = new RectF();
    private RectF quitRect   = new RectF();

    public PauseOverlay() {
        dimPaint.setColor(Color.parseColor("#BB000000"));
        titlePaint.setColor(Color.parseColor("#F6D06F"));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setFakeBoldText(true);
        btnBgPaint.setColor(Color.parseColor("#8F2F2F"));
        borderPaint.setColor(Color.parseColor("#F6D06F"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        btnTxtPaint.setColor(Color.WHITE);
        btnTxtPaint.setTextAlign(Paint.Align.CENTER);
        btnTxtPaint.setFakeBoldText(true);
    }

    public void layout(RectF gameArea) {
        this.gameAreaRect.set(gameArea);
        float cx = gameArea.centerX();
        float cy = gameArea.centerY();
        float bw = gameArea.width() * 0.45f;
        float bh = gameArea.height() * 0.07f;
        titlePaint.setTextSize(gameArea.width() * 0.10f);
        btnTxtPaint.setTextSize(bh * 0.50f);
        resumeRect.set(cx - bw/2f, cy + gameArea.height()*0.05f, cx + bw/2f, cy + gameArea.height()*0.05f + bh);
        quitRect.set(cx - bw/2f, resumeRect.bottom + bh*0.4f, cx + bw/2f, resumeRect.bottom + bh*1.4f);
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(gameAreaRect, dimPaint);
        canvas.drawText("PAUSED", gameAreaRect.centerX(),
                gameAreaRect.centerY() - gameAreaRect.height() * 0.05f, titlePaint);
        canvas.drawRect(resumeRect, btnBgPaint);
        canvas.drawRect(resumeRect, borderPaint);
        canvas.drawText("RESUME", resumeRect.centerX(), resumeRect.centerY() + resumeRect.height()*0.35f, btnTxtPaint);
        canvas.drawRect(quitRect, btnBgPaint);
        canvas.drawRect(quitRect, borderPaint);
        canvas.drawText("QUIT TO TITLE", quitRect.centerX(), quitRect.centerY() + quitRect.height()*0.35f, btnTxtPaint);
    }

    public boolean tapResume(float x, float y) { return resumeRect.contains(x, y); }
    public boolean tapQuit(float x, float y)   { return quitRect.contains(x, y); }
}
