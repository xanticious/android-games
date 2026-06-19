# Tower Defense (Lite)

Shared board, economy, enemy, and tower rules for the lite tower-defense games (Base Defense, Randomized Dice TD). Each game layers its own twist on top — Base Defense lets the player choose and place towers freely; Randomized Dice TD chooses placement/type by dice — but everything below is common.

## Core Concept

Enemies travel along a fixed **path** from a spawn to the player's **base**. The player places **towers** on buildable tiles beside the path; towers automatically target and attack enemies in range. Each enemy that reaches the base costs a **life**. Survive all waves to win the level.

```
+-----------------------------------------------+
| Lives: ###     Money: $###     Wave: x / N    |  <- HUD (top)
+-----------------------------------------------+
|  [T]                                          |
|   #######  PATH  ########                     |
|         #               #   [T]               |
|   [T]   ###### >> >> >> ###                   |
|                                BASE [*]        |
+-----------------------------------------------+
| [ Call Next Wave Early ]   [ tower palette ]  |  <- action bar (bottom)
+-----------------------------------------------+
```

## Map (random per run)

- The board is a **grid**. A path is generated from a spawn edge to the base; remaining tiles are **buildable** or **blocked** (decor/terrain).
- Generation guarantees a single continuous walkable path from spawn to base and at least a minimum number of buildable tiles adjacent to it (so the level is playable).
- Because maps are random, **not every level is guaranteed beatable** — this is accepted by design. (Games that must guarantee solvability use [level-solvability.md](level-solvability.md); these lite TD games deliberately do not.)
- Colors and tile art reference `ui/theme/Color.kt` only.

## Enemies & Waves

- Enemies spawn in **waves** (groups). A wave is a timed sequence of enemies with a per-wave count and HP/speed scaling that grows each wave.
- Each enemy has **HP**, **move speed**, and a **bounty** (money awarded on kill).
- An enemy that reaches the base removes one **life** and despawns. Reaching **0 lives** ends the run in defeat.
- Enemies follow the path; **slow** effects reduce their move speed for a duration.

## Economy

- The player has **money**, spent on buying and upgrading towers.
- Income sources: starting money, **bounty** per killed enemy, and an **early-call bonus**.
- **Call Next Wave Early**: the player may summon the next wave before its timer expires; doing so grants a **bonus proportional to the unspent timer** (more time skipped = bigger bonus). This is the main risk/reward lever — more pressure, more money.
- All costs/bounties are integer money values defined per game.

## Towers (shared framework)

Every tower shares this shape; individual games define the concrete set:

- **Range** — radius (in tiles) within which it can target enemies.
- **Damage** — per-hit damage (or per-tick for AoE/slow).
- **Fire rate** — attacks per second.
- **Targeting** — default *first* (enemy furthest along the path within range); ties broken by lowest HP.
- **Upgrade levels** — each tower can be upgraded with money to improve its stats; cost rises per level.
- Towers occupy one buildable tile and cannot be placed on the path or blocked tiles.

### Standard Tower Roles (v1)

The lite games draw from three roles so behavior stays readable:

| Role | Effect | Targeting note |
|------|--------|----------------|
| **Single-target** | High damage to one enemy at a time | Best vs. tough single enemies |
| **AoE** | Lower damage to all enemies in a small radius around the impact | Best vs. clustered groups |
| **Slow** | Little/no damage; applies a movement **slow** to enemies in range | Force-multiplier for the other two |

## Tower Targeting & Combat (pure)

- A pure controller resolves, per tick: which enemies are in each tower's range, target selection, damage application, slow application, kills, and bounties awarded.
- No Android/UI imports; deterministic given enemy positions and tower stats, so it is fully unit-testable (e.g. *single-target tower hits the furthest-progressed enemy in range*, *AoE damages all enemies within radius*, *slow reduces speed by X% for Y ms*).

## Win / Loss

- **Victory**: all waves cleared with **lives > 0**.
- **Defeat**: **lives reach 0**.
- Result is shown on a panel **below** the board, never overlaying it during play (per [victory-defeat.md](victory-defeat.md)).

## State Machine (shared shape)

```
Idle
 └─ LevelStarted → Building
Building
 ├─ TowerPlaced / TowerUpgraded → Building   (spend money)
 └─ WaveStarted (timer or early-call) → Wave
Wave
 ├─ Tick → Wave        (enemies move, towers fire, money/lives update)
 ├─ WaveCleared → Building   (more waves remain)
 ├─ AllWavesCleared → Victory
 └─ BaseOverrun → Defeat
Victory / Defeat
 └─ (replay / menu) → Idle
```

- States: `Idle`, `Building`, `Wave`, `Victory`, `Defeat`.
- Events: `LevelStarted`, `TowerPlaced`, `TowerUpgraded`, `WaveStarted`, `Tick`, `WaveCleared`, `AllWavesCleared`, `BaseOverrun`.

## Data Model (shared shape)

```
enum class TowerRole { SINGLE_TARGET, AOE, SLOW }
data class TowerStats(val range: Float, val damage: Int, val fireRate: Float, val slowPct: Int = 0)
data class Tower(val role: TowerRole, val tile: GridPos, val level: Int)
data class Enemy(val hp: Int, val speed: Float, val bounty: Int, val pathProgress: Float)
data class Wave(val enemies: List<Enemy>, val startDelayMs: Long)
data class TdMap(val grid: Grid, val path: List<GridPos>, val basePos: GridPos)
```
