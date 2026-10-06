package com.example.karateslicer.upgrade;

public enum AbilityType {
    FREEZE,     // all enemies stop moving for 3 seconds
    BOMB,       // all enemies on screen instantly killed (no kill award)
    TIME_SLOW,  // enemies at 30% speed for 5 seconds
    SHIELD      // next escape does not trigger kill penalty
}
