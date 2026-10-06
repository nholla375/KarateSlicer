package com.example.karateslicer.enemy;

import com.example.karateslicer.enemy.movement.BurstMovement;
import com.example.karateslicer.enemy.movement.MovementPattern;
import com.example.karateslicer.enemy.movement.StraightMovement;
import com.example.karateslicer.enemy.movement.VerticalOscillationMovement;
import com.example.karateslicer.enemy.movement.ZigzagMovement;
import com.example.karateslicer.state.GameState;
import com.example.karateslicer.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Decides when and what to spawn (spec §4.1).
 * Spawn timing is driven by GameState.getSpawnIntervalMs().
 */
public class EnemySpawner {

    private float spawnAccumulatorMs = 0f;
    private final Random rng = new Random();

    private float gameAreaW = 0f;
    private float gameAreaH = 0f;
    private float enemySize = 0f;

    private final GameState gameState;

    public EnemySpawner(GameState gameState) {
        this.gameState = gameState;
    }

    public void layout(float gameAreaW, float gameAreaH) {
        this.gameAreaW  = gameAreaW;
        this.gameAreaH  = gameAreaH;
        this.enemySize  = gameAreaW * Constants.ENEMY_SIZE_RATIO;
    }

    public void update(float dt, List<Enemy> enemies) {
        if (!gameState.isGameRunning()) return;

        spawnAccumulatorMs += dt * 1000f;

        while (spawnAccumulatorMs >= gameState.getSpawnIntervalMs()
                && enemies.size() < gameState.getMaxEnemies()) {
            spawnAccumulatorMs -= gameState.getSpawnIntervalMs();
            enemies.add(createEnemy(enemies));
        }
    }

    private Enemy createEnemy(List<Enemy> existing) {
        int wave = gameState.getWaveNumber();
        int tier = pickTier(wave);
        ShapeType shape = shapeForTier(tier);
        float speed = gameState.getEnemyBaseSpeed()
                + (rng.nextFloat() - 0.5f) * 20f; // ±10 px/s variance

        // Pick X with minimum gap from existing enemies (spec §4.1)
        float x = pickSpawnX(existing);
        float y = gameAreaH * Constants.HUD_TOP_BAR_RATIO + enemySize;

        MovementPattern movement = pickMovement(tier, wave);
        return new Enemy(x, y, enemySize, speed, tier, shape, movement);
    }

    private float pickSpawnX(List<Enemy> existing) {
        float minGap  = enemySize * 2.5f;

        float edgeMargin = 150f; // NEW
        float padding = enemySize + edgeMargin; // MODIFIED

        float rangeW  = gameAreaW - padding * 3;

        float bestCandidate = gameAreaW * 0.5f;
        float bestGap = -1f;

        for (int attempt = 0; attempt < 20; attempt++) {
            float candidate = padding + rng.nextFloat() * rangeW;

            boolean ok = true;
            float nearestGap = Float.MAX_VALUE;

            for (Enemy e : existing) {
                float gap = Math.abs(e.getX() - candidate);
                nearestGap = Math.min(nearestGap, gap);
                if (gap < minGap) ok = false;
            }

            if (ok) return candidate;

            if (nearestGap > bestGap) {
                bestGap = nearestGap;
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    /** Pick tier based on wave number and spec §7.2 unlock thresholds. */
    private int pickTier(int wave) {
        int maxTier = 2; // Tiers 1-2 always available
        if (wave >= 5  && gameState.getMaxHittableTier() >= 3) maxTier = 3;
        if (wave >= 10 && gameState.getMaxHittableTier() >= 4) maxTier = 4;
        if (wave >= 15 && gameState.getMaxHittableTier() >= 5) maxTier = 5;

        if (maxTier == 2) return rng.nextBoolean() ? 1 : 2;

        // Weight lower tiers more heavily
        float roll = rng.nextFloat();
        if (maxTier >= 5 && roll < 0.20f) return 5;
        if (maxTier >= 4 && roll < 0.35f) return 4;
        if (maxTier >= 3 && roll < 0.55f) return 3;
        return rng.nextBoolean() ? 1 : 2;
    }

    private ShapeType shapeForTier(int tier) {
        switch (tier) {
            case 2: return ShapeType.SQUARE;
            case 3: return ShapeType.TRIANGLE;
            case 4: return ShapeType.STAR;
            case 5: return ShapeType.LIGHTNING;
            default: return ShapeType.CIRCLE;
        }
    }

    /** Pick one movement pattern from wave-unlocked behaviors. */
    private MovementPattern pickMovement(int tier, int wave) {
        List<Integer> behaviors = new ArrayList<>();
        if (wave >= 5) behaviors.add(1);  // horizontal
        if (wave >= 10) behaviors.add(2); // dash
        if (wave >= 15) behaviors.add(3); // vertical

        if (behaviors.isEmpty()) return new StraightMovement();

        int choice = behaviors.get(rng.nextInt(behaviors.size()));
        switch (choice) {
            case 1: return new ZigzagMovement();
            case 2: return new BurstMovement();
            case 3: return new VerticalOscillationMovement();
            default: return new StraightMovement();
        }
    }
}
