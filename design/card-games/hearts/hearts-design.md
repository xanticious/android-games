# Hearts — Design Document

## Overview
- Hearts is a four-player trick-avoidance card game with one human and three AI opponents, adapted for offline Android play.
- Standard rules: avoid taking hearts (1 point each) and the Queen of Spades (13 points); lowest score wins when someone reaches the game-end threshold.
- "Shooting the Moon" — taking all hearts and the Queen of Spades in one hand — flips the scoring (0 to you, 26 to everyone else).
- The passing phase rotates left, right, across, and hold (no pass) over a four-hand cycle.
- The player's hand and the current trick are the visual anchor; scores and messaging stay outside the trick area.
- All match results, lowest-game records, and moons shot stay local to the device.
- AI plays quickly with legible card lead/follow so the player can track the trick.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface uses `Dark1` / `Aqua0`; the trick area is a clean central well with `Dark2` framing.
- The player's hand fans along the bottom; legal cards are fully lit while illegal cards dim during follow-suit enforcement.
- The lead suit is labeled; the Queen of Spades carries a subtle `Aqua4` warning accent when in hand.
- Trick winner is shown by sliding the four cards toward that seat with a brief `Aqua2` pulse; respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Hearts        Round 3   [⚙ Settings] │  ← Top bar
├─────────────────────────────────────┤
│            North (AI)                │
│        🂠��🂠🂠🂠🂠🂠🂠🂠🂠🂠🂠🂠         │
│  West (AI)    [ trick ]    East (AI) │  ← Trick well, seats around
│                🂱  🃞                  │
├─────────────────────────────────────┤
│  Scores  You 18  N 22  E 9  W 31     │  ← Score strip
├─────────────────────────────────────┤
│   Your hand: 🂲 🂵 🂸 🂾 🃊 🃔 🃗 ...     │
│   [Pass 3 cards →]  /  [Play card]   │
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens the table without reordering seats.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard (applies to all three bots, or per-seat if expanded later).
- **Game-end score**: 50 or 100 (default 100).
- **Jack of Diamonds variant** (on/off, default off): −10 points to whoever takes the Jack of Diamonds.
- **Pass direction reminder** (on/off): show the current pass direction prominently.

## How to Play
- Each hand: receive 13 cards, pass 3 per the rotation (left/right/across/hold).
- The holder of the 2♣ leads it to start the first trick.
- Follow the led suit if able; otherwise play any card (hearts cannot be led until "broken," unless only hearts remain).
- The highest card of the led suit wins the trick and leads the next.
- Score penalty cards at hand's end; lowest total when someone hits the game-end score wins.

## Controls
- During passing, tap to select exactly three cards, then confirm.
- During play, tap a legal card to play it; illegal cards are non-interactive with a gentle shake.

## AI Opponents
- **Easy**: follows suit and dumps high penalties carelessly; never tries to shoot the moon.
- **Medium**: tracks broken hearts and the Queen, ducks tricks, and recognizes obvious moon attempts.
- **Hard**: counts cards, manages void suits, defends against the human's moon, and occasionally shoots the moon itself.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Lowest winning game score | yes |
| Moons shot (by player) | yes |
| Queen of Spades taken count | yes |

## State Machine
- A dedicated `HeartsStateMachine` in `state/` exposes `StateFlow<HeartsState>`.
```
Idle
 └─ HandStarted → Dealing
Dealing
 └─ Dealt → Passing (or → Leading on hold-hand)
Passing
 └─ CardsPassed → Leading
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
 └─ NextHand → Dealing (rotate pass)
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `HeartsRules` controller enforces follow-suit, hearts-broken, the opening 2♣, trick winner, and moon scoring; unit-tested without Android imports.

## HUD
- Per-seat running scores, current round, and pass-direction reminder.
- Hearts-broken indicator and the led suit label.
- Hand-end and game-end panels slide up below the table per the common victory-defeat spec.
