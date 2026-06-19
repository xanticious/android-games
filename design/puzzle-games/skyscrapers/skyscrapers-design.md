# Skyscrapers — Design Document

## Overview
- Skyscrapers is a single-player Latin-square logic puzzle. The player fills an N×N grid with building heights 1..N so that each row and column contains every height exactly once.
- Edge clues state how many buildings are **visible** looking along that row/column; a taller building hides all shorter ones behind it.
- Each puzzle has a unique solution derivable by logic.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Size**: 4×4 (Easy), 5×5 (default), 6×6, 7×7.
- **Pencil marks**: on (default) / off — note candidate heights in a cell.
- **Auto-check visibility clues**: on (default) — satisfied edge clues dim; violated ones flag.
- **Given cells**: some puzzles seed a few fixed heights for accessibility.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Grid on `Dark1`; edge clue numbers in gutters on all four sides.
- Heights shown as large digits; optionally tinted by magnitude (low `Aqua0` → high `Aqua4`) but always with the digit so it is never color-only.
- Pencil marks are small digits in a corner cluster; conflicts use the `error` token.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Skyscrapers   00:40            ⚙ ? │
├─────────────────────────────────────┤
│        2   1   2   3                 │  ← top clues
│      ┌───┬───┬───┬───┐               │
│   2  │ 1 │ 2 │ 3 │ 4 │  1            │  ← left/right clues + grid
│   1  │ 4 │ 3 │ 2 │ 1 │  4            │
│   …                                  │
│        3   2   1   2     (bottom)    │
├─────────────────────────────────────┤
│  Undo  Pencil   Cells left: 9       │
└─────────────────────────────────────┘
```

## How to Play
- Fill each row and column with the heights 1..N, no repeats.
- Make the count of visible buildings from each edge match its clue (taller buildings hide shorter ones behind them).
- Solve when every clue and the Latin-square rule are satisfied.

## Controls
- **Tap** a cell then **tap** a height on the input pad (or **tap** to cycle 1..N). See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Hold (long-press)** a cell: enter/clear a pencil mark.
- **Undo**: revert the last entry.

## Gameplay Rules
- Each row/column is a permutation of 1..N.
- For an edge clue C, scanning inward, exactly C buildings are visible (each strictly taller than all before it).
- Solved when the full grid is a valid Latin square and all edge clues hold.
- Visibility counting, Latin-square checks, and a unique-solution generator are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `SkyscrapersStateMachine` in `state/` exposes `StateFlow<SkyscrapersState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ HeightEntered / Cleared → Playing (clue checks updated)
 ├─ PencilMarkToggled → Playing
 ├─ UndoRequested / HintRequested → Playing
 └─ GridValidAndCluesSatisfied → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved (per size) | yes |
| Best time (per size) | yes |
| No-hint solves | yes |
| Current / best streak | yes |

## HUD
- Top bar: title, timer, settings, help.
- Bottom: Undo, Pencil toggle, remaining-cells status.
- Solve message appears below the board; the completed grid stays visible.
