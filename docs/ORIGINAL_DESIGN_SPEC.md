> **Design-roadmap note:** This document captures the original design direction for KarateSlicer. The current prototype implements the core game loop, enemy systems, upgrades, persistence, and a simpler rule-based gesture set. Some gesture shapes and late-stage roadmap ideas described below are not yet implemented. See the root README for the current feature set.

# Karate Slicer — Game Design & Technical Specification

**Platform:** Android (Native Java, Android Studio)
**Version:** 1.1 Prototype Spec (updated: dual-screen layout, reworked gestures)
**Orientation:** Portrait only
**Min SDK:** API 26 (Android 8.0 Oreo)
**Target SDK:** API 34+

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Screen Layout](#2-screen-layout)
3. [Game States & Screens](#3-game-states--screens)
4. [Enemy System](#4-enemy-system)
5. [Gesture Recognition System](#5-gesture-recognition-system)
6. [Combat System](#6-combat-system)
7. [Wave System & Difficulty Scaling](#7-wave-system--difficulty-scaling)
8. [Upgrade & Skill Tree System](#8-upgrade--skill-tree-system)
9. [Currency & Economy](#9-currency--economy)
10. [Audio System](#10-audio-system)
11. [Persistence & High Score](#11-persistence--high-score)
12. [Class Architecture](#12-class-architecture)
13. [Constants Reference](#13-constants-reference)
14. [Development Phases](#14-development-phases)

---

## 1. Project Overview

Karate Slicer is a 2D arcade game in which enemies descend from the top of the screen and the player defeats them by drawing the correct freehand shape over each enemy using touch gestures. The game runs endlessly, tracking waves in the background to trigger bonus perk selections. Difficulty scales with wave progression through speed, enemy count, tier unlocks, and new movement behaviors.

---

## 2. Screen Layout

### Orientation
Portrait only. Lock to portrait in `AndroidManifest.xml`:
```xml
android:screenOrientation="portrait"
```

### Dual-Screen Architecture
The game uses **two separate full-screen Activities** instead of a split layout. This gives the full screen to gameplay and gives upgrades their own uncluttered space.

| Screen | Activity Class | Content |
|---|---|---|
| Gameplay Screen | `GameActivity` | Full-screen SurfaceView — enemy field, gesture drawing, HUD overlay |
| Upgrade Screen | `UpgradeActivity` | Full-screen XML scrollable layout — skill tree, ability buttons, stats |

### Gameplay Screen (GameActivity) Layout
The entire screen is the enemy field. A lightweight HUD is drawn on top via Canvas:
- **Top bar** (~8% of screen height): wave number (left), kill count (center), pause button (right, ~48×48dp)
- **Middle** (~84% of screen height): active enemy field — enemies spawn at the top and descend; all gesture input is captured here
- **Bottom bar** (~8% of screen height): "UPGRADES" button (center-bottom, tappable) + active shield indicator (left) + active ability cooldown icons (right)

The "UPGRADES" button pauses the game and launches `UpgradeActivity`. The game resumes automatically when the player returns.

### Upgrade Screen (UpgradeActivity) Layout
A standard Android XML layout using `ScrollView` so the tree can grow without clipping. Content top to bottom:
1. Back arrow / "← BACK TO GAME" button — returns to `GameActivity` and resumes
2. Kill count display (large, prominent, read from shared `GameState`)
3. Wave number display
4. Horizontal divider
5. **Combat Branch** section header + 4 skill node buttons (vertical list)
6. **Survival Branch** section header + 4 skill node buttons (vertical list)
7. Horizontal divider
8. **Special Abilities** section — 2×2 grid of ability buttons with cooldown state

Upgrade purchases are reflected immediately in the shared `GameState` singleton so they take effect the moment the player returns to gameplay.

### Navigation Flow
```
GameActivity (playing) → tap "UPGRADES" → game pauses → UpgradeActivity opens
UpgradeActivity → tap "← BACK" → UpgradeActivity closes → GameActivity resumes
```
Use `startActivity` / `finish()` pattern. `GameState` is a singleton so no data needs to be passed via Intent.

---

## 3. Game States & Screens

The game cycles through the following states, managed by a `GameStateManager` class:

### 3.1 States Enum
```
TITLE → PLAYING → PAUSED (→ UpgradeActivity) → WAVE_PERK_PICK → GAME_OVER → TITLE
```

### 3.2 Title Screen
- Drawn on `SurfaceView` (no XML layout needed)
- Shows: game title ("KARATE SLICER"), high score, "TAP TO START" prompt
- Simple animated background (enemies slowly drifting downward as decoration, no interaction)
- Tapping anywhere transitions to `PLAYING`

### 3.3 Playing State
- Normal gameplay loop active
- Pause button: top-right corner of screen (~48×48dp)
- "UPGRADES" button: bottom-center of screen — pauses the game loop and launches `UpgradeActivity`
- On return from `UpgradeActivity`, game loop resumes from `PLAYING` state

### 3.4 Paused State
- Game loop continues ticking but no enemy movement, no gesture input registered
- Overlay drawn on top of the gameplay area: semi-transparent dark rectangle + "PAUSED" text
- Two buttons: **Resume** and **Quit to Title**
- Pause button tapped again → Resume

### 3.5 Wave Perk Pick Screen
- Triggered automatically when a wave completion threshold is reached (see §7)
- Gameplay pauses (enemies freeze in place)
- Overlay presents **3 randomly selected upgrade cards** from the full upgrade pool
- Player taps one card to apply it for free
- If the player already owns an upgrade, it shows as an upgrade to the next level instead of a duplicate
- After selection, transitions back to `PLAYING`

### 3.6 Game Over Screen
- Triggered when kills drop below zero (see §9 for penalty rules)
- Shows:
  - "GAME OVER" title
  - Final kill count
  - Highest wave reached
  - Whether a new high score was achieved (highlighted)
- Two buttons: **Play Again** (resets all state) and **Main Menu**
- High score saved to `SharedPreferences` on entry to this screen

---

## 4. Enemy System

### 4.1 Spawn Behavior
- Enemies spawn at the **top edge** of the gameplay area, at a random X position within the gameplay area bounds
- A minimum horizontal gap between spawn points prevents overlapping spawns
- Spawn interval decreases as waves progress (see §7)
- Maximum concurrent enemies on screen: starts at **1**, scales up to **5** via the Spawn Upgrade (see §8)

### 4.2 Movement Patterns
Enemies gain new movement patterns at higher tiers. Each pattern is a self-contained behavior class:

| Pattern | Available From | Description |
|---|---|---|
| Straight | Tier 1 | Moves straight downward at constant speed |
| Zigzag | Tier 3 | Oscillates horizontally while descending |
| Burst | Tier 4 | Moves at normal speed, periodically surges downward for 0.3s |
| Homing-lite | Tier 5 | Gradually drifts toward center-X of gameplay area while descending |

At any given tier, enemies use the patterns unlocked up to and including that tier. Higher tiers are weighted more heavily at later waves.

### 4.3 Enemy Tiers & Visual Shapes

Each tier has a distinct Canvas-drawn shape. Color also differentiates tiers.

| Tier | Shape | Color | Required Gesture | Movement Patterns Available |
|---|---|---|---|---|
| 1 | Circle | Green | **Swipe Right** — single horizontal stroke, left to right | Straight |
| 2 | Square | Blue | **Circle / Loop** — any closed loop stroke (ends meet start) | Straight |
| 3 | Triangle | Orange | **V-shape** — stroke goes diagonally down-left, then pivots diagonally down-right | Straight, Zigzag |
| 4 | Star (5-point) | Red | **Z-shape** — stroke goes right, diagonally down-left, then right again | Straight, Zigzag, Burst |
| 5 | Lightning bolt | Purple | **Scratch** — rapid back-and-forth horizontal scrubbing (3+ direction reversals) | All patterns |

> **Note:** Tiers 1–2 are available at game start. Tiers 3–5 are unlocked via the Enemy Tier Unlock upgrade (see §8).

### 4.4 Enemy Properties (per instance)

| Property | Type | Description |
|---|---|---|
| `tier` | int | 1–5 |
| `shape` | ShapeType enum | CIRCLE, SQUARE, TRIANGLE, STAR, LIGHTNING |
| `x`, `y` | float | Current position (center point) |
| `speed` | float | Base pixels/second downward, scaled by wave |
| `movementPattern` | MovementPattern | Current behavior instance |
| `isAlive` | boolean | False when killed or off-screen |
| `hitFlashTimer` | int | Counts down for shake/flash feedback frames |
| `size` | float | Radius or half-width in pixels |

### 4.5 Floating Shape Icon
- Each enemy has a small icon drawn directly above it (not on it)
- The icon is a miniature version of the required gesture shape
- Icon offset: `enemy.y - enemy.size - 20px`
- Icon size: approximately 30×30px
- Icon color: white with a dark outline for readability on any background

### 4.6 Off-Screen Penalty
- An enemy is considered "escaped" when its `y` position exceeds the bottom of the gameplay area
- On escape: deduct **10 kills** from the player's kill count (see §9)
- A brief red flash or text ("–10") appears at the bottom of the gameplay area
- The enemy is removed from the active list

---

## 5. Gesture Recognition System

### 5.1 Design Philosophy
The gesture system prioritizes **forgiveness over precision**. Each gesture is chosen to be maximally distinct from all others based on a small number of easily measurable features. The recognizer uses only 3–4 key signals per gesture so there is minimal ambiguity even with messy real-finger input.

### 5.2 Input Capture
- All touch events are handled in `GameActivity` / `GameView.onTouchEvent()`
- Touch events are only processed during the `PLAYING` state
- Gesture input is valid across the **entire screen** (full-screen gameplay)

### 5.3 Stroke Recording
- On `ACTION_DOWN`: begin recording touch point list, clear previous stroke
- On `ACTION_MOVE`: append each point to the stroke list
- On `ACTION_UP`: finalize stroke, run recognition, clear stroke list

### 5.4 Hit Detection — Does the Stroke Touch an Enemy?
- After the stroke is finalized, check which enemy (if any) the stroke passed through
- A stroke "crosses" an enemy if **any recorded point** falls within the enemy's bounding circle/box (use the actual drawn shape bounds, not a fixed radius)
- If the stroke crosses multiple enemies, only the **first enemy crossed** (earliest point of contact along the stroke) is targeted
- If the stroke crosses no enemy, the gesture is discarded silently

### 5.5 The Five Gestures

Each gesture is recognized by a primary feature that is unique to it. The recognizer tests gestures in order of simplest → most complex and returns the first match.

---

#### Gesture 1 — Swipe Right (Tier 1)
**How to draw:** Drag finger left to right in a mostly horizontal line.

**Recognition rule:**
- Net X displacement > `MIN_SWIPE_DISTANCE_PX` (e.g. 150px)
- `|netX| / |netY| > SWIPE_AXIS_RATIO` (e.g. 2.5) — confirms it is more horizontal than diagonal
- Net X direction is positive (left to right)

**Why it's easy:** A single straight drag. No corners, no loops. Almost impossible to misfire on intent.

---

#### Gesture 2 — Circle / Loop (Tier 2)
**How to draw:** Draw any closed loop — oval, wobbly circle, rounded square, anything that ends near where it started.

**Recognition rule:**
- Stroke length > `MIN_STROKE_LENGTH_PX` (e.g. 200px)
- Closedness ratio: `distanceFromStartToEnd / totalStrokeLength < CLOSEDNESS_THRESHOLD` (e.g. 0.25)
- This threshold is intentionally generous — the stroke does **not** need to be a clean circle

**Why it's easy:** The shape does not matter at all. Any loop that ends near its start qualifies. A wobbly oval or lopsided circle both pass.

---

#### Gesture 3 — V-shape (Tier 3)
**How to draw:** Draw a V — stroke goes down-and-left, then pivots, then goes down-and-right (or vice versa).

**Recognition rule:**
- Not closed (closedness ratio > `CLOSEDNESS_THRESHOLD`)
- Exactly **1 corner** detected (a single direction-change angle exceeding `CORNER_ANGLE_THRESHOLD_DEG`, e.g. 70°)
- Both arms of the V trend downward (both have positive Y component)
- The two arm directions are roughly symmetric about the vertical axis (within ±40°)

**Why it's easy:** A natural V or checkmark stroke. The recognizer only cares that there is one clear turn with both sides going downward.

---

#### Gesture 4 — Z-shape (Tier 4)
**How to draw:** Draw a Z — horizontal stroke right, diagonal down-left, horizontal stroke right again.

**Recognition rule:**
- Not closed
- Exactly **2 corners** detected
- The 3 resulting stroke segments have directions that follow a right → down-left → right sequence (each segment's net direction tested against expected quadrants, within a ±45° tolerance)

**Why it's easy:** A Z is a natural, practiced letter stroke. The 3-segment structure is very distinct from all other gestures. Does not require the horizontals to be perfectly level.

---

#### Gesture 5 — Scratch (Tier 5)
**How to draw:** Rapidly scrub finger back and forth horizontally, at least 3 times.

**Recognition rule:**
- Not closed
- **3 or more horizontal direction reversals** detected (track when net X direction flips sign; each flip = 1 reversal)
- Average segment between reversals is predominantly horizontal (`|segmentX| / |segmentY| > 1.5`)

**Why it's easy:** The back-and-forth scrubbing motion is instinctive and fun. It is also completely unmistakeable — no other gesture produces 3+ direction reversals.

---

### 5.6 Recognition Order & Fallback

The recognizer tests in this order:
1. **Scratch** — check for 3+ reversals first (most distinctive, must be caught before corner count inflates)
2. **Circle** — check closedness
3. **Swipe Right** — check net horizontal direction + axis ratio
4. **Z-shape** — check for exactly 2 corners with correct segment sequence
5. **V-shape** — check for exactly 1 corner with downward arms
6. **UNKNOWN** — no match, treat as wrong gesture

### 5.7 Feedback on Correct Match
- Enemy immediately removed from screen
- Brief particle burst at enemy position (5–8 small colored rectangles flying outward, 20-frame animation)
- Kill count increments (see §9)
- "+N KILL" floating text rises from enemy position and fades over 30 frames

### 5.8 Feedback on Wrong Shape
- Enemy **not** killed
- Enemy flashes (alternates between its normal color and white) for 10 frames
- Enemy shakes horizontally (±5px oscillation) for 10 frames
- No kill currency change, no penalty

### 5.9 Tuning Constants
All tuning values are defined as constants in `GestureConfig.java`:

```java
// Minimum total stroke length to register as a gesture attempt
MIN_STROKE_LENGTH_PX = 150f;

// Minimum net displacement for a swipe right to register
MIN_SWIPE_DISTANCE_PX = 150f;

// Ratio of horizontal to vertical displacement required for Swipe Right
SWIPE_AXIS_RATIO = 2.5f;

// Max ratio of end-to-start distance vs total stroke length to count as "closed" (Circle)
// Generous: 0.25 means the loop can end up to 25% of stroke length away from start
CLOSEDNESS_THRESHOLD = 0.25f;

// Minimum angle change (degrees) at a point to count as a "corner"
CORNER_ANGLE_THRESHOLD_DEG = 70f;

// Minimum number of direction reversals to classify as Scratch
SCRATCH_REVERSAL_COUNT = 3;

// Minimum ratio of horizontal to vertical in a scratch segment
SCRATCH_AXIS_RATIO = 1.5f;

// Tolerance in degrees for Z-shape segment direction matching
Z_SEGMENT_TOLERANCE_DEG = 45f;
```

---

## 6. Combat System

### 6.1 Can the Player Hit This Enemy?

The player must have the correct **Combat Branch level** to affect enemies above a certain tier:

| Combat Level | Tiers Hittable |
|---|---|
| 1 (default) | Tier 1–2 |
| 2 | Tier 1–3 |
| 3 | Tier 1–4 |
| 4 (max) | Tier 1–5 |

If the player draws the correct shape on an enemy they cannot yet hit (tier too high), the enemy **flashes briefly** (same wrong-shape animation) but is not damaged. This communicates "you're not strong enough yet" without a penalty.

### 6.2 Hit Result Flow
```
Stroke finalized
  → Stroke hits enemy? → No → discard silently
  → Correct shape? → No → shake/flash enemy, no penalty
  → Player can hit this tier? → No → flash enemy (tier too high)
  → Yes → kill enemy, award kills, spawn particles
```

---

## 7. Wave System & Difficulty Scaling

### 7.1 Wave Definition
- A wave does not have a fixed number of enemies. Instead, a wave is **time-based**: each wave lasts `WAVE_DURATION_SECONDS` seconds (starts at 30s, decreases by 1s per wave, min 15s).
- Wave number is tracked in `GameState` and displayed in the UI.
- After each wave ends, a brief pause (2 seconds) occurs, then:
  - Difficulty parameters update (see §7.2)
  - Wave Perk Pick screen appears (see §3.5)

### 7.2 Difficulty Parameters Per Wave

| Parameter | Starting Value | Change Per Wave | Cap |
|---|---|---|---|
| Enemy base speed (px/s) | 120 | +10 per wave | 400 |
| Spawn interval (ms) | 3000 | -150 per wave | 500 |
| Max concurrent enemies | 1 (upgradeable) | — | 5 (upgrade-dependent) |
| Tier 3 enemy weight | 0% | Appears from wave 5 | 40% |
| Tier 4 enemy weight | 0% | Appears from wave 10 | 30% |
| Tier 5 enemy weight | 0% | Appears from wave 15 | 20% |
| Zigzag pattern enabled | No | From wave 6 | — |
| Burst pattern enabled | No | From wave 11 | — |
| Homing-lite enabled | No | From wave 16 | — |

> Enemy tier weights only activate if the player has unlocked those tiers via the Enemy Tier Unlock upgrade. Locking an upgrade does not slow the wave timer, but enemy tier variety is gated behind upgrades.

---

## 8. Upgrade & Skill Tree System

### 8.1 Overview
The right-panel skill tree has two branches. Each branch has 4 nodes arranged vertically. Nodes must be unlocked in order (top to bottom within a branch). Nodes in one branch are independent of the other branch.

Upgrades are purchased using kills (passive currency) or awarded free via Wave Perk Pick.

### 8.2 Combat Branch

| Node | Name | Effect | Kill Cost |
|---|---|---|---|
| C1 | Belt Rank I | Can hit Tier 3 enemies | 25 |
| C2 | Belt Rank II | Can hit Tier 4 enemies | 60 |
| C3 | Belt Rank III | Can hit Tier 5 enemies | 120 |
| C4 | Master Belt | Correct gesture awards +2 kills instead of +1 | 250 |

### 8.3 Survival Branch

| Node | Name | Effect | Kill Cost |
|---|---|---|---|
| S1 | Iron Skin I | Escape penalty reduced: 10 → 7 kills lost | 20 |
| S2 | Iron Skin II | Escape penalty reduced: 7 → 4 kills lost | 50 |
| S3 | Last Stand | Game Over threshold: kills < 0 → kills < -30 (buffer zone) | 100 |
| S4 | Iron Fortress | Escape penalty reduced to 1 kill lost | 200 |

### 8.4 Special Abilities

Special abilities are unlocked separately (not part of the tree branches). They appear as icon buttons at the bottom of the upgrade panel. They are **not purchased with kills** — they are awarded exclusively through Wave Perk Picks (see §3.5).

| Ability | Icon Color | Effect | Cooldown |
|---|---|---|---|
| Freeze | Cyan | All enemies stop moving for 3 seconds | 30s |
| Bomb | Red | All enemies currently on screen are instantly killed (no kill currency awarded) | 60s |
| Time Slow | Yellow | All enemies move at 30% speed for 5 seconds | 45s |
| Shield | Gray | Next escaped enemy does not trigger a kill penalty (one use, auto-replenishes on next Wave Perk Pick if selected again) | One-time block |

- Active ability buttons show a cooldown overlay (darkened + countdown number)
- Shield button shows a shield-active indicator when ready
- Abilities are activated by **tapping their button in the upgrade panel**

### 8.5 Wave Perk Pick Card Pool
Cards drawn for the perk pick screen are selected randomly from:
- Any unowned upgrade node (from either branch)
- Any unowned special ability
- If all upgrades and abilities are owned, offer: +50 kills bonus, +30s wave duration, or temp speed buff to all gestures (reduced recognition threshold for 1 wave)

---

## 9. Currency & Economy

### 9.1 Kill Count
- Displayed prominently in the upgrade panel and in the gameplay top bar
- Acts as both the **score** and the **currency**
- Spending kills on upgrades directly reduces the displayed kill count
- Kill count cannot go below a "game over threshold" (default: 0; extended to -30 with Last Stand upgrade)

### 9.2 Kill Awards
| Event | Kills Earned |
|---|---|
| Kill Tier 1 enemy | +1 |
| Kill Tier 2 enemy | +1 |
| Kill Tier 3 enemy | +2 |
| Kill Tier 4 enemy | +3 |
| Kill Tier 5 enemy | +4 |
| Master Belt active (C4) | Double all of the above |

### 9.3 Kill Penalties
| Event | Kills Lost |
|---|---|
| Enemy escapes (default) | -10 |
| Enemy escapes (Iron Skin I) | -7 |
| Enemy escapes (Iron Skin II) | -4 |
| Enemy escapes (Iron Fortress) | -1 |
| Shield active when enemy escapes | 0 (shield consumed) |

### 9.4 Game Over Condition
- Game Over triggers when: `killCount < GAME_OVER_THRESHOLD`
- Default: `GAME_OVER_THRESHOLD = 0`
- With Last Stand (S3): `GAME_OVER_THRESHOLD = -30`

---

## 10. Audio System

No audio files will be bundled in the prototype. The following sound event slots are defined and stubbed in a `SoundManager` class. Each slot is a no-op method that can be wired to `SoundPool` or `MediaPlayer` assets later.

| Slot Method | Trigger |
|---|---|
| `playKill()` | Enemy successfully killed |
| `playWrongGesture()` | Wrong shape drawn on enemy |
| `playEscape()` | Enemy reaches bottom, penalty applied |
| `playAbility(AbilityType)` | Special ability activated |
| `playWaveComplete()` | Wave timer ends |
| `playPerkSelect()` | Player selects a perk card |
| `playGameOver()` | Game over state entered |
| `playButtonTap()` | Any UI button tapped |

`SoundManager` is a singleton initialized in `MainActivity` and accessible globally.

---

## 11. Persistence & High Score

- Implemented with Android `SharedPreferences`
- Key: `"high_score"` (int)
- Written on Game Over if `currentKillCount > storedHighScore`
- Read on Title Screen to display "Best: X"
- A separate key `"highest_wave"` (int) tracks the best wave reached
- No other data is persisted (upgrades, kill count reset on each run)

```java
// Example usage
SharedPreferences prefs = context.getSharedPreferences("karate_slicer", Context.MODE_PRIVATE);
int best = prefs.getInt("high_score", 0);
prefs.edit().putInt("high_score", newScore).apply();
```

---

## 12. Class Architecture

### File Structure
```
app/src/main/java/com/example/karateslicer/
├── GameActivity.java           ← replaces MainActivity as game entry point
├── UpgradeActivity.java        ← new full-screen upgrade screen
├── GameView.java
├── GameThread.java
├── state/
│   ├── GameState.java          ← singleton shared between both Activities
│   └── GameStateManager.java
├── enemy/
│   ├── Enemy.java
│   ├── EnemySpawner.java
│   ├── ShapeType.java (enum)
│   └── movement/
│       ├── MovementPattern.java (interface)
│       ├── StraightMovement.java
│       ├── ZigzagMovement.java
│       ├── BurstMovement.java
│       └── HomingMovement.java
├── gesture/
│   ├── GestureHandler.java
│   ├── GestureRecognizer.java
│   └── GestureConfig.java
├── upgrade/
│   ├── UpgradeManager.java
│   ├── SkillNode.java
│   ├── SpecialAbility.java
│   └── AbilityType.java (enum)
├── ui/
│   ├── GameHUD.java
│   ├── TitleScreen.java
│   ├── PauseOverlay.java
│   ├── GameOverScreen.java
│   ├── WavePerkPicker.java
│   └── ParticleSystem.java
├── audio/
│   └── SoundManager.java
└── util/
    ├── ScoreManager.java
    └── Constants.java

app/src/main/res/layout/
├── activity_upgrade.xml        ← XML layout for UpgradeActivity (ScrollView tree)
```

### Key Class Responsibilities

**`GameActivity`** *(replaces MainActivity)*
- Entry point, sets content view to `GameView`
- Handles `onPause` / `onResume` lifecycle, delegating to `GameThread`
- On `onResume` after returning from `UpgradeActivity`: calls `gameThread.resumeGame()`
- Hosts the "UPGRADES" button tap → `startActivity(UpgradeActivity)` + pauses game loop

**`UpgradeActivity`**
- Separate full-screen Activity with `activity_upgrade.xml`
- Reads `GameState.getInstance()` on create to populate current kill count, wave, owned nodes
- Each skill node button: calls `UpgradeManager.purchase()` on tap; refreshes UI immediately
- Special ability buttons: show owned/cooldown state
- Back button / "← BACK" button: calls `finish()` to return to `GameActivity`

**`GameView` (extends `SurfaceView`, implements `SurfaceHolder.Callback`)**
- Owns the `GameThread`, `GameStateManager`, all subsystems
- Routes `onTouchEvent` to `GestureHandler` when in `PLAYING` state
- Routes tap events to `UpgradePanel` and ability buttons
- Main `draw(Canvas canvas)` method calls all subsystem renderers

**`GameThread` (extends `Thread`)**
- Fixed-timestep loop targeting 60 FPS
- Calls `gameView.update()` and `gameView.draw()` each tick
- Pauses/resumes via `boolean running`

**`GameState`**
- Plain data object: killCount, waveNumber, waveTimer, maxEnemies, spawnInterval, enemySpeedMultiplier, all upgrade levels
- Passed by reference to all subsystems that need to read/write game data

**`Enemy`**
- Holds per-instance data (see §4.4)
- `update(float deltaTime)` delegates to its `MovementPattern`
- `draw(Canvas canvas)` draws the shape and the floating icon

**`GestureRecognizer`**
- Stateless utility class
- `ShapeType recognize(List<PointF> stroke)` — returns recognized shape or `UNKNOWN`
- Internally runs the pre-processing and classification (see §5.4)

**`UpgradeManager`**
- Owns the skill tree node states and special ability states
- `canAfford(SkillNode node, int kills)` — checks cost
- `purchase(SkillNode node, GameState state)` — deducts kills, applies effect
- `applyPerkFree(Object perk, GameState state)` — applies wave perk without cost

**`ParticleSystem`**
- Pool of `Particle` objects (pre-allocated, reused to avoid GC)
- `emit(float x, float y, int color)` — activates N particles at position
- `update()` and `draw(Canvas)` each tick

---

## 13. Constants Reference

All tuning constants live in `Constants.java`:

```java
// Gameplay
public static final int STARTING_KILL_COUNT = 0;
public static final int GAME_OVER_THRESHOLD_DEFAULT = 0;
public static final int GAME_OVER_THRESHOLD_LAST_STAND = -30;
public static final int ESCAPE_PENALTY_DEFAULT = 10;
public static final int MAX_ENEMIES_START = 1;
public static final int MAX_ENEMIES_CAP = 5;

// Wave
public static final int WAVE_DURATION_START_SECONDS = 30;
public static final int WAVE_DURATION_MIN_SECONDS = 15;
public static final int WAVE_DURATION_DECREMENT_SECONDS = 1;
public static final int INTER_WAVE_PAUSE_SECONDS = 2;

// Difficulty
public static final float ENEMY_BASE_SPEED_START = 120f;   // px/s
public static final float ENEMY_BASE_SPEED_MAX = 400f;
public static final float ENEMY_SPEED_INCREMENT_PER_WAVE = 10f;
public static final int SPAWN_INTERVAL_START_MS = 3000;
public static final int SPAWN_INTERVAL_MIN_MS = 500;
public static final int SPAWN_INTERVAL_DECREMENT_MS = 150;

// Gesture (see GestureConfig.java for full set)
public static final float MIN_STROKE_LENGTH_PX = 150f;
public static final float MIN_SWIPE_DISTANCE_PX = 150f;
public static final float SWIPE_AXIS_RATIO = 2.5f;
public static final float CLOSEDNESS_THRESHOLD = 0.25f;
public static final float CORNER_ANGLE_THRESHOLD_DEG = 70f;
public static final int SCRATCH_REVERSAL_COUNT = 3;
public static final float SCRATCH_AXIS_RATIO = 1.5f;
public static final float Z_SEGMENT_TOLERANCE_DEG = 45f;

// Particles
public static final int PARTICLE_POOL_SIZE = 100;
public static final int PARTICLES_PER_KILL = 8;
public static final int PARTICLE_LIFETIME_FRAMES = 20;

// Hit feedback
public static final int WRONG_GESTURE_FLASH_FRAMES = 10;
public static final float WRONG_GESTURE_SHAKE_PX = 5f;

// Abilities
public static final int FREEZE_DURATION_MS = 3000;
public static final int TIME_SLOW_DURATION_MS = 5000;
public static final float TIME_SLOW_FACTOR = 0.3f;
public static final int FREEZE_COOLDOWN_MS = 30000;
public static final int BOMB_COOLDOWN_MS = 60000;
public static final int TIME_SLOW_COOLDOWN_MS = 45000;
```

---

## 14. Development Phases

### Phase 1 — Project Skeleton
- Create Android Studio project with Java, API 26 min
- `GameActivity` → sets `GameView` as full-screen content
- `GameView` with `SurfaceView` + basic `GameThread` loop at 60 FPS
- Draw full-screen gameplay area with HUD overlay: wave label (top-left), kill count (top-center), pause button (top-right), "UPGRADES" button (bottom-center)
- `UpgradeActivity` created with `activity_upgrade.xml` — placeholder text only ("Upgrade Screen — Coming in Phase 4")
- Tapping "UPGRADES" button pauses game loop and opens `UpgradeActivity`; returning resumes loop
- `GameState` implemented as a singleton
- Pause/resume lifecycle handled

**Deliverable:** App opens full-screen, HUD visible, "UPGRADES" button navigates to upgrade screen and back, game loop runs without crash.

### Phase 2 — Enemy Spawning & Movement
- `Enemy`, `ShapeType`, `StraightMovement` implemented
- `EnemySpawner` spawns enemies at top at defined intervals
- Enemies move downward and despawn when off-screen
- Escape penalty applied (kill count deducted, printed to logcat)
- Each tier drawn as its corresponding Canvas shape
- Floating shape icon drawn above each enemy

**Deliverable:** Enemies spawn, move, escape with logged penalty.

### Phase 3 — Gesture Recognition & Combat
- `GestureHandler` captures stroke points on `ACTION_DOWN/MOVE/UP`
- `GestureRecognizer` classifies strokes using the 5-gesture rule-based system (§5.5)
- Hit detection: does any stroke point fall within an enemy's bounds?
- Correct gesture → kill enemy, increment kills, spawn particles, floating text
- Wrong gesture → flash/shake feedback on enemy (§5.8)
- Kill count displayed in HUD

**Deliverable:** Player can kill enemies by performing correct gestures. All 5 gestures testable even if only Tier 1–2 enemies spawn yet.

### Phase 4 — Skill Tree & Upgrade Screen
- `SkillNode`, `UpgradeManager` implemented
- `UpgradeActivity` fully built out with `activity_upgrade.xml` — Combat Branch, Survival Branch, Special Abilities all rendered as tappable buttons
- Kill count and wave number displayed at top of upgrade screen (read from `GameState` singleton)
- Spend kills to unlock nodes; `GameState` updated immediately
- Belt Rank nodes gate enemy tier hits; Survival nodes reduce escape penalty
- Back button returns to gameplay seamlessly

**Deliverable:** Upgrade screen fully functional; purchasing nodes affects gameplay on return.

### Phase 5 — Wave System, Perk Picks & Special Abilities
- `GameStateManager` with all states implemented
- Wave timer and wave progression
- `WavePerkPicker` overlay with 3 random cards
- Special abilities wired to icon buttons
- `SoundManager` stubs in place

**Deliverable:** Full wave loop with perk selection working.

### Phase 6 — Title, Pause, Game Over & Persistence
- `TitleScreen`, `PauseOverlay`, `GameOverScreen` screens implemented
- High score and highest wave saved/loaded via `SharedPreferences`
- New high score highlighted on Game Over screen

**Deliverable:** Complete game loop from title to game over and back.

### Phase 7 — Difficulty Scaling & Advanced Movement
- Zigzag, Burst, Homing-lite movement patterns implemented
- Wave-based parameter scaling applied (speed, spawn rate, tier weights)
- Higher tier enemies introduced per unlock + wave thresholds

**Deliverable:** Game noticeably harder at wave 10+; movement variety present.

### Phase 8 — Polish
- Particle effects tuned
- Floating "+1 KILL" text on kill
- Penalty flash ("–10") at screen bottom
- Combo tracking (optional: show combo multiplier text for consecutive fast kills)
- Hit feedback tuned for game feel
- Upgrade panel animates when a node is unlocked

---

*End of Specification*
