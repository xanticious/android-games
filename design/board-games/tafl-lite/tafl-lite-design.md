# Tafl Lite — Design Document

## Overview
- Tafl Lite is an 11×11 Hnefatafl-inspired strategy game focused on asymmetric pressure and escape.
- One side controls 24 Attackers pressing inward from the outer formation.
- The other side controls 12 Defenders plus the King in the center.
- The human can play as Attackers, Defenders, or let the game assign a random side.
- The opposing side is always controlled by local AI.
- Attackers win by surrounding the King on all required orthogonal sides.
- Defenders win by escorting the King to any corner escape square.
- All pieces move like chess rooks across any number of empty orthogonal squares.
- Standard captures occur by sandwiching an enemy piece between two allied pieces.
- The King has special survival rules and requires a full surround, or three sides when trapped at the board edge.
- The digital adaptation must teach asymmetry clearly without reducing the tension of the original contest.
- Training Wheels Mode is a first-class feature and should feel supportive rather than punitive.
- The experience should be crisp, austere, and readable, with strong board-state clarity at a glance.
- The game is entirely offline and stores statistics, side preferences, and hint settings locally.
- Victory and defeat presentation follows `design/common/victory-defeat.md`; result panels never cover the board.

## Visual Style
- The board uses Material 3 surfaces with a restrained Scandinavian-inspired presentation filtered through the underwater palette.
- Base board surface uses `Aqua0` in light theme and `Dark1` in dark theme, with grid lines pulled from `Aqua3` or softened `Dark2` contrast.
- Corner escape squares use `Aqua2` highlights so they are always legible as special destinations.
- The throne square at center uses `Aqua4` accents and a distinct emblem ring.
- Attackers use darker, heavier tokens with `Dark2` cores and `Aqua1` edge contrast.
- Defenders use brighter tokens with `Aqua1` fill and `Aqua4` outline.
- The King receives the strongest emphasis: elevated disc, crown glyph, and persistent legal-state halo when selected.
- Valid move indicators are circular dots on reachable squares; capture-threat indicators use edge brackets, not arrows.
- Training Wheels feedback uses calm instructional styling, never red error panic.
- Status and evaluation messages sit in the bottom panel to preserve the board's visual integrity.
- Animations are sparse: slide movement, capture fade, throne pulse, and result panel reveal.
- Reduced-motion mode swaps movement slides for instant repositioning.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Tafl Lite        You: Defenders [⚙] │  ← Top bar: side, turn, settings
├─────────────────────────────────────┤
│ Attackers 24        King: Center     │  ← Status strip
├─────────────────────────────────────┤
│  C  ·  ·  ·  ·  A  ·  ·  ·  ·  C    │
│  ·  ·  ·  A  A  A  A  A  ·  ·  ·    │
│  ·  ·  ·  ·  ·  A  ·  ·  ·  ·  ·    │
│  ·  A  ·  ·  ·  D  ·  ·  ·  A  ·    │
│  ·  A  ·  ·  D  D  D  ·  ·  A  ·    │  ← 11×11 board, centered
│  A  A  A  D  D  K  D  D  A  A  A    │
│  ·  A  ·  ·  D  D  D  ·  ·  A  ·    │
│  ·  A  ·  ·  ·  D  ·  ·  ·  A  ·    │
│  ·  ·  ·  ·  ·  A  ·  ·  ·  ·  ·    │
│  ·  ·  ·  A  A  A  A  A  ·  ·  ·    │
│  C  ·  ·  ·  ·  A  ·  ·  ·  ·  C    │
├─────────────────────────────────────┤
│ Select a defender. Corners escape.   │  ← Training/status/result panel
└─────────────────────────────────────┘
```
- Portrait-first; tablet adds wider margins, side summaries, and larger move-history chips without changing the core board order.

## Settings
- **Player side**: Attackers, Defenders, or Random.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Training Wheels Mode** (on/off, default on): shows legal moves, capture danger, and why a selected move is blocked.
- **Capture warnings** (on/off): highlight pieces that can be captured next turn.
- **Move history** (on/off): show the last move and last capture below the board.
- **Reduced motion** follows the system preference and can be exposed as an in-game override if the app adds a shared setting.

## How to Play
- Attackers move first unless a rule variant setting later says otherwise.
- Tap one of your pieces, then tap a highlighted orthogonal destination in the same row or column.
- Pieces cannot jump over other pieces.
- Capture most enemy pieces by trapping them orthogonally between the moved piece and another allied piece.
- The King is harder to capture and must be surrounded on all required sides.
- Attackers win by capturing the King before he escapes.
- Defenders win the moment the King reaches any corner square.

## Controls
- Tap a piece to select it; tap it again or tap the board background to clear selection.
- Tap a legal destination to move.
- Long-press a piece or special square to show a short rules explanation.
- Undo is not available in ranked stat games; an optional practice undo can be offered only when stat recording is disabled.

## AI Opponents
- **Easy**: chooses legal moves with light escape/capture awareness and may miss multi-turn traps.
- **Medium**: values king lanes, attacker nets, defender shields, and immediate capture threats.
- **Hard**: searches forcing lines, tightens escape corridors, sacrifices pieces intentionally, and changes plans based on the player's side.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per side and difficulty | yes |
| Fastest escape as Defenders | yes |
| Fastest capture as Attackers | yes |
| Average moves per completed game | yes |
| Training Wheels games completed | yes |

## State Machine
- A dedicated `TaflLiteStateMachine` in `state/` exposes `StateFlow<TaflLiteState>`.
```
Idle
 └─ MatchStarted → ChoosingSide
ChoosingSide
 └─ SideChosen → Playing
Playing
 ├─ PieceSelected → PieceSelected
 ├─ MoveCommitted → ResolvingCapture
 ├─ InvalidMoveExplained → Playing
 └─ Surrendered → GameOver
PieceSelected
 ├─ DestinationChosen → ResolvingCapture
 └─ SelectionCleared → Playing
ResolvingCapture
 ├─ KingCaptured → GameOver
 ├─ KingEscaped → GameOver
 └─ TurnAdvanced → AiThinking / Playing
AiThinking
 └─ AiMoveChosen → ResolvingCapture
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `TaflLiteRules` controller validates rook-like movement, special squares, captures, king capture requirements, escape detection, and legal move generation; unit tests cover attacker and defender edge cases.

## HUD
- Top bar shows game name, current side, active turn, and settings access.
- Status strip shows remaining attackers, remaining defenders, king safety, and last capture.
- Bottom panel shows training explanations, turn prompts, and victory/defeat results below the board.
