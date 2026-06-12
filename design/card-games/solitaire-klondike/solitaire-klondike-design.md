# Solitaire (Klondike) — Design Document

## Overview
- Klondike is the classic single-player patience game, adapted for offline Android play.
- Build four foundations up by suit from Ace to King; the deal is won when all 52 cards reach the foundations.
- Layout: seven tableau columns (1–7 cards, top card face-up), a stock, a waste, and four foundations.
- Tableau builds down in alternating colors; only a King (or a valid sequence headed by a King) may fill an empty column.
- Configurable draw count, redeal limits, and an optional "only winnable deals" guarantee tailor difficulty.
- There is no opponent — this is pure solo play with undo support and an auto-complete when the outcome is decided.
- All stats (games won, win streak, best time, fewest moves) stay local to the device.
- Victory and defeat presentation follow `design/common/victory-defeat.md`: the panel never covers the board.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface uses `Dark1` (dark theme) / `Aqua0` (light theme); foundations and empty slots are outlined `Dark2` wells.
- Card faces are bright with high-contrast pips; red suits tinted toward `Aqua3` only if needed for color-blind contrast (suit symbols always present).
- Valid drop targets glow `Aqua2`; the actively dragged stack lifts with an `Aqua3` shadow.
- A hint highlights a suggested move with a brief `Aqua1` pulse.
- Motion: smooth card slides, foundation snap, and a calm winning cascade; all respect reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Solitaire (Klondike)   [↶][💡][⚙]    │  ← Top bar: undo, hint, settings
├─────────────────────────────────────┤
│ [Stock] [Waste]      [♠][♥][♦][♣]    │  ← Stock/Waste left, Foundations right
├─────────────────────────────────────┤
│   C1   C2   C3   C4   C5   C6   C7    │  ← Seven tableau columns
│   🂠    🂠    🂠    🂠    🂠    🂠   🂡    │
├─────────────────────────────────────┤
│  Time 02:14   Moves 37   Passes 1/3  │  ← Status strip
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens column spacing without changing layout order.

## Settings
- **Only possible games** (true/false, default false): when true, the dealer only deals solvable layouts.
- **Draw count**: "Draw 1" or "Draw 3" cards when drawing from the stock.
- **Deck passes**: number of times you may recycle the stock — **1**, **3**, or **Infinite**.
- **Left-handed layout** (on/off): mirror stock/waste and foundations.
- Settings appear on the per-game Settings screen before play.

## How to Play
- Move cards onto the tableau in descending rank and alternating color.
- Build foundations up by suit from Ace to King.
- Draw from the stock to the waste using the configured draw count; recycle per the deck-passes setting.
- Move a full ordered run between columns; expose face-down cards by clearing what sits on top.
- Win by sending all 52 cards to the foundations.

## Controls
- Tap stock to draw; tap waste/tableau card to auto-move to a valid foundation when available.
- Drag a card or ordered run to a destination; invalid drops snap back.
- Double-tap sends a card to its foundation if legal.
- Undo reverses the last move (unlimited within a deal); hint suggests one legal move.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Games won / played per setting profile | yes |
| Current and best win streak | yes |
| Best (fastest) completion time | yes |
| Fewest moves to win | yes |

## State Machine
- A dedicated `KlondikeStateMachine` in `state/` exposes `StateFlow<KlondikeState>`.
```
Idle
 └─ DealStarted → Dealing
Dealing
 └─ Dealt → Playing
Playing
 ├─ MoveMade → Playing (validate, score, check solvable)
 ├─ StockDrawn → Playing
 ├─ UndoRequested → Playing
 ├─ NoMovesLeft → Lost
 └─ AllFoundationsComplete → AutoFinishing
AutoFinishing
 └─ CascadeDone → Won
Won / Lost
 └─ NewDeal / Menu → Idle
```
- A pure `KlondikeRules` controller validates moves and detects win/loss; a `SolvableDealGenerator` controller produces guaranteed-winnable deals when the setting is on. Both are unit-tested with no Android imports.

## HUD
- Timer, move counter, and remaining-passes indicator in the status strip.
- Undo, hint, and settings in the top bar.
- Win/lose panel slides up below the board per the common victory-defeat spec.
