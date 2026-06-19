# 2048 — Design Document

## Overview
- 2048 is a single-player sliding-tile merge puzzle. The player swipes the whole board in one of four directions; all tiles slide that way and equal-valued neighbors that collide merge into their sum.
- After every move that changes the board, one new tile (2 with 90% probability, 4 with 10%) spawns in a random empty cell.
- The classic goal is the 2048 tile, but this version **keeps going and grows the board** (see Growing Board below) so play continues well past 2048.
- The game is lost only when the board is full and no merges are possible in any direction.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Growing Board (signature mechanic)
- The board starts at **4×4** with a target of **2048**.
- When the player first creates the current target tile, the board **grows by one row and one column** and the target advances to the next power of two:
  | Board | Reach this tile to grow | Grows to |
  |-------|-------------------------|----------|
  | 4×4 | 2048 | 5×5 |
  | 5×5 | 4096 | 6×6 |
  | 6×6 | 8192 | 7×7 |
  | 7×7 | 16384 | 8×8 |
  | 8×8 | 32768 (and beyond) | — (max size) |
- 8×8 is the maximum size; once reached the board never grows again and play continues for high score.
- **Growth animation:** existing tiles keep their values and positions (anchored to the top-left origin); a new empty column appears on the right and a new empty row on the bottom, sliding in over ≤200 ms. This is a board-area transition, not a full-screen one, and never covers tiles.
- Growing the board is celebrated with a brief "Board expanded to N×N!" line in the status area below the board.

## Number Abbreviation on Tiles
- Tile values grow large once the board expands, so tile labels are abbreviated to stay legible:
  - < 1,000 → exact digits (`2`, `64`, `512`).
  - ≥ 1,000 → the leading significant digits (rounded down) plus a unit suffix, kept to at most ~4 glyphs: `1k`, `16k`, `131k`, `5m`, `268m`, `4b`, `564b`, `2t`.
  - Suffixes: `k` (thousand), `m` (million), `b` (billion), `t` (trillion).
- Abbreviation is display-only; the controller always tracks exact integer values for merges, scoring, and win checks.
- The abbreviation helper is a pure function in `controller/` (e.g. `abbreviate(value: Long): String`) and is unit-tested.
- Font size auto-shrinks one step if an abbreviated label still overflows its cell (longest case ~4 glyphs like `564b`).

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Board background `Dark1`, empty cells `Dark0`, rounded-corner tiles.
- Tile color ramps through the palette by magnitude: low values use cool `Aqua0`/`Aqua1`, mid values `Aqua2`/`Aqua3`, high values `Aqua4`; the current-target tile gets a subtle glow. Tile **value labels** are always high-contrast against their fill.
- Slide animation: tiles ease to their destination (≤150 ms); merged tiles do a small scale-pop on landing.
- New-tile spawn: quick fade+scale in.

## Screen Layout
```
┌─────────────────────────────────────┐
│  2048           Score 10320   ⚙ ?   │  ← Top bar (score, best, settings, help)
├─────────────────────────────────────┤
│   [ 2 ][ 4 ][   ][ 8 ]              │
│   [16 ][ 2 ][32 ][   ]              │  ← N×N board (4×4 → 8×8)
│   [   ][128][ 4 ][ 2 ]              │
│   [ 8 ][16 ][1k ][16k]              │
├─────────────────────────────────────┤
│  Undo            Target: 2048 (4×4) │  ← Bottom action + status row
└─────────────────────────────────────┘
```

## Settings
- **Starting board size**: 4×4 (default). (Higher start sizes optional for variety; growth rules continue from there.)
- **Undo**: on (default) / off — turning off enables a "purist" high-score stat.
- **Spawn-4 chance**: 10% (default) / 0% (twos only, easier).

## How to Play
- Swipe up, down, left, or right to slide every tile that direction.
- Two tiles with the same value that collide merge into one tile of double the value; each tile can merge at most once per move.
- A new tile appears after each move that changed the board.
- Reach the target tile to expand the board and raise the target; keep going to 8×8 and chase a high score.
- The game ends when the board is full and no move would merge or move any tile.

## Controls
- **Swipe** (up/down/left/right): slide the board. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Undo** button: revert the last move (and its spawned tile), when enabled.

## Gameplay Rules
### Move resolution (per swipe)
1. For each line in the swipe direction, compress tiles toward the leading edge.
2. Merge adjacent equal tiles once, from the leading edge inward, summing their values.
3. Compress again to close gaps created by merges.
4. If the board changed, spawn one new tile in a random empty cell.
### Growth check
- After resolution, if any tile equals the current target value and the board is below 8×8, grow the board and advance the target.
### Loss check
- After spawning, if no empty cell exists and no pair of equal tiles is orthogonally adjacent, the game is over.

## State Machine
A dedicated `Game2048StateMachine` in `state/` exposes `StateFlow<Game2048State>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ Swiped [board changed] → Resolving
 ├─ Swiped [no change] → Playing
 └─ UndoRequested → Playing (previous snapshot restored)
Resolving
 ├─ ReachedTarget [size < 8] → Growing
 ├─ NoMovesLeft → GameOver
 └─ Settled → Playing
Growing
 └─ GrowthComplete → Playing (target advanced)
GameOver
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best score (per starting size) | yes |
| Highest tile ever reached | yes |
| Largest board reached | yes |
| Games played / games won (reached 2048) | yes |
| Best score with Undo off | yes |

## HUD
- Top bar: title, current score, best score, settings (⚙), help (?).
- Bottom row: Undo button; status text shows current target and board size, then expansion and game-over messages.
- Per the common rule, game-over and win messaging appear **below** the board and never cover tiles.
