package com.example.karateslicer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.karateslicer.state.GameState;
import com.example.karateslicer.upgrade.AbilityType;
import com.example.karateslicer.upgrade.SkillNode;
import com.example.karateslicer.upgrade.SpecialAbility;
import com.example.karateslicer.upgrade.UpgradeManager;

import java.util.List;

/**
 * Full-screen upgrade screen (spec §2, §8).
 * Reads GameState.getInstance() and UpgradeManager.getInstance() — both singletons.
 * Purchases take effect immediately; returning to GameActivity resumes gameplay.
 */
public class UpgradeActivity extends AppCompatActivity {

    private static final int UI_BG = Color.rgb(32, 38, 48);
    private static final int UI_PANEL = Color.rgb(38, 50, 58);
    private static final int UI_RED = Color.rgb(143, 47, 47);
    private static final int UI_GOLD = Color.rgb(246, 208, 111);
    private static final int UI_GREEN = Color.rgb(47, 107, 63);
    private static final int UI_LOCKED = Color.rgb(69, 75, 78);

    private TextView killCountView;
    private TextView waveNumberView;
    private Button[] combatButtons;
    private Button[] survivalButtons;
    private Button[] dojoButtons;
    private Button[] abilityButtons  = new Button[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(buildLayout());
        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    // =========================================================================
    // Layout (spec §2 — Upgrade Screen layout, built programmatically)
    // =========================================================================

    private View buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UI_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        // ← BACK TO GAME
        Button backBtn = new Button(this);
        backBtn.setText("← BACK TO GAME");
        applyPixelButton(backBtn, UI_RED);
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        root.addView(backBtn, matchWidthWrapHeight());

        addSpacer(root, 24);

        // Kill count (large, prominent)
        killCountView = new TextView(this);
        killCountView.setTextSize(48f);
        killCountView.setTextColor(UI_GOLD);
        killCountView.setGravity(Gravity.CENTER);
        killCountView.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(killCountView, matchWidthWrapHeight());

        TextView killLabel = new TextView(this);
        killLabel.setText("kills");
        killLabel.setTextSize(16f);
        killLabel.setTextColor(Color.WHITE);
        killLabel.setGravity(Gravity.CENTER);
        root.addView(killLabel, matchWidthWrapHeight());

        // Wave number
        waveNumberView = new TextView(this);
        waveNumberView.setTextSize(18f);
        waveNumberView.setTextColor(Color.WHITE);
        waveNumberView.setGravity(Gravity.CENTER);
        root.addView(waveNumberView, matchWidthWrapHeight());

        addDivider(root);

        // Combat Branch
        addSectionHeader(root, "COMBAT BRANCH");
        List<SkillNode> combatNodes = UpgradeManager.getInstance().getCombatNodes();
        combatButtons = new Button[combatNodes.size()];
        for (int i = 0; i < combatButtons.length; i++) {
            final int idx = i;
            combatButtons[i] = makeNodeButton(combatNodes.get(i), i == 0 || combatNodes.get(i - 1).isOwned());
            combatButtons[i].setOnClickListener(v -> {
                UpgradeManager.getInstance().tryPurchaseCombat(idx);
                refreshUI();
            });
            root.addView(combatButtons[i], matchWidthWrapHeight());
            addSpacer(root, 8);
        }

        addDivider(root);

        // Survival Branch
        addSectionHeader(root, "SURVIVAL BRANCH");
        List<SkillNode> survivalNodes = UpgradeManager.getInstance().getSurvivalNodes();
        survivalButtons = new Button[survivalNodes.size()];
        for (int i = 0; i < survivalButtons.length; i++) {
            final int idx = i;
            survivalButtons[i] = makeNodeButton(survivalNodes.get(i), i == 0 || survivalNodes.get(i - 1).isOwned());
            survivalButtons[i].setOnClickListener(v -> {
                UpgradeManager.getInstance().tryPurchaseSurvival(idx);
                refreshUI();
            });
            root.addView(survivalButtons[i], matchWidthWrapHeight());
            addSpacer(root, 8);
        }

        addDivider(root);

        // Special Abilities (2×2 grid)
        addSectionHeader(root, "DOJO BRANCH");
        List<SkillNode> dojoNodes = UpgradeManager.getInstance().getDojoNodes();
        dojoButtons = new Button[dojoNodes.size()];
        for (int i = 0; i < dojoButtons.length; i++) {
            final int idx = i;
            dojoButtons[i] = makeNodeButton(dojoNodes.get(i), i == 0 || dojoNodes.get(i - 1).isOwned());
            dojoButtons[i].setOnClickListener(v -> {
                UpgradeManager.getInstance().tryPurchaseDojo(idx);
                refreshUI();
            });
            root.addView(dojoButtons[i], matchWidthWrapHeight());
            addSpacer(root, 8);
        }

        addDivider(root);

        addSectionHeader(root, "SPECIAL ABILITIES");
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        List<SpecialAbility> abilities = UpgradeManager.getInstance().getAbilities();
        AbilityType[] types = AbilityType.values();
        for (int i = 0; i < 4 && i < abilities.size(); i++) {
            final int idx = i;
            abilityButtons[i] = makeAbilityButton(abilities.get(i));
            abilityButtons[i].setOnClickListener(v -> {
                UpgradeManager.getInstance().activateAbility(
                        types[idx], System.currentTimeMillis(), null);
                refreshUI();
            });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            params.setMargins(8, 8, 8, 8);
            grid.addView(abilityButtons[i], params);
        }
        root.addView(grid, matchWidthWrapHeight());

        addSpacer(root, 32);
        return scroll;
    }

    // =========================================================================
    // UI refresh (call after every purchase)
    // =========================================================================

    private void refreshUI() {
        GameState gs = GameState.getInstance();
        killCountView.setText(String.valueOf(gs.getKillCount()));
        waveNumberView.setText("Wave " + gs.getWaveNumber());

        List<SkillNode> combatNodes   = UpgradeManager.getInstance().getCombatNodes();
        List<SkillNode> survivalNodes = UpgradeManager.getInstance().getSurvivalNodes();
        List<SkillNode> dojoNodes     = UpgradeManager.getInstance().getDojoNodes();
        List<SpecialAbility> abilities = UpgradeManager.getInstance().getAbilities();

        for (int i = 0; i < combatButtons.length; i++) {
            boolean unlockable = (i == 0) || combatNodes.get(i - 1).isOwned();
            updateNodeButton(combatButtons[i], combatNodes.get(i), unlockable);
        }
        for (int i = 0; i < survivalButtons.length; i++) {
            boolean unlockable = (i == 0) || survivalNodes.get(i - 1).isOwned();
            updateNodeButton(survivalButtons[i], survivalNodes.get(i), unlockable);
        }
        for (int i = 0; i < dojoButtons.length; i++) {
            boolean unlockable = (i == 0) || dojoNodes.get(i - 1).isOwned();
            updateNodeButton(dojoButtons[i], dojoNodes.get(i), unlockable);
        }
        for (int i = 0; i < 4 && i < abilities.size(); i++) {
            updateAbilityButton(abilityButtons[i], abilities.get(i));
        }
    }

    // =========================================================================
    // Button factory helpers
    // =========================================================================

    private Button makeNodeButton(SkillNode node, boolean unlockable) {
        Button btn = new Button(this);
        updateNodeButton(btn, node, unlockable);
        return btn;
    }

    private void updateNodeButton(Button btn, SkillNode node, boolean unlockable) {
        String label;
        int bgColor;
        if (node.isOwned()) {
            label   = node.getName() + "\n✓ Owned — " + node.getEffectDesc();
            bgColor = UI_GREEN;
        } else if (unlockable) {
            label   = node.getName() + "\n" + node.getCost() + " kills — " + node.getEffectDesc();
            bgColor = UI_PANEL;
        } else {
            label   = node.getName() + "\n🔒 Locked";
            bgColor = UI_LOCKED;
        }
        btn.setText(label);
        applyPixelButton(btn, bgColor);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setEnabled(unlockable && !node.isOwned());
    }

    private Button makeAbilityButton(SpecialAbility ab) {
        Button btn = new Button(this);
        updateAbilityButton(btn, ab);
        return btn;
    }

    private void updateAbilityButton(Button btn, SpecialAbility ab) {
        long nowMs = System.currentTimeMillis();
        String label = abilityLabel(ab.getType()) + " " + abilityName(ab.getType());
        if (!ab.isUnlocked()) {
            label += "\n(Perk pick only)";
            applyPixelButton(btn, UI_LOCKED);
            btn.setEnabled(false);
        } else if (ab.isReady(nowMs)) {
            label += "\nREADY";
            applyPixelButton(btn, UI_GREEN);
            btn.setEnabled(true);
        } else {
            int secs = (int) Math.ceil(
                    (1f - ab.getCooldownProgress(nowMs)) * cooldownSeconds(ab.getType()));
            label += "\n" + secs + "s";
            applyPixelButton(btn, Color.rgb(120, 78, 30));
            btn.setEnabled(false);
        }
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
    }

    // =========================================================================
    // Layout helpers
    // =========================================================================

    private LinearLayout.LayoutParams matchWidthWrapHeight() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void addSpacer(LinearLayout parent, int heightDp) {
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)));
    }

    private void addDivider(LinearLayout parent) {
        View div = new View(this);
        div.setBackgroundColor(UI_GOLD);
        parent.addView(div, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(2)));
        addSpacer(parent, 16);
    }

    private void addSectionHeader(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20f);
        tv.setTextColor(UI_GOLD);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 16, 0, 8);
        parent.addView(tv, matchWidthWrapHeight());
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    private void applyPixelButton(Button btn, int fillColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(fillColor);
        bg.setStroke(dp(3), UI_GOLD);
        bg.setCornerRadius(0);
        btn.setBackground(bg);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
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

    private String abilityName(AbilityType t) {
        switch (t) {
            case FREEZE:    return "Freeze";
            case BOMB:      return "Bomb";
            case TIME_SLOW: return "Time Slow";
            case SHIELD:    return "Shield";
            default:        return "Ability";
        }
    }

    private int cooldownSeconds(AbilityType t) {
        switch (t) {
            case FREEZE:    return 30;
            case BOMB:      return 60;
            case TIME_SLOW: return 45;
            default:        return 0;
        }
    }
}
