# Qwirkle Lite — Design Document

## Overview
- Qwirkle Lite is a streamlined digital adaptation of the tile-laying game for one human player versus one, two, or three AI opponents.
- The tile set contains 6 shapes and 6 colors for 36 unique tiles, with 3 copies of each for a 108-tile bag.
- Every played line must share exactly one attribute.
- Valid lines are either all the same color with all different shapes, or all the same shape with all different colors.
- Duplicate exact tiles are never allowed in a single line.
- A line may contain at most 6 tiles.
- Completing a 6-tile line is a Qwirkle and awards 12 points total for that line.
- Each player starts with a hand of 6 tiles.
- On a turn, a player either places one or more tiles into a valid connected line extension, or trades selected tiles with the bag.
- The goal is to finish with the highest score after the bag empties and no player can continue.
- The tone should feel tactile, thoughtful, and readable, with strong placement guidance and score transparency.

## Visual Style
- Use Material 3 surfaces with the underwater palette from `ui/theme/Color.kt`.
- Screen background uses `Dark0`; cards, trays, and panel shells use `Dark1` and `Dark2`.
- The board grid uses `Aqua0` for low-contrast cells so tile silhouettes remain dominant.
- Valid placement markers use `Aqua2`; selected hand tiles use an `Aqua3` lift ring.
- Multi-tile pending placements connect with a faint `Aqua1` path line to show the active sequence.
- Score chips and action buttons use `Aqua3` as the primary accent.
- Tile faces are clean, high-contrast, and icon-driven; the underlying hand tray keeps enough padding that symbols remain readable on phones.
- Shapes and tile colors remain part of the game identity, while the surrounding UI chrome stays within the theme-token palette.
- The overall feel should evoke a polished tabletop set resting on a calm ocean-depth backdrop.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Qwirkle Lite   4 players   Bag 72 ⚙ │  ← Top bar
├─────────────────────────────────────┤
│ You 18  Bot A 14  Bot B 9  Bot C 6  │  ← Score strip
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐   │
│  │ · · · · · · · · · · · · · · · │   │
│  │ · · ◆R ◆B ◆G · · · · · · · · │   │  ← Board anchor
│  │ · · · · ▲G ○G ■G · · · · · · │   │
│  │ · · · · · · · · · · · · · · · │   │
│  └───────────────────────────────┘   │
├─────────────────────────────────────┤
│ Hand: [◆R] [◆Y] [▲G] [○B] [■G] [⬟R]│  ← Player tray
│ [Place selected] [Trade] [Undo]      │
├─────────────────────────────────────┤
│ Pending line: same color, no repeats │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait centers the board as a pannable grid with the player's hand fixed beneath it.
- Tablet layouts keep the board central and move opponent hands, score details, and line scoring previews into side gutters.
- Opponent hands show tile backs and counts only; exact AI tiles remain hidden.
- Placement prompts, score previews, trade confirmations, and result messages appear below the board and never cover placed tiles.

## Settings
- **AI opponents**: 1, 2, or 3 (default 2).
- **Opponent difficulty**: Easy, Medium, Hard.
- **Turn timer pressure**: Off, Relaxed, or Fast; affects only prompt pacing and stats labels, never forces automatic human moves.
- **Show legal anchors** (on/off, default on): marks empty cells adjacent to existing lines where a placement may begin.
- **Show line validation** (on/off, default on): explains shared attribute, duplicate tile, and six-tile-limit issues before confirmation.
- **Show score preview** (on/off, default on): previews points for each affected line and Qwirkle bonuses.
- **Fast AI turns** (on/off, default on): summarizes AI placements and trades while still showing final tile locations.

## How to Play
- Each player starts with six tiles.
- On your turn, place one or more tiles in a single row or column, or trade selected tiles with the bag.
- Every line containing your newly placed tiles must be valid: all tiles share exactly one attribute, either color or shape.
- A valid line cannot contain duplicate exact tiles and cannot exceed six tiles.
- Score one point per tile in every completed or extended line affected by your placement.
- A six-tile line is a Qwirkle and scores 12 points total for that line.
- After placing or trading, refill your hand from the bag when tiles remain.
- The match ends when the bag is empty and no player can continue; highest score wins.

## Controls
- Tap hand tiles to select or deselect them.
- Tap a board cell to place the first selected tile; continue tapping cells in the same row or column for multi-tile placements.
- Drag selected tiles from the tray onto board cells when touch precision allows.
- Tap **Undo** to return pending placements to your hand before confirmation.
- Tap **Place selected** to confirm a valid placement and score it.
- Tap **Trade** to exchange selected tiles instead of placing; trade is disabled when the bag lacks enough tiles.
- Tap a validation message to highlight the line or tile causing the issue.

## AI Opponents
- **Easy**: favors short valid extensions, obvious Qwirkles, and trades weak hands, but misses cross-line scoring opportunities.
- **Medium**: evaluates immediate score, duplicate risk, hand balance, and simple setup denial against the human player.
- **Hard**: searches multi-line placements, protects likely Qwirkle setups, tracks visible tile exhaustion, and chooses trades based on future hand potential.
- AI difficulty changes decision quality only; tile bag contents, scoring rules, and line validation remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses by opponent count and difficulty | yes |
| Highest single-turn score | yes |
| Qwirkles completed | yes |
| Average score per placement | yes |
| Trades taken | yes |
| Games won after final hand-out | yes |

## State Machine
- A dedicated `QwirkleLiteStateMachine` in `state/` exposes `StateFlow<QwirkleLiteState>`.
```
Idle
 └─ MatchStarted → DealingTiles
DealingTiles
 └─ TilesDealt → PlayerTurn / AiThinking
PlayerTurn
 ├─ TileSelected → PlayerTurn
 ├─ BoardCellChosen → BuildingPlacement
 ├─ TradeChosen → ConfirmingTrade
 └─ Surrendered → GameOver
BuildingPlacement
 ├─ TilePlacedPending → BuildingPlacement
 ├─ PlacementUndone → PlayerTurn
 ├─ PlacementRejected → PlayerTurn
 └─ PlacementConfirmed → ScoringPlacement
ConfirmingTrade
 ├─ TradeConfirmed → RefillingHands
 └─ TradeCancelled → PlayerTurn
ScoringPlacement
 └─ ScoreApplied → RefillingHands
RefillingHands
 ├─ NextTurn → PlayerTurn / AiThinking
 └─ GameFinished → GameOver
AiThinking
 └─ AiActionChosen → ScoringPlacement / RefillingHands
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `QwirkleLiteRules` controller validates shared-attribute lines, duplicate exact tiles, six-tile limits, connected placement, trades, bag refill, scoring, Qwirkle bonuses, and end-of-game detection; unit tests cover all rule paths without Android imports.

## HUD
- Top bar shows game name, player count, bag count, and settings access.
- Score strip shows all players, active turn, and last turn score.
- Board annotations show legal anchors, pending placement path, score previews, and invalid-line explanations.
- Player tray shows selected tiles, remaining hand count, trade state, and confirm actions.
- Victory/defeat/draw presentation follows `design/common/victory-defeat.md`: results never cover the board.
