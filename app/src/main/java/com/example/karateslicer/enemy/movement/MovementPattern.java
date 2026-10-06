package com.example.karateslicer.enemy.movement;

import com.example.karateslicer.enemy.Enemy;

/** Strategy interface for enemy movement behavior (spec §4.2). */
public interface MovementPattern {
    /**
     * @param enemy       the enemy to reposition
     * @param dt          seconds since last frame
     * @param gameAreaW   gameplay area width in pixels (for bounds clamping)
     */
    void update(Enemy enemy, float dt, float gameAreaW);
}
