# Planet Explorer — Design Document

## Overview
- Planet Explorer is a casual single-player 2D side-scrolling exploration game set on one large procedurally generated world.
- The world is divided into distinct biomes, each with its own hand-crafted adventurer character. The player can switch between biomes at any time — from the title screen or from within the game — and the matching adventurer is immediately placed at ground level in that biome.
- Movement starts simple (walk left, walk right, small hop jump) but each biome hides a special **Movement Ability** that permanently expands what every adventurer can do.
- The goal is to fill a **Field Book** by touching every animal and every mineral visible in each biome.
- There are no fail states, no timers, and no enemies. The loop is pure exploration and discovery.
- The game is fully offline, single-device, and local-stat only.

## Biomes and Adventurers

| Biome | Adventurer | Visual Theme |
|-------|-----------|--------------|
| Grassland | Safari Explorer | Tan khaki, wide-brim hat, binoculars |
| Reef | Deep-Sea Diver | Wetsuit, diving mask, oxygen tank |
| Snowfield | Polar Researcher | Insulated parka, goggles, backpack |
| Desert | Archaeologist | Field vest, sun scarf, trowel |
| Cavern | Spelunker | Helmet lamp, rope harness, gloves |
| Jungle | Jungle Scout | Camouflage shirt, machete holster |
| Highland | Mountaineer | Climbing gear, carabiner, ice axe |

New biomes can be added by appending rows to this table and creating the corresponding assets and terrain seed.

## Movement Abilities (Tools)

Each biome contains exactly one hidden Movement Ability. Once found, it is immediately added to **all** adventurers and persists across sessions.

| Tool | Found In | Effect |
|------|---------|--------|
| Shovel | Grassland | Dig straight down through dirt one tile at a time |
| Pick Axe | Cavern | Tunnel diagonally through rock (one tile per tap) |
| Spring Boots | Desert | Perform a high jump (roughly 3× base jump height) |
| Rocket Boots | Snowfield | Hold the jump button to fly upward until released or max height reached |
| Grappling Hook | Jungle | Fire a hook at an angle; swing across gaps and pull to ceilings |
| Flippers | Reef | Move freely through water tiles (base movement is slow in water without these) |
| Crampons | Highland | Cling to and walk up steep ice-covered walls |

Tools are obtained by reaching the tile where the tool item sits (glowing collectible icon). A brief discovery animation plays, and then the tool is available to all adventurers. A biome's tool is only accessible after earning at least one previous tool (directed discovery graph prevents dead-ends).

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- 2D side-scrolling scene with layered parallax backgrounds (far, mid, near layers).
- Terrain tiles: flat pixel-art blocks whose color palette varies by biome (e.g., grassland uses `Aqua2`/`Green2` tops with `Dark2` earth; cavern uses `Dark0`/`Dark1` stone with `Teal3` crystal veins).
- Adventurer sprites: per-biome character with a walking animation (4 frames) and an idle animation (2 frames).
- Animals: small animated sprites unique to each biome (e.g., safari has gazelle, elephant; reef has clownfish, crab).
- Minerals: static tile icons embedded in the terrain (gold vein, amethyst cluster, fossil chunk, etc.).
- Field Book UI: parchment-toned overlay panel; entries appear as hand-drawn silhouette cards that flip to reveal the full illustration when first discovered.
- Tool discovery: golden glow pulse around the tool item; a full-screen flash + tool card reveal animation on collection.
- No blood, damage, or death animations exist anywhere in the game.

## Screen Layout

### Exploration View
```
┌─────────────────────────────────────┐
│  Planet Explorer   📖 12   🌿 ⚙     │  ← Top bar (journal count, biome switcher, settings)
├─────────────────────────────────────┤
│                                     │
│  ≈≈≈≈ sky / background ≈≈≈≈≈≈≈≈≈  │
│                                     │  ← Scrollable terrain (biome-specific)
│  🦒   🌿   [🔩tool]   🪨   🦓     │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓   │  ← Ground / dig-able tiles
│                                     │
├─────────────────────────────────────┤
│  [← Walk] [Jump] [→ Walk] [Tool ▾] │  ← Controls (tool button shows current tool)
└─────────────────────────────────────┘
```

### Biome Switcher (accessible any time from top bar)
```
┌─────────────────────────────────────┐
│  Switch Adventurer          [✕]     │
│  ────────────────────────────────── │
│  ✅ Grassland   Safari Explorer     │
│  ✅ Reef        Deep-Sea Diver      │
│  🔒 Snowfield   Polar Researcher    │  ← biomes unlock after first visit
│  ...                                │
└─────────────────────────────────────┘
```

## Settings
- **World seed**: a text field that accepts an alphanumeric seed; default is a randomized value chosen at first launch and stored locally.
- **Terrain detail**: Low, Medium (default), High — controls the number of background detail layers rendered.
- **Show discovery hints** (on/off, default on): a faint sparkle animation near uncollected animals and minerals when the adventurer is nearby.
- **Show tool hint** (on/off, default on): a subtle glowing outline around the hidden tool item in each biome, visible only when the adventurer is within 5 tiles.
- **Control layout**: D-pad with Jump button (default), Tilt (accelerometer walk, tap jump).

## How to Play
- Walk your adventurer left and right to explore the terrain.
- Tap Jump to hop; use Movement Abilities to reach new areas (tool button appears once the first tool is found).
- Walk into an animal to touch it; it animates briefly and its Field Book entry fills in.
- Walk up to a mineral embedded in a tile and tap the tile (or use the appropriate digging tool) to collect it and fill its Field Book entry.
- Find and collect the hidden Movement Ability in each biome to unlock new traversal options for all adventurers.
- Open the Field Book at any time to see what you have and haven't discovered.
- Switch biomes via the top bar button to explore a different region with a different adventurer.

## Controls
- **Hold ← / →** (walk buttons or tilt): moves the adventurer horizontally.
- **Tap Jump**: performs the base hop (or high jump / fly if Spring Boots / Rocket Boots are unlocked).
- **Tap Tool ▾**: opens the tool selector wheel when multiple tools are unlocked; selected tool activates on the next directional or jump input.
- **Tap a mineral tile** (within reach): attempts to collect it (requires the appropriate tool if it is rock or underground).
- **Walking into an animal**: automatically registers a touch and logs the Field Book entry.
- **Tap 📖**: opens the Field Book overlay.
- **Tap 🌿 biome icon**: opens the biome switcher.

## Gameplay Loop

### World Generation
- The world is generated once from a seed at first launch and never regenerated (unless the player explicitly reseeds in settings).
- Each biome occupies a horizontal region of the world map with a defined width (grassland is widest; cavern is narrowest).
- Terrain height maps are generated per-biome using a deterministic noise function seeded from `(worldSeed, biomeId)`.
- Animals are placed at deterministic positions derived from the seed; they wander a small area but always return to their home position.
- Minerals are embedded in terrain tiles at deterministic positions; digging removes them permanently for that seed (no respawn).

### Field Book
- The Field Book has one section per biome, each listing all discoverable animals and minerals for that biome.
- Undiscovered entries show a silhouette and a placeholder name ("???"); discovered entries show the full illustration, name, and a short flavor description.
- A global counter in the top bar shows total discovered / total discoverable across all biomes.

### Tool Unlocking
- Tools are placed at specific terrain positions within their biome.
- A tool that requires traversal only unlockable with a prior tool is placed reachable via a base route that exists in the procedural terrain (the placement algorithm verifies this with a reachability check).
- When a tool is collected, a brief full-screen "You found the [Tool Name]!" card appears; the game resumes immediately after the player dismisses it.

## State Machine
- A dedicated `PlanetExplorerStateMachine` in `state/` exposes `StateFlow<PlanetExplorerState>`.
```
Idle
 └─ StartGame → LoadingWorld
LoadingWorld
 └─ WorldReady → ExploringBiome
ExploringBiome
 ├─ AdventurerMoved → ExploringBiome (position updated)
 ├─ AnimalTouched → ExploringBiome (field book updated)
 ├─ MineralCollected → ExploringBiome (field book updated, tile removed)
 ├─ ToolFound → ToolDiscovery
 ├─ BiomeSwitcherOpened → BiomeSwitcher
 └─ FieldBookOpened → FieldBookOverlay
ToolDiscovery
 └─ Dismissed → ExploringBiome (all adventurers gain tool)
BiomeSwitcher
 ├─ BiomeSelected → ExploringBiome (new biome, matching adventurer)
 └─ Dismissed → ExploringBiome
FieldBookOverlay
 └─ Closed → ExploringBiome
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Total field book entries discovered | yes |
| Entries per biome (discovered / total) | yes |
| Tools collected (count and list) | yes |
| Biomes visited | yes |
| Total distance walked (tiles) | yes |

## HUD
- Top bar: title, field book counter, biome switcher button, settings.
- Walk and jump controls fixed at the bottom; tool selector button appears once first tool is found.
- Discovery indicator: a small "+" pop that rises and fades over the adventurer when an entry is logged.
- Tool hint glow (if enabled): visible terrain glow near the hidden tool item.
- Field Book overlay: full-screen sheet that slides up from the bottom; the terrain scene dims behind it. Never obscures the game board while the game is running.
