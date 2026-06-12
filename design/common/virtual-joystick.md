# Virtual Joystick (Left-Thumb Knob)

## Purpose
Provides analog directional input for games that require free movement (Asteroids, Space Defender, Helicopter Dogfight, etc.).

## Layout
- Positioned in the lower-left quadrant of the screen.
- A fixed outer ring (diameter ~120dp) marks the joystick boundary.
- A draggable inner knob (diameter ~50dp) sits at the center at rest.

## Interaction
- The player touches anywhere within the outer ring to activate.
- Dragging the inner knob relative to the center produces a normalized directional vector `(dx, dy)` in the range `[-1, 1]` on each axis.
- The inner knob is clamped to the outer ring boundary — it never leaves the ring.
- On release, the knob springs back to center and the output vector resets to `(0, 0)`.

## Output
```
data class JoystickInput(
    val dx: Float,  // -1.0 (left) to +1.0 (right)
    val dy: Float   // -1.0 (up)   to +1.0 (down)
)
```

## Visual Design
- Outer ring: semi-transparent white circle with a subtle glow, opacity ~40%.
- Inner knob: solid accent-color circle with a slight drop shadow.
- Both elements adapt to the app's current theme (light/dark).
- Ring and knob use colors from `ui/theme/Color.kt` — no hard-coded hex values.

## Dead Zone
- A dead zone of radius 8dp around center suppresses micro-jitter.
- Input vectors inside the dead zone are output as `(0, 0)`.

## Games Using This Component
- Asteroids (movement)
- Space Defender (cannon positioning)
- Helicopter Dogfight (altitude control)
