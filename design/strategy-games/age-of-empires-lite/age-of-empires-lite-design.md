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
- Workers are **civilians**: they **cannot be targeted, attacked, or killed** by either side. They go about their economy work safely regardless of nearby combat.

<a id="worker-cap-tbd"></a>
> **Worker cap — TBD.** How big should the cap actually be? We haven't decided costs or production rates, so this is **relative** and `TBD` for now. It must be balanced jointly with the spawn interval, per-worker Food rate, military unit costs, and the Ploughshares threshold so that a heavily-farming player can win in ~10–15 minutes (see [Win / Loss](#win--loss)). We'll tweak all of these values together later.

### Economy Balance (Player Policy)
- The player sets a **percentage split** between Food and Study, e.g. `50% Food / 50% Study`, via a single slider.
- The auto-assigner distributes workers so the **proportion of gatherers** matches the target split (rounding to whole workers). Example: 10 workers at 70/30 → 7 farming Food, 3 studying in the Library.
- Changing the slider mid-match smoothly **re-balances** existing workers over the next few assignment ticks (no instant teleport; a worker finishes its current trip then may switch between farmland and Library).
- This is the player's primary economic lever and can be changed at any time from the in-match policy bar.

## Initial Village
Each side starts with a small, pre-built settlement:
- **Town Center** — surrounded by **farmland**. Workers are generated here and walk out to an unoccupied space on the farmland or to the Library. The Town Center **cannot be targeted or destroyed**.
- **Library** — depicted with **greek columns** and **open outdoor reading areas**. Workers walk to an unoccupied seat and read a book to produce **Study**.
- **Barracks** — military units are produced here and walk to the **parade grounds** once assembled, or to the player-set **rally point**. The Barracks **cannot be targeted or destroyed**.
- **City wall** with **bow towers** and **auto-opening/closing gates** surrounding the settlement. **Sections of wall, gates, and bow towers are the only structures that can be destroyed**; once destroyed they **automatically rebuild after some duration**.
- A couple of **workers**, one **military unit**, and a **King**.

The King is the settlement's most important figure — losing the King ends the game (see Win / Loss).

**What can be destroyed:** only **sections of enemy walls, gates, and bow towers**. Workers (civilians), Town Centers, Barracks, and the Library cannot be targeted or destroyed. The King is defeated only via the Military Might condition.

## Military

### Army Composition (Player Policy)
- The player specifies the **desired makeup** of their **ratio-based army** as proportions across the standard unit types, e.g. `50% Infantry / 25% Archers / 25% Cavalry`.
- The civilization **auto-trains** units to converge on that composition **whenever resources allow**. Unlike workers, **military units cost Food** and respect a **population/army cap**.
- If Food is insufficient, training waits. When it's time to queue a unit, the auto-trainer picks the unit type that is **farthest below** its target ratio (the largest deficit). If the live army is **exactly at ratio**, it queues the **cheapest** option.
  - This produces **interleaving** rather than batching: if you escaped a battle with lots of Cavalry but lost your Infantry and Archers, the trainer gradually alternates between Infantry and Archers to rebuild a balanced army, instead of producing all of one type and then all of the other.
- **Cannons are excluded from the ratio.** Because they're very expensive and you typically want only one or two, they are **not** part of the percentage composition (see below).
- Reinforcements trained during battle automatically rally to a player-set **rally point** near the front.

### Cannons (Manual, Off-Ratio Production)
- The **Cannon** is a specialist siege unit that is **not** governed by the Army Composition ratios.
- A **"Build Cannon (1, 2, or 3)"** control lets the player set the **next queued unit(s)** to be cannons. Selecting `2`, for example, queues the next two production slots as cannons.
- After the requested cannons are produced, production **reverts to the ratio-based logic** automatically.
- Cannons still cost Food (a high amount) and obey the army cap like any other unit.

### Unit Healing (Out-of-Combat Regeneration)
- Any **military unit that has been out of battle** for at least a **minimum cooldown** _[TBD duration]_ begins to **passively regenerate hit points** over time, up to its **full health**.
- Taking or dealing damage (re-entering battle) **resets** the cooldown; the unit must again stay clear of combat for the minimum time before regen resumes.
- This lets the player pull damaged units back to recover rather than losing them, reinforcing the "retreat and micro" combat fantasy. Regen rate and cooldown are **TBD** and tuned with the rest of the economy.

### Unit Types (v1)
A small rock-paper-scissors triangle keeps composition meaningful, plus a dedicated siege unit:
| Unit | Strong vs | Weak vs | Cost | Role |
|------|-----------|---------|------|------|
| Infantry | Cavalry | Archers | low Food | Frontline, durable |
| Archers  | Infantry | Cavalry | medium Food | Ranged damage |
| Cavalry  | Archers | Infantry | high Food | Fast flankers |
| Cannon   | Walls / gates / bow towers | Infantry, Archers, Cavalry (slow, fragile in open battle) | very high Food | Siege — destroys enemy fortifications |

- The **Cannon** specializes in **destroying the only destructible structures** (sections of enemy walls, gates, and bow towers). It is slow and weak against regular units, so it needs an escort.

### Upgrades & Upgrade Priority (Player Policy)
- Upgrades are organized into **tiered tracks** (each tier requires the previous one). Costs and magnitudes are **TBD** and will be tuned with the rest of the economy:
  - **Attack I/II/III** — increase **military unit damage** for all units.
  - **Defense I/II/III** — increase **military unit health** for all units.
  - **Speed I/II/III** — increase **military unit speed** for all units.
  - **City Wall I/II/III** — increase **wall health** and **bow tower damage**, and **reduce wall/gate/tower rebuild wait**.
  - **Agriculture I/II/III** — increase **Food production rate**.
  - **Learning I/II/III** — increase **Study production rate**.
- A special **Enlightenment** upgrade becomes available only **after every other upgrade is researched**; completing it wins the match via the Enlightenment victory condition (see Win / Loss).
- The player specifies an **ordered Upgrade Priority list**, e.g. `Attack I → Defense I → Agriculture I → Attack II`.
- The civilization **auto-researches** upgrades **in that order** as Study becomes available; **upgrades cost Study**. A higher-tier entry is skipped (deferred) until its lower-tier prerequisite is researched, then taken when reached in order.
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
- **Swords Beaten into Ploughshares** — a peaceful/economic victory: **accumulate a target amount of Food while surviving** _[TBD: exact threshold]_. The intended fantasy is a player who puts a few workers on Study but **almost all on farming**, raises a **skeleton army with basic upgrades**, and uses **superior defensive positioning** to hold out against a stronger enemy military until enough Food has been banked.

> **Note — values are TBD and relative.** We have not settled on costs or production rates yet, so the exact Ploughshares threshold (and most other numeric levers) can't be pinned down. Write `TBD` for now and tune everything together later. The threshold should be sized so a Ploughshares-focused player wins in roughly the same **~10–15 minute** match length as the other victory paths. Open questions to resolve when we tune:
> - **Max worker count?** _(TBD)_ — see [worker cap discussion](#worker-cap-tbd).
> - **Workers created every ___ seconds?** _(TBD)_
> - **Time to reach max worker capacity?** _(TBD minutes — derived from spawn interval × cap)_
> - **Food generated per farm worker per minute?** _(TBD)_
> - **Cost of each military unit (in Food)?** _(TBD)_
> - **Ploughshares Food threshold?** _(TBD)_ — pick so an almost-all-farming player reaches it in ~10–15 min.
>
> These are interdependent: pick the worker cap, spawn interval, per-worker Food rate, and unit costs together so the three victory paths converge on a similar match length.

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
+-----------------------------------------------------------+
| Food: ###   Study: ###  Workers x/cap  Army%              |  <- resource/HUD strip (top)
+-----------------------------------------------------------+
|                                                           |
|                       BATTLEFIELD                         |
|             (pan / zoom, units, villages)                 |
|                                                           |
+-----------------------------------------------------------+
| [Econ 50/50] [Army comp] [Build Cannon] [Upgrades] [Rally] |  <- inline policy bar (edge)
+-----------------------------------------------------------+
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
 ├─ TickElapsed → Playing        (worker spawn, gather, auto-train, auto-research, wall rebuild, out-of-combat unit regen)
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
- **ArmyTrainer** — given current army composition, target ratios, any pending **cannon build requests**, available Food, and army cap, returns the next unit type to train (or none). Cannon requests take priority and are consumed first; otherwise it picks the ratio unit with the **largest deficit**, or the **cheapest** unit when the army is exactly at ratio. Tested: cannon request produces a cannon then reverts to ratio; picks the type with the largest deficit vs target; ties at exact ratio pick the cheapest.
- **UnitRegenerator** — given units with their time-since-last-combat and a regen cooldown/rate, returns updated hit points (clamped to max) for units past the cooldown. Tested: a unit out of combat beyond the cooldown gains HP up to its max; a recently-damaged unit does not regen.
- **UpgradeScheduler** — given the ordered priority list, already-researched set, and available Study, returns the next affordable upgrade respecting tier prerequisites (each tier requires the previous; Enlightenment requires all other upgrades). Tested: higher tier deferred until lower tier researched.
- **CombatResolver** — applies the rock-paper-scissors damage modifiers between unit types.

## Data Model
```
enum class Resource { FOOD, STUDY }
enum class UnitType { INFANTRY, ARCHER, CAVALRY, CANNON }
enum class UpgradeId {
    ATTACK_I, ATTACK_II, ATTACK_III,
    DEFENSE_I, DEFENSE_II, DEFENSE_III,
    SPEED_I, SPEED_II, SPEED_III,
    CITY_WALL_I, CITY_WALL_II, CITY_WALL_III,
    AGRICULTURE_I, AGRICULTURE_II, AGRICULTURE_III,
    LEARNING_I, LEARNING_II, LEARNING_III,
    ENLIGHTENMENT
}
enum class Difficulty { EASY, NORMAL, HARD }

data class EconomyBalance(val foodPct: Int, val studyPct: Int)         // sums to 100
data class ArmyComposition(val ratios: Map<UnitType, Int>)            // proportional weights; CANNON excluded (built manually)
data class CannonBuildRequest(val count: Int)                        // 1, 2, or 3 next slots forced to CANNON, then reverts to ratios
data class UpgradePriority(val order: List<UpgradeId>)

data class MatchSettings(
    val difficulty: Difficulty,
    val economy: EconomyBalance,
    val army: ArmyComposition,
    val upgrades: UpgradePriority
)
data class ResourcePool(val food: Int, val study: Int)
data class Worker(val assignedTo: Resource)                          // FOOD = farmland, STUDY = library seat
data class MilitaryUnit(
    val type: UnitType,
    val hp: Int,
    val maxHp: Int,
    val secondsSinceLastCombat: Int                                 // drives out-of-combat regen
)
data class King(val hp: Int)
```

## Out of Scope (v1)
- More than one bot opponent; team/multiplayer.
- Player-placed buildings (the initial village's Town Center, Library, Barracks, walls, towers, and gates are fixed and pre-built; players do not construct new ones).
- Manual worker assignment or manual unit queueing.
- Map editor, fog-of-war variants, naval units.
- Campaign/missions (single skirmish vs one bot only).
- More than two resources.
