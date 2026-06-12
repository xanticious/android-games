# Falling Blocks (Arcade) — Design Document

## Overview
A high-definition sprint variant of classic tetromino gameplay. The sole objective is to clear 40 lines as fast as possible. No survival mechanics, no increasing speed — just pure efficiency and speed optimization against the clock. Race your personal best time.

## Visual Style
- Same visual language as Falling Blocks (survival mode), with a sprint-focused UI.
- A prominent stopwatch timer is the central HUD element.
- Progress bar shows lines cleared out of 40.
- When the final line is cleared, a burst animation plays and the final time is displayed large.
- Ghost piece, block colors, line-clear effects are identical to Falling Blocks.
- "Sprint" color accent on the progress bar and timer (a distinct color from the survival game).

## Screen Layout
```
┌─────────────────────────────────┐
│  [Lines: 32/40] [⏱ 01:23.456] │  ← HUD (top)
├──────────────────────┬──────────┤
│                      │ [NEXT]   │
│   GAME BOARD         │ [piece]  │
│   (10×20 grid)       │         │
│                      │ [HOLD]   │
│                      │ [piece]  │
│                      │         │
└──────────────────────┴──────────┘
     [Controls overlay — bottom]
```

## Controls
Identical to Falling Blocks (survival):
- Swipe left/right: move piece horizontally.
- Tap right half: rotate clockwise.
- Tap left half: rotate counter-clockwise.
- Swipe down slowly: soft drop.
- Swipe down fast (flick): hard drop.
- Tap hold area: hold piece.

## Gameplay Loop

### Sprint Structure
1. Player taps "Start" to begin the timer (or auto-starts on first input).
2. Tetrominoes fall at a constant speed (fixed at level-5 equivalent — fast but not maximum).
3. Player clears lines as efficiently as possible.
4. Timer stops when the 40th line is cleared.
5. Final time is recorded as the player's personal best if it's faster than their previous record.

### Line Count
- Only lines cleared count — no score, no level-ups.
- Progress: `N / 40 lines` displayed prominently.

### Key Mechanic — Piece Queue Visibility
- The game shows the next 3 upcoming pieces (not just 1), enabling the player to plan further ahead.
- This is a deliberate sprint-mode feature to reward pre-planning skill.

### Records
- Best sprint time is stored per profile.
- The game shows the current personal best time alongside the live timer.
- If the live timer is ahead of the personal best pace, a subtle green indicator appears.

## State Machine
```
Idle
 └─ GameStart → WaitingForFirstInput
WaitingForFirstInput
 └─ AnyInput → Playing (timer starts)
Playing
 ├─ LineCleared → Playing (increment line count)
 └─ 40LinesCleared → SprintComplete
SprintComplete
 └─ (show final time + best comparison) → Idle
```
Note: no game over state — the board cannot top out. The piece fall speed is calibrated so this is essentially impossible within 40 lines.

## HUD
- Line progress: top-left (e.g., "32 / 40").
- Timer: top-right, large, stopwatch format `MM:SS.mmm`.
- Personal best time: below the live timer (dims when not competitive, highlights gold when surpassed).
- Next 3 pieces: right panel.
- Hold piece: right panel below next pieces.
