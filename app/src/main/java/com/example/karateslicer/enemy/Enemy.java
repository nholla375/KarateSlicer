package com.example.karateslicer.enemy;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.example.karateslicer.enemy.movement.MovementPattern;
import com.example.karateslicer.util.Constants;

/**
 * A single enemy on screen (spec §4.4).
 *
 * x, y = center of the enemy.
 * size = radius (circles) or half-width (polygons).
 * Enemies descend from the top; they escape when y > gameAreaBottom.
 */
public class Enemy {

    // Tier color table (spec §4.3)
    private static final int[] TIER_COLORS = {
            0,
            Color.parseColor("#4CAF50"),  // Tier 1 — green
            Color.parseColor("#2196F3"),  // Tier 2 — blue
            Color.parseColor("#FF9800"),  // Tier 3 — orange
            Color.parseColor("#F44336"),  // Tier 4 — red
            Color.parseColor("#9C27B0"),  // Tier 5 — purple
    };

    // ---- Identity ----
    private final int tier;
    private final ShapeType shape;

    // ---- Position ----
    private float x, y;
    private final float size;   // radius / half-width in px

    // ---- Movement ----
    private final float speed;              // px/s downward
    private final MovementPattern movement;
    private float speedMultiplier = 1.0f;   // modified by Time Slow ability

    // ---- State ----
    private boolean alive   = true;
    private boolean escaped = false;

    // ---- Hit feedback ----
    private int hitFlashTimer = 0;   // counts down in frames
    private float shakeOffsetX = 0f;

    // ---- Drawing tools ----
    private final Paint bodyPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint facePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint faceLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  shapePath   = new Path();
    private final RectF mouthRect   = new RectF();

    public Enemy(float x, float y, float size, float speed, int tier,
                 ShapeType shape, MovementPattern movement) {
        this.x        = x;
        this.y        = y;
        this.size     = size;
        this.speed    = speed;
        this.tier     = tier;
        this.shape    = shape;
        this.movement = movement;

        int color = (tier >= 1 && tier < TIER_COLORS.length) ? TIER_COLORS[tier] : TIER_COLORS[1];
        bodyPaint.setColor(color);
        bodyPaint.setStyle(Paint.Style.FILL);

        facePaint.setColor(Color.BLACK);
        facePaint.setStyle(Paint.Style.FILL);

        faceLinePaint.setColor(Color.BLACK);
        faceLinePaint.setStyle(Paint.Style.STROKE);
        faceLinePaint.setStrokeCap(Paint.Cap.ROUND);

        outlinePaint.setColor(Color.BLACK);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3f);
    }

    // ==============================================================
    // Update
    // ==============================================================

    /**
     * @param dt              seconds since last frame
     * @param gameAreaW       gameplay area width (for movement pattern bounds)
     * @param gameAreaBottom  y at which the enemy has escaped
     * @param frozen          true when Freeze ability is active
     */
    public void update(float dt, float gameAreaW, float gameAreaBottom, boolean frozen) {
        if (!alive) return;
        if (!frozen) {
            movement.update(this, dt * speedMultiplier, gameAreaW);
        }

        // Tick hit flash
        if (hitFlashTimer > 0) {
            hitFlashTimer--;
            shakeOffsetX = (hitFlashTimer % 2 == 0) ? Constants.WRONG_GESTURE_SHAKE_PX
                                                     : -Constants.WRONG_GESTURE_SHAKE_PX;
        } else {
            shakeOffsetX = 0f;
        }

        // Escape check
        if (y - size > gameAreaBottom) {
            alive   = false;
            escaped = true;
        }
    }

    // ==============================================================
    // Draw
    // ==============================================================

    public void draw(Canvas canvas) {
        if (!alive) return;

        float drawX = x + shakeOffsetX;

        // Flash: alternate between body color and white
        if (hitFlashTimer > 0 && hitFlashTimer % 2 == 0) {
            bodyPaint.setColor(Color.WHITE);
        } else {
            int color = (tier >= 1 && tier < TIER_COLORS.length) ? TIER_COLORS[tier] : TIER_COLORS[1];
            bodyPaint.setColor(color);
        }

        drawShape(canvas, drawX, y, size, bodyPaint, true);
        drawFace(canvas, drawX, y, size);
    }

    private void drawShape(Canvas canvas, float cx, float cy, float r,
                           Paint paint, boolean filled) {
        shapePath.reset();
        switch (shape) {
            case CIRCLE:
                canvas.drawCircle(cx, cy, r, paint);
                break;
            case SQUARE:
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint);
                break;
            case TRIANGLE:
                shapePath.moveTo(cx,       cy - r);
                shapePath.lineTo(cx + r,   cy + r);
                shapePath.lineTo(cx - r,   cy + r);
                shapePath.close();
                canvas.drawPath(shapePath, paint);
                break;
            case STAR:
                drawStar(canvas, cx, cy, r, paint);
                break;
            case LIGHTNING:
                drawLightning(canvas, cx, cy, r, paint);
                break;
        }
    }

    private void drawStar(Canvas canvas, float cx, float cy, float R, Paint paint) {
        float innerR = R * 0.42f;
        shapePath.reset();
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * i / 5.0 - Math.PI / 2.0;
            float r      = (i % 2 == 0) ? R : innerR;
            float px     = cx + r * (float) Math.cos(angle);
            float py     = cy + r * (float) Math.sin(angle);
            if (i == 0) shapePath.moveTo(px, py);
            else        shapePath.lineTo(px, py);
        }
        shapePath.close();
        canvas.drawPath(shapePath, paint);
    }

    private void drawLightning(Canvas canvas, float cx, float cy, float R, Paint paint) {
        // Classic lightning bolt: wide at top-right, narrows to a tip at bottom-left
        shapePath.reset();
        shapePath.moveTo(cx + R * 0.35f, cy - R);          // top-right
        shapePath.lineTo(cx - R * 0.05f, cy - R * 0.05f);  // mid-left
        shapePath.lineTo(cx + R * 0.30f, cy - R * 0.05f);  // mid-right notch
        shapePath.lineTo(cx - R * 0.35f, cy + R);           // bottom-left tip
        shapePath.lineTo(cx + R * 0.05f, cy + R * 0.05f);  // lower-mid-right
        shapePath.lineTo(cx - R * 0.30f, cy + R * 0.05f);  // lower-mid-left notch
        shapePath.close();
        canvas.drawPath(shapePath, paint);
    }

    private void drawFace(Canvas canvas, float cx, float cy, float r) {
        float scale = 0.58f;
        if (shape == ShapeType.TRIANGLE) scale = 0.46f;
        if (shape == ShapeType.STAR) scale = 0.40f;
        if (shape == ShapeType.LIGHTNING) scale = 0.42f;

        float fr = r * scale;
        float eyeR = Math.max(2f, fr * 0.12f);
        float eyeY = cy - fr * 0.10f;
        float leftEyeX = cx - fr * 0.34f;
        float rightEyeX = cx + fr * 0.34f;

        facePaint.setColor(Color.BLACK);
        canvas.drawCircle(leftEyeX, eyeY, eyeR, facePaint);
        canvas.drawCircle(rightEyeX, eyeY, eyeR, facePaint);

        faceLinePaint.setColor(Color.BLACK);
        faceLinePaint.setStrokeWidth(Math.max(2f, fr * 0.10f));
        canvas.drawLine(leftEyeX - fr * 0.20f, eyeY - fr * 0.26f,
                leftEyeX + fr * 0.18f, eyeY - fr * 0.10f, faceLinePaint);
        canvas.drawLine(rightEyeX - fr * 0.18f, eyeY - fr * 0.10f,
                rightEyeX + fr * 0.20f, eyeY - fr * 0.26f, faceLinePaint);

        faceLinePaint.setStrokeWidth(Math.max(2f, fr * 0.09f));
        mouthRect.set(cx - fr * 0.34f, cy + fr * 0.10f,
                cx + fr * 0.34f, cy + fr * 0.56f);
        canvas.drawArc(mouthRect, 205f, 130f, false, faceLinePaint);
    }

    // ==============================================================
    // Hit feedback
    // ==============================================================

    /** Called when the player draws the wrong shape on this enemy. */
    public void triggerWrongGestureFeedback() {
        hitFlashTimer = Constants.WRONG_GESTURE_FLASH_FRAMES;
    }

    // ==============================================================
    // Setters (used by MovementPattern implementations)
    // ==============================================================

    public void setX(float x)                      { this.x = x; }
    public void setY(float y)                       { this.y = y; }
    public void setSpeedMultiplier(float mul)       { this.speedMultiplier = mul; }

    // ==============================================================
    // Getters
    // ==============================================================

    public float getX()       { return x; }
    public float getY()       { return y; }
    public float getSize()    { return size; }
    public float getSpeed()   { return Math.min(speed, Constants.ENEMY_BASE_SPEED_MAX); }
    public int   getTier()    { return tier; }
    public ShapeType getShape() { return shape; }
    public boolean isAlive()  { return alive; }
    public boolean hasEscaped() { return escaped; }

    public void kill()        { alive = false; }

    /** True if the given point is within the enemy's bounding circle. */
    public boolean containsPoint(float px, float py) {
        float dx = px - x;
        float dy = py - y;
        return dx * dx + dy * dy <= size * size;
    }
}
