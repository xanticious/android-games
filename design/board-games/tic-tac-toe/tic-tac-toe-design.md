# Tic Tac Toe — Design Document

## Overview
- Tic Tac Toe is a quick-play alignment game for one human versus a local AI opponent.
- The classic mode uses a 3×3 grid where players alternate marking X or O.
- The first player to complete a full row, column, or diagonal wins.
- If the board fills without a completed line, the game ends in a draw.
- Optional larger grids add longer challenges while preserving the same simple turn rhythm.
- Larger-grid modes use configurable board size and win length so difficulty can scale beyond solved 3×3 play.
- The human can choose X, O, or Random; X always takes the first turn.
- The design should feel instant, clean, and friendly while still supporting harder AI for larger grids.
- Rounds should complete in seconds on the classic grid and remain highly readable on phones.
- All match statistics, preferred side, grid settings, and difficulty settings remain local to the device.
- Victory, defeat, and draw presentation follows `design/common/victory-defeat.md`; result panels never cover the grid.

## Visual Style
- Use Material 3 surfaces and the underwater palette from `ui/theme/Color.kt`.
- The grid sits on a calm `Dark1` or `Aqua0` board plate with dividers using `Aqua3` or softened `Dark2` contrast.
- X marks use crisp angular strokes with `Aqua4` emphasis.
- O marks use rounded rings with `Aqua1` fill accents and `Aqua3` outlines.
- The selected or just-played cell receives a small `Aqua2` pulse.
- Winning lines glow with `Aqua3` but do not obscure the marks.
- Draw states use neutral Material 3 surface treatment with compact copy below the board.
- Larger grids reduce mark ornamentation and emphasize intersection clarity over decoration.
- Invalid tap feedback is a gentle shake or message strip, not an error overlay.
- Animations are short: mark draw-in, AI placement pop, winning-line pulse, and result panel reveal.
- Reduced-motion mode renders marks instantly and replaces pulses with static emphasis.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Tic Tac Toe        X to move   [⚙]  │  ← Top bar
├─────────────────────────────────────┤
│ You: X        AI: O        Easy      │  ← Match strip
├─────────────────────────────────────┤
│          ┌─────┬─────┬─────┐        │
│          │  X  │     │  O  │        │
│          ├─────┼─────┼─────┤        │  ← Board scales for 3×3, 4×4, 5×5
│          │     │  X  │     │        │
│          ├─────┼─────┼─────┤        │
│          │  O  │     │     │        │
│          └─────┴─────┴─────┘        │
├─────────────────────────────────────┤
│ Choose a square.                    │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait-first; tablets can place score history beside the board while keeping prompts and results below the grid.

## Settings
- **Grid size**: 3×3, 4×4, or 5×5.
- **Win length**: 3 on 3×3; 4 or 5 for larger grids where valid.
- **Player mark**: X, O, or Random.
- **Opponent difficulty**: Easy, Medium, Hard, Perfect for 3×3.
- **Opening player**: X first under standard rules; Random can assign the human mark before the match.
- **Show threats** (on/off, default off): highlights immediate winning or blocking squares in practice games.

## How to Play
- X moves first.
- Tap an empty square to place your mark.
- The AI places its mark after the human turn when the game is not over.
- Complete the required number of your marks in a row, column, or diagonal to win.
- Block the AI's lines while creating your own forks and threats.
- If every square fills before either side completes a line, the round is a draw.

## Controls
- Tap an empty cell to move.
- Tap the new-round button in the result panel to immediately replay with the same settings.
- Long-press a highlighted practice hint to explain the threat or block.
- Settings are available before play and from the top bar between completed rounds.

## AI Opponents
- **Easy**: chooses legal cells with light preference for center and corners.
- **Medium**: blocks immediate wins and takes immediate wins but can miss forks.
- **Hard**: creates and blocks forks, values center control, and searches larger-grid threats.
- **Perfect**: available for 3×3; never loses and forces a draw or win whenever mathematically possible.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses / draws per grid size and difficulty | yes |
| Current and best win streak | yes |
| Fastest win by moves | yes |
| Games played as X and O | yes |
| Practice hint usage | yes |

## State Machine
- A dedicated `TicTacToeStateMachine` in `state/` exposes `StateFlow<TicTacToeState>`.
```
Idle
 └─ MatchStarted → Playing
Playing
 ├─ CellSelected → ResolvingMove
 ├─ InvalidCellSelected → Playing
 └─ Surrendered → GameOver
ResolvingMove
 ├─ HumanWon → GameOver
 ├─ DrawReached → GameOver
 └─ AiTurnNeeded → AiThinking
AiThinking
 └─ AiMoveChosen → ResolvingAiMove
ResolvingAiMove
 ├─ AiWon → GameOver
 ├─ DrawReached → GameOver
 └─ HumanTurnReady → Playing
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `TicTacToeRules` controller validates legal moves, line detection, draw detection, board-size settings, and AI candidate evaluation; unit tests cover classic and larger-grid win conditions.

## HUD
- Top bar shows game name, active mark, and settings access.
- Match strip shows human mark, AI mark, difficulty, and grid profile.
- Bottom panel shows turn prompts, invalid-move explanations, and result actions below the board.
