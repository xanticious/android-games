# Snakes 2D — Design Document

## Overview
A reimagined snake game with full 2D free-angle movement. Instead of fixed 90-degree grid turns, the snake moves toward a tap-targeted destination in a straight line at any angle. The snake grows as food is collected. The player must avoid walls and the snake's own body.

## Visual Style
- HD clean vector aesthetic: the snake is rendered as a smooth, segmented tube with rounded joints.
- Snake body: gradient-colored (head is brighter, body slightly dims toward the tail).
- Eyes on the snake head add personality — they face the current direction of travel.
- Food items: bright glowing orbs of distinct colors. Multiple food types (see below).
- Walls: clean solid boundary with a subtle inner glow.
- Tap destination: a small pulsing crosshair/dot briefly appears where the player tapped.
- Path preview: optionally a short dotted line from the head to the current destination.
- Collision: the snake flashes white and fragments on death.
- Background: dark gradient, subtle grid pattern.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]   [Length]   [Best]    │  ← HUD (top)
├─────────────────────────────────┤
│  ┌───────────────────────────┐  │
│  │                           │  │
│  │     PLAY FIELD            │  │
│  │   ~snake~ ● food          │  │
│  │                           │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
     [Tap anywhere to set target]
```

## Controls
- **Set destination**: tap anywhere on the play field to set the snake's movement target.
- The snake travels in a straight line toward the tapped point.
- The snake does not stop at the destination — it passes through and continues in the same direction until a new tap redirects it.
- Multiple rapid taps update the destination in real time (the snake smoothly curves toward the new point, not a hard redirect — there is a gentle angular limit per frame to prevent instant 180° reversals).
- See `design/common/tap-targeting.md` — Variant B (Position Targeting).

## Gameplay Loop

### Core Loop
1. The snake starts small (5 segments).
2. Player taps to steer toward food.
3. Eating food adds segments to the tail (+1 segment per food by default).
4. If the snake hits a wall or its own body, the run ends.
5. Score and length are recorded; player can retry.

### Snake Movement Physics
- The snake has a fixed movement speed (pixels per frame).
- Speed does not change unless a power-up is active.
- Direction changes are smooth: the snake turns toward the tap target with a maximum turn rate per frame (prevents instant reversals but allows tight curves when tapped close to the head).
- The snake's body follows the head's exact path (chain-following segments).

### Food Types
| Type | Appearance | Effect |
|------|-----------|--------|
| Standard food | Small colored orb | +1 length, +10 score |
| Bonus food | Pulsing golden orb (disappears after 5s) | +3 length, +50 score |
| Speed boost food | Blue lightning orb | +1 length, +20 score, +25% speed for 5s |
| Slow food | Purple orb | +1 length, +20 score, −25% speed for 5s |
| Shrink food | Orange orb with minus icon | −3 length (minimum 5), +100 score |
| Ghost food | White translucent orb | +1 length, ghost mode 5s (can pass through own body once) |

### Difficulty / Progression
- Single endless mode: difficulty increases as the snake gets longer.
- Occasional "narrow passage" zones appear — walls temporarily intrude from the sides.
- Speed increases slightly every 25 segments of length.

### Win / Loss
- No formal "win" state — it's an endless score-chase.
- Game ends when the snake collides with a wall or its own body.
- Ghost mode (from ghost food) temporarily negates self-collision.

## Scoring
| Event | Score |
|-------|-------|
| Eat standard food | 10 |
| Eat bonus food | 50 |
| Eat speed/slow/ghost food | 20 |
| Eat shrink food | 100 |
| Length milestone every 10 segments | 200 |
| Survival time bonus (per 30s) | 150 |

## State Machine
```
Idle
 └─ GameStart → Playing
Playing
 ├─ TapDetected → Playing (target updated)
 ├─ FoodEaten → Playing (length/score update)
 ├─ GhostModeActive → Playing (collision logic modified)
 └─ Collision → Dead
Dead
 └─ (show score, retry or menu) → Idle
```

## HUD
- Score: top-left.
- Current length (segment count): top-center.
- Personal best length: top-right (dims normally, highlights if near/exceeding record).
- Active power-up icon + countdown: bottom-right corner.
- Tap destination dot: rendered on the play field (briefly, 1 second after tap).
