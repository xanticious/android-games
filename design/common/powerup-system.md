# Power-Up System

## Purpose
A reusable framework for spawning, collecting, and applying temporary or one-time game modifiers across multiple action games.

## Power-Up Types

| ID | Name | Effect | Duration |
|----|------|--------|----------|
| `explode` | Explosion | Destroys all bricks/enemies adjacent to the collected item | Instant |
| `multi-shot` | Multi-Shot | Fires 3 projectiles per shot instead of 1 | 15 seconds or end of level |
| `power-shot` | Power Shot | Doubles projectile damage | 20 seconds or end of level |
| `clear-screen` | Clear Screen | Destroys all standard bricks/enemies currently on screen | Instant |
| `slow-time` | Slow Time | Halves the speed of all enemies/falling objects | 10 seconds |
| `shield` | Shield | Absorbs the next hit that would end the game | Until hit |
| `lightning` | Lightning Strike | Destroys an entire column or row instantly | Instant |
| `color-bomb` | Color Bomb | Destroys all bubbles/items of the targeted color | Instant |
| `rapid-fire` | Rapid Fire | Increases projectile fire rate | 10 seconds |
| `wide-shot` | Wide Shot | Increases projectile size or spread angle | 15 seconds |

## Spawn Rules
- Power-ups are embedded inside special bricks or bubbles.
- When a power-up container is destroyed, the power-up icon floats or bounces at the destruction point.
- The icon must be collected (touched by the player's projectile or character) within 3 seconds or it disappears.
- No more than 2 power-ups may be active simultaneously.
- New power-up of the same type as an active one refreshes the timer instead of stacking.

## Visual Design
- Each power-up has a unique icon and a distinct background color (all from `ui/theme/Color.kt`).
- Spawned power-ups pulse with a soft glow animation to attract attention.
- Collected power-ups display a brief screen-edge flash in the power-up's color.
- Active power-ups are shown in the HUD with countdown bars (see `hud-elements.md`).

## Audio Cues
- Distinct sound for each power-up collection event.
- Expiry: a soft descending tone plays 2 seconds before a timed power-up ends.

## Game-Specific Notes
- **Brick Breaker variants**: power-ups drop from special bricks and must be caught by the cannon's ball stream or the player character.
- **Bubbles Pop variants**: power-ups are unlocked by matching bubbles adjacent to special marked bubbles.
- **Asteroids**: no standard power-ups; beacon explosions act as a built-in field-clear mechanic.
