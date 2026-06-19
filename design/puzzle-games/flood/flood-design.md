# Flood — Design Document

## Overview
- Flood is a single-player color-flooding puzzle. The board is a grid of randomly colored cells. A "flood region" starts at the **top-left cell** and includes every same-colored cell connected orthogonally to it.
- On each move the player picks a color; the flood region changes to that color and absorbs any newly adjacent cells of that color. Repeatedly choosing colors grows the region until it covers the entire board.
- The objective is to flood the whole board. This version does **not** fail the player for using extra moves — instead it always reports how many moves they took **above the optimal minimum** for that exact board.
- **Undo is always available** so the player can experiment freely.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Minimum-Moves Baseline (signature mechanic)
- For every generated board the controller computes the **minimum number of moves** needed to flood it, via a solver (BFS/IDA* over flood states). This minimum is the baseline.
- The player is never forced to hit the minimum; they keep choosing colors until the board is fully flooded.
- On completion the result line reports moves relative to optimal, e.g.:
  - `Solved in 23 moves — optimal! (+0)` or
  - `Solved in 27 moves — 4 above optimal (+4)`.
- The "+N above optimal" value is the core score the player tries to drive toward 0.
- The minimum solver is a pure function in `controller/` and is unit-tested on small known boards.

## Settings
- **Number of colors**: 4, 5, 6 (default), 7, 8 — fewer colors make boards easier, more colors harder.
- **Target moves (handicap above optimal)**: the par the player aims for, expressed as **moves above the minimum**: `+0` (perfect), `+2`, `+4` (default), `+6`. The target is purely a personal goal/medal threshold — exceeding it does not end the game.
- **Board size**: Small (10×10), Medium (14×14, default), Large (18×18).
- The displayed par = computed minimum + chosen handicap, recomputed per board.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Up to 8 distinct flood colors are drawn from the palette (`Aqua0`, `Aqua1`, `Aqua2`, `Aqua3`, `Aqua4`, plus `Dark0`, `Dark1`, `Dark2` shades), each paired with a **distinct shape/glyph or pattern** so color-blind players can tell them apart (palette-only differentiation is never relied upon — see [`common/puzzle-controls.md`](../../common/puzzle-controls.md) accessibility).
- Flood spread: when a color is chosen, the region recolors with a short ripple from the top-left over ≤200 ms (board-area only).
- The current flood region carries a subtle outline so its extent is always clear.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Flood        Moves 12   ⚙ ?        │  ← Top bar (move count, settings, help)
├─────────────────────────────────────┤
│   ███▓▓░░▒▒██                       │
│   ███▓▓░░▒▒██                       │  ← N×N colored board, flood from top-left
│   ▒▒▒██▓▓░░▒▒                       │
│   ░░▒▒██▓▓░░██                      │
├─────────────────────────────────────┤
│  [●][◆][▲][■][★][⬡]   Undo          │  ← Color palette buttons + Undo
│  Par: 18  (min 14, +4)              │  ← Status row
└─────────────────────────────────────┘
```

## How to Play
- Tap a color button to flood the top-left region with that color, absorbing connected same-color cells.
- Keep choosing colors until the entire board is one color.
- Try to finish at or under par; the result shows exactly how many moves above optimal you used.
- Use Undo freely to rethink a move — it never costs anything.

## Controls
- **Tap a color button**: apply that color as the next flood move. (Tapping the current region color is a no-op and is disabled.)
- **Undo** (always present): revert the last color choice; unlimited back to the start of the board.
- See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).

## Gameplay Rules
- The flood region is the maximal orthogonally connected set of same-colored cells containing the top-left cell.
- Choosing color C recolors the entire region to C, then re-expands the region to include any C-colored cells now adjacent.
- The board is solved when every cell shares one color.
- Move count increments on each applied color (not on undo). Undo decrements the count.

## State Machine
A dedicated `FloodStateMachine` in `state/` exposes `StateFlow<FloodState>`.
```
Idle
 └─ StartGame → Playing (minimum computed for this board)
Playing
 ├─ ColorChosen → Playing (region grown, moves++)
 ├─ UndoRequested → Playing (moves--)
 └─ BoardUniform → Solved (report moves vs optimal)
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Boards solved (per size / color count) | yes |
| Best "above optimal" (lowest +N, per config) | yes |
| Perfect (+0) solves | yes |
| Boards solved within target par | yes |
| Total undos used (info) | yes |

## HUD
- Top bar: title, current move count, settings, help.
- Color palette row: one button per active color, each labeled with its distinguishing glyph; the current region color is shown disabled.
- Status row: live `Par: X (min Y, +Z)`. On solve, the result line (`Solved in N moves — K above optimal`) appears **below** the board; the finished board stays fully visible.
