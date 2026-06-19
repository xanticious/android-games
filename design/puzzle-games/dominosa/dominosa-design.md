# Dominosa — Design Document

## Overview
- Dominosa is a single-player deduction puzzle played on a grid of numbers. The grid contains every domino of a complete set exactly once, but the domino boundaries are hidden — only the pip numbers are shown.
- The player partitions the whole grid into 1×2 dominoes (horizontal or vertical) so that **each domino value pair appears exactly once**.
- A puzzle of "size N" uses the complete domino set from 0–N. That set has (N+1)×(N+2)/2 dominoes and fills a grid of **(N+1) rows × (N+2) columns**.
- Every generated puzzle has a unique solution.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings (sizes)
- **Size**: choose the highest pip value N, which determines the board and the domino set:
  | Size (N) | Dominoes | Grid (rows × cols) |
  |----------|----------|--------------------|
  | 3 | 10 | 4 × 5 |
  | 4 | 15 | 5 × 6 |
  | 5 | 21 | 6 × 7 |
  | 6 (default) | 28 | 7 × 8 |
  | 7 | 36 | 8 × 9 |
  | 8 | 45 | 9 × 10 |
- **Show remaining-pairs tracker**: on (default) / off — a checklist of which value pairs are still unplaced.
- **Highlight conflicts**: on (default) / off — flag a duplicated pair or an over-counted value live.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Each cell shows its pip number (digit) centered on a `Dark0` tile; grid lines `Dark2`.
- A placed domino draws a rounded capsule spanning its two cells, filled `Aqua2` with the two numbers readable on top.
- A pending/edge mark (the player asserting two cells are split or joined) uses a thin `Aqua1` connector or a small divider.
- Conflicts pulse with the Material 3 `error` token border (no full-screen flash).

## Screen Layout
```
┌─────────────────────────────────────┐
│  Dominosa        00:42          ⚙ ? │  ← Top bar (timer, settings, help)
├─────────────────────────────────────┤
│   3 1 2 0 2                         │
│   1 0 3 3 1                         │  ← (N+1)×(N+2) number grid
│   2 2 0 1 3                         │     dominoes drawn as the player commits them
│   0 3 1 2 0                         │
├─────────────────────────────────────┤
│  Undo  Hint   Pairs left: 4 / 10    │  ← Bottom action + status row
└─────────────────────────────────────┘
```

## How to Play
- Cover the whole grid with dominoes so every number-pair (0-0, 0-1, … up to N-N) is used exactly once.
- Each domino covers two orthogonally adjacent cells. No cell is left uncovered and none is covered twice.
- Use the remaining-pairs tracker to deduce which placements are forced.

## Controls
- **Tap the edge between two cells**: place a domino joining them (or cycle: join → blocked/split → clear). See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Drag from one cell to an adjacent cell**: place a domino across them.
- **Hold a placed domino**: remove it.
- **Undo / Hint**: standard; Hint completes one forced domino.

## Gameplay Rules
- A placement is legal only if both target cells are currently uncovered and adjacent.
- The puzzle is solved when every cell is covered and the multiset of placed value-pairs equals the full domino set with no duplicates.
- Conflict detection (optional setting) flags the first duplicated pair as soon as it is created.

## Puzzle Generation (controller)
- Start from a solved tiling of the (N+1)×(N+2) grid using each domino once, then erase the domino boundaries to leave only numbers.
- Verify uniqueness with a solver; regenerate if multiple solutions exist.
- Generation and solving live in `controller/` as pure functions and are unit-tested for: correct grid dimensions per size, complete-set coverage, and unique solvability.

## State Machine
A dedicated `DominosaStateMachine` in `state/` exposes `StateFlow<DominosaState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ DominoPlaced → Playing (coverage updated)
 ├─ DominoRemoved → Playing
 ├─ UndoRequested → Playing
 ├─ HintRequested → Playing (one forced domino placed)
 └─ BoardComplete [valid set] → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved (per size) | yes |
| Best time (per size) | yes |
| No-hint solves | yes |
| Current / best daily streak | yes |

## HUD
- Top bar: title, timer, settings, help.
- Bottom row: Undo, Hint, and a "pairs left" / "cells left" status.
- Solve message appears below the board; the completed tiling stays fully visible.
