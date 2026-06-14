# Planet Explorer — Design Document

## Overview
- Planet Explorer is a casual single-player exploration game. The player pilots a small spacecraft through a solar system of procedurally generated planets and moons, landing on each to scan, collect, and catalog alien specimens.
- There are no combat mechanics and no fail states. Fuel is unlimited. The experience is driven by curiosity and collection.
- Each planet visit follows the same loop: land, explore the surface on foot, find scannable flora and fauna, collect one sample of each type, and return to the ship.
- Discoveries are logged in a growing Field Journal that persists between sessions.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Space background: deep `Dark0` with scattered star sprites and a distant nebula gradient using `Aqua4` and `Teal3` tones.
- Planets: procedurally colored spheres using palette tones; surface biome affects color saturation (icy = desaturated blues, volcanic = warm reds/oranges from `Coral` range, lush = `Aqua2`/`Green2` tones).
- Ship interior (HUD frame): minimal metallic panel UI strip at top and bottom.
- Surface exploration: side-scrolling pixel-art-style terrain rendered with a flat, vibrant palette.
- Flora/fauna sprites are hand-authored assets with palette-swapped color variants generated procedurally.
- Scanning animation: concentric ring pulse in `Aqua3` emanating from the scanner crosshair.

## Screen Layout

### Space View (navigation between planets)
```
┌─────────────────────────────────────┐
│  Planet Explorer   Journal: 12  ⚙   │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│   ○  ◌   ●  ◉                       │  ← Solar system map (tap a body to travel)
│      ◌         ◉  ◌                 │
│                  ☆ ← ship           │
│                                     │
├─────────────────────────────────────┤
│  [  Land  ]   [Journal]  [Map]      │  ← Action bar
└─────────────────────────────────────┘
```

### Surface View (exploration on a planet)
```
┌─────────────────────────────────────┐
│  🌍 Kelpius IV   Found: 2/5    ⚙    │  ← Top bar (planet name + scan goal)
├─────────────────────────────────────┤
│  ~~~terrain~~~  [flora] [fauna]     │
│                                     │  ← Scrollable side-scrolling surface
│ [🚀ship]                            │
├─────────────────────────────────────┤
│  [← Move] [Scan] [→ Move] [Depart] │  ← Controls
└─────────────────────────────────────┘
```

## Settings
- **Solar system size**: Small (5 bodies), Medium (10, default), Large (20).
- **Specimens per planet**: 3 (easy), 5 (default), 8 (thorough).
- **Scanner hint** (on/off, default on): a faint sparkle effect near unscanned specimens visible in surface view.
- **Surface scroll speed**: Slow, Normal (default), Fast.

## How to Play
- From the solar system map, tap any planet or moon to fly the ship to it.
- Tap Land to descend to the surface.
- Walk left or right using the move buttons to explore the terrain.
- When you see a specimen (flora or fauna), approach it and tap Scan to scan and collect it.
- Each unique specimen type logs one entry in the Field Journal.
- After collecting all specimens on a planet (or whenever you like), tap Depart to return to space.
- Visit other bodies to discover more specimens and fill the journal.

## Controls
- **Tap** a planet/moon on the map: selects destination and flies the ship there (instant travel).
- **Tap Land**: transitions from space view to surface view.
- **Hold ← / →** move buttons: walks the explorer left/right.
- **Tap Scan**: scans the nearest specimen within range (range indicator shown as a small arc under the explorer).
- **Tap Depart**: leaves the surface and returns to space view.
- **Tap Journal**: opens the Field Journal overlay (specimen list, notes, collection counts).
- **Tap Map**: returns to the full solar system map from any planet.

## Gameplay Loop

### Planet Generation
- Each planet is created with a seed derived from its position in the system.
- Parameters generated per planet: biome type (icy/lush/volcanic/desert/ocean), surface terrain height map, specimen pool (3–8 types selected from the master asset list with palette swaps), background sky color.
- Moons are mini-planets with 1–3 specimens and a narrower terrain band.

### Surface Exploration
- The surface is a looping horizontal strip (~10 screen widths long). Scrolling past the right edge wraps back to the left.
- Specimens are placed at fixed positions for the planet's seed; positions are stable across visits.
- Already-collected specimens are shown as faint outlines (not re-scannable).
- Scanner range is a short fixed radius; moving near a specimen illuminates the Scan button.
- A found/total counter in the top bar tracks progress on the current surface.

### Field Journal
- Each unique specimen has an entry: name (procedurally generated), planet of first discovery, illustration (asset + palette swap), and a one-line flavor description.
- Journal entries are sorted by discovery order; a filter by planet or biome type is available.
- Total unique specimens discovered is shown on the Journal button in the space view.

## State Machine
- A dedicated `PlanetExplorerStateMachine` in `state/` exposes `StateFlow<PlanetExplorerState>`.
```
Idle
 └─ StartGame → SpaceView
SpaceView
 ├─ PlanetSelected → Traveling
 └─ JournalOpened → JournalOverlay
Traveling
 └─ ArrivedAtPlanet → SpaceView (ship at new position)
SpaceView (at planet)
 └─ LandingInitiated → SurfaceView
SurfaceView
 ├─ ExplorerMoved → SurfaceView (position updated)
 ├─ SpecimenScanned → SurfaceView (journal updated)
 ├─ AllSpecimensFound → SurfaceView (depart prompt shown)
 └─ Departed → SpaceView
JournalOverlay
 └─ JournalClosed → SpaceView / SurfaceView
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Total unique specimens discovered | yes |
| Planets fully explored (all specimens) | yes |
| Total planets visited | yes |
| Rarest specimen found (fewest planets visited at time of find) | yes |

## HUD
- Space view top bar: title, total journal entries, settings.
- Surface view top bar: planet name, specimens found on this visit / total on planet.
- Scanner button pulses when a specimen is in range.
- Journal overlay lists all discovered specimens with planet of origin; tappable entries show the full entry.
- No timers or score counters are shown at any time.
