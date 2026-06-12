# Visual Style Guidelines — Action Games

## Art Direction
All action games use high-definition 2D graphics with a consistent visual language:
- Crisp vector-style artwork with subtle lighting effects.
- Particle systems for explosions, trails, and destruction events.
- Smooth 60 fps animations for all gameplay elements.
- Backgrounds feature depth through parallax layers (at least 2 layers where applicable).

## Color Palette
All color values must reference tokens from `ui/theme/Color.kt`. No hard-coded hex values in game composables or design specs.

Key roles:
| Role | Usage |
|------|-------|
| Primary | Player character, player projectiles, key UI elements |
| Secondary | Enemy elements, hazard indicators |
| Accent | Power-ups, beacons, collectibles |
| Surface | Game board background |
| Background | Screen background, HUD backdrop |
| Error | Damage indicators, low-health warnings, danger zones |

## Typography
- HUD numerals: large bold monospace for scores and timers (ensures fixed-width counter roll).
- Labels: short, uppercase, medium weight.
- No decorative fonts — legibility at small sizes takes priority.

## Game Board Proportions
- The game board fills the maximum available vertical space between HUD (top) and action panel / HUD (bottom).
- On phones: board is portrait-oriented, full width.
- On tablets: board may be centered with side gutters, or expanded to fill the width with adjusted HUD positions.

## Particle Effects
- Explosions: radial burst of 8–16 particles, velocity-decayed, color-matched to the destroyed object.
- Projectile trails: short fading line, 4–8 frames, in the projectile's primary color.
- Collection events (beacons, power-ups): outward ring + sparkle burst.
- Destruction of enemies/bricks: debris fragments with short arc trajectories.

## Animations
- Idle animations (floating asteroids, hovering enemies): gentle sine-wave oscillation, amplitude ≤5dp.
- Hit flash: sprite flashes white for 2 frames on damage.
- Death sequence: shrink + fade over 0.3s for small objects; explosion particle burst for large objects.
- Level transition: board fades to black over 0.4s, new level fades in.

## Theme Adaptation
- Light theme: backgrounds use lighter surface tones; particle effects slightly muted.
- Dark theme: backgrounds are near-black; neon-adjacent accent colors for projectiles and power-ups.
- Both themes use the same color tokens — no theme-specific hex values.

## Audio (Design Notes)
- Sound design is out of scope for this document, but visual feedback must be sufficient on its own (all events are visually distinguishable without audio).
