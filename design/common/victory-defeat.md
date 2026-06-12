# Victory and Defeat Presentation

## Core Rule
Victory and defeat status must **never** overlay the game board. These screens appear below the board or replace it entirely only after gameplay has fully stopped.

## Victory (Level Complete)

### Layout
- The game board fades to a dim, non-interactive state.
- A victory panel slides up from the bottom of the screen, occupying roughly the bottom third.
- The panel is never full-screen — the dimmed board remains visible above it.

### Content
- "Level Complete" or equivalent heading.
- Star rating (1–3 stars) based on performance criteria defined per game.
- Score breakdown: base score, time bonus, accuracy bonus, combo bonus.
- "Next Level" button (primary action).
- "Replay" button (secondary action).
- "Menu" button (tertiary action).

### Animation
- Star icons animate in one by one with a pop-and-glow effect.
- Score totals roll up numerically.
- Total animation duration: ≤2 seconds.

## Defeat (Game Over)

### Layout
- Same bottom-panel approach as victory.
- The board remains visible above the panel, frozen at the state of defeat.

### Content
- "Game Over" heading.
- Final score.
- Best score (highlighted if a new record was set).
- "Try Again" button (primary action).
- "Menu" button (secondary action).

### Animation
- A brief screen-shake on the game board (≤0.3s) signals the moment of defeat.
- The defeat panel slides up immediately after.
- If a new high score was achieved, a subtle particle burst plays behind the score.

## New High Score Callout
- Shown within the victory or defeat panel, not as a separate overlay.
- Brief text label: "New Best!" with a gold color highlight.
- Accompanied by a short particle animation (stars or sparkles from `ui/theme/Color.kt`).

## Accessibility
- All panels support both light and dark themes.
- Buttons meet minimum touch target size of 48dp × 48dp.
- All animations respect the system's "reduce motion" accessibility setting — use instant transitions when enabled.
