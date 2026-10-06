package com.example.karateslicer.enemy.movement;

import com.example.karateslicer.enemy.Enemy;

/** Moves straight downward at constant speed. Available from Tier 1. */
public class StraightMovement implements MovementPattern {

    @Override
    public void update(Enemy enemy, float dt, float gameAreaW) {
        enemy.setY(enemy.getY() + enemy.getSpeed() * dt);
    }
}
