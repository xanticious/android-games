package com.xanticious.androidgames.controller.games.treasuremapper

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mapcommon.GridCell
import com.xanticious.androidgames.model.games.mapcommon.GridDirection
import com.xanticious.androidgames.model.games.mapcommon.GridSize
import com.xanticious.androidgames.model.games.treasuremapper.ClueComplexity
import com.xanticious.androidgames.model.games.treasuremapper.LandmarkDensity
import com.xanticious.androidgames.model.games.treasuremapper.TreasureClue
import com.xanticious.androidgames.model.games.treasuremapper.TreasureClueStep
import com.xanticious.androidgames.model.games.treasuremapper.TreasureDigResult
import com.xanticious.androidgames.model.games.treasuremapper.TreasureLandmark
import com.xanticious.androidgames.model.games.treasuremapper.TreasureMapperConfig
import com.xanticious.androidgames.model.games.treasuremapper.TreasureMapperRound
import com.xanticious.androidgames.model.games.treasuremapper.TreasureTile
import com.xanticious.androidgames.model.games.treasuremapper.TreasureWorld
import kotlin.math.abs
import kotlin.random.Random

class TreasureMapperController {
    fun configFor(difficulty: GameDifficulty): TreasureMapperConfig = when (difficulty) {
        GameDifficulty.EASY -> TreasureMapperConfig(GridSize(10, 10), 3, ClueComplexity.SIMPLE, LandmarkDensity.NORMAL, true, false)
        GameDifficulty.MEDIUM -> TreasureMapperConfig(GridSize(14, 14), 3, ClueComplexity.STANDARD, LandmarkDensity.NORMAL, true, false)
        GameDifficulty.HARD -> TreasureMapperConfig(GridSize(18, 18), 3, ClueComplexity.TRICKY, LandmarkDensity.DENSE, true, false)
    }

    fun generateWorld(config: TreasureMapperConfig, random: Random = Random.Default): TreasureWorld {
        val tiles = mutableMapOf<GridCell, TreasureTile>()
        config.gridSize.cells.forEach { cell -> tiles[cell] = generateTile(cell, config, random) }
        ensureUniqueLandmark(tiles, config.gridSize, random)
        val landmarks = buildLandmarks(tiles).filter { landmark -> tiles.values.count { it == landmark.tile } == 1 }
        val landmark = landmarks.firstOrNull() ?: buildLandmarks(tiles).first()
        val steps = generateSteps(config, landmark.cell, random)
        val treasure = resolvePath(landmark.cell, steps)
        val clue = TreasureClue(landmark, steps, buildClueText(landmark, steps))
        return TreasureWorld(config.gridSize, tiles.toMap(), buildLandmarks(tiles), clue, treasure)
    }

    fun resolvePath(start: GridCell, steps: List<TreasureClueStep>): GridCell =
        steps.fold(start) { cell, step -> cell.move(step.direction, step.paces) }

    fun selectCell(round: TreasureMapperRound, cell: GridCell): TreasureMapperRound =
        if (round.world.size.contains(cell) && !round.solved && !round.revealed) round.copy(selected = cell) else round

    fun deselectCell(round: TreasureMapperRound): TreasureMapperRound = round.copy(selected = null)

    fun submitDig(round: TreasureMapperRound): TreasureMapperRound {
        val guess = round.selected ?: return round
        if (round.solved || round.revealed) return round
        val distance = distance(guess, round.world.treasure)
        val correct = guess == round.world.treasure
        val triesUsed = round.triesUsed + 1
        val score = scoreDig(correct, distance, triesUsed, round.triesRemaining)
        val result = TreasureDigResult(guess, correct, distance, triesUsed, score)
        return if (correct) {
            round.copy(solved = true, selected = null, triesUsed = triesUsed, result = result)
        } else {
            val remaining = (round.triesRemaining - 1).coerceAtLeast(0)
            round.copy(
                selected = null,
                wrongDigs = round.wrongDigs + guess,
                triesRemaining = remaining,
                triesUsed = triesUsed,
                revealed = remaining == 0,
                result = result
            )
        }
    }

    fun scoreDig(correct: Boolean, distance: Int, triesUsed: Int, triesRemainingBeforeDig: Int): Int = when {
        correct -> (120 - (triesUsed - 1) * 30).coerceAtLeast(30)
        triesRemainingBeforeDig <= 1 -> 0
        else -> (20 - distance * 2).coerceAtLeast(1)
    }

    fun distance(a: GridCell, b: GridCell): Int = abs(a.x - b.x) + abs(a.y - b.y)

    private fun generateTile(cell: GridCell, config: TreasureMapperConfig, random: Random): TreasureTile {
        val roll = random.nextInt(100)
        val densityBoost = when (config.landmarkDensity) {
            LandmarkDensity.SPARSE -> -8
            LandmarkDensity.NORMAL -> 0
            LandmarkDensity.DENSE -> 10
        }
        val waterBand = (cell.x + cell.y + random.nextInt(4)) % 11 == 0
        return when {
            waterBand && roll < 22 -> TreasureTile.WATER
            roll < 4 + densityBoost / 3 -> TreasureTile.BIG_TREE
            roll < 14 + densityBoost -> TreasureTile.SMALL_TREE
            roll < 23 + densityBoost -> TreasureTile.ROCK
            roll < 28 + densityBoost -> TreasureTile.FENCE_POST
            else -> TreasureTile.CLEARING
        }
    }

    private fun ensureUniqueLandmark(tiles: MutableMap<GridCell, TreasureTile>, size: GridSize, random: Random) {
        val unique = TreasureTile.entries.any { tile -> tile != TreasureTile.CLEARING && tiles.values.count { it == tile } == 1 }
        if (unique) return
        val cell = GridCell(random.nextInt(size.columns), random.nextInt(size.rows))
        tiles[cell] = TreasureTile.BIG_TREE
        tiles.keys.filter { it != cell && tiles[it] == TreasureTile.BIG_TREE }.forEach { tiles[it] = TreasureTile.CLEARING }
    }

    private fun buildLandmarks(tiles: Map<GridCell, TreasureTile>): List<TreasureLandmark> = tiles
        .filterValues { it != TreasureTile.CLEARING && it != TreasureTile.WATER }
        .map { (cell, tile) -> TreasureLandmark(cell, tile, landmarkName(tile)) }
        .sortedWith(compareBy({ it.cell.y }, { it.cell.x }))

    private fun landmarkName(tile: TreasureTile): String = when (tile) {
        TreasureTile.BIG_TREE -> "the big tree"
        TreasureTile.SMALL_TREE -> "the small tree"
        TreasureTile.ROCK -> "the big rock"
        TreasureTile.FENCE_POST -> "the fence post"
        TreasureTile.WATER -> "the blue water"
        TreasureTile.CLEARING -> "the clearing"
    }

    private fun generateSteps(config: TreasureMapperConfig, start: GridCell, random: Random): List<TreasureClueStep> {
        val count = when (config.clueComplexity) {
            ClueComplexity.SIMPLE -> 1
            ClueComplexity.STANDARD -> 2
            ClueComplexity.TRICKY -> 3
        }
        repeat(200) {
            val steps = mutableListOf<TreasureClueStep>()
            var current = start
            repeat(count) { index ->
                val directions = GridDirection.entries.filter { direction ->
                    index == 0 || direction != steps.last().direction
                }.shuffled(random)
                val direction = directions.first { dir -> maxPaces(config.gridSize, current, dir) > 0 }
                val max = maxPaces(config.gridSize, current, direction)
                val paces = 1 + random.nextInt(max)
                steps += TreasureClueStep(direction, paces)
                current = current.move(direction, paces)
            }
            if (config.gridSize.contains(current) && current != start) return steps
        }
        return listOf(TreasureClueStep(GridDirection.EAST, 1)).filter { config.gridSize.contains(start.move(it.direction, it.paces)) }
    }

    private fun maxPaces(size: GridSize, cell: GridCell, direction: GridDirection): Int = when (direction) {
        GridDirection.NORTH -> cell.y
        GridDirection.SOUTH -> size.rows - cell.y - 1
        GridDirection.EAST -> size.columns - cell.x - 1
        GridDirection.WEST -> cell.x
    }

    private fun buildClueText(landmark: TreasureLandmark, steps: List<TreasureClueStep>): String {
        val first = steps.first()
        val tail = steps.drop(1)
        val start = "Starting at ${landmark.name}, walk ${first.paces} paces ${first.direction.label}"
        return when (tail.size) {
            0 -> "$start. Dig here."
            1 -> "$start, then ${tail[0].paces} paces ${tail[0].direction.label}. Dig here."
            else -> "$start, ${tail[0].paces} paces ${tail[0].direction.label}, then ${tail[1].paces} paces ${tail[1].direction.label}. Dig here."
        }
    }
}
