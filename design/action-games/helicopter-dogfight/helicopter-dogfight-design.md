# Helicopter Dogfight — Design Document

## Overview
A high-definition side-scrolling action game inspired by Contra's aesthetic. The player pilots a combat helicopter that always faces right. Enemies appear on-screen firing horizontal projectiles; the player evades by moving up and down and must destroy all enemies before the screen auto-scrolls to the next section.

## Visual Style
- Bold, detailed pixel-art-inspired HD sprites: chunky, readable character and vehicle silhouettes.
- Visual tone: Contra-meets-Apocalypse Now — jungle outposts, ruined factories, mountain passes.
- Player helicopter: a military chopper silhouette with rotating rotor blades and a gun barrel facing right.
- Enemies: ground turrets, enemy helicopters, AA guns, missile launchers, troop carriers. Each has a distinct silhouette.
- Enemy projectiles: horizontal tracer rounds, rockets (homing), and spread-fire bursts.
- Player projectiles: rapid-fire bright rounds with a muzzle flash at the barrel.
- Background: layered parallax — near foreground terrain, mid-layer buildings/jungle, distant sky.
- Explosions: multi-frame boom animation with smoke cloud and debris particles.
- Screen-shake on large explosions.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Health ████░]  [Score]  [Wave]│  ← HUD (top)
├─────────────────────────────────┤
│   [distant mountains]           │  ← Parallax bg layer 3
│     [mid buildings / jungle]    │  ← Parallax bg layer 2
│ [enemies + terrain]             │  ← Parallax bg layer 1
│     [HELI →]  ~~bullets~~      │  ← Player + projectiles
│ [ground / terrain boundary]    │
└─────────────────────────────────┘
     [Altitude Control — right side]
```

## Controls
- **Altitude**: right thumb vertical drag (or virtual joystick right side) moves the helicopter up and down.
- **Horizontal movement**: player can drift slightly left/right within a zone — controlled by horizontal component of the same joystick.
- **Auto-fire**: the helicopter fires automatically at a fixed rate. No fire button needed.
- The screen auto-scrolls rightward at a fixed rate. The player cannot prevent the scroll.

## Gameplay Loop

### Screen-Based Structure
- The game world is divided into discrete "screens."
- Each screen contains a set number of enemies.
- **All enemies on the current screen must be destroyed before the auto-scroll advances to the next screen.**
- While enemies remain, the scroll pauses (but enemies continue firing).
- Once all enemies are cleared, a brief "All Clear" overlay appears and the screen advances.

### Enemies

| Type | Behavior | HP | Projectile |
|------|----------|----|-----------|
| Ground Turret | Stationary, fires 2-shot burst | 3 | Horizontal tracer |
| AA Gun | Aims at player's altitude | 2 | Fast single shot |
| Enemy Heli | Moves up/down, fires bursts | 4 | 3-shot spread |
| Missile Launcher | Slow homing missile | 5 | Homing rocket |
| Troop Carrier | Crosses screen, drops soldiers | 6 | Multiple tracers |
| Boss (every 5 screens) | Multi-phase, multiple attacks | 30 | Various |

### Player Helicopter
- Health: 5 HP.
- Taking a hit flashes the sprite and reduces HP by 1.
- At 0 HP, the helicopter crashes (brief explosion animation), and a life is lost.
- 3 lives per game; brief respawn invincibility (2 seconds).
- Firepower upgrades drop from elite enemies:
  - Double Barrel: fires 2 parallel shots.
  - Rocket Pod: fires a slower but higher-damage rocket alongside bullets.
  - Spread Shot: fires a 3-way spread.
  - Upgrades persist until a hit is taken (one hit downgrades to the previous tier, like classic arcade games).

### Bosses
- Appear every 5 screens; the screen does not scroll until the boss is defeated.
- Bosses have multiple attack phases (change pattern at each HP threshold).
- Defeating a boss restores 1 HP and drops a guaranteed upgrade.

## Scoring
| Event | Points |
|-------|--------|
| Destroy ground turret | 100 |
| Destroy AA gun | 75 |
| Destroy enemy heli | 150 |
| Destroy missile launcher | 200 |
| Destroy troop carrier | 250 |
| Destroy boss | 2000 + 100 × phase_count |
| No-damage screen clear | 500 bonus |
| Collect upgrade drop | 50 |

## Difficulty Scaling
- Enemy count per screen increases every 5 screens.
- Enemy HP increases after screen 10.
- Projectile speed increases after screen 15.
- New enemy types introduced progressively.

## State Machine
```
Idle
 └─ GameStart → Spawning
Spawning
 └─ ScreenReady → Playing
Playing
 ├─ EnemyDestroyed → Playing
 ├─ AllEnemiesDestroyed → ScreenClear
 ├─ PlayerHit [HP > 0] → Playing (damage flash)
 ├─ PlayerHit [HP == 0, lives > 0] → Respawning
 └─ PlayerHit [HP == 0, lives == 0] → GameOver
ScreenClear
 └─ AllClearDelay → Scrolling
Scrolling
 └─ NextScreenReady → Spawning
Respawning
 └─ RespawnComplete → Playing
GameOver
 └─ (terminal state)
```

## HUD
- Health bar: top-left (5 segment bar).
- Score: top-center.
- Screen/wave number: top-right.
- Lives: below health bar (helicopter icons).
- Active weapon upgrade: bottom-right icon.
- Boss health bar (when applicable): full-width bar below the top HUD strip.
