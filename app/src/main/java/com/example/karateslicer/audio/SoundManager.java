package com.example.karateslicer.audio;

import android.content.Context;
import com.example.karateslicer.upgrade.AbilityType;

/**
 * SoundManager stubs all audio event slots.
 * Wire each method to SoundPool / MediaPlayer assets when audio is added.
 */
public class SoundManager {

    private static SoundManager instance;

    private SoundManager(Context context) {
        // SoundPool initialization goes here
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) instance = new SoundManager(context);
        return instance;
    }

    public void playKill()                          { /* TODO */ }
    public void playWrongGesture()                  { /* TODO */ }
    public void playEscape()                        { /* TODO */ }
    public void playAbility(AbilityType type)       { /* TODO */ }
    public void playWaveComplete()                  { /* TODO */ }
    public void playPerkSelect()                    { /* TODO */ }
    public void playGameOver()                      { /* TODO */ }
    public void playButtonTap()                     { /* TODO */ }
}
