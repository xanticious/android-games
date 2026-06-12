# Shogi Lite — Design Document

## Overview
- Shogi Lite is a streamlined offline implementation of Japanese chess for one human versus one AI opponent.
- The board is 9×9 and follows standard Shogi movement, capture, promotion, and drop rules.
- Captured pieces switch allegiance and enter the captor's hand for possible later drops.
- Promotion becomes available when eligible pieces move into, within, or out of the promotion zone, defined as the farthest three ranks.
- Core pieces included are King, Rook, Bishop, Gold General, Silver General, Knight, Lance, and Pawn, along with their promoted forms where applicable.
- The design should make a traditionally complex ruleset feel approachable without removing authentic decision-making.
- Training support, move hints, and visible captured-piece hands are essential for readability.
- The game remains strictly local, single-player, and fully turn-based.

## Visual Style
- Use Material 3 with the underwater palette from `ui/theme/Color.kt`.
- Background depth uses `Dark0`; side trays, hand panels, and supporting chrome use `Dark1` and `Dark2`.
- Board cells use `Aqua0` as the neutral wood-replacement surface, giving enough contrast for either piece style.
- Valid move hints use `Aqua2`; active selection borders and path emphasis use `Aqua3`.
- Promotion-zone awareness uses a very soft `Aqua1` tint band across the last three ranks on each side.
- Check warning accents use `Aqua3` text and icon treatment rather than a loud overlay.
- Piece style is switchable between International iconography and Traditional kanji, but both sit on the same board and HUD language system.
- Piece art should feel substantial and tactile, with clear facing direction so ownership is always readable.
- Drop targets and illegal-drop reasons should be visually explicit to reduce learning friction.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Shogi Lite    Medium      Kanji  ⚙  │  ← Top bar
├─────────────────────────────────────┤
│ AI hand: 歩×2 角×1                  │  ← Captured-piece hand
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐   │
│  │ 香 桂 銀 金 王 金 銀 桂 香       │
│  │ · 飛 · · · · · 角 ·           │
│  │ 歩 歩 歩 歩 歩 歩 歩 歩 歩       │  ← Board anchor
│  │ · · · · · · · · ·           │
│  │ · · · · ◎ · · · ·           │
│  │ 歩 歩 歩 歩 歩 歩 歩 歩 歩       │
│  │ · 角 · · · · · 飛 ·           │
│  │ 香 桂 銀 金 玉 金 銀 桂 香       │
│  └───────────────────────────────┘   │
├─────────────────────────────────────┤
│ Your hand: 歩×1 銀×1                 │  ← Player hand
│ Selected pawn: move or drop.         │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait stacks the AI hand, 9×9 board, player hand, and prompt panel so captured pieces remain visible without covering the board.
- Tablet layouts keep the board square and place captured-piece hands, move history, promotion notes, and hint explanations in side gutters.
- Promotion zones are lightly tinted on the board; legal moves and legal drops are visually distinct.
- Check, checkmate, promotion choices, illegal-drop explanations, and results appear below or beside the board, never as modal overlays on the board.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard.
- **Piece style**: International icons or Traditional kanji.
- **Player side**: First player, Second player, or Random.
- **Move hints** (on/off, default on): highlights legal moves for selected board pieces.
- **Drop hints** (on/off, default on): highlights legal drop squares for selected captured pieces.
- **Training mode** (on/off, default off): explains promotion, check, pinned pieces, and illegal drops.
- **Auto-promote forced moves** (on/off, default on): automatically promotes pieces that would otherwise have no legal future move.
- **Fast AI turns** (on/off, default on): summarizes AI movement, captures, drops, and promotions quickly.

## How to Play
- Players alternate moving one piece, dropping one captured piece from hand, or promoting when eligible.
- The board is 9×9. Each side's promotion zone is the farthest three ranks from that side.
- Pieces move according to standard Shogi rules. King, Rook, Bishop, Gold General, Silver General, Knight, Lance, and Pawn are included with their normal promoted forms where applicable.
- Captured pieces change allegiance and enter the captor's hand.
- A piece in hand may be dropped onto a legal empty square as that player's piece.
- Eligible pieces may promote when moving into, within, or out of the promotion zone.
- A player may not make a move or drop that leaves their own king in check.
- Checkmate wins the game.

## Controls
- Tap one of your board pieces to show legal moves; tap a highlighted destination to move.
- Tap a piece in your captured-piece hand to show legal drop targets; tap a highlighted square to drop it.
- When promotion is optional, choose Promote or Keep from the inline prompt below the board.
- Tap a checked king, illegal drop marker, or training hint to read the reason in the prompt panel.
- Pinch to zoom the board if piece labels become tight on smaller phones.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: prefers material captures, simple checks, and safe drops, but misses multi-move mating threats and promotion tactics.
- **Medium**: evaluates king safety, promotion value, drops near the king, piece activity, and basic forced sequences.
- **Hard**: searches checks, drops, promotions, pins, sacrifice lines, defensive resources, and mating nets while respecting all Shogi legality.
- AI difficulty changes decision quality only; movement, capture, promotion, drop, and checkmate rules remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Results as first player / second player | yes |
| Checkmates delivered | yes |
| Drops played | yes |
| Promotions chosen | yes |
| Training explanations viewed | yes |

## State Machine
- A dedicated `ShogiLiteStateMachine` in `state/` exposes `StateFlow<ShogiLiteState>`.
```
Idle
 └─ MatchStarted → ChoosingSide
ChoosingSide
 └─ SideChosen → PlayerTurn / AiThinking
PlayerTurn
 ├─ BoardPieceSelected → SelectingMove
 ├─ HandPieceSelected → SelectingDrop
 └─ Surrendered → GameOver
SelectingMove
 ├─ DestinationChosen → ValidatingMove
 ├─ SelectionChanged → PlayerTurn
 └─ InvalidMoveExplained → PlayerTurn
SelectingDrop
 ├─ DropSquareChosen → ValidatingDrop
 ├─ SelectionChanged → PlayerTurn
 └─ InvalidDropExplained → PlayerTurn
ValidatingMove
 ├─ PromotionRequired → ChoosingPromotion
 ├─ PromotionOptional → ChoosingPromotion
 ├─ MoveAccepted → ResolvingMove
 └─ MoveRejected → PlayerTurn
ChoosingPromotion
 └─ PromotionChosen → ResolvingMove
ValidatingDrop
 ├─ DropAccepted → ResolvingMove
 └─ DropRejected → PlayerTurn
ResolvingMove
 └─ MoveResolved → CheckingResult
CheckingResult
 ├─ Checkmate → GameOver
 ├─ TurnAdvanced → PlayerTurn / AiThinking
 └─ IllegalStateExplained → PlayerTurn
AiThinking
 └─ AiActionChosen → ValidatingMove / ValidatingDrop
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `ShogiLiteRules` controller validates standard piece movement, captures to hand, drops, promotion eligibility, forced promotion, check, self-check prevention, checkmate, and hint generation; unit tests cover controller behavior without Android imports.

## HUD
- Top bar shows game name, difficulty, player side, piece style, and settings access.
- Captured-piece hand panels show piece counts for both sides and highlight selected drops.
- Board annotations show legal moves, legal drops, promotion zones, last move, check warning, and training markers.
- Prompt/result panel explains selected piece behavior, illegal actions, promotion choices, checkmate, and rematch actions below the board.
- Victory/defeat presentation follows `design/common/victory-defeat.md`: results never cover the board.
