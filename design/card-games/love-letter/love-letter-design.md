# Love Letter — Design Document

## Overview
- Love Letter is a fast, elegant deduction card game for 2–4 players (one human plus 1–3 AI), adapted for offline Android play.
- A tiny 16-card deck drives quick rounds: on your turn, draw one card so you hold two, then play one and resolve its effect.
- Win a round by being the last player standing or by holding the highest card when the deck runs out; first to the required tokens of affection wins the game.
- The played-card row and the player's two cards are the anchor; eliminations and tokens sit outside the play area.
- Light deduction is central — track discards to guess opponents' hands.
- All match results and win records stay local to the device.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface uses `Dark1` / `Aqua0`; each opponent seat shows a face-down hand and a tidy discard row.
- The player's two cards sit prominently at the bottom; the playable card lifts with an `Aqua3` ring on selection.
- Token-of-affection markers use `Aqua4`; eliminated players dim with a clear "out" badge.
- Card effects animate briefly (e.g., a Guard "guess" reveal, a Baron compare); respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Love Letter    Round 2   [⚙][❔]      │  ← Top bar: settings, card reference
├─────────────────────────────────────┤
│  Bot A ♥        Bot B          Bot C │
│  discards: 1 2  discards: 5    (out) │  ← Opponents + discards + tokens
├─────────────────────────────────────┤
│  Deck: 6 left      Burned: 🂠         │
├─────────────────────────────────────┤
│  Your hand: [Priest 2] [Guard 1]     │
│  [Play card] (choose target/guess)   │
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens seats. A non-modal card-reference panel (❔) lists all card effects.

## Settings
- **Number of players**: 2, 3, or 4 (one human and 1–3 bots).
- **Opponent difficulty**: Easy, Medium, Hard.
- **Tokens to win**: scales with player count by default (e.g., 7 for two players, 5 for three, 4 for four) and is adjustable.
- **Show discard history** (on/off, default on): aid deduction by keeping each player's discards visible.

## How to Play
- Each round, one card is burned face-down; in a two-player game three more are revealed.
- On your turn, draw a card (you now hold two) and play one, resolving its effect:
  - Guard: name a non-Guard card; if a chosen opponent holds it, they are out.
  - Priest: privately see an opponent's hand.
  - Baron: compare hands with an opponent; lower card is out.
  - Handmaid: immune until your next turn.
  - Prince: a chosen player discards their hand and draws anew.
  - King: trade hands with a chosen player.
  - Countess: must be played if held with King or Prince.
  - Princess: if you ever discard it, you are out.
- A round ends when one player remains or the deck empties (highest card wins); the winner gains a token.

## Controls
- Tap one of your two cards to play it; if it needs a target or a guess, a non-overlay chooser appears below the board.
- Card reference (❔) opens a non-modal effect list; opening it dims but never replaces the table.

## AI Opponents
- **Easy**: plays cards with simple rules, guesses Guards at random, ignores discard memory.
- **Medium**: uses visible discards to narrow guesses, protects with Handmaid, avoids dumping the Princess.
- **Hard**: tracks full discard counts to deduce likely hands, times Barons/Guards well, and plays around Countess tells.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per player count and difficulty | yes |
| Rounds won by survival vs. highest card | yes |
| Successful Guard guesses | yes |
| Fastest game (fewest rounds) | yes |

## State Machine
- A dedicated `LoveLetterStateMachine` in `state/` exposes `StateFlow<LoveLetterState>`.
```
Idle
 └─ RoundStarted → Setup
Setup
 └─ DeckBurned → TurnStart
TurnStart
 └─ CardDrawn → AwaitingPlay
AwaitingPlay
 ├─ CardPlayed → ResolvingEffect
 └─ TargetChosen → ResolvingEffect
ResolvingEffect
 ├─ PlayerEliminated → CheckRoundEnd
 └─ EffectResolved → CheckRoundEnd
CheckRoundEnd
 ├─ RoundContinues → TurnStart (next player)
 └─ RoundOver → AwardToken
AwardToken
 ├─ GameWon → GameOver
 └─ NextRound → Setup
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `LoveLetterRules` controller resolves each card effect, enforces the Countess rule, and determines round/game winners; unit-tested without Android imports.

## HUD
- Per-player token counts, discard rows, deck-remaining count, and the burned-card indicator.
- Round number and whose-turn highlight.
- Round-end and game-end panels slide up below the table per the common victory-defeat spec.
