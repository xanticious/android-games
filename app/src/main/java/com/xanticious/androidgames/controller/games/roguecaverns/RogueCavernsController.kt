package com.xanticious.androidgames.controller.games.roguecaverns

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.roguecaverns.CombatAction
import com.xanticious.androidgames.model.games.roguecaverns.Element
import com.xanticious.androidgames.model.games.roguecaverns.Hero
import com.xanticious.androidgames.model.games.roguecaverns.HeroStats
import com.xanticious.androidgames.model.games.roguecaverns.Level
import com.xanticious.androidgames.model.games.roguecaverns.MetaProfile
import com.xanticious.androidgames.model.games.roguecaverns.Monster
import com.xanticious.androidgames.model.games.roguecaverns.PermanentUpgrades
import com.xanticious.androidgames.model.games.roguecaverns.RogueCavernsState
import com.xanticious.androidgames.model.games.roguecaverns.Room
import com.xanticious.androidgames.model.games.roguecaverns.RoomType
import com.xanticious.androidgames.model.games.roguecaverns.RunSummary
import kotlin.random.Random

// ─── Combat result ─────────────────────────────────────────────────────────────

data class CombatTurnResult(
    val heroHp: Int,
    val monsterHp: Int,
    val fled: Boolean,
    val log: String
)

// ─── Level Generator ───────────────────────────────────────────────────────────

object LevelGenerator {

    private const val COLS = 6
    private const val ROWS = 5

    /**
     * Builds a deterministic [Level] for [depth] using [seed].
     *
     * Layout rules:
     * - 1 SAFE room at the hero's start position (0, rows/2)
     * - 1 DESCENT room (not at start)
     * - 0-1 additional SAFE rooms
     * - 2-3 TREASURE rooms
     * - ~40% of remaining rooms become MONSTER; the rest stay EMPTY
     */
    fun generateLevel(depth: Int, seed: Long): Level {
        val random = Random(seed)
        val startPos = GridPos(0, ROWS / 2)

        val allPositions = (0 until COLS).flatMap { x -> (0 until ROWS).map { y -> GridPos(x, y) } }
        val shuffled = allPositions.filter { it != startPos }.shuffled(random)

        val roomTypeMap = mutableMapOf<GridPos, RoomType>()
        roomTypeMap[startPos] = RoomType.SAFE

        var idx = 0

        // DESCENT — prefer columns further right (x >= 3) so the map has a sense of direction
        val descentCandidates = shuffled.filter { it.x >= 3 }
        val descentPos = descentCandidates.firstOrNull() ?: shuffled[idx]
        roomTypeMap[descentPos] = RoomType.DESCENT
        while (idx < shuffled.size && shuffled[idx] == descentPos) idx++

        // 0-1 extra SAFE rooms
        val extraSafe = random.nextInt(2)
        repeat(extraSafe) {
            if (idx < shuffled.size) roomTypeMap[shuffled[idx++]] = RoomType.SAFE
        }

        // 2-3 TREASURE rooms
        val treasureCount = 2 + random.nextInt(2)
        repeat(treasureCount) {
            if (idx < shuffled.size) roomTypeMap[shuffled[idx++]] = RoomType.TREASURE
        }

        // Fill the rest: MONSTER (~40%) or EMPTY
        while (idx < shuffled.size) {
            val pos = shuffled[idx++]
            roomTypeMap[pos] = if (random.nextFloat() < 0.4f) RoomType.MONSTER else RoomType.EMPTY
        }

        val possibleMonsters = MonsterFactory.monstersForDepth(depth)
        var nextMonsterId = 1

        val rooms = allPositions.map { pos ->
            val type = roomTypeMap[pos] ?: RoomType.EMPTY
            val monster = if (type == RoomType.MONSTER) {
                possibleMonsters[random.nextInt(possibleMonsters.size)].copy(id = nextMonsterId++)
            } else null
            val treasure = if (type == RoomType.TREASURE) 50 + random.nextInt(51) else 0
            Room(pos = pos, type = type, explored = pos == startPos, monster = monster, treasure = treasure)
        }

        return Level(depth = depth, rooms = rooms, cols = COLS, rows = ROWS, seed = seed)
    }
}

// ─── Monster Factory ───────────────────────────────────────────────────────────

object MonsterFactory {

    /**
     * Returns template monsters available at [depth].  The [id] field is 0
     * (placeholder); [LevelGenerator] assigns unique IDs when placing them.
     */
    fun monstersForDepth(depth: Int): List<Monster> = when {
        depth <= 1 -> listOf(
            Monster(0, "Bat",  maxHp = 20, hp = 20, attack = 5,  defense = 2, element = Element.AIR,   xpReward = 30,  depth = depth),
            Monster(0, "Rat",  maxHp = 15, hp = 15, attack = 7,  defense = 1, element = Element.NONE,  xpReward = 25,  depth = depth)
        )
        depth == 2 -> listOf(
            Monster(0, "Cave Spider", maxHp = 30, hp = 30, attack = 8,  defense = 3, element = Element.EARTH, xpReward = 50,  depth = depth),
            Monster(0, "Goblin",      maxHp = 35, hp = 35, attack = 10, defense = 4, element = Element.NONE,  xpReward = 60,  depth = depth)
        )
        else -> listOf(
            Monster(0, "Troll",  maxHp = 60, hp = 60, attack = 15, defense = 6, element = Element.EARTH, xpReward = 100, depth = depth),
            Monster(0, "Dragon", maxHp = 80, hp = 80, attack = 20, defense = 8, element = Element.FIRE,  xpReward = 150, depth = depth)
        )
    }
}

// ─── Combat Resolver ───────────────────────────────────────────────────────────

object CombatResolver {

    /**
     * Resolves one full turn (hero acts then monster acts).
     *
     * Element advantage table (+25% attacker damage):
     * - FIRE  > EARTH
     * - WATER > FIRE
     * - EARTH > WATER
     * - AIR   > all
     *
     * SKILL: 1.5× hero damage, but hero's defense is ignored this turn.
     * USE_ITEM: heals hero for 15 HP; monster still attacks.
     * FLEE: 50% chance — if successful, returns immediately with fled = true.
     */
    fun resolveTurn(
        hero: Hero,
        monster: Monster,
        heroAction: CombatAction,
        monsterAction: CombatAction,
        random: Random = Random.Default
    ): CombatTurnResult {
        var heroHp = hero.hp
        var monsterHp = monster.hp
        val log = StringBuilder()
        var heroSkippedDefense = false

        // ── Hero action ───────────────────────────────────────────────────────
        when (heroAction) {
            CombatAction.ATTACK -> {
                val dmg = calcDamage(hero.stats.attack, monster.defense, hero.stats.element, monster.element, random)
                monsterHp -= dmg
                log.append("Hero attacks for $dmg damage.")
            }
            CombatAction.SKILL -> {
                heroSkippedDefense = true
                val base = maxOf(1, hero.stats.attack - monster.defense)
                val variance = (base * 0.2f * (random.nextFloat() * 2f - 1f)).toInt()
                val elemMult = elementMultiplier(hero.stats.element, monster.element)
                val dmg = maxOf(1, ((base + variance) * 1.5f * elemMult).toInt())
                monsterHp -= dmg
                log.append("Hero uses skill for $dmg damage!")
            }
            CombatAction.USE_ITEM -> {
                val healed = minOf(15, hero.stats.maxHp - heroHp)
                heroHp += healed
                log.append("Hero uses item, restoring $healed HP.")
            }
            CombatAction.FLEE -> {
                return if (random.nextFloat() < 0.5f) {
                    CombatTurnResult(heroHp, monsterHp, fled = true, log = "Hero fled successfully!")
                } else {
                    // Flee failed — monster still attacks
                    val dmg = calcDamage(
                        monster.attack, hero.stats.defense, monster.element, hero.stats.element, random
                    )
                    heroHp -= dmg
                    log.append("Hero failed to flee! ${monster.name} attacks for $dmg damage.")
                    CombatTurnResult(heroHp.coerceAtLeast(0), monsterHp.coerceAtLeast(0), fled = false, log = log.toString())
                }
            }
        }

        // ── Monster action (only if monster is still alive) ───────────────────
        if (monsterHp > 0) {
            val effectiveDefense = if (heroSkippedDefense) 0 else hero.stats.defense
            when (monsterAction) {
                CombatAction.ATTACK -> {
                    val dmg = calcDamage(monster.attack, effectiveDefense, monster.element, hero.stats.element, random)
                    heroHp -= dmg
                    log.append(" ${monster.name} attacks for $dmg damage.")
                }
                CombatAction.SKILL -> {
                    val base = maxOf(1, monster.attack - effectiveDefense)
                    val variance = (base * 0.2f * (random.nextFloat() * 2f - 1f)).toInt()
                    val elemMult = elementMultiplier(monster.element, hero.stats.element)
                    val dmg = maxOf(1, ((base + variance) * 1.5f * elemMult).toInt())
                    heroHp -= dmg
                    log.append(" ${monster.name} uses skill for $dmg damage!")
                }
                else -> {
                    // Monsters don't use items or flee
                    val dmg = calcDamage(monster.attack, effectiveDefense, monster.element, hero.stats.element, random)
                    heroHp -= dmg
                    log.append(" ${monster.name} attacks for $dmg damage.")
                }
            }
        } else {
            log.append(" ${monster.name} is defeated!")
        }

        return CombatTurnResult(
            heroHp = heroHp.coerceAtLeast(0),
            monsterHp = monsterHp.coerceAtLeast(0),
            fled = false,
            log = log.toString()
        )
    }

    private fun calcDamage(
        attack: Int,
        defense: Int,
        attackerElement: Element,
        defenderElement: Element,
        random: Random
    ): Int {
        val base = maxOf(1, attack - defense)
        val variance = (base * 0.2f * (random.nextFloat() * 2f - 1f)).toInt()
        val mult = elementMultiplier(attackerElement, defenderElement)
        return maxOf(1, ((base + variance) * mult).toInt())
    }

    private fun elementMultiplier(attacker: Element, defender: Element): Float {
        if (attacker == Element.NONE || defender == Element.NONE) return 1.0f
        return when {
            attacker == Element.AIR -> 1.25f
            attacker == Element.FIRE  && defender == Element.EARTH -> 1.25f
            attacker == Element.WATER && defender == Element.FIRE  -> 1.25f
            attacker == Element.EARTH && defender == Element.WATER -> 1.25f
            else -> 1.0f
        }
    }
}

// ─── Monster AI ────────────────────────────────────────────────────────────────

object MonsterAi {

    /**
     * Chooses the monster's combat action based on [difficulty] and the current
     * [heroHp].  No [Random] parameter — uses [Random.Default] internally so
     * callers do not need to thread a seed.
     */
    fun chooseMonsterAction(
        monster: Monster,
        heroHp: Int,
        difficulty: GameDifficulty
    ): CombatAction = when (difficulty) {
        GameDifficulty.EASY -> CombatAction.ATTACK
        GameDifficulty.MEDIUM -> {
            if (Random.Default.nextFloat() < 0.8f) CombatAction.ATTACK else CombatAction.SKILL
        }
        GameDifficulty.HARD -> when {
            // Go for the kill when hero is low
            heroHp <= 20 -> CombatAction.ATTACK
            else -> {
                val roll = Random.Default.nextFloat()
                when {
                    roll < 0.60f -> CombatAction.ATTACK
                    roll < 0.90f -> CombatAction.SKILL
                    else -> CombatAction.USE_ITEM
                }
            }
        }
    }
}

// ─── XP Calculator ─────────────────────────────────────────────────────────────

object XpCalculator {

    /**
     * Computes total XP earned for a run.
     *
     * base = depthReached × 100 + kills × 20
     * if banked: base × 1.2
     * fortune bonus: +20 % per rank
     */
    fun award(summary: RunSummary, fortune: Int): Long {
        var base = summary.depthReached * 100 + summary.kills * 20
        if (summary.banked) base = (base * 1.2).toInt()
        return (base * (1.0 + fortune * 0.2)).toLong()
    }
}

// ─── Meta Progression ──────────────────────────────────────────────────────────

object MetaProgression {

    /**
     * XP cost to advance an upgrade from [rank] → rank + 1.
     * Costs: 100, 300, 600, 1000, … (triangular scaling)
     */
    fun upgradeCost(rank: Int): Int = 100 * (rank + 1) * (rank + 2) / 2

    /** Builds a fresh [Hero] with base stats augmented by [upgrades]. */
    fun startingHero(upgrades: PermanentUpgrades): Hero {
        val stats = HeroStats(
            maxHp = 50 + upgrades.vitality * 20,
            attack = 10 + upgrades.power * 5,
            defense = 5 + upgrades.guard * 3
        )
        return Hero(stats = stats, hp = stats.maxHp)
    }

    fun canAfford(profile: MetaProfile, upgradeType: String): Boolean {
        val rank = rankFor(profile.upgrades, upgradeType) ?: return false
        return profile.totalXp >= upgradeCost(rank)
    }

    /**
     * Applies one rank to the named upgrade if the player can afford it.
     * Returns the updated [MetaProfile] or null if XP is insufficient or the
     * [upgradeType] is unrecognised.
     */
    fun applyUpgrade(profile: MetaProfile, upgradeType: String): MetaProfile? {
        val rank = rankFor(profile.upgrades, upgradeType) ?: return null
        val cost = upgradeCost(rank)
        if (profile.totalXp < cost) return null
        val newUpgrades = when (upgradeType) {
            "vitality" -> profile.upgrades.copy(vitality = profile.upgrades.vitality + 1)
            "power"    -> profile.upgrades.copy(power    = profile.upgrades.power    + 1)
            "guard"    -> profile.upgrades.copy(guard    = profile.upgrades.guard    + 1)
            "fortune"  -> profile.upgrades.copy(fortune  = profile.upgrades.fortune  + 1)
            else -> return null
        }
        return profile.copy(totalXp = profile.totalXp - cost, upgrades = newUpgrades)
    }

    private fun rankFor(upgrades: PermanentUpgrades, type: String): Int? = when (type) {
        "vitality" -> upgrades.vitality
        "power"    -> upgrades.power
        "guard"    -> upgrades.guard
        "fortune"  -> upgrades.fortune
        else -> null
    }
}

// ─── Exploration ───────────────────────────────────────────────────────────────

object Exploration {

    /**
     * Moves the hero to [target] (must be adjacent and in-bounds), marking that
     * room as explored.  Returns [state] unchanged if the move is not valid.
     */
    fun moveHero(state: RogueCavernsState, target: GridPos): RogueCavernsState {
        val level = state.currentLevel ?: return state
        if (target.manhattanDistanceTo(state.heroPos) != 1) return state
        if (target.x !in 0 until level.cols || target.y !in 0 until level.rows) return state

        val updatedRooms = level.rooms.map { room ->
            if (room.pos == target) room.copy(explored = true) else room
        }
        return state.copy(
            currentLevel = level.copy(rooms = updatedRooms),
            heroPos = target
        )
    }

    /** Descends to the next level using [nextLevelSeed] for procedural generation. */
    fun descend(state: RogueCavernsState, nextLevelSeed: Long): RogueCavernsState {
        val nextDepth = state.depth + 1
        val nextLevel = LevelGenerator.generateLevel(nextDepth, nextLevelSeed)
        return state.copy(
            currentLevel = nextLevel,
            heroPos = nextLevel.heroStartPos,
            depth = nextDepth,
            currentMonster = null,
            combatLog = emptyList()
        )
    }

    /**
     * Ends the run voluntarily (hero is in a SAFE room).  Computes the run
     * summary with XP earned and updates the meta profile.
     */
    fun bankAndExit(state: RogueCavernsState): RogueCavernsState {
        val baseSummary = RunSummary(state.depth, state.kills, banked = true, xpEarned = 0L)
        val xp = XpCalculator.award(baseSummary, state.metaProfile.upgrades.fortune)
        val summary = baseSummary.copy(xpEarned = xp)
        val updatedProfile = state.metaProfile.copy(
            totalXp = state.metaProfile.totalXp + xp,
            bestDepth = maxOf(state.metaProfile.bestDepth, state.depth)
        )
        return state.copy(metaProfile = updatedProfile, runSummary = summary)
    }
}
