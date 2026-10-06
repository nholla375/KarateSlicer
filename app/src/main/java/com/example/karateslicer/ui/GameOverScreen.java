package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.karateslicer.state.GameState;
import com.example.karateslicer.util.ScoreManager;

/** Game Over overlay drawn over the gameplay area (spec §3.6). */
public class GameOverScreen {

    private final GameState    gameState;
    private final ScoreManager scoreManager;

    private RectF gameAreaRect = new RectF();

    private final Paint dimPaint       = new Paint();
    private final Paint titlePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint newRecordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint btnBgPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint    = new Paint();
    private final Paint btnTxtPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF playAgainRect = new RectF();
    private RectF menuRect      = new RectF();

    private boolean newHighScore = false;

    public GameOverScreen(GameState gameState, ScoreManager scoreManager) {
        this.gameState    = gameState;
        this.scoreManager = scoreManager;

        dimPaint.setColor(Color.parseColor("#CC000000"));
        titlePaint.setColor(Color.parseColor("#F6D06F"));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setFakeBoldText(true);
        statPaint.setColor(Color.WHITE);
        statPaint.setTextAlign(Paint.Align.CENTER);
        newRecordPaint.setColor(Color.parseColor("#FFD700"));
        newRecordPaint.setTextAlign(Paint.Align.CENTER);
        newRecordPaint.setFakeBoldText(true);
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
        float h  = gameArea.height();
        float bw = gameArea.width() * 0.45f;
        float bh = h * 0.07f;
        titlePaint.setTextSize(gameArea.width() * 0.12f);
        statPaint.setTextSize(gameArea.width() * 0.07f);
        newRecordPaint.setTextSize(gameArea.width() * 0.065f);
        btnTxtPaint.setTextSize(bh * 0.55f);

        float btnY = gameArea.top + h * 0.70f;
        playAgainRect.set(cx - bw/2f, btnY, cx + bw/2f, btnY + bh);
        menuRect.set(cx - bw/2f, btnY + bh * 1.4f, cx + bw/2f, btnY + bh * 2.4f);
    }

    /** Call when entering GAME_OVER state to record the score. */
    public void onEnter() {
        newHighScore = scoreManager.submitScore(
                gameState.getKillCount(), gameState.getWaveNumber());
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(gameAreaRect, dimPaint);

        float cx = gameAreaRect.centerX();
        float h  = gameAreaRect.height();

        canvas.drawText("GAME OVER", cx, gameAreaRect.top + h * 0.22f, titlePaint);
        canvas.drawText("Kills: " + gameState.getKillCount(),
                cx, gameAreaRect.top + h * 0.38f, statPaint);
        canvas.drawText("Wave: " + gameState.getWaveNumber(),
                cx, gameAreaRect.top + h * 0.47f, statPaint);

        if (newHighScore) {
            canvas.drawText("★ NEW HIGH SCORE ★",
                    cx, gameAreaRect.top + h * 0.58f, newRecordPaint);
        }

        canvas.drawRect(playAgainRect, btnBgPaint);
        canvas.drawRect(playAgainRect, borderPaint);
        canvas.drawText("PLAY AGAIN", playAgainRect.centerX(),
                playAgainRect.centerY() + playAgainRect.height()*0.35f, btnTxtPaint);

        canvas.drawRect(menuRect, btnBgPaint);
        canvas.drawRect(menuRect, borderPaint);
        canvas.drawText("MAIN MENU", menuRect.centerX(),
                menuRect.centerY() + menuRect.height()*0.35f, btnTxtPaint);
    }

    public boolean tapPlayAgain(float x, float y) { return playAgainRect.contains(x, y); }
    public boolean tapMainMenu(float x, float y)  { return menuRect.contains(x, y); }
}
