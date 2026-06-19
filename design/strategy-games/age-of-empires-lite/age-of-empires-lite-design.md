# Age of Empires Lite — Design Document

## Overview
A streamlined real-time strategy game inspired by Age of Empires, designed to **remove the tedium of macro-management** so the player can focus on commanding armies. In a traditional RTS the player constantly clicks to train villagers, assign them to resources, build farms, queue military units, and research upgrades. Age of Empires Lite **automates all of that economy and production micro** through high-level *policies* the player sets once (and can adjust at any time). The player's hands-on attention is reserved for the part that's actually fun: maneuvering and microing their army in battle.

One human player versus **one bot**, on a single map, with selectable bot difficulty. Offline, local-only.

## Design Pillars
- **Set policies, not actions.** The player declares *intent* (resource balance, army composition, upgrade order); the civilization carries it out automatically.
- **Free, rate-limited workers.** Villagers spawn automatically over time at no resource cost, up to a cap — no economy clicking.
- **Combat is the game.** Almost all active input is army movement and battle micro.
- **No modals.** Policy panels are inline screens; the battlefield is never covered (per project UX rules).

## What the Player Controls vs. What Is Automated
| Concern | Who handles it |
|---------|----------------|
| Worker spawning | **Automated** — fixed time rate, free, up to max worker cap |
| Worker resource assignment | **Automated** — workers self-assign to match the Economy Balance |
| Farming / gathering micro | **Automated** |
| Military unit training | **Automated** — civ trains toward the Army Composition (costs resources) |
| Upgrade research | **Automated** — researched in the player's Upgrade Priority order (costs resources) |
| **Economy Balance policy** | **Player** (Settings + adjustable in-match) |
| **Army Composition policy** | **Player** (Settings + adjustable in-match) |
| **Upgrade Priority policy** | **Player** (Settings, adjustable in-match) |
| **Army movement & battle micro** | **Player** (the core activity) |

## Economy

### Resources
Two resources keep the model simple:
- **Food** — primarily trains/maintains military units (and is the implicit "growth" resource).
- **Wood** — primarily builds/trains certain units and pays for some upgrades.

(A deliberately small economy; gold/stone are folded into these two to avoid extra management.)

### Workers (Automated)
- Workers **spawn automatically** on a **fixed time interval** (e.g. one every N seconds), **at no resource cost**, until the **worker cap** is reached.
- Each worker continuously gathers one resource and contributes to a global resource income.
- Workers **self-assign** to resources to track the **Economy Balance** target as closely as possible (see below). When the target changes, idle/next-rebalanced workers shift assignment; there is no manual assignment.
- Worker cap and spawn interval are fixed per difficulty/map (not micromanaged by the player).

### Economy Balance (Player Policy)
- The player sets a **percentage split** between Food and Wood, e.g. `50% Food / 50% Wood`, via a single slider.
- The auto-assigner distributes workers so the **proportion of gatherers** matches the target split (rounding to whole workers). Example: 10 workers at 70/30 → 7 on Food, 3 on Wood.
- Changing the slider mid-match smoothly **re-balances** existing workers over the next few assignment ticks (no instant teleport; a worker finishes its current trip then may switch).
- This is the player's primary economic lever and can be changed at any time from the in-match policy bar.

## Military

### Army Composition (Player Policy)
- The player specifies the **desired makeup** of their army as proportions across the available unit types, e.g. `50% Infantry / 30% Archers / 20% Cavalry`.
- The civilization **auto-trains** units to converge on that composition **whenever resources allow**. Unlike workers, **military units cost resources** (Food/Wood) and respect a **population/army cap**.
- If resources are insufficient, training waits; the auto-trainer always trains the unit type that brings the live army closest to the target ratio next.
- Reinforcements trained during battle automatically rally to a player-set **rally point** near the front.

### Unit Types (v1)
A small rock-paper-scissors triangle keeps composition meaningful:
| Unit | Strong vs | Weak vs | Cost | Role |
|------|-----------|---------|------|------|
| Infantry | Cavalry | Archers | low Food | Frontline, durable |
| Archers  | Infantry | Cavalry | Food + Wood | Ranged damage |
| Cavalry  | Archers | Infantry | high Food | Fast flankers |

### Upgrades & Upgrade Priority (Player Policy)
- Upgrades improve units along tracks: **Damage I/II**, **Defense I/II**, **Speed I/II** (tiered; II requires I).
- The player specifies an **ordered Upgrade Priority list**, e.g. `Damage I → Defense I → Damage II → Speed I`.
- The civilization **auto-researches** upgrades **in that order** as resources become available; **upgrades cost resources**. A tier-II entry is skipped (deferred) until its tier-I prerequisite is researched, then taken when reached in order.
- The priority list is set on the Settings screen and can be reordered mid-match from the policy bar.

## The Bot Opponent
- Exactly **one** AI opponent, using the same policy-driven systems (its own auto-economy, auto-training, auto-upgrades) plus battlefield decision-making.
- **Difficulty** is chosen before the match and scales the bot, not the player:
  | Difficulty | Bot economy rate | Bot army cap | Upgrade speed | Battle micro |
  |------------|------------------|-------------|---------------|--------------|
  | Easy   | slower spawn / lower cap | low  | slow  | passive, poor focus-fire |
  | Normal | baseline | baseline | baseline | reasonable engagements |
  | Hard   | faster / higher cap | high | fast | aggressive, focus-fires, flanks |
- Difficulty adjusts the bot's numeric levers and the aggressiveness of its combat AI; the rules are identical for both sides.

## Win / Loss
- **Victory**: destroy the enemy's base (Town Center) / eliminate the bot.
- **Defeat**: the player's base is destroyed.
- Result is shown on a results panel **below/after** the battlefield, never overlaying it during play (per [../../common/victory-defeat.md](../../common/victory-defeat.md)).

## Controls & Gameplay (Battlefield)
- **Camera**: drag to pan, pinch to zoom.
- **Select**: tap a unit, or drag a selection box to select a group; double-tap selects all of a type on screen.
- **Command**: with units selected, tap ground to move, tap an enemy to attack; the player micros formations, focus-fire, and retreats.
- **Rally point**: tap-and-hold to set where new auto-trained units gather.
- No joystick; tap/drag based, consistent with the project's modal-free, tap-targeting conventions.
- The **policy bar** (Economy Balance slider, Army Composition, Upgrade Priority) is a slim inline panel docked at a screen edge — adjustable any time, never covering the battlefield.

## Screen Layout
```
+-----------------------------------------------+
| Food: ###   Wood: ###   Workers x/cap  Army%  |  <- resource/HUD strip (top)
+-----------------------------------------------+
|                                               |
|                BATTLEFIELD                    |
|        (pan / zoom, units, bases)             |
|                                               |
+-----------------------------------------------+
| [Econ 50/50] [Army comp] [Upgrades] [Rally]   |  <- inline policy bar (edge)
+-----------------------------------------------+
```

## Screen Flow
Standard per-game flow: **Settings -> How to Play -> Gameplay -> Results**.
- **Settings (pre-match)**: bot **difficulty**, **initial Economy Balance** split, **initial Army Composition**, and **Upgrade Priority** order. These seed the in-match policies and are all adjustable later.
- **How to Play**: explains that economy/training/upgrades are automatic via policies, and that the player's job is to command the army.

## State Machine
`AgeOfEmpiresLiteState` (per project rule: each game owns a KStateMachine, state exposed via `StateFlow`):
```
Idle
 └─ MatchStarted → Playing
Playing
 ├─ PolicyChanged → Playing      (economy / army / upgrade priority updated)
 ├─ TickElapsed → Playing        (worker spawn, gather, auto-train, auto-research)
 ├─ PlayerBaseDestroyed → Defeat
 └─ EnemyBaseDestroyed → Victory
Victory / Defeat
 └─ (replay / menu) → Idle
```
- States: `Idle`, `Playing`, `Victory`, `Defeat`.
- Events: `MatchStarted`, `PolicyChanged`, `TickElapsed`, `PlayerBaseDestroyed`, `EnemyBaseDestroyed`.

## Controllers (pure, `controller/`)
Automation logic is pure and unit-testable, with no Android/UI imports:
- **WorkerAssigner** — given worker count and an Economy Balance target, returns the per-resource worker allocation (proportional rounding; rebalances toward target). Tested: 10 workers @ 70/30 -> 7/3; total preserved.
- **ArmyTrainer** — given current army composition, target composition, available resources, and army cap, returns the next unit type to train (or none). Tested: picks the type with the largest deficit vs target.
- **UpgradeScheduler** — given the ordered priority list, already-researched set, and resources, returns the next affordable upgrade respecting tier prerequisites. Tested: tier-II deferred until tier-I researched.
- **CombatResolver** — applies the rock-paper-scissors damage modifiers between unit types.

## Data Model
```
enum class Resource { FOOD, WOOD }
enum class UnitType { INFANTRY, ARCHER, CAVALRY }
enum class UpgradeId { DAMAGE_I, DAMAGE_II, DEFENSE_I, DEFENSE_II, SPEED_I, SPEED_II }
enum class Difficulty { EASY, NORMAL, HARD }

data class EconomyBalance(val foodPct: Int, val woodPct: Int)          // sums to 100
data class ArmyComposition(val ratios: Map<UnitType, Int>)            // proportional weights
data class UpgradePriority(val order: List<UpgradeId>)

data class MatchSettings(
    val difficulty: Difficulty,
    val economy: EconomyBalance,
    val army: ArmyComposition,
    val upgrades: UpgradePriority
)
data class ResourcePool(val food: Int, val wood: Int)
data class Worker(val assignedTo: Resource)
data class MilitaryUnit(val type: UnitType, val hp: Int)
```

## Out of Scope (v1)
- More than one bot opponent; team/multiplayer.
- Buildings beyond bases (no walls, towers, tech buildings to place).
- Manual worker assignment or manual unit queueing.
- Map editor, fog-of-war variants, naval units.
- Campaign/missions (single skirmish vs one bot only).
- More than two resources.
