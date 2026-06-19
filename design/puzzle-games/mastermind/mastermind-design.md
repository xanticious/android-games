# Mastermind — Design Document

## Overview
- Mastermind is a single-player code-breaking deduction game. The game hides a secret code of colored pegs; the player guesses the sequence and receives feedback after each guess.
- Feedback uses key pegs: **black** = right color in the right position; **white** = right color in the wrong position. The player narrows down the code within a limited number of guesses.
- The player wins by reproducing the exact code; they lose if they run out of guess rows.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Code length**: 4 (default), 5, 6 pegs.
- **Color count**: 6 (default), 7, 8 available colors.
- **Allow duplicate colors in code**: on (default) / off.
- **Allow blanks**: off (default) / on — an empty slot becomes a valid code element (harder).
- **Guess limit**: 10 (default), 12, 8.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Code pegs use up to 8 palette colors, each paired with a **distinct shape/glyph** so color-blind players can play by shape (never color alone).
- Guess rows stack vertically on `Dark1`; the current editable row is highlighted `Aqua2`.
- Key-peg feedback: small black/white markers beside each guessed row; the secret row stays hidden (face-down covers) until the game ends.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Mastermind     Guess 3 / 10    ⚙ ? │
├─────────────────────────────────────┤
│   [?][?][?][?]   ← secret (hidden)   │
│   ─────────────────────────────     │
│   [●][◆][▲][■]   ●● ○   (2 blk,1 wht)│
│   [◆][●][■][▲]   ● ○○                │  ← past guesses + key-peg feedback
│   [▲][■][●][◆]   ← current row       │
├─────────────────────────────────────┤
│   palette [●][◆][▲][■][★][⬡]  Submit │
│  Undo            Status              │
└─────────────────────────────────────┘
```

## How to Play
- Fill the current row by choosing a color for each slot, then submit your guess.
- Read the black/white key pegs to learn how close you are: black = exact, white = right color wrong spot.
- Crack the code before you run out of rows.

## Controls
- **Tap** a palette color then **tap** a slot (or drag a color onto a slot) to set it. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Tap** a filled slot: clear it (or cycle to next color).
- **Submit**: lock the current guess and reveal its feedback (enabled only when the row is complete).
- **Undo**: clear the in-progress current row (does not undo submitted guesses).

## Gameplay Rules
- Black count = positions where guess color equals secret color.
- White count = additional color matches accounting for multiplicity, excluding blacks (standard Mastermind scoring).
- Win when a guess earns all-black feedback equal to code length; lose when the guess limit is reached without a win — then the secret is revealed.
- Feedback scoring is a pure function in `controller/`, unit-tested including duplicate-color edge cases.

## State Machine
A dedicated `MastermindStateMachine` in `state/` exposes `StateFlow<MastermindState>`.
```
Idle
 └─ StartGame → Playing (secret generated)
Playing
 ├─ SlotSet / SlotCleared → Playing
 ├─ GuessSubmitted [not solved, rows remain] → Playing (feedback added)
 ├─ GuessSubmitted [all black] → Solved
 └─ GuessSubmitted [last row used] → Failed (secret revealed)
Solved / Failed
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Games won (per config) | yes |
| Fewest guesses to win (per config) | yes |
| Win rate | yes |
| Current / best win streak | yes |

## HUD
- Top bar: title, guess counter, settings, help.
- Bottom: color palette, Submit, Undo.
- Win/lose message appears below the board; on loss the secret row flips up and stays visible.
