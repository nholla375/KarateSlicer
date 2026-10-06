package com.example.karateslicer.enemy.movement;

import com.example.karateslicer.enemy.Enemy;

/** Oscillates vertically while descending. Available from wave 15. */
public class VerticalOscillationMovement implements MovementPattern {

    private static final float FREQ = 0.7f;
    private static final float AMP = 16f;
    private float phase = 0f;

    @Override
    public void update(Enemy enemy, float dt, float gameAreaW) {
        phase += dt * FREQ * (float)(2 * Math.PI);
        float offset = (float) Math.sin(phase) * AMP;
        enemy.setY(enemy.getY() + enemy.getSpeed() * dt + offset * dt);
    }
}
