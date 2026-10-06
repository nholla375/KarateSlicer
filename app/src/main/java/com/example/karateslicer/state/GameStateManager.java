package com.example.karateslicer.state;

/** Manages transitions between top-level game screens (spec §3.1). */
public class GameStateManager {

    public enum State {
        TITLE,
        PLAYING,
        PAUSED,
        WAVE_PERK_PICK,
        GAME_OVER
    }

    private State current = State.TITLE;

    public State getCurrentState() { return current; }

    public void setState(State next) { current = next; }

    public boolean is(State s) { return current == s; }
}
