package com.example.karateslicer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Entry point for Karate Slicer (spec §12 — replaces MainActivity).
 * Sets up full-screen GameView. When the player taps UPGRADES, the game
 * pauses and UpgradeActivity opens. On return, the game loop resumes.
 */
public class GameActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        gameView = new GameView(this);
        gameView.setUpgradeListener(() ->
                startActivity(new Intent(GameActivity.this, UpgradeActivity.class)));
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeGame();
        // Restore PLAYING state if we returned from UpgradeActivity (spec §2)
        gameView.resumeFromUpgrades();
    }
}
