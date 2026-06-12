# Reversi — Design Document

## Overview
- Reversi is a standard 8×8 abstract strategy game for one human versus one AI opponent.
- Players alternate placing discs to outflank and flip opposing discs in horizontal, vertical, and diagonal directions.
- A legal move must bracket at least one contiguous line of opponent discs between the newly placed disc and another friendly disc.
- If the active player has no valid move, the turn is skipped automatically.
- The game ends when the board is full or neither player can move.
- The winner is the side with more discs on the board at the end.
- Black always moves first under standard rules.
- The digital version emphasizes clean hints, training support, and strong move-legibility without clutter.
- The feel should be contemplative and sharp, with immediate feedback on flips and turn control.

## Visual Style
- Use Material 3 with the underwater palette from `ui/theme/Color.kt`.
- Overall background uses `Dark0`; board surround and HUD cards use `Dark1` and `Dark2`.
- The 8×8 board uses an `Aqua0`-influenced playable surface with clear cell borders.
- Valid move hints, when enabled, use subtle `Aqua2` dots centered in legal cells.
- Selected move previews and last-move emphasis use `Aqua3`.
- Invalid or blocked training-wheel moves grey down via a muted overlay derived from `Dark2` rather than a bright error splash.
- Disc art is glossy but restrained so counts and flank lines remain readable.
- Flip animations are crisp quarter-turn rotations with very short timing.
- Typography uses Material 3 hierarchy with large numeric counts and compact instructional copy.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Reversi        Black first      [⚙] │  ← Top bar
├─────────────────────────────────────┤
│ You ● 18        AI ○ 14   Turn: You │  ← Status strip
├─────────────────────────────────────┤
│   A B C D E F G H                   │
│  ┌───────────────────────────────┐   │
│1 │ · · · · · · · ·               │
│2 │ · · · · · · · ·               │
│3 │ · · ◌ · · · · ·               │  ← Board anchor
│4 │ · · · ● ○ · · ·               │
│5 │ · · · ○ ● · · ·               │
│6 │ · · · · ◌ · · ·               │
│  └───────────────────────────────┘   │
├─────────────────────────────────────┤
│ Hint: D3 flips 1 disc diagonally.    │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait keeps the 8×8 board square and centered with counts and turn state above it.
- Tablet layouts may add a side panel for move history, training explanations, and projected flip counts while preserving the same board orientation.
- Legal move hints, last move, flip previews, pass notices, and results appear as board annotations plus text below the board.
- Victory, defeat, and draw panels appear below the board and never cover discs or final counts.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard.
- **Player color**: Black, White, or Random; Black always takes the first move.
- **Move hints** (on/off, default on): marks legal moves with subtle dots.
- **Training mode** (on/off, default off): explains why invalid cells are illegal and previews flip lines before confirmation.
- **Show flip counts** (on/off, default on): displays how many discs each legal move would flip.
- **Confirm move** (on/off, default off): when on, selected moves preview before final placement.
- **Fast AI turns** (on/off, default on): keeps AI placement and flips short while still visible.

## How to Play
- Black moves first.
- On your turn, place one disc on an empty square that brackets at least one straight line of opposing discs.
- Bracketing means the placed disc and an existing friendly disc enclose one or more contiguous opposing discs horizontally, vertically, or diagonally.
- All bracketed opposing discs flip to your color.
- If you have no legal move, your turn is skipped automatically.
- The game ends when the board is full or neither side can move.
- The side with more discs wins; equal counts produce a draw.

## Controls
- Tap a legal hint or empty cell to place a disc.
- When confirm move is enabled, tap once to preview flip lines and tap **Confirm** below the board to commit.
- Tap an illegal cell in training mode to show the missing bracket reason in the prompt panel.
- Tap the hint panel to cycle through legal move explanations when training mode is enabled.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: takes legal moves with simple flip-count preference and may give away corners or stable edges.
- **Medium**: values corners, avoids obvious bad squares, considers mobility, and balances immediate flips against position.
- **Hard**: evaluates stability, parity, frontier discs, mobility, corner access, pass pressure, and late-game exact counts.
- AI difficulty changes decision quality only; legal move generation, automatic passes, and scoring remain deterministic.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses / draws per difficulty | yes |
| Results as Black / White | yes |
| Final disc margin | yes |
| Corners captured | yes |
| Forced passes caused | yes |
| Training hints viewed | yes |

## State Machine
- A dedicated `ReversiStateMachine` in `state/` exposes `StateFlow<ReversiState>`.
```
Idle
 └─ MatchStarted → ChoosingColor
ChoosingColor
 └─ ColorChosen → PlayerTurn / AiThinking
PlayerTurn
 ├─ MovePreviewed → PlayerTurn
 ├─ CellChosen → ValidatingMove
 ├─ NoLegalMove → PassingTurn
 └─ Surrendered → GameOver
ValidatingMove
 ├─ MoveAccepted → FlippingDiscs
 └─ MoveRejected → PlayerTurn
FlippingDiscs
 └─ FlipsCompleted → CheckingResult
PassingTurn
 └─ TurnPassed → CheckingResult
CheckingResult
 ├─ GameFinished → GameOver
 └─ TurnAdvanced → PlayerTurn / AiThinking
AiThinking
 ├─ AiMoveChosen → ValidatingMove
 └─ AiNoLegalMove → PassingTurn
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `ReversiRules` controller validates legal moves in eight directions, flip resolution, automatic passes, board-full and no-move end states, score counts, hint data, and AI evaluation helpers; unit tests cover all rule behavior without Android imports.

## HUD
- Top bar shows game name, player color, Black-first rule, and settings access.
- Status strip shows disc counts, active turn, difficulty, move number, and last move.
- Board annotations show legal hints, previewed flip lines, last move, and stable result highlights when relevant.
- Prompt/result panel explains turn prompts, passes, invalid moves, final disc counts, and rematch actions below the board.
- Victory/defeat/draw presentation follows `design/common/victory-defeat.md`: results never cover the board.
