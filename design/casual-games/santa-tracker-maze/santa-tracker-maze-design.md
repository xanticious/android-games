# Santa Tracker Maze — Design Document

## Overview
- Santa Tracker Maze is a holiday-themed single-player maze navigation game.
- The player guides Santa's sleigh through a series of procedurally generated top-down maze levels, collecting wrapped gifts along the route before exiting through the chimney goal tile.
- Difficulty scales with maze size and collectible count. There are no enemies and no lives — the only challenge is navigating the maze and finding all gifts efficiently.
- Seasonal visual themes (winter forest, rooftop snow, toy workshop) rotate across levels.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`, extended with holiday accent tokens: Candy Red (`#D63031`), Holly Green (`#00B894`), Warm Gold (`#FDCB6E`). These extended tokens are defined only in `ui/theme/Color.kt`.
- Maze walls: thick rounded outlines in the current theme's accent color against a `Dark1` floor tile.
- Santa sprite: a small sleigh-and-reindeer icon that rotates to face the direction of travel.
- Gifts: bright wrapped-present icons with randomly assigned ribbon colors from the palette.
- Chimney goal: a glowing chimney icon with a pulsing `Aqua2` smoke ring.
- Collected gift animation: a brief sparkle burst, then the gift fades from the tile.
- Level complete: snowflake burst animation from the chimney; the result panel appears below the maze.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Santa Maze   Level 4   🎁 2/5  ⚙  │  ← Top bar (level, gifts collected/total)
├─────────────────────────────────────┤
│                                     │
│   ┌─┬───┬─┐                         │
│   │S│   │ │                         │  ← Maze (scrollable/zoomable for large mazes)
│   │ └─┐ │ │   🎁      🎁            │
│   │   │ └─┘                         │
│   └───┴─🏠┘   ← chimney goal        │
│                                     │
├─────────────────────────────────────┤
│  [←][↑][↓][→]   (or swipe gestures) │  ← D-pad controls (toggleable with swipe)
└─────────────────────────────────────┘
```
- The maze fits the screen on small levels; large levels scroll as the sleigh approaches the edge.
- The result panel (time, gifts, moves) appears below the maze after level completion.
- Tablets show the full maze at once for small/medium sizes.

## Settings
- **Maze size**: Small (10×10), Medium (16×16, default), Large (24×24).
- **Gift count**: 3, 5 (default), 8.
- **Control style**: D-pad buttons (default), Swipe gestures.
- **Show minimap** (on/off, default off): a small corner minimap revealing explored paths.
- **Show move counter** (on/off, default on): counts total moves taken.

## How to Play
- Navigate Santa's sleigh through the maze using the d-pad or swipe gestures.
- Pick up all wrapped gifts scattered through the maze by moving over them.
- Find the chimney (goal tile) and move onto it to complete the level.
- All gifts must be collected before the chimney accepts Santa — it glows red/locked until all gifts are gathered, then turns gold/open.
- Try to complete each level in as few moves as possible.

## Controls
- **Tap** d-pad arrows (or swipe in that direction on the maze): moves Santa one tile in that direction.
- Santa cannot move through walls; invalid moves are silently ignored (no error feedback).
- **Tap** minimap toggle (top bar): shows/hides the corner minimap (if enabled in settings).
- **Tap** hint button (top bar, one per level): Santa briefly glows and a short path toward the nearest uncollected gift is shown for 2 seconds.
- After level completion, tap **Next Level** or **Menu** from the result panel.

## Gameplay Loop

### Maze Generation
- Mazes are generated using a randomized depth-first search algorithm, guaranteeing every cell is reachable.
- Gifts are placed in dead-end cells first, then in cells at least 3 path-moves from the start, to ensure interesting navigation.
- The chimney goal is placed in a dead-end cell maximally far from the start (using longest-path heuristic).
- Each level's seed is deterministic from `(levelNumber, mazeSizeEnum)` so a given level always generates the same maze.

### Scoring
- Primary ranking: fewest moves to collect all gifts and reach the chimney.
- Secondary ranking: fastest time.
- Each level records the player's best move count and time.

### Level Progression
- Levels are numbered 1–∞; the maze size setting is fixed for a run.
- After each level, maze complexity increases slightly (more dead ends, longer critical path).
- The player may change settings and restart from Level 1 at any time.

## State Machine
- A dedicated `SantaMazeStateMachine` in `state/` exposes `StateFlow<SantaMazeState>`.
```
Idle
 └─ StartGame → Generating
Generating
 └─ MazeReady → Playing
Playing
 ├─ MoveMade [valid, no gift] → Playing (position updated)
 ├─ MoveMade [valid, gift tile] → Playing (gift collected, counter updated)
 ├─ MoveMade [invalid] → Playing (no change)
 ├─ HintRequested → Hinting
 └─ AllGiftsCollected + OnGoalTile → LevelComplete
Hinting
 └─ HintExpired → Playing
LevelComplete
 ├─ NextLevel → Generating
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Levels completed | yes |
| Best move count per level | yes |
| Best time per level | yes |
| Hints used per level | yes |
| Total gifts collected (lifetime) | yes |

## HUD
- Top bar: title, current level, gift counter (`collected/total`), settings.
- Maze area: Santa sprite, gift sprites, chimney sprite, wall tiles.
- Move counter (if enabled): small counter below top bar or inline.
- Minimap (if enabled): small overlay in a corner, showing explored cells.
- Result panel below maze after level complete: moves taken, time, best move count, best time.
