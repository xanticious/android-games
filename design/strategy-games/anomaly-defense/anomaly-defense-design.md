# Anomaly Defense — Design Document

## Overview
A tower-defense game played **from the attacker's side**. Instead of building defenses, the player studies a fixed enemy defense — turret networks and the routes through them — then **buys a force from a budget** and **assigns those units to routes**, trying to push enough of the force through to the objective. Every level is **procedurally generated and pre-verified beatable**: the game generates the defenses and paths, then runs many simulated attacks (Monte Carlo) to confirm a winning plan exists before showing the level. Difficulty is expressed as how much **budget leeway** the player gets over that best-found solution.

Solvability is guaranteed via the shared technique in [../../common/level-solvability.md](../../common/level-solvability.md). Offline, local-only, single player.

## Design Pillars
- **You are the attacker.** The fun is reading a defense and engineering a breach, not building one.
- **Plan, then watch.** The player composes and assigns a force, commits, then the assault **auto-simulates**; no real-time micro.
- **Always fair.** Levels are proven beatable before play; difficulty tunes the *margin*, never solvability.
- **No modals.** Planning panels are inline; the battlefield is never covered (project UX rules).

## Anatomy of a Level
- A **map** with one or more **routes** running from attacker spawn(s) to the **objective**.
- **Defenders** placed along/near routes: turrets with a **range**, **damage**, **fire rate**, and targeting. Some routes are well covered (turret crossfire), others are gaps.
- A per-level **budget** (currency) the player spends on attacking units.
- The player **wins** if at least the required number of units (e.g. one, or a quota) reach the objective.

## Attacking Units (v1)
A small set with clear trade-offs so composition choices matter:

| Unit | Cost | Trait | Counters |
|------|------|-------|----------|
| **Runner** | low | fast, fragile | overwhelms by speed/numbers through light coverage |
| **Tank** | high | slow, high HP | soaks turret fire on heavy routes |
| **Shielded** | medium | absorbs first N hits (shield), then normal | timing windows / alpha-strike turrets |
| **Saboteur** | medium | briefly disables a turret it passes | opens a window for following units |

(Exact roster is data; the point is rock-paper-scissors against turret types and route coverage.)

## Player Flow Within a Level
1. **Recon** — the defenses and routes are revealed (turret ranges drawn). The player can inspect coverage before spending.
2. **Compose** — spend the **budget** to buy a unit mix (see roster). Remaining budget is shown live.
3. **Assign** — drop purchased units onto **routes** (and optionally a release order/timing). Multiple units can share a route.
4. **Commit** — launch the assault. The outcome **auto-simulates** with the same engine used for verification.
5. **Result** — win (quota reached) or loss (force wiped before quota); shown below the board. Retry re-opens Compose/Assign with the same level.

```
+-----------------------------------------------+
| Budget: $###   Objective: get 1 through       |  <- HUD (top)
+-----------------------------------------------+
|  spawn >--- ROUTE A ---[turret)--->  OBJECTIVE|
|        >--- ROUTE B --(turret)(turret)->       |
|        >--- ROUTE C ---------------->          |  (turret ranges shaded)
+-----------------------------------------------+
| [Runner $] [Tank $$$] [Shield $$] [Sabo $$]   |  <- buy palette
| [ Assign to: A | B | C ]      [ COMMIT ]       |  <- inline planning bar
+-----------------------------------------------+
```

## Generation & Solvability (Monte Carlo)
Per [../../common/level-solvability.md](../../common/level-solvability.md):
1. **Generate** a candidate: map, routes, turret placements/stats, and a candidate budget — all from a **seed**.
2. **Simulate** many automated attacker plans (random and heuristic compositions/assignments) against the static defense.
3. The **reference solution** is the **cheapest winning plan** found (its total unit cost = `referenceCost`).
4. **Reject** candidates where no plan wins; **accept** and set the **budget** from `referenceCost × difficulty margin`.

Because the defense is static and the assault deterministic given a plan + seed, simulation is cheap and the verification engine is identical to the in-game assault engine.

## Difficulty (budget margin)
Difficulty sets how much room the player has over the best solution the simulator found:

| Difficulty | Budget |
|------------|--------|
| Easy   | `referenceCost` × generous multiplier (plenty of leeway for a sloppy plan) |
| Normal | moderate multiplier |
| Hard   | **just a little more** than `referenceCost` (near-optimal play required) |

On Hard the budget barely exceeds the cheapest winning composition, so the player must essentially rediscover an optimal breach; on Easy they can brute-force with extra units.

## Assault Simulation (pure)
- The assault is resolved by a **pure controller** (`controller/`): given the static defense and the player's plan (units per route + order), step units along routes, apply turret fire (range/damage/fire-rate/targeting), shields, saboteur disables, and report which units reach the objective.
- Deterministic given plan + seed → identical results in verification and in play; no Android imports; fully unit-testable.

## Win / Loss
- **Victory**: the required number of units reach the objective.
- **Defeat**: the force is destroyed before meeting the quota (player may retry/replan).
- Result shown on a panel **below** the board, never overlaying it (per [../../common/victory-defeat.md](../../common/victory-defeat.md)).

## Screen Flow
Standard per-game flow: **Settings → How to Play → Gameplay → Results**.
- **Settings (pre-level)**: **difficulty** (budget leeway) and Random vs. saved **seed**.
- **How to Play**: explains that you attack, study the defense, spend a budget, assign to routes, then commit and watch the simulated breach.

## State Machine
`AnomalyDefenseState` (KStateMachine, state exposed via `StateFlow`):
```
Idle
 └─ LevelLoaded → Recon
Recon
 └─ PlanningStarted → Planning
Planning
 ├─ UnitBought / UnitAssigned → Planning   (budget & assignments update)
 └─ Committed → Assault
Assault
 ├─ AssaultTick → Assault                  (simulate units vs turrets)
 ├─ ObjectiveReached → Victory             (quota met)
 └─ ForceWiped → Defeat
Victory / Defeat
 └─ (retry / next / menu) → Idle
```
- States: `Idle`, `Recon`, `Planning`, `Assault`, `Victory`, `Defeat`.
- Events: `LevelLoaded`, `PlanningStarted`, `UnitBought`, `UnitAssigned`, `Committed`, `AssaultTick`, `ObjectiveReached`, `ForceWiped`.

## Controllers (pure, `controller/`)
- **LevelGenerator** — `seed → CandidateLevel` (map, routes, turrets, candidate budget).
- **AssaultSimulator** — `(defense, plan, seed) → AssaultResult` (also used for verification).
- **SolvabilityVerifier** — runs many `AssaultSimulator` rollouts → `SolvabilityReport` (beatable? `referenceCost`).
- **BudgetSetter** — `(referenceCost, difficulty) → budget`.

## Data Model
```
enum class AttackUnit { RUNNER, TANK, SHIELDED, SABOTEUR }
enum class Difficulty { EASY, NORMAL, HARD }
data class Turret(val pos: GridPos, val range: Float, val damage: Int, val fireRate: Float)
data class Route(val id: Int, val tiles: List<GridPos>)
data class Defense(val routes: List<Route>, val turrets: List<Turret>, val objective: GridPos, val quota: Int)
data class UnitCost(val unit: AttackUnit, val cost: Int)
data class Assignment(val routeId: Int, val units: List<AttackUnit>, val order: List<Int>)
data class AttackPlan(val assignments: List<Assignment>)
data class AssaultResult(val unitsThrough: Int, val won: Boolean)
data class Level(val defense: Defense, val budget: Int, val seed: Long)
```

## Out of Scope (v1)
- Real-time control of units during the assault (plan-then-simulate only).
- Player-built defenses (player is always the attacker).
- Air units / verticality; multi-objective maps.
- Campaign meta-progression between levels.
