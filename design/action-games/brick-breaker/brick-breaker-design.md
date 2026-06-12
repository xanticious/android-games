# Brick Breaker — Design Document

## Overview
A turn-based brick breaker where bricks fall one row per turn and the player fires a cannon loaded with 20 balls per turn. The goal is to destroy all bricks before any reach the bottom row.

## Visual Style
- HD 2D graphics with a bold, colorful aesthetic.
- Bricks are solid colored blocks with rounded corners, rendered with a subtle gradient and inner shadow.
- Each brick displays a bold white number showing its remaining hit points.
- Special bricks have a distinct border glow or icon overlay indicating their power-up type.
- Ball trails leave a short luminous streak in the ball's color.
- Cannon: a sleek mechanical launcher at the bottom of the screen, animating to point in the selected fire direction.
- Background: animated gradient or simple geometric pattern — never distracting from the bricks.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]     [Level]   [Turns]  │  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│         BRICK FIELD             │
│   (bricks descend each turn)    │
│                                 │
│         [aim line preview]      │
├─────────────────────────────────┤
│      [CANNON — aim + fire]      │  ← Control zone (bottom)
│  [Launch position slider]       │
└─────────────────────────────────┘
```

## Controls
- **Launch Position**: drag a slider (or drag the cannon itself) left and right along the bottom edge.
- **Aim Angle**: drag up from the cannon to set the fire angle. A dotted trajectory preview shows the ball's path and first bounce.
- **Fire**: release the drag or tap a "Fire" button to launch all 20 balls in sequence along the chosen trajectory.
- Angle is clamped between 10° and 170° (nearly vertical in either direction) — flat horizontal shots are not allowed.

## Gameplay Loop

### Turn Structure
1. **Aim Phase**: Player sets position and angle. Trajectory preview updates in real time.
2. **Fire Phase**: All 20 balls launch in sequence, 0.1s apart. Balls bounce off walls and bricks.
3. **Resolution Phase**: Count hits, apply damage, remove destroyed bricks, drop power-ups.
4. **Drop Phase**: All remaining bricks descend one row.
5. Repeat until bricks reach the bottom (defeat) or the field is cleared (victory).

### Bricks
- Brick types:
  - **Standard brick**: HP between 1 and `level_number × 3`. No special effect.
  - **Power-up brick**: standard HP but drops a power-up when destroyed (see `design/common/powerup-system.md`). Marked with a glowing icon.
  - **Steel brick**: immune to standard shots; requires a Power Shot active power-up or 2× the listed HP to destroy.
- New bricks spawn at the top each turn, filling a row (with gaps) generated procedurally.
- Each ball deals 1 damage per hit. Multi-shot power-up fires 3 balls per tick.

### Power-Ups Available
From `design/common/powerup-system.md`:
- Explode (instant area clear around power-up brick's position)
- Multi-Shot (3 balls per tick for the rest of the level)
- Power Shot (double damage for 20 seconds or rest of level)
- Clear Screen (destroys all standard bricks currently visible)
- Wide Shot (increases ball size, wider hitbox for remainder of level)

### Win / Loss
- **Win**: clear all bricks from the field before any brick reaches the bottom.
- **Loss**: any brick enters the bottom row at the start of a Drop Phase.

## Difficulty Scaling
- Level 1–3: max brick HP = level × 3, new rows always have gaps.
- Level 4+: max brick HP = level × 3, gaps narrow, steel bricks introduced.
- Ball count (20) is constant across all levels.

## Scoring
| Event | Points |
|-------|--------|
| Destroy standard brick | brick_max_hp × 10 |
| Destroy power-up brick | brick_max_hp × 15 |
| Destroy steel brick | brick_max_hp × 20 |
| Level complete bonus | 500 × level_number |
| Clear Screen power-up used | 200 per brick cleared |

## State Machine
```
Idle
 └─ LevelStart → Spawning
Spawning
 └─ FieldReady → AimPhase
AimPhase
 └─ FireTapped → FirePhase
FirePhase
 └─ AllBallsLanded → ResolutionPhase
ResolutionPhase
 ├─ BricksRemain → DropPhase
 └─ FieldCleared → LevelComplete
DropPhase
 ├─ NoBricksAtBottom → AimPhase
 └─ BricksAtBottom → GameOver
LevelComplete
 └─ NextLevel → Spawning
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Level number: top-center.
- Turns played: top-right.
- Active power-ups: left side, stacked icons with timers.
- Ball count: bottom-right (always 20, resets each turn).
