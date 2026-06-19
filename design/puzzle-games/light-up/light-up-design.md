# Light Up — Design Document

## Overview
- Light Up (also known as Akari) is a single-player logic puzzle on a rectangular grid of white and black cells.
- The player places **light bulbs** on white cells. A bulb illuminates its own cell and projects light horizontally and vertically until a black cell blocks it.
- The puzzle is solved when **every white cell is lit** and **no bulb shines on another bulb**. Numbered black cells additionally constrain exactly how many bulbs touch their four orthogonal neighbors.
- Every generated puzzle has a unique solution.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Controls (intuitive, signature)
- **Tap** a white cell: place a light bulb (tap again to remove it). See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Hold (long-press)** a white cell: place a **mark** — a small dot the player uses to note "no bulb belongs here." Hold again to clear the mark. Marks are player annotations only; they never emit light and never affect win checks.
- The cell interaction cycles are predictable:
  - empty → **tap** → bulb → **tap** → empty
  - empty → **hold** → mark → **hold** → empty
- Tapping a marked cell is a convenient shortcut: it clears the mark and places a bulb in one step.
- Black cells (clue and plain) are not interactive.

## Settings
- **Board size**: 7×7 (default), 10×10, 14×14.
- **Difficulty**: Easy, Medium (default), Hard — affects clue density and required deduction depth.
- **Auto-light preview**: on (default) / off — show projected light as bulbs are placed.
- **Highlight conflicts**: on (default) / off — flag bulbs that see each other and clue cells whose count is exceeded.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- White cells: light `Aqua0` surface; black cells: `Dark2`; numbered black cells show their digit in `Aqua0`.
- Bulb: a clear bulb glyph; lit cells gain a soft warm-against-cool glow drawn from `Aqua1`.
- Mark: a small unobtrusive `Aqua3` dot, clearly different from a bulb.
- Conflicts: a bulb seeing another bulb is outlined with the Material 3 `error` token; an over-satisfied clue turns its number to the `error` token. No full-screen flashing.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Light Up      00:51            ⚙ ? │  ← Top bar (timer, settings, help)
├─────────────────────────────────────┤
│   . . ■ . . 2 .                     │
│   . 💡. . ■ . .                      │  ← grid: white cells, black cells (■),
│   ■ . . . . . 1                     │     numbered black clue cells, bulbs (💡)
│   . . 0 . ■ . .                     │
├─────────────────────────────────────┤
│  Undo  Hint     Unlit cells: 6      │  ← Bottom action + status row
└─────────────────────────────────────┘
```

## How to Play
- **Tap** an empty white cell to drop a bulb; it lights its row and column until a black cell blocks it.
- **Hold** a cell to mark it as "no bulb" while you reason — marks are just notes.
- A numbered black cell must touch exactly that many bulbs on its four orthogonal sides.
- Solve it when every white cell is lit and no two bulbs can see each other.

## Gameplay Rules
- A bulb lights its own cell and extends a beam up, down, left, and right until blocked by a black cell or the grid edge.
- **No two bulbs may illuminate each other** (i.e. two bulbs cannot share an unobstructed row/column segment).
- A numbered black cell requires exactly that many bulbs among its (up to four) orthogonal neighbors. Unnumbered black cells have no constraint.
- The puzzle is solved when all white cells are lit, no bulb-sees-bulb conflict exists, and all clue counts are satisfied. Marks are ignored for solving.

## Puzzle Generation (controller)
- Generate a valid bulb placement, derive clue numbers, then thin clues while a solver confirms the solution stays unique.
- Generation, light-propagation, and solve-checking are pure functions in `controller/`, unit-tested for: correct illumination spans, bulb-sees-bulb detection, clue-count satisfaction, and unique solvability.

## State Machine
A dedicated `LightUpStateMachine` in `state/` exposes `StateFlow<LightUpState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ BulbPlaced / BulbRemoved → Playing (lighting recomputed)
 ├─ MarkToggled → Playing (annotation only)
 ├─ UndoRequested → Playing
 ├─ HintRequested → Playing (one forced bulb/mark revealed)
 └─ BoardSolved → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved (per size / difficulty) | yes |
| Best time (per size / difficulty) | yes |
| No-hint solves | yes |
| Current / best streak | yes |

## HUD
- Top bar: title, timer, settings, help.
- Bottom row: Undo, Hint, and a live "unlit cells" / conflict count.
- Solve message appears below the board; the fully lit grid stays visible.
