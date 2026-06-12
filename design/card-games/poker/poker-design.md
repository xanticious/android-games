# Poker — Design Document

## Overview
- A single-variant Five-Card Draw poker game: one human versus three AI players, adapted for offline Android play.
- Each hand: ante, deal five cards, a first betting round, a draw (discard and replace), a second betting round, then showdown.
- Standard hand rankings apply (high card up to royal flush); the best five-card hand wins the pot.
- Chips are play money only — no real currency, no store, no ads — and all balances stay local to the device.
- The game runs until the human busts out or chooses to leave; bots that bust are replaced or sit out per settings.
- The community area (pot, board state) and the player's hand are the anchor; results and chip counts sit outside the card area.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Felt-style table uses `Dark1` / `Aqua0`; chip stacks are color-coded denominations with labels for accessibility.
- The player's five cards fan along the bottom; cards marked for discard lift with an `Aqua3` ring.
- The active-to-act seat is highlighted `Aqua2`; the pot total sits centered with an `Aqua4` accent.
- Bet/raise actions animate chips sliding to the pot; showdown reveals flip in turn. Respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Poker — 5-Card Draw    [⚙ Settings]  │  ← Top bar
├─────────────────────────────────────┤
│   Bot A $420   Bot B $0 (out)        │
│        Pot: $180      Bot C $610      │  ← Opponents + pot
├─────────────────────────────────────┤
│   To call: $20   [Fold][Call][Raise] │  ← Action bar
├─────────────────────────────────────┤
│   Your cards: 🂡 🂮 🂭 🃁 🃔             │
│   You $500   Ante $5   [Draw: tap to │
│   mark discards, then Confirm]       │
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens the table without reordering seats.

## Settings
- **Starting cash**: configurable amount per player (e.g., $200 / $500 / $1000).
- **Per-bot difficulty**: each of the three bots independently set to Easy, Medium, or Hard.
- **Ante amount**: fixed ante each player posts before the deal.
- **Blinds** (on/off + small/big amounts): optional small/big blind structure layered on or instead of antes.
- **Bet limit style**: No-Limit, Pot-Limit, or Fixed-Limit (default Fixed-Limit for approachable play).
- **Bots rejoin on bust** (on/off): whether busted bots re-buy or sit out.

## How to Play
- Post the ante (and blinds, if enabled).
- Each player is dealt five cards face-down.
- First betting round: in turn, fold, check/call, or raise.
- Draw: discard 0–5 cards and draw replacements.
- Second betting round, then showdown — best five-card hand takes the pot (split on ties).

## Controls
- Action bar buttons: Fold, Check/Call, Bet/Raise with a chip-amount stepper or slider.
- During the draw, tap cards to mark/unmark discards, then Confirm.
- A non-overlay hand-strength hint can be enabled to label your current best hand (off by default for skill play).

## AI Opponents
- **Easy**: plays hand strength only, rarely bluffs, predictable draws.
- **Medium**: considers pot odds, draws sensibly, bluffs occasionally, reacts to bet sizing.
- **Hard**: models position and ranges, varies bet sizing, bluffs and slow-plays, exploits passive play.
- Each bot uses its independently configured difficulty.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Sessions played / biggest final stack | yes |
| Hands won / showdowns won | yes |
| Best hand achieved (e.g., four of a kind) | yes |
| Largest pot won | yes |

## State Machine
- A dedicated `PokerStateMachine` in `state/` exposes `StateFlow<PokerState>`.
```
Idle
 └─ HandStarted → Anteing
Anteing
 └─ AntesPosted → Dealing
Dealing
 └─ Dealt → BettingRound1
BettingRound1
 ├─ ActionTaken → BettingRound1
 └─ RoundClosed → Drawing
Drawing
 └─ DrawsComplete → BettingRound2
BettingRound2
 ├─ ActionTaken → BettingRound2
 ├─ AllButOneFolded → AwardPot
 └─ RoundClosed → Showdown
Showdown → AwardPot
AwardPot
 ├─ HumanBusted → SessionOver
 └─ NextHand → Anteing
SessionOver
 └─ NewSession / Menu → Idle
```
- A pure `HandEvaluator` controller ranks five-card hands and resolves ties; a `BettingController` enforces legal actions and pot/side-pot math. Both are unit-tested without Android imports.

## HUD
- Pot total, each seat's stack and current bet, the to-call amount, and whose turn it is.
- Ante/blind reminder and bet-limit indicator.
- Hand-result and session-over panels slide up below the table per the common victory-defeat spec.
