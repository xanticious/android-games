# Puzzle Controls — Common Component

Shared input conventions for puzzle games so gestures mean the same thing everywhere. Individual game docs list only the gestures they use and reference this file for the standard meaning.

## Standard Gestures
| Gesture | Standard meaning |
|---------|------------------|
| **Tap** | Primary action on a cell (reveal, place, select, cycle to next value, rotate one step). |
| **Hold (long-press)** | Secondary / annotation action (place a mark or flag, open a value picker, show a hint about that cell). |
| **Drag** | Continuous action across cells (draw a path/line, drag a piece, paint). |
| **Swipe** | Whole-board directional action (e.g. 2048 tile slide). |
| **Double-tap** | Reserved; used only where a game explicitly defines it. |

## Undo / Redo
- Games that benefit from exploration (Flood, Numberlink, Pathfinder, Pipes, Sokoban, Sliding Puzzle, Pentomino, Peg Solitaire) expose an always-available **Undo** button in the bottom action row.
- Undo steps back exactly one player action and is unlimited up to the start of the current puzzle unless a game states otherwise.
- Redo is optional per game; when present it sits next to Undo and is disabled once a new action diverges from the undone history.

## Hints
- Hints are opt-in per game and surfaced through a **Hint** button (when offered) or a long-press on a cell.
- A hint reveals the smallest useful nudge (one correct cell, one valid move) rather than solving the puzzle.
- Hint usage is recorded in local stats so "no-hint" completions can be tracked.

## Reset / New
- **Reset** restores the current puzzle to its starting state without changing the puzzle.
- **New** generates or loads a fresh puzzle at the current settings.

## Accessibility
- All interactive cells expose content descriptions for TalkBack.
- No control depends on color alone; shape, label, or position always disambiguates (important for color-based games like Flood and Sudoku Colors).
- Touch targets meet the Material 3 minimum (48dp) even when a grid cell is rendered smaller, by expanding the hit area.
