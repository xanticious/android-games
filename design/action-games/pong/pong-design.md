# Pong — Design Document

## Overview
A high-definition reimagining of Pong as a bat-and-ball court game. The ball bounces off the top and bottom walls (wallyball style). The player must swing their bat to deflect the ball back across the net before it hits their side wall. The opponent (AI) defends the other side. The key skill is timing and positioning bat swings — not sliding a paddle.

## Visual Style
- HD stylized court art: clean neon-on-dark aesthetic, sharp lines.
- The play field is a horizontal rectangle divided by a central net.
- Both walls (top and bottom) are solid reflective surfaces.
- The ball: a bright glowing sphere with a short motion-blur trail.
- The bat: a solid rectangular paddle shape that swings with a visible arc animation.
  - Forehand swing: the bat sweeps from one side to the other (e.g., right-to-left).
  - Backhand swing: mirror sweep.
- Swing arc: a brief ghost arc shows the bat's path immediately before impact.
- Net: pulsing glow effect when the ball crosses it.
- Point scored: losing side wall briefly flashes red; score increments with a pop animation.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score: AI]       [Score: You] │  ← HUD (top)
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ top wall                    │ │
│ │                             │ │
│ │  [AI BAT]   |NET|  [P BAT]  │ │  ← Play field (landscape orientation)
│ │                             │ │
│ │ bottom wall                 │ │
│ └─────────────────────────────┘ │
├─────────────────────────────────┤
│  [F/B Toggle]     [Tap to Swing]│  ← Controls (bottom strip)
└─────────────────────────────────┘
```
Note: The game uses landscape orientation for an optimal court feel.

## Controls

### Left Thumb — Forehand / Backhand Toggle
- A toggle button in the lower-left.
- **Forehand**: bat sweeps in the primary direction (e.g., from back of court toward the net).
- **Backhand**: bat sweeps in the reverse direction.
- Toggling changes the visual bat stance immediately.
- Strategy: use forehand/backhand to control ball angle and spin direction.

### Right Hand — Tap to Swing
- See `design/common/tap-targeting.md` — Variant C (Swing Targeting).
- The player taps anywhere in their half of the court.
- The bat swings to that position (or nearest reachable point) instantly.
- Cooldown: a fixed delay (0.8 seconds default) after each swing before the next swing is allowed.
- Cooldown visual: the bat becomes semi-transparent until the cooldown ends.

## Gameplay Loop

### Ball Physics
- The ball travels in straight lines, bouncing off the top and bottom walls at equal angles (no spin in basic mode).
- Hitting the ball with the bat adds a slight angle deflection based on where on the bat face it hits (edge hits angle outward, center hits straight).
- Forehand/backhand affects the deflection direction: forehand sends the ball toward the top wall, backhand toward the bottom wall.
- Ball speed increases slightly after each rally exchange (resets at each point).

### Scoring
- First to 11 points wins the set.
- A point is scored when the ball passes through the opponent's side wall.
- Sets won: best of 3 sets wins the match (vs. AI).

### AI Opponent
- Three difficulty settings: Easy, Medium, Hard.
- Easy: AI has a fixed (slow) reaction time and limited swing range.
- Medium: AI reacts faster and sometimes predicts wall bounces.
- Hard: AI near-perfectly tracks the ball with occasional intentional errors to remain beatable.

### Match Structure
- Player vs. AI.
- Match plays out as best-of-3 sets.
- Match result is recorded in local profile stats (wins, losses, best winning streak).

## Scoring Details
| Event | Record |
|-------|--------|
| Rally length (exchanges without a point) | shown in HUD for current rally |
| Longest rally (all-time) | stored in profile |
| Fastest point (shortest rally) | tracked |
| Win/Loss record vs. each difficulty | stored per profile |

## State Machine
```
Idle
 └─ MatchStart → Serving
Serving [player]
 └─ ServeTapped → Playing
Serving [AI]
 └─ AIServeDelay → Playing
Playing
 ├─ BallHitsPlayerWall → PointToAI
 ├─ BallHitsAIWall → PointToPlayer
 └─ SwingInput → Playing (bat swings, may hit ball)
PointToAI / PointToPlayer
 ├─ SetContinues → Serving
 └─ SetWon → SetOver
SetOver
 ├─ MatchContinues → Serving (next set)
 └─ MatchOver → MatchResult
MatchResult
 └─ (show results, rematch or menu) → Idle
```

## HUD
- Scores: top corners (AI left, Player right).
- Current set indicator: top-center (e.g., "Set 2 of 3").
- Current rally counter: center of net area (briefly visible, fades).
- Swing cooldown: arc indicator around the tap zone or bat icon.
- F/B toggle button: always visible in bottom-left.
