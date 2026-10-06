package com.example.karateslicer.enemy.movement;

import com.example.karateslicer.enemy.Enemy;

/**
 * Gradually drifts toward the center-X of the gameplay area while descending.
 * Available from Tier 5.
 */
public class HomingMovement implements MovementPattern {

    private static final float DRIFT_SPEED = 60f; // px/s horizontal drift toward center

    @Override
    public void update(Enemy enemy, float dt, float gameAreaW) {
        enemy.setY(enemy.getY() + enemy.getSpeed() * dt);

        float centerX = gameAreaW * 0.5f;
        float dx      = centerX - enemy.getX();
        float step    = DRIFT_SPEED * dt;
        if (Math.abs(dx) < step) {
            enemy.setX(centerX);
        } else {
            enemy.setX(enemy.getX() + Math.signum(dx) * step);
        }
    }
}
