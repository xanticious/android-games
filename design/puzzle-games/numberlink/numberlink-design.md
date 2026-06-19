# Numberlink — Design Document

## Overview
- Numberlink is a single-player path puzzle. The grid contains pairs of matching numbers (endpoints). The player draws a path connecting each pair.
- Paths run orthogonally through cells, may not cross or overlap each other, and a perfect solution **fills every cell** of the grid.
- Each puzzle has a unique solution that covers the whole board.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Size**: 5×5 (Easy), 7×7 (default), 9×9, 12×12.
- **Require full coverage**: on (default) / off — when on, every cell must be used; when off, simply connecting all pairs without crossings wins.
- **Auto-clear on overlap**: on (default) — drawing through an existing path trims the older path from that point.
- **Color + number labels**: both shown (default) so endpoints are distinguishable without color.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Endpoints are numbered discs; each pair uses a distinct palette color **and** its number, so color-blind players can match by digit.
- Paths render as thick rounded ribbons in the pair's color, flowing cell to cell.
- The active path being drawn highlights with an `Aqua2` glow; a completed pair's endpoints get a check accent.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Numberlink    Pairs 3/5        ⚙ ? │
├─────────────────────────────────────┤
│   ①═══╗ . ②                          │
│   . . ║ . ║                          │  ← grid with numbered endpoints
│   ② . ╚═══╝   ③                       │     and drawn ribbon paths
│   . . . ③ . ①                        │
├─────────────────────────────────────┤
│  Undo  Clear     Cells filled 18/25 │
└─────────────────────────────────────┘
```

## How to Play
- Drag from one numbered endpoint to its twin to lay a path.
- Paths can't cross or share cells; for a perfect solve, fill every cell.
- Connect all pairs to win.

## Controls
- **Drag** from an endpoint (or any cell of an existing path) to extend/route a path. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Tap** an endpoint: clears that pair's path.
- **Drag across another path**: trims the crossed path (auto-clear) so the new one can pass.
- **Undo / Clear**: revert last segment / clear all paths.

## Gameplay Rules
- A path occupies whole cells and connects exactly its two like-numbered endpoints.
- No two paths may occupy the same cell (no crossings/overlaps).
- Win = all pairs connected (and, if full-coverage is on, every cell occupied).
- Path validity, coverage checking, and a uniqueness solver are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `NumberlinkStateMachine` in `state/` exposes `StateFlow<NumberlinkState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ PathExtended → Playing
 ├─ PathCleared → Playing
 ├─ UndoRequested → Playing
 └─ AllPairsConnected [coverage ok] → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved (per size) | yes |
| Best time (per size) | yes |
| Perfect (full-coverage) solves | yes |
| Current / best streak | yes |

## HUD
- Top bar: title, pairs-connected counter, settings, help.
- Bottom: Undo, Clear, and a cells-filled status (for full-coverage mode).
- Solve message appears below the board; the completed paths stay visible.
