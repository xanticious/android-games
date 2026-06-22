# Asteroids — Design Document

## Overview
A high-definition reimagining of the classic arcade game with updated movement controls and an objective-based level structure. The player pilots a lone spaceship through asteroid fields, collecting beacons to advance.

## Visual Style
- HD vector-style space art. Ship and asteroids rendered with subtle rim lighting and particle trails.
- Deep space background: layered parallax starfields (2–3 layers at different scroll speeds).
- Asteroids have distinct visual tiers by size: large (3 segments), medium (2 segments), small (1 segment). Each tier has a cracked, rocky texture.
- Explosions emit a burst of rock-fragment particles matching the asteroid's color.
- Beacon: glowing pulsing orb with a distinct accent color.
- Ship: sleek silhouette with a faint engine trail particle stream.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Lives]   [Score]   [●●●○○]   │  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│         GAME BOARD              │
│     (wrapping play field)       │
│                                 │
└─────────────────────────────────┘
     [Acceleration Knob: left-fixed / right-fixed / floating]   ← Control (overlaid on board)
```
- The game board wraps on all four edges (objects exiting one side reappear on the opposite side).
- Controls are overlaid on the game board corners; they do not reduce board size.

## Controls

### Acceleration Knob (360°)
- A single large analog knob (twice the size of the shared default) controls **acceleration**.
- The knob accelerates the ship in the knob's own direction — full 360° control. The player does
  not rotate then thrust; instead they can be moving one way and accelerate toward any other.
- The knob's deflection from center sets the acceleration magnitude (analog, for precise control).
- Knob placement is selectable in the in-game settings screen:
  - **Left Thumb (fixed)** — anchored at the lower-left.
  - **Right Thumb (fixed)** — anchored at the lower-right.
  - **Floating** — no home; the ring appears wherever any finger first touches and tracks from there.

### Ship Movement
- The ship always drifts: speed is clamped between a small non-zero **min speed** and a **max speed**.
- It starts each life moving at min speed.
- The ship's heading **auto-aligns** to its current velocity direction.

### Firing — Autofire
- There is no tap-to-fire. The ship fires automatically on a fixed cadence.
- Each shot targets the **nearest asteroid** (wrap-aware), regardless of ship heading.
- Projectiles are large (visually prominent) and travel at a moderate speed.
- Projectiles fade out after travelling a fixed distance across the field.
- Autofire **pauses** during the post-damage teleport/freeze animation.

### Taking Damage — Teleport to Safety
- On any asteroid collision the ship **teleports** into a large gap between asteroids and its
  velocity is clamped down to min speed.
- The ship's **collision boundary is tighter than its bounding circle** so it hugs the rendered
  triangular sprite — asteroids must visibly touch the ship to count as a hit.
- Asteroids **freeze** for ~2 seconds during the teleport/damage animation before play resumes.
- The mode timer (for timed modes) does **not** pause during this freeze.

## Game Modes
Selectable on the in-game settings screen before play begins:

- **Classic** — survive as many levels as you can, starting with 3 lives. HUD shows hearts.
- **Level Challenge** — choose a number of levels (e.g. 10). Count-up timer and infinite lives;
  complete that many levels as quickly as possible. HUD shows the timer and level progress.
- **Time Challenge** — choose a duration (1–20 min). Count-down timer and infinite lives; destroy
  as many asteroids as possible before time runs out. HUD shows the timer and asteroid kill count.

For modes with infinite lives, the HUD shows the timer instead of hearts. Damage still teleports the
ship to safety, but no life is lost.

## Gameplay Loop

### Level Structure
Each level is an asteroid field. The objective is to collect 5 beacons.

1. **Beacon 1** is already on the field when the level begins, at a random position.
2. **Beacons 2–5** each appear immediately when the previous beacon is collected — there is no
   spawn delay; exactly one beacon is on the field at any time until all five are collected.
3. Collecting a beacon (touching it with the ship) triggers an area-of-effect explosion:
   - All asteroids within a radius of ~25% of the screen's shorter dimension take 1 damage.
   - Large asteroids (3 HP) that reach 0 HP split into 2 Medium asteroids.
   - Medium asteroids (2 HP) that reach 0 HP split into 2 Small asteroids.
   - Small asteroids (1 HP) that reach 0 HP are destroyed.
4. The field keeps refilling with a fresh wave of large asteroids whenever it is cleared, until
   all 5 beacons are collected.
5. After all 5 beacons are collected, the level ends.

### Asteroids
- Three sizes: Large, Medium, Small.
- Large asteroid: 3 HP. On death → splits into 2 Medium.
- Medium asteroid: 2 HP. On death → splits into 2 Small.
- Small asteroid: 1 HP. On death → destroyed.
- Beacon explosion counts as 1 HP of damage to all asteroids in range.
- Player projectile: 1 HP damage per hit.
- Asteroids drift at constant velocity; direction and speed are randomized at spawn.
- On level start, the field has `4 + (level_number × 2)` large asteroids, capped at 20.
- Asteroids wrap around screen edges.

### Player Ship
- Starts each level at screen center, already drifting at min speed.
- Classic mode: three lives per game. Level/Time Challenge modes: infinite lives.
- Brief invincibility (2 seconds) after taking damage, shown as a blinking ship sprite.
- On collision the ship teleports to a safe gap; asteroids freeze for ~2 seconds.
- Ship wraps around screen edges.

### Scoring
| Event | Points |
|-------|--------|
| Destroy small asteroid | 100 |
| Destroy medium asteroid | 50 |
| Destroy large asteroid | 20 |
| Collect beacon | 500 + (beacon_number × 100) |
| Level complete bonus | 1000 × level_number |

## Difficulty Scaling
- Asteroid count increases each level.
- Asteroid drift speed increases slightly each level (capped at 1.5× initial speed by level 5).
- Asteroids keep respawning within a level until all 5 beacons are collected.

## State Machine (per-game)
```
Idle
 └─ GameStarted → Setup
Setup (one screen: difficulty + knob placement + game mode + How to Play + Start)
 ├─ OpenHowToPlay → HowToPlay
 └─ ConfigConfirmed → Spawning
HowToPlay
 └─ BackToSetup → Setup
Spawning
 └─ FieldReady → Playing
Playing
 ├─ BeaconCollected → Playing (explosion fires, next beacon appears immediately)
 ├─ PlayerHit → Playing (teleport to safety + asteroid freeze; no phase change)
 ├─ AllBeaconsCollected → LevelComplete
 └─ GameEnded → GameOver        (out of lives, or time expired)
LevelComplete
 ├─ NextLevel → Spawning
 └─ GameEnded → GameOver        (level-challenge target reached)
GameOver
 └─ Retry → Spawning
```

## Settings & How to Play
- Asteroids owns its full settings flow inside its own composable (`selfConfigured = true` in the
  catalog), so the lobby launches it directly and the shared game-settings screen is skipped.
- A single Setup screen collects **difficulty**, **knob placement**, and **game mode**, and offers
  **How to Play** and **Start Game** buttons.

## Victory / Defeat
- See `design/common/victory-defeat.md`.
- Level Complete: star rating based on score and lives remaining.
- Game Over: shows final score and best score.

## HUD
- See `design/common/hud-elements.md`.
- Beacon tracker: 5 dots (filled = collected, pulsing = active/spawned, hollow = not yet spawned).
- Classic mode: lives shown as small ship icons (up to 3) on the left.
- Infinite-life modes: the mode timer replaces the hearts on the left (count-up for Level Challenge,
  count-down for Time Challenge); Time Challenge also shows the asteroid kill count.
- Score: top-center.
