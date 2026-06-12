# Qwirkle Lite — Design Document

## Overview
- Qwirkle Lite is a streamlined digital adaptation of the tile-laying game for one human player versus one, two, or three AI opponents.
- The tile set contains 6 shapes and 6 colors for 36 unique tiles, with 3 copies of each for a 108-tile bag.
- Every played line must share exactly one attribute.
- Valid lines are either all the same color with all different shapes, or all the same shape with all different colors.
- Duplicate exact tiles are never allowed in a single line.
- A line may contain at most 6 tiles.
- Completing a 6-tile line is a Qwirkle and awards 12 points total for that line.
- Each player starts with a hand of 6 tiles.
- On a turn, a player either places one or more tiles into a valid connected line extension, or trades selected tiles with the bag.
- The goal is to finish with the highest score after the bag empties and no player can continue.
- The tone should feel tactile, thoughtful, and readable, with strong placement guidance and score transparency.

## Visual Style
- Use Material 3 surfaces with the underwater palette from `ui/theme/Color.kt`.
- Screen background uses `Dark0`; cards, trays, and panel shells use `Dark1` and `Dark2`.
- The board grid uses `Aqua0` for low-contrast cells so tile silhouettes remain dominant.
- Valid placement markers use `Aqua2`; selected hand tiles use an `Aqua3` lift ring.
- Multi-tile pending placements connect with a faint `Aqua1` path line to show the active sequence.
- Score chips and action buttons use `Aqua3` as the primary accent.
- Tile faces are clean, high-contrast, and icon-driven; the underlying hand tray keeps enough padding that symbols remain readable on phones.
- Shapes and tile colors remain part of the game identity, while the surrounding UI chrome stays within the theme-token palette.
- The overall feel should evoke a polished tabletop set resting on a calm ocean-depth backdrop.

## Screen Layout
