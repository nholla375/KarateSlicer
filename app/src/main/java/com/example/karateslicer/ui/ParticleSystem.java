package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.example.karateslicer.util.Constants;

/**
 * Pre-allocated pool of particles used for kill bursts (spec §5.5).
 * Avoids per-frame allocations to keep GC pauses off the game thread.
 */
public class ParticleSystem {

    private static final int   POOL_SIZE   = Constants.PARTICLE_POOL_SIZE;
    private static final int   LIFETIME    = Constants.PARTICLE_LIFETIME_FRAMES;
    private static final float SPEED       = 200f; // px/s

    private final float[] px      = new float[POOL_SIZE];
    private final float[] py      = new float[POOL_SIZE];
    private final float[] vx      = new float[POOL_SIZE];
    private final float[] vy      = new float[POOL_SIZE];
    private final int[]   color   = new int[POOL_SIZE];
    private final int[]   life    = new int[POOL_SIZE]; // frames remaining; 0 = inactive

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ParticleSystem() {
        // life[i] = 0 means inactive — no extra init needed
    }

    /**
     * Activate N particles at (x, y) flying in random directions.
     * Wraps around the pool if needed.
     */
    public void emit(float x, float y, int particleColor, int count) {
        int emitted = 0;
        for (int i = 0; i < POOL_SIZE && emitted < count; i++) {
            if (life[i] == 0) {
                px[i]    = x;
                py[i]    = y;
                double angle = Math.random() * 2 * Math.PI;
                float  spd   = SPEED * (0.5f + (float) Math.random() * 0.5f);
                vx[i]   = spd * (float) Math.cos(angle);
                vy[i]   = spd * (float) Math.sin(angle);
                color[i]= particleColor;
                life[i] = LIFETIME;
                emitted++;
            }
        }
    }

    public void update(float dt) {
        for (int i = 0; i < POOL_SIZE; i++) {
            if (life[i] > 0) {
                px[i] += vx[i] * dt;
                py[i] += vy[i] * dt;
                vy[i] += 300f * dt; // gravity
                life[i]--;
            }
        }
    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < POOL_SIZE; i++) {
            if (life[i] > 0) {
                float alpha = (float) life[i] / LIFETIME;
                paint.setColor(color[i]);
                paint.setAlpha((int)(alpha * 255));
                canvas.drawRect(px[i] - 5f, py[i] - 5f, px[i] + 5f, py[i] + 5f, paint);
            }
        }
    }
}
