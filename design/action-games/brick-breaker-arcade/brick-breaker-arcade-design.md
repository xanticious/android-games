# Brick Breaker (Arcade) — Design Document

## Overview
A real-time arcade brick breaker. Bricks slowly descend from the top at a constant speed. The player controls a cannon's left-right position at the bottom and fires balls upward continuously. The cannon always fires a built-in **multi-shot** spread (one vertical ball plus two slightly angled balls, one to each side) — aim by repositioning the cannon. Destroy all bricks before any reach the bottom.

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
- **Cannon Position**: drag or slide *anywhere on the screen* (not just the bottom control zone) to move the cannon left and right. A slider is also available. The cannon always fires its multi-shot spread from its current position.
- **Auto-Fire**: the cannon fires automatically at a fixed rate (1 volley per 0.3 seconds by default). No tap required to fire.
- The player's only active input is horizontal cannon positioning.

## Multi-Shot (Always Active)
- The cannon's fire is **always a three-ball spread**, not a power-up: one ball travels straight up, and two more launch at a slight outward angle, one to each side.
- The angled balls give light horizontal coverage so repositioning the cannon still matters for aiming, but a single volley sweeps a small arc rather than a single column.
- Because multi-shot is built in, it is **not** available as a collectible power-up (see Power-Ups below).

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
Power-up bricks drop one of two collectibles. The player collects them by sliding the cannon under the falling icon; collection instantly increments the matching HUD chip (no extra ball animation):
- **Extra Ball**: +1 to the player's ball bank.
- **Extra Strength**: +1 to the damage multiplier (each ball deals more damage).

(Multi-Shot is not a power-up in this mode — the cannon's three-ball spread is always active.)

### Lives
- Player starts with 3 lives.
- A life is lost when any brick reaches the bottom row.
- On life loss, **all current bricks are automatically pushed up by half a screen height** to give the player breathing room before play resumes.
- Brief 2-second pause after life loss; the brick field then continues from its new (raised) position.
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
 └─ RespawnDelay → Playing (bricks shifted up half a screen)
LevelComplete
 └─ NextLevel → Playing (next level init)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Level.
- Balls: current ball bank (starts at 20). The chip highlights when it increments.
- Strength: current damage multiplier (starts at ×1). The chip highlights when it increments.
- Lives: heart icons.
- A dashed bottom boundary line marks the death line; it pulses red when bricks approach it.
- Active power-ups: left side, icon stack with countdown bars.
