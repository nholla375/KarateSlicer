package com.example.karateslicer.util;

public final class Constants {
    private Constants() {}

    // Gameplay
    public static final int   STARTING_KILL_COUNT             = 0;
    public static final int   GAME_OVER_THRESHOLD_DEFAULT     = 0;
    public static final int   GAME_OVER_THRESHOLD_LAST_STAND  = -30;
    public static final int   ESCAPE_PENALTY_DEFAULT          = 10;
    public static final int   MAX_ENEMIES_START               = 1;
    public static final int   MAX_ENEMIES_CAP                 = 8;

    // Wave
    public static final int   WAVE_DURATION_START_SECONDS     = 30;
    public static final int   WAVE_DURATION_MIN_SECONDS       = 15;
    public static final int   WAVE_DURATION_DECREMENT_SECONDS = 1;
    public static final int   INTER_WAVE_PAUSE_SECONDS        = 2;

    // Difficulty
    public static final float ENEMY_BASE_SPEED_START          = 120f;
    public static final float ENEMY_BASE_SPEED_MAX            = 400f;
    public static final float ENEMY_SPEED_INCREMENT_PER_WAVE  = 10f;
    public static final int   SPAWN_INTERVAL_START_MS         = 3000;
    public static final int   SPAWN_INTERVAL_MIN_MS           = 500;
    public static final int   SPAWN_INTERVAL_DECREMENT_MS     = 150;

    // Particles
    public static final int   PARTICLE_POOL_SIZE              = 100;
    public static final int   PARTICLES_PER_KILL              = 8;
    public static final int   PARTICLE_LIFETIME_FRAMES        = 20;

    // Hit feedback
    public static final int   WRONG_GESTURE_FLASH_FRAMES      = 10;
    public static final float WRONG_GESTURE_SHAKE_PX          = 5f;

    // Abilities
    public static final int   FREEZE_DURATION_MS              = 3000;
    public static final int   TIME_SLOW_DURATION_MS           = 5000;
    public static final float TIME_SLOW_FACTOR                = 0.3f;
    public static final int   FREEZE_COOLDOWN_MS              = 30000;
    public static final int   BOMB_COOLDOWN_MS                = 60000;
    public static final int   TIME_SLOW_COOLDOWN_MS           = 45000;

    // Enemy size (as fraction of game area width)
    public static final float ENEMY_SIZE_RATIO                = 0.07f;

    // HUD
    public static final float HUD_TOP_BAR_RATIO               = 0.08f;
    public static final float HUD_BOTTOM_BAR_RATIO            = 0.08f;

    // Kill awards
    public static final int   KILL_AWARD_TIER_1               = 1;
    public static final int   KILL_AWARD_TIER_2               = 1;
    public static final int   KILL_AWARD_TIER_3               = 2;
    public static final int   KILL_AWARD_TIER_4               = 3;
    public static final int   KILL_AWARD_TIER_5               = 4;

    // Combat branch costs
    public static final int   COST_BELT_RANK_1                = 25;
    public static final int   COST_BELT_RANK_2                = 50;
    public static final int   COST_BELT_RANK_3                = 250;
    public static final int   COST_MASTER_BELT                = 500;

    // Survival branch costs
    public static final int   COST_IRON_SKIN_1                = 20;
    public static final int   COST_IRON_SKIN_2                = 100;
    public static final int   COST_LAST_STAND                 = 350;
    public static final int   COST_IRON_FORTRESS              = 600;
    public static final int   COST_DOJO                       = 10;
    public static final int   COST_DOJO_INCREMENT             = 500;
    public static final int   DOJO_UPGRADE_COUNT              = 5;
    public static final float DOJO_KPS_START                  = 2f;
    public static final float DOJO_KPS_INCREMENT              = 1f;

    // Escape penalties per upgrade level
    public static final int   ESCAPE_PENALTY_IRON_SKIN_1      = 7;
    public static final int   ESCAPE_PENALTY_IRON_SKIN_2      = 4;
    public static final int   ESCAPE_PENALTY_IRON_FORTRESS    = 1;
}
