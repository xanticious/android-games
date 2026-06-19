# Anagrams (Arcade) — Design Document

## Overview
- Anagrams (Arcade) is the **timed** variant of Anagrams: identical six-letter bank and
  word-building, but a **countdown timer** drives the round.
- The player taps letters to build words from a bank of six letters and submits them; the
  goal is to find as many hidden words as possible before the timer expires.
- The round ends when the countdown reaches zero, when all targets are found, or when the
  player taps **Give Up**.
- All letter-bank interaction, action buttons, countdown behavior, and results come from
  `design/common/word-builder-controls.md`. Word data comes from
  `design/common/word-data-sources.md` (Wordnik list).
- This doc only describes what differs from regular `anagrams`: the timer and arcade
  scoring. See `anagrams/anagrams-design.md` for the shared base.
- Victory/results presentation follows `design/common/victory-defeat.md`; the summary
  never overlays the play area.

## Difference From Regular Anagrams
| Aspect | Anagrams | Anagrams (Arcade) |
|--------|----------|-------------------|
| Timer | None | Countdown shown in top bar |
| Round end | All found or Give Up | Timer expiry, all found, or Give Up |
| Scoring | Length-based | Length-based plus remaining-time/speed bonus |
| Settings | Min length, bias | Adds round duration |

## Screen Layout
```
┌─────────────────────────────────────┐
│ Anagrams (Arcade)        01:12  [⚙ ?]│  ← Top bar with countdown
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
- Identical to Anagrams except the top bar shows the live countdown, styled per
  `hud-elements.md`. The countdown turns to a warning accent in the final seconds.

## Settings
- Inherits Anagrams settings (minimum word length, letter-set bias, show found-count).
- **Round duration** (e.g. 60 / 90 / 120 seconds; default 90): length of the countdown.
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Six letters are dealt and the countdown starts immediately.
- Tap letters to spell words and **Submit** before the clock runs out.
- Targets are shown as blanks arranged alphabetically and grouped by length, exactly as in
  Anagrams.
- **Submit & Keep**, **Backspace**, and **Give Up** behave as in
  `design/common/word-builder-controls.md`.
- When the timer hits zero the round ends and unfound targets are revealed.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best single-round score | yes |
| Best completion percentage | yes |
| Longest word found | yes |
| Rounds fully cleared before time | yes |
| Total words found (lifetime) | yes |
- Score rewards longer words and adds a bonus for words found quickly / time remaining at
  clear. Exact curve lives in the controller.

## State Machine
- A dedicated `AnagramsArcadeStateMachine` in `state/` exposes
  `StateFlow<AnagramsArcadeState>`. It mirrors the Anagrams machine plus a timer:
```
Idle
 └─ RoundStarted → Playing
Playing
 ├─ LetterTapped / EntryBackspaced / WordSubmitted → Playing
 ├─ TimeExpired → RoundOver
 ├─ AllWordsFound → RoundOver
 └─ GaveUp → RoundOver
RoundOver
 └─ NewRound / Replay / Menu → Idle
```
- A ticking clock raises `TimeExpired` automatically at zero. The pure rules controller is
  shared with Anagrams (set generation, enumeration, validation) plus the speed-bonus
  scoring; unit tests cover timer-driven round end and bonus scoring with no Android
  imports.
