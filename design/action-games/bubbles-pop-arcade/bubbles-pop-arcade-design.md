# Bubbles Pop (Arcade) — Design Document

## Overview
The real-time version of Bubbles Pop. The bubble cluster descends continuously and the player must fire rapidly and accurately to pop bubbles before they overwhelm the screen. Power-ups add chaos and strategy.

## Visual Style
Same as Bubbles Pop (turn-based) with arcade additions:
- Continuous downward drift animation on the cluster — bubbles are always subtly moving.
- Speed indicator: a subtle arrow or glow on the cluster's bottom row showing descent rate.
- When the cluster is near the danger line, a pulsing red border appears around the play area.

## Screen Layout
Identical to Bubbles Pop (turn-based). Danger line is the same; the difference is timing.

## Controls
- **Aim**: tap or hold-drag to aim the cannon. The angle updates in real time.
- **Fire**: tap anywhere in the play area (above the cannon) to fire immediately at the aimed angle. No per-turn restriction — fire as fast as you can aim.
- The cannon has a 0.4-second cooldown between shots to prevent pure spam.

## Gameplay Loop

### Real-Time Structure
- The cluster descends at a constant speed, accelerating slightly every 30 seconds.
- Player fires freely (subject to cooldown).
- Match logic is identical to turn-based: pop groups of 3+ same-color bubbles.
- Power-ups spawn in the cluster and are revealed when adjacent bubbles are popped.
- Power-ups float downward after being freed; collect by matching the surrounding area or touching the icon with a bubble.

### Power-Ups
From `design/common/powerup-system.md`:
- Lightning Strike: destroys an entire column of bubbles instantly.
- Color Bomb: destroys all bubbles of the targeted color.
- Slow Time: halves cluster descent speed for 10 seconds.
- Wild Shot: next fired bubble matches any color.
- Shield: absorbs one "danger line crossed" event.

### Lives
- Player has 3 lives.
- A life is lost when the cluster touches the danger line.
- After losing a life, the cluster resets to a short version with fewer rows and play resumes.
- Game over when all lives are lost.

## Scoring
Identical base scoring to Bubbles Pop (turn-based), plus:
| Event | Points |
|-------|--------|
| Rapid combo (pop within 2s of previous pop) | +50% multiplier per chain |
| Power-up collected | 100 |
| Survive 1 minute | 500 bonus |

## Difficulty Scaling
- Descent speed increases over time and per level.
- More colors introduced at higher levels.
- Power-up spawn frequency decreases at higher levels.

## State Machine
```
Idle
 └─ LevelStart → Playing
Playing
 ├─ BubblePopped → Playing (check cascade)
 ├─ ClusterTouchesDangerLine [lives > 0] → LifeLost
 ├─ ClusterTouchesDangerLine [lives == 0] → GameOver
 └─ ClusterCleared → LevelComplete
LifeLost
 └─ ClusterReset → Playing
LevelComplete
 └─ NextLevel → Playing (new cluster, faster speed)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Level: top-center.
- Lives: top-right (heart icons).
- Active power-ups: left side, icon stack with timers.
- Combo multiplier: bottom-center, briefly visible during active combos.
