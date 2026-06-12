# Chess Puzzles — Design Document

## Overview
Chess Puzzles is a 10-round score attack mode built from curated or generated one-move positions.
Each round presents a single chess position and asks the player to find the best move for the side to play.
The player gets exactly one move per puzzle, after which the app compares the choice against engine analysis.
Base scoring uses evaluation distance from the engine's top line: within 0.1 earns 100, within 0.5 earns 75, within 1.5 earns 50, within 3.0 earns 25, and anything worse earns 0.
Speed adds a bonus: +20 within 10 seconds, +10 within 30 seconds, and +5 within 60 seconds.
The running total builds across all 10 puzzles and best run score is stored locally on device.

## Visual Style
Use Material 3 with a tournament-study presentation built from the underwater palette.
Background layers use `Dark0` and `Dark1`; the board frame and key dividers use `Aqua4`.
Light board squares can sit near `Aqua0`, dark squares near `Dark2`, while selected squares and legal targets use `Aqua2`.
Engine feedback arrows and scoring accents use `Aqua3`, and general text uses `Aqua0`.
Result animations stay restrained and appear only below the board.
The board must remain fully visible during scoring, review, and session-complete states.

## Screen Layout
