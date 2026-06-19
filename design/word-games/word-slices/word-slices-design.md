# Word Slices — Design Document

## Overview
- Word Slices is a cake-themed reskin and refinement of classic Hangman (this game
  replaces the old `hangman` entry).
- A hidden word is shown as blanks. The player guesses letters one at a time; each **wrong**
  guess removes one slice from a **12-slice cake**.
- The challenge is to reveal the whole word before the last slice of cake disappears.
- **Keep going until every letter is found:** the player may keep guessing even after all
  12 slices are gone, but once the cake is gone the round is already a loss — the extra
  guesses just complete the reveal so the player can see the finished word.
- Words come from a **large, complex dictionary** (Wordnik) so the game stays challenging;
  see `design/common/word-data-sources.md`.
- On the victory screen the player sees the word's **definition** (from the bundled
  Wiktionary data).
- Victory/defeat presentation follows `design/common/victory-defeat.md`; status never
  overlays the play area.

## Theme & Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`; cake elements
  use warm Material 3 tertiary/secondary roles built from those tokens (no raw hex).
- The cake is drawn as a ring/round of **12 equal slices**. Remaining slices are full and
  appetizing; lost slices fade/empty out one at a time with a short bite animation.
- Revealed letters drop into their blanks; the alphabet keys for used letters dim.
- Transitions are instantaneous; full-screen celebration is reserved for victory and never
  covers the word or cake.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Word Slices                    [⚙ ?] │  ← Top bar
├─────────────────────────────────────┤
│            ( cake: 9 / 12 )           │  ← 12-slice cake, slices vanish on misses
├─────────────────────────────────────┤
│        _ R _ A _ G _ E                 │  ← Word blanks (revealed letters fill in)
│        Misses: J  Q  Z                 │  ← Wrong letters guessed
├─────────────────────────────────────┤
│  Q W E R T Y U I O P                   │
│   A S D F G H J K L                    │  ← On-screen letter keyboard
│    Z X C V B N M                       │
└─────────────────────────────────────┘
```
- Portrait stacks cake, word blanks, then keyboard. The cake and word are the anchors and
  are never covered by status text.
- Tablet layouts enlarge the cake and may place the keyboard beside the word.

## Settings
- **Difficulty** (Easy / Medium / Hard; default Medium): biases word selection toward
  shorter/common vs longer/complex Wordnik words.
- **Reveal definition on win** (on/off, default on): show the Wiktionary definition on the
  victory screen.
- **Letter case** (UPPER / lower display; default UPPER).
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- A hidden word appears as blanks; the cake starts with all **12 slices**.
- Tap a letter. If it is in the word, every matching blank fills in. If it is not, one
  slice of cake disappears.
- Reveal the whole word before the last slice is gone to win.
- If the cake runs out, you have lost the round; remaining guesses simply finish revealing
  the word so you can see the answer.

## Controls
- Tap on-screen alphabet keys to guess. Used keys are disabled.
- Settings (⚙) and How to Play (?) are reachable from the top bar.

## Word Selection
- A pure controller chooses a word from the Wordnik-derived list, filtered by difficulty
  (length / commonness), excluding offensive words per the shared filter.
- The Wiktionary definition for the chosen word is looked up from the bundled data; if no
  definition is available, the victory screen shows a graceful fallback.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Words guessed correctly | yes |
| Current and best win streak | yes |
| Best (fewest) misses on a win | yes |
| Perfect (zero-miss) wins | yes |
| Words lost | yes |
- No timer and no points; success is measured by streaks and miss counts.

## State Machine
- A dedicated `WordSlicesStateMachine` in `state/` exposes `StateFlow<WordSlicesState>`.
```
Idle
 └─ RoundStarted → Guessing
Guessing
 ├─ CorrectLetter → Guessing       (fills blanks)
 ├─ WrongLetter → Guessing         (removes a slice; if slices == 0 → mark lost)
 ├─ WordFullyRevealed → Won        (only if reached before/with last slice)
 └─ CakeGone → Lost                (player may continue revealing for the answer)
Won
 └─ NewWord / Menu → Idle
Lost
 └─ NewWord / Menu → Idle
```
- The pure `WordSlicesRules` controller handles word selection, letter matching, slice
  accounting, win/lose determination, and definition lookup with no Android imports; unit
  tests cover slice loss, the 12-slice loss boundary, and win detection.

## Victory / Defeat
- Per `design/common/victory-defeat.md`. On a win the cake and completed word stay visible
  and the **Wiktionary definition** appears in the summary below.
- On a loss the fully revealed word and the empty cake plate stay visible with a concise
  "Out of cake" message below; no overlay covers the board.
