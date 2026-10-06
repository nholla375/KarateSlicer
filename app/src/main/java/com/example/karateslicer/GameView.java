package com.example.karateslicer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.karateslicer.audio.SoundManager;
import com.example.karateslicer.enemy.Enemy;
import com.example.karateslicer.enemy.EnemySpawner;
import com.example.karateslicer.gesture.GestureHandler;
import com.example.karateslicer.state.GameState;
import com.example.karateslicer.state.GameStateManager;
import com.example.karateslicer.state.GameStateManager.State;
import com.example.karateslicer.ui.GameHUD;
import com.example.karateslicer.ui.GameOverScreen;
import com.example.karateslicer.ui.ParticleSystem;
import com.example.karateslicer.ui.PauseOverlay;
import com.example.karateslicer.ui.TitleScreen;
import com.example.karateslicer.ui.WavePerkPicker;
import com.example.karateslicer.upgrade.AbilityType;
import com.example.karateslicer.upgrade.SkillNode;
import com.example.karateslicer.upgrade.SpecialAbility;
import com.example.karateslicer.upgrade.UpgradeManager;
import com.example.karateslicer.util.Constants;
import com.example.karateslicer.util.ScoreManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Full-screen game view (spec §2 — dual-screen architecture).
 * Upgrade panel is a separate UpgradeActivity; this view owns only gameplay.
 * GameThread calls update() and draw() at ~60 FPS.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "GameView";

    /** Callback so GameActivity can launch UpgradeActivity on UPGRADES tap. */
    public interface UpgradeListener {
        void onUpgradesTapped();
    }

    private UpgradeListener upgradeListener;

    public void setUpgradeListener(UpgradeListener listener) {
        this.upgradeListener = listener;
    }

    // ---- Layout ----
    private final RectF gameAreaRect = new RectF(); // full screen
    private float       fieldBottom  = 0f;          // y where enemies escape (above bottom bar)
    private volatile boolean layoutReady = false;

    // ---- Core ----
    private GameThread    gameThread;
    private SoundManager  sound;
    private ScoreManager  scoreManager;
    private GameStateManager gsm;

    // ---- Game systems ----
    private EnemySpawner  enemySpawner;
    private final List<Enemy> enemies = new CopyOnWriteArrayList<>();
    private GestureHandler    gestureHandler;
    private ParticleSystem    particles;

    // ---- UI renderers ----
    private GameHUD        hud;
    private TitleScreen    titleScreen;
    private PauseOverlay   pauseOverlay;
    private GameOverScreen gameOverScreen;
    private WavePerkPicker perkPicker;

    // ---- Background ----
    private final Paint gameAreaBgPaint  = new Paint();
    private final Paint bottomBarBgPaint = new Paint();
    private final Paint dojoPaint        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dojoRect         = new RectF();
    private final Path  dojoPath         = new Path();

    // ---- Inter-wave timer ----
    private float   interWaveTimer      = 0f;
    private boolean waitingForInterWave = false;

    // ---- Pause button: top-right (~48dp, spec §2) ----
    private final RectF pauseButtonRect = new RectF();
    private final RectF helpButtonRect  = new RectF();
    private final Paint pauseBtnBg      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pauseBtnTxt     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean showHelpOverlay     = false;
    private final RectF helpOverlayRect = new RectF();
    private final Paint helpOverlayBgPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint helpOverlayTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint helpOverlayTextPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pixelBorderPaint      = new Paint();

    // ---- UPGRADES button: bottom-center (spec §2) ----
    private final RectF upgradesButtonRect = new RectF();
    private final Paint upgradesBtnBg      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint upgradesBtnTxt     = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---- Ability icons: bottom-right (spec §2) ----
    private final RectF[] abilityIconRects = new RectF[4];
    private final Paint   abilityIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint   abilityIconTxt   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint   abilityCooldownOverlay = new Paint();
    private final Paint   bottomInfoTxt    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // =========================================================================
    // Constructor
    // =========================================================================

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        gameAreaBgPaint.setColor(Color.parseColor("#0f0f1a"));
        bottomBarBgPaint.setColor(Color.parseColor("#202630"));

        pauseBtnBg.setColor(Color.parseColor("#26323A"));
        pauseBtnTxt.setColor(Color.WHITE);
        pauseBtnTxt.setTextAlign(Paint.Align.CENTER);
        pauseBtnTxt.setFakeBoldText(true);

        helpOverlayBgPaint.setColor(Color.parseColor("#D8202630"));
        helpOverlayTitlePaint.setColor(Color.parseColor("#F6D06F"));
        helpOverlayTitlePaint.setTextAlign(Paint.Align.CENTER);
        helpOverlayTitlePaint.setFakeBoldText(true);
        helpOverlayTextPaint.setColor(Color.WHITE);
        helpOverlayTextPaint.setTextAlign(Paint.Align.CENTER);

        pixelBorderPaint.setStyle(Paint.Style.STROKE);
        pixelBorderPaint.setStrokeWidth(4f);
        pixelBorderPaint.setColor(Color.parseColor("#F6D06F"));

        upgradesBtnBg.setColor(Color.parseColor("#8F2F2F"));
        upgradesBtnTxt.setColor(Color.WHITE);
        upgradesBtnTxt.setTextAlign(Paint.Align.CENTER);
        upgradesBtnTxt.setFakeBoldText(true);

        abilityIconPaint.setTextAlign(Paint.Align.CENTER);
        abilityIconTxt.setColor(Color.WHITE);
        abilityIconTxt.setTextAlign(Paint.Align.CENTER);
        abilityCooldownOverlay.setColor(0x88000000);
        bottomInfoTxt.setColor(Color.parseColor("#F6D06F"));
        bottomInfoTxt.setTextAlign(Paint.Align.RIGHT);
        bottomInfoTxt.setFakeBoldText(true);

        for (int i = 0; i < 4; i++) abilityIconRects[i] = new RectF();

        initSystems(context);
    }

    private void initSystems(Context context) {
        sound        = SoundManager.getInstance(context);
        scoreManager = new ScoreManager(context);
        gsm          = new GameStateManager();
        enemySpawner = new EnemySpawner(GameState.getInstance());
        particles    = new ParticleSystem();
        hud          = new GameHUD(GameState.getInstance());
        titleScreen  = new TitleScreen(scoreManager);
        pauseOverlay = new PauseOverlay();
        gameOverScreen = new GameOverScreen(GameState.getInstance(), scoreManager);
        perkPicker   = new WavePerkPicker(UpgradeManager.getInstance());
    }

    // =========================================================================
    // SurfaceHolder.Callback
    // =========================================================================

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startGameThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Full-screen game area (spec §2 — dual-screen architecture)
        gameAreaRect.set(0, 0, width, height);

        float topBarH    = height * Constants.HUD_TOP_BAR_RATIO;
        float bottomBarH = height * Constants.HUD_BOTTOM_BAR_RATIO;
        fieldBottom = height - bottomBarH; // y where enemies escape

        // Top-right HUD buttons
        float btnInset = 8f;
        float btnGap = 8f;
        float btnSize = Math.min(topBarH - btnInset * 2f, Math.min(width, height) * 0.06f);
        float btnTop = (topBarH - btnSize) / 2f;
        helpButtonRect.set(width - btnSize - btnInset, btnTop, width - btnInset, btnTop + btnSize);
        pauseButtonRect.set(helpButtonRect.left - btnGap - btnSize, btnTop,
                helpButtonRect.left - btnGap, btnTop + btnSize);
        pauseBtnTxt.setTextSize(btnSize * 0.55f);
        helpOverlayTitlePaint.setTextSize(width * 0.055f);
        helpOverlayTextPaint.setTextSize(width * 0.045f);

        float barTop = height - bottomBarH;

        // Ability icons: bottom-center (4 icons)
        float iconSize = Math.min(bottomBarH * 0.66f, width * 0.09f);
        float iconPad  = (bottomBarH - iconSize) / 2f;
        float iconGap = 6f;
        float iconTotalW = 4 * iconSize + 3 * iconGap;
        float iconStartX = width / 2f - iconTotalW / 2f;
        for (int i = 0; i < 4; i++) {
            float left = iconStartX + i * (iconSize + iconGap);
            abilityIconRects[i].set(left, barTop + iconPad,
                                    left + iconSize, barTop + iconPad + iconSize);
        }
        abilityIconTxt.setTextSize(iconSize * 0.5f);

        // UPGRADES button: bottom-left
        float upgradesBtnW = Math.min(width * 0.25f, Math.max(width * 0.18f, iconStartX - 16f));
        float upgradesBtnH = bottomBarH * 0.65f;
        float upgradesLeft = 12f;
        upgradesButtonRect.set(
            upgradesLeft,
            barTop + (bottomBarH - upgradesBtnH) / 2f,
            upgradesLeft + upgradesBtnW,
            barTop + (bottomBarH + upgradesBtnH) / 2f
        );
        upgradesBtnTxt.setTextSize(upgradesBtnH * 0.34f);
        bottomInfoTxt.setTextSize(bottomBarH * 0.26f);

        // Lay out subsystems
        hud.layout(gameAreaRect);
        hud.setRightTextInset(gameAreaRect.right - pauseButtonRect.left + btnGap);
        gestureHandler = new GestureHandler(GameState.getInstance(), hud, particles, sound);
        gestureHandler.setGameAreaRight(width);
        enemySpawner.layout(width, height);
        RectF overlayArea = new RectF(0, topBarH, width, fieldBottom);
        titleScreen.layout(gameAreaRect);
        pauseOverlay.layout(overlayArea);
        gameOverScreen.layout(overlayArea);
        perkPicker.layout(overlayArea);

        layoutReady = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopGameThread();
    }

    // =========================================================================
    // Thread management
    // =========================================================================

    private void startGameThread() {
        if (gameThread != null && gameThread.isAlive()) return;
        gameThread = new GameThread(getHolder(), this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    private void stopGameThread() {
        if (gameThread == null) return;
        gameThread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try { gameThread.join(); retry = false; }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        gameThread = null;
    }

    public void pauseGame()  { stopGameThread(); }

    public void resumeGame() {
        if (getHolder().getSurface().isValid()) startGameThread();
    }

    // =========================================================================
    // Game loop — called by GameThread
    // =========================================================================

    public void update(float dt) {
        if (!layoutReady) return;

        long nowMs = System.currentTimeMillis();

        switch (gsm.getCurrentState()) {
            case TITLE:          titleScreen.update(dt); break;
            case PLAYING:        updatePlaying(dt, nowMs); break;
            case PAUSED:         break;
            case WAVE_PERK_PICK: break;
            case GAME_OVER:      break;
        }
    }

    private void updatePlaying(float dt, long nowMs) {
        GameState gameState = GameState.getInstance();
        if (gameState.isGameOver()) {
            finishRunAndReturnToTitle();
            return;
        }

        float passiveKills = gameState.getPassiveKPS() * dt;
        gameState.addKills(passiveKills);

        // Wave timer
        if (!waitingForInterWave) {
            if (gameState.tickWaveTimer(dt)) {
                waitingForInterWave = true;
                interWaveTimer = Constants.INTER_WAVE_PAUSE_SECONDS;
                sound.playWaveComplete();
            }
        } else {
            interWaveTimer -= dt;
            if (interWaveTimer <= 0f) {
                waitingForInterWave = false;
                gameState.advanceWave();
                perkPicker.prepare();
                gsm.setState(State.WAVE_PERK_PICK);
            }
        }

        enemySpawner.update(dt, enemies);
        UpgradeManager.getInstance().update(nowMs, enemies);

        boolean frozen = UpgradeManager.getInstance().isFrozen(nowMs);
        List<Enemy> dead = new ArrayList<>();
        for (Enemy e : enemies) {
            e.update(dt, gameAreaRect.width(), fieldBottom, frozen);
            if (!e.isAlive()) dead.add(e);
        }
        for (Enemy e : dead) {
            if (e.hasEscaped()) {
                int damage = escapeDamage(e);
                gameState.loseLives(damage);
                hud.triggerEscapeFlash(damage, e.getX(), fieldBottom - 20f);
                sound.playEscape();
                Log.d(TAG, "Escaped. Damage=" + damage
                        + " lives=" + gameState.getLives());
                if (gameState.isGameOver() || !gameState.isGameRunning()) {
                    finishRunAndReturnToTitle();
                    return;
                }
            }
            enemies.remove(e);
        }

        particles.update(dt);
        hud.update(dt);

        // Game over check
        if (gameState.isGameOver() || !gameState.isGameRunning()) {
            finishRunAndReturnToTitle();
        }
    }

    private void finishRunAndReturnToTitle() {
        GameState gameState = GameState.getInstance();
        scoreManager.submitScore(gameState.getKillCount(), gameState.getWaveNumber());
        sound.playGameOver();
        resetToTitle();
    }

    private int escapeDamage(Enemy enemy) {
        switch (enemy.getTier()) {
            case 2: return 2;
            case 3: return 3;
            case 4: return 4;
            case 5: return 5;
            default: return 1;
        }
    }

    // =========================================================================
    // Draw — called by GameThread
    // =========================================================================

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (!layoutReady || canvas == null) return;

        long nowMs = System.currentTimeMillis();

        switch (gsm.getCurrentState()) {
            case TITLE:
                titleScreen.draw(canvas);
                break;

            case PLAYING:
                drawGameplay(canvas, nowMs);
                break;

            case PAUSED:
                drawGameplay(canvas, nowMs);
                pauseOverlay.draw(canvas);
                break;

            case WAVE_PERK_PICK:
                drawGameplay(canvas, nowMs);
                perkPicker.draw(canvas);
                break;

            case GAME_OVER:
                drawGameplay(canvas, nowMs);
                gameOverScreen.draw(canvas);
                break;
        }
    }

    private void drawGameplay(Canvas canvas, long nowMs) {
        drawDojoBackground(canvas);
        drawDojoStructure(canvas);
        hud.draw(canvas);
        for (Enemy e : enemies) e.draw(canvas);
        particles.draw(canvas);
        gestureHandler.draw(canvas);
        if (showHelpOverlay) drawHelpOverlay(canvas);
        drawHelpButton(canvas);
        drawPauseButton(canvas);
        drawBottomBar(canvas, nowMs);
    }

    private void drawDojoBackground(Canvas canvas) {
        float w = gameAreaRect.width();
        float h = fieldBottom;
        float top = gameAreaRect.top;
        float grid = 6f;
        float horizonBottom = top + h * 0.30f;
        float midBottom = top + h * 0.64f;
        float castleBaseY = h - grid * 24f;

        canvas.drawRect(gameAreaRect, gameAreaBgPaint);

        dojoPaint.setStyle(Paint.Style.FILL);
        dojoPaint.setAntiAlias(false);

        // Far battlefield: dark, hazy spawn zone.
        dojoPaint.setColor(Color.parseColor("#25313A"));
        canvas.drawRect(0, top, w, horizonBottom, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#2E3C42"));
        canvas.drawRect(0, top + h * 0.08f, w, top + h * 0.12f, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#344447"));
        canvas.drawRect(0, top + h * 0.20f, w, top + h * 0.24f, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#1D252B"));
        for (int i = 0; i < 6; i++) {
            float x = i * w / 5f - grid * 3f;
            canvas.drawRect(x, horizonBottom - grid * (5 + i % 2), x + grid * 5f, horizonBottom, dojoPaint);
            canvas.drawRect(x + grid, horizonBottom - grid * (8 + i % 3), x + grid * 2f, horizonBottom, dojoPaint);
        }

        // Midground hills and battlefield lanes.
        dojoPaint.setColor(Color.parseColor("#3E5145"));
        canvas.drawRect(0, horizonBottom, w, midBottom, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#4E634D"));
        for (int i = 0; i < 8; i++) {
            float x = i * w / 7f - grid * 4f;
            canvas.drawRect(x, horizonBottom + grid * (3 + i % 2), x + grid * 10f, horizonBottom + grid * (8 + i % 2), dojoPaint);
        }
        dojoPaint.setColor(Color.parseColor("#354738"));
        for (int i = 0; i < 7; i++) {
            float x = i * w / 6f - grid * 5f;
            canvas.drawRect(x, midBottom - grid * (8 + i % 3), x + grid * 13f, midBottom, dojoPaint);
        }

        // Main grass play field.
        dojoPaint.setColor(Color.parseColor("#2F6B3F"));
        canvas.drawRect(0, midBottom, w, h, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#3E7F47"));
        for (int row = 0; row < 9; row++) {
            float y = midBottom + row * grid * 4f;
            for (int col = 0; col < 12; col++) {
                float x = col * w / 12f + ((row % 2) * grid * 2f);
                canvas.drawRect(x, y, x + grid * 2f, y + grid, dojoPaint);
            }
        }
        dojoPaint.setColor(Color.parseColor("#255A35"));
        for (int row = 0; row < 7; row++) {
            float y = midBottom + grid * 2f + row * grid * 5f;
            for (int col = 0; col < 8; col++) {
                float x = col * w / 8f + ((row % 2) * grid * 3f);
                canvas.drawRect(x, y, x + grid, y + grid, dojoPaint);
            }
        }

        // Pixel castle base: player goal at bottom center.
        float castleW = Math.min(w * 0.56f, grid * 46f);
        float castleH = grid * 19f;
        float castleL = w * 0.5f - castleW * 0.5f;
        float castleR = castleL + castleW;
        float towerW = grid * 9f;

        dojoPaint.setColor(Color.parseColor("#283036"));
        canvas.drawRect(castleL - grid, castleBaseY + grid, castleR + grid, castleBaseY + castleH + grid, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#8A9092"));
        canvas.drawRect(castleL + towerW, castleBaseY + grid * 4f, castleR - towerW, castleBaseY + castleH, dojoPaint);
        canvas.drawRect(castleL, castleBaseY, castleL + towerW, castleBaseY + castleH, dojoPaint);
        canvas.drawRect(castleR - towerW, castleBaseY, castleR, castleBaseY + castleH, dojoPaint);
        canvas.drawRect(w * 0.5f - towerW * 0.5f, castleBaseY - grid * 3f, w * 0.5f + towerW * 0.5f, castleBaseY + castleH, dojoPaint);

        dojoPaint.setColor(Color.parseColor("#B8BDBD"));
        canvas.drawRect(castleL, castleBaseY, castleR, castleBaseY + grid * 2f, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#5D666A"));
        for (float x = castleL; x < castleR; x += grid * 3f) {
            canvas.drawRect(x, castleBaseY, x + grid * 1.5f, castleBaseY - grid * 2f, dojoPaint);
        }
        canvas.drawRect(castleL + grid * 2f, castleBaseY + grid * 5f, castleL + grid * 4f, castleBaseY + grid * 8f, dojoPaint);
        canvas.drawRect(castleR - grid * 4f, castleBaseY + grid * 5f, castleR - grid * 2f, castleBaseY + grid * 8f, dojoPaint);
        canvas.drawRect(w * 0.5f - grid * 2f, castleBaseY + grid * 8f, w * 0.5f + grid * 2f, castleBaseY + castleH, dojoPaint);
        dojoPaint.setAntiAlias(true);
    }

    private void drawDojoStructure(Canvas canvas) {
        int dojoLevel = 0;
        for (SkillNode node : UpgradeManager.getInstance().getDojoNodes()) {
            if (node.isOwned()) dojoLevel++;
        }
        if (dojoLevel <= 0) return;

        float w = gameAreaRect.width();
        float h = fieldBottom;
        float grid = 6f;

        float castleW = Math.min(w * 0.56f, grid * 46f);
        float castleL = w * 0.5f - castleW * 0.5f;
        float castleBaseY = h - grid * 24f;
        float castleH = grid * 19f;
        float baseline = castleBaseY + castleH;

        float dojoW = Math.min(w * 0.28f, grid * (21f + dojoLevel));
        float dojoH = grid * (12f + Math.min(dojoLevel, 3) * 0.8f);
        float dojoL = Math.max(grid * 2f, castleL - dojoW - grid * 3f);
        float dojoB = baseline;
        float dojoT = dojoB - dojoH;
        float roofH = grid * 5f;

        dojoPaint.setAntiAlias(false);
        dojoPaint.setStyle(Paint.Style.FILL);

        // Shadow/base block
        dojoPaint.setColor(Color.parseColor("#283036"));
        canvas.drawRect(dojoL - grid, dojoB - grid, dojoL + dojoW + grid, dojoB + grid, dojoPaint);

        // Building body
        dojoPaint.setColor(Color.parseColor("#8B5A35"));
        canvas.drawRect(dojoL, dojoT + roofH, dojoL + dojoW, dojoB, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#A87445"));
        canvas.drawRect(dojoL, dojoT + roofH, dojoL + dojoW, dojoT + roofH + grid * 2f, dojoPaint);

        // Pixel roof, top-left lit
        dojoPaint.setColor(Color.parseColor("#5B2F2F"));
        dojoPath.reset();
        dojoPath.moveTo(dojoL - grid * 2f, dojoT + roofH);
        dojoPath.lineTo(dojoL + dojoW * 0.5f, dojoT);
        dojoPath.lineTo(dojoL + dojoW + grid * 2f, dojoT + roofH);
        dojoPath.close();
        canvas.drawPath(dojoPath, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#7A3E36"));
        canvas.drawRect(dojoL + grid, dojoT + roofH - grid, dojoL + dojoW - grid, dojoT + roofH, dojoPaint);

        if (dojoLevel >= 3) {
            dojoPaint.setColor(Color.parseColor("#F6D06F"));
            canvas.drawRect(dojoL + grid * 2f, dojoT + roofH - grid * 2f,
                    dojoL + dojoW - grid * 2f, dojoT + roofH - grid, dojoPaint);
        }

        // Door and blocky wall texture
        dojoPaint.setColor(Color.parseColor("#3A2420"));
        canvas.drawRect(dojoL + dojoW * 0.40f, dojoB - grid * 4f,
                dojoL + dojoW * 0.60f, dojoB, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#6F472D"));
        for (int i = 0; i < 3; i++) {
            float y = dojoT + roofH + grid * (2f + i * 1.5f);
            canvas.drawRect(dojoL + grid, y, dojoL + dojoW - grid, y + grid * 0.5f, dojoPaint);
        }

        // Progress banner with one mark per dojo upgrade.
        float bannerW = grid * 3f;
        float bannerL = dojoL + dojoW - bannerW - grid;
        float bannerT = dojoT + roofH + grid;
        float bannerB = dojoB - grid;
        dojoPaint.setColor(Color.parseColor("#B83232"));
        canvas.drawRect(bannerL, bannerT, bannerL + bannerW, bannerB, dojoPaint);
        dojoPaint.setColor(Color.parseColor("#F6D06F"));
        canvas.drawRect(bannerL, bannerT, bannerL + bannerW, bannerT + grid * 0.7f, dojoPaint);

        int marks = Math.min(dojoLevel, 5);
        float usableH = bannerB - bannerT - grid * 2f;
        for (int i = 0; i < marks; i++) {
            float markY = bannerT + grid * 1.4f + i * (usableH / Math.max(1, marks));
            canvas.drawRect(bannerL + grid * 0.6f, markY,
                    bannerL + bannerW - grid * 0.6f, markY + grid * 0.45f, dojoPaint);
        }

        if (dojoLevel >= 4) {
            dojoPaint.setColor(Color.parseColor("#B8BDBD"));
            canvas.drawRect(dojoL + grid, dojoB - grid * 1.5f, dojoL + dojoW - grid, dojoB - grid, dojoPaint);
        }

        dojoPaint.setAntiAlias(true);
    }

    private void drawHelpButton(Canvas canvas) {
        drawPixelPanel(canvas, helpButtonRect, pauseBtnBg);
        canvas.drawText("?",
                helpButtonRect.centerX(),
                helpButtonRect.centerY() + helpButtonRect.height() * 0.3f,
                pauseBtnTxt);
    }

    private void drawPauseButton(Canvas canvas) {
        drawPixelPanel(canvas, pauseButtonRect, pauseBtnBg);
        canvas.drawText("II",
                pauseButtonRect.centerX(),
                pauseButtonRect.centerY() + pauseButtonRect.height() * 0.3f,
                pauseBtnTxt);
    }

    private void drawHelpOverlay(Canvas canvas) {
        float overlayW = gameAreaRect.width() * 0.62f;
        float overlayH = gameAreaRect.height() * 0.38f;
        float overlayTop = gameAreaRect.height() * Constants.HUD_TOP_BAR_RATIO + 16f;
        helpOverlayRect.set(
                gameAreaRect.centerX() - overlayW / 2f,
                overlayTop,
                gameAreaRect.centerX() + overlayW / 2f,
                overlayTop + overlayH
        );

        drawPixelPanel(canvas, helpOverlayRect, helpOverlayBgPaint);
        canvas.drawText("GESTURES",
                helpOverlayRect.centerX(),
                helpOverlayRect.top + overlayH * 0.24f,
                helpOverlayTitlePaint);
        canvas.drawText("Swipe Right -> Circle",
                helpOverlayRect.centerX(),
                helpOverlayRect.top + overlayH * 0.34f,
                helpOverlayTextPaint);
        canvas.drawText("Draw a Loop -> Square",
                helpOverlayRect.centerX(),
                helpOverlayRect.top + overlayH * 0.48f,
                helpOverlayTextPaint);
        canvas.drawText("Swipe Down -> Triangle",
                helpOverlayRect.centerX(),
                helpOverlayRect.top + overlayH * 0.62f,
                helpOverlayTextPaint);
        canvas.drawText("Swipe Left -> Star",
                helpOverlayRect.centerX(),
                helpOverlayRect.top + overlayH * 0.76f,
                helpOverlayTextPaint);
        canvas.drawText("Up, then Down -> Lightning",
                helpOverlayRect.centerX(),
                helpOverlayRect.top + overlayH * 0.90f,
                helpOverlayTextPaint);
    }

    private void drawBottomBar(Canvas canvas, long nowMs) {
        float barTop = fieldBottom;
        float barH   = gameAreaRect.height() - barTop;
        canvas.drawRect(0, barTop, gameAreaRect.width(), gameAreaRect.height(), bottomBarBgPaint);

        // UPGRADES button
        drawPixelPanel(canvas, upgradesButtonRect, upgradesBtnBg);
        drawFittedText(canvas, "UPGRADES",
                upgradesButtonRect.centerX(),
                upgradesButtonRect.centerY() + upgradesButtonRect.height() * 0.3f,
                upgradesButtonRect.width() - 8f,
                upgradesBtnTxt,
                upgradesButtonRect.height() * 0.34f);

        // Shield indicator is hidden to keep the bottom HUD lanes clear.
        if (false && GameState.getInstance().isShieldActive()) {
            Paint shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shieldPaint.setColor(Color.CYAN);
            shieldPaint.setTextSize(barH * 0.42f);
            shieldPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("🛡", 12f, barTop + barH * 0.75f, shieldPaint);
        }

        // Ability cooldown icons (bottom center)
        List<SpecialAbility> abilities = UpgradeManager.getInstance().getAbilities();
        AbilityType[] types = AbilityType.values();
        for (int i = 0; i < 4 && i < abilities.size(); i++) {
            SpecialAbility ab = abilities.get(i);
            RectF r = abilityIconRects[i];

            int iconColor = abilityIconColor(types[i]);
            abilityIconPaint.setColor(ab.isUnlocked() ? iconColor : 0xFF555555);
            drawPixelPanel(canvas, r, abilityIconPaint);

            canvas.drawText(abilityLabel(types[i]),
                    r.centerX(), r.centerY() + r.height() * 0.3f, abilityIconTxt);

            // Cooldown overlay
            if (ab.isUnlocked()) {
                float prog = ab.getCooldownProgress(nowMs);
                if (prog < 1f) {
                    RectF overlay = new RectF(r.left, r.top, r.right,
                                             r.top + r.height() * (1f - prog));
                    canvas.drawRect(overlay, abilityCooldownOverlay);
                    canvas.drawRect(r, pixelBorderPaint);
                }
            }
        }

        drawFittedText(canvas, String.format(Locale.US, "Passive +%.1f/s",
                        GameState.getInstance().getPassiveKPS()),
                gameAreaRect.right - 12f,
                barTop + barH * 0.64f,
                gameAreaRect.width() * 0.24f,
                bottomInfoTxt,
                barH * 0.26f);
    }

    private void drawPixelPanel(Canvas canvas, RectF rect, Paint fillPaint) {
        canvas.drawRect(rect, fillPaint);
        canvas.drawRect(rect, pixelBorderPaint);
    }

    private void drawFittedText(Canvas canvas, String text, float x, float y,
                                float maxWidth, Paint paint, float baseSize) {
        paint.setTextSize(baseSize);
        while (paint.measureText(text) > maxWidth && paint.getTextSize() > 10f) {
            paint.setTextSize(paint.getTextSize() - 1f);
        }
        canvas.drawText(text, x, y, paint);
    }

    private int abilityIconColor(AbilityType t) {
        switch (t) {
            case FREEZE:    return Color.CYAN;
            case BOMB:      return Color.RED;
            case TIME_SLOW: return Color.YELLOW;
            case SHIELD:    return Color.GRAY;
            default:        return Color.WHITE;
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

    // =========================================================================
    // Touch input
    // =========================================================================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!layoutReady) return true;

        float x = event.getX(), y = event.getY();
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_UP
                && gsm.getCurrentState() != State.TITLE
                && helpButtonRect.contains(x, y)) {
            showHelpOverlay = !showHelpOverlay;
            sound.playButtonTap();
            return true;
        }

        switch (gsm.getCurrentState()) {

            case TITLE:
                if (action == MotionEvent.ACTION_UP) startNewGame();
                break;

            case PLAYING:
                if (action == MotionEvent.ACTION_UP && pauseButtonRect.contains(x, y)) {
                    gsm.setState(State.PAUSED);
                    sound.playButtonTap();
                } else if (action == MotionEvent.ACTION_UP && handleAbilityTap(x, y)) {
                    sound.playButtonTap();
                } else if (action == MotionEvent.ACTION_UP && upgradesButtonRect.contains(x, y)) {
                    // Pause game loop and launch UpgradeActivity (spec §2)
                    gsm.setState(State.PAUSED);
                    sound.playButtonTap();
                    if (upgradeListener != null) upgradeListener.onUpgradesTapped();
                } else if (!showHelpOverlay && y < fieldBottom) {
                    // Gesture input only in the enemy field (above bottom bar)
                    gestureHandler.onTouchEvent(event, enemies);
                }
                break;

            case PAUSED:
                if (action == MotionEvent.ACTION_UP) {
                    if (pauseOverlay.tapResume(x, y) || pauseButtonRect.contains(x, y)) {
                        gsm.setState(State.PLAYING);
                        sound.playButtonTap();
                    } else if (pauseOverlay.tapQuit(x, y)) {
                        resetToTitle();
                        sound.playButtonTap();
                    }
                }
                break;

            case WAVE_PERK_PICK:
                if (action == MotionEvent.ACTION_UP) {
                    Object picked = perkPicker.handleTap(x, y);
                    if (picked != null) {
                        perkPicker.applySelection(picked, GameState.getInstance());
                        sound.playPerkSelect();
                        gsm.setState(State.PLAYING);
                    }
                }
                break;

            case GAME_OVER:
                if (action == MotionEvent.ACTION_UP) {
                    if (gameOverScreen.tapPlayAgain(x, y)) {
                        startNewGame();
                        sound.playButtonTap();
                    } else if (gameOverScreen.tapMainMenu(x, y)) {
                        resetToTitle();
                        sound.playButtonTap();
                    }
                }
                break;
        }

        return true;
    }

    private boolean handleAbilityTap(float x, float y) {
        AbilityType[] types = AbilityType.values();
        long nowMs = System.currentTimeMillis();
        for (int i = 0; i < 4 && i < types.length; i++) {
            if (abilityIconRects[i].contains(x, y)) {
                return UpgradeManager.getInstance().activateAbility(types[i], nowMs, enemies);
            }
        }
        return false;
    }

    // =========================================================================
    // Game flow helpers
    // =========================================================================

    private void startNewGame() {
        GameState.getInstance().resetGame();
        UpgradeManager.getInstance().reset();
        enemies.clear();
        waitingForInterWave = false;
        showHelpOverlay = false;
        perkPicker = new WavePerkPicker(UpgradeManager.getInstance());
        perkPicker.layout(new RectF(0,
                gameAreaRect.height() * Constants.HUD_TOP_BAR_RATIO,
                gameAreaRect.width(), fieldBottom));
        gsm.setState(State.PLAYING);
    }

    private void resetToTitle() {
        GameState.getInstance().resetGame();
        UpgradeManager.getInstance().reset();
        enemies.clear();
        waitingForInterWave = false;
        showHelpOverlay = false;
        perkPicker = new WavePerkPicker(UpgradeManager.getInstance());
        perkPicker.layout(new RectF(0,
                gameAreaRect.height() * Constants.HUD_TOP_BAR_RATIO,
                gameAreaRect.width(), fieldBottom));
        gsm.setState(State.TITLE);
    }

    /** Called by GameActivity when returning from UpgradeActivity. */
    public void resumeFromUpgrades() {
        if (gsm.is(State.PAUSED)) {
            gsm.setState(State.PLAYING);
        }
    }
}
