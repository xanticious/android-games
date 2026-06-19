# Anagrams — Design Document

## Overview
- Anagrams is a single-player word-finding game played from a bank of **six letters**.
- The player taps letters to build words and submits them; every valid word hidden in
  the six-letter bank counts toward completing the round.
- This is the **untimed** variant: there is no countdown, so the player can hunt for
  every word at their own pace. The timed variant is `anagrams-arcade`.
- The round is complete when all target words are found, or the player taps **Give Up**.
- Shared letter-bank interaction, action buttons, and results behavior come from
  `design/common/word-builder-controls.md`. Word data comes from
  `design/common/word-data-sources.md` (Wordnik list).
- Victory/results presentation follows `design/common/victory-defeat.md`; the summary
  never overlays the play area.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Letter tiles are light surfaces with `Dark1` glyphs; tapped tiles lift and gain an
  `Aqua3` outline.
- Target blanks render as underscored slots grouped into cards by word length.
- Found words fill their blanks in `Aqua4`; no hex values outside `ui/theme/Color.kt`.
- Transitions are instantaneous; full-screen animation is reserved for the results
  celebration and never covers the board.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Anagrams                       [⚙ ?] │  ← Top bar (no timer)
├─────────────────────────────────────┤
│  Found 4 / 19            Score 240    │  ← Progress strip
├─────────────────────────────────────┤
│  3:  ___  ___  ___                    │
│  4:  ____ ____ [RATE] ____            │  ← Targets: blanks, grouped by length,
│  5:  _____ [TRACE] _____              │     alphabetical within a group
│  6:  ______                           │
├─────────────────────────────────────┤
│  current entry:  T R A                │
│        [ R  A  T  E  C  T ]           │  ← Six-letter bank
│  Back   Submit   Submit&Keep   GiveUp │  ← Action row
└─────────────────────────────────────┘
```
- Portrait stacks progress strip, target blanks, current entry, bank, and action row.
- Target blanks scroll if the solution set is large; bank and actions stay pinned.
- Tablet layouts widen the target area and may show length groups side by side.

## Settings
- **Minimum word length** (2, 3, or 4; default 3): shortest word that counts as a target.
- **Letter set bias** (Balanced / Vowel-heavy; default Balanced): nudges the generated
  six letters toward sets with more solutions for an easier round.
- **Show found-count target** (on/off, default on): shows "Found x / total".
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Six letters are dealt into the bank.
- Tap letters to spell a word in the current entry; tap **Submit** to score it.
- Every valid word that can be made from the six letters (respecting the minimum length)
  is a target, shown as a blank arranged alphabetically and grouped by length.
- Use **Submit & Keep** to keep your tapped letters so you can quickly form a related
  word; use **Backspace** to undo the last tap.
- There is no clock — find as many as you can. Tap **Give Up** to reveal the rest.

## Controls
- Per `design/common/word-builder-controls.md`: tap bank tiles to build, Backspace,
  Submit, Submit & Keep, Give Up.
- No adjacency rule: any unused bank tile may be tapped in any order.

## Round Generation
- A pure controller picks a six-letter set from the Wordnik-derived length-indexed data
  such that it yields a satisfying number of valid words above the minimum length.
- The full solution set (the targets) is computed up front so progress and the end-screen
  reveal are exact.
- Generation guarantees at least a configured minimum number of target words; otherwise it
  re-rolls.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Rounds completed (all words found) | yes |
| Best single-round score | yes |
| Longest word found | yes |
| Best completion percentage | yes |
| Total words found (lifetime) | yes |
- Scoring rewards longer words (longer word = more points); exact curve lives in the
  controller.

## State Machine
- A dedicated `AnagramsStateMachine` in `state/` exposes `StateFlow<AnagramsState>`.
```
Idle
 └─ RoundStarted → Playing
Playing
 ├─ LetterTapped → Playing
 ├─ EntryBackspaced → Playing
 ├─ WordSubmitted → Playing        (valid → scored; invalid → inline reject)
 ├─ AllWordsFound → RoundOver
 └─ GaveUp → RoundOver
RoundOver
 └─ NewRound / Replay / Menu → Idle
```
- No timer node: this variant cannot expire. The pure `AnagramsRules` controller handles
  set generation, solution enumeration, word validation, and scoring with no Android
  imports; unit tests cover generation, validation, and scoring.

## HUD
- Top bar: game name, settings (⚙), and how-to-play (?). No countdown.
- Progress strip: found-count target and running score.
- Per `hud-elements.md` for shared styling.
