# Zilch — Design Document

## Overview
- Zilch is a fast scoring dice game also known as Ten Thousand and closely related to Farkle-style rulesets.
- One human player competes against one to four local AI opponents.
- The goal is to finish with the highest score once someone reaches 10,000 points and the final round concludes.
- Matches should feel readable, snappy, and satisfying in short sessions.
- Every turn revolves around deciding whether to bank points or keep rolling for more.
- Unbanked turn points are always at risk.
- A roll with no scoring result is a zilch.
- Zilching immediately ends the turn and wipes all unbanked points from that turn.
- Players must set aside at least one valid scoring die or scoring combination after every successful roll.
- If all six dice score, the player earns hot dice and rolls all six again while keeping the running turn total.
- Standard win flow uses a final round after the first player crosses 10,000.
- Optional Keep Going Mode continues the match after a winner is declared to show long-tail pacing.
- Keep Going Mode does not change the official winner of the completed match.
- The design should support clear combination recognition without requiring players to memorize hidden scoring.
- The full experience stays local and offline, matching the app's no-social, no-store product scope.
- Victory and defeat presentation follows `design/common/victory-defeat.md`; results never cover the dice tray or score panels.

## Visual Style
- The table is presented as a polished tabletop tray inside a Material 3 card layout.
- Background surfaces use `Dark0` and `Dark1` to create a calm, high-contrast play area.
- Primary accents use `Aqua3` and `Aqua4` on buttons, chips, and score highlights.
- Selected scoring dice glow with `Aqua2` rings and soft elevation.
- Neutral dice use light surfaces derived from `Aqua0` with `Dark1` pips for strong readability.
- Warning states such as zilch risk and final-round urgency use theme error or tertiary treatment instead of custom hex colors.
- Score panels use stacked cards with underwater palette accents rather than casino styling.
- Dice animations are crisp and toy-like, with short bounces and subtle shadows.
- Locked dice slide upward into a set-aside lane instead of shrinking away.
- The current player row is emphasized with a strong container tint and an icon marker.
- Typography favors large score numbers and compact rule reminders.
- The overall tone is modern tabletop, not gritty gambling.
- All color references in implementation must come from `ui/theme/Color.kt` and Material 3 roles built from those tokens.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Zilch            Final: no     [⚙]  │  ← Top bar
├─────────────────────────────────────┤
│ You 4,850   Bot A 5,200   Bot B 3,900│  ← Score strip
├─────────────────────────────────────┤
│ Turn points: 650     Risk: Medium    │  ← Turn summary
│ Set aside: [⚀] [⚄⚄⚄]                │
│ Roll dice: [⚁] [⚂] [⚅]              │  ← Dice tray
│ [Bank 650]             [Roll 3 dice] │
├─────────────────────────────────────┤
│ Scoring help: three 5s = 500         │  ← Combination helper/result panel
└─────────────────────────────────────┘
```
- Portrait stacks score strip, turn summary, dice tray, and controls; tablets can keep opponent scorecards in a side column.

## Settings
- **AI opponents**: 1, 2, 3, or 4.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Target score**: 5,000, 10,000, or 20,000; default 10,000.
- **Opening bank minimum**: None, 500, or 1,000; default None unless a variant profile changes it.
- **Keep Going Mode** (on/off, default off).
- **Combination helper** (on/off, default on): shows recognized scoring dice and point values.
- **Fast AI turns** (on/off): summarizes AI rolls, banks, zilches, and hot dice.

## How to Play
- Roll six dice to start your turn.
- After every scoring roll, set aside at least one scoring die or scoring combination.
- Bank your turn points to add them safely to your total, or roll the remaining dice to risk them for more.
- If a roll has no scoring dice, you zilch and lose all unbanked points from that turn.
- If all six dice are set aside as scoring dice, you get hot dice and may roll all six again while keeping the current turn total.
- When a player reaches the target score, every other player receives one final turn.
- Highest score after the final round wins.

## Controls
- Tap dice to select a valid scoring die or combination.
- Tap Bank to end the turn and keep current turn points.
- Tap Roll to roll the remaining dice after selecting at least one scoring result.
- Long-press a combination in the helper panel to explain its score.
- During Keep Going Mode, use the below-board result panel to continue or return to the lobby.

## AI Opponents
- **Easy**: banks early, recognizes simple singles and triples, and rarely pushes hot dice.
- **Medium**: weighs current lead, remaining dice count, and common combination odds before banking.
- **Hard**: adapts risk to target distance, final-round pressure, opponent totals, and hot-dice opportunities.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per opponent count and difficulty | yes |
| Highest banked turn | yes |
| Zilches rolled | yes |
| Hot dice streaks | yes |
| Final-round comebacks | yes |
| Keep Going extra turns after official result | yes |

## State Machine
- A dedicated `ZilchStateMachine` in `state/` exposes `StateFlow<ZilchState>`.
```
Idle
 └─ MatchStarted → TurnStarted
TurnStarted
 └─ DiceRolled → SelectingScorers
SelectingScorers
 ├─ ScoringDiceSelected → DecisionPoint
 ├─ NoScorersRolled → Zilched
 └─ InvalidSelectionExplained → SelectingScorers
DecisionPoint
 ├─ Banked → ResolvingBank
 ├─ RolledAgain → SelectingScorers
 └─ HotDiceEarned → HotDice
HotDice
 └─ RolledAllDice → SelectingScorers
Zilched
 └─ TurnEnded → AiTurn / TurnStarted / FinalRoundCheck
ResolvingBank
 ├─ TargetReached → FinalRoundCheck
 └─ NextTurn → AiTurn / TurnStarted
AiTurn
 └─ AiTurnResolved → FinalRoundCheck / TurnStarted
FinalRoundCheck
 ├─ FinalRoundComplete → GameOver
 └─ NextFinalTurn → AiTurn / TurnStarted
GameOver
 └─ KeepGoing / Rematch / Menu → Idle
```
- A pure `ZilchRules` controller recognizes scoring combinations, validates set-aside selections, computes turn totals, detects zilches and hot dice, manages final-round eligibility, and separates official results from Keep Going results; all rules are unit-tested without Android imports.

## HUD
- Top bar shows target score, final-round state, and settings access.
- Score strip shows all players and highlights the current player.
- Turn summary shows banked score, unbanked turn points, selected scoring dice, and current risk label.
- Combination helper explains available scoring choices without covering dice.
- Result panel below the play area shows official winner, final scores, and Keep Going actions.
