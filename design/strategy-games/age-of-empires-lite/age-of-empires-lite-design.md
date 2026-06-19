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
- **Food** (shown as a **grain icon**) — used to **train military units**.
- **Study** (shown as a **book icon**) — used to **research upgrades**.

There is no wood, stone, or gold; folding everything into these two resources avoids extra management.

### Workers (Automated)
- Workers **spawn automatically** at the **Town Center** on a **fixed time interval** (e.g. one every N seconds), **at no resource cost**, until the **worker cap** is reached.
- Each new worker walks from the Town Center to an **unoccupied spot on the farmland** (producing Food) or to an **unoccupied seat in the Library** where it reads a book (producing Study), contributing to a global resource income.
- Workers **self-assign** between farmland and Library seats to track the **Economy Balance** target as closely as possible (see below). When the target changes, idle/next-rebalanced workers shift assignment; there is no manual assignment.
- Worker cap and spawn interval are fixed per difficulty/map (not micromanaged by the player).

### Economy Balance (Player Policy)
- The player sets a **percentage split** between Food and Study, e.g. `50% Food / 50% Study`, via a single slider.
- The auto-assigner distributes workers so the **proportion of gatherers** matches the target split (rounding to whole workers). Example: 10 workers at 70/30 → 7 farming Food, 3 studying in the Library.
- Changing the slider mid-match smoothly **re-balances** existing workers over the next few assignment ticks (no instant teleport; a worker finishes its current trip then may switch between farmland and Library).
- This is the player's primary economic lever and can be changed at any time from the in-match policy bar.

## Initial Village
Each side starts with a small, pre-built settlement:
- **Town Center** — surrounded by **farmland**. Workers are generated here and walk out to an unoccupied space on the farmland or to the Library.
- **Library** — depicted with **greek columns** and **open outdoor reading areas**. Workers walk to an unoccupied seat and read a book to produce **Study**.
- **Barracks** — military units are produced here and walk to the **parade grounds** once assembled, or to the player-set **rally point**.
- **City wall** with **bow towers** and **auto-opening/closing gates** surrounding the settlement. The wall and gates **can be destroyed**, but **automatically rebuild after some duration**.
- A couple of **workers**, one **military unit**, and a **King**.

The King is the settlement's most important figure — losing the King ends the game (see Win / Loss).

## Military

### Army Composition (Player Policy)
- The player specifies the **desired makeup** of their army as proportions across the available unit types, e.g. `50% Infantry / 30% Archers / 20% Cavalry`.
- The civilization **auto-trains** units to converge on that composition **whenever resources allow**. Unlike workers, **military units cost Food** and respect a **population/army cap**.
- If Food is insufficient, training waits; the auto-trainer always trains the unit type that brings the live army closest to the target ratio next.
- Reinforcements trained during battle automatically rally to a player-set **rally point** near the front.

### Unit Types (v1)
A small rock-paper-scissors triangle keeps composition meaningful:
| Unit | Strong vs | Weak vs | Cost | Role |
|------|-----------|---------|------|------|
| Infantry | Cavalry | Archers | low Food | Frontline, durable |
| Archers  | Infantry | Cavalry | medium Food | Ranged damage |
| Cavalry  | Archers | Infantry | high Food | Fast flankers |

### Upgrades & Upgrade Priority (Player Policy)
- Upgrades improve units along tracks: **Damage I/II**, **Defense I/II**, **Speed I/II** (tiered; II requires I).
- A special **Enlightenment** upgrade becomes available only **after every other upgrade is researched**; completing it wins the match via the Enlightenment victory condition (see Win / Loss).
- The player specifies an **ordered Upgrade Priority list**, e.g. `Damage I → Defense I → Damage II → Speed I`.
- The civilization **auto-researches** upgrades **in that order** as Study becomes available; **upgrades cost Study**. A tier-II entry is skipped (deferred) until its tier-I prerequisite is researched, then taken when reached in order.
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
There are multiple paths to victory. A match ends the moment any one of these is met by either side.

- **Military Might** — **destroy the enemy's King.** This is the classic conquest victory; protect your own King while hunting down the opponent's.
- **Enlightenment** — **research every available upgrade, then research the special "Enlightenment" upgrade.** When a side begins the Enlightenment upgrade, it is **announced to all players** ("Enlightenment is being researched") and a **countdown timer** appears. If the timer completes, that side wins; if their Library production is interrupted such that Study runs out, the research can stall (the countdown is tied to completing the upgrade). This gives opponents a window to react militarily.
- **Swords Beaten into Ploughshares** — a peaceful/economic victory: if you ever have more than _[TODO: threshold not specified — see note below]_.

> **Note:** The problem statement that requested this rewrite was cut off mid-sentence at the "Swords Beaten into Ploughshares" condition ("...if you ever have more than"). The exact threshold (e.g. a ratio of workers to military units, or a worker count) needs to be confirmed before this condition is finalized.

- **Defeat**: your own King is destroyed (the opponent achieves Military Might), or an opponent reaches any of their own victory conditions first.
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
| Food: ###   Study: ###  Workers x/cap  Army%  |  <- resource/HUD strip (top)
+-----------------------------------------------+
|                                               |
|                BATTLEFIELD                    |
|        (pan / zoom, units, villages)          |
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
 ├─ TickElapsed → Playing        (worker spawn, gather, auto-train, auto-research, wall rebuild)
 ├─ EnlightenmentStarted → Playing   (announce + countdown begins)
 ├─ PlayerKingDestroyed → Defeat
 ├─ EnemyKingDestroyed → Victory          (Military Might)
 ├─ PlayerEnlightenmentCompleted → Victory (Enlightenment)
 ├─ EnemyEnlightenmentCompleted → Defeat
 ├─ PlayerPloughsharesReached → Victory    (Swords Beaten into Ploughshares)
 └─ EnemyPloughsharesReached → Defeat
Victory / Defeat
 └─ (replay / menu) → Idle
```
- States: `Idle`, `Playing`, `Victory`, `Defeat`.
- Events: `MatchStarted`, `PolicyChanged`, `TickElapsed`, `EnlightenmentStarted`, `PlayerKingDestroyed`, `EnemyKingDestroyed`, `PlayerEnlightenmentCompleted`, `EnemyEnlightenmentCompleted`, `PlayerPloughsharesReached`, `EnemyPloughsharesReached`.

## Controllers (pure, `controller/`)
Automation logic is pure and unit-testable, with no Android/UI imports:
- **WorkerAssigner** — given worker count and an Economy Balance target, returns the per-resource worker allocation (proportional rounding; rebalances toward target). Tested: 10 workers @ 70/30 -> 7 food / 3 study; total preserved.
- **ArmyTrainer** — given current army composition, target composition, available Food, and army cap, returns the next unit type to train (or none). Tested: picks the type with the largest deficit vs target.
- **UpgradeScheduler** — given the ordered priority list, already-researched set, and available Study, returns the next affordable upgrade respecting tier prerequisites (Enlightenment requires all other upgrades). Tested: tier-II deferred until tier-I researched.
- **CombatResolver** — applies the rock-paper-scissors damage modifiers between unit types.

## Data Model
```
enum class Resource { FOOD, STUDY }
enum class UnitType { INFANTRY, ARCHER, CAVALRY }
enum class UpgradeId { DAMAGE_I, DAMAGE_II, DEFENSE_I, DEFENSE_II, SPEED_I, SPEED_II, ENLIGHTENMENT }
enum class Difficulty { EASY, NORMAL, HARD }

data class EconomyBalance(val foodPct: Int, val studyPct: Int)         // sums to 100
data class ArmyComposition(val ratios: Map<UnitType, Int>)            // proportional weights
data class UpgradePriority(val order: List<UpgradeId>)

data class MatchSettings(
    val difficulty: Difficulty,
    val economy: EconomyBalance,
    val army: ArmyComposition,
    val upgrades: UpgradePriority
)
data class ResourcePool(val food: Int, val study: Int)
data class Worker(val assignedTo: Resource)                          // FOOD = farmland, STUDY = library seat
data class MilitaryUnit(val type: UnitType, val hp: Int)
data class King(val hp: Int)
```

## Out of Scope (v1)
- More than one bot opponent; team/multiplayer.
- Player-placed buildings (the initial village's Town Center, Library, Barracks, walls, towers, and gates are fixed and pre-built; players do not construct new ones).
- Manual worker assignment or manual unit queueing.
- Map editor, fog-of-war variants, naval units.
- Campaign/missions (single skirmish vs one bot only).
- More than two resources.
