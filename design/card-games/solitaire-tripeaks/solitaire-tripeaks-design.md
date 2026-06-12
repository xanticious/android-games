# Solitaire (TriPeaks) — Design Document

## Overview
- TriPeaks is a single-deck card-clearing patience game adapted for offline Android play.
- Twenty-eight cards form three overlapping peaks above a ten-card base row; the rest form a stock with one waste/foundation pile.
- Remove an exposed peak card if its rank is exactly one higher or one lower than the current waste card (rank wraps: Ace links to both King and Two).
- Long uninterrupted chains build a streak multiplier for higher scores.
- There is no opponent — relaxed solo play; the timed variant is documented separately (`solitaire-tripeaks-timed`).
- All stats (games won, best score, longest chain) stay local to the device.
- Victory and defeat presentation follow `design/common/victory-defeat.md`.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface uses `Dark1` / `Aqua0`; the three peaks overlap in a clean tiered fan.
- Exposed cards are fully opaque; covered cards dim. The active waste card sits prominently with an `Aqua3` accent.
- A growing chain shows a streak counter with an `Aqua2`-to-`Aqua4` color ramp; respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Solitaire (TriPeaks)   [↶][💡][⚙]    │  ← Top bar
├─────────────────────────────────────┤
│     🂠        🂠        🂠              │
│    🂠 🂠      🂠 🂠      🂠 🂠            │  ← Three overlapping peaks
│   🂡 🂢 🂣  🂤 🂥 🂦  🂧 🂨 🂩 🂪           │  ← Base row (exposed)
├─────────────────────────────────────┤
│ [Stock x18]   [Waste 🂸]   Chain x5   │  ← Stock / waste / chain
│            Score 1,240                │
└─────────────────────────────────────┘
```
- Portrait-first; tablet scales peaks larger.

## Settings
- **Wrapping ranks** (on/off, default on): when on, Ace bridges King and Two.
- **Only possible games** (true/false, default false): deal only clearable layouts.
- **Scoring style**: Classic (chain bonuses) or Casual (flat per-card, relaxed).

## How to Play
- Tap any exposed card one rank above or below the waste card to move it onto the waste and extend your chain.
- When stuck, draw from the stock to set a new waste card (this breaks the current chain).
- Clear all three peaks to win the deal; remaining stock at win time grants bonus points.

## Controls
- Tap an eligible card to play it; ineligible taps give a gentle shake.
- Tap the stock to draw; undo reverses the last play.
- Hint highlights one currently playable card.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Games won / played | yes |
| Best score | yes |
| Longest single chain | yes |
| Peaks cleared (lifetime) | yes |

## State Machine
- A dedicated `TriPeaksStateMachine` in `state/` exposes `StateFlow<TriPeaksState>`.
```
Idle
 └─ DealStarted → Dealing
Dealing
 └─ Dealt → Playing
Playing
 ├─ CardPlayed → Playing (extend chain, score, re-expose)
 ├─ StockDrawn → Playing (reset chain)
 ├─ UndoRequested → Playing
 ├─ NoMovesAndNoStock → Lost
 └─ AllPeaksCleared → Won
Won / Lost
 └─ NewDeal / Menu → Idle
```
- A pure `TriPeaksRules` controller validates exposure, rank-adjacency (with optional wrap), chain scoring, and win/loss; unit-tested without Android imports.

## HUD
- Score, chain multiplier, and remaining stock count.
- Undo, hint, settings in the top bar.
- Win/lose panel slides up below the board per the common victory-defeat spec.
