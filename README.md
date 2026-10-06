# KarateSlicer

A gesture-controlled Android arcade game built in Java. Instead of tapping attack buttons, the player draws gestures directly on the screen to defeat incoming enemies while managing waves, upgrades, movement patterns, and score pressure.

The project is built around a custom `SurfaceView` game loop and a lightweight rule-based gesture recognizer rather than a full game engine.

## Demo

A short gameplay GIF will be added here. See [`docs/demo`](docs/demo/README.md) for the capture plan.

<!-- After recording, uncomment:
![KarateSlicer gameplay demo](docs/demo/karate-slicer-demo.gif)
-->

## Screenshots

Recommended screenshots and filenames are documented in [`docs/screenshots`](docs/screenshots/README.md).

<!-- After adding screenshots, uncomment:
| Title | Gameplay | Upgrades |
| --- | --- | --- |
| ![](docs/screenshots/title.png) | ![](docs/screenshots/gameplay.png) | ![](docs/screenshots/upgrades.png) |
-->

## Core Gameplay

Enemies descend through the play area with a displayed shape. The player traces a gesture over an enemy; the game captures the stroke, classifies it using geometric rules, checks whether that gesture matches the enemy, and resolves the hit.

The current recognizer maps these implemented gestures:

| Player gesture | Enemy type |
| --- | --- |
| Swipe right | Circle |
| Closed loop | Square |
| Swipe down | Triangle |
| Swipe left | Star |

The `LIGHTNING` enemy type exists in the game model, while its intended higher-tier gesture is part of the design roadmap rather than the current recognizer.

## Technical Highlights

### Custom game loop

`GameThread` runs the update/render cycle on a dedicated thread and targets approximately 60 FPS. It computes delta time, caps unusually large frame gaps, updates game state, locks the `SurfaceView` canvas, renders the frame, and sleeps for the remaining frame budget.

This keeps frame progression independent of ordinary Android Activity UI updates.

### Rule-based gesture recognition

`GestureRecognizer` works from raw `PointF` stroke samples and uses simple geometric features rather than a machine-learning model:

- total stroke length filters accidental taps;
- net X/Y displacement identifies directional swipes;
- an axis-dominance ratio rejects heavily diagonal strokes;
- endpoint distance divided by total stroke length detects closed loops.

The recognizer is deliberately small and interpretable, which made it useful for tuning game feel and understanding false positives.

### Strategy-style enemy movement

Enemy motion is separated behind a `MovementPattern` interface. Implementations include:

- `StraightMovement`
- `ZigzagMovement`
- `BurstMovement`
- `HomingMovement`
- `VerticalOscillationMovement`

This lets the spawner/game logic vary enemy behavior without putting every movement algorithm into the `Enemy` class.

### Game-state and progression systems

The prototype includes:

- title / playing / paused / perk-pick / game-over states
- wave progression and difficulty scaling
- enemy spawning and escape penalties
- combat and survival upgrade branches
- special abilities
- HUD and upgrade UI
- particle effects and floating score text
- local high-score persistence with `SharedPreferences`
- audio hooks through `SoundManager`

## Architecture

```text
GameActivity
   └── GameView (SurfaceView)
        ├── GameThread
        ├── GameState / GameStateManager
        ├── EnemySpawner
        │    └── Enemy
        │         └── MovementPattern
        ├── GestureHandler
        │    └── GestureRecognizer
        ├── UpgradeManager
        └── UI / effects
             ├── GameHUD
             ├── PauseOverlay
             ├── TitleScreen
             ├── GameOverScreen
             ├── WavePerkPicker
             ├── UpgradePanel
             ├── ParticleSystem
             └── FloatingText
```

## Project Structure

```text
app/src/main/java/com/example/karateslicer/
├── GameActivity.java
├── GameThread.java
├── GameView.java
├── UpgradeActivity.java
├── audio/
├── enemy/
│   └── movement/
├── gesture/
├── state/
├── ui/
├── upgrade/
└── util/

docs/
├── ORIGINAL_DESIGN_SPEC.md
├── demo/
└── screenshots/
```

## Tech Stack

- Java 11
- Android SDK
- `SurfaceView` / `Canvas`
- Android touch input (`MotionEvent` / `PointF`)
- `SharedPreferences`
- Android Activities and XML layouts
- JUnit / AndroidX instrumentation testing

No external game engine is used.

## Gesture Recognition Flow

```text
Touch down / move / up
        ↓
GestureHandler collects PointF samples
        ↓
Reject stroke if total length is too short
        ↓
Check directional / closed-loop geometry
        ↓
GestureRecognizer returns ShapeType
        ↓
Hit detection + enemy match
        ↓
Kill / feedback / score update
```

The gesture thresholds are kept in `gesture/GestureConfig.java` so recognition can be tuned without rewriting the classifier.

## Run Locally

### Requirements

- Android Studio
- Android SDK 29+
- JDK 11+
- Emulator or Android device

Open the project in Android Studio, let Gradle sync complete, and run the `app` configuration.

From a terminal:

```bash
./gradlew assembleDebug
```

## Tests

The Android instrumentation tests include synthetic strokes for the implemented recognizer:

- right swipe → circle
- closed loop → square
- downward swipe → triangle
- left swipe → star
- short stroke → unknown

Run them with a connected device/emulator:

```bash
./gradlew connectedAndroidTest
```

## Design Roadmap

[`docs/ORIGINAL_DESIGN_SPEC.md`](docs/ORIGINAL_DESIGN_SPEC.md) preserves the larger design document used while planning the game. It includes additional gesture ideas and later-stage systems. It is intentionally labeled as a roadmap because not every planned mechanic is part of the current prototype.

Keeping the document separate from the implemented feature list makes it possible to compare design intent with engineering tradeoffs and future work.

## Future Improvements

- Implement and test the intended fifth/high-tier gesture
- Expand the recognizer with resampling and direction-change features
- Add a larger synthetic/noisy gesture test corpus
- Add frame/update performance instrumentation
- Improve lifecycle handling around thread pause/resume edge cases
- Add audio assets to the existing `SoundManager` hooks
- Add accessibility and onboarding instructions for gesture mappings

## What This Project Demonstrates

KarateSlicer was an exercise in building interactive systems below the level of standard Android forms: frame timing, touch sampling, geometry-based input classification, composable movement algorithms, state machines, progression systems, rendering, and game feedback all have to cooperate in real time.
