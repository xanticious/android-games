# Brick Breaker (Cannon Arcade) — Design Document

## Overview
The real-time version of Brick Breaker (Cannon). The cannon on the left fires continuously and the player must destroy all target bricks before the countdown timer runs out. No turns — timing, aim precision, and quick decisions are everything.

## Visual Style
Identical to Brick Breaker (Cannon) with arcade-mode additions:
- Countdown timer displayed as a prominent visual bar at the top of the screen, depleting in real time.
- When time is low (≤10s), the bar pulses red.
- Screen-edge vignette effect that intensifies as time runs low.
- Cannon fires at a rate controlled by the player (hold to charge, release to fire) or auto-fires on a cooldown.

## Screen Layout
```
┌─────────────────────────────────┐
│  ████████████ TIMER ████░░░░░░  │  ← Timer bar (full width)
│  [Score]  [Targets: N]          │  ← HUD row
├─────────────────────────────────┤
│ [C]     │                       │
│ [A]     │   BRICK STRUCTURES    │
│ [N]     │                       │
│ [N]     │                       │
│ [O]     │                       │
│ [N]     │                       │
├─────────────────────────────────┤
│  [Aim Dial]         [Fire Btn]  │  ← Control zone
└─────────────────────────────────┘
```

## Controls
- **Aim**: drag the aim dial or swipe on the cannon barrel to adjust angle in real time.
- **Fire**: tap the Fire button to launch a ball immediately. A brief cooldown (0.5s default) prevents spamming.
- The arc preview is always visible showing the current aim angle's trajectory.

## Gameplay Loop

### Real-Time Structure
- Timer starts at a level-determined value (e.g., 60 seconds at level 1, decreasing by 5s per level, minimum 30s).
- Player aims and fires balls at will (subject to cooldown).
- Brick physics resolve immediately after each ball stops moving.
- No turn structure — multiple balls can be in flight simultaneously if the cooldown allows.

### Bricks
Same as Brick Breaker (Cannon):
- Standard bricks, target bricks (must be destroyed to win), steel bricks.
- Falling cascade physics when supports are destroyed.

### Ball Physics
Same parabolic arc, gravity, and bounce rules as Brick Breaker (Cannon).

### Power-Ups
Same power-up types as Brick Breaker (Cannon). Collected by the ball passing through the drop point.
- Time Bonus power-up added (arcade-exclusive): adds 10 seconds to the timer.

### Win / Loss
- **Win**: all target bricks destroyed before the timer expires.
- **Loss**: timer reaches zero with any target brick remaining.

## Scoring
| Event | Points |
|-------|--------|
| Destroy standard brick | 50 |
| Destroy target brick | 300 |
| Cascade destruction (per extra brick) | 75 |
| Seconds remaining at level end | 100 × seconds_left |
| All bricks cleared (not just targets) | 1000 bonus |

## Difficulty Scaling
- Timer decreases per level.
- Structure complexity increases per level.
- More target bricks per level.
- Cooldown between shots increases slightly at higher levels.

## State Machine
```
Idle
 └─ LevelStart → Playing
Playing
 ├─ BallFired → Playing (ball in flight)
 ├─ AllTargetsDestroyed → LevelComplete
 └─ TimerExpired → GameOver
LevelComplete
 └─ NextLevel → Playing (next level init)
GameOver
 └─ (terminal state)
```

## HUD
- Timer bar: full-width top strip, depleting left to right.
- Score: left below timer bar.
- Target bricks remaining: center below timer bar.
- Active power-ups: bottom-left corner, icons with countdown bars.
- Cooldown indicator: arc around the Fire button showing when next shot is available.
