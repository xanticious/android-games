package com.xanticious.androidgames.games.piratetreasuremaze

import com.xanticious.androidgames.controller.games.piratetreasuremaze.PirateTreasureMazeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mapcommon.GridCell
import com.xanticious.androidgames.model.games.mapcommon.GridDirection
import com.xanticious.androidgames.model.games.mapcommon.GridSize
import com.xanticious.androidgames.model.games.piratetreasuremaze.MazeAlgorithm
import com.xanticious.androidgames.model.games.piratetreasuremaze.MazeCellWalls
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateMaze
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateMazeRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PirateTreasureMazeControllerTest {
    private val controller = PirateTreasureMazeController()

    @Test
    fun generateMaze_mediumDifficulty_placesConfiguredChestCount() {
        val config = controller.configFor(GameDifficulty.MEDIUM)
        val maze = controller.generateMaze(config, Random(31))
        assertEquals(config.chestCount, maze.chests.size)
    }

    @Test
    fun generateMaze_seededBacktracker_reachesEveryCell() {
        val config = controller.configFor(GameDifficulty.EASY)
        val maze = controller.generateMaze(config, Random(37))
        assertEquals(config.size.cells.size, reachableCount(maze))
    }

    @Test
    fun generateMaze_generatedMaze_hasPositiveOptimalPath() {
        val maze = controller.generateMaze(controller.configFor(GameDifficulty.EASY), Random(41))
        assertTrue(maze.optimalMoves > 0)
    }

    @Test
    fun move_throughOpenPassage_updatesPlayerCell() {
        val maze = twoCellMaze(chests = emptySet())
        val result = controller.move(PirateMazeRun.initial(maze), GridDirection.EAST)
        assertEquals(GridCell(1, 0), result.run.player)
    }

    @Test
    fun move_intoClosedWall_keepsPlayerCell() {
        val maze = twoCellMaze(chests = emptySet())
        val result = controller.move(PirateMazeRun.initial(maze), GridDirection.SOUTH)
        assertEquals(GridCell(0, 0), result.run.player)
    }

    @Test
    fun move_exitWithChestRemaining_staysBeforeExit() {
        val maze = twoCellMaze(chests = setOf(GridCell(0, 0)))
        val result = controller.move(PirateMazeRun.initial(maze), GridDirection.EAST)
        assertEquals(GridCell(0, 0), result.run.player)
    }

    @Test
    fun scoreRun_optimalTime_returnsPerfectScore() {
        assertEquals(100, controller.scoreRun(3.5f, 10, GridSize(2, 1)))
    }

    private fun twoCellMaze(chests: Set<GridCell>): PirateMaze {
        val start = GridCell(0, 0)
        val exit = GridCell(1, 0)
        val walls = mapOf(
            start to MazeCellWalls(east = false),
            exit to MazeCellWalls(west = false)
        )
        val maze = PirateMaze(GridSize(2, 1), walls, start, exit, chests, MazeAlgorithm.BACKTRACKER, 0)
        return maze.copy(optimalMoves = controller.computeOptimalMoves(maze))
    }

    private fun reachableCount(maze: PirateMaze): Int {
        val queue = ArrayDeque<GridCell>()
        val seen = mutableSetOf(maze.start)
        queue.add(maze.start)
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            controller.openNeighbors(maze.walls, maze.size, cell).forEach { next ->
                if (seen.add(next)) queue.add(next)
            }
        }
        return seen.size
    }
}
