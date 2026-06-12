# Chinese Checkers — Design Document

## Overview
Chinese Checkers is a six-player race game presented as 1 human versus 5 AI opponents on a fully populated six-point star.
Each player starts with 10 marbles in one triangular home area and tries to be the first to fill the opposite triangle.
Moves follow standard rules: one adjacent step or a chain of hops over any adjacent occupied piece, regardless of color.
There is no capture; the drama comes from traffic, lane control, and timing long hop chains.
The human selects one color before the match and the five remaining colors are assigned to AI opponents.
Every AI opponent has its own difficulty setting so mixed-skill tables are possible.
Training Wheels mode can optionally restrict the player to stronger moves only.
All results and local stats stay on device.

## Visual Style
Use Material 3 with the underwater palette from `ui/theme/Color.kt`.
Background surfaces rely on `Dark0` and `Dark1`, while the star board rests on a crisp `Dark1` panel.
Connection lines, board nodes, and destination outlines use `Aqua3`; readable text uses `Aqua0`.
Selection rings, valid hops, and route previews use `Aqua2`, with softer helper accents in `Aqua1`.
Progress meters and turn emphasis use `Aqua4` tracks with `Aqua2` fills.
Player marbles keep their own game colors, but surrounding chrome, highlights, and status UI stay tied to the token palette.
Animations are short and tactile, and any result celebration appears below the board rather than over it.

## Screen Layout
