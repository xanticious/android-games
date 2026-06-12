# Deck Builder Fleet — Design Document

## Overview
- Deck Builder Fleet is a fast, head-to-head deck-building duel for one human versus one AI, adapted for offline Android play.
- Inspired by the structure of competitive deck-builders: buy cards from a shared market to build a trade-and-combat engine, then reduce the opponent's health to zero.
- Theme uses real-world ocean-faring vessels and coastal forts — entirely original art and naming, no third-party properties.
- Always two players (human vs. bot); the central Trade Row and both players' health are the visual anchors.
- Victory/defeat status appears below the board, never over the cards.
- All match results, win streaks, and fastest victories stay local to the device.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Nautical-flat art: clean ship silhouettes on a calm sea surface (`Dark1` / `Aqua0`).
- Trade Row cards are elevated Material cards; affordable ships highlight `Aqua2`.
- The opponent's health uses `Aqua4`; the player's uses `Aqua2`; forts (bases) show a shield emblem and a separate HP pip.
- Plays animate as ship slides and cannon-flash combat ticks; sinking a fort plays a brief splash below the board. Respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Deck Builder Fleet     [⚙][❔]        │  ← Top bar
├─────────────────────────────────────┤
│  Opponent (Bot)   ❤ 32   Forts: [🛡18]│
├─────────────────────────────────────┤
│  Trade Row: [Skiff][Sub][Battleship] │
│             [Carrier][Fort]   Pool 🂠 │  ← Shared market + draw pool
├─────────────────────────────────────┤
│  You  ❤ 40   💰Trade 6   ⚔Combat 9    │
│  Hand: [card][card][card][card][card]│
│  [Play all] [Buy] [Attack]           │
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens the Trade Row and shows both discard piles.

## Settings
- **Difficulty**: Easy, Medium, Hard.
- **Starting health**: configurable for both players (e.g., 30 / 40 / 50; default 50).
- **Trade Row size**: 5 (default) or 6 for more options.
- **Explorer-style basic buy** (on/off): an always-available neutral ship to smooth out turns.

## Ship & Fort Types (original classes)
Vessels are grouped into classes that reward building "fleets" of the same class (ally synergies):
- **Row Boat / Skiff** — cheap starter and early-game trade; thin but reliable.
- **Submarine** — stealth combat that can bypass enemy forts.
- **Battleship** — heavy combat damage; expensive but decisive.
- **Aircraft Carrier** — launches reusable strike effects and card draw.
- **Fort (coastal base)** — stays in play across turns, providing ongoing trade/combat or a defensive wall the opponent must destroy before attacking health.
Same-class synergies grant bonus effects when two of a class are in play the same turn.

## How to Play
- Start with a thin starter deck (row boats + basic trade cards) and the chosen starting health.
- On your turn: draw 5 cards, play them to generate Trade (currency) and Combat (damage).
- Spend Trade to buy ships/forts from the Trade Row into your discard pile.
- Spend Combat to destroy enemy forts first, then to reduce the opponent's health.
- Forts you play remain in front of you until destroyed; reduce the opponent to 0 health to win.

## Controls
- Tap a hand card to play it; tap a Trade Row card to buy (greyed when unaffordable).
- Tap a target (enemy fort or the opponent's health) to allocate Combat.
- Reference (❔) opens a non-modal glossary of classes, keywords, and synergies.

## AI Opponent
- **Easy**: buys by raw cost, attacks face whenever possible, ignores synergies.
- **Medium**: pursues a class focus, clears forts sensibly, balances trade vs. combat.
- **Hard**: optimizes buys for synergy curves, manages forts defensively, and times alpha-strikes for lethal.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Current and best win streak | yes |
| Fastest victory (fewest turns) | yes |
| Favored ship class / win rate by class | yes |

## State Machine
- A dedicated `FleetDeckStateMachine` in `state/` exposes `StateFlow<FleetDeckState>`.
```
Idle
 └─ MatchStarted → Setup
Setup
 └─ DecksBuilt → TurnStart
TurnStart
 └─ HandDrawn → PlayerActions
PlayerActions
 ├─ CardPlayed → PlayerActions
 ├─ ShipBought → PlayerActions
 ├─ CombatAssigned → PlayerActions (forts before health)
 └─ TurnEnded → OpponentTurn
OpponentTurn
 └─ OpponentResolved → CheckEnd
CheckEnd
 ├─ OpponentHealthZero → Victory
 ├─ PlayerHealthZero → Defeat
 └─ Continue → TurnStart
Victory / Defeat
 └─ Rematch / Menu → Idle
```
- Pure controllers: `DeckEngine` (draw/discard/shuffle), `TradeRow` (refill/cost), and `CombatResolver` (fort-before-face targeting, synergy bonuses) — all unit-tested without Android imports.

## HUD
- Both players' health, the Trade Row, in-play forts with their HP, and current Trade/Combat pools.
- Whose-turn indicator and reference button.
- Victory/defeat panel slides up below the board per the common victory-defeat spec.
