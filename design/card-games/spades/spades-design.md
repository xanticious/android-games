# Spades — Design Document

## Overview
- Spades is a four-player partnership trick-taking card game with one human and three AI players (two teams of two), adapted for offline Android play.
- Standard rules: spades are always trump; each player bids the number of tricks they expect to take, and partners' bids combine for the team contract.
- Make your team's combined bid to score 10 points per bid trick; overtricks ("bags") score 1 each but trigger a penalty every 10 accumulated bags.
- Failing the contract loses 10 points per bid trick. A successful Nil (bidding zero) scores a bonus; failing Nil is penalized.
- The player's hand and the current trick are the visual anchor; bids and scores stay outside the trick area.
- All match results and best games stay local to the device.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface uses `Dark1` / `Aqua0`; partner (across) is visually linked to the player with a shared `Aqua2` team tint, opponents with `Aqua4`.
- The trick well is centered; legal cards are lit and illegal cards dim during follow-suit enforcement.
- Bid badges sit beside each seat; "spades broken" shows a small persistent indicator.
- Trick resolution slides cards to the winning seat with a brief pulse; respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Spades       Round 4   [⚙ Settings]  │  ← Top bar
├─────────────────────────────────────┤
│           Partner (AI) bid 3         │
│  Opp West bid 2  [trick]  Opp East 4 │  ← Trick well + seat bids
│                 🂡  🂫                  │
├─────────────────────────────────────┤
│ Team Us 320 (bags 4)  Them 280 (b 7) │  ← Score strip
├─────────────────────────────────────┤
│   Your hand: 🂢 🂦 🂩 🂭 🃁 🃅 🃊 ...     │
│   [Bid: 0–13 / Nil]  /  [Play card]  │
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens the table without reordering seats.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard.
- **Game-end score**: 250 or 500 (default 500).
- **Nil allowed** (on/off, default on); **Blind Nil** (on/off, default off).
- **Bag penalty**: 100 points per 10 bags (default) or off.

## How to Play
- Each hand: receive 13 cards and bid the tricks you expect to take (including Nil for zero).
- Play proceeds clockwise; follow the led suit if able, otherwise play anything (spades cannot be led until "broken").
- Highest spade wins a trick; if none, the highest card of the led suit wins.
- Score the team contract at hand's end; first team to the game-end score wins (highest if both pass it same hand).

## Controls
- During bidding, choose your bid with a stepper (0–13) or tap Nil.
- During play, tap a legal card; illegal cards are non-interactive with a gentle shake.

## AI Opponents
- **Easy**: bids by counting high cards loosely; plays follow-suit with minimal planning.
- **Medium**: bids with trump/long-suit awareness, supports partner, and tracks spades broken.
- **Hard**: counts cards, coordinates with the partner contract, sets opponents' Nils, and manages bags.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Team wins / losses per difficulty | yes |
| Successful Nils / Blind Nils | yes |
| Bags incurred (lifetime) | yes |
| Best winning margin | yes |

## State Machine
- A dedicated `SpadesStateMachine` in `state/` exposes `StateFlow<SpadesState>`.
```
Idle
 └─ HandStarted → Dealing
Dealing
 └─ Dealt → Bidding
Bidding
 └─ AllBidsIn → Leading
Leading
 └─ LeadPlayed → FollowingTrick
FollowingTrick
 ├─ CardPlayed → FollowingTrick
 └─ TrickComplete → ResolveTrick
ResolveTrick
 ├─ HandContinues → Leading
 └─ HandComplete → Scoring
Scoring
 ├─ GameEndReached → GameOver
 └─ NextHand → Dealing
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `SpadesRules` controller enforces follow-suit, spades-broken, trick winner, contract/bag/Nil scoring; unit-tested without Android imports.

## HUD
- Per-team scores with bag counts, per-seat bids, current round, and spades-broken indicator.
- Hand-end and game-end panels slide up below the table per the common victory-defeat spec.
