# Idle Bounce — Design Document

## Overview
A 2D idle game where a bouncy ball perpetually descends through procedurally layered terrain. Each layer represents a geological stratum with a defined HP pool. The ball bounces automatically; the player upgrades the ball and watches progress accumulate.

---

## Core Loop
1. Ball bounces against the current layer.
2. Each bounce deals damage equal to ball power.
3. When a layer's HP reaches 0 it is destroyed and the player earns coins.
4. The ball drops to the next layer and the loop repeats.

---

## Terrain Layers

| Layer Type | HP Range | Coin Reward | Notes |
|------------|----------|-------------|-------|
| Dirt       | 10–50    | 1–5         | Tutorial material |
| Gravel     | 50–150   | 5–20        | First real grind |
| Stone      | 150–500  | 20–80       | Needs first upgrade |
| Ore Vein   | 300–800  | 80–250      | Bonus coins on destroy |
| Deep Rock  | 800–2000 | 250–700     | Requires mid-game upgrades |
| Bedrock    | 2000+    | 700+        | Late-game; scales infinitely |

Layers are procedurally ordered with increasing depth. HP and reward values scale with a depth multiplier.

---

## Ball Upgrades

| Upgrade | Effect | Cost Curve |
|---------|--------|------------|
| Bounce Power | +damage per hit | Exponential |
| Bounce Speed | +hits per second | Exponential |
| Ricochet | hits layer twice per bounce | Milestone unlock |
| Lucky Strike | 5% chance of critical (3× damage) | Flat prerequisite |
| Drill Tip | pierces partial HP carry-over to next layer | Late upgrade |

---

## Visual Design
- Ball: a bright, cartoonish rubber ball with a subtle shadow.
- Terrain layers are horizontal bands; each type has a distinct texture/color (brown dirt → grey gravel → dark stone → glowing ore → near-black bedrock).
- Particle effects on each hit (dust, sparks for ore, cracks for stone).
- Depth counter displayed prominently (e.g., "−342 m").
- Coin counter and current layer HP bar always visible.

---

## State Machine
- `IdleBounceState`: `Playing`, `UpgradeMenuOpen`
- Events: `BallHit`, `LayerDestroyed`, `UpgradePurchased`, `UpgradeMenuOpened`, `UpgradeMenuClosed`

---

## Data Model
```
data class Layer(val type: LayerType, val maxHp: Int, val reward: Long)
data class Ball(val power: Int, val hitsPerSecond: Float)
data class BounceUpgrade(val id: String, val name: String, val cost: Long, val purchased: Boolean)
```

---

## Progression
- Soft prestige: after reaching the first bedrock layer, the player can "Reset Depth" for a permanent multiplier bonus.
- Each prestige resets depth but preserves upgrades; the multiplier makes the next run faster.

---

## Out of Scope (v1)
- Online leaderboards
- Multiple balls
- Sound effects (placeholder silence is fine)
