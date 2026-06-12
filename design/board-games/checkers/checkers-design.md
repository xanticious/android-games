# Checkers — Design Document

## Overview
Checkers is a clean tactical duel built around tempo, forced captures, promotion timing, and board control.
This implementation follows an American Checkers presentation on an 8x8 board with dark-square movement only.
It is a single-player game against AI with strong training support for learning better moves.
The tone is focused and modern, presenting classic wooden-board clarity through the app's underwater Material 3 aesthetic.
All gameplay interactions are tap-first and highly legible.
Forced jumps are mandatory.
If a capture sequence continues, the same piece must keep jumping until the sequence ends.
Per product rules, kings move multiple squares diagonally in any direction, creating higher mobility and clearer endgame swing potential.
The game is ideal for short repeatable matches with local stat tracking by difficulty and training mode usage.

## Visual Style
The board sits on a Dark0 background with a framed tabletop panel in Dark1.
Dark squares use Dark2-based values with slight texture.
Light squares use Aqua0-tinted surfaces so the board still feels bright and readable.
Red and Black are the player-facing color names for rule clarity, but piece rendering is palette-aware with rich red and near-black discs accented by Aqua edge light.
Selected pieces use an Aqua4 ring and a soft Aqua1 glow.
Legal move destinations use bright Aqua3 dots.
Capture destinations add a stronger pulse so mandatory jumps are unmistakable.
Kings gain a prominent crown badge embedded into the piece top.
When training wheels blocks a move, the rejected destination gives a gentle shake and a brief Aqua1 message strip below the board.

## Screen Layout
