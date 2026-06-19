# Minesweeper — Design Document

## Overview
- Minesweeper is a single-player deduction game on a grid hiding a fixed number of mines. The player reveals cells; a revealed non-mine cell shows how many of its eight neighbors are mines (blank if zero).
- Revealing a mine ends the game. The player wins by revealing every safe cell (flagging mines is optional).
- The **first tap is always safe** — the board is generated/adjusted so the first revealed cell (and ideally its neighborhood) contains no mine.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Difficulty**: Beginner (9×9, 10 mines), Intermediate (16×16, 40 mines, default), Expert (30×16, 99 mines), Custom (size + mine count).
- **Safe first tap**: on (default) / off.
- **Flag mode toggle vs long-press**: long-press to flag (default) / a persistent dig/flag toggle button.
- **Auto-chord**: on (default) / off — tapping a satisfied number reveals its remaining neighbors.
- **Question marks**: off (default) / on — long-press cycles flag → ? → empty.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Covered cells: raised `Dark2`; revealed cells: recessed `Dark0`.
- Neighbor numbers 1–8 use a fixed, legible color ramp drawn from the palette (and are always digits, never color-only).
- Flag: an `Aqua2` flag glyph; mine: a clear mine glyph; the triggering mine on loss is emphasized with the `error` token.
- Reveal cascade (for zero-cells) animates outward briefly (board-area only).

## Screen Layout
```
┌─────────────────────────────────────┐
│  Minesweeper  Mines 32  00:48   ⚙ ? │  ← mines-remaining + timer
├─────────────────────────────────────┤
│   1 1 1 . . 1 ⚑ 1                    │
│   1 ⚑ 1 . . 1 2 2                    │  ← grid: numbers, blanks, flags
│   1 1 1 . . . 1 ⚑                    │
│   . . . 1 1 2 2 2                    │
├─────────────────────────────────────┤
│  Dig/Flag toggle      Status         │
└─────────────────────────────────────┘
```

## How to Play
- Tap a covered cell to reveal it. Numbers tell you how many mines touch that cell.
- Long-press to flag a suspected mine.
- Reveal every safe cell to win; reveal a mine and the game is over.

## Controls
- **Tap** a covered cell: reveal it. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Hold (long-press)** a covered cell: place/remove a flag (or cycle to ? if enabled).
- **Tap** a satisfied number (auto-chord on): reveal its un-flagged neighbors.
- Optional **Dig/Flag toggle** button for players who prefer single taps for both.

## Gameplay Rules
- Each non-mine cell's number equals the count of mines in its 8-neighborhood; zero-cells flood-reveal their neighbors recursively.
- Flags are advisory only; they don't have to be correct to win.
- Win = all non-mine cells revealed. Loss = a mine is revealed (all mines then shown).
- Board generation (with safe-first-tap), neighbor counting, and flood reveal are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `MinesweeperStateMachine` in `state/` exposes `StateFlow<MinesweeperState>`.
```
Idle
 └─ StartGame → Ready
Ready
 └─ FirstReveal → Playing (mines placed avoiding first cell; timer starts)
Playing
 ├─ CellRevealed [safe] → Playing (cascade if zero)
 ├─ FlagToggled → Playing
 ├─ CellRevealed [mine] → Lost
 └─ AllSafeRevealed → Won
Won / Lost
 └─ NewGame → Ready
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Games won (per difficulty) | yes |
| Best time (per difficulty) | yes |
| Win rate | yes |
| Current / best win streak | yes |

## HUD
- Top bar: title, mines-remaining (mines minus flags), timer, settings, help.
- Bottom: optional dig/flag toggle and status text.
- Win/loss message appears below the board; on loss the full mine layout stays visible.
