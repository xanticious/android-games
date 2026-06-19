# Base Defense — Design Document

## Overview
A deliberately **lite** tower-defense game. Enemies march along a path toward your base; you place and upgrade towers to stop them. The scope is intentionally small: **three tower types**, money earned from kills and from **calling the next enemy group in early**, and **randomly generated maps** each play. It is honest about being casual — you might not be able to beat every random map, and that's fine.

All shared TD board, economy, enemy, wave, tower, and combat rules live in [../../common/tower-defense.md](../../common/tower-defense.md). This document covers only what is specific to Base Defense.

## Design Pillars
- **Small and readable.** Three towers, one resource, one path. No tech trees.
- **Tempo by choice.** Calling the next group early trades safety for money.
- **Fresh every time.** Random maps; no fixed campaign, no guaranteed win.

## Towers (the three types)
Base Defense ships exactly the three shared roles from the common doc — nothing more:

| Tower | Role | Behavior |
|-------|------|----------|
| **Gun** | Single-target damage | Fires at one enemy (furthest along, in range) for high single-target damage. |
| **Mortar** | AoE damage | Lobs at a point; damages all enemies in a small blast radius. Great vs. clusters. |
| **Frost** | Slows enemies | Little/no damage; slows enemies in range, multiplying the other towers' effectiveness. |

### Buying vs. Upgrading (money)
The player spends money two ways (per the shared economy):
- **Buy a new tower** — place another Gun/Mortar/Frost on a buildable tile.
- **Upgrade an existing tower** — improve its stats; **cost rises per level**. Each tower has a small fixed number of upgrade levels (v1: 3 levels).

| Upgrade improves | Gun | Mortar | Frost |
|------------------|-----|--------|-------|
| Primary stat | damage / fire rate | damage / blast radius | slow strength / radius |
| Range | +range per level (all) | | |

The player balances **going wide** (more towers covering more of the path) vs. **going tall** (fewer, stronger towers).

## Economy & Early Call
Per [../../common/tower-defense.md](../../common/tower-defense.md):
- **Money** comes from a starting purse, **bounty** per killed enemy, and the **early-call bonus**.
- **Call Next Group Early**: each enemy group has a countdown before it auto-spawns. Summoning it early grants a **bonus scaled to the time skipped** — more unspent timer = more money. This is the core risk/reward: rush groups for economy, but only if your towers can handle the overlap.

## Maps (random each time)
- Each play generates a fresh map (grid, single path spawn→base, buildable tiles) per the shared map rules.
- **No solvability guarantee** — Base Defense intentionally does **not** use [../../common/level-solvability.md](../../common/level-solvability.md); some seeds will be harder than others or even unwinnable. The casual framing makes this acceptable.
- A **seed** is shown on the results screen so a fun map can be replayed.

## Win / Loss
- **Victory**: clear all enemy groups with **lives > 0**.
- **Defeat**: enemies overrun the base (**lives reach 0**).
- Result panel appears **below** the board (per [../../common/victory-defeat.md](../../common/victory-defeat.md)) with groups cleared, money earned, and the map **seed**.

## Screen Layout
```
+-----------------------------------------------+
| Lives: ###   Money: $###   Group: x / N       |  <- HUD (top)
+-----------------------------------------------+
|   #####  PATH  #####                          |
|        #            #     [Gun]               |
|  [Frost]  ###### >> ##  [Mortar]              |
|                              BASE [*]          |
+-----------------------------------------------+
| [Gun $] [Mortar $] [Frost $]  [ Call Early ]  |  <- action bar (bottom)
+-----------------------------------------------+
```
- Tapping a buildable tile with a tower type selected places it; tapping an existing tower opens an **inline** upgrade/sell affordance (no modal over the board).

## Difficulty
A Settings difficulty scales the **shared wave levers** (enemy HP/speed/count growth, starting money/lives) — Easy gives more starting money and slower scaling; Hard gives less and tougher groups. No new mechanics per level.

## Screen Flow
Standard per-game flow: **Settings → How to Play → Gameplay → Results**.
- **Settings**: difficulty and Random vs. saved seed.
- **How to Play**: the three towers, buying vs. upgrading, calling groups early for money, and that maps are random and not always winnable.

## State Machine
Reuses the shared TD shape (`Idle → Building ⇄ Wave → Victory/Defeat`) from [../../common/tower-defense.md](../../common/tower-defense.md).
- `BaseDefenseState` mirrors the shared states; no extra states for v1.
- The early-call simply raises `WaveStarted` before the timer and awards the bonus.

## Controllers (pure, `controller/`)
- **MapGenerator** — `seed → TdMap` (path + buildable tiles; no solvability check).
- **CombatResolver** — shared per-tick tower targeting/damage/slow/bounty resolution.
- **EconomyCalculator** — bounty and **early-call bonus** (`f(timeSkipped)`), and **upgrade cost** (`f(tower, level)`).

Pure and unit-testable (e.g. *early-call bonus increases with time skipped*, *upgrade cost rises per level*, *Frost reduces enemy speed by the configured %*).

## Data Model (game-specific)
Builds on the shared `TowerRole` / `Tower` / `Enemy` / `Wave` / `TdMap` types.
```
enum class BaseTower { GUN, MORTAR, FROST }     // maps to SINGLE_TARGET / AOE / SLOW
data class UpgradeCurve(val baseCost: Int, val perLevel: Int)   // cost = base + perLevel*level
data class BaseDefenseSettings(val difficulty: Difficulty, val seed: Long)
// EconomyCalculator.earlyCallBonus(timeSkippedMs): Int
// EconomyCalculator.upgradeCost(tower: BaseTower, level: Int): Int
```

## Out of Scope (v1)
- More than three tower types; tower targeting modes beyond the shared default.
- Tower synergies / combination systems (kept deliberately simple).
- Guaranteed-winnable maps (random, may be unbeatable).
- Boss enemies, flying enemies, multiple paths.
- Campaign / level progression / saved meta-currency.
