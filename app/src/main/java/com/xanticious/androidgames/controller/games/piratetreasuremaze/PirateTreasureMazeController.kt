package com.xanticious.androidgames.controller.games.piratetreasuremaze

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mapcommon.GridCell
import com.xanticious.androidgames.model.games.mapcommon.GridDirection
import com.xanticious.androidgames.model.games.mapcommon.GridSize
import com.xanticious.androidgames.model.games.piratetreasuremaze.MazeAlgorithm
import com.xanticious.androidgames.model.games.piratetreasuremaze.MazeAlgorithmSelection
import com.xanticious.androidgames.model.games.piratetreasuremaze.MazeCellWalls
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateControlStyle
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateMaze
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateMazeRun
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateMoveResult
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateTreasureMazeConfig
import kotlin.math.roundToInt
import kotlin.random.Random

class PirateTreasureMazeController {
    fun configFor(difficulty: GameDifficulty): PirateTreasureMazeConfig = when (difficulty) {
        GameDifficulty.EASY -> PirateTreasureMazeConfig(GridSize(10, 10), 3, PirateControlStyle.DPAD, false, MazeAlgorithmSelection.RANDOM, AVG_MOVE_SECONDS)
        GameDifficulty.MEDIUM -> PirateTreasureMazeConfig(GridSize(16, 16), 5, PirateControlStyle.DPAD, false, MazeAlgorithmSelection.RANDOM, AVG_MOVE_SECONDS)
        GameDifficulty.HARD -> PirateTreasureMazeConfig(GridSize(24, 24), 8, PirateControlStyle.DPAD, false, MazeAlgorithmSelection.RANDOM, AVG_MOVE_SECONDS)
    }

    fun generateMaze(config: PirateTreasureMazeConfig, random: Random = Random.Default): PirateMaze {
        val algorithm = when (config.algorithmSelection) {
            MazeAlgorithmSelection.RANDOM -> if (random.nextBoolean()) MazeAlgorithm.BACKTRACKER else MazeAlgorithm.PRIM
            MazeAlgorithmSelection.BACKTRACKER -> MazeAlgorithm.BACKTRACKER
            MazeAlgorithmSelection.PRIM -> MazeAlgorithm.PRIM
        }
        val walls = when (algorithm) {
            MazeAlgorithm.BACKTRACKER -> generateBacktracker(config.size, random)
            MazeAlgorithm.PRIM -> generatePrim(config.size, random)
        }
        val start = GridCell(0, 0)
        val exit = GridCell(config.size.columns - 1, config.size.rows - 1)
        val chests = placeChests(config.size, walls, start, exit, config.chestCount)
        val skeleton = PirateMaze(config.size, walls, start, exit, chests, algorithm, 0)
        return skeleton.copy(optimalMoves = computeOptimalMoves(skeleton))
    }

    fun tick(run: PirateMazeRun, dtSeconds: Float): PirateMazeRun =
        if (run.completed) run else run.copy(elapsedSeconds = run.elapsedSeconds + dtSeconds.coerceAtLeast(0f))

    fun move(run: PirateMazeRun, direction: GridDirection): PirateMoveResult {
        if (run.completed) return PirateMoveResult(run, moved = false, openedChest = false, completed = true)
        if (!run.maze.wallsAt(run.player).isOpen(direction)) return PirateMoveResult(run, moved = false, openedChest = false, completed = false)
        val next = run.player.move(direction)
        if (!run.maze.size.contains(next)) return PirateMoveResult(run, moved = false, openedChest = false, completed = false)
        if (next == run.maze.exit && run.openedChests.size < run.maze.chests.size) {
            return PirateMoveResult(run, moved = false, openedChest = false, completed = false)
        }
        val opened = next in run.maze.chests && next !in run.openedChests
        val openedChests = if (opened) run.openedChests + next else run.openedChests
        val completed = next == run.maze.exit && openedChests.size == run.maze.chests.size
        val moved = run.copy(
            player = next,
            openedChests = openedChests,
            completed = completed,
            moves = run.moves + 1,
            score = if (completed) scoreRun(run.elapsedSeconds, run.maze.optimalMoves, run.maze.size) else run.score
        )
        return PirateMoveResult(moved, moved = true, openedChest = opened, completed = completed)
    }

    fun scoreRun(elapsedSeconds: Float, optimalMoves: Int, size: GridSize): Int {
        if (optimalMoves <= 0) return 0
        val optimalTime = optimalMoves * AVG_MOVE_SECONDS
        val actual = elapsedSeconds.coerceAtLeast(optimalTime)
        return ((optimalTime / actual) * 100f).roundToInt().coerceIn(0, 100)
    }

    fun computeOptimalMoves(maze: PirateMaze): Int {
        val chestList = maze.chests.toList()
        val allMask = (1 shl chestList.size) - 1
        data class Node(val cell: GridCell, val mask: Int)
        val startChestIndex = chestList.indexOf(maze.start)
        val startMask = if (startChestIndex >= 0) 1 shl startChestIndex else 0
        val start = Node(maze.start, startMask)
        val queue = ArrayDeque<Node>()
        val dist = mutableMapOf(start to 0)
        queue.add(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val d = dist[node] ?: 0
            if (node.cell == maze.exit && node.mask == allMask) return d
            openNeighbors(maze.walls, maze.size, node.cell).forEach { next ->
                val chestIndex = chestList.indexOf(next)
                val nextMask = if (chestIndex >= 0) node.mask or (1 shl chestIndex) else node.mask
                if (next == maze.exit && nextMask != allMask) return@forEach
                val nextNode = Node(next, nextMask)
                if (nextNode !in dist) {
                    dist[nextNode] = d + 1
                    queue.add(nextNode)
                }
            }
        }
        return 0
    }

    fun openNeighbors(walls: Map<GridCell, MazeCellWalls>, size: GridSize, cell: GridCell): List<GridCell> =
        GridDirection.entries.mapNotNull { direction ->
            val next = cell.move(direction)
            if (size.contains(next) && (walls[cell] ?: MazeCellWalls()).isOpen(direction)) next else null
        }

    private fun generateBacktracker(size: GridSize, random: Random): Map<GridCell, MazeCellWalls> {
        val walls = initialWalls(size)
        val visited = mutableSetOf<GridCell>()
        val stack = mutableListOf(GridCell(0, 0))
        visited += stack.first()
        while (stack.isNotEmpty()) {
            val current = stack.last()
            val candidates = size.cardinalNeighbors(current).filter { it !in visited }
            if (candidates.isEmpty()) {
                stack.removeAt(stack.lastIndex)
            } else {
                val next = candidates[random.nextInt(candidates.size)]
                openBetween(walls, current, next)
                visited += next
                stack += next
            }
        }
        return walls.toMap()
    }

    private fun generatePrim(size: GridSize, random: Random): Map<GridCell, MazeCellWalls> {
        val walls = initialWalls(size)
        val visited = mutableSetOf(GridCell(0, 0))
        val frontier = mutableListOf<Pair<GridCell, GridCell>>()
        fun addFrontier(cell: GridCell) {
            size.cardinalNeighbors(cell).filter { it !in visited }.forEach { frontier += cell to it }
        }
        addFrontier(GridCell(0, 0))
        while (frontier.isNotEmpty()) {
            val index = random.nextInt(frontier.size)
            val (from, to) = frontier.removeAt(index)
            if (to !in visited) {
                openBetween(walls, from, to)
                visited += to
                addFrontier(to)
            }
        }
        return walls.toMap()
    }

    private fun initialWalls(size: GridSize): MutableMap<GridCell, MazeCellWalls> =
        size.cells.associateWith { MazeCellWalls() }.toMutableMap()

    private fun openBetween(walls: MutableMap<GridCell, MazeCellWalls>, a: GridCell, b: GridCell) {
        val direction = GridDirection.entries.first { a.move(it) == b }
        val opposite = when (direction) {
            GridDirection.NORTH -> GridDirection.SOUTH
            GridDirection.SOUTH -> GridDirection.NORTH
            GridDirection.EAST -> GridDirection.WEST
            GridDirection.WEST -> GridDirection.EAST
        }
        walls[a] = (walls[a] ?: MazeCellWalls()).open(direction)
        walls[b] = (walls[b] ?: MazeCellWalls()).open(opposite)
    }

    private fun placeChests(
        size: GridSize,
        walls: Map<GridCell, MazeCellWalls>,
        start: GridCell,
        exit: GridCell,
        count: Int
    ): Set<GridCell> {
        val distances = distancesFrom(walls, size, start)
        return distances.entries.asSequence()
            .filter { (cell, _) -> cell != start && cell != exit }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(count)
            .toSet()
    }

    private fun distancesFrom(walls: Map<GridCell, MazeCellWalls>, size: GridSize, start: GridCell): Map<GridCell, Int> {
        val queue = ArrayDeque<GridCell>()
        val dist = mutableMapOf(start to 0)
        queue.add(start)
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val d = dist[cell] ?: 0
            openNeighbors(walls, size, cell).forEach { next ->
                if (next !in dist) {
                    dist[next] = d + 1
                    queue.add(next)
                }
            }
        }
        return dist
    }

    private companion object {
        const val AVG_MOVE_SECONDS = 0.35f
    }
}
