# Match Three — Design Document

## Overview
- Match Three is a casual, endless tile-swapping puzzle game in the spirit of Bejeweled Zen mode.
- The player swaps two adjacent tiles (horizontally or vertically) to create a row or column of three or more matching gems. Matched tiles are cleared and new gems drop in from above to fill the gaps.
- There is no timer, no score, and no lose condition. The game runs as long as the player chooses.
- If the board ever has no valid swaps that would produce a match, it reshuffles automatically until at least one valid move exists. A brief "Reshuffling…" animation plays so the player understands what happened.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Gems: 6 gem types, each a distinct color drawn from the palette — Aqua, Teal, Coral, Green, Sand, and Deep (dark blue). Each type has a unique shape (circle, diamond, hexagon, star, teardrop, square) so color-blind players can distinguish them by shape alone.
- Board background: a deep `Dark1` grid with subtle cell outlines.
- Match animation: matched gems briefly flash white, then dissolve upward with a particle burst. Cascade matches produce progressively brighter flashes.
- Drop animation: replacement gems fall from above with a smooth ease-in curve; they land with a light bounce.
- Reshuffle animation: all gems spin and scatter briefly, then re-settle in their new positions.
- Invalid swap attempt: the two gems nudge toward each other and bounce back (no penalty, just feedback).
- No timers, score counters, or lives are shown at any time.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Match Three            Gems: 342 ⚙ │  ← Top bar (lifetime gem count, settings)
├─────────────────────────────────────┤
│                                     │
│  [◆][●][★][⬡][●][◆][▼]            │
│  [▼][★][◆][●][⬡][▼][★]            │  ← 7×7 gem board (default; configurable)
│  [⬡][▼][●][★][◆][●][⬡]            │
│  [●][◆][⬡][▼][★][⬡][●]            │
│  [★][●][▼][◆][●][★][◆]            │
│  [◆][⬡][★][●][▼][◆][●]            │
│  [▼][●][◆][⬡][★][▼][⬡]            │
│                                     │
└─────────────────────────────────────┘
```
- No result panel exists — the game is endless.
- Tablets may use an 8×8 or 9×9 board to fill the extra space.

## Settings
- **Board size**: 6×6, 7×7 (default), 8×8, 9×9.
- **Gem count** (colors/types on the board): 4, 5, 6 (default) — fewer types make matches easier.
- **Drop speed**: Slow, Normal (default), Fast — controls how quickly replacement gems fall.
- **Show hint** (on/off, default on): after 8 seconds of inactivity, one valid swap is briefly highlighted.

## How to Play
- Tap a gem to select it (it lifts slightly); tap an adjacent gem to swap them.
- If the swap creates a match of 3 or more in a row or column, the matched gems clear and new ones drop in. Cascades fire automatically if the new drops create additional matches.
- If the swap does not create any match, the gems snap back to their original positions.
- There is nothing to win or lose — play as long as you like and close whenever you want.

## Controls
- **Tap** a gem: selects it.
- **Tap** an adjacent gem: performs the swap.
- **Tap** the same gem again: deselects it.
- **Drag** from one gem to an adjacent gem: shortcut swap (optional; same result as two taps).

## Gameplay Rules

### Match Resolution
- Matches of 3 clear all matched tiles simultaneously.
- Matches of 4 clear tiles and spawn a **Line Gem** in the matched row/column (clears entire row or column when matched again).
- Matches of 5 or more clear tiles and spawn a **Nova Gem** (clears all gems of one color when swapped with any gem).
- Line Gems and Nova Gems are purely cosmetic bonuses; they do not change the relaxed tone of the game.
- After tiles clear, gems above fall down to fill gaps. New gems generate at the top to fill remaining empty cells.
- Cascades (chain reactions caused by falling gems landing in matching positions) are resolved fully before the player's next move is accepted.

### Dead-Board Detection
- After every board state change, the controller checks whether any swap would produce a match.
- If no such swap exists, the reshuffle triggers automatically after a 1-second pause.
- The reshuffle preserves gem counts per type and guarantees at least one valid move in the resulting arrangement.

## State Machine
- A dedicated `MatchThreeStateMachine` in `state/` exposes `StateFlow<MatchThreeState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ GemSelected → Playing (selection updated)
 ├─ GemDeselected → Playing (selection cleared)
 ├─ SwapAttempted [creates match] → Resolving
 ├─ SwapAttempted [no match] → Playing (gems bounce back)
 └─ NoMovesAvailable → Reshuffling
Resolving
 ├─ CascadeDetected → Resolving (continue cascade)
 └─ BoardStable + MovesAvailable → Playing
BoardStable + NoMovesAvailable
 └─ → Reshuffling
Reshuffling
 └─ ReshuffleComplete → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Total gems cleared (lifetime) | yes |
| Longest cascade chain (lifetime best) | yes |
| Longest single session (minutes) | yes |
| Nova Gems created (lifetime) | yes |
| Line Gems created (lifetime) | yes |

## HUD
- Top bar: title, lifetime gem count, settings.
- Board: full-bleed grid occupying most of the screen.
- Selected gem indicator: the selected gem lifts and glows with a white outline.
- Hint highlight (if enabled and triggered): a subtle pulse on the two gems involved in the suggested swap.
- Reshuffle indicator: a brief centered "Reshuffling…" text overlay that fades in and out over the board.
- No score, timer, level counter, or progress bar is ever shown.
