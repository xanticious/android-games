# Boggle — Design Document

## Overview
- Boggle is a single-player word-finding race on a square grid of random letters.
- The player taps **adjacent** letters to trace words; each tile may be used **once per
  word**. A countdown timer limits the round.
- The grid size is a setting: **3×3 Quick Boggle** or **4×4 Classic Boggle**.
- The round ends when the countdown reaches zero or the player taps **Give Up**. The
  results screen shows the player's score and **every possible word** in the grid.
- Letter-bank/tracing interaction, action buttons, countdown behavior, and the results
  layout come from `design/common/word-builder-controls.md`. Word data comes from
  `design/common/word-data-sources.md` (Wordnik list).
- Victory/results presentation follows `design/common/victory-defeat.md`; the summary
  never overlays the board.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Letter tiles are light `Aqua0`-derived cells with `Dark1` glyphs; the active trace path
  highlights tapped cells with an `Aqua3` outline and connects them with an `Aqua2` line.
- Adjacent, still-tappable cells subtly brighten while a trace is in progress.
- No hex values outside `ui/theme/Color.kt`; transitions are instantaneous.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Boggle                   01:40  [⚙ ?]│  ← Top bar with countdown
├─────────────────────────────────────┤
│  Words 7        Score 18              │  ← Progress strip
├─────────────────────────────────────┤
│        ┌───┬───┬───┬───┐             │
│        │ T │ A │ R │ E │             │
│        ├───┼───┼───┼───┤             │  ← 4×4 Classic (or 3×3 Quick)
│        │ S │ O │ N │ D │             │
│        ├───┼───┼───┼───┤             │
│        │ L │ I │ P │ G │             │
│        ├───┼───┼───┼───┤             │
│        │ C │ E │ A │ T │             │
│        └───┴───┴───┴───┘             │
├─────────────────────────────────────┤
│  current entry:  T A R                │
│  Back   Submit   Submit&Keep   GiveUp │  ← Action row
└─────────────────────────────────────┘
```
- The grid is the visual anchor and is centered; progress strip above, actions below.
- Portrait and landscape both size the grid to fit without scrolling or shrinking glyphs
  below a legible minimum.

## Settings
- **Game mode**: **3×3 Quick Boggle** or **4×4 Classic Boggle** (default 4×4).
- **Round duration** (e.g. 90 / 120 / 180 seconds; default depends on mode).
- **Minimum word length** (3 for 4×4, 3 for 3×3; configurable 3–4): shortest scoring word.
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- A grid of random letters appears and the countdown starts.
- Trace a word by tapping a starting cell, then tapping **adjacent** cells (including
  diagonals). A cell cannot be reused within the same word.
- Tap **Submit** to score a valid traced word, **Submit & Keep** to keep the trace and
  tweak it, **Backspace** to drop the last tapped cell, and **Give Up** to end early.
- When time expires the round ends and the full solution list is revealed.

## Controls
- Per `design/common/word-builder-controls.md`, with the Boggle-specific **adjacency
  rule**: each newly tapped cell must be orthogonally or diagonally adjacent to the
  previous cell and not already used in the current trace.

## Grid Generation
- A pure controller fills the grid with weighted random letters (Boggle-style dice
  distribution) and enumerates every valid word reachable by adjacency above the minimum
  length, using the Wordnik-derived word data.
- Generation guarantees a configured minimum number of solution words; otherwise it
  re-rolls so rounds are never barren.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best score per mode (3×3, 4×4) | yes |
| Most words in a round per mode | yes |
| Longest word found | yes |
| Best percentage of possible words found | yes |
| Total words found (lifetime) | yes |
- Longer words score more (standard Boggle length curve); exact values live in the
  controller.

## State Machine
- A dedicated `BoggleStateMachine` in `state/` exposes `StateFlow<BoggleState>`.
```
Idle
 └─ RoundStarted → Playing
Playing
 ├─ CellTapped → Playing          (rejects non-adjacent / reused cells)
 ├─ EntryBackspaced → Playing
 ├─ WordSubmitted → Playing        (valid → scored; invalid/duplicate → inline reject)
 ├─ TimeExpired → RoundOver
 └─ GaveUp → RoundOver
RoundOver
 └─ NewRound / Replay / Menu → Idle
```
- The pure `BoggleRules` controller handles grid generation, adjacency-path validation,
  full solution enumeration, duplicate detection, and scoring with no Android imports;
  unit tests cover adjacency rules, enumeration, and scoring.

## HUD
- Top bar: game name, countdown, settings, how-to-play.
- Progress strip: words-found count and running score; styled per `hud-elements.md`.

## Results Screen
- Shows the final score and **all possible words** grouped by length, with the player's
  found words highlighted, per `design/common/word-builder-controls.md`.
