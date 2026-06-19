# Pentomino — Design Document

## Overview
- Pentomino is a single-player tiling puzzle. The player fits the twelve distinct pentominoes (five-cell shapes named F, I, L, N, P, T, U, V, W, X, Y, Z) into a rectangular board with no gaps and no overlaps.
- The standard board for all twelve pieces (60 cells) is 6×10 (default), with 5×12, 4×15, and 3×20 variants.
- Pieces may be rotated and (optionally) flipped to fit.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Board**: 6×10 (default), 5×12, 4×15, 3×20.
- **Allow reflections (flips)**: on (default) / off — off makes some boards unsolvable, so it is paired with compatible boards.
- **Snap-to-grid**: on (default) — pieces lock to cell alignment.
- **Mode**: Free tiling (default) / Daily preset challenge.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Each of the twelve pentominoes has its own palette color and is labeled with its letter (color-blind safe via letters).
- Board on `Dark1`; placed pieces render as joined rounded cells; the piece tray sits below as a scrollable strip.
- Picking up a piece lifts it with an `Aqua2` glow; a legal drop snaps with a soft settle, an illegal drop eases back to the tray.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Pentomino    Placed 7/12       ⚙ ? │
├─────────────────────────────────────┤
│   ┌──────────────────────────┐      │
│   │ F F P P L L L L T . . .   │      │  ← 6×10 board with placed pieces
│   │ F F F P P . T T T . . .   │      │
│   └──────────────────────────┘      │
├─────────────────────────────────────┤
│  Tray ◀ [N][U][V][W][X][Y][Z] ▶ ↻ ⇋ │  ← scrollable tray + rotate/flip
│  Undo            Status              │
└─────────────────────────────────────┘
```

## How to Play
- Drag pentominoes from the tray onto the board.
- Rotate or flip a piece to make it fit.
- Cover the entire board with all twelve pieces, no gaps or overlaps.

## Controls
- **Drag** a tray piece onto the board; **release** over a legal fit to place it. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Rotate (↻)** / **Flip (⇋)** buttons, or **tap** a held piece to rotate, **double-tap** to flip.
- **Tap** a placed piece: lift it back to the tray.
- **Undo**: revert the last placement.

## Gameplay Rules
- A placement is legal only if all five cells land on empty in-board cells.
- The puzzle is solved when all twelve pieces are placed and every board cell is covered exactly once.
- Piece geometry (rotations/reflections), legal-placement checks, and a solver (for hints/uniqueness of presets) are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `PentominoStateMachine` in `state/` exposes `StateFlow<PentominoState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ PiecePickedUp → Dragging
 ├─ PieceRemoved → Playing
 ├─ UndoRequested → Playing
 └─ BoardFullyTiled → Solved
Dragging
 ├─ DroppedLegally → Playing (piece placed)
 └─ DroppedIllegally → Playing (returns to tray)
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Boards solved (per board) | yes |
| Best time (per board) | yes |
| Distinct solutions found | yes |
| Current / best streak | yes |

## HUD
- Top bar: title, placed/total, settings, help.
- Bottom: scrollable piece tray, rotate/flip, Undo, status.
- Solve message appears below the board; the completed tiling stays visible.
