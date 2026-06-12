# Tafl Lite — Design Document

## Overview
- Tafl Lite is an 11×11 Hnefatafl-inspired strategy game focused on asymmetric pressure and escape.
- One side controls 24 Attackers entering from the outer formation.
- The other side controls 12 Defenders plus the King in the center.
- The human can play as Attackers, Defenders, or let the game assign a random side.
- Attackers win by surrounding the King on all required orthogonal sides.
- Defenders win by escorting the King to any corner escape square.
- All pieces move like chess rooks across any number of empty orthogonal squares.
- Standard captures occur by sandwiching an enemy piece between two allied pieces.
- The King has special survival rules and requires a full surround, or three sides when trapped at the board edge.
- The digital adaptation must teach asymmetry clearly without reducing the tension of the original contest.
- Training Wheels Mode is a first-class feature and should feel supportive rather than punitive.
- The experience should be crisp, austere, and readable, with strong board-state clarity at a glance.
- The game is entirely offline and stores statistics, side preferences, and hint settings locally.

## Visual Style
- The board uses Material 3 surfaces with a restrained Scandinavian-inspired presentation filtered through the underwater palette.
- Base board surface uses `Aqua0` in light theme and `Dark1` in dark theme, with grid lines pulled from `Aqua3` or softened `Dark2` contrast.
- Corner escape squares use `Aqua2` highlights so they are always legible as special destinations.
- The throne square at center uses `Aqua4` accents and a distinct emblem ring.
- Attackers use darker, heavier tokens with `Dark2` cores and `Aqua1` edge contrast.
- Defenders use brighter tokens with `Aqua1` fill and `Aqua4` outline.
- The King receives the strongest emphasis: elevated disc, crown glyph, and persistent legal-state halo when selected.
- Valid move indicators are circular dots on reachable squares; capture-threat indicators use edge brackets, not arrows.
- Training Wheels feedback uses calm instructional styling, never red error panic.
- Status and evaluation messages sit in the bottom panel to preserve the board's visual integrity.
- Animations are sparse: slide movement, capture fade, throne pulse, and result panel reveal.
- Reduced-motion mode swaps movement slides for instant repositioning.

## Screen Layout
