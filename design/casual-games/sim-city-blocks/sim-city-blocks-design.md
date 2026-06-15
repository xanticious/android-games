# Sim City Blocks — Design Document

## Overview
- Sim City Blocks is a single-player city-building game where the player issues high-level commands (buy zones, build facilities, trigger upgrades) but does not place individual tiles. The game's layout engine places new buildings in sensible, algorithmically chosen locations.
- The town starts with a pre-built seed: a railroad running east-west, a grid of dirt roads branching off it, and two Level 1 residential houses. The player begins with a modest cash reserve.
- The game is **not** an idle game. It has active failure conditions: running out of money, unemployed residents leaving, underpowered buildings going dark, and random disasters.
- The player's job is to maintain a balanced, growing city: residential buildings require jobs from commercial and industrial zones; commercial and industrial zones require workers from residential buildings; every structure consumes energy that must be upgraded over time.
- The game is fully offline, single-device, and local-stat only.

## Player Actions

### Buy Zones
| Button | Effect | Cost |
|--------|--------|------|
| Buy Residential Zone | Adds one Level 1 house at a sensible empty lot | Low |
| Buy Commercial Zone | Adds one Level 1 shop near residential areas | Medium |
| Buy Industrial Zone | Adds one Level 1 factory near the railroad/roads | High |

### Build Civic Buildings
| Button | Effect | Cost |
|--------|--------|------|
| Build Fire Station | Reduces fire disaster damage radius | High |
| Build Police Station | Reduces crime drain on commercial income | High |
| Build Park | Increases nearby residential happiness; generates no income | Medium |

### Upgrade
| Button | What Upgrades |
|--------|---------------|
| Upgrade Roads | Dirt roads → Paved roads: reduces commute penalty, boosts happiness |
| Upgrade Railroads | Single track → Double track: increases industrial income cap |
| Upgrade Residential | Bumps all Level 1 houses → Level 2 (more population capacity) |
| Upgrade Commercial | Bumps all Level 1 shops → Level 2 (more revenue per worker) |
| Upgrade Industrial | Bumps all Level 1 factories → Level 2 (more jobs, more energy demand) |
| Upgrade Parks | Expands park radius and happiness bonus |
| Upgrade Energy | Raises the city's total power capacity (required to support upgraded buildings) |

Upgrades apply globally to the current tier of each type. Each upgrade tier costs progressively more. Upgrading requires sufficient funds and, in some cases, prerequisite upgrades.

## Layout Engine
- When the player purchases a zone, the layout engine selects the best available empty cell based on weighted criteria:
  - **Residential**: near parks, roads, and existing residences; away from industrial zones.
  - **Commercial**: near residential zones and roads; not adjacent to industrial zones.
  - **Industrial**: near the railroad or a major road intersection; away from residential zones.
  - **Fire Station / Police Station**: maximizes coverage radius over existing buildings.
  - **Park**: fills gaps between residential blocks; maximizes number of residential neighbors.
- If no suitable cell is available (city is fully built out), the buy button is disabled with a "No room" indicator.
- The layout engine never places two buildings of the same type side-by-side if a better spread is possible.

## Starting State
- A railroad runs across the center of the grid (east-west).
- Dirt roads branch north and south at two intersections, forming a small neighborhood grid.
- Two Level 1 residential houses are pre-placed on road-adjacent lots.
- Starting cash: enough to buy roughly 3–4 additional zones or one civic building.
- Energy capacity starts at Level 1, supporting only the initial buildings.

## Balance and Failure Conditions

### Employment Loop
- Each commercial and industrial zone has a **worker demand** (number of residents it employs).
- Each residential zone contributes **residents** (workers available to commute).
- If a business has zero available workers, it generates no income and becomes idle (shown with a grey overlay).
- If residents have no jobs available in the city, their happiness drops over time. If happiness falls to zero for a residential zone, that zone is abandoned (removed from the city, reducing population).

### Energy
- Every building consumes energy. Total consumption is tracked against total capacity.
- When consumption exceeds capacity, buildings are randomly powered off in a rolling brownout (shown with a flickering icon). Unpowered buildings generate no income and do not provide services.
- Upgrading Energy increases total capacity. Energy upgrades are required before most zone upgrades become available.

### Finances
- Each city cycle (one in-game day, roughly 30 real seconds by default) income and expenses are computed.
  - **Income**: revenue from commercial and industrial zones (based on workers employed and level).
  - **Expenses**: maintenance for all buildings (roads, railroad, civic buildings), energy costs, and zone upkeep.
- If the budget balance goes negative and remains negative for 3 consecutive cycles, the player cannot purchase new zones until revenue recovers.
- If balance stays critically negative for 10 consecutive cycles, the game enters a deficit spiral warning — the player has a grace period to fix finances before the game ends.

### Random Disasters
- Disasters fire at random intervals (configurable frequency in settings). Each disaster affects a region of the city.
  - **Fire**: destroys 1–3 adjacent buildings. A nearby Fire Station reduces the affected radius.
  - **Economic Slump**: reduces commercial income by 50% for 3 cycles.
  - **Blackout**: temporarily disables Energy Level 1 buildings for 2 cycles.
  - **Exodus**: one random residential zone loses half its population for 2 cycles (temporarily reducing worker supply).
- Disaster events appear as a notification banner; the affected area is highlighted on the city grid.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Top-down city grid with a subtle tile texture.
- Building icons use flat silhouettes:
  - Residential (Level 1): small house; Level 2: two-story house.
  - Commercial (Level 1): single-window shop; Level 2: multi-window storefront.
  - Industrial (Level 1): single smokestack; Level 2: two smokestacks.
  - Fire Station: red/white icon. Police Station: blue badge icon.
  - Park: green tree. Railroad: parallel lines. Roads: grey strips.
- Happiness indicators float above residential zones (green / yellow / red face).
- Energy warning: buildings flash amber when on brownout rotation.
- Abandoned zone: building icon fades to grey and crumbles briefly before disappearing.
- Disaster highlight: a `Coral` glow pulses over affected tiles.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Sim City Blocks  Pop:120  $1,450 ⚙ │  ← Top bar (population, budget, settings)
├─────────────────────────────────────┤
│  ⚡ Energy: 74% used  📅 Day 14      │  ← Status strip
├─────────────────────────────────────┤
│                                     │
│  [🏠][🏠][🌳][🏪][🏭][🏭]          │
│  ══════════════════════════         │  ← Railroad
│  [🏠][🏠][🏪][🏠][🏪][  ]          │  ← City grid (scrollable)
│  [  ][🏠][  ][🏠][  ][  ]          │
│                                     │
├─────────────────────────────────────┤
│ [🏠 Buy Res] [🏪 Buy Com] [🏭 Buy Ind] │
│ [🚒 Fire] [🚓 Police] [🌳 Park]     │  ← Action buttons
│ [⬆ Upgrade ▾]                       │  ← Upgrade expandable row
└─────────────────────────────────────┘
```
- Tapping an existing building opens a detail panel below the grid showing that building's stats (employment, income, energy use, level).
- The upgrade button expands to show all available upgrade options with costs.

## Settings
- **Cycle speed**: Slow (60 s/day), Normal (30 s/day, default), Fast (15 s/day).
- **Disaster frequency**: Off, Rare (default), Occasional, Frequent.
- **Show job/population overlay** (on/off, default on): floating employment indicators over each zone.
- **Show energy overlay** (on/off, default off): overlays energy consumption per building.

## State Machine
- A dedicated `SimCityBlocksStateMachine` in `state/` exposes `StateFlow<SimCityState>`.
```
Idle
 └─ StartGame → Running
Running
 ├─ ActionTaken [buy/build/upgrade] → Running (layout engine places building, stats updated)
 ├─ CycleAdvanced → Running (income, expenses, happiness recalculated)
 ├─ DisasterTriggered → Running (affected area highlighted, stats adjusted)
 ├─ ZoneAbandoned → Running (zone removed, population reduced)
 └─ DeficitCritical [10 cycles] → GameOver
GameOver
 ├─ NewGame → Idle
 └─ Continue (load last save) → Running
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Peak population achieved | yes |
| Peak budget balance achieved | yes |
| Longest run (in-game days) | yes |
| Disasters survived | yes |
| Total zones built (lifetime) | yes |

## HUD
- Top bar: title, population count, current budget balance, settings.
- Status strip: energy utilization percentage, current in-game day.
- City grid: buildings with happiness indicators; scrollable on large grid sizes.
- Action button row: buy and build buttons with cost labels; disabled buttons show reason (e.g., "No room", "Need funds", "Need Energy Upgrade").
- Upgrade expand row: tapping shows a scrollable list of upgrades with costs and prerequisite indicators.
- Detail panel: slides up from below the grid when a building is tapped; shows the building's current stats without covering the grid.
- Disaster notification: a banner slides in from the top for 3 seconds when a disaster fires.
- Deficit warning: a persistent red banner below the status strip when the budget has been negative for 3+ cycles.
