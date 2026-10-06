package com.example.karateslicer.enemy.movement;

import com.example.karateslicer.enemy.Enemy;

/** Oscillates horizontally while descending. Available from Tier 3. */
public class ZigzagMovement implements MovementPattern {

    private float phase = 0f;
    private float baseX = Float.NaN;
    private static final float FREQ = 0.6f;  // oscillations per second
    private static final float AMP  = 0.06f; // fraction of game area width

    @Override
    public void update(Enemy enemy, float dt, float gameAreaW) {
        enemy.setY(enemy.getY() + enemy.getSpeed() * dt);
        if (Float.isNaN(baseX)) baseX = enemy.getX();

        phase += dt * FREQ * (float)(2 * Math.PI);
        float offset = (float) Math.sin(phase) * gameAreaW * AMP;
        enemy.setX(baseX + offset);
    }
}
