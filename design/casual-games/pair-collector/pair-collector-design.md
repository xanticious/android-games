# Pair Collector — Design Document

## Overview
- Pair Collector is a single-player visual-search game using a standard 52-card deck of playing cards.
- Each round presents a grid of cards. All but one are unique; exactly one card is duplicated (appears twice). The player must spot both copies and tap them.
- The game ends after 10 completed rounds (win) or after accumulating 3 strikes (loss from wrong taps or time-outs if a time limit is configured).
- A count-up timer starts when the first round begins and runs continuously until the game ends, tracking total time to complete all 10 rounds.
- Personal bests track both highest round reached and fastest 10-round completion time.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Cards: standard playing-card design with clear suit symbols and rank labels. Card faces use high-contrast colors: black for clubs/spades, red for hearts/diamonds.
- Card backs: a unified navy/gold back design consistent across all rounds.
- Card grid: centered on screen with a subtle `Dark1` felt-table background.
- Correct pair: both cards briefly flash `Aqua3` and slide off screen; the round counter increments.
- Wrong tap: a brief `Coral` pulse on the tapped card; a strike indicator activates in the top bar.
- Round transition: cards slide out to the right, new cards slide in from the left.
- Strike counter: 3 heart (or anchor) icons in the top bar; each strike dims one icon.
- Timer: count-up display in the top bar, running from the first round through the last.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Pair Collector  R:3/10  ⏱1:14  ❤❤❤│  ← Top bar (round, timer, strikes)
├─────────────────────────────────────┤
│                                     │
│  [A♠][K♥][7♣][Q♦][2♠][J♥]         │
│  [9♣][4♦][8♠][Q♠][3♥][6♣]         │  ← Card grid (configurable size)
│  [5♦][10♥][J♣][2♦][9♥][A♣]        │
│                                     │
├─────────────────────────────────────┤
│  Find the duplicate card            │  ← Instruction strip (disappears after R1)
└─────────────────────────────────────┘
```
- Two taps per round: first tap selects a card (highlighted border); second tap either completes the pair (both correct) or registers a strike (one or both wrong).
- The result panel appears below the card grid after all 10 rounds or after 3 strikes; it never overlays the grid.

## Settings
- **Card count per round**: 10, 16, 20 (default), 30 — controls how many cards are shown in the grid.
- **Time limit per round**: Off (default), 10 s, 20 s, 30 s — if enabled, failing to find the pair before time expires counts as a strike and the round advances.
- **Card back style**: Navy/Gold (default), Pirate, Classic Red.
- **Show round timer** (on/off, default on): displays a secondary per-round elapsed indicator (the main timer still runs regardless).

## How to Play
- Study the card grid. One card appears exactly twice; all others are unique.
- Tap the first copy of the duplicated card to select it (highlighted border appears).
- Tap the second copy to complete the pair.
  - If both taps are on the duplicated card: round scored, cards clear, next round begins.
  - If either tap is on a non-duplicated card: strike recorded, selection resets, try again in the same round.
- The game ends when you complete 10 rounds (victory) or collect 3 strikes (defeat).
- After the game, your total time for 10 rounds (or the round you reached) is recorded.

## Controls
- **Tap** a card: selects it (first tap) or submits the pair (second tap).
- **Tap** the same selected card again: deselects it.
- No other controls are needed.

## Round Generation
- Each round draws cards from the standard 52-card deck (Ace–King × 4 suits = 52 unique cards).
- The generator selects `cardCount − 1` unique cards, then duplicates one at random to produce exactly `cardCount` cards for the round.
- Cards are shuffled and placed into the grid in random order.
- The same card is never the duplicate in two consecutive rounds.
- Card count setting determines grid size; grid layout adapts to screen width (portrait: 4–5 columns; landscape: 6–7 columns).

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Highest round reached | yes |
| Best total time for a 10-round win | yes |
| Total pairs found (lifetime) | yes |
| Perfect wins (0 strikes over 10 rounds) | yes |
| Win streak | yes |

## State Machine
- A dedicated `PairCollectorStateMachine` in `state/` exposes `StateFlow<PairCollectorState>`.
```
Idle
 └─ StartGame → DealingRound
DealingRound
 └─ RoundReady → PlayingRound
PlayingRound
 ├─ FirstCardTapped → PlayingRound (card selected)
 ├─ CardDeselected → PlayingRound (selection cleared)
 ├─ SecondCardTapped [both match duplicate] → RoundComplete
 ├─ SecondCardTapped [mismatch] → PlayingRound (strike added, selection cleared)
 └─ RoundTimeExpired [time limit mode] → PlayingRound (strike added, round resets)
RoundComplete
 ├─ MoreRoundsRemaining → DealingRound (round counter incremented)
 └─ AllRoundsComplete → GameOver (victory)
PlayingRound [3 strikes reached]
 └─ → GameOver (defeat)
GameOver
 ├─ Rematch → DealingRound
 └─ BackToMenu → Idle
```

## HUD
- Top bar: title, round counter (`R:X/10`), count-up timer, strike indicator (3 icons).
- Card grid: fills the screen center; no persistent overlays during play.
- First-card selection: highlighted `Aqua3` border on the selected card.
- Instruction strip below the grid for the first round only; hidden from round 2 onward.
- Result panel below the grid after game over: rounds completed, total time, best time, personal bests, rematch / menu buttons. Never overlays the card grid.
