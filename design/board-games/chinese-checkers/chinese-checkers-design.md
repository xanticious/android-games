# Chinese Checkers — Design Document

## Overview
- Chinese Checkers is a six-player race game presented as 1 human versus 5 AI opponents on a fully populated six-point star.
- Each player starts with 10 marbles in one triangular home area and tries to be the first to fill the opposite triangle.
- Moves follow standard rules: one adjacent step or a chain of hops over any adjacent occupied piece, regardless of color.
- There is no capture; the drama comes from traffic, lane control, and timing long hop chains.
- The human selects one color before the match and the five remaining colors are assigned to AI opponents.
- Every AI opponent has its own difficulty setting so mixed-skill tables are possible.
- Training Wheels mode can optionally restrict the player to stronger moves only.
- All results and local stats stay on device.
- The board must remain visible during turn prompts, AI progress, finish order, and result messaging.

## Visual Style
- Material 3 presentation using the underwater palette from `ui/theme/Color.kt`.
- Background surfaces rely on `Dark0` and `Dark1`, while the star board rests on a crisp `Dark1` panel.
- Connection lines, board nodes, and destination outlines use `Aqua3`; readable text uses `Aqua0`.
- Selection rings, valid hops, and route previews use `Aqua2`, with softer helper accents in `Aqua1`.
- Progress meters and turn emphasis use `Aqua4` tracks with `Aqua2` fills.
- Player marbles keep their own game colors, but surrounding chrome, highlights, and status UI stay tied to the underwater palette.
- AI thinking uses subtle pulse indicators around that opponent's home triangle, not full-screen blocking effects.
- Animations are short and tactile: marble step, hop chain bounce, route preview fade, and below-board result reveal.
- Any result celebration appears below the board rather than over it.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Chinese Checkers  You: Teal    [⚙]  │  ← Top bar
├─────────────────────────────────────┤
│ Turn: You   Finish: —   Training on  │  ← Status strip
├─────────────────────────────────────┤
│             six-point star           │
│          marble board centered       │  ← Board anchor
│       with home/destination zones     │
├─────────────────────────────────────┤
│ Opponents: N Easy  NE Hard  SE Med   │  ← AI summary
│ Select a marble. Hops are highlighted│  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait stacks status, star board, opponent summary, and prompt panel.
- Tablets may keep per-opponent progress cards in side columns while preserving the central board.
- Finish-order and victory/defeat content appears in the bottom panel and never covers the star.

## Settings
- **Player color**: choose any of the six seats or Random.
- **North AI difficulty**: Easy, Medium, Hard.
- **North-East AI difficulty**: Easy, Medium, Hard.
- **South-East AI difficulty**: Easy, Medium, Hard.
- **South AI difficulty**: Easy, Medium, Hard.
- **South-West AI difficulty**: Easy, Medium, Hard.
- **North-West AI difficulty**: Easy, Medium, Hard.
- Seats occupied by the human hide the corresponding AI difficulty, leaving exactly five active AI difficulty controls.
- **Training Wheels Mode** (on/off, default on): restricts player selection to strong moves and explains blocked or weak choices.
- **Show hop chains** (on/off, default on): previews all reachable landing points and the selected route.
- **Fast AI turns** (on/off, default on): summarizes AI movement after a short visible animation.
- **Progress meters** (on/off, default on): shows how many marbles each player has advanced into the destination triangle.

## How to Play
- Choose a starting color; your goal is the opposite triangle.
- On your turn, move one marble either one adjacent step or through a chain of jumps.
- A jump hops over any adjacent occupied marble into the empty space immediately beyond it.
- Jump chains may continue as long as each next hop is legal; marbles are never captured or removed.
- You may hop over your own marbles or any opponent's marbles.
- The first player to fill all 10 destination spaces wins; remaining finish order can be recorded for stats.
- Training Wheels can limit your available moves to stronger race-progressing options while still teaching why other legal moves are not recommended.

## Controls
- Tap a marble to select it; reachable steps and hop landings highlight.
- Tap a highlighted landing to move immediately if it is a single step or a complete selected route.
- For multi-hop turns, tap each landing in sequence or tap an available final route preview when shown.
- Tap Undo Hop before committing the full chain to step back within the current move only.
- Long-press a destination triangle or highlighted landing for a short explanation.

## AI Opponents
- **Easy**: prefers forward movement and obvious hops, but may block itself and ignore long chains.
- **Medium**: balances forward progress, lane clearing, traffic avoidance, and opportunistic multi-hop routes.
- **Hard**: plans multi-turn routes, opens lanes for its own marbles, blocks key opponent corridors, and times long hop chains.
- Each of the five AI opponents uses its own selected difficulty; difficulty changes decision quality only, not rules.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses by seat and AI difficulty mix | yes |
| Finish placement distribution | yes |
| Fastest win by turns | yes |
| Longest player hop chain | yes |
| Average destination-fill progress | yes |
| Training Wheels games completed | yes |

## State Machine
- A dedicated `ChineseCheckersStateMachine` in `state/` exposes `StateFlow<ChineseCheckersState>`.
```
Idle
 └─ MatchStarted → ChoosingSeat
ChoosingSeat
 └─ SeatChosen → PlayerTurn / AiTurn
PlayerTurn
 ├─ MarbleSelected → ChoosingRoute
 ├─ InvalidMoveExplained → PlayerTurn
 └─ Surrendered → GameOver
ChoosingRoute
 ├─ HopAdded → ChoosingRoute
 ├─ RouteUndone → ChoosingRoute
 ├─ RouteCommitted → ResolvingMove
 └─ SelectionCleared → PlayerTurn
ResolvingMove
 ├─ PlayerFinished → FinishOrderUpdated
 └─ TurnAdvanced → AiTurn / PlayerTurn
AiTurn
 └─ AiMoveResolved → ResolvingMove
FinishOrderUpdated
 ├─ MatchComplete → GameOver
 └─ NextTurn → AiTurn / PlayerTurn
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `ChineseCheckersRules` controller validates star-board coordinates, adjacent steps, hop chains, destination occupancy, finish detection, route previews, and Training Wheels candidate filtering; unit tests cover movement and finish edge cases without Android imports.

## HUD
- Top bar shows game name, human color, active turn, and settings access.
- Status strip shows current turn, finish placement, Training Wheels state, and route length.
- Opponent summary shows five AI seats, difficulty labels, and progress meters.
- Prompt/result panel shows selected route, invalid-move explanations, finish order, and final results below the board.
- Victory/defeat presentation follows `design/common/victory-defeat.md`: results never cover the board.
