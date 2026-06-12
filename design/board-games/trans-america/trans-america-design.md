# Trans-America — Design Document

## Overview
- Trans-America is a route-building race in which each player tries to connect five secret destination cities on a shared North American rail map.
- The app supports one human player against 1, 2, 3, or 4 local AI opponents.
- Every turn, a player places either one standard railway segment on non-mountain terrain or one mountain crossing worth two segment cost.
- All regular track contributes to a shared rail network that every player may travel through.
- With the Personal Tracks expansion enabled, each player also owns two personal segments that only that player may traverse.
- Personal tracks create private shortcuts and denial opportunities without breaking the core shared-network identity.
- The first player to connect all five of their destination cities wins immediately.
- Losing players are scored by how many of their five cities remain unconnected at the moment of victory.
- The Android adaptation must make route legality obvious while preserving the satisfaction of tracing a growing rail web.
- The map should feel strategic and readable, not cartographically dense.
- Secret destination cards belong only to the local human and must remain hidden from AI UI summaries.
- All results, unlock-free statistics, and preferences remain local to the device.
- Victory and defeat presentation follows `design/common/victory-defeat.md`; the map remains visible when the result panel appears.

## Visual Style
- Use Material 3 styling with a stylized map surface that fits the underwater palette rather than photoreal terrain.
- Ocean and off-map areas use `Dark0` or `Aqua4` toned backgrounds depending on theme.
- Plains route lanes use understated `Aqua1` connectors with higher-contrast node outlines.
- Mountain routes use stronger `Aqua3` or `Dark2` contrast plus a terrain icon so the doubled cost is obvious.
- City nodes use elevated circular chips, with connected destination cities gaining an `Aqua2` completion glow.
- Shared regular track uses a neutral high-contrast rail line that every player can read as public infrastructure.
- Personal tracks are color-coded per player using token combinations derived from palette tokens and labels, not raw hex values.
- The human player's personal track should always feel distinct and collectible, with a visible remaining-count badge.
- Destination cards use clean card stacks with city names, region hints, and connection checkmarks.
- The overall tone should feel modern board-game premium rather than simulation-heavy.
- Map zoom and pan should be smooth but restrained.
- Reduced-motion mode removes track-laying animations and snaps segments into place.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Trans-America   Your turn      [⚙]  │  ← Top bar
├─────────────────────────────────────┤
│ Segments: 1 regular / mountain costs │  ← Turn allowance
├─────────────────────────────────────┤
│  Vancouver ─── Calgary      Seattle  │
│       ╲        ╱   ▲M          │      │
│        Portland ─── Salt Lake  │      │  ← Zoomable rail map
│            ╲       ╱      Denver      │
│ Chicago ─── Omaha ─── Kansas City     │
│    │          ╲        ╱              │
│ New York ─── Pittsburgh ─── Atlanta   │
├─────────────────────────────────────┤
│ Your cities: SEA ✓  DEN ·  ATL · ... │  ← Secret destination cards
├─────────────────────────────────────┤
│ AI progress: Bot A 3/5  Bot B ?/5    │  ← Public summaries only
└─────────────────────────────────────┘
```
- Portrait uses a zoomable map with destination cards below; tablets may show destination cards and opponent summaries in side panels.

## Settings
- **AI opponents**: 1, 2, 3, or 4.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Personal Tracks expansion** (on/off, default off).
- **Show route hints** (on/off, default on for Easy): highlights legal segments and mountain costs.
- **Map labels**: Full, Compact, or Hidden until zoomed.
- **Fast AI turns** (on/off): skips per-segment AI animation while retaining final placement feedback.

## How to Play
- Each player receives five secret destination cities.
- On your turn, place one regular rail segment or one mountain segment if your turn allowance can pay its cost.
- Regular track is shared, so any player can use rail already built by anyone.
- Personal tracks, when enabled, can be used only by their owner.
- A destination is connected when it can trace a continuous route through usable track to the player's network.
- The first player to connect all five destination cities wins immediately.
- Other players are ranked by how many destination cities remain unconnected.

## Controls
- Drag or tap between adjacent city nodes or route points to preview a segment.
- Confirm the highlighted segment to place it.
- Pinch to zoom and drag to pan the map.
- Tap a destination card to center the map on that city.
- Long-press a route to explain whether it is legal, mountain terrain, shared, or personal.

## AI Opponents
- **Easy**: builds toward the nearest destination with little sharing awareness.
- **Medium**: piggybacks on shared rails, avoids inefficient mountain crossings, and uses personal tracks for obvious shortcuts.
- **Hard**: evaluates multi-city connection clusters, times personal tracks for maximum leverage, and blocks shared opportunities when it does not slow its own route.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per opponent count and difficulty | yes |
| Average unconnected cities when winning | yes |
| Fastest win by turns | yes |
| Personal tracks used per match | yes |
| Mountain segments placed | yes |

## State Machine
- A dedicated `TransAmericaStateMachine` in `state/` exposes `StateFlow<TransAmericaState>`.
```
Idle
 └─ MatchStarted → DealingDestinations
DealingDestinations
 └─ DestinationsAssigned → PlayerTurn / AiTurn
PlayerTurn
 ├─ SegmentPreviewed → SegmentPreview
 ├─ InvalidSegmentExplained → PlayerTurn
 └─ Surrendered → GameOver
SegmentPreview
 ├─ SegmentConfirmed → ResolvingPlacement
 └─ PreviewCancelled → PlayerTurn
AiTurn
 └─ AiSegmentChosen → ResolvingPlacement
ResolvingPlacement
 ├─ PlayerConnectedAll → GameOver
 ├─ AiConnectedAll → GameOver
 └─ NextTurn → PlayerTurn / AiTurn
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `TransAmericaRules` controller validates segment placement, terrain cost, shared versus personal track connectivity, destination completion, and winner detection; route-finding tests run without Android imports.

## HUD
- Top bar shows active player, round position, and settings access.
- Turn allowance strip explains whether the current placement can cross mountains.
- Destination card row shows only the human player's secret cities and completion checkmarks.
- Opponent summary shows public progress without revealing hidden destination names.
- Result panel ranks all players below the map.
