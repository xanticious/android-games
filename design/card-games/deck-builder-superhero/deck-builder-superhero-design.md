# Deck Builder Superhero — Design Document

## Overview
- Deck Builder Superhero is a cooperative deck-building card game for one human plus 0–3 AI teammates, adapted for offline Android play.
- Inspired by the structure of cooperative hero deck-builders: the team works together to defeat a Super Villain (or a sequence of them) before its scheme completes.
- Uses entirely original, royalty-free heroes, villains, and allies — no Marvel, DC, or other third-party properties.
- Each player chooses a favorite hero (or random); that hero defines their starting deck's flavor and a unique ability. Bots are assigned distinct starter decks from the remaining heroes.
- The shared "Recruit Row" market and the Villain track are the visual anchors; victory/defeat status sits below the board, never over it.
- All match results, villains defeated, and high scores stay local to the device.
- Turns resolve quickly; AI teammates play visibly so the human can follow the cooperative plan.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Comic-flat art direction: bold flat fills, clean panel borders, no gradients-as-texture.
- Background surfaces use `Dark0` / `Dark1` (dark) and `Aqua0` (light); the Recruit Row uses elevated Material cards.
- Recruitable allies highlight `Aqua2` when affordable; the active Super Villain uses an ominous `Aqua4`/`Dark2` frame with a visible health/scheme meter.
- Each hero has a signature accent drawn from the palette so teammates are visually distinct.
- Card plays animate as quick slides and pops; defeating a villain triggers a celebratory burst below the board. Respects reduce-motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Deck Builder Superhero  [⚙][❔]       │  ← Top bar: settings, reference
├─────────────────────────────────────┤
│  SUPER VILLAIN: The Hollow  HP 18    │
│  Scheme ▓▓▓▓░░░  (3 turns left)      │  ← Villain + scheme track
├─────────────────────────────────────┤
│  Recruit Row: [Ally][Ally][Ally][Ally][Ally]
├─────────────────────────────────────┤
│  Teammates: Tigerman(AI) Echo(AI)    │  ← Bot status (decks/HP)
├─────────────────────────────────────┤
│  You: Monk   ⚡Power 4  🗡Attack 3     │
│  Hand: [card][card][card][card][card]│
│  [Play all] [Buy] [Attack Villain]   │
└─────────────────────────────────────┘
```
- Portrait-first; tablet widens the Recruit Row and teammate panel.

## Settings
- **Number of bot teammates**: 0, 1, 2, or 3 (0 = solo co-op against the villain).
- **Hero select**: choose your hero from the roster, or Random.
- **Difficulty**: Easy, Medium, Hard — scales villain HP, scheme speed, and bot competence.
- **Villain count**: single villain or a sequence (1–3) for a longer campaign run.
- **Friendly fire off** (always) — this is purely cooperative; there is no PvP.

## Heroes (original roster)
Each hero starts with a distinct 10-card deck and one signature ability:
- **Monk** — *simplify your life*: cards focus on culling/trashing weak cards from your deck and gaining efficiency; ability trashes a card to draw.
- **Tigerman** — *raw power*: cards deal heavy attack damage; ability adds burst attack but costs card draw.
- **Echo** — *swarm*: cheap cards that chain extra draws and plays; ability replays the last small card.
- **Bling** — *income*: cards generate power/currency to buy expensive allies fast; ability converts unspent power into a discount.
- **Aegis** — *defense*: cards block villain attacks and protect teammates; ability shields a chosen teammate.
- **Volt** — *tempo*: cards give bonus actions when played in sequence; ability untaps/replays a played card.
Heroes are balanced around the same starting power curve; abilities define their identity, not raw stats.

## How to Play
- On your turn: draw a hand (5 cards by default), play cards to generate Power (to buy) and Attack (to fight).
- Spend Power to recruit allies from the Recruit Row into your discard pile (they shuffle into your deck later).
- Spend Attack to damage the Super Villain or clear its minions/threats.
- The Villain advances its Scheme each round; if the scheme track fills before the villain is defeated, the team loses.
- Defeat the villain (reduce HP to zero) — or all villains in a sequence — to win cooperatively.

## Controls
- Tap a hand card to play it; tap a Recruit Row ally to buy it (greyed if unaffordable).
- Tap the Villain to allocate Attack; abilities are a tappable hero button with clear prompts (non-overlay choosers).
- The reference (❔) opens a non-modal card/keyword glossary.

## AI Teammates
- Bots share the cooperative goal: they buy allies that complement the team, focus Attack to finish threats, and use abilities to protect low-HP teammates.
- **Easy**: plays cards greedily, minimal coordination.
- **Medium**: prioritizes scheme threats and efficient buys.
- **Hard**: sequences plays for maximum team output and defends proactively.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Villains defeated per difficulty | yes |
| Campaign runs completed (sequences) | yes |
| Fastest victory (fewest rounds) | yes |
| Most-used hero / win rate per hero | yes |

## State Machine
- A dedicated `SuperheroDeckStateMachine` in `state/` exposes `StateFlow<SuperheroDeckState>`.
```
Idle
 └─ GameStarted → HeroSelect
HeroSelect
 └─ HeroesAssigned → Setup
Setup
 └─ DecksBuilt → TurnStart
TurnStart
 └─ HandDrawn → PlayerActions
PlayerActions
 ├─ CardPlayed → PlayerActions
 ├─ AllyRecruited → PlayerActions
 ├─ VillainAttacked → PlayerActions
 └─ TurnEnded → VillainPhase
VillainPhase
 ├─ SchemeAdvanced → CheckEnd
 └─ MinionsActed → CheckEnd
CheckEnd
 ├─ VillainDefeated → (NextVillain → Setup | Victory)
 ├─ SchemeCompleted → Defeat
 └─ Continue → TurnStart (next player)
Victory / Defeat
 └─ Replay / Menu → Idle
```
- Pure controllers: `DeckEngine` (draw/discard/shuffle/trash), `RecruitMarket` (refill/cost), and `VillainController` (scheme/attacks) — all unit-tested without Android imports.

## HUD
- Villain HP and scheme track, the Recruit Row, current Power/Attack pools, and teammate status.
- Whose-turn indicator and reference button.
- Victory/defeat panel slides up below the board per the common victory-defeat spec.
