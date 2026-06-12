# HUD Elements

## Purpose
Standardized on-screen display components for game state information. All HUD elements appear outside the active game board area — never overlapping playfield content.

## Score Display
- Positioned in the top-center of the screen.
- Large, bold numeric display using the game's primary font.
- Animated: score increments with a brief counter-roll animation (+N points pop-up).
- High score shown below or beside the current score in a smaller weight.

## Lives / Health
- Positioned in the top-left corner.
- Represented as icon repeats (e.g., small ship icons for Asteroids, heart icons for others) or a segmented health bar.
- Lost lives animate with a brief fade-out and scale-down.
- When lives reach zero, the game-over sequence begins (see `victory-defeat.md`).

## Timer
- Positioned in the top-right corner.
- Countdown format: `MM:SS` or `SS.ms` depending on game precision needs.
- At ≤10 seconds remaining: timer turns red and pulses once per second.
- Arcade games use a continuously depleting bar beneath the score for instant readability.

## Level / Wave Indicator
- Shown briefly as a full-width overlay at the start of each level/wave (e.g., "Wave 3").
- Fades out after 1.5 seconds; does not cover the game board during active play.
- Persistent level indicator (small, top-left below lives) shows current level number.

## Beacon / Objective Tracker (Asteroids-specific)
- Shows beacon progress as icon dots: filled = collected, hollow = remaining.
- Positioned below the score in the top-center.
- A brief pulse animation plays when a beacon spawns.

## Ammo / Shot Counter
- Used in games with limited shots per turn (Brick Breaker, Missile Command).
- Positioned in the bottom-right corner.
- Displayed as a numeric count with a small icon.
- Depletes visually as shots are fired.

## Power-Up Active Indicator
- Positioned on the left side of the screen, stacked vertically.
- Each active power-up shows an icon with a countdown timer bar.
- Icons use distinct shapes and colors per power-up type (see `powerup-system.md`).

## General Rules
- All HUD elements use the color palette from `ui/theme/Color.kt`.
- HUD text uses the app's standard bold heading typeface.
- No HUD element may overlap the primary game board.
- HUD elements respect safe area insets on all devices.
