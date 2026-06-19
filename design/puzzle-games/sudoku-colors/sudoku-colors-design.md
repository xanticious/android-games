# Sudoku Colors — Design Document

## Overview
- Sudoku Colors is a single-player logic puzzle: classic 9×9 Sudoku where the nine symbols are **colors** instead of digits. Each row, column, and 3×3 box must contain all nine colors exactly once.
- Some cells are pre-filled (givens); the player deduces the rest. Each puzzle has a unique solution.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Difficulty**: Easy, Medium (default), Hard, Expert — controls the number/placement of givens.
- **Symbol mode**: Colors (default) / Colors + digit overlay — overlays 1–9 on each color for accessibility (and for players who prefer numbers).
- **Pencil marks**: on (default) — note candidate colors per cell.
- **Highlight conflicts**: on (default) — flag a color repeated in a row/column/box.
- **Auto-pencil**: optional helper that fills obvious candidates.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Nine distinct colors drawn from the palette; because color is the symbol, the **digit-overlay option and distinct value shapes** guarantee color-blind playability (color is never the sole differentiator when overlay is on).
- 3×3 boxes separated by heavier `Dark2` borders; givens are visually locked (slightly inset, non-editable).
- Selected cell and its peers (same row/column/box) get a soft `Aqua1` highlight; conflicts use the `error` token.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Sudoku Colors  00:46           ⚙ ? │
├─────────────────────────────────────┤
│   ┌───────┬───────┬───────┐         │
│   │ ▰ ▱ ▰ │ ▱ ▰ ▱ │ ▰ ▱ ▰ │         │  ← 9×9 grid of colored cells
│   │ ▱ ▰ ▱ │ ▰ ▱ ▰ │ ▱ ▰ ▱ │         │     (givens locked)
│   │ …                             │         │
│   └───────┴───────┴───────┘         │
├─────────────────────────────────────┤
│  palette [1..9 colors]  Pencil      │  ← color input pad + pencil toggle
│  Undo            Cells left: 31     │
└─────────────────────────────────────┘
```

## How to Play
- Tap a cell, then tap a color on the pad to fill it.
- Every row, column, and 3×3 box must contain all nine colors with none repeated.
- Solve when the whole grid is validly filled.

## Controls
- **Tap** a cell then **tap** a color on the input pad to place it. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Hold (long-press)** a cell, or toggle **Pencil**, to add/remove candidate-color marks.
- **Tap** a filled (non-given) cell with the same color: clears it.
- **Undo**: revert the last entry.

## Gameplay Rules
- A placement is a conflict if the color already appears in that cell's row, column, or 3×3 box.
- Solved = every cell filled with no conflicts (equivalently, matches the unique solution).
- Givens cannot be changed.
- Conflict detection, candidate computation, and unique-solution generation/grading are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `SudokuColorsStateMachine` in `state/` exposes `StateFlow<SudokuColorsState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ ColorPlaced / Cleared → Playing (conflict checks updated)
 ├─ PencilMarkToggled → Playing
 ├─ UndoRequested / HintRequested → Playing
 └─ GridCompleteNoConflicts → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved (per difficulty) | yes |
| Best time (per difficulty) | yes |
| No-hint / no-mistake solves | yes |
| Current / best streak | yes |

## HUD
- Top bar: title, timer, settings, help.
- Bottom: color input pad, Pencil toggle, Undo, remaining-cells status.
- Solve message appears below the board; the completed grid stays visible.
