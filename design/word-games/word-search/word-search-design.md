# Word Search — Design Document

## Overview
- Word Search is a single-player letter-grid puzzle where hidden words are placed
  horizontally, vertically, diagonally, and backwards, and the player drags across letters
  to find each listed word.
- Found words are marked with **colored rounded pill / oval shapes** drawn over their
  letters.
- The app ships **40 categories** (e.g. States, Body Parts, Countries, School Subjects,
  Presidents); each puzzle uses a **random subset** of those categories' words.
- The grid **size is a setting**, but is **capped to a sensible maximum for the screen** so
  letters never need scrolling or shrink below a legible size.
- The puzzle is solved when all listed words are found. Victory presentation follows
  `design/common/victory-defeat.md`; the grid stays visible and the summary appears below.

## Word Placement & Non-Overlapping First Letters
- A key placement rule: **the first-letter cell of each word may not be reused as the first
  letter of any other word.** Each word has a unique starting cell.
- This lets the player **ignore already-found (filled/colored) letters while scanning**,
  improving contrast and reducing visual noise, because no remaining word starts inside a
  found word's pill.
- Words may otherwise cross/share interior cells as usual.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- The grid is a rounded `Dark1` card of light letter cells with `Dark1` glyphs.
- Found words are overlaid with **rounded pill/oval** shapes in distinct `Aqua`-family
  colors (cycled per word) at reduced opacity so the letters remain readable; the word list
  entry is struck through / tinted to match its pill.
- No hex values outside `ui/theme/Color.kt`; transitions are instantaneous.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Word Search     Found 4/8      [⚙ ?]  │  ← Top bar
├─────────────────────────────────────┤
│   ┌──────── N×N grid ───────────┐     │
│   │ letters with colored pills   │     │  ← Grid anchor (drag to select)
│   └──────────────────────────────┘    │
├─────────────────────────────────────┤
│  TEXAS  LIVER  FRANCE  HISTORY …       │  ← Word list (found entries tinted)
└─────────────────────────────────────┘
```
- The grid is the anchor and is sized to fit the screen at the chosen size without scrolling
  or sub-legible letters.

## Settings
- **Grid size** (e.g. Small / Medium / Large), each mapping to an N×N grid, **capped** so
  the largest selectable size still fits the current screen with legible letters (no
  scrolling, no tiny scaled-down glyphs). Sizes that would not fit are disabled.
- **Category mix** (Random / pick categories; default Random subset of the 40).
- **Allow diagonals / backwards** (on/off, default on): direction difficulty.
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- A grid of letters and a list of hidden words appear.
- Drag from a word's first letter across to its last letter (in any of the eight
  directions) to select it; a correct selection locks in a colored pill and checks the word
  off the list.
- Because each word starts on a unique cell, you can scan past already-found pills.
- Find every listed word to solve the puzzle.

## Controls
- Press and drag across a straight line of cells to select a candidate word; release to
  submit. Incorrect selections clear without penalty.
- Settings (⚙) and How to Play (?) from the top bar.

## Category Content & Generation
- 40 themed categories are bundled (e.g. States, Body Parts, Countries, School Subjects,
  Presidents, …). Each is a curated list of category words.
- A pure controller picks a random subset of categories, selects words that fit the grid,
  and places them with the unique-first-letter rule and the chosen directions, filling
  remaining cells with random letters.
- Generation guarantees all chosen words fit and the first-letter-uniqueness rule holds;
  otherwise it re-rolls. Grid size is clamped to the screen cap before placement.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles solved | yes |
| Best solve time per size | yes |
| Words found (lifetime) | yes |
| Current and best solve streak | yes |
| Categories played | yes |
- Optional timer for best-time tracking; the puzzle itself cannot be lost.

## State Machine
- A dedicated `WordSearchStateMachine` in `state/` exposes `StateFlow<WordSearchState>`.
```
Idle
 └─ PuzzleStarted → Searching
Searching
 ├─ SelectionMade → Searching      (match → mark pill; miss → clear)
 └─ AllWordsFound → Solved
Solved
 └─ NewPuzzle / Replay / Menu → Idle
```
- The pure `WordSearchRules` controller handles category selection, placement with the
  unique-first-letter rule, size clamping inputs, selection-to-word matching, and completion
  detection with no Android imports; unit tests cover placement validity, the first-letter
  rule, and selection matching.

## HUD
- Top bar: game name, found/total count, optional timer, settings, how-to-play.
- Per `hud-elements.md` for shared styling.
