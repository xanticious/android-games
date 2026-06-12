# Hex — Design Document

## Overview
Hex is a connection strategy game played on a rhombus of hexagonal cells where each side tries to connect its opposite borders.
The default board is 11×11, with smaller and larger options for pacing and difficulty variation.
There are no draws in Hex, so every valid match produces a winner once a complete path is formed.
This version includes the optional swap rule, letting the second player claim the first move's position after turn one to balance opening advantage.
The design should make edge goals unmistakable, keep touch input simple, and clearly reveal the winning path at match end.
All play is offline against local AI, with stats kept only on the device and navigation handled by the app's state machines.
Result text and actions remain below the board, preserving a full view of the finished network.

## Visual Style
- Material 3 layout with the underwater palette from `ui/theme/Color.kt`.
- The board frame uses `Dark1` and `Aqua4`, with edge goal bands tinted differently for the two players.
- Blue and red player choices are adapted into the app palette through cool/warm themed fills while still harmonizing with `Aqua2`, `Aqua3`, and `Dark2` UI surfaces.
- Empty cells use a muted `Dark2` face so placed stones read immediately.
- The currently targetable cell can lift slightly with an `Aqua1` outline.
- The last placed cell gets a crisp `Aqua3` border marker.
- When the swap rule is active, the first-move cell stays specially marked until the second player chooses swap or decline.
- Winning connections animate as a glowing linked path using `Aqua3` accents along the connected chain.
- Edge labels should remain subtle but always visible so players understand which sides they are trying to join.

## Screen Layout
