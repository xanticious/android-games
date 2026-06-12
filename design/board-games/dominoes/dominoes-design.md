# Dominoes — Design Document

## Overview
- Dominoes is a classic matching tile game adapted for offline Android play against local AI.
- One human player faces one AI opponent in a clean two-player draw-dominoes ruleset with an optional Fives scoring variant.
- Players lay rectangular domino tiles end-to-end, matching pip counts on the open ends of the chain.
- The goal is to empty your hand first; if the round blocks, the lower remaining pip total wins the round.
- In Fives, players also score during the round whenever the open ends of the chain total a multiple of five.
- Matches play to a target score so short rounds build into a satisfying session.
- The design emphasizes readable pips, clear legal endpoints, and calm tabletop pacing without casino styling.
- All match results, scores, and stats stay local to the device.
- The domino chain is always the visual anchor; draw pile, hands, scoring, and result messaging stay outside the playable chain.
- Victory and defeat presentation follows `design/common/victory-defeat.md`; results never cover the domino layout.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Background surfaces use `Dark0` and `Dark1`, with the table well rendered as a rounded `Dark1` card.
- Domino tiles use light surfaces derived from `Aqua0` with `Dark1` pips and center dividers for strong contrast.
- Selected playable tiles lift slightly and receive an `Aqua3` outline.
- Legal open ends glow with `Aqua2` rings and small pip-count labels.
- Score chips, turn markers, and match target accents use `Aqua3` and `Aqua4`.
- Blocked or must-draw prompts use Material 3 error/tertiary roles rather than custom hex colors.
- Tile placement animates with a short slide and snap; chain reflow is instant or lightly animated depending on reduce-motion.
- The tone is modern tabletop, calm and tactile, not gambling-themed.
- All color references in implementation must come from `ui/theme/Color.kt` and Material 3 roles built from those tokens.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Dominoes      Fives to 100     [⚙]  │  ← Top bar
├─────────────────────────────────────┤
│ AI: 6 tiles     Score You 20  AI 15  │  ← Opponent/score strip
├─────────────────────────────────────┤
│        [draw pile: 9]                │
│   ┌─────────────────────────────┐    │
│   │  6|6 — 6|3 — 3|5 — 5|0      │    │  ← Domino chain anchor
│   └─────────────────────────────┘    │
├─────────────────────────────────────┤
│ Your hand: [0|0] [0|5] [2|4] [3|6]  │
│ Match an open end or draw a tile.    │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait stacks score strip, chain well, hand row, and prompt panel.
- The chain may wrap into multiple readable rows, but open ends must remain visually clear.
- Tablet layouts can widen the chain well and show round history in a side column.
- Result panels appear below the chain/hand area and never obscure placed tiles.

## Settings
- **Ruleset**: Draw Dominoes or Fives.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Target score**: 50, 100, or 150; default 100.
- **Starting double rule**: Highest double starts (default) or highest tile starts.
- **Show playable tiles** (on/off, default on): highlights tiles in the player's hand that match an open end.
- **Show open-end total** (on/off, default on): displays the current end sum and Fives scoring opportunity.
- **Fast AI turns** (on/off, default on): summarizes AI draws and placements with visible tile movement.

## How to Play
- Each round starts with shuffled double-six dominoes and a dealt hand for each player.
- The starting player places the highest eligible double, or the highest tile if that setting is selected and no double is required.
- On your turn, play one tile from your hand onto either open end of the chain, matching the pip count on that end.
- Doubles are placed crosswise for readability but still create only the normal matching end for this ruleset.
- If you cannot play, draw from the boneyard until you can play or the boneyard is empty.
- Empty your hand to win the round. If no player can move and the boneyard is empty, the round blocks; the lower remaining pip total wins.
- Round points come from the opponent's remaining pips, rounded by the selected ruleset if needed.
- In Fives, also score during play whenever the open ends total 5, 10, 15, or another multiple of five.
- First player to reach the target score wins the match.

## Controls
- Tap a playable tile to select it; legal open ends glow.
- Tap an open end to place the selected tile there.
- If a selected tile can only go on one end, tapping it a second time may auto-place it when that accessibility shortcut is enabled globally.
- Tap Draw when no playable tile is available.
- Long-press a tile, open end, or score chip for a short rules explanation.
- After match end, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: plays the first legal tile, draws when stuck, and rarely considers future pip flexibility.
- **Medium**: prefers reducing high pips, preserving useful suits, and taking obvious Fives scoring placements.
- **Hard**: tracks seen tiles, estimates the player's blocked suits, manages doubles, and balances immediate Fives points against round control.
- AI difficulty changes decision quality only; draw rules, Fives scoring, and target score remain unchanged.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty and ruleset | yes |
| Highest single-round score | yes |
| Fives points scored during play | yes |
| Blocked rounds won | yes |
| Average remaining pips at round end | yes |
| Rounds won by dominoing out | yes |

## State Machine
- A dedicated `DominoesStateMachine` in `state/` exposes `StateFlow<DominoesState>`.
```
Idle
 └─ MatchStarted → Dealing
Dealing
 └─ TilesDealt → ChoosingStarter
ChoosingStarter
 └─ StarterChosen → PlayerTurn / AiTurn
PlayerTurn
 ├─ TileSelected → ChoosingEnd
 ├─ DrawRequested → DrawingTile
 ├─ NoMoveExplained → PlayerTurn
 └─ Surrendered → GameOver
ChoosingEnd
 ├─ EndChosen → PlacingTile
 └─ SelectionCleared → PlayerTurn
DrawingTile
 └─ TileDrawn → PlayerTurn / TurnPassed
PlacingTile
 └─ TilePlaced → ScoringPlay
ScoringPlay
 ├─ RoundWon → RoundScoring
 ├─ RoundBlocked → RoundScoring
 └─ TurnAdvanced → AiTurn / PlayerTurn
AiTurn
 └─ AiActionResolved → ScoringPlay / TurnAdvanced
RoundScoring
 ├─ TargetReached → GameOver
 └─ NextRound → Dealing
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `DominoesRules` controller validates tile matching, draw availability, start-tile selection, blocked rounds, remaining-pip scoring, Fives open-end scoring, target-score completion, and legal move generation; unit tests cover all rules without Android imports.

## HUD
- Top bar shows game name, ruleset, target score, and settings access.
- Score strip shows match score, round score events, AI tile count, and current turn.
- Chain well shows placed tiles, legal open ends, draw pile count, and Fives end total when enabled.
- Hand row shows the player's tiles with playable highlights.
- Prompt/result panel shows draw requirements, scoring breakdowns, blocked-round summaries, and final results below the chain.
