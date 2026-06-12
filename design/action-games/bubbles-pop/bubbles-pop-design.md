# Bubbles Pop — Design Document

## Overview
A turn-based bubble shooter with high-definition visuals. The player fires one bubble per turn from a cannon at the bottom, matching groups of 3+ same-colored bubbles to pop them. The goal is to clear the hanging cluster before it descends too low.

## Visual Style
- HD glossy bubble art: each bubble is a translucent sphere with a specular highlight and color-matched inner glow.
- Bubbles come in 5–7 distinct colors (all from `ui/theme/Color.kt`), clearly distinguishable in both light and dark themes.
- Bubble cluster hangs from the top of the screen; bubbles in the cluster sway slightly on a gentle idle animation.
- Popped bubbles: burst particle effect (shards + color mist) before disappearing.
- Falling bubbles (disconnected from cluster): bounce once at the bottom before disappearing.
- Cannon: smooth metal tube at screen center-bottom, rotating to aim.
- Background: soft underwater gradient (matching the app's underwater palette).

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]   [Level]   [Danger]   │  ← HUD (top)
├─────────────────────────────────┤
│  ● ● ● ● ● ● ● ● ● ●           │
│   ● ● ● ● ● ● ● ● ●            │
│  ● ● ● ● ● ● ● ● ● ●           │  ← Bubble cluster (descends over turns)
│   ...                           │
│  ─ ─ ─ ─ DANGER LINE ─ ─ ─     │
│                                 │
│         [aim preview]           │
│          [CANNON]               │  ← Control zone (bottom)
└─────────────────────────────────┘
```

## Controls
- **Aim**: drag left or right from the cannon to set the aim angle. A dotted line preview shows the trajectory including wall bounces.
- **Fire**: release the drag or tap a "Fire" button to shoot the bubble.
- One shot per turn.
- The next bubble in the queue is shown beside the cannon before firing.

## Gameplay Loop

### Turn Structure
1. **Aim Phase**: Player sets angle. Preview shows path and landing position.
2. **Fire Phase**: Bubble travels along the preview path (can bounce off side walls), attaches to the cluster.
3. **Match Phase**: Check for groups of 3+ matching bubbles. Pop them; remove any disconnected bubbles (they fall).
4. **Descend Phase**: If no match was made this turn, the entire cluster descends one row.
5. If the cluster's lowest bubble crosses the danger line → game over.

### Bubbles
- Active colors in play: 5 at level 1, up to 7 at later levels.
- Cluster generates procedurally with controlled color distribution (no unsolvable states).
- Special bubbles introduced at level 3+:
  - **Bomb bubble**: popping this also destroys all 6 adjacent bubbles.
  - **Rainbow bubble**: matches any color (fired by the player as a rare bonus).
  - **Stone bubble**: cannot be matched; must be disconnected from the cluster by removing surrounding bubbles.

### Win / Loss
- **Win**: clear the entire bubble cluster.
- **Loss**: any bubble crosses the danger line at the bottom of the play area.

## Scoring
| Event | Points |
|-------|--------|
| Pop 3 bubbles | 50 |
| Pop 4 bubbles | 100 |
| Pop 5+ bubbles | 150 + 30 per extra bubble |
| Cascade (fallen disconnected bubbles) | 20 per bubble |
| Bomb explosion | 200 |
| Level complete bonus | 300 × level_number |

## Difficulty Scaling
- More bubble colors added per level.
- Stone bubbles introduced at level 3.
- Cluster starts with more rows at higher levels.
- Descend rate: every 2 missed shots (no match) at level 1, every 1 missed shot at level 5+.

## State Machine
```
Idle
 └─ LevelStart → AimPhase
AimPhase
 └─ FireTapped → FirePhase
FirePhase
 └─ BubbleAttached → MatchPhase
MatchPhase
 ├─ MatchFound → ResolutionPhase (pops + falls)
 └─ NoMatch → DescendPhase
ResolutionPhase
 ├─ ClusterEmpty → LevelComplete
 └─ ClusterRemains → AimPhase
DescendPhase
 ├─ BelowDangerLine → GameOver
 └─ AboveDangerLine → AimPhase
LevelComplete
 └─ NextLevel → AimPhase (new cluster)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Level: top-center.
- Danger indicator: top-right (fills red as cluster nears the danger line).
- Next bubble preview: bottom-left corner.
- Turns without a match (misfire counter): bottom-right, shows current miss streak with a warning color at 2+ misses.
