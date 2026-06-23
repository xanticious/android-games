package com.xanticious.androidgames.model.games.treasuremapper

import com.xanticious.androidgames.model.games.mapcommon.GridCell
import com.xanticious.androidgames.model.games.mapcommon.GridDirection
import com.xanticious.androidgames.model.games.mapcommon.GridSize

enum class TreasureTile { CLEARING, BIG_TREE, SMALL_TREE, ROCK, WATER, FENCE_POST }

enum class ClueComplexity { SIMPLE, STANDARD, TRICKY }

enum class LandmarkDensity { SPARSE, NORMAL, DENSE }

data class TreasureMapperConfig(
    val gridSize: GridSize,
    val maxTries: Int,
    val clueComplexity: ClueComplexity,
    val landmarkDensity: LandmarkDensity,
    val showCompass: Boolean,
    val showStepGuide: Boolean
)

data class TreasureLandmark(
    val cell: GridCell,
    val tile: TreasureTile,
    val name: String
)

data class TreasureClueStep(val direction: GridDirection, val paces: Int)

data class TreasureClue(
    val landmark: TreasureLandmark,
    val steps: List<TreasureClueStep>,
    val text: String
)

data class TreasureWorld(
    val size: GridSize,
    val tiles: Map<GridCell, TreasureTile>,
    val landmarks: List<TreasureLandmark>,
    val clue: TreasureClue,
    val treasure: GridCell
) {
    fun tileAt(cell: GridCell): TreasureTile = tiles[cell] ?: TreasureTile.CLEARING
}

data class TreasureMapperRound(
    val world: TreasureWorld,
    val selected: GridCell?,
    val wrongDigs: Set<GridCell>,
    val triesRemaining: Int,
    val triesUsed: Int,
    val solved: Boolean,
    val revealed: Boolean,
    val result: TreasureDigResult?
) {
    companion object {
        fun initial(world: TreasureWorld, maxTries: Int): TreasureMapperRound = TreasureMapperRound(
            world = world,
            selected = null,
            wrongDigs = emptySet(),
            triesRemaining = maxTries,
            triesUsed = 0,
            solved = false,
            revealed = false,
            result = null
        )
    }
}

data class TreasureDigResult(
    val guess: GridCell,
    val correct: Boolean,
    val distance: Int,
    val triesUsed: Int,
    val score: Int
)
