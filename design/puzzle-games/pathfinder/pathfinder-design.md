# Pathfinder — Design Document

## Overview
- Pathfinder is a single-player grid path puzzle. The board holds pairs of colored dots; the player draws non-crossing routes connecting each matching pair, and a perfect solution fills every cell. (It is the "flow" style sibling of Numberlink, distinguished by its dot endpoints and color-first presentation.)
- Routes are orthogonal, may not overlap, and ideally cover the whole grid.
- Each puzzle has a unique full-coverage solution.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Size**: 5×5 (Easy), 8×8 (default), 10×10, 13×13.
- **Require full coverage**: on (default) / off.
- **Auto-trim on cross**: on (default) — dragging through an existing route trims it from the crossing point.
- **Endpoint glyphs**: distinct shape per color (default on) so pairs are distinguishable without color.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Endpoint dots use distinct palette colors paired with shapes; routes are thick rounded ribbons in the pair's color.
- The route in progress glows `Aqua2`; a fully connected pair pulses once on completion.
- Coverage meter shows percentage of cells filled.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Pathfinder    Pairs 2/4        ⚙ ? │
├─────────────────────────────────────┤
│   ● ─ ─ ╮ ▲                          │
│   . . . │ .                          │  ← grid: colored dots + ribbon routes
│   ▲ . . ╰ ─ ●                        │
│   ■ . . . . ■                        │
├─────────────────────────────────────┤
│  Undo  Clear      Filled 22/25       │
└─────────────────────────────────────┘
```

## How to Play
- Drag from one dot to its matching dot to draw a route.
- Routes can't overlap; for a perfect solve, cover every cell.
- Connect all pairs to finish.

## Controls
- **Drag** from a dot (or an existing route) to lay/extend a route. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Tap** a dot: clear that route.
- **Drag across another route**: auto-trims it so the new one passes.
- **Undo / Clear**: revert last segment / clear all routes.

## Gameplay Rules
- A route connects exactly its two like-colored dots through whole cells.
- No two routes share a cell.
- Win = all pairs connected (and all cells used if full-coverage is on).
- Route validity, coverage, and a uniqueness solver are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `PathfinderStateMachine` in `state/` exposes `StateFlow<PathfinderState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ RouteExtended / RouteCleared → Playing
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
- Bottom: Undo, Clear, coverage status.
- Solve message appears below the board; the completed routes stay visible.
