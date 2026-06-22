package com.xanticious.androidgames.model.games.piratetreasuremaze

import com.xanticious.androidgames.model.games.mapcommon.GridCell
import com.xanticious.androidgames.model.games.mapcommon.GridDirection
import com.xanticious.androidgames.model.games.mapcommon.GridSize

enum class MazeAlgorithm(val tag: String) { BACKTRACKER("Winding"), PRIM("Organic") }

enum class MazeAlgorithmSelection { RANDOM, BACKTRACKER, PRIM }

enum class PirateControlStyle { DPAD, SWIPE }

data class PirateTreasureMazeConfig(
    val size: GridSize,
    val chestCount: Int,
    val controlStyle: PirateControlStyle,
    val showMinimap: Boolean,
    val algorithmSelection: MazeAlgorithmSelection,
    val averageMoveSeconds: Float
)

data class MazeCellWalls(
    val north: Boolean = true,
    val south: Boolean = true,
    val east: Boolean = true,
    val west: Boolean = true
) {
    fun isOpen(direction: GridDirection): Boolean = when (direction) {
        GridDirection.NORTH -> !north
        GridDirection.SOUTH -> !south
        GridDirection.EAST -> !east
        GridDirection.WEST -> !west
    }

    fun open(direction: GridDirection): MazeCellWalls = when (direction) {
        GridDirection.NORTH -> copy(north = false)
        GridDirection.SOUTH -> copy(south = false)
        GridDirection.EAST -> copy(east = false)
        GridDirection.WEST -> copy(west = false)
    }
}

data class PirateMaze(
    val size: GridSize,
    val walls: Map<GridCell, MazeCellWalls>,
    val start: GridCell,
    val exit: GridCell,
    val chests: Set<GridCell>,
    val algorithm: MazeAlgorithm,
    val optimalMoves: Int
) {
    fun wallsAt(cell: GridCell): MazeCellWalls = walls[cell] ?: MazeCellWalls()
}

data class PirateMazeRun(
    val maze: PirateMaze,
    val player: GridCell,
    val openedChests: Set<GridCell>,
    val elapsedSeconds: Float,
    val completed: Boolean,
    val moves: Int,
    val score: Int
) {
    companion object {
        fun initial(maze: PirateMaze): PirateMazeRun = PirateMazeRun(
            maze = maze,
            player = maze.start,
            openedChests = emptySet(),
            elapsedSeconds = 0f,
            completed = false,
            moves = 0,
            score = 0
        )
    }
}

data class PirateMoveResult(val run: PirateMazeRun, val moved: Boolean, val openedChest: Boolean, val completed: Boolean)
