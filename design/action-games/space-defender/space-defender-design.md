# Space Defender — Design Document

## Overview
A high-definition reimagining of Space Invaders with strategic AI-driven enemies. Instead of a predictable marching fleet, the player faces 1–3 intelligent enemies per wave that move with intent, fire multi-angle projectiles, and adapt their behavior. The player's cannon auto-fires and is repositioned with a left or right thumb joystick. The goal is to survive all waves.

## Visual Style
- HD sci-fi aesthetic: detailed enemy ship sprites with animated thrusters and weapon glow effects.
- Enemy ships: sleek, organic alien designs — different silhouettes per type. No two enemy types look the same.
- Player cannon: a heavy turret base on a hovering platform, with a glowing barrel.
- Projectiles: bright neon beams (player); varied enemy projectiles per enemy type (spread bolts, homing missiles, arcing energy blasts).
- Background: deep space with nebula colors from `ui/theme/Color.kt`, planet backdrop, asteroid debris.
- Explosion: multi-ring shockwave + debris burst.
- Shields: translucent barrier panels with damage cracks that deepen with each hit.
- Screen-edge warning flash when an enemy fires a heavy projectile.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]  [Wave: N]  [Lives ♥♥♥]│  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│   [enemy ship(s) — top area]    │
│                                 │
│   [shield panels — middle]      │
│                                 │
│   [PLAYER CANNON — bottom]      │
│   ↑↑↑ auto-fire projectiles ↑↑↑│
└─────────────────────────────────┘
       [Joystick — bottom overlay]
```

## Controls
- **Position**: left OR right thumb virtual joystick (see `design/common/virtual-joystick.md`).
- Horizontal `dx` input slides the cannon left and right.
- Vertical `dy` is unused.
- **Auto-fire**: the cannon fires a continuous stream of projectiles straight upward automatically. No fire button.
- The player's only active input is horizontal positioning.

## Gameplay Loop

### Wave Structure
- Each wave contains 1–3 enemy ships spawned at the top of the screen.
- Enemies must all be destroyed to clear the wave.
- After clearing a wave, a brief inter-wave screen shows the wave number and a short countdown before the next wave.
- Difficulty increases with each wave (more enemies, faster movement, more complex fire patterns).

### Enemies

#### Enemy Types

| Type | Movement | Fire Pattern | HP |
|------|----------|-------------|-----|
| Scout | Side-to-side sweep | Single fast shot aimed at player | 3 |
| Gunner | Diagonal zigzag | 3-shot spread | 5 |
| Sniper | Slow horizontal drift | Single accurate homing shot | 4 |
| Bomber | Slow descent | Vertical drop bombs + wide spread | 8 |
| Commander | Erratic | Calls reinforcement mini-ships; fires 5-way burst | 12 |

#### Enemy AI Behavior
- Enemies track the player's position and predict near-future position when firing.
- At lower HP thresholds, enemies become more aggressive (fire faster, move more erratically).
- Enemies spread out horizontally to reduce the effectiveness of the player staying in one position.
- When multiple enemies are on screen, one acts as "flanker" (moves toward the player's current position) while another is "baiter" (moves opposite to draw the player out of position).

### Shields
- 2–3 shield panels are placed between the player and enemies.
- Each panel has a graphical damage state (undamaged → cracked → broken).
- Panels absorb both enemy and player projectiles.
- Destroyed shields do not respawn within a wave.
- New shields spawn at the start of every 5th wave.

### Player
- 3 lives per game.
- Taking a hit flashes the cannon and reduces lives by 1.
- Brief invincibility (1.5 seconds) after a hit.
- No health bar — one hit per life.

### Scoring
| Event | Points |
|-------|--------|
| Destroy Scout | 100 |
| Destroy Gunner | 200 |
| Destroy Sniper | 250 |
| Destroy Bomber | 350 |
| Destroy Commander | 600 |
| Wave cleared with shields intact | 150 × shields_remaining |
| Wave cleared with full lives | 500 |
| Back-to-back wave no-hit | +20% score multiplier |

## Difficulty Scaling
| Wave | Changes |
|------|---------|
| 1–3 | 1 Scout or Gunner, slow movement |
| 4–6 | 2 enemies, Sniper introduced |
| 7–10 | 2–3 enemies, Bomber introduced |
| 11–15 | 3 enemies, Commander introduced |
| 16+ | 3 enemies, mixed types, faster projectiles |

## State Machine
```
Idle
 └─ GameStart → WaveIntro
WaveIntro
 └─ IntroComplete → Playing
Playing
 ├─ EnemyDestroyed → Playing
 ├─ AllEnemiesDestroyed → WaveComplete
 ├─ PlayerHit [lives > 0] → Playing (invincibility)
 └─ PlayerHit [lives == 0] → GameOver
WaveComplete
 └─ IntroDelay → WaveIntro (next wave)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Wave number: top-center.
- Lives: top-right (heart icons, up to 3).
- Score multiplier indicator: below score (briefly visible when multiplier is active).
- Enemy count remaining: shown briefly at wave start, then as a small number top-right below lives.
