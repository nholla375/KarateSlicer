package com.example.karateslicer.gesture;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.MotionEvent;

import com.example.karateslicer.enemy.Enemy;
import com.example.karateslicer.enemy.ShapeType;
import com.example.karateslicer.state.GameState;
import com.example.karateslicer.ui.GameHUD;
import com.example.karateslicer.ui.ParticleSystem;
import com.example.karateslicer.audio.SoundManager;
import com.example.karateslicer.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures the player's stroke, runs recognition, and resolves combat (spec §5).
 *
 * Call onTouchEvent() from GameView.onTouchEvent() when in PLAYING state.
 * Call draw() to render the live stroke trail.
 */
public class GestureHandler {

    private final GestureRecognizer recognizer = new GestureRecognizer();
    private final List<PointF> currentStroke   = new ArrayList<>();

    private final GameState     gameState;
    private final GameHUD       hud;
    private final ParticleSystem particles;
    private final SoundManager  sound;

    // Ink trail paint
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Gameplay area bounds (gestures only registered inside this region)
    private float gameAreaRight = Float.MAX_VALUE;
    private Enemy primedLightning = null;

    public GestureHandler(GameState gameState, GameHUD hud,
                          ParticleSystem particles, SoundManager sound) {
        this.gameState = gameState;
        this.hud       = hud;
        this.particles = particles;
        this.sound     = sound;

        strokePaint.setColor(0xCCFFFFFF);
        strokePaint.setStrokeWidth(4f);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setGameAreaRight(float right) { this.gameAreaRight = right; }

    // ------------------------------------------------------------------
    // Input (call from GameView on the UI thread — list is only read from
    // the game thread during draw(), so a brief inconsistency is benign for a trail)
    // ------------------------------------------------------------------

    public void onTouchEvent(MotionEvent event, List<Enemy> enemies) {
        float x = event.getX(), y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                currentStroke.clear();
                if (x < gameAreaRight) currentStroke.add(new PointF(x, y));
                break;

            case MotionEvent.ACTION_MOVE:
                if (!currentStroke.isEmpty() && x < gameAreaRight) {
                    currentStroke.add(new PointF(x, y));
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!currentStroke.isEmpty()) {
                    finalize(enemies);
                    currentStroke.clear();
                }
                break;
        }
    }

    private void finalize(List<Enemy> enemies) {
        if (currentStroke.size() < 2) return;

        // Find first enemy the stroke crosses (spec §5.3)
        Enemy target = null;
        for (PointF p : currentStroke) {
            for (Enemy e : enemies) {
                if (e.isAlive() && e.containsPoint(p.x, p.y)) {
                    target = e;
                    break;
                }
            }
            if (target != null) break;
        }

        if (target == null) return; // miss — discard silently

        // Recognize drawn shape
        ShapeType drawn = recognizer.recognize(currentStroke);

        if (target.getShape() == ShapeType.LIGHTNING) {
            if (target.getTier() > gameState.getMaxHittableTier()) {
                target.triggerWrongGestureFeedback();
                return;
            }

            if (primedLightning == target && recognizer.isSwipeDown(currentStroke)) {
                primedLightning = null;
                killTarget(target);
                return;
            }

            if (recognizer.isSwipeUp(currentStroke)) {
                primedLightning = target;
                return;
            }

            primedLightning = null;
            target.triggerWrongGestureFeedback();
            sound.playWrongGesture();
            return;
        }

        primedLightning = null;

        // Spec §6.2 hit result flow
        if (drawn != target.getShape()) {
            target.triggerWrongGestureFeedback();
            sound.playWrongGesture();
            return;
        }

        if (target.getTier() > gameState.getMaxHittableTier()) {
            target.triggerWrongGestureFeedback(); // tier too high flash
            return;
        }

        if (gameState.isPiercingUnlocked()) {
            killPiercedTargets(enemies, drawn);
        } else {
            killTarget(target);
        }
    }

    private void killTarget(Enemy target) {
        target.kill();
        int award = gameState.getKillAwardForTier(target.getTier());
        gameState.addKills(award);
        hud.triggerKillText(award, target.getX(), target.getY());
        particles.emit(target.getX(), target.getY(),
                       tierParticleColor(target.getTier()),
                       Constants.PARTICLES_PER_KILL);
        sound.playKill();
    }

    private void killPiercedTargets(List<Enemy> enemies, ShapeType drawn) {
        boolean killedAny = false;
        for (Enemy e : enemies) {
            if (!e.isAlive()
                    || e.getShape() != drawn
                    || e.getTier() > gameState.getMaxHittableTier()) {
                continue;
            }
            if (strokeTouchesEnemy(e)) {
                killTarget(e);
                killedAny = true;
            }
        }
        if (!killedAny) {
            sound.playWrongGesture();
        }
    }

    private boolean strokeTouchesEnemy(Enemy enemy) {
        for (PointF p : currentStroke) {
            if (enemy.containsPoint(p.x, p.y)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Draw the live stroke trail
    // ------------------------------------------------------------------

    public void draw(Canvas canvas) {
        List<PointF> snap = new ArrayList<>(currentStroke); // snapshot for thread safety
        if (snap.size() < 2) return;

        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(snap.get(0).x, snap.get(0).y);
        for (int i = 1; i < snap.size(); i++) {
            path.lineTo(snap.get(i).x, snap.get(i).y);
        }
        canvas.drawPath(path, strokePaint);
    }

    private int tierParticleColor(int tier) {
        switch (tier) {
            case 2: return 0xFF2196F3;
            case 3: return 0xFFFF9800;
            case 4: return 0xFFF44336;
            case 5: return 0xFF9C27B0;
            default: return 0xFF4CAF50;
        }
    }
}
