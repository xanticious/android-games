# Brick Breaker (Arcade) — Design Document

## Overview
A real-time arcade brick breaker. Bricks slowly descend from the top at a constant speed. The player controls a cannon's left-right position at the bottom and fires balls upward continuously. Shooting is strictly vertical (no angle adjustment — aim by repositioning the cannon). Destroy all bricks before any reach the bottom.

## Visual Style
Identical visual language to Brick Breaker (turn-based). Key arcade-mode additions:
- Motion blur on fast-falling bricks in later levels.
- Speed-up visual cue: a flashing "Fast" indicator when bricks accelerate.
- Rapid-fire balls are smaller and brighter than standard balls.
- Cannon glows brighter while firing and dims slightly during cooldown.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]   [Level]   [Lives]    │  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│         BRICK FIELD             │
│   (bricks descend in real time) │
│                                 │
│                                 │
├─────────────────────────────────┤
│  ◄──── [CANNON SLIDER] ────►    │  ← Control zone (bottom)
└─────────────────────────────────┘
```

## Controls
- **Cannon Position**: drag or slide anywhere on the bottom control zone to move the cannon left and right. The cannon fires straight up from its current position.
- **Auto-Fire**: the cannon fires automatically at a fixed rate (1 ball per 0.3 seconds by default). No tap required to fire.
- The player's only active input is horizontal cannon positioning.

## Gameplay Loop

### Real-Time Structure
- Bricks descend continuously at a speed determined by the current level.
- The cannon fires automatically.
- New rows of bricks generate at the top at fixed intervals (e.g., every 5 seconds at level 1).
- As bricks are destroyed, new rows replace them from the top.
- The level ends when a predefined number of rows have been generated and all surviving bricks are cleared.

### Bricks
Same types as Brick Breaker (turn-based):
- Standard bricks with level-scaled HP.
- Power-up bricks that drop collectibles.
- Steel bricks (introduced at higher levels).

### Power-Ups
Power-ups drop from destroyed power-up bricks and float downward. The player collects them by sliding the cannon under the falling power-up icon.

Available power-ups (from `design/common/powerup-system.md`):
- Rapid Fire (fires faster)
- Multi-Shot (3 balls at once)
- Power Shot (double damage)
- Clear Screen (destroys all visible bricks instantly)
- Explode (destroys all bricks adjacent to the collection point)
- Wide Shot (wider cannon beam)

### Lives
- Player starts with 3 lives.
- A life is lost when any brick reaches the bottom row.
- Brief 2-second pause after life loss; the brick field continues from where it was.
- Game over when all lives are lost.

## Difficulty Scaling
- Brick descent speed increases per level.
- Max brick HP scales with level.
- Row generation rate increases with level.
- Steel bricks appear from level 4 onward.

## Scoring
| Event | Points |
|-------|--------|
| Destroy standard brick | brick_max_hp × 10 |
| Destroy power-up brick | brick_max_hp × 15 |
| Destroy steel brick | brick_max_hp × 20 |
| Collect power-up | 100 |
| Survive level (no lives lost) | 1000 × level_number |

## State Machine
```
Idle
 └─ LevelStart → Playing
Playing
 ├─ BrickReachedBottom [lives > 0] → LifeLost
 ├─ BrickReachedBottom [lives == 0] → GameOver
 ├─ AllRowsGeneratedAndCleared → LevelComplete
 └─ PowerUpDropped → Playing (power-up floating)
LifeLost
 └─ RespawnDelay → Playing
LevelComplete
 └─ NextLevel → Playing (next level init)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Level: top-center.
- Lives: top-right (heart icons).
- Active power-ups: left side, icon stack with countdown bars.
- Descent speed indicator: subtle bar at the right edge, filling as bricks approach the bottom.
