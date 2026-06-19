# Empire Skirmish — Design Document

## Overview
A compact **turn-based tactics** game on a grid. The player commands a small band — **swordsmen, bowmen, mages, and a king** — against an enemy band. Units **move a short distance and attack** when foes are in range. The single rule that matters above all: **don't let your king die.** Levels are **procedurally generated but simulated for solvability** before play, so every skirmish is winnable with good tactics.

Level solvability uses the shared technique in [../../common/level-solvability.md](../../common/level-solvability.md). Offline, local-only, single player vs. AI.

## Design Pillars
- **Small battles, sharp decisions.** A handful of units per side; every move counts.
- **Protect the king.** Losing the king is instant defeat — positioning and screening are the core tension.
- **Always solvable.** Random maps are verified beatable; difficulty tunes the margin, not fairness.
- **No modals.** Selection and movement are inline on the board (project UX rules).

## Board & Units
- A rectangular **grid** of tiles; some tiles are **blocking terrain** (impassable), some give **cover** (optional defense bonus). Units occupy one tile.
- Movement and attacks are **range-limited** per unit type.

### Unit Types (v1)
| Unit | Move | Attack | Role |
|------|------|--------|------|
| **Swordsman** | medium | melee (adjacent), high damage | frontline; screens the king |
| **Bowman** | medium | ranged (line/area, several tiles), medium damage | picks off approaching enemies |
| **Mage** | short | ranged area-of-effect, hits a small cluster | crowd control / soft targets |
| **King** | short | weak melee | **must survive**; a unit, not a pawn — can fight but is fragile |

A light rock-paper-scissors keeps choices meaningful (e.g. swordsmen close on bowmen, bowmen outrange swordsmen, mages punish clustering). Exact numbers are data.

## Turn Structure
- Strict **alternating turns**: the player moves/acts with their units, then the enemy AI does.
- On a unit's activation: **move up to its range**, then **attack** if an enemy is within attack range (move-then-attack; v1 keeps it to one move + one attack per unit per turn).
- A unit reduced to 0 HP is removed. **If a king (either side) dies, the battle ends immediately.**

```
+-----------------------------------------------+
| Your turn   Units: 4    Enemy: 5              |  <- HUD (top)
+-----------------------------------------------+
|  . . # . . E . .                              |   # blocking terrain
|  . S . . . . b .                              |   S/b/m/K = your sword/bow/mage/king
|  . K m . ^ . . .                              |   E/... = enemy units
|  . . . . . . . s                              |   (movement range highlights on select)
+-----------------------------------------------+
| [ End Turn ]                 [ Undo move ]    |  <- action bar (bottom)
+-----------------------------------------------+
```

## Controls
- **Tap a unit** to select it; reachable tiles and attack targets highlight on the board (no overlay panel).
- **Tap a highlighted tile** to move; **tap a highlighted enemy** to attack.
- **Undo** the current unit's move before it attacks/commits; **End Turn** passes to the enemy.
- Tap/drag based, consistent with the project's modal-free conventions; no joystick.

## Win / Loss
- **Victory**: the **enemy king dies** (or, optionally, all enemies are eliminated).
- **Defeat**: **your king dies.** (Losing other units is survivable.)
- Result shown on a panel **below** the board, never overlaying it during play (per [../../common/victory-defeat.md](../../common/victory-defeat.md)).

## Generation & Solvability (Monte Carlo)
Per [../../common/level-solvability.md](../../common/level-solvability.md):
1. **Generate** a candidate from a **seed**: grid, terrain, and starting placements/rosters for both sides.
2. **Simulate** many automated playthroughs (random + heuristic tactical policies for the player side vs. the enemy AI).
3. The **reference solution** is the best winning line found (e.g. **fewest units lost** / fewest turns).
4. **Reject** candidates with no winning rollout; **accept** otherwise and set difficulty from the margin.

This guarantees a winnable skirmish while keeping random variety.

## Difficulty (margin to the reference)
Difficulty adjusts the player's leeway over the reference solution rather than adding mechanics:

| Difficulty | Leeway |
|------------|--------|
| Easy   | favorable rosters/positions; the reference win loses few/no units; forgiving enemy AI |
| Normal | balanced rosters; competent enemy AI |
| Hard   | slim rosters/positions where even the reference solution is tight; aggressive enemy AI that targets the king |

The enemy AI aggression and the generated force balance are scaled per difficulty; solvability is always preserved.

## Combat Resolution (pure)
- A **pure controller** (`controller/`) resolves movement legality (range + terrain + occupancy), attack legality/range, and damage (with type modifiers and optional cover). No Android imports; deterministic, fully unit-testable.
- The same resolver powers both the in-game turns and the solvability simulation, so verification matches play exactly.

## Screen Flow
Standard per-game flow: **Settings → How to Play → Gameplay → Results**.
- **Settings (pre-battle)**: **difficulty** and Random vs. saved **seed**.
- **How to Play**: explains move-then-attack, unit ranges/roles, that the king must survive, and that levels are always winnable.

## State Machine
`EmpireSkirmishState` (KStateMachine, state exposed via `StateFlow`):
```
Idle
 └─ BattleStarted → PlayerTurn
PlayerTurn
 ├─ UnitMoved / UnitAttacked → PlayerTurn
 ├─ KingDied(player) → Defeat
 ├─ KingDied(enemy) → Victory
 └─ TurnEnded → EnemyTurn
EnemyTurn
 ├─ EnemyActed → EnemyTurn
 ├─ KingDied(player) → Defeat
 ├─ KingDied(enemy) → Victory
 └─ EnemyTurnEnded → PlayerTurn
Victory / Defeat
 └─ (replay / next / menu) → Idle
```
- States: `Idle`, `PlayerTurn`, `EnemyTurn`, `Victory`, `Defeat`.
- Events: `BattleStarted`, `UnitMoved`, `UnitAttacked`, `TurnEnded`, `EnemyActed`, `EnemyTurnEnded`, `KingDied`.

## Controllers (pure, `controller/`)
- **LevelGenerator** — `seed → CandidateBattle` (grid, terrain, rosters, placements).
- **MoveResolver** — reachable tiles given a unit's move range, terrain, and occupancy.
- **CombatResolver** — attack range/legality and damage with type modifiers and cover.
- **EnemyAi** — picks enemy moves/attacks (difficulty-scaled; prioritizes the player king on Hard).
- **SolvabilityVerifier** — many rollouts → `SolvabilityReport` (beatable? reference quality).

## Data Model
```
enum class UnitType { SWORDSMAN, BOWMAN, MAGE, KING }
enum class Side { PLAYER, ENEMY }
enum class Difficulty { EASY, NORMAL, HARD }
data class GridPos(val x: Int, val y: Int)
data class Tile(val pos: GridPos, val blocking: Boolean, val cover: Boolean)
data class Unit(val type: UnitType, val side: Side, val pos: GridPos, val hp: Int)
data class Battle(val grid: List<Tile>, val units: List<Unit>, val seed: Long)
data class UnitStats(val moveRange: Int, val attackRange: Int, val damage: Int, val maxHp: Int)
```

## Out of Scope (v1)
- Multiple moves/attacks per unit per turn; action-point economies.
- Fog of war, capturing nodes, reinforcements over time.
- More than the four unit types; unit leveling between battles.
- Campaign / persistent army between skirmishes.
