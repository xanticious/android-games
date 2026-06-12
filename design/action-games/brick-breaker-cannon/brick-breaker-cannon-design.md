# Brick Breaker (Cannon) — Design Document

## Overview
A turn-based brick breaker with physics-driven projectile arcs. The cannon is fixed on the left side of the screen and fires balls that follow gravity-affected arc trajectories — similar in feel to Angry Birds. The goal is to destroy all special (target) bricks within a limited number of turns.

## Visual Style
- Warm, earthy palette contrasting with the cool underwater app theme — the cannon level "arena" feels like a siege.
- Cannon: a chunky mechanical launcher mounted on a pivot on the left wall. Rotates to aim.
- Bricks: solid stone or wood-textured blocks arranged in structures on the right half of the field.
- Special (target) bricks: glowing gold border with a crown/star icon.
- Projectile: a heavy cannonball with a visible arc trajectory preview (dotted line + landing X marker).
- Physics: realistic arc including bounces off walls and floor (up to 2 bounces per ball before disappearing).
- Destruction: bricks crumble with debris particles; structural gaps can cause higher bricks to fall.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]  [Target Bricks: N]  [Turns Left: N] │  ← HUD
├─────────────────────────────────┤
│ [C]     │                       │
│ [A]     │   BRICK STRUCTURES    │
│ [N]     │   (right side field)  │
│ [N]     │                       │
│ [O]     │                       │
│ [N]     │                       │
├─────────────────────────────────┤
│  [Aim Dial / Drag to Aim]       │  ← Control zone
└─────────────────────────────────┘
```

## Controls
- **Aim**: drag the cannon barrel up/down or use a circular aim dial at the bottom to set the launch angle.
- A real-time arc preview updates as the angle changes, showing the ball's full trajectory including bounces.
- **Fire**: tap a "Fire" button or release the drag to launch.
- After firing, the next turn begins after all physics have resolved.
- Cannon angle is adjustable from 5° (nearly straight right) to 85° (nearly straight up).

## Gameplay Loop

### Turn Structure
1. **Aim Phase**: Player adjusts the cannon angle. Arc preview visible.
2. **Fire Phase**: Ball launches, travels along the arc, bounces (up to 2 times), and hits bricks.
3. **Resolution Phase**: Damaged/destroyed bricks animate. Remaining bricks above gaps fall (gravity-style cascade).
4. **Check Phase**: Check if all target bricks are destroyed (win) or turns are exhausted (loss).
5. Return to Aim Phase for next turn.

### Bricks
- **Standard bricks**: various HP values (1–4). Block the cannon's path.
- **Target bricks**: must all be destroyed to win the level. Count shown in HUD. Glowing gold appearance.
- **Steel bricks**: immune to direct hits; can only be moved/displaced by falling cascade mechanics.
- Bricks obey simple falling physics when support bricks beneath them are destroyed.

### Ball Physics
- One ball fired per turn (default). Multi-shot power-up fires 2 balls simultaneously.
- Ball travels along a parabolic arc affected by a fixed gravity constant.
- Ball bounces off left/right/top walls and the floor (up to 2 bounces total).
- Ball deals damage equal to a per-level base value (default: 1 damage per hit).
- Power Shot power-up doubles damage.

### Power-Ups
Power-up bricks release collectibles when destroyed. Collected automatically if the ball hits the release point:
- Explode: destroys all bricks in a radius around the target brick.
- Multi-Shot: next turn fires 2 balls simultaneously.
- Power Shot: next ball deals double damage.
- Clear Screen: destroys all non-steel standard bricks instantly.

### Win / Loss
- **Win**: all target bricks destroyed within the turn limit.
- **Loss**: turns run out and at least one target brick remains.

## Levels
- Each level is a hand-crafted or procedurally generated brick layout with a specific turn limit.
- Turn limit = base + (structure complexity factor).
- Early levels: open structures, easy arc angles. Later levels: enclosed structures, tight angles, steel brick shields.

## Scoring
| Event | Points |
|-------|--------|
| Destroy standard brick | 50 |
| Destroy target brick | 300 |
| Cascade destruction (per extra brick) | 75 |
| Turns remaining at level end | 200 × turns_left |
| All bricks cleared (not just targets) | 1000 bonus |

## State Machine
```
Idle
 └─ LevelStart → AimPhase
AimPhase
 └─ FireTapped → FirePhase
FirePhase
 └─ BallResolved → ResolutionPhase
ResolutionPhase
 ├─ AllTargetsDestroyed → LevelComplete
 ├─ TurnsRemain → AimPhase
 └─ TurnsExhausted → GameOver
LevelComplete
 └─ NextLevel → AimPhase (next level)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Target bricks remaining: top-center with icon.
- Turns remaining: top-right.
- Active power-ups: bottom-left, stacked with icons.
