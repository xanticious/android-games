# Connect Four — Design Document

## Overview
Connect Four is a fast, readable vertical alignment game built for short single-player sessions against local AI.
The default board is the classic 7-column by 6-row grid, with an optional Wide variant for extra strategic space.
A move is always one tap: choose a column and let the disc fall to the lowest open cell.
The design focuses on strong hover previews, clear column targeting, and a satisfying finish when four connected discs win.
All state remains local to the device, and the game fits the app's simple screen progression of settings, how-to-play, match, and results.
Because the board resolves quickly, the UI should feel crisp, immediate, and highly legible at phone size.
No result element may cover the grid; the end state is communicated below the board while the winning pattern remains visible.

## Visual Style
- Material 3 cards and buttons using the underwater palette from `ui/theme/Color.kt`.
- Board shell uses `Dark1` with `Aqua4` edge accents and recessed circular slots.
- Neutral slot backgrounds use `Dark0` so the player pieces remain vivid.
- Red and yellow player choices are stylized as warm/cool themed discs rendered against the underwater UI shell; outlines use `Aqua0` or `Dark0` for contrast.
- Hover preview uses a semi-transparent disc with `Aqua2` glow and subtle bob animation.
- Winning four cells receive a bright `Aqua3` halo plus a soft pulse.
- Text and HUD cards use `Aqua0` on dark surfaces, with `Aqua1` for secondary information.
- Light theme can brighten surrounding surfaces, but the board itself keeps strong slot contrast.
- Disc drop animation should feel weighty, with a quick settle bounce on landing.

## Screen Layout
