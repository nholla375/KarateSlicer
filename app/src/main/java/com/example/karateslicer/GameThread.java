package com.example.karateslicer;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * GameThread is the heartbeat of the game.
 * It runs on a background thread so the game loop never blocks the Android UI thread.
 *
 * The loop uses a fixed timestep approach:
 *   - It tracks how long each frame actually took (deltaTime)
 *   - It passes deltaTime to update() so movement is frame-rate independent
 *   - It caps deltaTime to avoid a huge jump if the app is backgrounded
 *
 * To adjust target frame rate: change TARGET_FPS below.
 */
public class GameThread extends Thread {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Desired frames per second. 60 is standard for mobile games. */
    private static final int TARGET_FPS = 60;

    /** Time budget per frame in nanoseconds. */
    private static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;

    /**
     * Maximum delta we allow in seconds.
     * If the app was paused and resumed, we don't want enemies to teleport.
     */
    private static final float MAX_DELTA_SECONDS = 0.05f; // = ~3 missed frames

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Controlled by GameView to start/stop the loop. */
    private volatile boolean running = false;

    /** The SurfaceHolder gives us access to the Canvas. */
    private final SurfaceHolder surfaceHolder;

    /** The view that owns update() and draw() logic. */
    private final GameView gameView;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView      = gameView;
        setName("GameThread"); // visible in Android Studio's thread monitor
    }

    // -----------------------------------------------------------------------
    // Control
    // -----------------------------------------------------------------------

    public void setRunning(boolean running) {
        this.running = running;
    }

    // -----------------------------------------------------------------------
    // Loop
    // -----------------------------------------------------------------------

    @Override
    public void run() {
        long previousTimeNs = System.nanoTime();

        while (running) {
            long currentTimeNs = System.nanoTime();
            long elapsedNs     = currentTimeNs - previousTimeNs;
            previousTimeNs     = currentTimeNs;

            // Convert to seconds, capped so a long pause doesn't cause issues
            float deltaSeconds = Math.min(elapsedNs / 1_000_000_000f, MAX_DELTA_SECONDS);

            // --- Update game logic ---
            gameView.update(deltaSeconds);

            // --- Draw ---
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    // Synchronize on the holder so the surface isn't destroyed mid-draw
                    synchronized (surfaceHolder) {
                        gameView.draw(canvas);
                    }
                }
            } finally {
                // Always unlock, even if an exception occurred
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            // --- Sleep to maintain target FPS ---
            long frameEndNs   = System.nanoTime();
            long frameTookNs  = frameEndNs - currentTimeNs;
            long sleepNs      = FRAME_TIME_NS - frameTookNs;

            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}