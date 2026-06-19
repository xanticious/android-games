# Logic Grid — Design Document

## Overview
- Logic Grid is a single-player deduction puzzle (the "Einstein"/zebra style). The player is given several categories, each with the same number of unique items, and a list of clues. Exactly one consistent assignment links every item across categories.
- The player records deductions on a grid of cross-reference matrices, marking each pair as **yes (●)**, **no (✕)**, or unknown, until a single solution emerges.
- Every puzzle has a unique, clue-derivable solution requiring no guessing.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Size**: 3 categories × 4 items (Easy, default), 4×4 (Medium), 5×5 (Hard).
- **Auto-eliminate**: on (default) / off — marking a "yes" auto-marks the implied "no"s in that row/column block.
- **Theme/flavor**: rotating themed item sets (pets, houses, hobbies) for variety; purely cosmetic.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Cross-reference matrices on `Dark1`; category labels along the top and left.
- Cell states: empty, `Aqua2` ● for yes, `Aqua4` ✕ for no — distinguished by glyph as well as color.
- The clue list sits beside/below the grid; tapped clues can be struck through to track usage.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Logic Grid     00:00           ⚙ ? │
├─────────────────────────────────────┤
│        | red | blue| green|         │
│   Ann  |  ✕  |  ●  |  ✕  |          │  ← cross-reference matrices
│   Bob  |  ●  |  ✕  |  ✕  |          │
│   Cal  |  ✕  |  ✕  |  ●  |          │
│   ── Clues ──                       │
│   1. Ann is not in the red house.   │
│   2. Bob likes the blue cat. …      │
├─────────────────────────────────────┤
│  Undo  Hint     Cells left: 12      │
└─────────────────────────────────────┘
```

## How to Play
- Read the clues and mark each item-pair as yes or no in the matrices.
- A "yes" in a row/column forces "no" everywhere else in that block (handled automatically if auto-eliminate is on).
- Solve when every category is fully and consistently linked.

## Controls
- **Tap** a matrix cell: cycle empty → ✕ → ● → empty. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Tap** a clue: toggle a strikethrough to track which clues you've applied.
- **Undo / Hint**: standard; Hint reveals one forced mark with the clue that justifies it.

## Gameplay Rules
- The solution is the unique full assignment consistent with all clues.
- A mark conflicting with a forced deduction is flagged (optional) using the `error` token.
- Solved when the player's "yes" marks exactly match the unique solution and no contradictions remain.

## Puzzle Generation (controller)
- Pick a random full assignment, generate a clue set, then minimize clues while a constraint-propagation solver confirms uniqueness without guessing. Pure functions in `controller/`, unit-tested for unique solvability and no-guess solvability.

## State Machine
A dedicated `LogicGridStateMachine` in `state/` exposes `StateFlow<LogicGridState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ CellMarked → Playing (auto-eliminations applied)
 ├─ UndoRequested → Playing
 ├─ HintRequested → Playing
 └─ GridConsistentAndComplete → Solved
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
- Bottom row: Undo, Hint, remaining-cells status.
- Solve message appears below the grid; the completed matrices stay visible.
