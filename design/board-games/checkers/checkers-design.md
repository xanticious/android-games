# Checkers — Design Document

## Overview
Checkers is a clean tactical duel built around tempo, forced captures, promotion timing, and board control.
This implementation follows an American Checkers presentation on an 8x8 board with dark-square movement only.
It is a single-player game against AI with strong training support for learning better moves.
The tone is focused and modern, presenting classic wooden-board clarity through the app's underwater Material 3 aesthetic.
All gameplay interactions are tap-first and highly legible.
Forced jumps are mandatory.
If a capture sequence continues, the same piece must keep jumping until the sequence ends.
Per product rules, kings move multiple squares diagonally in any direction, creating higher mobility and clearer endgame swing potential.
The game is ideal for short repeatable matches with local stat tracking by difficulty and training mode usage.

## Visual Style
The board sits on a Dark0 background with a framed tabletop panel in Dark1.
Dark squares use Dark2-based values with slight texture.
Light squares use Aqua0-tinted surfaces so the board still feels bright and readable.
Red and Black are the player-facing color names for rule clarity, but piece rendering is palette-aware with rich red and near-black discs accented by Aqua edge light.
Selected pieces use an Aqua4 ring and a soft Aqua1 glow.
Legal move destinations use bright Aqua3 dots.
Capture destinations add a stronger pulse so mandatory jumps are unmistakable.
Kings gain a prominent crown badge embedded into the piece top.
When training wheels blocks a move, the rejected destination gives a gentle shake and a brief Aqua1 message strip below the board.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Checkers      You: Red       [⚙]    │  ← Top bar
├─────────────────────────────────────┤
│ Captured  Red 4      Black 2        │  ← Score strip
├─────────────────────────────────────┤
│      a b c d e f g h                │
│   8  ■ □ ■ □ ■ □ ■ □                │
│   7  □ ● □ ● □ ● □ ●                │
│   6  ■ □ ■ □ ■ □ ■ □                │  ← Board anchor
│   5  □ · □ · □ · □ ·                │
│   4  · □ · □ · □ · □                │
│   3  □ ○ □ ○ □ ○ □ ○                │
│   2  ○ □ ○ □ ○ □ ○ □                │
│   1  □ ■ □ ■ □ ■ □ ■                │
├─────────────────────────────────────┤
│ Turn: Red   Forced jump available   │
│ [Undo selection] [Training hint]     │
└─────────────────────────────────────┘
```
Portrait-first: the 8x8 board remains square and centered, with captured counts above and action messaging below. Tablet layouts keep the board square while moving history, hints, and captured-piece detail into side panels.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard (default Medium).
- **Training Wheels** (on/off, default on): highlights legal moves, warns when a forced jump is available, and blocks illegal non-captures during mandatory jumps.
- **Show coordinates** (on/off, default on): labels rows and columns around the board.
- **Move confirmation** (on/off, default off): requires a confirm tap after selecting a legal destination.
- **Player color**: Red (default) or Black.
- Settings are shown on the per-game Settings screen before play begins.

## How to Play
Move diagonally on dark squares and capture by jumping over opposing pieces. Forced jumps are mandatory; if a jump sequence can continue after a capture, the same piece must keep jumping until no capture remains.

Regular pieces move forward diagonally and promote when they reach the far row. Kings move multiple squares diagonally in any direction, and can capture over an opposing piece when the landing square beyond it is legal. The game ends when one side has no pieces or no legal moves.

## Controls
- Tap one of your pieces to select it; legal destinations appear as Aqua3 dots and capture destinations pulse more strongly.
- Tap a highlighted destination to move or jump. During multi-jump sequences, only the continuing capture piece remains selectable.
- With Training Wheels on, illegal moves are non-interactive or gently rejected with the message strip below the board.
- Tap **Undo selection** to clear the current piece before moving; completed turns are not rewound unless a later implementation adds full undo as a setting.
- Tap **Training hint** to highlight the highest-priority legal capture or safe move when Training Wheels is enabled.

## AI Opponents
- **Easy**: takes required jumps, otherwise favors simple forward moves and may miss king threats.
- **Medium**: prioritizes captures, promotion races, piece safety, and basic king mobility.
- **Hard**: searches multi-jump tactics, sacrifices, promotion timing, tempo, and long-range king control.
- AI difficulty changes decision quality only; forced jumps, multi-jump continuation, promotion, and multi-square king movement never change.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Wins / losses with Training Wheels on | yes |
| Captures made per game | yes |
| Kings promoted per game | yes |
| Longest forced jump sequence | yes |
| Games won with no kings lost | yes |

## State Machine
A dedicated `CheckersStateMachine` in `state/` exposes `StateFlow<CheckersState>` and owns selection, mandatory jumps, multi-jump continuation, AI turns, promotion, and game-over transitions.
```
Idle
 └─ MatchStarted → AwaitingSelection
AwaitingSelection
 ├─ PieceSelected → ChoosingDestination
 └─ AiTurnStarted → AiThinking
ChoosingDestination
 ├─ MoveRejected → AwaitingSelection
 ├─ MovePlayed → ResolvingMove
 └─ SelectionCleared → AwaitingSelection
ResolvingMove
 ├─ JumpRequired → ChoosingDestination
 ├─ PiecePromoted → TurnEnding
 ├─ TurnComplete → TurnEnding
 └─ GameFinished → GameOver
AiThinking
 └─ AiMoveChosen → ResolvingMove
TurnEnding
 └─ TurnPassed → AwaitingSelection
GameOver
 └─ Rematch / Menu → Idle
```
A pure `CheckersRules` controller in `controller/` has no Android imports and is unit-tested for legal movement, mandatory captures, multi-jump continuation, long-range kings, promotion, blocked-piece losses, Training Wheels rejection messages, and winner detection.

## HUD
- Current player, selected piece, forced-jump message, captured counts, and coordinates when enabled.
- Training Wheels hint and rejection message strip below the board; it never covers playable squares.
- Promotion and multi-jump prompts appear in the HUD/action area, not as modals.
- Victory/defeat messaging follows `design/common/victory-defeat.md` and appears below the board, never over the checkers board.
