package com.example.karateslicer.upgrade;

import com.example.karateslicer.util.Constants;

/** A special ability button in the upgrade panel (spec §8.4). */
public class SpecialAbility {

    private final AbilityType type;
    private boolean unlocked  = false;
    private long    lastUsedMs = -1_000_000L; // far in the past so it's ready immediately

    public SpecialAbility(AbilityType type) {
        this.type = type;
    }

    public AbilityType getType()    { return type; }
    public boolean     isUnlocked() { return unlocked; }
    public void        unlock()     { unlocked = true; }

    public boolean isReady(long nowMs) {
        return unlocked && (nowMs - lastUsedMs) >= getCooldownMs();
    }

    public void use(long nowMs) { lastUsedMs = nowMs; }

    /** Cooldown progress 0..1 (1 = fully charged). */
    public float getCooldownProgress(long nowMs) {
        if (!unlocked) return 0f;
        long elapsed = nowMs - lastUsedMs;
        return Math.min(1f, (float) elapsed / getCooldownMs());
    }

    private long getCooldownMs() {
        switch (type) {
            case FREEZE:    return Constants.FREEZE_COOLDOWN_MS;
            case BOMB:      return Constants.BOMB_COOLDOWN_MS;
            case TIME_SLOW: return Constants.TIME_SLOW_COOLDOWN_MS;
            case SHIELD:    return 0; // shield is one-time per perk pick
            default:        return 30_000L;
        }
    }

    /** Shield is special: one-time use, replenishes on next perk pick. */
    public boolean isShieldReady() {
        return type == AbilityType.SHIELD && unlocked;
    }
}
