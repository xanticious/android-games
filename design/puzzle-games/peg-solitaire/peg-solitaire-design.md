# Peg Solitaire — Design Document

## Overview
- Peg Solitaire is a single-player board puzzle. Pegs sit in holes on a cross-shaped (or other) board with one hole empty. The player jumps a peg orthogonally over an adjacent peg into an empty hole beyond, removing the jumped peg.
- The goal is to reduce the board to a single peg, ideally finishing in the center.
- The game can stall when no jumps remain with more than one peg left; the player can undo or restart.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Board**: English cross (33 holes, default), European (37 holes), Triangle (15), Plus.
- **Starting empty hole**: Center (default) / choose.
- **Goal**: One peg anywhere (default) / one peg in center (harder).
- **Move hints**: on (default) / off.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Holes are recessed `Dark0` circles on a `Dark1` board; pegs are raised `Aqua2` discs.
- Selected peg lifts with an `Aqua1` highlight; legal destinations show a faint ring.
- A jump animates the peg arcing over its neighbor; the captured peg fades out (board-area only).

## Screen Layout
```
┌─────────────────────────────────────┐
│  Peg Solitaire  Pegs 18    00:25 ⚙? │
├─────────────────────────────────────┤
│        ● ● ●                         │
│        ● ● ●                         │
│    ● ● ● ○ ● ● ●                     │  ← cross board, ○ = empty hole
│    ● ● ● ● ● ● ●                     │
│    ● ● ● ● ● ● ●                     │
│        ● ● ●                         │
│        ● ● ●                         │
├─────────────────────────────────────┤
│  Undo  Hint        Moves left: 6     │
└─────────────────────────────────────┘
```

## How to Play
- Tap a peg, then tap an empty hole two cells away in a straight line with a peg in between.
- The jumped peg is removed.
- Keep jumping until one peg remains.

## Controls
- **Tap** a peg to select it; **tap** a highlighted destination to jump. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Drag** a peg directly onto a legal destination (shortcut).
- **Undo**: revert the last jump (unlimited).
- **Hint**: highlight one available jump.

## Gameplay Rules
- A legal move jumps a peg over exactly one orthogonally adjacent peg into the empty hole immediately beyond; the middle peg is removed.
- Win = exactly one peg remains (in center, if that goal is selected).
- Stall = more than one peg and no legal jumps; offer Undo/Restart.
- Legal-move generation, jump application, and stall/solve detection are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `PegSolitaireStateMachine` in `state/` exposes `StateFlow<PegSolitaireState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ PegSelected → Playing
 ├─ JumpMade → Playing (peg removed)
 ├─ Undo / Hint → Playing
 ├─ OnePegLeft → Solved
 └─ NoMovesLeft (>1 peg) → Stalled
Stalled
 ├─ Undo → Playing
 └─ NewGame → Playing
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins (per board) | yes |
| Center-finish wins | yes |
| Best (fewest pegs left on a loss) | yes |
| Current / best win streak | yes |

## HUD
- Top bar: title, peg count, timer, settings, help.
- Bottom: Undo, Hint, available-moves indicator.
- Win/stall message appears below the board; the final position stays visible.
