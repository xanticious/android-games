# Hole Swallowing Game — Design Document

## Overview
A high-definition arcade game where the player controls a growing sinkhole that rolls through a city, swallowing objects to grow larger and eventually devour entire buildings. The objective is to consume enough mass before the timer runs out.

## Visual Style
- Vivid, cartoonish city aesthetic: bright buildings, colorful objects, toy-like vehicles.
- The hole is rendered as a sharp-edged circular void with a swirling dark vortex animation inside.
- Objects tip, spin, and shrink into the hole with satisfying physics animations.
- Ground cracks appear at the edges of the hole as it grows.
- City environment uses layered tile-based sections: residential, downtown, industrial, waterfront.
- Screen camera follows the hole from slightly above (top-down with subtle perspective tilt).
- Background sky: vivid blue with fluffy clouds, darkening in later levels.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]  [Size: ██░░░░]  [Time]│  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│       CITY MAP (top-down)       │
│         [HOLE]                  │
│                                 │
└─────────────────────────────────┘
     [Joystick — bottom overlay]
```

## Controls
- **Movement**: left thumb virtual joystick (see `design/common/virtual-joystick.md`).
- The hole rolls in the joystick direction.
- Speed is constant (the hole doesn't accelerate/decelerate — it rolls or stops).
- Right thumb is unused — one-thumb gameplay.

## Gameplay Loop

### Core Loop
- The player rolls the hole around a city map.
- Objects smaller than the current hole size get swallowed automatically on contact.
- Each swallowed object increases the hole's diameter proportionally to the object's mass.
- As the hole grows, previously too-large objects become swallowable.
- Swallow enough mass to reach the target size before the timer expires.

### Objects and Sizes
Objects are divided into size tiers. The hole can only swallow objects smaller than its current diameter.

| Tier | Example Objects | Size Value |
|------|----------------|------------|
| 1 (tiny) | Coins, flowers, fire hydrants | 0.5 |
| 2 (small) | Mailboxes, benches, scooters | 1 |
| 3 (medium) | Cars, food carts, large trees | 2 |
| 4 (large) | Buses, shipping containers | 5 |
| 5 (huge) | Small buildings, cranes | 10 |
| 6 (massive) | Large buildings, skyscrapers | 25 |

### Level Structure
- Each level is a pre-built city map with a specific target score and time limit.
- Maps are partitioned into distinct zones (residential, shopping district, port, etc.).
- The timer counts down from a level-specific value (e.g., 2 minutes at level 1).
- Bonus time pickups (glowing clocks) appear on the map — swallowing them adds 5 seconds.

### Scoring
| Event | Points |
|-------|--------|
| Swallow tier 1 object | 10 |
| Swallow tier 2 object | 25 |
| Swallow tier 3 object | 50 |
| Swallow tier 4 object | 150 |
| Swallow tier 5 object | 400 |
| Swallow tier 6 object | 1000 |
| Collect time bonus | 50 |
| Level complete with time remaining | time_remaining_seconds × 20 |

### Lose Condition
- Timer reaches zero before the target score is met.

### Win Condition
- Score reaches the target before time expires.

## Difficulty Scaling
- Each level has a higher target score and less time.
- Some maps have obstacles (water channels, barriers) the hole cannot cross.
- Later levels have fewer high-value objects, requiring efficient routing.

## State Machine
```
Idle
 └─ LevelStart → Playing
Playing
 ├─ ObjectSwallowed → Playing (hole grows, score updates)
 ├─ TimeBonusCollected → Playing (timer extended)
 ├─ TargetReached → LevelComplete
 └─ TimerExpired → GameOver
LevelComplete
 └─ NextLevel → Playing (new map)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Size progress bar (current size vs. target size): top-center.
- Countdown timer: top-right (turns red and pulses at ≤10 seconds).
- Object swallow popups: brief +N score labels that float up from the swallowed object's position.
