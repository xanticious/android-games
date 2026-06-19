# Typing Sprint — Design Document

## Overview
- Typing Sprint is a typing speed-and-accuracy game where letters or words stream/fall down
  the screen and the player must enter them before they expire.
- The player chooses what falls — individual **Letters** or whole **Words** — and how they
  input: an **on-screen keyboard** or a **physical-keyboard input field** (for tablets with
  a hardware keyboard).
- Words are drawn from the **Wordnik** word list; see `design/common/word-data-sources.md`.
- The game tracks words-per-minute and accuracy; it ends when too many items are missed or
  the field overflows. Victory/defeat presentation follows
  `design/common/victory-defeat.md`.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Falling items are light cells with `Dark1` glyphs; the next/locked target highlights with
  an `Aqua3` outline. Correctly cleared items pop in `Aqua4`; missed items flash the
  Material 3 error role.
- No hex values outside `ui/theme/Color.kt`; item motion is smooth, UI transitions instant.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Typing Sprint   WPM 48  Acc 96% [⚙ ?]│  ← Top bar / stats strip
├─────────────────────────────────────┤
│     river        cloud                │
│              maple                     │  ← Falling letters or words
│   stone               quartz          │
│ ───────────────────────────────────  │  ← Miss line
├─────────────────────────────────────┤
│  input:  qua▌                          │  ← Physical-keyboard field …
│  Q W E R T Y U I O P                   │
│   A S D F G H J K L                    │  ← … or on-screen keyboard
│    Z X C V B N M  ⌫                    │
└─────────────────────────────────────┘
```
- The input mode setting decides whether the on-screen keyboard or the text field is shown.
- The play field is the anchor; stats strip above, input below. Legible at all sizes.

## Settings
- **Falling items**: **Letters** or **Words** (default Words). Letters mode streams single
  characters; Words mode streams Wordnik words.
- **Input mode**: **On-screen keyboard** or **Input field** (type with a physical keyboard).
  Default On-screen.
- **Difficulty / fall speed** (Slow / Normal / Fast; default Normal).
- **Word length range** (Words mode only; e.g. Short / Mixed / Long; default Mixed).
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Items fall from the top toward the miss line.
- Type the item to clear it: in Letters mode press the matching key; in Words mode type the
  whole word (then the locked word clears when complete).
- With **Input field** mode, type into the field using a physical keyboard; with **On-screen
  keyboard** mode, tap the rendered keys.
- Clearing items keeps the field clear and builds your WPM; missed items count against you.
- The game ends after too many misses or a field overflow.

## Controls
- **On-screen keyboard**: tap letter keys; ⌫ corrects the current word in Words mode.
- **Input field**: a focused text field captures the physical keyboard; Enter/space submits
  a word in Words mode, each keystroke matches in Letters mode.
- Settings (⚙) and How to Play (?) from the top bar.

## Word Source
- A pure controller draws words from the Wordnik-derived list filtered by the word-length
  setting, excluding offensive words via the shared filter.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best WPM (per mode) | yes |
| Best accuracy | yes |
| Longest streak without a miss | yes |
| Items cleared (lifetime) | yes |
| Best survival time | yes |
- WPM and accuracy are computed live; bests are stored per Letters/Words mode.

## State Machine
- A dedicated `TypingSprintStateMachine` in `state/` exposes `StateFlow<TypingSprintState>`.
```
Idle
 └─ SprintStarted → Running
Running
 ├─ ItemSpawned → Running
 ├─ KeyEntered / WordTyped → Running   (match → clear & score; mismatch → tracked)
 ├─ ItemMissed → Running               (counts toward fail threshold)
 └─ FieldOverflowed / TooManyMisses → GameOver
GameOver
 └─ NewSprint / Replay / Menu → Idle
```
- The pure `TypingSprintRules` controller handles spawning, input matching for both modes,
  WPM/accuracy computation, miss accounting, and game-over thresholds with no Android
  imports; unit tests cover letter vs word matching, WPM/accuracy, and game-over.

## HUD
- Top bar / stats strip: live WPM, accuracy, settings, how-to-play.
- Per `hud-elements.md` for shared styling.
