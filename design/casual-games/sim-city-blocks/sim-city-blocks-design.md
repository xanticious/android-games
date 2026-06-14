# Sim City Blocks — Design Document

## Overview
- Sim City Blocks is a casual single-player city-planning game on a compact square grid.
- The player places residential, commercial, and industrial zone tiles onto the grid to grow a small city while keeping citizen happiness and the city budget positive.
- Adjacency rules between zone types drive happiness and income: residences near parks thrive; industrial zones near residences reduce happiness; commercial zones near residences generate income.
- The game is turn-based: each turn, the player places one tile or takes one demolish/upgrade action, then the city's economy and happiness are updated.
- There is no explicit end condition; the player builds as long as they choose. The target is to maximize city score (population × happiness).
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Grid surface: a light `Dark0`/`Dark1` checker pattern.
- Zone tiles use distinct flat-colored icons:
  - Residential: `Aqua2` house silhouette.
  - Commercial: `Teal3` shop/briefcase silhouette.
  - Industrial: `Coral` factory silhouette.
  - Park/Green Space: `Green2` tree silhouette.
  - Road: `Dark3` asphalt strip.
- A small happiness indicator floats above each residential tile (green smile / yellow neutral / red frown).
- Budget bar: a horizontal bar at the bottom of the screen, green when positive, red when in deficit.
- Tile placement preview: ghost tile shown on hover/press before confirming placement.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Sim City Blocks   Score: 1,240 ⚙   │  ← Top bar
├─────────────────────────────────────┤
│  💰 Budget: +$320/turn  😊 Happy: 74%│  ← Economy strip
├─────────────────────────────────────┤
│  [🏠][🏠][🏪][🌳]                  │
│  [🏭][🏠][🏠][🏪]                  │  ← Grid (8×8 default; scrollable for larger)
│  [🏠][🌳][🏪][🏠]                  │
│  [  ][  ][  ][  ]  ← empty cells    │
├─────────────────────────────────────┤
│  [🏠Res][🏪Com][🏭Ind][🌳Park][🛣Rd] │  ← Tile palette (scrollable)
│  Cost: $50               [Place]    │  ← Cost display + Place button
└─────────────────────────────────────┘
```
- Tapping an empty cell with a tile selected places it.
- Tapping an occupied cell enters an info/demolish view.
- The grid scrolls/zooms on larger grid sizes.

## Settings
- **Grid size**: 6×6, 8×8 (default), 12×12.
- **Starting budget**: $500 (tight), $1000 (default), $2000 (relaxed).
- **Show happiness indicators** (on/off, default on).
- **Show adjacency hints** (on/off, default on): highlights cells where placing the selected tile would be beneficial (faint green glow) or harmful (faint red glow).
- **Auto-advance turn** (on/off, default off): automatically ends the turn after each tile placement.

## How to Play
- Select a tile type from the palette at the bottom.
- Tap an empty cell to preview the tile (ghost + cost displayed).
- Tap **Place** or the cell again to confirm placement. The tile cost is deducted from the budget.
- After placing a tile, the economy updates: income and expenses are calculated based on zone adjacency, and city happiness is recalculated.
- If the budget goes negative, no new tiles can be placed until revenue recovers.
- Tap any placed tile to see its stats or demolish it (demolishing refunds 50% of cost).
- Aim for the highest city score: population (number of residential tiles) × average happiness percentage.

## Adjacency Rules
| Zone | Adjacent to | Effect |
|------|------------|--------|
| Residential | Park | +10 happiness per neighboring park |
| Residential | Commercial | +$20 income, 0 happiness change |
| Residential | Industrial | −15 happiness |
| Residential | Road | +5 happiness (connectivity bonus) |
| Commercial | Residential | +$30 income |
| Commercial | Road | +$15 income |
| Industrial | Road | +$25 income |
| Industrial | Residential | −$5 income (protests) |
| Park | Any | No income cost; provides happiness to adjacent residences |
- Each tile accrues income/expense per-turn based on all eight neighbors.
- Roads do not generate income but reduce negative adjacency penalties.

## Gameplay Loop
1. Player selects a tile from the palette.
2. Player taps a target cell; cost and adjacency impact preview are shown.
3. Player confirms placement (or cancels).
4. Economy update fires: budget changes, happiness recalculates, city score updates.
5. If budget < 0: show deficit warning; no placements allowed until balance is positive again.
6. Player continues at their own pace; there is no turn limit or time pressure.

## State Machine
- A dedicated `SimCityBlocksStateMachine` in `state/` exposes `StateFlow<SimCityBlocksState>`.
```
Idle
 └─ StartGame → Planning
Planning
 ├─ TileSelected → TilePreviewing
 ├─ CellInspected → CellDetail
 └─ (no active action)
TilePreviewing
 ├─ PlacementConfirmed [budget ok] → Updating
 ├─ PlacementConfirmed [no budget] → Planning (error shown)
 └─ PlacementCancelled → Planning
CellDetail
 ├─ DemolishConfirmed → Updating
 └─ DetailClosed → Planning
Updating
 └─ EconomyUpdated → Planning
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Highest city score achieved | yes |
| Most residential tiles in a city | yes |
| Highest average happiness reached | yes |
| Total turns played (lifetime) | yes |
| Longest budget-positive streak | yes |

## HUD
- Top bar: title, current city score, settings.
- Economy strip: budget per turn (green/red), overall happiness percentage.
- Grid: zone tiles with happiness indicators; empty cells show a subtle grid line.
- Tile palette: scrollable chip row with tile icon, name, and cost; selected tile is highlighted.
- Adjacency hint overlay (if enabled): green/red cell tinting while a tile is selected.
- Deficit warning: a red banner below the economy strip when the budget is negative.
- City score and happiness are updated immediately after each placement; no summary panel is shown unless the player opens it via settings.
