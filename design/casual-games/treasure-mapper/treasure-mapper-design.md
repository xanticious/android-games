# Treasure Mapper — Design Document

## Overview
- Treasure Mapper is a single-player puzzle game about reading procedurally generated treasure map instructions and identifying the correct cell in a 2D top-down world grid.
- Each round presents a freshly generated world with identifiable landmarks (trees, rocks, water, clearings) and a treasure map clue written in plain directional prose (e.g., "Starting at the tall pine, walk 7 paces east, 4 paces north. Dig here.").
- The player reads the clue, studies the world, selects the cell they believe is correct, and taps **Dig Here**.
- The player has three attempts per round. On each wrong dig the cell is marked (wrong), a try is consumed, and the player may try again.
- After three failed attempts, the correct cell is revealed. Then a new world and new clue are generated.
- There is no timer and no persistent fail state — every game is a fresh puzzle.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- 2D top-down world grid rendered with a flat, clean tile aesthetic.
- Landmark tiles:
  - **Big Tree**: dark green canopy circle on a lighter green base.
  - **Small Tree**: smaller version of the above.
  - **Rock**: grey rounded boulder icon.
  - **Water**: animated blue shimmer tile.
  - **Clearing**: light sand/grass open cell.
  - **Fence Post**: small wooden post icon (used as reference markers).
- Grid cells are lightly outlined; the selected cell highlights with a `Teal3` border.
- Correct dig: cell reveals a treasure chest icon with a golden sparkle burst.
- Wrong dig: cell shows an `X` icon with a brief `Coral` pulse; the marker remains for the rest of the round.
- Revealed treasure (after 3 failures): the correct cell pulses with a golden glow and shows the chest.
- Clue text is displayed in a parchment-toned strip below the world grid.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Treasure Mapper   Tries: ❤❤❤   ⚙  │  ← Top bar (remaining tries, settings)
├─────────────────────────────────────┤
│                                     │
│  🌲 🪨 〰️ 〰️ 🌲 🌲 🪨            │
│  🌲 🌳 〰️ 🌿 🌿 🌲 🌿            │  ← World grid (scrollable/zoomable)
│  🌿 🌿 🪨 🌿 🌳 🌿 🌿            │
│  🌿 🌿 🌿 🌿 🌿 🪨 🌿            │
│  🌿 🪨 🌿 🌿 🌿 🌿 🌿            │
│                                     │
├─────────────────────────────────────┤
│  "Start at the big rock, walk       │
│   8 paces east, 3 paces south."     │  ← Clue strip (parchment style)
├─────────────────────────────────────┤
│           [ Dig Here ]              │  ← Dig button (enabled when a cell is selected)
└─────────────────────────────────────┘
```
- After all 3 tries are used, the **Dig Here** button changes to **New Map** and the correct cell is revealed.
- On a correct find, the result (tries used, record) appears below the grid; a **New Map** button advances to the next round.

## Settings
- **Grid size**: Small (10×10), Medium (14×14, default), Large (18×18).
- **Clue complexity**: Simple (1 step: one landmark + one direction), Standard (2 steps, default), Tricky (3 steps with a turn).
- **Landmark density**: Sparse, Normal (default), Dense.
- **Show compass** (on/off, default on): a compass rose overlay on the map showing cardinal directions.
- **Show step guide** (on/off, default off): highlights the landmark named in the clue when the player taps it (accessibility aid).

## How to Play
- Read the clue in the strip below the world.
- Identify the named landmark on the grid.
- Follow the directional steps in the clue (counting grid cells as "paces").
- Tap the cell you believe the treasure is buried in. It highlights with a selection border.
- Tap **Dig Here** to submit your guess.
  - Correct: treasure chest revealed, result panel shows, **New Map** advances.
  - Wrong: that cell is marked with an X, you lose one try, try again.
- After 3 wrong tries, the correct cell is revealed and **New Map** starts a fresh round.

## Clue Generation

### Landmarks
- Each world has a set of named landmarks generated from the landmark tiles present on the grid. Landmarks that are unique on the grid are eligible as clue starting points (e.g., "the big oak" only if there is exactly one big-tree tile).
- If no unique landmark is present, the generator ensures at least one is placed.

### Clue Structure
- **Simple** (1 step): "Starting at [Landmark], walk [N] paces [Direction]."
- **Standard** (2 steps): "Starting at [Landmark], walk [N] paces [Direction1], then [M] paces [Direction2]."
- **Tricky** (3 steps): "Starting at [Landmark], walk [N] paces [Direction1], [M] paces [Direction2], then [P] paces [Direction3]."
- Directions are cardinal: north, south, east, west. Diagonal directions are not used.
- Paces are in whole grid cells (1 pace = 1 cell). The treasure always lands within the grid boundary.
- The generator verifies that the clue uniquely identifies exactly one cell on the current map (no accidental ambiguity where two different readings land on the same cell).

### World Generation
- Each new map generates a fresh world from a random seed using a simple noise-based tile placement algorithm.
- Landmark types and positions are deterministic from the seed. The clue and treasure position are computed from the seed after world generation.
- The player never sees the same world twice (unless they replay via the stats screen).

## State Machine
- A dedicated `TreasureMapperStateMachine` in `state/` exposes `StateFlow<TreasureMapperState>`.
```
Idle
 └─ StartGame → GeneratingMap
GeneratingMap
 └─ MapReady → WaitingForGuess
WaitingForGuess
 ├─ CellSelected → WaitingForGuess (selection updated)
 ├─ CellDeselected → WaitingForGuess (selection cleared)
 └─ DigSubmitted → Evaluating
Evaluating
 ├─ CorrectDig → RoundComplete (treasure revealed)
 ├─ WrongDig [tries remaining] → WaitingForGuess (cell marked, try consumed)
 └─ WrongDig [no tries remaining] → RoundFailed (correct cell revealed)
RoundComplete
 └─ NewMap → GeneratingMap
RoundFailed
 └─ NewMap → GeneratingMap
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Rounds solved on first try | yes |
| Rounds solved on second try | yes |
| Rounds solved on third try | yes |
| Rounds failed (treasure revealed) | yes |
| Current win streak (consecutive solves without reveal) | yes |
| Best win streak | yes |

## HUD
- Top bar: title, remaining tries (3 heart/shovel icons), settings.
- World grid: full-width scrollable/zoomable; selected cell highlighted.
- Compass rose (if enabled): overlaid on the top-right corner of the grid.
- Clue strip: parchment-styled text below the grid; tappable to expand for long clues.
- Dig button: prominent CTA below the clue strip; enabled only when a cell is selected; changes to **New Map** after round end.
- Result panel: appears below the grid after a correct dig or after three failures. Shows tries used, personal bests, and the **New Map** button. Never covers the world grid.
