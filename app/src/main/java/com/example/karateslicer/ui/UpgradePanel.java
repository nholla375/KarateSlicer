package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.karateslicer.state.GameState;
import com.example.karateslicer.upgrade.AbilityType;
import com.example.karateslicer.upgrade.SkillNode;
import com.example.karateslicer.upgrade.SpecialAbility;
import com.example.karateslicer.upgrade.UpgradeManager;

import java.util.List;

/**
 * Draws the right-side upgrade panel (spec §2 — Upgrade Panel Sub-layout).
 * Also handles tap routing to UpgradeManager.
 */
public class UpgradePanel {

    private final GameState     gameState;
    private final UpgradeManager mgr;

    private RectF panelRect = new RectF();

    // Node button rects (built in layout())
    private final RectF[] combatRects   = new RectF[4];
    private final RectF[] survivalRects = new RectF[4];
    private final RectF[] abilityRects  = new RectF[4]; // one per AbilityType

    // Paint objects
    private final Paint bgPaint      = new Paint();
    private final Paint dividerPaint = new Paint();
    private final Paint borderPaint  = new Paint();
    private final Paint headerPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodeOwned    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodeAvail    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodeLocked   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodeText     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint abilityReady = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint abilityLock  = new Paint(Paint.ANTI_ALIAS_FLAG);
    public UpgradePanel(GameState gameState, UpgradeManager mgr) {
        this.gameState = gameState;
        this.mgr       = mgr;

        bgPaint.setColor(Color.parseColor("#202630"));
        dividerPaint.setColor(Color.parseColor("#F6D06F"));
        dividerPaint.setStrokeWidth(1.5f);
        borderPaint.setColor(Color.parseColor("#F6D06F"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        headerPaint.setColor(Color.parseColor("#F6D06F"));
        headerPaint.setFakeBoldText(true);
        headerPaint.setTextAlign(Paint.Align.CENTER);

        statPaint.setColor(Color.WHITE);
        statPaint.setTextAlign(Paint.Align.CENTER);

        nodeOwned.setColor(Color.parseColor("#2F6B3F"));
        nodeAvail.setColor(Color.parseColor("#26323A"));
        nodeLocked.setColor(Color.parseColor("#454B4E"));

        nodeText.setColor(Color.WHITE);
        nodeText.setTextAlign(Paint.Align.CENTER);
        nodeText.setFakeBoldText(false);

        abilityReady.setColor(Color.parseColor("#2F6B3F"));
        abilityLock.setColor(Color.parseColor("#454B4E"));

        for (int i = 0; i < 4; i++) { combatRects[i] = new RectF(); survivalRects[i] = new RectF(); abilityRects[i] = new RectF(); }
    }

    public void layout(RectF rect) {
        this.panelRect.set(rect);
        float w  = rect.width();
        float h  = rect.height();
        float cx = rect.centerX();
        float pad= w * 0.07f;
        float bw = w - pad * 2f;

        // Text sizes
        headerPaint.setTextSize(w * 0.13f);
        statPaint.setTextSize(w * 0.10f);
        nodeText.setTextSize(w * 0.085f);

        // Layout sections (approximate fractions of panel height):
        // 0–7%: kill count
        // 7–13%: wave number
        // 13–15%: divider
        // 15–56%: combat branch (4 nodes × ~10% each)
        // 56–58%: divider
        // 58–90%: survival branch (4 nodes)
        // 90–92%: divider
        // 92–100%: ability buttons

        float nodeH   = h * 0.09f;
        float gapY    = h * 0.01f;

        float combatStartY   = rect.top + h * 0.17f;
        for (int i = 0; i < 4; i++) {
            float top = combatStartY + i * (nodeH + gapY);
            combatRects[i].set(rect.left + pad, top, rect.left + pad + bw, top + nodeH);
        }

        float survivalStartY = combatRects[3].bottom + h * 0.03f;
        for (int i = 0; i < 4; i++) {
            float top = survivalStartY + i * (nodeH + gapY);
            survivalRects[i].set(rect.left + pad, top, rect.left + pad + bw, top + nodeH);
        }

        // Ability buttons — row of 4 at the bottom
        float abilityY  = rect.bottom - h * 0.09f;
        float abilitySize = w * 0.18f;
        float abilityGap  = (w - abilitySize * 4) / 5f;
        for (int i = 0; i < 4; i++) {
            float left = rect.left + abilityGap + i * (abilitySize + abilityGap);
            abilityRects[i].set(left, abilityY, left + abilitySize, abilityY + abilitySize);
        }
    }

    public void draw(Canvas canvas, long nowMs) {
        canvas.drawRect(panelRect, bgPaint);

        float cx = panelRect.centerX();
        float h  = panelRect.height();

        // Kill count (large, prominent — spec §2)
        headerPaint.setTextSize(panelRect.width() * 0.18f);
        canvas.drawText(String.valueOf(gameState.getKillCount()),
                cx, panelRect.top + h * 0.065f, headerPaint);
        headerPaint.setTextSize(panelRect.width() * 0.11f);
        canvas.drawText("kills", cx, panelRect.top + h * 0.105f, headerPaint);

        statPaint.setTextSize(panelRect.width() * 0.095f);
        canvas.drawText("Wave " + gameState.getWaveNumber(),
                cx, panelRect.top + h * 0.145f, statPaint);

        // Divider
        drawHDivider(canvas, panelRect.top + h * 0.16f);

        // Combat branch
        headerPaint.setTextSize(panelRect.width() * 0.10f);
        canvas.drawText("COMBAT", cx,
                combatRects[0].top - panelRect.height() * 0.01f, headerPaint);
        List<SkillNode> combat = mgr.getCombatNodes();
        for (int i = 0; i < 4 && i < combat.size(); i++) {
            drawNode(canvas, combatRects[i], combat.get(i), i == 0 || combat.get(i-1).isOwned());
        }

        // Survival branch
        drawHDivider(canvas, combatRects[3].bottom + panelRect.height() * 0.01f);
        canvas.drawText("SURVIVAL", cx,
                survivalRects[0].top - panelRect.height() * 0.01f, headerPaint);
        List<SkillNode> survival = mgr.getSurvivalNodes();
        for (int i = 0; i < 4 && i < survival.size(); i++) {
            drawNode(canvas, survivalRects[i], survival.get(i), i == 0 || survival.get(i-1).isOwned());
        }

        // Ability buttons
        drawHDivider(canvas, survivalRects[3].bottom + panelRect.height() * 0.01f);
        List<SpecialAbility> abilities = mgr.getAbilities();
        AbilityType[] types = AbilityType.values();
        for (int i = 0; i < 4 && i < abilities.size(); i++) {
            drawAbility(canvas, abilityRects[i], abilities.get(i), nowMs);
        }
    }

    private void drawNode(Canvas canvas, RectF rect, SkillNode node, boolean unlockable) {
        Paint bg = node.isOwned()  ? nodeOwned
                 : unlockable      ? nodeAvail
                                   : nodeLocked;
        canvas.drawRect(rect, bg);
        canvas.drawRect(rect, borderPaint);

        float cy = rect.centerY();
        nodeText.setTextSize(rect.height() * 0.32f);
        canvas.drawText(node.getName(), rect.centerX(), cy - rect.height() * 0.12f, nodeText);

        String sub = node.isOwned() ? "✓ Owned"
                   : unlockable     ? node.getCost() + " kills"
                                    : "Locked";
        nodeText.setTextSize(rect.height() * 0.26f);
        canvas.drawText(sub, rect.centerX(), cy + rect.height() * 0.28f, nodeText);
    }

    private void drawAbility(Canvas canvas, RectF rect, SpecialAbility ab, long nowMs) {
        Paint bg = ab.isUnlocked() ? abilityReady : abilityLock;
        canvas.drawRect(rect, bg);
        canvas.drawRect(rect, borderPaint);

        nodeText.setTextSize(rect.height() * 0.28f);
        nodeText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(abilityLabel(ab.getType()),
                rect.centerX(), rect.centerY() + rect.height() * 0.1f, nodeText);

        // Cooldown overlay
        if (ab.isUnlocked()) {
            float progress = ab.getCooldownProgress(nowMs);
            if (progress < 1f) {
                Paint overlay = new Paint();
                overlay.setColor(0x88000000);
                RectF cooldownRect = new RectF(rect.left, rect.top,
                        rect.right, rect.top + rect.height() * (1f - progress));
                canvas.drawRect(cooldownRect, overlay);
                canvas.drawRect(rect, borderPaint);
            }
        }
    }

    private String abilityLabel(AbilityType t) {
        switch (t) {
            case FREEZE:    return "❄";
            case BOMB:      return "💣";
            case TIME_SLOW: return "⏳";
            case SHIELD:    return "🛡";
            default:        return "?";
        }
    }

    private void drawHDivider(Canvas canvas, float y) {
        canvas.drawLine(panelRect.left + 8f, y, panelRect.right - 8f, y, dividerPaint);
    }

    // ------------------------------------------------------------------
    // Touch handling
    // ------------------------------------------------------------------

    /**
     * Route a tap to the correct node or ability.
     * @return true if a tap was handled
     */
    public boolean handleTap(float x, float y, long nowMs,
                             List<com.example.karateslicer.enemy.Enemy> enemies) {
        for (int i = 0; i < 4; i++) {
            if (combatRects[i].contains(x, y))   { mgr.tryPurchaseCombat(i);   return true; }
            if (survivalRects[i].contains(x, y)) { mgr.tryPurchaseSurvival(i); return true; }
        }
        AbilityType[] types = AbilityType.values();
        for (int i = 0; i < abilityRects.length && i < types.length; i++) {
            if (abilityRects[i].contains(x, y)) {
                mgr.activateAbility(types[i], nowMs, enemies);
                return true;
            }
        }
        return false;
    }

    public RectF getPanelRect() { return panelRect; }
}
