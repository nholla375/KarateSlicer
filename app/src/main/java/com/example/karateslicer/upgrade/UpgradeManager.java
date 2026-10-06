package com.example.karateslicer.upgrade;

import com.example.karateslicer.state.GameState;
import com.example.karateslicer.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the skill tree nodes and special abilities (spec §8).
 * Singleton so both GameActivity and UpgradeActivity share the same instance (spec §12).
 * Applies effects to GameState when nodes are purchased or free perks awarded.
 */
public class UpgradeManager {

    // ---- Singleton ----
    private static UpgradeManager instance;

    public static UpgradeManager getInstance() {
        if (instance == null) instance = new UpgradeManager(GameState.getInstance());
        return instance;
    }

    // ---- Combat Branch (C1–C4) ----
    private final List<SkillNode> combatNodes = new ArrayList<>();
    // ---- Survival Branch (S1–S4) ----
    private final List<SkillNode> survivalNodes = new ArrayList<>();
    private final List<SkillNode> dojoNodes = new ArrayList<>();
    // ---- Special Abilities ----
    private final List<SpecialAbility> abilities = new ArrayList<>();

    private final GameState gameState;

    public static class KillBonusReward {
        private final int amount;

        public KillBonusReward(int amount) {
            this.amount = amount;
        }

        public int getAmount() {
            return amount;
        }
    }

    // Active ability timers
    private long freezeEndMs    = 0;
    private long timeSlowEndMs  = 0;

    public UpgradeManager(GameState gameState) {
        this.gameState = gameState;
        buildTree();
    }

    /** Reset all nodes and abilities for a new game run. */
    public void reset() {
        combatNodes.clear();
        survivalNodes.clear();
        dojoNodes.clear();
        abilities.clear();
        freezeEndMs   = 0;
        timeSlowEndMs = 0;
        buildTree();
    }

    private void buildTree() {
        // Combat branch (spec §8.2)
        combatNodes.add(new SkillNode("Piercing",      "Hit all matching enemies", Constants.COST_BELT_RANK_1,  SkillNode.Branch.COMBAT,   0));
        combatNodes.add(new SkillNode("Belt Rank II",  "Hit Tier 4",          Constants.COST_BELT_RANK_2,  SkillNode.Branch.COMBAT,   1));
        combatNodes.add(new SkillNode("Belt Rank III", "Hit Tier 5",          Constants.COST_BELT_RANK_3,  SkillNode.Branch.COMBAT,   2));
        combatNodes.add(new SkillNode("Master Belt",   "+2 kills per kill",   Constants.COST_MASTER_BELT,  SkillNode.Branch.COMBAT,   3));

        // Survival branch (spec §8.3)
        survivalNodes.add(new SkillNode("Iron Skin I",    "+5 max lives",  Constants.COST_IRON_SKIN_1,  SkillNode.Branch.SURVIVAL, 0));
        survivalNodes.add(new SkillNode("Iron Skin II",   "+10 max lives", Constants.COST_IRON_SKIN_2,  SkillNode.Branch.SURVIVAL, 1));
        survivalNodes.add(new SkillNode("Last Stand",     "+15 max lives", Constants.COST_LAST_STAND,   SkillNode.Branch.SURVIVAL, 2));
        survivalNodes.add(new SkillNode("Iron Fortress",  "+20 max lives", Constants.COST_IRON_FORTRESS,SkillNode.Branch.SURVIVAL, 3));

        for (int i = 0; i < Constants.DOJO_UPGRADE_COUNT; i++) {
            int cost = Constants.COST_DOJO + i * Constants.COST_DOJO_INCREMENT;
            float kps = Constants.DOJO_KPS_START + i * Constants.DOJO_KPS_INCREMENT;
            dojoNodes.add(new SkillNode("Dojo " + (i + 1),
                    "+" + kps + " passive KPS", cost, SkillNode.Branch.DOJO, i));
        }

        // Abilities
        for (AbilityType t : AbilityType.values()) abilities.add(new SpecialAbility(t));
    }

    // ------------------------------------------------------------------
    // Purchase (tapping in upgrade panel)
    // ------------------------------------------------------------------

    public boolean tryPurchaseCombat(int index) {
        return tryPurchase(combatNodes, index);
    }

    public boolean tryPurchaseSurvival(int index) {
        return tryPurchase(survivalNodes, index);
    }

    public boolean tryPurchaseDojo(int index) {
        return tryPurchase(dojoNodes, index);
    }

    private boolean tryPurchase(List<SkillNode> branch, int index) {
        if (index < 0 || index >= branch.size()) return false;
        SkillNode node = branch.get(index);
        if (node.isOwned()) return false;
        // Must own all previous nodes in the branch
        for (int i = 0; i < index; i++) if (!branch.get(i).isOwned()) return false;
        if (!gameState.spendKills(node.getCost())) return false;
        applyNode(node);
        return true;
    }

    // ------------------------------------------------------------------
    // Free perk (Wave Perk Pick)
    // ------------------------------------------------------------------

    public void applyPerkFree(Object perk) {
        if (perk instanceof SkillNode) {
            SkillNode node = (SkillNode) perk;
            if (!node.isOwned()) applyNode(node);
        } else if (perk instanceof SpecialAbility) {
            ((SpecialAbility) perk).unlock();
        }
    }

    private void applyNode(SkillNode node) {
        node.setOwned();
        switch (node.getBranch()) {
            case COMBAT:
                int newCombatLevel = 1;
                for (SkillNode n : combatNodes) if (n.isOwned()) newCombatLevel++;
                gameState.setCombatLevel(Math.min(newCombatLevel, 4));
                if (node.getIndex() == 0) gameState.setPiercingUnlocked(true);
                if (node.getIndex() == 3) gameState.setMasterBelt(true);
                break;
            case SURVIVAL:
                gameState.increaseMaxLives((node.getIndex() + 1) * 5);
                break;
            case DOJO:
                gameState.addPassiveKPS(Constants.DOJO_KPS_START
                        + node.getIndex() * Constants.DOJO_KPS_INCREMENT);
                break;
        }
    }

    // ------------------------------------------------------------------
    // Ability activation (tap on ability button)
    // ------------------------------------------------------------------

    public boolean activateAbility(AbilityType type, long nowMs,
                                   List<com.example.karateslicer.enemy.Enemy> enemies) {
        SpecialAbility ability = getAbility(type);
        if (ability == null || !ability.isReady(nowMs)) return false;
        ability.use(nowMs);

        switch (type) {
            case FREEZE:
                freezeEndMs = nowMs + Constants.FREEZE_DURATION_MS;
                break;
            case BOMB:
                if (enemies != null) {
                    for (com.example.karateslicer.enemy.Enemy e : enemies) e.kill();
                }
                break;
            case TIME_SLOW:
                timeSlowEndMs = nowMs + Constants.TIME_SLOW_DURATION_MS;
                if (enemies != null) {
                    for (com.example.karateslicer.enemy.Enemy e : enemies) {
                        e.setSpeedMultiplier(Constants.TIME_SLOW_FACTOR);
                    }
                }
                break;
            case SHIELD:
                gameState.activateShield();
                ability.unlock(); // keep showing as owned; one-time per pick
                break;
        }
        return true;
    }

    public void update(long nowMs, List<com.example.karateslicer.enemy.Enemy> enemies) {
        // Restore speed after Time Slow expires
        if (timeSlowEndMs > 0 && nowMs > timeSlowEndMs) {
            timeSlowEndMs = 0;
            for (com.example.karateslicer.enemy.Enemy e : enemies) e.setSpeedMultiplier(1f);
        }
    }

    public boolean isFrozen(long nowMs) { return nowMs < freezeEndMs; }

    // ------------------------------------------------------------------
    // Perk pool (spec §8.5)
    // ------------------------------------------------------------------

    /** Returns weighted supplemental rewards for the perk picker. */
    public List<Object> buildPerkPool() {
        List<Object> pool = new ArrayList<>();
        addKillBonus(pool, 10, 60);
        addKillBonus(pool, 20, 25);
        addKillBonus(pool, 30, 10);
        addKillBonus(pool, 50, 3);
        for (SpecialAbility a : abilities) if (!a.isUnlocked()) pool.add(a);
        return pool;
    }

    private void addKillBonus(List<Object> pool, int amount, int weight) {
        for (int i = 0; i < weight; i++) {
            pool.add(new KillBonusReward(amount));
        }
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public List<SkillNode>      getCombatNodes()   { return combatNodes; }
    public List<SkillNode>      getSurvivalNodes() { return survivalNodes; }
    public List<SkillNode>      getDojoNodes()     { return dojoNodes; }
    public List<SpecialAbility> getAbilities()     { return abilities; }

    private SpecialAbility getAbility(AbilityType type) {
        for (SpecialAbility a : abilities) if (a.getType() == type) return a;
        return null;
    }
}
