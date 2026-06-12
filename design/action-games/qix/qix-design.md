# Qix — Design Document

## Overview
A high-definition reimagining of the 1981 vector arcade classic. The player draws lines across an open playfield to claim territory. A bouncing geometric entity (the Qix) moves through unclaimed space. Sparx enemies patrol the boundary of claimed territory. Claim a target percentage of the field to complete each level. Risk management is central — fast draws claim more points but leave you exposed longer.

## Visual Style
- HD neon-on-dark aesthetic: deep black background, vivid geometric shapes.
- Playfield: a crisp glowing border rectangle.
- Unclaimed area: dark void with subtle texture.
- Claimed area: filled with a bright translucent color (the "stix fill"), building up layer by layer.
- Qix: an animated geometric figure — a slowly morphing star/diamond shape with glowing edges. Color shifts over time (using accent palette from `ui/theme/Color.kt`).
- Sparx: bright moving dots that trail a faint glow as they patrol the border.
- Player: a small bright square cursor with a directional indicator.
- Drawing lines: the player's active line glows brighter than the claimed fill, clearly showing the incomplete territory.
- When territory is claimed: a brief fill-sweep animation floods the newly enclosed area with color.
- Level complete: the filled playfield bursts into a brief particle explosion.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]  [Claimed: 67%]  [Lives]│  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │                         │    │
│  │  PLAYFIELD              │    │
│  │  [■■■] claimed          │    │
│  │  [ ] unclaimed (Qix)    │    │
│  │  →player cursor         │    │
│  └─────────────────────────┘    │
│                                 │
└─────────────────────────────────┘
       [D-pad or joystick — bottom]
```

## Controls
- **Move player**: left thumb virtual joystick (see `design/common/virtual-joystick.md`) OR a visible on-screen D-pad (4-directional).
- Movement is 4-directional (up, down, left, right) — no diagonal movement.
- **Draw mode**: the player enters draw mode automatically when they move off the boundary into unclaimed space.
- **Draw type**: a toggle button switches between Fast Draw and Slow Draw.
  - **Slow Draw**: lower risk, lower reward. Safe to traverse through slow sections.
  - **Fast Draw**: higher risk (more points), player moves faster but the line glows brighter (attracting Sparx faster).
- **Claim territory**: drawing a line that reconnects to the existing boundary closes the enclosed area, which fills immediately.

## Gameplay Loop

### Core Loop
1. Player starts on the playfield boundary.
2. Player moves off the boundary into unclaimed space, drawing a line behind them.
3. The player reconnects to the boundary (any part of it), enclosing area.
4. The enclosed area fills in with color. The side not containing the Qix is claimed.
5. Claimed percentage updates. If ≥ target percentage (default: 75%), the level is complete.
6. Repeat until level complete or lives are exhausted.

### The Qix
- The Qix bounces around the unclaimed area.
- If the Qix touches the player's active (in-progress) line before it is completed, the player loses a life.
- The Qix does not enter claimed territory.
- In later levels, 2 Qix entities appear simultaneously.
- The Qix changes direction and speed unpredictably.

### Sparx
- Sparx spawn from the corners of the playfield boundary and patrol the boundary edge.
- If a Sparx reaches the player's position while they are on the boundary or drawing, the player loses a life.
- There are always 2 Sparx per level (more in later levels). They travel in opposite directions.
- Sparx speed increases with level.

### Fuse
- If the player stays in draw mode without connecting back to the boundary for too long, a "Fuse" ignites at the starting point of the active line and travels toward the player.
- If the Fuse catches the player, a life is lost.
- The Fuse is a secondary pressure mechanic to prevent stalling.
- Fuse speed: slow at level 1, increasing each level.

### Scoring
| Event | Points |
|-------|--------|
| Claim territory (slow draw) | 10 × percentage_of_total_area_claimed |
| Claim territory (fast draw) | 20 × percentage_of_total_area_claimed |
| Trap Qix inside claimed area | 1000 bonus |
| Level complete at exactly 75% | base score |
| Level complete above 75% | +50 per extra % claimed |
| Maximum fill (≥95%) | 3000 bonus |

### Lives
- 3 lives per game.
- Life lost: Qix touches active line, Sparx touches player, or Fuse catches player.
- Brief invincibility after respawn (2 seconds).
- On respawn, any in-progress line is abandoned (the partial line disappears and the player returns to the nearest boundary point).

## Difficulty Scaling
| Level | Changes |
|-------|---------|
| 1–2 | 1 Qix, 2 Sparx (slow), no Fuse |
| 3–5 | 1 Qix (faster), 2 Sparx, Fuse introduced |
| 6–8 | 1 Qix (fast), 3 Sparx, faster Fuse |
| 9–12 | 2 Qix, 3 Sparx, fast Fuse |
| 13+ | 2 Qix (fast), 4 Sparx, very fast Fuse |

## State Machine
```
Idle
 └─ LevelStart → OnBoundary
OnBoundary
 ├─ MoveOffBoundary → Drawing
 └─ SparxHitsPlayer → LifeLost
Drawing
 ├─ ReconnectsToBoundary → Claiming
 ├─ QixTouchesLine → LifeLost
 ├─ SparxTouchesPlayer → LifeLost
 └─ FuseCatchesPlayer → LifeLost
Claiming
 ├─ TargetReached → LevelComplete
 └─ TargetNotReached → OnBoundary
LifeLost
 ├─ [lives > 0] → OnBoundary (respawn)
 └─ [lives == 0] → GameOver
LevelComplete
 └─ NextLevel → OnBoundary (new level)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Claimed percentage: top-center (e.g., "67% / 75%"). Progress bar fills.
- Lives: top-right (player icon repeats).
- Draw mode indicator (Slow/Fast): bottom-left toggle button with visual state.
- Fuse warning: when Fuse is active, a small animated fuse icon appears with a countdown pip.
