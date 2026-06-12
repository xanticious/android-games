# Cribbage — Design Document

## Overview
- Cribbage is a two-player card game (one human vs. one AI) adapted for offline Android play.
- Standard 121-point cribbage is preserved: the deal, the discard to the crib, the play (pegging), and the show (counting hands and crib).
- A digital pegging board is the primary score display, with both players' pegs advancing along the track.
- The dealer alternates each hand; "his heels" scores two for the dealer when the starter is a Jack.
- The game emphasizes calm, readable scoring with explicit confirmation of every count so the player learns the combinations.
- An optional **Show Combos Sheet** is a cheat sheet for novice players who have not yet memorized the scoring combinations: it lists every combination of cards, suits, and colors that scores and how many points each is worth, so players can reference it while deciding which cards to discard or play.
- All match results, fastest wins, and skunk records stay local to the device.
- The board is always the visual anchor; the hand, crib, and count messaging live outside the playable card area.
- AI turns resolve quickly with visible peg movement and a short count breakdown so the player can follow the math.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Background surfaces use `Dark0` and `Dark1` in dark theme and `Aqua0` for large light-theme surfaces.
- The pegging board is a clean, flat track (no faux wood); holes are evenly spaced wells with `Dark2` recesses.
- Player pegs use `Aqua2`; opponent pegs use `Aqua4`; the trailing (older) peg of each pair is dimmed for readability.
- Cards use elevated Material cards with high-contrast suit pips; selected discards lift with an `Aqua3` ring.
- The starter (cut) card sits apart with a subtle `Aqua1` accent banner.
- Score events animate with a short peg slide and a numeric count-up; "fifteen," "pair," "run," and "thirty-one" call-outs fade in beside the board, never over the cards.
- Motion is restrained and respects the system reduce-motion setting by switching to instant transitions.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Cribbage     [≡ Combos] [⚙ Settings]│  ← Top bar
├─────────────────────────────────────┤
│  Opponent hand (face-down)   Crib 🂠 │
├─────────────────────────────────────┤
│        Pegging board (121 track)     │  ← Score anchor
│        You: 64    Opponent: 58       │
├─────────────────────────────────────┤
│   Starter 🂡    Play pile: 7 5 (=12)  │  ← Active play area
├─────────────────────────────────────┤
│        Your hand: 🂱 🂾 🃊 🃔          │
│        [Play / Discard to crib]      │
└─────────────────────────────────────┘
```
- Portrait-first. Tablet layouts widen the board and place the combos sheet as a side panel instead of a bottom sheet.

## Settings
- **Show Combos Sheet** (on/off, default on): adds a persistent "Combos" button that opens the cheat sheet of every scoring combination and its point value (see How to Play), so novice players never have to memorize the combos before deciding what to discard or play. When off, the button is hidden.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Target score**: 121 (default) or 61 (short game).
- **Skunk line shown** (on/off): highlights the 90-point skunk line on the board.
- **Auto-count my hand** (on/off, default off): when on, the app totals the player's show automatically; when off, the player taps to claim points (muggins-lite practice, no penalty).
- Settings are presented on the per-game Settings screen before play begins.

## How to Play (and Combos Sheet)
The How to Play screen explains the four phases: deal, discard, play, show. The **Combos Sheet** is a quick-reference cheat sheet for players who have not memorized cribbage scoring; it lists every scoring combination and its point value so novices can choose discards and plays with confidence:

| Combination | Points |
|-------------|--------|
| Fifteen (cards totaling 15) | 2 |
| Pair | 2 |
| Pair royal (three of a kind) | 6 |
| Double pair royal (four of a kind) | 12 |
| Run of 3 / 4 / 5 | 3 / 4 / 5 |
| Flush (4 cards in hand, same suit) | 4 |
| Flush (5 with starter) | 5 |
| Crib flush (5 only) | 5 |
| His Nobs (Jack matching starter suit, in hand) | 1 |
| His Heels (Jack cut as starter, to dealer) | 2 |
| Thirty-one (play total hits 31) | 2 |
| Go (opponent cannot play) | 1 |
| Last card (play ends below 31) | 1 |

- The combos sheet is a non-modal reference panel; opening it dims but never replaces the board, consistent with the no-modal navigation rule.

## Gameplay Loop
1. **Deal**: each player receives six cards; dealer alternates each hand.
2. **Discard**: each player sends two cards to the crib (the dealer's crib).
3. **Cut**: the non-dealer cuts the starter card; His Heels scores if it is a Jack.
4. **Play (pegging)**: players alternate laying cards, announcing the running total, scoring fifteens, thirty-ones, pairs, runs, gos, and last card; the total never exceeds 31.
5. **Show**: non-dealer counts hand first, then dealer counts hand, then dealer counts crib (order matters for close games).
6. First to the target score wins immediately, even mid-count.

## AI Opponents
- **Easy**: discards and plays with simple heuristics (keeps obvious fifteens/pairs, otherwise random); rarely optimizes the crib.
- **Medium**: evaluates expected hand value for discards and avoids feeding the opponent's pegging.
- **Hard**: uses expected-value discard tables, crib ownership awareness, and defensive pegging.
- AI difficulty never changes the rules — only decision quality.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Skunks for / against | yes |
| Best (highest) single hand counted | yes |
| Average pegging points per game | yes |

## State Machine
- A dedicated `CribbageStateMachine` in `state/` exposes `StateFlow<CribbageState>`.
```
Idle
 └─ HandStarted → Dealing
Dealing
 └─ CardsDealt → Discarding
Discarding
 └─ CribCompleted → Cutting
Cutting
 ├─ HeelsScored → Playing
 └─ NoHeels → Playing
Playing
 ├─ CardPlayed → Playing (peg scores)
 ├─ GoDeclared → Playing
 └─ PlayFinished → CountingNonDealer
CountingNonDealer → CountingDealer → CountingCrib
CountingCrib
 ├─ TargetReached → GameOver
 └─ HandContinues → Dealing (swap dealer)
GameOver
 └─ Rematch / Menu → Idle
```
- Scoring math lives in a pure `CribbageScorer` controller (no Android imports), unit-tested for fifteens, runs, pairs, flushes, nobs, and pegging edge cases (e.g., runs interrupted by out-of-sequence cards).

## HUD
- Pegging board with both scores numerically labeled and the skunk line optional.
- Running play total shown beside the play pile.
- Combos button (when enabled) and Settings in the top bar.
- Whose-turn indicator and a non-overlay count breakdown panel below the board.
