# Missile Command — Design Document

## Overview
A high-definition reimagining of the 1980 arcade classic. The player defends six cities from relentless missile barrages by firing interceptor rockets from three launch silos. Tap the screen to send an interceptor to that point — it detonates in an expanding explosion that destroys any enemy missile passing through it. Survive wave after wave of escalating attacks.

## Visual Style
- HD art with a dramatic, high-stakes visual language: cold night sky, distant mountain silhouette, city lights on the horizon.
- Enemy missiles: white-to-red gradient streaks descending from the top of the screen, trailing smoke behind them.
- Enemy bombers (later waves): slow-moving aircraft that release additional missiles mid-screen.
- Smart bombs (later waves): zigzagging missiles that evade slower interceptors.
- Interceptors: bright blue/cyan rockets launched from silos, visible in flight until they detonate.
- Detonation: expanding circular blast ring — bright white core fading to orange-red, then dissipating as a smoke ring.
- Cities: detailed pixel-art skyline silhouettes. When a city is destroyed, it crumbles in a brief animation and goes dark.
- Silos: launch pad structures on the ground. When ammo is exhausted, the silo shows an "empty" state. Destroyed silos are rubble.
- Background: starfield, faint moon glow, and distant mountains. Color shifts toward red/orange as waves escalate.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]   [Wave: N]   [Cities] │  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│   [incoming missiles / bombers] │
│         (descend from top)      │
│                                 │
│  ──────────────────────────     │  ← Ground line
│  [Silo L]  ○ ○ ○  [Silo C]  ○ ○ ○  [Silo R] │
│  ◼ ◼ ◼   CITIES   ◼ ◼ ◼      │  ← City row
└─────────────────────────────────┘
```

## Controls
- **Launch interceptor**: tap anywhere above the ground line. An interceptor launches from the nearest available silo to the tapped point and detonates there.
- The game automatically selects the best silo (closest with ammo remaining).
- Player can tap rapidly for multiple simultaneous interceptors.
- No joystick needed — pure tap-to-target gameplay.

## Gameplay Loop

### Wave Structure
- Each wave is a salvo of enemy missiles.
- All missiles in a wave are launched simultaneously from the top of the screen, each targeting a city or silo.
- The player must intercept missiles before they reach the ground.
- A wave ends when all enemy missiles are either intercepted or have hit their targets.
- Between waves: a brief score tally screen, then the next wave begins.

### Silos
- 3 silos: Left, Center, Right.
- Each silo starts with 10 interceptor missiles per wave.
- Unused interceptors at end of wave are bonus points (not carried over).
- Silos are refilled to 10 at the start of each wave.
- Silos can be destroyed by enemy missiles. A destroyed silo cannot launch until repaired (repaired at the start of every 3rd wave if originally present).

### Cities
- 6 cities arranged across the bottom.
- A city is destroyed when a missile hits it directly.
- Destroyed cities do not come back (permanent loss for the rest of the game).
- If all 6 cities are destroyed, the game ends.
- Bonus cities: every 10,000 points, one destroyed city is rebuilt (maximum 6 at any time).

### Enemy Missile Types
| Type | Behavior | Speed | Introduced |
|------|----------|-------|-----------|
| Standard missile | Straight line descent | Medium | Wave 1 |
| Fast missile | Straight line, faster | Fast | Wave 3 |
| Bomber | Horizontal aircraft, drops standard missiles | Slow | Wave 5 |
| MIRV | Splits into 3 standard missiles mid-descent | Medium | Wave 7 |
| Smart bomb | Evades interceptors with slight juke moves | Medium | Wave 10 |
| Satellite | Crosses the top; drops missiles downward | Fast | Wave 12 |

### Scoring
| Event | Points |
|-------|--------|
| Intercept standard missile | 25 |
| Intercept fast missile | 50 |
| Intercept smart bomb | 100 |
| Intercept MIRV (before split) | 125 |
| Intercept MIRV child | 25 each |
| Destroy bomber | 100 |
| Destroy satellite | 150 |
| Multi-kill (2+ in one blast) | 2× per additional missile |
| Unused interceptor (wave end) | 5 per missile |
| Surviving city (wave end) | 100 |
| Surviving silo (wave end) | 50 |

## Difficulty Scaling
- More missiles per wave each level.
- Missile descent speed increases per wave.
- Smart bombs and MIRVs appear from wave 7+.
- Wave-end bonus opportunities decrease (faster missiles, less time to intercept).

## State Machine
```
Idle
 └─ GameStart → WaveIntro
WaveIntro
 └─ IntroDelay → Playing
Playing
 ├─ TapDetected → Playing (interceptor launched)
 ├─ MissileIntercepted → Playing (score update)
 ├─ MissileHitCity → Playing (city destroyed)
 ├─ MissileHitSilo → Playing (silo damaged)
 ├─ AllMissilesResolved → WaveTally
 └─ AllCitiesDestroyed → GameOver
WaveTally
 └─ TallyComplete → WaveIntro (next wave)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Wave number: top-center.
- Cities remaining: top-right (icon count, e.g., 4 city icons).
- Silo ammo: shown as small dots above each silo (10 dots, depleting as used).
- Interceptor detonation radius: brief transparent circle at tap point before launch (shows expected blast zone).
