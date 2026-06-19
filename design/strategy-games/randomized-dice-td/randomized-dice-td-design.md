# Randomized Dice TD — Design Document

## Overview
A tower-defense game built around **randomness as the core mechanic**. Instead of freely placing towers, the player spends money on **dice-driven actions**: cheap **random** buys/upgrades, or expensive **deterministic** buys/upgrades you fully control. Every run forces you to adapt to the hand you're dealt and decide when to gamble on cheap rolls versus pay up for exactly what you want. Maps are randomly generated each play.

All shared TD board, economy, enemy, wave, tower, and combat rules live in [../../common/tower-defense.md](../../common/tower-defense.md). This document covers only the dice-driven purchase model that makes this game distinct.

## Design Pillars
- **Spend money, manage luck.** The economy isn't just "buy towers" — it's choosing your level of control vs. cost.
- **Cheap chaos vs. costly certainty.** Random options are budget-efficient but unpredictable; specific options cost a premium for control.
- **Adapt every run.** Random maps + random rolls mean no two runs play the same.

## The Four Purchase Actions
The entire economy revolves around four buy buttons. They use the **standard three tower roles** (single-target / AoE / slow) from the common doc and the shared per-level upgrade curve.

| Action | Cost | What it does |
|--------|------|--------------|
| **Buy Random Tower** | **cheap** | Spawns a **random tower type** on a **random buildable tile**. Most money-efficient way to add firepower; you don't choose type or place. |
| **Buy Specific Tower** | **expensive** (a lot more) | You **pick the tower type** and **place it where you want**. Full control, premium price. |
| **Upgrade Random Tower** | **median** | Upgrades a **randomly chosen** existing tower by one level. Cheaper than targeted upgrading, but luck decides which tower improves. |
| **Upgrade Specific Tower** | **scales with that tower's level** | Upgrades a **tower you choose**; cost depends on its **current level** (higher level = higher cost, per the shared upgrade curve). |

### Cost Relationships
- `BuyRandom  <  UpgradeRandom  <  BuySpecific` (random placement/type is the discount; control is the premium).
- `UpgradeSpecific(tower)` is a **function of the target tower's level** — low-level towers are cheap to push, high-level towers get expensive (diminishing returns on dumping money into one tower).
- Exact numbers are data/tunable; the **ordering and the level-scaling of Upgrade Specific** are the design-defining constraints.

### Randomness Rules (fair & seeded)
- **Buy Random Tower**: tower **type** is uniformly (or weighted) random; **tile** is uniformly random among currently **buildable** tiles. If no buildable tile exists, the action is unavailable (greyed out, no charge).
- **Upgrade Random Tower**: target is uniformly random among towers **not yet at max level**. If all towers are maxed, the action is unavailable.
- All randomness draws from the run's **seed** so a run is reproducible and the logic is unit-testable.

## Strategy Surface
The interesting decisions this creates:
- **Early game**: spam **Buy Random** to flood cheap coverage, accept messy placement.
- **Mid game**: **Upgrade Random** to efficiently raise your overall power, or pay for a **Specific** tower to plug a known gap in coverage.
- **Late game**: **Upgrade Specific** a key tower despite rising cost, or keep buying breadth — weighing the level-scaled price against another cheap random.
- The shared **Call Next Group Early** bonus stacks another gamble: rush groups for money to fund more rolls.

## Maps & Economy
- Random maps each play (grid, single path, buildable tiles) per the shared map rules; **no solvability guarantee** (this game does **not** use [../../common/level-solvability.md](../../common/level-solvability.md) — luck means some runs won't be winnable, which is the point).
- Money sources are the shared ones: starting purse, kill **bounty**, and the **early-call bonus**.
- A **seed** drives both the map and the dice; shown on results for replay.

## Win / Loss
- **Victory**: clear all groups with **lives > 0**.
- **Defeat**: base overrun (**lives = 0**).
- Result panel **below** the board (per [../../common/victory-defeat.md](../../common/victory-defeat.md)) with groups cleared, money spent per action type, and the run **seed**.

## Screen Layout
```
+-----------------------------------------------+
| Lives: ###   Money: $###   Group: x / N       |  <- HUD (top)
+-----------------------------------------------+
|   #####  PATH  #####                          |
|        #            #   [?]   [Gun L2]        |
|   [Frost] ##### >> ##         [Mortar]        |
|                              BASE [*]          |
+-----------------------------------------------+
| [Buy Random $]  [Buy Specific $$$]            |
| [Upgrade Random $$]  [Upgrade Specific $?]    |  <- dice action bar
|                          [ Call Group Early ] |
+-----------------------------------------------+
```
- **Buy Specific** enters a place-mode: choose type, then tap a buildable tile.
- **Upgrade Specific** enters a select-mode: tap a tower; its level-scaled cost is shown inline before confirming. All inline — no modal over the board.

## Difficulty
A Settings difficulty scales the **shared wave levers** and the **action costs / starting money** (Easy: cheaper rolls, more starting money; Hard: pricier control, leaner economy). No new mechanics per level.

## State Machine
Reuses the shared TD shape (`Idle → Building ⇄ Wave → Victory/Defeat`) from [../../common/tower-defense.md](../../common/tower-defense.md).
- `RandomizedDiceTdState` mirrors the shared states; the four purchase actions are events handled in `Building` (and may also be allowed mid-`Wave` per tuning).
- Events add: `BoughtRandom`, `BoughtSpecific`, `UpgradedRandom`, `UpgradedSpecific`.

## Controllers (pure, `controller/`)
- **MapGenerator** — `seed → TdMap` (no solvability check).
- **DicePurchaser** — given the current board, money, and seed/RNG state, resolves each of the four actions (which type/tile/tower, cost charged) and returns the updated board + money. Deterministic from seed → fully unit-testable.
- **CostCalculator** — `BuyRandom`, `BuySpecific`, `UpgradeRandom` flat/median costs and `UpgradeSpecific(level)` scaling.
- **CombatResolver** — shared per-tick tower targeting/damage/slow/bounty resolution.

Tested behaviors: *Buy Random places only on buildable tiles*, *Upgrade Random never targets a maxed tower*, *Upgrade Specific cost increases with tower level*, *cost order BuyRandom < UpgradeRandom < BuySpecific*, *same seed ⇒ same rolls*.

## Data Model (game-specific)
Builds on the shared `TowerRole` / `Tower` / `TdMap` types.
```
enum class DiceAction { BUY_RANDOM, BUY_SPECIFIC, UPGRADE_RANDOM, UPGRADE_SPECIFIC }
data class ActionCosts(
    val buyRandom: Int,
    val buySpecific: Int,
    val upgradeRandom: Int,
    val upgradeSpecificBase: Int,
    val upgradeSpecificPerLevel: Int    // cost = base + perLevel * targetLevel
)
data class DiceTdSettings(val difficulty: Difficulty, val seed: Long)
// DicePurchaser.resolve(action, board, money, rng): PurchaseResult
```

## Out of Scope (v1)
- More than the three shared tower types.
- Re-roll / "reroll the placement" meta-actions (kept to the four core buys).
- Guaranteed-winnable maps (random + dice; may be unbeatable).
- Tower synergies, boss/flying enemies, multiple paths.
- Persistent meta-progression between runs.
