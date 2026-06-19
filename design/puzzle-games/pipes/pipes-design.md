# Pipes — Design Document

## Overview
- Pipes (also known as Net) is a single-player rotation puzzle. The grid is filled with pipe-segment tiles (straights, bends, T-junctions, and endpoints) that begin randomly rotated.
- The player rotates tiles so that every pipe connects into one fully linked network with **no loose ends** and no leaks, all fed from a central source.
- Each puzzle has a unique fully connected configuration.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Size**: 5×5 (Easy), 8×8 (default), 11×11, 14×14.
- **Wrap edges**: off (default) / on — pipes may connect across opposite edges (harder).
- **Lock solved tiles**: on (default) / off — correctly oriented, connected tiles lock to reduce fiddling.
- **Show flow**: on (default) — connected pipes fill with color from the source.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Empty/unconnected pipes draw in `Dark2`; pipes connected to the source fill with `Aqua2` "water," visually flowing outward.
- The source tile is marked with an `Aqua4` core; endpoints are small caps that glow when fed.
- Rotation animates a quick 90° spin (board-area only); a fully solved network does a single gentle flow pulse.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Pipes        00:29   Rot 41    ⚙ ? │
├─────────────────────────────────────┤
│   ╋ ━ ┓ ┏ ━                          │
│   ┃ ◉ ┻ ┃ ┓     ◉ = source           │  ← grid of rotatable pipe tiles
│   ┗ ┳ ━ ┛ ┃                          │
│   ━ ┃ ┏ ━ ┛                          │
├─────────────────────────────────────┤
│  Undo            Connected 18/25     │
└─────────────────────────────────────┘
```

## How to Play
- Tap a tile to rotate it 90°.
- Connect every pipe into one network fed by the source, leaving no open ends.
- The board lights up with flow as connections complete.

## Controls
- **Tap** a tile: rotate 90° clockwise. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Hold (long-press)** a tile: rotate 90° counter-clockwise.
- **Undo**: revert the last rotation.

## Gameplay Rules
- Two adjacent tiles are connected when both have a pipe opening on their shared edge.
- The puzzle is solved when every pipe opening connects to a neighbor (no opening points off-grid or to a closed edge) and all tiles are reachable from the source as one network.
- Connectivity/flow computation, solved-check, and unique-solution generation are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `PipesStateMachine` in `state/` exposes `StateFlow<PipesState>`.
```
Idle
 └─ StartGame → Playing (tiles randomly rotated)
Playing
 ├─ TileRotated → Playing (flow recomputed, solved tiles may lock)
 ├─ UndoRequested → Playing
 └─ NetworkFullyConnected → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved (per size) | yes |
| Best time (per size) | yes |
| Fewest rotations (per size) | yes |
| Current / best streak | yes |

## HUD
- Top bar: title, timer, rotation count, settings, help.
- Bottom: Undo and a connected-tiles status.
- Solve message appears below the board; the fully flowing network stays visible.
