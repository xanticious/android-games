# Solitaire (FreeCell) — Design Document

## Overview
- FreeCell is a single-deck open-information patience game adapted for offline Android play.
- All 52 cards are dealt face-up across eight tableau columns; nearly every deal is solvable through skill.
- Four free cells provide temporary single-card storage; four foundations build up by suit from Ace to King.
- Tableau builds down in alternating colors; a "supermove" of multiple cards is allowed when enough free cells and empty columns exist to stage it.
- There is no hidden information and no opponent — the challenge is pure planning.
- All stats (games won, win streak, best time, fewest moves) stay local to the device.
- Victory and defeat presentation follow `design/common/victory-defeat.md`.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface uses `Dark1` / `Aqua0`; free cells and foundations are outlined `Dark2` wells with small labels.
- Valid drop targets glow `Aqua2`; supermove previews show a translucent `Aqua1` stack ghost.
- Foundation completion plays a brief celebratory cascade; motion respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Solitaire (FreeCell)   [↶][💡][⚙]    │  ← Top bar
├─────────────────────────────────────┤
│ [ ][ ][ ][ ]          [♠][♥][♦][♣]   │  ← Free cells (left) / Foundations (right)
├─────────────────────────────────────┤
│  C1  C2  C3  C4  C5  C6  C7  C8       │  ← Eight tableau columns (all face-up)
│  🂡   🂲   🃎   🂸   🂥   🃔   🂩   ��       │
├─────────────────────────────────────┤
│  Time 03:02   Moves 51   Free 2/4    │  ← Status strip
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens columns.

## Settings
- **Number of free cells**: 4 (standard) or a reduced count (3, 2) for a harder challenge.
- **Only possible games** (true/false, default true): deal only solvable layouts (almost all FreeCell deals are anyway).
- **Auto-move to foundations** (on/off, default on): safe cards advance automatically.
- **Left-handed layout** (on/off): mirror free cells and foundations.

## How to Play
- Build foundations up by suit from Ace to King.
- Build tableau columns down in alternating colors.
- Park a single card in any empty free cell; an empty column accepts any card or a valid supermove.
- Maximum cards moved at once = (free cells + 1) × 2^(empty columns); the app enforces and previews this.
- Win by moving all 52 cards to the foundations.

## Controls
- Tap a card to send it to a free cell or foundation when a single safe target exists.
- Drag a card or valid run to a destination; the app auto-stages supermoves through free cells/empty columns.
- Undo reverses the last move (unlimited); hint suggests one productive move.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Games won / played | yes |
| Current and best win streak | yes |
| Best (fastest) completion time | yes |
| Fewest moves to win | yes |

## State Machine
- A dedicated `FreeCellStateMachine` in `state/` exposes `StateFlow<FreeCellState>`.
```
Idle
 └─ DealStarted → Dealing
Dealing
 └─ Dealt → Playing
Playing
 ├─ MoveMade → Playing (validate supermove capacity, score)
 ├─ UndoRequested → Playing
 ├─ NoMovesLeft → Lost
 └─ AllFoundationsComplete → AutoFinishing
AutoFinishing
 └─ CascadeDone → Won
Won / Lost
 └─ NewDeal / Menu → Idle
```
- A pure `FreeCellRules` controller validates moves, computes supermove capacity, and detects win/loss; unit-tested without Android imports.

## HUD
- Timer, move counter, and free-cell availability in the status strip.
- Undo, hint, settings in the top bar.
- Win/lose panel slides up below the board per the common victory-defeat spec.
