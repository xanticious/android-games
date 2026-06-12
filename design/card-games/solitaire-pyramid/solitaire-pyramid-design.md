# Solitaire (Pyramid) — Design Document

## Overview
- Pyramid is a single-deck matching patience game adapted for offline Android play.
- Twenty-eight cards form a seven-row pyramid; the rest form a stock with a waste pile.
- Remove pairs of exposed cards whose ranks sum to 13 (Ace=1 … Queen=12); a King (13) is removed alone.
- A card is "exposed" only when no card overlaps it from below; clearing the pyramid wins the deal.
- There is no opponent — pure solo play with undo and an optional redeal limit.
- All stats (games won, win streak, best time, best score) stay local to the device.
- Victory and defeat presentation follow `design/common/victory-defeat.md`.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface uses `Dark1` / `Aqua0`; the pyramid overlaps cards in a clean stepped formation.
- Exposed (selectable) cards are fully opaque; blocked cards dim slightly so the player reads availability at a glance.
- A first selection highlights with an `Aqua3` ring; a completed sum-of-13 pair flashes `Aqua2` before clearing.
- Motion: card lift on select, fade-out on match; respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Solitaire (Pyramid)    [↶][💡][⚙]    │  ← Top bar
├─────────────────────────────────────┤
│              🂠                        │
│            🂠  🂠                       │
│          ��  🂠  🂠                      │  ← 7-row pyramid (28 cards)
│        🂠  🂠  🂠  🂠                     │
│      🂠  🂠  🂠  🂠  🂠                    │
│    🂠  🂠  🂠  🂠  🂠  🂠                   │
│  🂡  🂢  🂣  🂤  🂥  🂦  🂧                  │
├─────────────────────────────────────┤
│ [Stock]  [Waste 🂸]      Score 240    │  ← Stock / Waste / score
└─────────────────────────────────────┘
```
- Portrait-first; tablet scales the pyramid larger.

## Settings
- **Redeals through the stock**: 0, 2, or Unlimited.
- **Only possible games** (true/false, default false): deal only solvable layouts.
- **Show available pairs hint** (on/off): subtly mark cards that can currently combine.

## How to Play
- Tap two exposed cards whose ranks total 13 to remove the pair.
- Tap a King to remove it on its own.
- Draw from the stock to the waste; the top waste card and any exposed pyramid card may combine.
- Recycle the stock per the redeal setting.
- Win by clearing the entire pyramid.

## Controls
- Tap to select; tap a second card to complete a sum-of-13 match (or tap a King to remove).
- Tap the stock to draw; undo reverses the last action.
- Hint highlights one currently legal pair.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Games won / played | yes |
| Current and best win streak | yes |
| Best (fastest) completion time | yes |
| Best score (cards cleared / chained removals) | yes |

## State Machine
- A dedicated `PyramidStateMachine` in `state/` exposes `StateFlow<PyramidState>`.
```
Idle
 └─ DealStarted → Dealing
Dealing
 └─ Dealt → Playing
Playing
 ├─ CardSelected → Playing (await pair)
 ├─ PairCleared → Playing (score, re-expose)
 ├─ StockDrawn → Playing
 ├─ UndoRequested → Playing
 ├─ NoMovesLeft → Lost
 └─ PyramidCleared → Won
Won / Lost
 └─ NewDeal / Menu → Idle
```
- A pure `PyramidRules` controller validates exposure, sum-of-13 matches, and win/loss; unit-tested without Android imports.

## HUD
- Score, stock count, and redeal indicator.
- Undo, hint, settings in the top bar.
- Win/lose panel slides up below the board per the common victory-defeat spec.
