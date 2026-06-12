# Tap Targeting System

## Purpose
Allows the player to specify a target point or direction by tapping on the right side of the screen. Used for shooting, aiming, and directing characters to locations.

## Variants

### Variant A — Directional Fire (Asteroids)
- The player taps a point in the right half of the screen.
- The tap coordinates define a direction vector from the ship's center to the tap point.
- A valid fire cone is defined as ±20° from the ship's current heading.
- If the tap falls within the cone, the projectile fires in that exact direction.
- If the tap falls outside the cone, the direction is clamped to the nearest cone edge (±20°).
- Visual feedback: a brief arc overlay shows the valid fire cone and highlights the clamped direction if the tap was out of range.

### Variant B — Position Targeting (Snakes 2D, Overcooked Lite)
- The player taps a destination point on the screen.
- The game entity (snake head, cook) moves toward that point in a straight line.
- A subtle tap-ripple animation marks the destination.
- A dotted line preview may optionally show the intended path.

### Variant C — Swing Targeting (Pong)
- The player taps a point in their half of the screen.
- The bat swings to that position immediately.
- A visual arc shows the bat's sweep range before impact.
- After a swing, the bat enters a cooldown state (visual indicator: bat fades slightly).

## Input Validation
- Taps in restricted zones (e.g., the joystick area) are ignored for targeting purposes.
- A minimum tap duration of 30ms distinguishes intentional taps from accidental touches.

## Visual Feedback
- All tap targets show a brief ripple or flash animation at the tap point.
- Invalid taps (e.g., outside play area) produce no feedback.
- Clamped directions (Variant A) show a brief orange indicator at the clamped edge.

## Games Using This Component
- Asteroids (Variant A — directional fire)
- Snakes 2D (Variant B — snake movement target)
- Overcooked Lite (Variant B — cook movement/interaction)
- Pong (Variant C — bat swing)
