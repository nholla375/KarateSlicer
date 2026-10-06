package com.example.karateslicer.upgrade;

/**
 * A single node in the Combat or Survival upgrade branch (spec §8.2 / §8.3).
 */
public class SkillNode {

    public enum Branch { COMBAT, SURVIVAL, DOJO }

    private final String name;
    private final String effectDesc;
    private final int    cost;
    private final Branch branch;
    private final int    index; // 0-based position within its branch

    private boolean owned = false;

    public SkillNode(String name, String effectDesc, int cost, Branch branch, int index) {
        this.name       = name;
        this.effectDesc = effectDesc;
        this.cost       = cost;
        this.branch     = branch;
        this.index      = index;
    }

    public String  getName()       { return name; }
    public String  getEffectDesc() { return effectDesc; }
    public int     getCost()       { return cost; }
    public Branch  getBranch()     { return branch; }
    public int     getIndex()      { return index; }
    public boolean isOwned()       { return owned; }
    public void    setOwned()      { owned = true; }
}
