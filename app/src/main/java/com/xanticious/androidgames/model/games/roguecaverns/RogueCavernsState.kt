package com.xanticious.androidgames.model.games.roguecaverns

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos

enum class Element { NONE, FIRE, WATER, EARTH, AIR }
enum class CombatAction { ATTACK, SKILL, USE_ITEM, FLEE }

data class HeroStats(
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val element: Element = Element.NONE
)

data class Hero(val stats: HeroStats, val hp: Int)

data class Monster(
    val id: Int,
    val name: String,
    val maxHp: Int,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val element: Element,
    val xpReward: Int,
    val depth: Int
)

enum class RoomType { EMPTY, MONSTER, TREASURE, DESCENT, SAFE }

data class Room(
    val pos: GridPos,
    val type: RoomType,
    val explored: Boolean,
    val monster: Monster?,
    val treasure: Int
)

data class Level(
    val depth: Int,
    val rooms: List<Room>,
    val cols: Int,
    val rows: Int,
    val seed: Long
) {
    val heroStartPos: GridPos get() = GridPos(0, rows / 2)
    val descentPos: GridPos? get() = rooms.firstOrNull { it.type == RoomType.DESCENT }?.pos
}

data class PermanentUpgrades(
    val vitality: Int = 0,
    val power: Int = 0,
    val guard: Int = 0,
    val fortune: Int = 0
)

data class MetaProfile(
    val totalXp: Long = 0L,
    val upgrades: PermanentUpgrades = PermanentUpgrades(),
    val bestDepth: Int = 0
)

data class RunSummary(
    val depthReached: Int,
    val kills: Int,
    val banked: Boolean,
    val xpEarned: Long
)

data class RogueCavernsState(
    val metaProfile: MetaProfile,
    val currentLevel: Level?,
    val heroPos: GridPos,
    val hero: Hero,
    val depth: Int,
    val kills: Int,
    val currentMonster: Monster?,
    val combatLog: List<String>,
    val runSummary: RunSummary?,
    val seed: Long
)
