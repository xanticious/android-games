# Match Three (Arcade) — Design Document

## Overview
- Match Three (Arcade) is the timed variant of Match Three. All core mechanics (gem types, swap rules, cascade resolution, dead-board reshuffle) are identical to the base game.
- The key difference: after each completed valid swap (one that produces at least one match), a per-move countdown timer starts. The player must land their **next** valid matching swap before the timer reaches zero or the run ends.
- Invalid swaps (that produce no match) do not reset the timer — only a swap that actually clears gems resets it.
- The goal is to survive as long as possible. The run score is measured in total swaps made (each valid match counts as one swap).
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Inherits all visual conventions from Match Three (gem designs, animations, palette).
- Additional arcade elements:
  - A **move timer bar** displayed prominently below the top bar. It depletes from full to empty over the countdown period, changing from `Aqua3` → `Sand` → `Coral` as time runs low.
  - When the timer reaches the last 2 seconds, the bar pulses and the screen edges flash `Coral` subtly.
  - Game over: gems drain downward off the board with a quick cascade-out animation; the score panel slides in from the bottom.
  - High-score flash: if the current run beats the previous best, a brief golden flash plays.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Match Three Arcade   Swaps: 34  ⚙  │  ← Top bar (swap count, settings)
├─────────────────────────────────────┤
│  ████████████████░░░░░░  4.2s      │  ← Move timer bar (full width, depleting)
├─────────────────────────────────────┤
│                                     │
│  [◆][●][★][⬡][●][◆][▼]            │
│  [▼][★][◆][●][⬡][▼][★]            │  ← 7×7 gem board (default; configurable)
│  [⬡][▼][●][★][◆][●][⬡]            │
│  [●][◆][⬡][▼][★][⬡][●]            │
│  [★][●][▼][◆][●][★][◆]            │
│  [◆][⬡][★][●][▼][◆][●]            │
│  [▼][●][◆][⬡][★][▼][⬡]            │
│                                     │
└─────────────────────────────────────┘
```
- The result panel appears below the board on game over; it never overlays the board.
- Tablets use the same board sizes as the base Match Three game.

## Settings
- **Board size**: 6×6, 7×7 (default), 8×8, 9×9.
- **Gem count** (colors/types): 4, 5, 6 (default).
- **Move time limit**: 3 s, 5 s (default), 8 s, 10 s.
- **Cascade time bonus** (on/off, default on): each cascade level beyond the first adds 0.5 s to the current timer (rewards chain-building).
- **Drop speed**: Slow, Normal (default), Fast.
- **Show hint** (on/off, default off): hint is disabled by default in Arcade mode to maintain tension.

## How to Play
- Play exactly as in Match Three — swap adjacent gems to create matches of 3 or more.
- After your swap clears at least one match, the move timer starts counting down.
- Make your next valid matching swap before the timer runs out.
- Invalid swaps (no match) do not reset the timer — choose carefully.
- Cascades (chain reactions) resolve automatically and each cascade level adds time if the bonus setting is on.
- Survive as many swaps as possible. When the timer hits zero, the run ends.

## Timer Mechanics
- The timer starts fresh after each swap that produces at least one match (including the very first swap of the game).
- The timer does **not** run during cascade resolution — it pauses while the board is animating falling gems and clearing matches.
- The timer resumes when the board is stable and awaiting the player's next input.
- If a dead-board reshuffle is triggered, the timer pauses for the duration of the reshuffle animation, then resumes at its current value.

## Cascade Time Bonus
- When the cascade bonus setting is on:
  - A 1-level cascade (no chain) grants no bonus.
  - A 2-level cascade adds 0.5 s to the timer after resolution.
  - A 3-level cascade adds 1.0 s.
  - Each additional level adds another 0.5 s, up to a maximum of 3.0 s per cascade event.
- Bonus time is added after the cascade resolves, capped at the configured move time limit.

## State Machine
- A dedicated `MatchThreeArcadeStateMachine` in `state/` exposes `StateFlow<MatchThreeArcadeState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ GemSelected → Playing (selection updated)
 ├─ GemDeselected → Playing (selection cleared)
 ├─ SwapAttempted [creates match] → Resolving
 ├─ SwapAttempted [no match] → Playing (gems bounce back; timer continues)
 ├─ TimerExpired → GameOver
 └─ NoMovesAvailable → Reshuffling
Resolving
 ├─ CascadeDetected → Resolving (timer paused, cascade continues)
 └─ BoardStable + MovesAvailable → Playing (timer reset to full)
BoardStable + NoMovesAvailable
 └─ → Reshuffling
Reshuffling
 └─ ReshuffleComplete → Playing (timer reset to full)
GameOver
 ├─ Rematch → Playing
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best swap count (highest run) | yes |
| Total swaps made (lifetime) | yes |
| Longest cascade chain (lifetime best) | yes |
| Best run per move-time-limit setting | yes |
| Total runs played | yes |

## HUD
- Top bar: title, current swap count, settings.
- Move timer bar: full-width bar immediately below the top bar; color shifts as time depletes.
- Board: occupies the remaining screen space.
- Cascade bonus indicator: a brief "+0.5 s" or similar pop-up that rises and fades when bonus time is added.
- Game over result panel below the board: swap count, personal best, time limit used, rematch / menu buttons. Never overlays the board.
