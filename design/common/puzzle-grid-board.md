# Puzzle Grid Board — Common Component

This component defines the shared grid/board rendering used by the grid-based puzzle games (2048, Dominosa, Flood, Light Up, Logic Grid, Minesweeper, Nonogram, Numberlink, Pathfinder, Pentomino, Pipes, Skyscrapers, Sliding Puzzle, Sokoban, Sudoku Colors, and others). Individual game design docs reference this file instead of repeating layout rules.

## Layering & Boundaries
- The board is a pure `@Composable` in `view/`. It renders cells from immutable model state and emits cell/gesture callbacks only — no game logic lives in the composable.
- Cell geometry (rows, columns, valid moves, win checks) is computed by the game's controller in `controller/`. The composable receives a ready-to-render snapshot.

## Sizing & Responsiveness
- The board is square-celled: every cell is the same width and height.
- Cell size is derived from the smaller of available width/height divided by the larger grid dimension, so the whole board always fits on screen without scrolling.
- On phones the board is centered horizontally and fills the available width.
- On tablets the board is centered with side gutters; cells grow up to a sensible maximum so very small grids do not become comically large.
- The board occupies the vertical space between the top bar and the status area; it never extends under the status area.

## Visual Tokens
- All colors come from `ui/theme/Color.kt`. No hex values appear in board composables or in these design docs.
- Board background: `Dark1`.
- Cell fill (empty/default): `Dark0`.
- Cell outline / grid lines: `Dark2`.
- Given/locked content (clue cells, fixed tiles): emphasized with `Aqua4` text or border.
- Player-entered content: `Aqua0`/`Aqua1` foreground.
- Highlight / current selection: `Aqua2` outline or glow.
- Error / conflict feedback: `Aqua3` is reserved for neutral emphasis; genuine error states use the Material 3 `error` role token.

## Theme Adaptation
- Light and dark themes are system-driven via `isSystemInDarkTheme()`.
- The same tokens are used in both themes — MaterialTheme remaps surface/foreground tones; no theme-specific hex values.

## Transitions
- Cell state changes (place, clear, reveal, mark) are instantaneous per the project rule that only victory/defeat may use full-screen animation.
- Where a game benefits from motion feedback (a tile sliding, a flood spreading), the motion is short (≤200 ms) and confined to the affected cells.
