package com.xanticious.androidgames.model.games.chinesecheckers

import com.xanticious.androidgames.model.GameDifficulty
import kotlin.math.abs

/** Two-seat Chinese Checkers setup: human south, AI north. */
enum class ChineseCheckersSide { PLAYER, AI }

data class ChineseCheckersCoordinate(val x: Int, val y: Int, val z: Int) {
    init { require(x + y + z == 0) { "Cube coordinates must sum to zero." } }
}

data class ChineseCheckersHole(
    val coordinate: ChineseCheckersCoordinate,
    val displayX: Float,
    val displayY: Float,
    val homeFor: ChineseCheckersSide?,
    val targetFor: ChineseCheckersSide?
)

data class ChineseCheckersMove(
    val from: ChineseCheckersCoordinate,
    val to: ChineseCheckersCoordinate,
    val path: List<ChineseCheckersCoordinate>
) {
    init {
        require(path.size >= 2) { "A move needs a start and destination." }
        require(path.first() == from) { "Path must start at from." }
        require(path.last() == to) { "Path must end at to." }
    }

    val isHop: Boolean get() = path.size > 2 || cubeDistance(from, to) == 2
    val hopCount: Int get() = if (isHop) path.size - 1 else 0

    private fun cubeDistance(a: ChineseCheckersCoordinate, b: ChineseCheckersCoordinate): Int =
        (abs(a.x - b.x) + abs(a.y - b.y) + abs(a.z - b.z)) / 2
}

data class ChineseCheckersConfig(
    val difficulty: GameDifficulty,
    val playerSide: ChineseCheckersSide = ChineseCheckersSide.PLAYER,
    val aiSide: ChineseCheckersSide = ChineseCheckersSide.AI,
    val showHopChains: Boolean = true,
    val trainingWheels: Boolean = false
)

data class ChineseCheckersResult(
    val winner: ChineseCheckersSide,
    val loser: ChineseCheckersSide,
    val turnsPlayed: Int
)

data class ChineseCheckersState(
    val holes: List<ChineseCheckersHole>,
    val connections: List<Pair<ChineseCheckersCoordinate, ChineseCheckersCoordinate>>,
    val occupancy: Map<ChineseCheckersCoordinate, ChineseCheckersSide>,
    val homeRegions: Map<ChineseCheckersSide, Set<ChineseCheckersCoordinate>>,
    val targetRegions: Map<ChineseCheckersSide, Set<ChineseCheckersCoordinate>>,
    val currentPlayer: ChineseCheckersSide,
    val selectedPeg: ChineseCheckersCoordinate?,
    val config: ChineseCheckersConfig,
    val result: ChineseCheckersResult?,
    val turnsPlayed: Int = 0,
    val message: String = "Select a marble."
) {
    val holeCoordinates: Set<ChineseCheckersCoordinate> get() = holes.map { it.coordinate }.toSet()

    companion object {
        const val HOME_SIZE = 10
    }
}
