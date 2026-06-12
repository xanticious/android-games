# Splendor — Design Document

## Overview
- Splendor is a fast engine-building board game adapted for offline single-player Android play.
- The app supports one human player against 1, 2, or 3 AI rivals.
- Standard rules are preserved: collect gem tokens, reserve cards, buy development cards, and attract nobles.
- The six token colors are white, blue, green, red, black, and gold as the wild reserve reward.
- The win threshold is 15 prestige points, with end-of-round resolution so all players in the round finish equally.
- The digital version emphasizes readability over table realism.
- Information density is high, so the layout must keep all major card markets visible without hiding the player's reserve.
- The design should feel premium, jewel-like, and calm rather than flashy.
- Material 3 surfaces, rounded cards, and the underwater palette establish the app-wide visual identity.
- All profile data, AI history, win rates, and settings remain local to the device.
- The human turn is always explicit and confirm-driven to prevent accidental gem collection or purchases.
- AI turns should resolve quickly with compact animations and visible card movement.
- Noble visits are automatic and never require a separate confirm step.
- All actionable game information must be accessible in portrait orientation first, with tablet layouts widening rather than reordering the logic.

## Visual Style
- Use Material 3 components with the underwater palette from `ui/theme/Color.kt`.
- Primary background surfaces use `Dark0` and `Dark1` in dark theme, and `Aqua0` for large light-theme surfaces.
- Interactive highlights use `Aqua2` for valid actions and `Aqua3` for confirmed selections.
- Key score and prestige accents use `Aqua4` so point-bearing elements stand out.
- Card frames use elevated Material cards with soft tonal contrast instead of faux wood or velvet textures.
- Gem token chips should be color-coded with labeled icons so color-blind players can read both hue and symbol.
- Reserved cards use a subtle banner treatment with `Aqua1` accents so they are distinct from open market cards.
- Noble tiles use a brighter elevated style and a prestige emblem to communicate rarity.
- Disabled actions dim with lower-emphasis typography rather than disappearing.
- The bank area should look orderly, using evenly spaced circular token wells.
- Motion is restrained: tap pulses, card lifts, token slides, and short score count-ups.
- All animations respect reduced-motion settings by switching to instant transitions.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Splendor      4 players   Round 6 ⚙ │  ← Top bar
├─────────────────────────────────────┤
│ Bank: Wh4 Bl3 Gr5 Rd2 Bk4 Gold1     │  ← Gem bank
├─────────────────────────────────────┤
│ Nobles: [3Wh3Bl3Gr] [4Rd4Bk] [mix]  │
│ Tier III: [card] [card] [card] [card]│
│ Tier II : [card] [card] [card] [card]│  ← Market anchor
│ Tier I  : [card] [card] [card] [card]│
├─────────────────────────────────────┤
│ AI A 8pt  AI B 6pt  AI C 5pt        │  ← Opponent strip
├─────────────────────────────────────┤
│ You: 7pt  Gems 7/10  Reserve 2/3     │  ← Player engine tray
│ [Take gems] [Reserve] [Buy] [Undo]   │
│ Confirm your action.                 │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait keeps the gem bank, nobles, three market tiers, opponent strip, and player tray in one readable vertical flow.
- The market is the primary anchor; the player tray remains fixed near the bottom when scrolling is necessary.
- Tablet layouts widen the market rows and move opponent details into side panels without changing turn logic.
- Action prompts, noble visits, AI summaries, end-of-round notices, and results appear below the market or in the player tray area and never cover cards, gems, or nobles.

## Settings
- **AI opponents**: 1, 2, or 3 (default 3).
- **Opponent difficulty**: Easy, Medium, Hard.
- **Player order**: First, Last, or Random.
- **Card text density**: Compact or Expanded.
- **Show affordability hints** (on/off, default on): marks cards the player can buy now or after selected gems.
- **Confirm actions** (on/off, default on): requires explicit confirmation for gem takes, reserves, and purchases.
- **Fast AI turns** (on/off, default on): summarizes token movement, reserves, buys, and noble visits.
- **Reduced motion** follows the system setting and switches token and card movement to instant transitions.

## How to Play
- On your turn, take gem tokens, reserve one development card, or buy one development card.
- Take either three different gem colors or two of the same color when at least four tokens of that color are available before taking.
- Reserve a visible card or the top card of a deck to gain one gold token if any gold remains.
- Buy cards by paying their gem cost after applying discounts from your purchased development cards.
- Purchased cards add permanent discounts and may add prestige points.
- Nobles visit automatically when your development cards meet their requirements.
- When any player reaches 15 prestige points, finish the current round so all players have had the same number of turns.
- Highest prestige wins; ties use standard Splendor tie handling based on fewer purchased development cards, then shared result if still tied.

## Controls
- Tap gem tokens in the bank to stage a take-gems action; invalid combinations are disabled or explained in the prompt panel.
- Tap a market card to inspect cost, discount, prestige, buy, and reserve actions.
- Tap a deck back to reserve a blind card when reserve space is available.
- Tap a reserved card in the player tray to inspect or buy it.
- Tap **Undo** to clear the staged action before confirmation.
- Tap **Confirm** to commit the selected legal action.
- Tap opponent summaries to expand compact local details such as score, gems, reserves count, and visible purchased discounts.

## AI Opponents
- **Easy**: buys affordable point cards, takes obvious gems, and reserves occasionally, but misses efficient engine planning.
- **Medium**: balances discounts, prestige, noble progress, reserve denial, token limits, and short-term affordability.
- **Hard**: evaluates race timing, opponent threats, noble tempo, scarce tokens, reserve tactics, end-round tie breakers, and multi-turn engine value.
- AI difficulty changes decision quality only; token counts, market refill, noble visits, and end-of-round resolution remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per opponent count and difficulty | yes |
| Results by player order | yes |
| Highest prestige total | yes |
| Wins by noble visit | yes |
| Cards purchased per game | yes |
| Games decided by tie breaker | yes |

## State Machine
- A dedicated `SplendorStateMachine` in `state/` exposes `StateFlow<SplendorState>`.
```
Idle
 └─ MatchStarted → SettingUpMarket
SettingUpMarket
 └─ MarketReady → ChoosingOrder
ChoosingOrder
 └─ OrderChosen → PlayerTurn / AiThinking
PlayerTurn
 ├─ GemsSelected → BuildingAction
 ├─ CardSelected → BuildingAction
 ├─ ReservedCardSelected → BuildingAction
 ├─ ActionCleared → PlayerTurn
 └─ Surrendered → GameOver
BuildingAction
 ├─ ActionUpdated → BuildingAction
 ├─ ActionConfirmed → ValidatingAction
 └─ ActionCancelled → PlayerTurn
ValidatingAction
 ├─ ActionAccepted → ResolvingAction
 └─ ActionRejected → PlayerTurn
ResolvingAction
 ├─ NobleVisited → ResolvingAction
 └─ ActionResolved → CheckingRound
CheckingRound
 ├─ ThresholdReached → FinalRound
 ├─ TurnAdvanced → PlayerTurn / AiThinking
 └─ MarketRefilled → PlayerTurn / AiThinking
FinalRound
 ├─ FinalTurnAdvanced → PlayerTurn / AiThinking
 └─ FinalRoundComplete → GameOver
AiThinking
 └─ AiActionChosen → ValidatingAction
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `SplendorRules` controller validates gem-taking rules, reserve limits, gold awards, card affordability, purchases, market refill, noble visits, prestige threshold, final-round completion, tie breakers, and AI evaluation helpers; unit tests cover rules without Android imports.

## HUD
- Top bar shows game name, player count, round state, active player, and settings access.
- Bank area shows available token counts, selected tokens, and token-limit warnings.
- Market annotations show affordability, reserved-card availability, prestige, and noble requirements.
- Opponent strip shows scores, gem counts, reserve counts, discounts, and recent AI action summaries.
- Player tray shows prestige, discounts, gems, reserved cards, staged action, confirm state, and prompt/result text.
- Victory/defeat/draw presentation follows `design/common/victory-defeat.md`: results never cover the market, bank, nobles, or player tray.
