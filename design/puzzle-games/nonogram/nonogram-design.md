# Nonogram — Design Document

## Overview
- Nonogram (picture logic / griddler) is a single-player puzzle. Numeric clues on each row and column describe runs of consecutive filled cells. The player deduces which cells are filled to reveal a hidden picture.
- A row clue like `3 1` means a run of 3 filled cells, then a gap, then a run of 1, in order, with at least one empty cell between runs.
- Every puzzle has a unique solution solvable by logic alone.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Size**: 5×5 (Easy), 10×10 (default), 15×15, 20×20.
- **Auto-cross completed lines**: on (default) / off — mark a line's empties with ✕ once its runs are satisfied.
- **Mistake checking**: Relaxed (default, free marking) / Strict (filling a wrong cell is flagged immediately).
- **Reveal picture on solve**: on (default).

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Filled cells `Aqua2`; cells marked empty show a small `Aqua4` ✕; unknown cells `Dark0`.
- Clue gutters on the top and left, with 5-cell separators (slightly heavier `Dark2` lines) for readability.
- Satisfied clue numbers dim to indicate completion.
- On solve, the filled cells render as the finished picture (board-area reveal only).

## Screen Layout
```
┌─────────────────────────────────────┐
│  Nonogram      00:37            ⚙ ? │
├─────────────────────────────────────┤
│         2 1 | 3 | 1 1 | 4 | 2       │  ← column clues
│   3   | ■ ■ ✕ ■ ✕                    │
│   1 1 | ■ ✕ ✕ ✕ ■                    │  ← row clues + grid
│   4   | ✕ ■ ■ ■ ■                    │
│   2   | ■ ■ ✕ ✕ ✕                    │
├─────────────────────────────────────┤
│  Undo  Fill/Cross toggle   Status   │
└─────────────────────────────────────┘
```

## How to Play
- Use the row and column clues to figure out which cells are filled.
- Fill cells you're sure about and cross out cells you've ruled out.
- Complete every run exactly as the clues describe to reveal the picture.

## Controls
- **Tap** a cell: fill it (primary). See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Hold (long-press)** a cell: mark it empty with ✕ (or use the Fill/Cross toggle).
- **Drag**: paint a straight line of fills or crosses in the current mode.
- **Undo**: revert the last mark.

## Gameplay Rules
- A line is satisfied when its filled runs match its clue sequence exactly (lengths and order, separated by ≥1 empty).
- The puzzle is solved when every row and column clue is satisfied; the player's ✕ marks are advisory and ignored for win detection.
- Strict mode flags a fill that contradicts the unique solution using the `error` token.
- Clue generation and the line-solver (for uniqueness + hints) are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `NonogramStateMachine` in `state/` exposes `StateFlow<NonogramState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ CellFilled / CellCrossed / CellCleared → Playing (line satisfaction updated)
 ├─ UndoRequested → Playing
 ├─ HintRequested → Playing (one deducible cell revealed)
 └─ AllLinesSatisfied → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved (per size) | yes |
| Best time (per size) | yes |
| No-hint / no-mistake solves | yes |
| Current / best streak | yes |

## HUD
- Top bar: title, timer, settings, help.
- Bottom: Undo, Fill/Cross mode toggle, status.
- Solve message appears below the board; the revealed picture stays visible.
