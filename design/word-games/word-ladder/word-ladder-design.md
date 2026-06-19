# Word Ladder — Design Document

## Overview
- Word Ladder is a single-player puzzle where the player transforms a **start word** into a
  **target word** by changing **one letter at a time**, where every intermediate step is a
  valid word of the same length.
- **Any valid solution is accepted** — the player does not have to find the shortest path to
  win. On the **victory screen the shortest possible path is revealed** so the player can
  compare.
- All steps are validated against the **Wordnik** word list; see
  `design/common/word-data-sources.md`.
- The puzzle is solved when the player reaches the target word. Victory presentation follows
  `design/common/victory-defeat.md`; the ladder stays visible and the summary appears below.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- The ladder is a vertical list of word rows from start to current; the changed letter in
  each new rung is highlighted in `Aqua3`. The target word is pinned and styled in `Aqua4`.
- No hex values outside `ui/theme/Color.kt`; transitions are instantaneous.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Word Ladder     Steps 4        [⚙ ?]  │  ← Top bar
├─────────────────────────────────────┤
│  Target:  WARM                         │  ← Pinned target
├─────────────────────────────────────┤
│  COLD                                  │
│  CORD                                  │  ← Ladder built so far
│  WORD                                  │
│  WARD                                  │
│  W A R _                               │  ← Current entry (one-letter change)
├─────────────────────────────────────┤
│  input:  war▌      Submit   Undo       │  ← Field or on-screen keyboard
└─────────────────────────────────────┘
```
- The ladder is the anchor and grows downward; target stays pinned at the top.

## Settings
- **Word length** (3 / 4 / 5; default 4): length of all words in the ladder.
- **Difficulty** (Easy / Medium / Hard; default Medium): biases start/target pairs by the
  length of their shortest path (longer shortest paths are harder).
- **Input mode**: Input field (physical keyboard) or On-screen keyboard (default On-screen).
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- You are given a start word and a target word of the same length.
- Each move, change exactly **one letter** of the current word to make another valid Wordnik
  word, then **Submit**. Letters keep their positions; only one changes per step.
- Continue until you reach the target word — **any** valid sequence wins.
- **Undo** removes your last rung. There is no required step count to win.

## Controls
- Type the next rung into the field or tap the on-screen keyboard; Submit (or Enter)
  commits a valid one-letter change.
- Invalid words or multi-letter changes are rejected inline.
- **Undo** steps back one rung; Settings (⚙) and How to Play (?) from the top bar.

## Puzzle Generation & Solvability
- A pure controller chooses a start/target pair that is **guaranteed solvable**: it builds
  the graph of same-length words (edges = one-letter difference) from the Wordnik data and
  picks pairs that are connected, using shortest-path length to set difficulty. See
  `design/common/level-solvability.md` for the generate→verify guarantee.
- The controller also computes the **shortest path** (e.g. BFS over the word graph) so it
  can be shown on the victory screen.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved | yes |
| Optimal solves (matched the shortest path) | yes |
| Best (fewest) steps over par | yes |
| Current and best solve streak | yes |
- Success is measured by solving and by how close the player got to the shortest path.

## State Machine
- A dedicated `WordLadderStateMachine` in `state/` exposes `StateFlow<WordLadderState>`.
```
Idle
 └─ PuzzleStarted → Climbing
Climbing
 ├─ RungSubmitted → Climbing       (valid one-letter change → extend; else reject)
 ├─ RungUndone → Climbing
 └─ TargetReached → Solved
Solved
 └─ NewPuzzle / Replay / Menu → Idle
```
- The pure `WordLadderRules` controller handles one-letter-change validation, Wordnik
  membership, solvable pair generation, and shortest-path computation with no Android
  imports; unit tests cover the one-letter rule, solvability, and shortest-path length.

## Victory Screen
- Per `design/common/victory-defeat.md`: the player's ladder stays visible and the summary
  below shows step count and the **shortest possible path** for comparison.
