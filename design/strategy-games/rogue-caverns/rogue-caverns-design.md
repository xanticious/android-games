# Rogue Caverns — Design Document

## Overview
A roguelite cavern crawler with **persistent power between lives**. Rather than punishing **permadeath**, Rogue Caverns takes the **Hades / Dauntless** approach: every run ends eventually, but you **earn experience based on how well you did**, and a portion of that becomes **permanent power** that makes your next descent stronger. Each run drops you into **procedurally generated** cavern levels; combat against monsters is **turn-based like Pokémon** (you and the enemy alternate choosing actions until one side faints). Die or dive deep — either way you come back a little tougher.

Offline, local-only, single player. All persistent data stays on device.

## Design Pillars
- **Death is progress, not punishment.** Every run feeds meta-progression; you always come back stronger.
- **Run-to-run momentum.** Procedural levels keep each descent fresh; permanent upgrades give a sense of growth.
- **Readable turn-based combat.** Pokémon-style: pick an action, see the result, repeat. No twitch.

## The Loop (two layers)

### 1. The Run (a single life)
1. Start a descent with your **current permanent power** (from meta-progression) and a fresh, empty run state.
2. Explore procedurally generated cavern **levels**; deeper levels are harder and more rewarding.
3. Encounter **monsters** → enter **turn-based combat**.
4. The run ends when your hero **faints** (HP hits 0) **or** you choose to **bank and exit** at a safe point.
5. On run end, convert performance into **experience** (see *Meta-Progression*).

### 2. Meta-Progression (between lives)
- Experience earned per run accrues to a **permanent profile** (local).
- Spend earned XP/currency on **permanent upgrades** (a small skill tree / stat boosts) that apply to every future run from the start.
- This is the Hades/Dauntless hook: **some of each life's power carries forward**, so even a short run advances you.

```
   +----------------- META (persists) -----------------+
   |  Permanent upgrades  •  Total XP  •  Best depth    |
   +---------------------------+------------------------+
                               | start run with current power
                               v
   +----------------- RUN (one life) -------------------+
   |  Procedural levels  ->  turn-based combat  ->  ...  |
   |  ends on faint or bank-and-exit                     |
   +---------------------------+------------------------+
                               | award XP by performance
                               ^----------- back to META
```

## Experience Earned per Life
A pure controller scores each run and converts it to XP. Doing *better* earns *more* (so risk and depth are rewarded):

| Factor | Effect on XP |
|--------|--------------|
| **Depth reached** | deeper levels = more XP (primary driver) |
| **Monsters defeated** | per-kill XP |
| **Bank-and-exit vs. faint** | banking safely may retain a bonus; fainting still grants the base XP earned so far |
| **Optional objectives** | small bonuses for treasures/challenges found |

Even a losing run yields XP, so the player always makes progress toward the next permanent upgrade.

## Permanent Upgrades (Meta)
Spent between runs; apply from the start of every future run. Kept small and legible (v1):
- **Vitality** — higher starting max HP.
- **Power** — higher base attack.
- **Guard** — damage reduction.
- **Fortune** — more XP / better loot rates.
- (Each upgrade has a few ranks; costs rise per rank.)

These are **permanent**; in-run pickups (below) are temporary to that life only.

## Procedural Levels
- Each level is a generated cavern layout (rooms/corridors on a grid) with monsters, optional treasures, and a descent point to the next level. **Depth** increases difficulty (monster strength/count) and reward.
- Generation is seeded; a run's **seed** is shown on the results screen so a memorable descent can be replayed.
- Generation is a **pure controller** (`controller/`): `(depth, seed) → Level`. No Android imports; deterministic and unit-testable (same seed ⇒ same level; deeper ⇒ stronger monsters).

## Turn-Based Combat (Pokémon-style)
When the hero meets a monster, play switches to a **turn-based encounter**:
- Each turn the player chooses an **action** (Attack, a special **Skill**, use an **Item**, or attempt to **Flee**); the monster's AI chooses its action; resolution is **alternating** by a speed/initiative order.
- Damage uses simple attack/defense math with optional **type/element** modifiers (a light rock-paper-scissors), echoing Pokémon's type chart at small scale.
- A combatant at **0 HP faints**: the monster is defeated (XP/loot), or the **hero faints** (the run ends).
- Combat is the only place a full-screen result is allowed, and only **after** the encounter resolves — the encounter view itself never overlays a victory/defeat banner mid-fight (per [../../common/victory-defeat.md](../../common/victory-defeat.md)).

```
+-----------------------------------------------+
|  Enemy: Cave Bat        HP ███████░░░          |
+-----------------------------------------------+
|                  (encounter scene)             |
+-----------------------------------------------+
|  You: Hero              HP █████░░░░░          |
+-----------------------------------------------+
|  [ Attack ]  [ Skill ]  [ Item ]  [ Flee ]    |  <- action menu (below)
+-----------------------------------------------+
```

## In-Run vs. Permanent Power
| Scope | Examples | Lifetime |
|-------|----------|----------|
| **In-run (temporary)** | level-up boosts, found weapons, consumables, elixirs | this life only; reset on run end |
| **Permanent (meta)** | Vitality/Power/Guard/Fortune ranks | forever; applied at the start of every run |

This separation gives both the moment-to-moment build variety of a roguelite run and the long-arc growth of Hades/Dauntless.

## Win / Loss & Run End
- There is **no permanent loss** — "death" simply ends the current life and grants its XP.
- A run ends on **faint** or **bank-and-exit**; the **Results** screen (below the board) shows depth reached, monsters defeated, **XP earned**, total XP / next upgrade progress, and the run **seed**.
- "Best depth" and total XP are tracked locally and highlighted on a new record.

## Screen Flow
Standard per-game flow, with a meta hub: **Settings → How to Play → (Meta Hub ⇄ Run) → Results**.
- **Settings**: difficulty (scales monster strength/XP rates) and Random vs. saved **seed**.
- **Meta Hub**: spend earned XP on permanent upgrades before starting a descent.
- **How to Play**: explains that death isn't permanent, each life earns XP toward permanent power, levels are procedural, and combat is turn-based.

## State Machine
`RogueCavernsState` (KStateMachine, state exposed via `StateFlow`):
```
Idle
 └─ HubOpened → MetaHub
MetaHub
 ├─ UpgradePurchased → MetaHub     (spend XP on permanent upgrades)
 └─ RunStarted → Exploring
Exploring
 ├─ MovedRoom → Exploring
 ├─ DescendedLevel → Exploring     (next procedural level, deeper)
 ├─ EncounterStarted → Combat
 ├─ BankedAndExited → RunResults
 └─ HeroFainted → RunResults
Combat
 ├─ TurnTaken → Combat             (player & monster alternate)
 ├─ MonsterDefeated → Exploring    (award XP/loot)
 └─ HeroFainted → RunResults
RunResults
 └─ (continue) → MetaHub           (XP banked into meta)
```
- States: `Idle`, `MetaHub`, `Exploring`, `Combat`, `RunResults`.
- Events: `HubOpened`, `UpgradePurchased`, `RunStarted`, `MovedRoom`, `DescendedLevel`, `EncounterStarted`, `TurnTaken`, `MonsterDefeated`, `BankedAndExited`, `HeroFainted`.

## Controllers (pure, `controller/`)
- **LevelGenerator** — `(depth, seed) → Level`.
- **CombatResolver** — applies an action pair (player + monster) → new HP/states, with type modifiers and initiative order. Deterministic; fully unit-testable.
- **MonsterAi** — chooses the monster's action each turn (difficulty-scaled).
- **XpCalculator** — `RunSummary → xpEarned` (depth + kills + objectives + bank bonus).
- **MetaProgression** — applies permanent upgrades to a starting hero; computes upgrade costs per rank.

## Data Model
```
enum class Element { NONE, FIRE, WATER, EARTH, AIR }     // light type chart
enum class Difficulty { EASY, NORMAL, HARD }
data class HeroStats(val maxHp: Int, val attack: Int, val defense: Int)
data class Hero(val stats: HeroStats, val hp: Int, val element: Element)
data class Monster(val name: String, val maxHp: Int, val hp: Int, val attack: Int, val defense: Int, val element: Element)
data class Level(val depth: Int, val rooms: List<Room>, val seed: Long)
data class PermanentUpgrades(val vitality: Int, val power: Int, val guard: Int, val fortune: Int)
data class RunSummary(val depthReached: Int, val kills: Int, val banked: Boolean)
data class MetaProfile(val totalXp: Long, val upgrades: PermanentUpgrades, val bestDepth: Int)
// XpCalculator.award(summary, fortune): Long
// MetaProgression.startingHero(upgrades): Hero
```

## Out of Scope (v1)
- Real-time/action combat (turn-based only).
- Multiple playable classes (single hero archetype in v1; permanent upgrades provide variety).
- Permadeath of meta-progression (meta is never lost).
- Crafting, town hubs beyond the upgrade screen, NPC questlines.
- Online or shared progression.
