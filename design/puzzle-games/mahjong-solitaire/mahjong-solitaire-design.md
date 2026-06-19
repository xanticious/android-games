# Mahjong Solitaire — Design Document

## Overview
- Mahjong Solitaire is a single-player tile-matching game. 144 tiles are stacked in a multi-layer layout (the classic "Turtle" by default). The player removes tiles two at a time by matching pairs.
- Only **free** tiles can be selected: a tile is free when it has no tile on top of it and at least one of its left or right long edges is open.
- Matching rules follow standard Mahjong sets: identical tiles match; the four Flowers match each other and the four Seasons match each other (any-within-group).
- The board is cleared when all 72 pairs are removed. A game can stall (no legal pair among free tiles); the player may shuffle or undo.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Layout**: Turtle (default), Pyramid, Fortress, Dragon — different stacked arrangements.
- **Guaranteed solvable**: on (default) / off — when on, only winnable deals are dealt.
- **Match hints**: on (default) / off — a Hint button highlights one available matching pair.
- **Tile face set**: Traditional / High-contrast — both drawn from the palette; high-contrast aids readability.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Tiles are rounded rectangles with subtle drop shadows to convey stacking depth; faces use clear suit glyphs (Dots, Bamboo, Characters, Winds, Dragons, Flowers, Seasons).
- Free tiles read at full opacity; blocked tiles are slightly dimmed so the player can see what is playable.
- Selection: first tapped tile gets an `Aqua2` outline; a match dissolves both tiles upward; a mismatch nudges and deselects.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Mahjong       Pairs 31/72      ⚙ ? │
├─────────────────────────────────────┤
│        ▦▦▦▦▦▦▦▦▦▦▦▦                 │
│       ▦▦▦ ▦▦▦▦▦▦ ▦▦▦                │  ← stacked tile layout (depth shading)
│      ▦▦▦▦▦▦▦▦▦▦▦▦▦▦                 │
│       ▦▦▦▦▦▦▦▦▦▦▦▦                  │
├─────────────────────────────────────┤
│  Undo  Hint  Shuffle  Moves left: 5 │
└─────────────────────────────────────┘
```

## How to Play
- Tap a free tile to select it, then tap a matching free tile to remove the pair.
- A tile is free if nothing sits on top of it and its left or right side is open.
- Clear all tiles to win. If you get stuck, use Hint, Undo, or Shuffle.

## Controls
- **Tap** a free tile: select / deselect; tapping a matching free tile removes the pair. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Undo**: restore the most recently removed pair (unlimited).
- **Hint**: highlight one available matching pair.
- **Shuffle**: re-randomize remaining tiles (recorded in stats; may be limited per game in a future tweak).

## Gameplay Rules
- Freeness is recomputed after every removal.
- Tiles match if identical, or both Flowers, or both Seasons.
- Win: all tiles removed. Stall: no matching pair among free tiles (offer Shuffle).
- Freeness, match-legality, and stall-detection are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `MahjongStateMachine` in `state/` exposes `StateFlow<MahjongState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ TileSelected → Playing
 ├─ PairMatched → Playing (tiles removed, freeness updated)
 ├─ Mismatch → Playing (deselect)
 ├─ Undo / Hint / Shuffle → Playing
 ├─ NoMovesLeft → Stalled
 └─ BoardCleared → Solved
Stalled
 ├─ Shuffle → Playing
 └─ NewGame → Playing
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Games won (per layout) | yes |
| Best time (per layout) | yes |
| Win streak | yes |
| No-shuffle / no-hint wins | yes |

## HUD
- Top bar: title, pairs cleared, settings, help.
- Bottom row: Undo, Hint, Shuffle, and an available-moves indicator.
- Win message appears below the layout; the cleared board area stays visible.
