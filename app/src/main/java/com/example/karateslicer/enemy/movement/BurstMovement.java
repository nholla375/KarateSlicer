package com.example.karateslicer.enemy.movement;

import com.example.karateslicer.enemy.Enemy;

/**
 * Moves at normal speed, then surges downward for 0.3s on a 2.5s cycle.
 * Available from Tier 4.
 */
public class BurstMovement implements MovementPattern {

    private static final float CYCLE_S   = 2.5f;
    private static final float SURGE_S   = 0.3f;
    private static final float SURGE_MUL = 2.0f;

    private float cycleTimer = 0f;

    @Override
    public void update(Enemy enemy, float dt, float gameAreaW) {
        cycleTimer += dt;
        if (cycleTimer > CYCLE_S) cycleTimer -= CYCLE_S;

        float speedMul = (cycleTimer >= (CYCLE_S - SURGE_S)) ? SURGE_MUL : 1.0f;
        enemy.setY(enemy.getY() + enemy.getSpeed() * speedMul * dt);
    }
}
