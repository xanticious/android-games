# Endless Runner — Design Document

## Overview
A high-definition infinite side-scroller featuring a ninja silhouette character with glowing eyes, set in a stylized Japanese world. The character runs automatically; the player taps to jump, double-jump, or slide under obstacles. The world blends feudal Japanese architecture with dramatic lighting and ink-wash art aesthetics.

## Visual Style

### Character
- A pure black silhouette of a lithe ninja, defined only by their glowing white/cyan eyes.
- Eyes convey emotion: normal glow, wide-open on near-misses, narrow squint when sliding.
- Running animation: fluid 8-frame cycle with a slight motion blur.
- Jump: single and double-jump arcs are visually distinct (double-jump adds a brief dashed ki-burst trail).
- Slide: character drops to one knee, silhouette flattens.
- Death: brief explosion of black ink splatter, then the level fades.

### World
- Japanese-style platforms: wooden temple rooftops, torii gate segments, paper lantern platforms, stone bridge sections.
- Obstacles: low-hanging banners, swinging noren curtains (duck through), bamboo spikes, leaping stone guardian statues.
- Background: layered parallax — foreground rooftops, midground pagoda skyline, distant misty mountains with ink-wash filtering.
- Day/night cycle: the game shifts between dawn (orange-gold), day (clear blue), dusk (deep red), and night (indigo + fireflies) over long distances.
- Cherry blossoms drift in the midground layer.
- Ground tiles animate: running over a wooden bridge creates wood-creak ripple effects.

### Color Palette
- Primary world tones: deep indigo, vermilion, aged wood brown, stone grey.
- Accent: glowing paper-lantern amber, neon-cyan for the ninja's eyes and ki effects.
- All colors from `ui/theme/Color.kt`.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score/Distance]   [Best]      │  ← HUD (top)
├─────────────────────────────────┤
│  [background layers - parallax] │
│                                 │
│  [obstacles + platforms]        │
│    [NINJA →→→→→→→→]             │  ← Character (lower third)
│  [ground layer]                 │
├─────────────────────────────────┤
│  [TAP anywhere to jump/slide]   │  ← Tap zone hint (fades after tutorial)
└─────────────────────────────────┘
```

## Controls
- **Jump**: tap once to jump; tap again while airborne for a double-jump.
- **Slide**: swipe down (or tap and hold) to slide under obstacles.
- **No left/right control** — the character always runs at the current auto-speed.
- No on-screen buttons — the entire screen is the tap zone (except the HUD strip at top).

## Gameplay Loop

### Infinite Structure
- The world scrolls infinitely. Speed starts slow and increases continuously over time.
- Distance is measured in meters, displayed as the score.
- Personal best distance is recorded per profile.

### Obstacles and Platforms
- Obstacles appear with enough visual lead time for the player to react at normal speed.
- Obstacle density and complexity increase with distance.
- Platform gaps require precision jumping — landing on a rooftop earns bonus points.

### Power-Ups
Power-ups appear as glowing scroll pickups on the path:
- **Ki Shield**: absorbs one hit (ninja flickers with a golden aura).
- **Speed Burst**: brief speed increase with screen-edge speed lines effect.
- **Slow-Mo**: briefly slows obstacle generation rate (world darkens and desaturates).
- **Double Points**: all distance/pickup scoring doubled for 10 seconds.
- **Magnet**: pulls nearby coins/pickups to the ninja automatically.

### Collectibles
- **Spirit Coins**: golden spinning coin pickups scattered along the path. Accumulate for an in-game currency used to unlock cosmetic eye-color variants.
- Coin multiplier increases with combo streaks (avoiding hits).

### Hit and Lives
- One hit ends the run (no lives). The ninja leaves a dramatic ink-splatter.
- Ki Shield (power-up) absorbs one hit.

### Milestones
Every 500m, a "Milestone" event triggers:
- Brief fanfare animation (flying paper cranes, lantern burst).
- Score multiplier increases by 0.1×.
- Obstacle pattern resets briefly (a short "breather" section).

## Scoring
| Event | Points/Distance |
|-------|----------------|
| Distance | 1 point per meter |
| Platform landing | 50 pts |
| Spirit Coin collected | 10 pts |
| Milestone reached | 500 pts |
| Near-miss obstacle | 25 pts |

## State Machine
```
Idle
 └─ TapToStart → Running
Running
 ├─ TapDetected → Running (jump or slide applied)
 ├─ PowerUpCollected → Running (effect active)
 ├─ ObstacleHit [shield active] → Running (shield consumed)
 └─ ObstacleHit [no shield] → Dead
Dead
 └─ (show result, retry or menu) → Idle
```

## HUD
- Distance/score: top-left (meters + multiplier indicator).
- Personal best: top-right (dims when not near record; brightens when approaching).
- Active power-up icons: bottom-right, small with countdown bars.
- No lives indicator (one-hit death).
