# Reversi — Design Document

## Overview
- Reversi is a standard 8×8 abstract strategy game for one human versus one AI opponent.
- Players alternate placing discs to outflank and flip opposing discs in horizontal, vertical, and diagonal directions.
- A legal move must bracket at least one contiguous line of opponent discs between the newly placed disc and another friendly disc.
- If the active player has no valid move, the turn is skipped automatically.
- The game ends when the board is full or neither player can move.
- The winner is the side with more discs on the board at the end.
- Black always moves first under standard rules.
- The digital version emphasizes clean hints, training support, and strong move-legibility without clutter.
- The feel should be contemplative and sharp, with immediate feedback on flips and turn control.

## Visual Style
- Use Material 3 with the underwater palette from `ui/theme/Color.kt`.
- Overall background uses `Dark0`; board surround and HUD cards use `Dark1` and `Dark2`.
- The 8×8 board uses an `Aqua0`-influenced playable surface with clear cell borders.
- Valid move hints, when enabled, use subtle `Aqua2` dots centered in legal cells.
- Selected move previews and last-move emphasis use `Aqua3`.
- Invalid or blocked training-wheel moves grey down via a muted overlay derived from `Dark2` rather than a bright error splash.
- Disc art is glossy but restrained so counts and flank lines remain readable.
- Flip animations are crisp quarter-turn rotations with very short timing.
- Typography uses Material 3 hierarchy with large numeric counts and compact instructional copy.

## Screen Layout
