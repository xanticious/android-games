# Treasure Mapper — Design Document

## Overview
- Treasure Mapper is a single-player casual exploration game structured as a series of layered map puzzles.
- Each level presents a hand-drawn map of a location (jungle, desert ruins, undersea cave, mountain pass, etc.). The map contains riddles, symbols, and directional clues that guide the player to one or more buried treasure caches.
- The player follows the map, solves a small environmental puzzle at the dig site, and uncovers the cache to complete the level.
- Levels are grouped by location; each location has 3–5 maps of increasing complexity.
- There are no time limits and no lives. The experience is unhurried exploration and puzzle solving.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Map view: parchment-toned background (`#F5E6C8` warm cream, defined in `ui/theme/Color.kt`) with sepia ink illustrations.
- Map symbols: compass rose, X-marks, dotted trail lines, landmark icons (tree, rock, arch, well).
- Location environment view (exploration): a top-down pixel-art scene matching the map's setting.
- Player character: a small explorer sprite with a lantern.
- Dig sites: marked with a glowing shovel icon when the player is adjacent.
- Treasure cache reveal: a sparkle burst animation followed by the cache opening with the contents shown.
- All colors for UI chrome and action chips derive from `ui/theme/Color.kt`; no hex values outside that file.

## Screen Layout

### Map View
```
┌─────────────────────────────────────┐
│  Treasure Mapper   Location 2  ⚙    │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│     [parchment map illustration]    │  ← Zoomable map image (pinch to zoom)
│    ↓ N     🌊  🌴  🗿  X?           │
│                                     │
├─────────────────────────────────────┤
│  Clue: "Three paces east of the     │
│  stone arch, where the roots part." │  ← Clue strip (scrollable for multi-line)
│  [Explore →]                        │  ← Button to enter exploration view
└─────────────────────────────────────┘
```

### Exploration View
```
┌─────────────────────────────────────┐
│  Jungle Ruins   Caches: 0/2    ⚙    │  ← Top bar (cache progress)
├─────────────────────────────────────┤
│                                     │
│   🌴 🗿  👤  🌴                     │  ← Top-down tile scene (scrollable)
│   🌿      ⛏← dig site              │
│        🌴     🗿                    │
│                                     │
├─────────────────────────────────────┤
│  [←][↑][↓][→]         [Map / Clue] │  ← Move controls + map toggle
└─────────────────────────────────────┘
```

## Settings
- **Move control style**: D-pad (default), Swipe gestures.
- **Show footprints** (on/off, default on): faint trail marks showing where the player has walked.
- **Clue font size**: Small, Medium (default), Large.
- **Map overlay** (on/off, default on): a tap-to-view map button always visible in the exploration view.

## How to Play
- Start in the Map View for the current level. Study the map and the clue to understand where the treasure is.
- Tap **Explore** to enter the environment.
- Walk to the indicated dig site (follow map landmarks and clue directions).
- When adjacent to a dig site, a shovel icon appears. Tap it to start the dig puzzle.
- Solve the small dig puzzle (see Dig Puzzles below) to uncover the cache.
- Some levels have multiple caches; collect all caches to complete the level.
- Tap **Map / Clue** at any time to review the map and current clue without leaving the environment.

## Dig Puzzles
- Each cache is protected by one of three puzzle types chosen based on location theme:
  - **Pattern Trace**: follow a sequence of directional arrows shown briefly (2 seconds), then reproduce the sequence by tapping arrows. Sequence length scales with level difficulty.
  - **Symbol Match**: a small 3×3 grid of symbols; rearrange them to match the pattern shown on the map. (Sliding-tile variant for harder levels.)
  - **Key Sequence**: a lock with 4–6 tumblers; each tumbler cycles through symbols; tap tumblers to match the combination shown in the clue. Tumblers can be reset freely.
- Dig puzzles have no time limit and no fail state — the player may retry indefinitely.

## Level Progression
- Locations unlock in order (Location 1 → 2 → 3 → …); within a location, maps unlock in sequence.
- A location is fully explored when all its maps are completed (all caches found).
- Replay any completed map at any time from the location menu.
- Future locations are shown as locked with a silhouette preview.

## State Machine
- A dedicated `TreasureMapperStateMachine` in `state/` exposes `StateFlow<TreasureMapperState>`.
```
Idle
 └─ StartLevel → MapView
MapView
 ├─ MapZoomed → MapView (view state updated)
 └─ ExploreSelected → ExplorationView
ExplorationView
 ├─ ExplorerMoved → ExplorationView (position updated)
 ├─ DigSiteReached → DigPuzzle
 ├─ MapToggled → MapView (returns to ExplorationView on close)
 └─ AllCachesFound → LevelComplete
DigPuzzle
 ├─ PuzzleSolved → ExplorationView (cache collected, count updated)
 └─ PuzzleRetried → DigPuzzle (reset puzzle state)
LevelComplete
 ├─ NextMap → MapView (next map in location)
 ├─ NextLocation → MapView (first map of next location)
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Maps completed per location | yes |
| Total caches found (lifetime) | yes |
| Locations fully explored | yes |
| Dig puzzle retries (fewest for a completion) | yes |
| Total exploration tiles walked | yes |

## HUD
- Map view: top bar with location name and settings; clue strip below map; Explore button.
- Exploration view: top bar with location name and cache progress; move controls; map toggle.
- Dig site indicator: shovel icon appears above the player sprite when adjacent to a dig site.
- Dig puzzle: occupies the lower portion of the screen over the exploration view; the exploration scene remains partially visible above it.
- Level complete panel below the exploration scene: caches found, puzzle retries, next actions.
