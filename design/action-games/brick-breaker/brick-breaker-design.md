# Brick Breaker — Design Document

## Overview
A turn-based brick breaker where bricks descend one row per turn and the player fires a bank of balls each turn (starting at 20). Each level has a finite number of rows to destroy — level 1 has 1 row, level 2 has 2 rows, and so on up to a maximum of 20 rows. Rows beyond what fits on screen are stacked off screen above the field and feed into view one row per turn. The goal is to destroy every row before any brick reaches the bottom boundary.

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
│  [Launch position slider] [Clear]│
└─────────────────────────────────┘
```

## Controls
- **Launch Position**: drag a slider (or drag the cannon itself) left and right along the bottom edge.
- **Aim Angle**: drag up from the cannon to set the fire angle. A dotted trajectory preview shows the ball's path and first bounce.
- **Fire**: release the drag or tap a "Fire" button to launch the whole ball bank in sequence along the chosen trajectory.
- **Clear**: tap the "Clear" button during the Fire Phase to immediately remove all balls still in play and advance to the Resolution Phase. Use it when a ball gets stuck bouncing or the player simply doesn't want to wait for the volley to finish.
- Angle is clamped between 10° and 170° (nearly vertical in either direction) — flat horizontal shots are not allowed.

## Gameplay Loop

### Turn Structure
1. **Aim Phase**: Player sets position and angle. Trajectory preview updates in real time.
2. **Fire Phase**: The full ball bank launches in sequence, 0.1s apart. Balls bounce off walls and bricks. The player may tap **Clear** at any point to remove all remaining balls and end the phase early.
3. **Resolution Phase**: Count hits, apply damage, remove destroyed bricks, drop power-ups.
4. **Drop Phase**: All remaining bricks descend one row.
5. Repeat until bricks reach the bottom (defeat) or the field is cleared (victory).

### Bricks
- Brick types:
  - **Standard brick**: HP between 1 and `level_number × 3`. No special effect.
  - **Power-up brick**: standard HP but drops a power-up when destroyed (see `design/common/powerup-system.md`). Marked with a glowing icon.
  - **Steel brick**: immune to standard shots; requires a Power Shot active power-up or 2× the listed HP to destroy.
- New bricks spawn at the top each turn, filling a row (with gaps) generated procedurally.
- Each ball deals damage equal to the current strength multiplier (1 by default).

### Power-Ups Available
Power-up bricks drop one of two collectibles, each worth a single point. They are collected instantly when the brick is destroyed — no falling icon animation — and the matching HUD chip (Balls or Strength) briefly highlights to show it incremented:
- **Extra Ball**: +1 to the player's ball bank (more balls fired per turn).
- **Extra Strength**: +1 to the damage multiplier (each ball deals more damage).

Both the ball bank and the strength multiplier persist across turns and levels.

### Win / Loss
- **Win**: destroy every row of the level. You can also beat a level early by clearing all bricks currently on screen; any power-ups from rows still off screen are awarded automatically.
- **Loss**: any brick crosses the bottom boundary at the start of a Drop Phase.

## Difficulty Scaling
- Rows per level scale with the level number (level N = N rows, capped at 20).
- Level 1–3: max brick HP = level × 3, new rows always have gaps.
- Level 4+: max brick HP = level × 3, gaps narrow, steel bricks introduced.
- The ball bank starts at 20 and grows only via Extra Ball power-ups.

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
 ├─ AllBallsLanded → ResolutionPhase
 └─ ClearTapped → ResolutionPhase   (all in-flight balls removed)
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
- Level number.
- Balls: current ball bank (starts at 20). The chip highlights when it increments.
- Strength: current damage multiplier (starts at ×1). The chip highlights when it increments.
- Turn counter.
- A dashed bottom boundary line marks the death line; it pulses red when bricks sit one row away from crossing it.
- Active power-ups: left side, stacked icons with timers.
