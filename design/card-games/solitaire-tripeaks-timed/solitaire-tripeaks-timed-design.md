# Solitaire (TriPeaks Timed) — Design Document

## Overview
- TriPeaks Timed is the fast-paced, against-the-clock variant of TriPeaks (see `solitaire-tripeaks`).
- Core rules are identical: three overlapping peaks, play exposed cards one rank above or below the waste, chain for combos.
- A countdown timer drives the tension: clearing cards adds time, idling drains it, and reaching zero ends the run.
- Designed for quick, replayable sessions and high-score chasing rather than relaxed play.
- All stats (best score, longest chain, longest survival) stay local to the device.
- Victory and defeat presentation follow `design/common/victory-defeat.md`; the timer never overlays the peaks.

## Visual Style
- Inherits the TriPeaks visual language (underwater palette from `ui/theme/Color.kt`).
- A prominent timer bar sits in the status strip, transitioning from `Aqua2` (safe) through `Aqua4` to a warning treatment as it nears empty.
- Card clears emit a brief `Aqua1` spark and a "+time" tick; combo milestones pulse the timer bar.
- Fast, snappy motion tuned for speed; respects reduce-motion (instant transitions, timer still visible numerically).

## Screen Layout
```
┌─────────────────────────────────────┐
│ TriPeaks Timed         [⏸][⚙]        │  ← Top bar: pause, settings
├─────────────────────────────────────┤
│     🂠        🂠        🂠              │
│    🂠 🂠      🂠 🂠      🂠 🂠            │
│   🂡 🂢 🂣  🂤 🂥 🂦  🂧 🂨 🂩 🂪           │
├─────────────────────────────────────┤
│ ⏱ ▓▓▓▓▓▓░░░░  +2s   Chain x7         │  ← Timer bar + chain
│ [Stock]  [Waste 🂸]    Score 3,180    │
└─────────────────────────────────────┘
```
- Portrait-first; tablet scales peaks larger. No pop-up timer over the board.

## Settings
- **Difficulty (timer speed)**: Relaxed, Standard, Blitz — controls drain rate and time-per-clear.
- **Starting time**: short presets (e.g., 30s / 60s / 90s).
- **Continuous deal** (on/off): when a board clears, immediately deal a new one and keep the timer running for endless high-score runs.
- **Wrapping ranks** (on/off, default on).

## How to Play
- Play exposed cards one rank above/below the waste as fast as possible; each clear adds time.
- Build chains to earn larger time bonuses and score multipliers.
- Draw from the stock when stuck (breaks the chain and grants no time).
- The run ends when the timer hits zero; with Continuous deal on, the goal is the highest score before time expires.

## Controls
- Tap an eligible card to play it instantly; tap stock to draw.
- Pause suspends the timer and dims the board (non-modal pause panel below the board).
- No undo in timed mode (speed-focused); a brief input buffer prevents misfire penalties.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best score per difficulty | yes |
| Longest single chain | yes |
| Longest survival time | yes |
| Boards cleared in one run (Continuous) | yes |

## State Machine
- A dedicated `TriPeaksTimedStateMachine` in `state/` exposes `StateFlow<TriPeaksTimedState>`.
```
Idle
 └─ RunStarted → Dealing
Dealing
 └─ Dealt → Playing
Playing
 ├─ CardPlayed → Playing (add time, score, chain)
 ├─ StockDrawn → Playing (reset chain)
 ├─ TimerTick → Playing (drain; if continuous, deal on clear)
 ├─ BoardCleared → Dealing (continuous) | Playing (bonus)
 ├─ PauseRequested → Paused
 └─ TimerExpired → RunOver
Paused
 └─ Resumed → Playing
RunOver
 └─ Retry / Menu → Idle
```
- Reuses the pure `TriPeaksRules` controller for card legality and chains; a separate `TimedRunController` manages time accounting and is unit-tested without Android imports.

## HUD
- Timer bar with current bonus tick, chain multiplier, score, and stock count.
- Pause and settings in the top bar.
- Run-over panel slides up below the board per the common victory-defeat spec, highlighting a new best.
