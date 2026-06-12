# Chess — Design Document

## Overview
- Chess is a full-featured offline board game adaptation for phone and tablet play against local AI.
- It supports standard chess and Chess960, preserves all classical rules, and presents information clearly without clutter.
- The player always faces one local AI opponent.
- Core rules include castling, en passant, promotion, check, checkmate, stalemate, insufficient material, threefold repetition, and the 50-move rule.
- Chess960 uses randomized legal starting positions with bishops on opposite colors and the king between rooks; castling follows Chess960 castling rules.
- Training Wheels mode can optionally restrict the player's choices to strong candidate moves without changing the underlying game rules.
- The experience emphasizes readable move feedback, deliberate touch input, and strong post-move explanation hooks for future training features.
- All matches are single-player, all stats stay on-device, and all navigation remains inside the app's simple state-machine flow.
- The board is always the visual anchor, while move history, promotion choice, and result messaging stay outside the playable grid.

## Visual Style
- Material 3 presentation using the underwater palette from `ui/theme/Color.kt`.
- Background surfaces use `Dark0` and `Dark1`; board frame accents use `Aqua4`.
- Light squares use a tinted `Aqua0` treatment; dark squares use a muted `Dark2` treatment.
- White pieces use bright `Aqua0` fills with `Dark1` line detail for readability.
- Black pieces use `Dark0` / `Dark1` bodies with `Aqua1` edge highlights so they remain legible in dark theme.
- Valid move indicators use small `Aqua2` dots for quiet moves and `Aqua3` rings for captures.
- Selected piece glow uses `Aqua3`; the last move trail uses a subtle `Aqua1` wash when enabled.
- Check warning highlights the threatened king square with a restrained `Aqua2` pulse plus a stronger border treatment.
- Captured pieces, clocks, and buttons sit on cards with rounded Material 3 surfaces, never on top of the board.
- Animations are short and intentional: piece slides, capture fade-outs, check pulse, promotion reveal, and end-state panel motion.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Chess        Standard  You: White ⚙ │  ← Top bar
├─────────────────────────────────────┤
│ AI captured: ♙ ♘     Move 18...Nc6   │  ← Opponent strip
├─────────────────────────────────────┤
│                                     │
│             8×8 board                │  ← Board anchor
│                                     │
├─────────────────────────────────────┤
│ You captured: ♟ ♞     Check: no      │  ← Player strip
│ Select a piece. Training: on         │  ← Status/result panel
└─────────────────────────────────────┘
```
- Portrait-first: the board remains centered and square with captured pieces and move feedback above and below it.
- Tablet layouts can add side gutters for notation, captured pieces, and engine-free move explanations.
- Promotion choices appear in an inline panel adjacent to the bottom status area, not as a blocking modal over the board.

## Settings
- **Ruleset**: Standard chess or Chess960.
- **Player color**: White, Black, or Random.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Training Wheels Mode** (on/off, default off): restricts selectable player moves to strong candidate moves and explains why weaker legal moves are hidden.
- **Show legal moves** (on/off, default on): highlights destinations for the selected piece.
- **Show last move** (on/off, default on): lightly marks the origin and destination of the previous move.
- **Move history** (on/off, default on): shows compact algebraic notation outside the board.
- **Fast AI turns** (on/off, default on): resolves AI thinking with a short delay and visible move animation.

## How to Play
- Choose Standard or Chess960, select a color, and start against the local AI.
- White moves first. Tap one of your pieces, then tap a highlighted destination to make a legal move.
- Capture by moving onto an opposing piece's square.
- Check means a king is under attack; a legal move must get out of check.
- Checkmate wins immediately. Stalemate, insufficient material, threefold repetition, and the 50-move rule end the game as draws.
- Pawns promote on the last rank; choose queen, rook, bishop, or knight from the inline promotion panel.
- In Chess960, the starting back rank changes, but the final castled king and rook squares match normal chess conventions.

## Controls
- Tap a piece to select it; tap the same piece or board background to clear selection.
- Tap a legal destination to commit the move.
- Long-press a highlighted destination for a short rule explanation when Training Wheels is enabled.
- Tap a move-history entry to preview that move's origin and destination without changing game state.
- Use Rematch or Menu only from the below-board result panel after the game ends.

## AI Opponents
- **Easy**: makes legal moves with basic material awareness, accepts simple captures, and misses deeper tactics.
- **Medium**: balances material, king safety, development, pawn structure, checks, and one-to-two move tactics.
- **Hard**: searches deeper tactical lines, values endgame conversion, handles Chess960 development, and avoids common traps.
- AI difficulty changes decision quality only; it never changes legal moves, draw rules, Chess960 castling rules, or Training Wheels filtering.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses / draws per difficulty | yes |
| Results by ruleset and player color | yes |
| Fastest checkmate by player | yes |
| Average moves per completed game | yes |
| Promotions made | yes |
| Training Wheels games completed | yes |

## State Machine
- A dedicated `ChessStateMachine` in `state/` exposes `StateFlow<ChessState>`.
```
Idle
 └─ MatchStarted → ChoosingSide
ChoosingSide
 └─ SideChosen → PlayerTurn / AiThinking
PlayerTurn
 ├─ PieceSelected → PieceSelected
 ├─ InvalidMoveExplained → PlayerTurn
 └─ Surrendered → GameOver
PieceSelected
 ├─ DestinationChosen → ResolvingMove
 └─ SelectionCleared → PlayerTurn
ResolvingMove
 ├─ PromotionRequired → PromotionChoice
 ├─ CheckmateDetected → GameOver
 ├─ DrawDetected → GameOver
 └─ TurnAdvanced → AiThinking / PlayerTurn
PromotionChoice
 └─ PromotionSelected → ResolvingMove
AiThinking
 └─ AiMoveChosen → ResolvingMove
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `ChessRules` controller validates legal moves, check states, castling, en passant, promotion, draw detection, Chess960 setup/castling, and Training Wheels candidate filtering; all rules are unit-tested without Android imports.

## HUD
- Top bar shows ruleset, player color, turn, and settings access.
- Opponent and player strips show captured pieces, last move, check status, and compact notation.
- Status panel shows prompts, Training Wheels explanations, promotion choices, and final result below the board.
- Victory/defeat/draw messaging follows `design/common/victory-defeat.md`: result panels never cover the board.
