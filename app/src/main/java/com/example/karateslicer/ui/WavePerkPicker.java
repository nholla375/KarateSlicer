package com.example.karateslicer.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.karateslicer.upgrade.AbilityType;
import com.example.karateslicer.upgrade.SkillNode;
import com.example.karateslicer.upgrade.SpecialAbility;
import com.example.karateslicer.upgrade.UpgradeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Overlay presenting 3 random upgrade cards after each wave (spec §3.5).
 * Enemies are frozen while this is visible.
 */
public class WavePerkPicker {

    private final UpgradeManager mgr;

    private RectF gameAreaRect = new RectF();

    private final Paint dimPaint   = new Paint();
    private final Paint headerPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardBgPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardBorderPaint= new Paint();
    private final Paint cardTxtPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTxtPaint= new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF[] cardRects = { new RectF(), new RectF(), new RectF() };
    private List<Object> currentCards = new ArrayList<>();
    private float cardTitleSize = 0f;
    private float cardSubSize = 0f;

    public WavePerkPicker(UpgradeManager mgr) {
        this.mgr = mgr;
        dimPaint.setColor(Color.parseColor("#BB000000"));
        headerPaint.setColor(Color.parseColor("#F6D06F"));
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setFakeBoldText(true);
        cardBgPaint.setColor(Color.parseColor("#26323A"));
        cardBorderPaint.setColor(Color.parseColor("#F6D06F"));
        cardBorderPaint.setStyle(Paint.Style.STROKE);
        cardBorderPaint.setStrokeWidth(4f);
        cardTxtPaint.setColor(Color.WHITE);
        cardTxtPaint.setTextAlign(Paint.Align.CENTER);
        cardTxtPaint.setFakeBoldText(true);
        subTxtPaint.setColor(Color.parseColor("#BBBBBB"));
        subTxtPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void layout(RectF gameArea) {
        this.gameAreaRect.set(gameArea);
        float w = gameArea.width();
        float h = gameArea.height();
        headerPaint.setTextSize(w * 0.08f);
        cardTitleSize = w * 0.065f;
        cardSubSize = w * 0.05f;
        cardTxtPaint.setTextSize(cardTitleSize);
        subTxtPaint.setTextSize(cardSubSize);

        float cardW = w * 0.26f;
        float cardH = h * 0.28f;
        float totalW = cardW * 3 + w * 0.04f * 2;
        float startX = gameArea.left + (w - totalW) / 2f;
        float cardY  = gameArea.top + h * 0.38f;

        for (int i = 0; i < 3; i++) {
            float left = startX + i * (cardW + w * 0.04f);
            cardRects[i].set(left, cardY, left + cardW, cardY + cardH);
        }
    }

    /** Call when entering WAVE_PERK_PICK state to populate cards. */
    public void prepare() {
        List<Object> pool = mgr.buildPerkPool();
        Collections.shuffle(pool);
        currentCards.clear();
        Set<String> seen = new HashSet<>();
        for (Object perk : pool) {
            if (seen.add(perkKey(perk))) currentCards.add(perk);
            if (currentCards.size() >= 3) break;
        }
        while (currentCards.size() < 3) currentCards.add(new UpgradeManager.KillBonusReward(10));
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(gameAreaRect, dimPaint);
        canvas.drawText("CHOOSE A PERK", gameAreaRect.centerX(),
                gameAreaRect.top + gameAreaRect.height() * 0.32f, headerPaint);

        for (int i = 0; i < 3 && i < currentCards.size(); i++) {
            drawCard(canvas, cardRects[i], currentCards.get(i));
        }
    }

    private void drawCard(Canvas canvas, RectF rect, Object perk) {
        canvas.drawRect(rect, cardBgPaint);
        canvas.drawRect(rect, cardBorderPaint);

        String title, sub;
        if (perk instanceof SkillNode) {
            SkillNode n = (SkillNode) perk;
            title = n.getName();
            sub   = n.getEffectDesc();
        } else if (perk instanceof SpecialAbility) {
            SpecialAbility a = (SpecialAbility) perk;
            title = abilityName(a.getType());
            sub   = abilityDesc(a.getType());
        } else if (perk instanceof UpgradeManager.KillBonusReward) {
            UpgradeManager.KillBonusReward bonus = (UpgradeManager.KillBonusReward) perk;
            title = "+" + bonus.getAmount() + " Kills";
            sub   = "Bonus reward";
        } else {
            title = "+50 Kills";
            sub   = "Bonus reward";
        }

        drawFittedText(canvas, title, rect.centerX(), rect.centerY() - rect.height()*0.08f,
                rect.width() * 0.86f, cardTxtPaint, cardTitleSize);
        drawFittedText(canvas, sub, rect.centerX(), rect.centerY() + rect.height()*0.18f,
                rect.width() * 0.86f, subTxtPaint, cardSubSize);
    }

    private void drawFittedText(Canvas canvas, String text, float x, float y,
                                float maxWidth, Paint paint, float baseSize) {
        paint.setTextSize(baseSize);
        while (paint.measureText(text) > maxWidth && paint.getTextSize() > 14f) {
            paint.setTextSize(paint.getTextSize() - 1f);
        }
        canvas.drawText(text, x, y, paint);
    }

    /**
     * Call on tap; returns the selected perk if a card was tapped, else null.
     */
    public Object handleTap(float x, float y) {
        for (int i = 0; i < 3 && i < currentCards.size(); i++) {
            if (cardRects[i].contains(x, y)) return currentCards.get(i);
        }
        return null;
    }

    /** Apply the chosen perk. Returns false if it was a fallback bonus. */
    public boolean applySelection(Object perk, com.example.karateslicer.state.GameState gs) {
        if (perk instanceof UpgradeManager.KillBonusReward) {
            gs.addKills(((UpgradeManager.KillBonusReward) perk).getAmount());
            return true;
        }
        if ("bonus_kills".equals(perk)) {
            gs.addKills(50);
            return true;
        }
        mgr.applyPerkFree(perk);
        return true;
    }

    private String perkKey(Object perk) {
        if (perk instanceof UpgradeManager.KillBonusReward) {
            return "kills_" + ((UpgradeManager.KillBonusReward) perk).getAmount();
        }
        if (perk instanceof SpecialAbility) {
            return "ability_" + ((SpecialAbility) perk).getType().name();
        }
        if (perk instanceof SkillNode) {
            SkillNode node = (SkillNode) perk;
            return "node_" + node.getBranch().name() + "_" + node.getIndex();
        }
        return String.valueOf(perk);
    }

    private String abilityName(AbilityType t) {
        switch (t) {
            case FREEZE:    return "Freeze";
            case BOMB:      return "Bomb";
            case TIME_SLOW: return "Time Slow";
            case SHIELD:    return "Shield";
            default:        return "Ability";
        }
    }

    private String abilityDesc(AbilityType t) {
        switch (t) {
            case FREEZE:    return "Stop all enemies 3s";
            case BOMB:      return "Clear all enemies";
            case TIME_SLOW: return "30% speed 5s";
            case SHIELD:    return "Block one escape";
            default:        return "";
        }
    }
}
