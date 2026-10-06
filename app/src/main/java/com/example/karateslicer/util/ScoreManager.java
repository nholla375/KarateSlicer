package com.example.karateslicer.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists high score and highest wave via SharedPreferences (spec §11). */
public class ScoreManager {

    private static final String PREFS_NAME   = "karate_slicer";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_HIGH_WAVE  = "highest_wave";

    private final SharedPreferences prefs;

    public ScoreManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getHighScore() { return prefs.getInt(KEY_HIGH_SCORE, 0); }
    public int getHighWave()  { return prefs.getInt(KEY_HIGH_WAVE,  0); }

    /**
     * Submit a run's results. Saves if they beat the stored records.
     * @return true if a new high score was set
     */
    public boolean submitScore(int kills, int wave) {
        boolean newRecord = false;
        SharedPreferences.Editor edit = prefs.edit();
        if (kills > getHighScore()) { edit.putInt(KEY_HIGH_SCORE, kills); newRecord = true; }
        if (wave  > getHighWave())  { edit.putInt(KEY_HIGH_WAVE,  wave);  }
        edit.apply();
        return newRecord;
    }
}
