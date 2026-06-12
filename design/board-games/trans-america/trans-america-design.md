# Trans-America — Design Document

## Overview
- Trans-America is a route-building race in which each player tries to connect five secret destination cities on a shared North American rail map.
- The app supports one human player against 1, 2, 3, or 4 AI opponents.
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
