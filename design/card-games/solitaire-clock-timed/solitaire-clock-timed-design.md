# Solitaire (Clock Timed) — Design Document

## Overview
- Clock Timed is the classic "fortune-telling" Clock solitaire played against a stopwatch, adapted for offline Android play.
- Thirteen piles of four cards are arranged like a clock face (positions 1–12 plus a center King pile).
- Flip the top card of the King (center) pile, place it face-up beneath its matching hour pile, then flip that pile's top card, and continue.
- The deal is won if all four Kings are turned up last (i.e., every other pile is exhausted first).
- Two modes change whether the deal is winnable; in both, the goal is to finish as fast as possible.
- This is pure solo play with no opponent; classic Clock is largely luck, so the mode choice tunes that.
- All stats (best time per mode, win rate) stay local to the device.
- Victory and defeat presentation follow `design/common/victory-defeat.md`; the timer never overlays the clock.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- The twelve hour piles sit evenly around a circle on a `Dark1` / `Aqua0` surface; the King pile occupies the center.
- The active (just-placed) pile pulses `Aqua3`; placed cards fan slightly so counts are readable.
- Kings turning up are accented `Aqua4`; the timer sits in the status strip, not over the clock.
- Motion: card slide to its hour position and a flip reveal; respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Solitaire (Clock Timed)   [⏸][⚙]     │  ← Top bar
├─────────────────────────────────────┤
│            11   12    1               │
│        10              2              │
│       9      [K center]    3          │  ← Clock face of 13 piles
│        8               4              │
│            7    6     5               │
├─────────────────────────────────────┤
│  ⏱ 00:42    Kings up 2/4   Best 00:31│  ← Timer / progress / best
└─────────────────────────────────────┘
```
- Portrait-first; tablet enlarges the clock radius.

## Settings
- **Mode**:
  - **Always possible (just go fast)** — the deal is pre-arranged to be solvable, so the only challenge is speed.
  - **Classic** — a true random deal (~1 in 13 chance of finishing); finishing is mostly fate.
- **Auto-place** (on/off, default on): the next card moves automatically after a flip, so the player only watches/taps to advance speed; when off, the player taps each placement (more control, useful for screenshots/learning).
- **Show pile counts** (on/off): display remaining cards per pile.

## How to Play
- Start by flipping the top card of the center King pile.
- Place each flipped card face-up under its matching hour pile (Ace=1 … Queen=12, King=center).
- Flip the top of the pile you just fed, and repeat.
- You lose the deal if the fourth King turns up while other piles still have face-down cards.
- You win if every hour pile is completed and the Kings come up last; in both modes, your time is recorded.

## Controls
- Tap to flip/advance (or hold to fast-forward when auto-place is on).
- Pause suspends the timer with a non-modal panel below the clock.
- No undo (the outcome is largely determined by the shuffle); the focus is speed and replay.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best (fastest) winning time per mode | yes |
| Wins / deals played per mode | yes |
| Win rate (Classic mode) | yes |
| Longest win streak (Always-possible) | yes |

## State Machine
- A dedicated `ClockSolitaireStateMachine` in `state/` exposes `StateFlow<ClockSolitaireState>`.
```
Idle
 └─ DealStarted → Dealing
Dealing
 └─ Dealt → Playing (timer starts)
Playing
 ├─ CardFlipped → Playing (place under matching hour)
 ├─ KingTurnedUp → Playing (count kings)
 ├─ PauseRequested → Paused
 ├─ FourthKingEarly → Lost
 └─ AllPilesComplete → Won
Paused
 └─ Resumed → Playing
Won / Lost
 └─ NewDeal / Menu → Idle
```
- A pure `ClockRules` controller handles placement, win/loss detection, and the solvable-deal arrangement for Always-possible mode; unit-tested without Android imports.

## HUD
- Timer, kings-up progress, and best-time reference.
- Pause and settings in the top bar.
- Win/lose panel slides up below the clock per the common victory-defeat spec, flagging a new best time.
