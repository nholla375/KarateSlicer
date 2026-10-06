package com.example.karateslicer.state;

import com.example.karateslicer.util.Constants;

/**
 * Single source of truth for all runtime game data.
 * Singleton so both GameActivity and UpgradeActivity share the same instance (spec §12).
 * killCount serves as both score and currency (spec §9.1).
 */
public class GameState {

    // ---- Singleton ----
    private static GameState instance;

    public static GameState getInstance() {
        if (instance == null) instance = new GameState();
        return instance;
    }

    // ---- Kill count (score + currency) ----
    private int killCount   = Constants.STARTING_KILL_COUNT;
    private float partialKills = 0f;
    private float passiveKillsPerSecond = 0f;

    // ---- Wave ----
    private int   waveNumber = 1;
    private float waveTimer  = Constants.WAVE_DURATION_START_SECONDS;

    // ---- Difficulty (updated each wave by advanceWave()) ----
    private float enemyBaseSpeed  = Constants.ENEMY_BASE_SPEED_START;
    private int   spawnIntervalMs = Constants.SPAWN_INTERVAL_START_MS;
    private int   maxEnemies      = Constants.MAX_ENEMIES_START;

    // ---- Health ----
    private int lives = 10;
    private int maxLives = 10;

    // ---- Upgrade-modifiable thresholds ----
    private int escapePenalty    = Constants.ESCAPE_PENALTY_DEFAULT;
    private int gameOverThreshold = Constants.GAME_OVER_THRESHOLD_DEFAULT;

    // ---- Upgrade flags ----
    private int  combatLevel     = 1;   // 1–4 — gates which enemy tiers can be hit
    private boolean masterBelt   = false; // C4 — doubles kill awards
    private boolean piercingUnlocked = false;

    // ---- Game flow ----
    private boolean gameRunning  = true;
    private boolean shieldActive = false; // absorbs one escape penalty

    // ==============================================================
    // Kill count
    // ==============================================================

    public int getKillCount() { return killCount; }

    public void addKills(int amount) {
        killCount += amount;
    }

    public void addKills(float amount) {
        partialKills += amount;

        int wholeKills = (int) partialKills;
        if (wholeKills > 0) {
            killCount += wholeKills;
            partialKills -= wholeKills;
        }
    }

    public void addPassiveKPS(float amount) {
        passiveKillsPerSecond += amount;
    }

    public float getPassiveKPS() { return passiveKillsPerSecond; }

    public int getLives() { return lives; }
    public int getMaxLives() { return maxLives; }

    public void loseLives(int amount) {
        if (shieldActive) {
            shieldActive = false;
            return;
        }
        lives -= amount;
        if (lives < 0) lives = 0;
        if (lives <= 0) gameRunning = false;
    }

    public void increaseMaxLives(int bonus) {
        maxLives += bonus;
        lives += bonus;
    }

    public boolean isGameOver() {
        return lives <= 0;
    }

    public boolean spendKills(int cost) {
        if (killCount >= cost) { killCount -= cost; return true; }
        return false;
    }

    /**
     * Called when an enemy reaches the bottom of the game area.
     * Shield absorbs one hit. Otherwise deducts escapePenalty.
     * @return actual kills deducted (0 if shield absorbed)
     */
    public int applyEscapePenalty() {
        if (shieldActive) {
            shieldActive = false;
            return 0;
        }
        killCount -= escapePenalty;
        checkGameOver();
        return escapePenalty;
    }

    private void checkGameOver() {
        if (killCount < gameOverThreshold) {
            killCount = gameOverThreshold; // clamp so display is clean
            gameRunning = false;
        }
    }

    /** Kill award by tier, applying Master Belt doubling (spec §9.2). */
    public int getKillAwardForTier(int tier) {
        int base;
        switch (tier) {
            case 3:  base = Constants.KILL_AWARD_TIER_3; break;
            case 4:  base = Constants.KILL_AWARD_TIER_4; break;
            case 5:  base = Constants.KILL_AWARD_TIER_5; break;
            default: base = Constants.KILL_AWARD_TIER_1; break;
        }
        return masterBelt ? base * 2 : base;
    }

    // ==============================================================
    // Wave
    // ==============================================================

    public int   getWaveNumber()  { return waveNumber; }
    public float getWaveTimer()   { return waveTimer; }

    /**
     * Advance the wave timer by one frame.
     * @return true if the wave has ended
     */
    public boolean tickWaveTimer(float deltaSeconds) {
        waveTimer -= deltaSeconds;
        return waveTimer <= 0f;
    }

    public void advanceWave() {
        waveNumber++;

        // Speed increases per wave, capped at max (spec §7.2)
        enemyBaseSpeed = Math.min(
                Constants.ENEMY_BASE_SPEED_START
                        + Constants.ENEMY_SPEED_INCREMENT_PER_WAVE * (waveNumber - 1),
                Constants.ENEMY_BASE_SPEED_MAX);

        // Spawn interval shrinks per wave, capped at min (spec §7.2)
        spawnIntervalMs = Math.max(
                Constants.SPAWN_INTERVAL_START_MS
                        - Constants.SPAWN_INTERVAL_DECREMENT_MS * (waveNumber - 1),
                Constants.SPAWN_INTERVAL_MIN_MS);

        if (waveNumber >= 15) {
            maxEnemies = Math.min(10 + (waveNumber - 15) / 2, 30);
        } else if (waveNumber >= 10) {
            maxEnemies = 8;
        } else if (waveNumber >= 5) {
            maxEnemies = 5;
        } else {
            maxEnemies = Constants.MAX_ENEMIES_START;
        }

        // Wave duration shrinks by 1s each wave, min 15s (spec §7.1)
        float newDuration = Math.max(
                Constants.WAVE_DURATION_START_SECONDS
                        - Constants.WAVE_DURATION_DECREMENT_SECONDS * (waveNumber - 1),
                Constants.WAVE_DURATION_MIN_SECONDS);
        waveTimer = newDuration;
    }

    // ==============================================================
    // Difficulty getters
    // ==============================================================

    public float getEnemyBaseSpeed()  { return enemyBaseSpeed; }
    public int   getSpawnIntervalMs() { return spawnIntervalMs; }
    public int   getMaxEnemies()      { return maxEnemies; }

    // ==============================================================
    // Upgrade-applied setters
    // ==============================================================

    public void setMaxEnemies(int max)          { maxEnemies = max; }
    public void setEscapePenalty(int penalty)   { escapePenalty = penalty; }
    public void setGameOverThreshold(int t)     { gameOverThreshold = t; }
    public void setCombatLevel(int level)       { combatLevel = level; }
    public void setMasterBelt(boolean on)       { masterBelt = on; }
    public void setPiercingUnlocked(boolean on) { piercingUnlocked = on; }
    public void activateShield()                { shieldActive = true; }
    public boolean isShieldActive()             { return shieldActive; }

    public int getCombatLevel() { return combatLevel; }
    public boolean isPiercingUnlocked() { return piercingUnlocked; }

    /** Max tier the player can currently hit (spec §6.1). */
    public int getMaxHittableTier() {
        switch (combatLevel) {
            case 2:  return 3;
            case 3:  return 4;
            case 4:  return 5;
            default: return 2;
        }
    }

    // ==============================================================
    // Game flow
    // ==============================================================

    public boolean isGameRunning() { return gameRunning; }

    public void triggerGameOver()  { gameRunning = false; }

    public void resetGame() {
        killCount         = Constants.STARTING_KILL_COUNT;
        partialKills      = 0f;
        passiveKillsPerSecond = 0f;
        waveNumber        = 1;
        waveTimer         = Constants.WAVE_DURATION_START_SECONDS;
        enemyBaseSpeed    = Constants.ENEMY_BASE_SPEED_START;
        spawnIntervalMs   = Constants.SPAWN_INTERVAL_START_MS;
        maxEnemies        = Constants.MAX_ENEMIES_START;
        lives             = 10;
        maxLives          = 10;
        escapePenalty     = Constants.ESCAPE_PENALTY_DEFAULT;
        gameOverThreshold = Constants.GAME_OVER_THRESHOLD_DEFAULT;
        combatLevel       = 1;
        masterBelt        = false;
        piercingUnlocked  = false;
        shieldActive      = false;
        gameRunning       = true;
    }
}
