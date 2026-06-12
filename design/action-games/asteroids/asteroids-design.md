# Asteroids — Design Document

## Overview
A high-definition reimagining of the classic arcade game with updated movement controls and an objective-based level structure. The player pilots a lone spaceship through asteroid fields, collecting beacons to advance.

## Visual Style
- HD vector-style space art. Ship and asteroids rendered with subtle rim lighting and particle trails.
- Deep space background: layered parallax starfields (2–3 layers at different scroll speeds).
- Asteroids have distinct visual tiers by size: large (3 segments), medium (2 segments), small (1 segment). Each tier has a cracked, rocky texture.
- Explosions emit a burst of rock-fragment particles matching the asteroid's color.
- Beacon: glowing pulsing orb with a distinct accent color. Pulses faster as the collection timer approaches.
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
     [Joystick]       [Tap Zone]     ← Controls (overlaid on board corners)
```
- The game board wraps on all four edges (objects exiting one side reappear on the opposite side).
- Controls are overlaid on the game board corners; they do not reduce board size.

## Controls

### Left Thumb — Virtual Joystick
- See `design/common/virtual-joystick.md` for full spec.
- `dx` controls rotation speed: left rotates ship counter-clockwise, right rotates clockwise.
- `dy` up applies thrust in the direction the ship is pointing.
- `dy` down applies reverse thrust (backward deceleration).
- Joystick input is analog — partial deflection gives partial rotation/thrust.

### Right Hand — Tap to Fire
- See `design/common/tap-targeting.md` — Variant A (Directional Fire).
- Player taps anywhere in the right half of the screen.
- The tap point defines a direction from the ship center.
- Valid fire cone: ±20° from the ship's current heading.
- Out-of-cone taps clamp to the nearest cone edge.
- Fire rate: one projectile per tap; no auto-fire.
- Projectiles are large (visually prominent) and travel at a moderate speed (~40% of original arcade speed equivalent).

## Gameplay Loop

### Level Structure
Each level is an asteroid field. The objective is to collect 5 beacons.

1. **Beacon 1** spawns 10 seconds after the level begins at a random position in the field.
2. **Beacons 2–5** each spawn 10 seconds after the previous beacon is collected.
3. Collecting a beacon (touching it with the ship) triggers an area-of-effect explosion:
   - All asteroids within a radius of ~25% of the screen's shorter dimension take 1 damage.
   - Large asteroids (3 HP) that reach 0 HP split into 2 Medium asteroids.
   - Medium asteroids (2 HP) that reach 0 HP split into 2 Small asteroids.
   - Small asteroids (1 HP) that reach 0 HP are destroyed.
4. After all 5 beacons are collected, the level ends.

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
- Starts each level at screen center.
- Three lives per game.
- Brief invincibility (2 seconds) after losing a life, shown as a blinking ship sprite.
- Ship is destroyed by any asteroid collision.
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
- Beacon spawn time remains constant at 10 seconds between beacons regardless of level.

## State Machine (per-level)
```
Idle
 └─ LevelStart → Spawning
Spawning
 └─ FieldReady → Playing
Playing
 ├─ BeaconSpawn → Playing (beacon appears on field)
 ├─ BeaconCollected → Playing (explosion fires, next beacon timer starts)
 ├─ AllBeaconsCollected → LevelComplete
 ├─ PlayerHit [lives > 0] → Respawning
 └─ PlayerHit [lives == 0] → GameOver
Respawning
 └─ RespawnComplete → Playing
LevelComplete
 └─ NextLevel → Spawning
GameOver
 └─ (terminal state)
```

## Victory / Defeat
- See `design/common/victory-defeat.md`.
- Level Complete: star rating based on score and lives remaining.
- Game Over: shows final score and best score.

## HUD
- See `design/common/hud-elements.md`.
- Beacon tracker: 5 dots (filled = collected, pulsing = active/spawned, hollow = not yet spawned).
- Lives: small ship icons (up to 3).
- Score: top-center.
