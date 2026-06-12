# Backgammon — Design Document

## Overview
Backgammon is a classic race-and-contact board game focused on tempo, risk, and efficient checker movement.
This version is tuned for single-player portrait play on Android with clear tap-driven interactions.
The tone is elegant, calm, and premium rather than casino-like.
Matches use standard rules with hitting, entering from the bar, bearing off, gammons, and backgammons.
The doubling cube is intentionally omitted to keep decision flow simple and stats easier to compare.
The player always faces an AI opponent.
Dice roll automatically at the start of every turn.
The main skill expression comes from reading the board, minimizing blots, building primes, and timing the race home.

## Visual Style
The board uses a polished tabletop look with Material 3 spacing and the underwater palette.
Primary background surfaces use Dark0 and Dark1.
Point separators, borders, and deep shadows use Dark2.
Active highlights, move indicators, and dice accents use Aqua3 and Aqua4.
Soft selection fills and legal-move glows use Aqua0 and Aqua1.
Checker colors are White and Black as rules-facing labels, but rendering uses light and dark discs with subtle Aqua reflections so they still fit the app palette.
Triangles are alternating dark and light wood-inspired surfaces expressed through Dark1, Dark2, Aqua0, and muted neutral tints derived from Material 3 surfaces.
The center bar is visually strong and slightly raised so portrait orientation remains readable.
Dice are large rounded Material 3 chips with bold pips.
Hit checkers on the bar stack vertically with a slight offset.
Borne-off checkers appear in neat trays at the top and bottom edges of the screen, never over the board.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Backgammon      Auto dice   [⚙]     │  ← Top bar
├─────────────────────────────────────┤
│ Off tray: AI 3        Bar: ●●       │
│ ┌─────────────────────────────────┐ │
│ │ 13 14 15 16 17 18 │ 19 ... 24  │ │
│ │ ▾  ▾  ▾  ▾  ▾  ▾  │ ▾      ▾   │ │
│ │                                 │ │  ← Board anchor
│ │ ▴  ▴  ▴  ▴  ▴  ▴  │ ▴      ▴   │ │
│ │ 12 11 10  9  8  7 │  6 ...  1  │ │
│ └─────────────────────────────────┘ │
│ Off tray: You 5      Dice: ⚂ ⚄     │
├─────────────────────────────────────┤
│ Turn: You   Moves left: 5, 3        │  ← HUD strip
│ [Undo move] [Confirm turn]          │
└─────────────────────────────────────┘
```
Portrait-first: the board fills the center vertically with trays above and below it. Tablet layouts keep the same board orientation but widen side gutters for move history, borne-off counts, and pip-count details.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard (default Medium).
- **Match length**: Single game (default), best of 3, or best of 5; the doubling cube remains omitted in every format.
- **Show legal moves** (on/off, default on): highlights destinations after selecting a checker.
- **Show pip counts** (on/off, default on): displays current race totals in the HUD.
- **Confirm turn** (on/off, default on): requires an explicit confirmation after all dice values are used or skipped.
- Settings are chosen on the per-game Settings screen before play begins.

## How to Play
Move all 15 of your checkers around the board into your home board, then bear them off before the AI does. Dice roll automatically at the start of each turn. Each die value moves one checker that many points; doubles are played four times.

A single opposing checker on a point is a blot and can be hit, sending it to the bar. If you have checkers on the bar, you must enter them before moving any other checker. Points with two or more opposing checkers are blocked. Once all of your checkers are in your home board, legal dice values may bear checkers off. Wins can be normal wins, gammons, or backgammons; no doubling cube is used.

## Controls
- Tap a checker to select it; legal destinations glow with Aqua0/Aqua1 fills and Aqua3/Aqua4 accents.
- Tap a destination to spend the matching die value. If two dice can reach the same point, the app chooses the exact die only when unambiguous; otherwise it prompts with inline die chips below the board.
- Tap a bar checker to enter from the bar when required.
- Tap **Undo move** before confirming the turn to step back through the current turn only.
- Tap **Confirm turn** when all legal dice are used or no legal move remains.

## AI Opponents
- **Easy**: prioritizes entering from the bar, bearing off, and obvious hits, but leaves avoidable blots and misses prime-building tactics.
- **Medium**: balances racing, blocking, hitting, and safety; avoids most loose blots and recognizes simple gammon threats.
- **Hard**: evaluates pip count, contact timing, anchors, primes, hit equity, gammon risk, and bear-off efficiency.
- AI difficulty changes decision quality only; the rules, automatic dice, gammons, backgammons, and omitted doubling cube never change.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Normal wins, gammons, and backgammons for / against | yes |
| Best match streak | yes |
| Average pip-count lead at win | yes |
| Games won from the bar after being hit | yes |

## State Machine
A dedicated `BackgammonStateMachine` in `state/` exposes `StateFlow<BackgammonState>` and owns turn, dice, bar-entry, bearing-off, and game-over transitions.
```
Idle
 └─ MatchStarted → RollingDice
RollingDice
 └─ DiceRolled → SelectingChecker
SelectingChecker
 ├─ CheckerSelected → ChoosingMove
 ├─ NoLegalMoves → EndingTurn
 └─ GameFinished → GameOver
ChoosingMove
 ├─ MovePlayed → SelectingChecker
 ├─ MoveUndone → SelectingChecker
 └─ TurnConfirmed → EndingTurn
EndingTurn
 └─ TurnPassed → RollingDice
GameOver
 └─ Rematch / Menu → Idle
```
A pure `BackgammonRules` controller in `controller/` has no Android imports and is unit-tested for legal movement, forced bar entry, hitting, blocked points, doubles, bearing off, gammons, and backgammons.

## HUD
- Current player, automatic dice roll, unused dice values, and confirm-turn status.
- Bar counts, borne-off trays, pip counts when enabled, and a compact move-history strip.
- Inline warnings for forced bar entry, no legal moves, and bear-off eligibility.
- Victory/defeat messaging follows `design/common/victory-defeat.md`: it appears below the board, never over the board.
