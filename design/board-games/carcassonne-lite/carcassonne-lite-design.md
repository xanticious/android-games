# Carcassonne Lite — Design Document

## Overview
Carcassonne Lite is a streamlined tile-laying strategy game about spatial planning, tactical scoring, and long-term territory control.
The player competes against one to three AI opponents using a shared draw pile from the standard 72-tile base set.
Each turn centers on one clean sequence: draw, rotate, place, optionally deploy a meeple, then score any completed features.
The tone is cozy and thoughtful, with an elegant tabletop presentation optimized for touch and portrait-friendly play.
This Lite version keeps the core base-set feature families: cities, roads, monasteries, and fields.
Fields only score at the end of the game.
Meeples placed on features do not return mid-game in this ruleset, creating sharper scarcity and simpler bookkeeping.
The result is a more strategic long-arc experience with less rules overhead during active turns.

## Visual Style
The board uses a crisp tabletop tile look with soft Material 3 elevation and underwater-palette framing.
Empty board space uses Dark0 with a faint grid fade to help orientation during scrolling.
Placed tiles sit on Dark1 shadows with thin Dark2 outlines.
Active placement hints use Aqua3 borders and Aqua1 glows.
Invalid placement previews use a muted error treatment, never a usable-looking highlight.
The current drawn tile rests in a prominent tray using Aqua0 surface accents.
Meeples use player-distinct silhouettes with palette-safe tints, but all UI chrome still references Aqua and Dark tokens for consistency.
Score pills and turn chips use Aqua2, Aqua3, and Aqua4 for emphasis.
Completed feature flashes should be subtle and classy rather than noisy.
The board may become very large, so visual clutter must stay low as the map expands.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Carcassonne Lite   Tile 28/72 [⚙]   │  ← Top bar
├─────────────────────────────────────┤
│ Scores  You 24  AI1 19  AI2 21      │  ← Score strip
├─────────────────────────────────────┤
│                                     │
│          expanding tile map         │
│      [city][road][field]            │
│   [road][monastery][city]           │  ← Board anchor
│        [field][road][preview]       │
│                                     │
├─────────────────────────────────────┤
│ Drawn tile       Meeples: You 4     │
│ [⟲ Rotate] [Place] [Skip meeple]    │
└─────────────────────────────────────┘
```
Portrait-first: the expanding map is the center anchor with pan and zoom gestures, while the drawn tile tray and controls stay fixed below it. Tablet layouts place the drawn tile, player scores, and meeple inventory in side panels so more of the map remains visible.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard (default Medium).
- **AI opponents**: 1, 2, or 3 (default 2).
- **Tile set**: Base 72-tile Lite set (default; fixed for initial release).
- **Show placement hints** (on/off, default on): highlights legal edges for the drawn tile.
- **Show scoring preview** (on/off, default on): estimates immediate completed-feature points before placement.
- **Confirm meeple skip** (on/off, default on): asks for inline confirmation before ending a turn without placing a meeple.
- Settings are chosen on the per-game Settings screen before the shared draw pile is created.

## How to Play
Draw one tile each turn, rotate it, and place it so all touching edges match: roads connect to roads, city edges to city edges, and fields to fields. After placing the tile, you may place one meeple on an unclaimed feature of that tile if you have a meeple available.

Completed roads, cities, and monasteries score immediately for the player or AI with the most meeples on that feature. Meeples do not return mid-game in this Lite ruleset, even after a feature scores, so each placement is a long-term commitment. Fields score only at the end of the game based on completed cities they supply. The game ends when the 72-tile draw pile is empty; highest score wins after end-game field and unfinished-feature scoring.

## Controls
- Drag to pan the map and pinch to zoom; double-tap recenters on the current legal placement area.
- Tap **Rotate** or twist the drawn tile to rotate it in 90-degree increments.
- Drag the drawn tile onto the map or tap a highlighted legal location, then tap **Place**.
- After placement, tap a valid feature segment on the tile to place a meeple, or tap **Skip meeple**.
- Tap score chips to inspect a compact non-modal breakdown below the board.

## AI Opponents
- **Easy**: places legal tiles quickly, claims obvious completed features, and rarely plans field scoring.
- **Medium**: values immediate points, meeple scarcity, monastery completion, and blocking simple opponent features.
- **Hard**: evaluates tile probabilities, shared-feature contests, long-term fields, opponent meeple shortages, and end-game tempo.
- AI difficulty changes decision quality only; tile legality, meeple limits, no mid-game meeple return, field scoring, and player count rules never change.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty and AI count | yes |
| Highest final score | yes |
| Largest completed city score | yes |
| Most field points in a game | yes |
| Games won with zero meeples remaining | yes |
| Average final margin | yes |

## State Machine
A dedicated `CarcassonneLiteStateMachine` in `state/` exposes `StateFlow<CarcassonneLiteState>` and owns draw, rotate, placement, meeple, scoring, AI turn, and final scoring transitions.
```
Idle
 └─ MatchStarted → DrawingTile
DrawingTile
 └─ TileDrawn → PlacingTile
PlacingTile
 ├─ TileRotated → PlacingTile
 ├─ TilePreviewed → PlacingTile
 └─ TilePlaced → MeepleDecision
MeepleDecision
 ├─ MeeplePlaced → ScoringFeatures
 └─ MeepleSkipped → ScoringFeatures
ScoringFeatures
 ├─ TurnPassed → AiTurn
 └─ DrawPileEmpty → FinalScoring
AiTurn
 └─ AiTurnCompleted → DrawingTile
FinalScoring
 └─ ScoresFinalized → GameOver
GameOver
 └─ Rematch / Menu → Idle
```
A pure `CarcassonneLiteRules` controller in `controller/` has no Android imports and is unit-tested for edge matching, rotation, feature ownership, immediate scoring, permanent meeple placement, field scoring at end only, AI count setup, and final winner determination.

## HUD
- Tile count, current player, scores for all players, and remaining meeples.
- Drawn-tile tray, rotation, legal-placement hints, scoring preview when enabled, and current turn action.
- End-game scoring breakdown separates immediate features, unfinished features, and fields.
- Victory/defeat messaging follows `design/common/victory-defeat.md` and appears below the map, never over placed tiles.
