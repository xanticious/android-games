package com.xanticious.androidgames.model.games.empireskirmish

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos

enum class UnitType { SWORDSMAN, BOWMAN, MAGE, KING }
enum class Side { PLAYER, ENEMY }

data class Tile(val pos: GridPos, val blocking: Boolean, val cover: Boolean)

data class SkirmishUnit(
    val id: Int,
    val type: UnitType,
    val side: Side,
    val pos: GridPos,
    val hp: Int,
    val maxHp: Int,
    val hasMoved: Boolean = false,
    val hasAttacked: Boolean = false
)

data class UnitStats(
    val moveRange: Int,
    val attackRange: Int,
    val damage: Int,
    val maxHp: Int
)

data class Battle(
    val grid: List<Tile>,
    val cols: Int,
    val rows: Int,
    val units: List<SkirmishUnit>,
    val seed: Long
)

data class EmpireSkirmishState(
    val battle: Battle,
    val isPlayerTurn: Boolean,
    val selectedUnitId: Int?,
    val reachableTiles: Set<GridPos>,
    val attackablePositions: Set<GridPos>,
    val pendingMove: GridPos?,
    val turnNumber: Int,
    val winner: Side?
)
