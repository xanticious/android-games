# Letter Drop — Design Document

## Overview
- Letter Drop is a single-player action-word game where letter tiles **fall** from the top
  of the screen and the player taps them in order to spell words before they pile up.
- Forming longer valid words clears more tiles and scores more; letting the stack reach the
  top ends the game.
- All accepted words are validated against the **Wordnik** word list; see
  `design/common/word-data-sources.md`.
- Victory/defeat presentation follows `design/common/victory-defeat.md`; status never
  overlays the play field.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Falling tiles are light `Aqua0`-derived cells with `Dark1` glyphs; tapped (queued) tiles
  highlight with an `Aqua3` outline and connect to the current-entry line.
- The danger zone near the top tints toward the Material 3 error role as the stack rises.
- No hex values outside `ui/theme/Color.kt`; tile motion is smooth, UI transitions instant.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Letter Drop              Score 320 [⚙ ?]│  ← Top bar
├─────────────────────────────────────┤
│   R        T                          │
│       A          E                    │  ← Falling letters
│   N        O          S               │
│ ───────────────────────────────────  │  ← Stack rises from the bottom
│  current entry:  R A T                 │
│  Back    Submit    Clear               │  ← Action row
└─────────────────────────────────────┘
```
- The play field is the anchor; the current entry and actions sit at the bottom.
- Portrait and landscape both keep tiles legible without scrolling.

## Settings
- **Drop speed** (Slow / Normal / Fast; default Normal): how quickly tiles fall and the
  stack grows.
- **Minimum word length** (2–4; default 3): shortest word that may be submitted.
- **Letter distribution** (Balanced / Vowel-rich; default Balanced): biases spawned letters.
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Letters fall continuously and settle into a rising stack.
- Tap falling/stacked tiles in order to build a word in the current entry, then **Submit**.
- A valid Wordnik word clears its tiles and scores points; longer words score more and
  relieve more pressure.
- **Backspace** removes the last queued tile from the entry; **Clear** empties the entry
  without submitting.
- The game ends when the stack reaches the top of the field.

## Controls
- Tap a tile to queue its letter; tap again (or Backspace) to unqueue the last.
- Submit validates and clears; Clear cancels the current selection.
- Settings (⚙) and How to Play (?) from the top bar.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best score | yes |
| Longest word formed | yes |
| Most tiles cleared in one word | yes |
| Total words formed (lifetime) | yes |
| Longest survival time | yes |
- Score scales with word length and tiles cleared; exact curve lives in the controller.

## State Machine
- A dedicated `LetterDropStateMachine` in `state/` exposes `StateFlow<LetterDropState>`.
```
Idle
 └─ GameStarted → Falling
Falling
 ├─ TileSpawned / TileSettled → Falling
 ├─ TileQueued / EntryBackspaced / EntryCleared → Falling
 ├─ WordSubmitted → Falling        (valid → clear & score; invalid → inline reject)
 └─ StackOverflowed → GameOver
GameOver
 └─ NewGame / Replay / Menu → Idle
```
- The spawn/fall loop drives `TileSpawned`/`TileSettled`; `StackOverflowed` fires when the
  stack reaches the top. The pure `LetterDropRules` controller handles spawning weights,
  word validation against Wordnik, tile clearing, and scoring with no Android imports; unit
  tests cover validation, clearing, and game-over detection.

## HUD
- Top bar: game name, running score, settings, how-to-play.
- Per `hud-elements.md` for shared styling.
