# Ludo — Design Document

## Overview
- Ludo is a classic race board game for one human player against one, two, or three AI opponents.
- The board uses the familiar four-corner layout with four home yards, a shared clockwise outer track, and a colored home column for each side.
- Each side owns four tokens and attempts to move all four from home, around the board, and into the final home stretch.
- A roll of 6 allows a token to enter play from home if its start square is available.
- A roll of 6 also grants a bonus roll after resolving the selected move.
- Tokens must move the exact remaining count to finish into the final home slot.
- Landing on an opposing token sends that token back to its home yard unless the destination is protected.
- Optional Safe Squares designate star spaces that protect tokens from capture.
- If Safe Squares are disabled, any shared outer-track square can become a capture square.
- One human plays against deterministic local AI opponents; all player names, results, and stats remain local.
- The experience should feel breezy and readable for casual players while preserving the tension of blocking, racing, and timing.
- The app ships Ludo as a fully guided digital board game with clear move hints, deterministic turn order, and no hidden rules.

## Visual Style
- Use Material 3 surfaces with the underwater palette from `ui/theme/Color.kt`.
- Main background uses `Dark0` so the board reads as a bright object on a calm deep-water backdrop.
- Board frame and outer chrome use `Dark1` and `Dark2` to separate the play field from HUD regions.
- Primary interactive emphasis uses `Aqua3`; secondary highlights use `Aqua2`; soft selection halos use `Aqua1`.
- Neutral light board surfaces use `Aqua0` to keep tracks legible in both light and dark system themes.
- Because the game requires four player identities, each token set keeps its classic red, green, blue, or yellow identity through piece art and labels, while selection, HUD, and feedback still use theme tokens.
- Home yards are large rounded quadrants with strong shape boundaries, not flat saturated blocks.
- Safe squares use a star badge and an `Aqua2` ring so their rule meaning is obvious even if token colors vary.
- Move indicators are circular pips placed on candidate destination cells using `Aqua3` with a subtle `Aqua0` outline.
- The die appears as a chunky Material card with a raised face and bold pips.
- Motion is snappy and short; token travel animates cell by cell with a clean easing curve so captures and home entry are easy to follow.
- All implementation colors come from `ui/theme/Color.kt` or Material 3 roles built from those tokens.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Ludo          4 players  Safe on ⚙  │  ← Top bar
├─────────────────────────────────────┤
│ You 1/4 home   AI A 0   AI B 2      │  ← Score strip
├─────────────────────────────────────┤
│ ┌───────────────┬───────────────┐   │
│ │ AI yard       │ AI yard       │   │
│ │   ● ●         │         ● ●   │   │
│ ├─────── track / home columns ──┤   │  ← Board anchor
│ │   ● ●         │         ● ●   │   │
│ │ You yard      │ AI yard       │   │
│ └───────────────┴───────────────┘   │
├─────────────────────────────────────┤
│ Die: ⚅   Choose a token to move.     │  ← Prompt/result panel
│ [Roll] [Move highlighted token]      │
└─────────────────────────────────────┘
```
- Portrait keeps the square board centered, with score summary above and die controls below.
- Tablets may add a side panel for full turn order and captured-token history without shrinking the board.
- Candidate moves and captures are shown on the board, but prompts and results stay below it.
- Victory/defeat panels never cover home columns, yards, or final token positions.

## Settings
- **AI opponents**: 1, 2, or 3.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Safe Squares**: on/off (default on).
- **Turn order**: clockwise with Human first, Random seat, or Random first player.
- **Show legal moves** (on/off, default on): highlights tokens that can use the current roll.
- **Fast AI turns** (on/off, default on): summarizes AI rolls and moves while preserving captures and finishes.
- **Auto-roll when only action** (on/off, default on): rolls automatically when the human has no decision before rolling.

## How to Play
- Roll the die when your turn begins.
- Roll a 6 to enter one token from your home yard onto your start square if available.
- A token already on the track moves clockwise by the exact die value.
- Roll a 6 to receive one bonus roll after the selected move resolves.
- Land on an opposing token to send it back to its home yard unless the square is safe.
- Enter your colored home column after completing the outer track.
- Tokens must use an exact roll to reach the final home slot.
- If no token can legally use the roll, your turn passes unless a bonus roll is still pending.
- Win by moving all four of your tokens into the final home area before every AI opponent.

## Controls
- Tap **Roll** to roll the die when prompted.
- Tap a highlighted token to preview its destination.
- Tap the destination or selected token again to confirm the move.
- Tap a safe-square star to show its protection rule.
- During AI turns, tap the summary card to expand the sequence of rolls, captures, and bonus rolls.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: enters tokens on sixes and moves the furthest token, but misses captures, safety, and exact-finish planning.
- **Medium**: prioritizes captures, safe squares, home-column progress, and spreading tokens to reduce risk.
- **Hard**: evaluates race position, capture threats, bonus-roll chains, blocking starts, safe-square timing, and endgame exact rolls.
- AI difficulty changes decision quality only; die results, legal moves, captures, safe squares, and bonus rolls remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per opponent count and difficulty | yes |
| Results with Safe Squares on / off | yes |
| Average turns to finish | yes |
| Captures made / suffered | yes |
| Bonus rolls earned from sixes | yes |
| Tokens finished by exact roll | yes |

## State Machine
- A dedicated `LudoStateMachine` in `state/` exposes `StateFlow<LudoState>`.
```
Idle
 └─ MatchStarted → SettingTurnOrder
SettingTurnOrder
 └─ TurnOrderReady → AwaitingRoll / AiTurn
AwaitingRoll
 └─ DieRolled → SelectingToken
SelectingToken
 ├─ TokenSelected → PreviewingMove
 ├─ NoLegalMove → EndingTurn
 └─ Surrendered → GameOver
PreviewingMove
 ├─ MoveConfirmed → MovingToken
 └─ SelectionCleared → SelectingToken
MovingToken
 └─ TokenArrived → ResolvingSquare
ResolvingSquare
 ├─ TokenCaptured → CheckingResult
 ├─ TokenFinished → CheckingResult
 └─ MoveResolved → CheckingResult
CheckingResult
 ├─ PlayerFinishedAllTokens → GameOver
 ├─ BonusRollGranted → AwaitingRoll / AiTurn
 └─ TurnAdvanced → AwaitingRoll / AiTurn
AiTurn
 └─ AiTurnResolved → CheckingResult
EndingTurn
 └─ TurnPassed → AwaitingRoll / AiTurn
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `LudoRules` controller validates dice use, entering on six, bonus rolls, legal movement, exact finish, captures, safe squares, turn order, and win detection; unit tests cover one to three AI opponents without Android imports.

## HUD
- Top bar shows player count, safe-square setting, difficulty, and settings access.
- Score strip shows each side's finished-token count and highlights the active player.
- Die card shows current roll, bonus-roll state, and whether a roll has legal moves.
- Board annotations show movable tokens, destinations, safe squares, captures, and final home progress.
- Prompt/result panel explains no-move turns, captures, bonus rolls, wins, and rematch actions below the board.
- Victory/defeat presentation follows `design/common/victory-defeat.md`: results never cover the board.
