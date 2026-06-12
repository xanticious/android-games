# Bubbles Pop Snake (Arcade) — Design Document

## Overview
A Zuma's Revenge-style bubble shooter. A chain of colored bubbles winds along a spiral or S-shaped track toward an exit hole. The player controls a rotating launcher in the center or side of the screen and fires bubbles into the chain to create color matches of 3+, eliminating segments and stopping the chain before it reaches the exit.

## Visual Style
- Richly detailed track: the path is a glowing tube or stone channel that winds across the screen.
- Chain of bubbles: tightly packed, each bubble slightly overlapping the next, moving fluidly along the track.
- Exit hole: a glowing vortex or drain at one end of the track. When bubbles enter it, a danger pulse animates.
- Launcher: ornate rotating shooter in the center (or mounted at the edge), showing current and next bubble.
- Matched groups implode with a satisfying burst; the chain visibly contracts as the gap closes.
- Power-up bubbles in the chain glow with distinct icons.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]   [Level]   [Lives]    │  ← HUD (top)
├─────────────────────────────────┤
│   ~~~~ winding track ~~~~       │
│  ●●●●●●●● chain ●●●●●●●●      │  ← Bubble chain on track
│       [LAUNCHER]                │  ← Center launcher
│  ●●●●●●● chain ●●●●●●●●●     │
│                  [EXIT VORTEX] │
└─────────────────────────────────┘
```

## Controls
- **Aim**: tap or drag to rotate the launcher toward the desired point in the chain.
- **Fire**: tap the launch point or tap anywhere in the play area. The bubble flies in the aimed direction and inserts into the chain at the nearest matching position, or attaches at the end of the chain if no match.
- Cooldown: 0.3 seconds between shots.
- "Swap" button: swap current and next bubble (limited uses per level, shown in HUD).

## Gameplay Loop

### Real-Time Structure
- The bubble chain moves continuously along the track toward the exit.
- When 3 or more same-colored bubbles are adjacent in the chain, they pop and the chain contracts.
- Chain speed starts slow and accelerates over time and per level.
- Popping bubbles near the exit gains bonus points (risk vs. reward).

### Power-Up Bubbles (in chain)
Power-up bubbles are embedded in the chain. Matching the color adjacent to a power-up or shooting directly at a power-up triggers its effect:
- Lightning: destroys all bubbles of a specific color in the chain.
- Slow: slows the chain for 10 seconds.
- Reverse: briefly reverses the chain's movement direction.
- Bomb: destroys a 5-bubble radius around impact.
- Color Storm: temporarily makes all chain bubbles one color for 3 seconds.
- Wildcard: the fired bubble matches any color on contact.

### Backfire Mechanic
- If the player fires a bubble that has no match nearby, it embeds in the chain and the chain gains 2 extra bubbles (penalty).
- This encourages thoughtful shot selection over rapid firing.

### Lives
- 3 lives per game.
- A life is lost when any bubble exits through the exit vortex.
- After a life loss, the chain resets to a shorter length from a safe position.

### Win / Loss
- **Win**: clear the entire bubble chain to complete a level.
- **Loss**: lose all 3 lives.
- Levels end when the chain is fully eliminated. Next level loads with a new, longer, faster chain.

## Scoring
| Event | Points |
|-------|--------|
| Pop 3 bubbles | 50 |
| Pop 4 bubbles | 100 |
| Pop 5+ bubbles | 150 + 30 per extra |
| Near-exit pop (last 20% of track) | 2× multiplier |
| Chain reaction cascade | 200 per extra group popped |
| Power-up triggered | 150 |
| Level complete (bubbles remaining × 10) | bonus |

## Difficulty Scaling
- Chain speed increases per level.
- More colors introduced per level.
- Chain length increases per level.
- Power-up frequency decreases per level.
- Backfire penalty increases per level (2 → 3 extra bubbles).

## State Machine
```
Idle
 └─ LevelStart → Playing
Playing
 ├─ BubblePopped → Playing (check chain contract)
 ├─ BubbleExitedVortex [lives > 0] → LifeLost
 ├─ BubbleExitedVortex [lives == 0] → GameOver
 └─ ChainCleared → LevelComplete
LifeLost
 └─ ChainReset → Playing
LevelComplete
 └─ NextLevel → Playing (new chain, faster)
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Level: top-center.
- Lives: top-right.
- Next bubble preview: beside the launcher.
- Swap remaining: small counter near next bubble preview.
- Active power-ups: bottom-right, icon stack.
- Chain danger indicator: glows brighter as more bubbles approach the exit.
