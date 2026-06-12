# Yahtzee — Design Document

## Overview
- Yahtzee is a turn-based dice game in which players try to maximize score across 13 categories.
- The app supports solo score attacks and optional matches against 1, 2, or 3 local AI opponents.
- Each turn allows up to three rolls of five dice.
- Between rolls, the player may lock or unlock any individual die.
- At the end of the turn, one score category must be filled, even if it scores zero.
- Standard upper-section categories are Ones, Twos, Threes, Fours, Fives, and Sixes.
- Lower-section categories are Three-of-a-Kind, Four-of-a-Kind, Full House, Small Straight, Large Straight, Chance, and Yahtzee.
- Upper bonus awards 35 points when the upper subtotal reaches at least 63.
- Additional Yahtzees after the Yahtzee box is already filled award 100-point bonuses and act as wilds for full house and straight interpretation.
- The digital version should feel tactile, readable, and fast, with the scorecard always in view.
- Strategy support comes from score previews, optional AI difficulty, and clear turn-state communication rather than forced guidance.
- All statistics and preferences remain local to the device.
- Victory and defeat presentation follows `design/common/victory-defeat.md`; results appear below the dice and scorecard area.

## Visual Style
- Use Material 3 surfaces with an underwater-casino feel built from the app palette.
- Table background surfaces use `Dark0` and `Dark1` in dark mode, while light mode leans on `Aqua0` with elevated cards.
- Dice bodies use high-contrast surfaces with pips tinted from `Dark2` or `Aqua4` for clarity.
- Locked dice gain an `Aqua2` border and a subtle filled chip underneath.
- Roll and confirm emphasis buttons use `Aqua3`, while score previews use lower-emphasis typography tinted toward `Aqua1`.
- Completed score rows use standard text contrast; suggested high-value rows may receive a faint `Aqua2` background wash.
- The scorecard should resemble a clean ledger, not paper scraps or casino felt.
- Player columns must remain legible on both phones and tablets.
- Pointer hover states on large screens can elevate category rows, but touch interactions remain primary.
- Animations are light: dice bounce, lock snap, row highlight, and total count-up.
- Reduced-motion mode converts rolls to quick face swaps with no bounce.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Yahtzee          Turn 7/13     [⚙]  │  ← Top bar
├─────────────────────────────────────┤
│ You 148       Bot A 132       Medium│  ← Player score strip
├─────────────────────────────────────┤
│        [⚂] [⚄] [⚄] [⚅] [⚁]          │  ← Dice tray
│        Lock  Lock   ·    ·   Lock    │
│              [Roll 2/3]              │
├─────────────────────────────────────┤
│ Category          You   Bot A Preview│
│ Ones               3     4      0    │
│ Twos               -     6      2    │  ← Scrollable scorecard
│ Full House        25     -     25    │
│ Yahtzee            -     0     50    │
├─────────────────────────────────────┤
│ Choose a category or roll again.     │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait keeps dice above a vertically scrollable scorecard; tablets can show all player columns without horizontal scrolling.

## Settings
- **AI opponents**: 0, 1, 2, or 3.
- **Opponent difficulty**: Easy, Medium, Hard; hidden when AI opponents is 0.
- **Score previews** (on/off, default on): show the score each open category would receive from current dice.
- **Yahtzee bonus rules**: Standard bonus and wild behavior enabled by default.
- **Fast AI turns** (on/off): summarize AI rolls and selected categories quickly.
- **Practice undo** (on/off, default off): allows undoing category selection only in non-stat practice games.

## How to Play
- Roll five dice up to three times on your turn.
- After each roll, lock dice you want to keep and roll the rest.
- You may score after any roll, but must score after the third roll.
- Choose one unfilled category; it becomes locked for the rest of the game.
- Upper categories score the sum of matching dice; reach 63 upper points for the bonus.
- Lower categories score combinations such as full house, straights, chance, and Yahtzee.
- After all 13 categories are filled, the highest total wins; solo mode records the final score as a score attack.

## Controls
- Tap a die to lock or unlock it between rolls.
- Tap Roll to roll all unlocked dice.
- Tap a scorecard row to preview details; tap Confirm Score to fill that category.
- Long-press a category name to show its rule.
- In AI matches, AI turns auto-play with a short summary before returning control.

## AI Opponents
- **Easy**: chases obvious high face totals and simple combinations.
- **Medium**: compares expected category values, protects the upper bonus, and avoids wasting Yahtzee too early.
- **Hard**: uses probability-aware roll selection, endgame category planning, and opponent score pressure when deciding risk.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Solo games played and best score | yes |
| Wins / losses per AI count and difficulty | yes |
| Average final score | yes |
| Yahtzees rolled and Yahtzee bonuses earned | yes |
| Upper bonuses achieved | yes |

## State Machine
- A dedicated `YahtzeeStateMachine` in `state/` exposes `StateFlow<YahtzeeState>`.
```
Idle
 └─ MatchStarted → TurnStarted
TurnStarted
 └─ DiceRolled → Rolling
Rolling
 ├─ DiceLocked → Rolling
 ├─ DiceRolled → Rolling
 ├─ CategorySelected → ScoringCategory
 └─ RollLimitReached → MustScore
MustScore
 └─ CategorySelected → ScoringCategory
ScoringCategory
 ├─ ScoreAppliedAndGameComplete → GameOver
 ├─ NextHumanTurn → TurnStarted
 └─ NextAiTurn → AiThinking
AiThinking
 └─ AiScoreChosen → ScoringCategory
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `YahtzeeRules` controller calculates category scores, upper bonus, Yahtzee bonuses, wild handling, legal scoring categories, and final totals; probability helpers for AI live in controllers and are unit-tested without Android imports.

## HUD
- Top bar shows game name, turn count, and settings access.
- Score strip shows current totals for the human and any AI opponents.
- Dice tray shows roll count, lock state, and roll/score actions.
- Scorecard shows filled values, previews for open rows, subtotal, bonus, and total.
- Result panel below the play area shows solo score grade or match ranking.
