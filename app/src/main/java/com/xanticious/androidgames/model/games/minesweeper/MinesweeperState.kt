package com.xanticious.androidgames.model.games.minesweeper

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * Immutable Minesweeper board model (`design/puzzle-games/mine-sweeper`).
 *
 * The board is a [rows]×[cols] grid flattened row-major into [cells]. Mines are
 * placed lazily after the first tap (first-click safety). Each cell tracks its
 * cover state and, once mines are placed, its neighbor-mine count.
 */

/** Board dimensions and mine count for one game configuration. */
data class MinesweeperConfig(val rows: Int, val cols: Int, val mines: Int)

/** Available preset difficulties, matching the design doc. */
enum class MinesweeperDifficulty(val label: String, val config: MinesweeperConfig) {
    BEGINNER("Beginner", MinesweeperConfig(9, 9, 10)),
    INTERMEDIATE("Intermediate", MinesweeperConfig(16, 16, 40)),
    EXPERT("Expert", MinesweeperConfig(16, 30, 99))
}

/** Visibility/mark state of one cell. */
enum class CellMark { COVERED, REVEALED, FLAGGED }

/**
 * One cell on the Minesweeper grid.
 *
 * [hasMine] and [neighborCount] are meaningless (both zero) until
 * [MinesweeperState.minesPlaced] is true.
 */
data class MinesweeperCell(
    val mark: CellMark = CellMark.COVERED,
    val hasMine: Boolean = false,
    val neighborCount: Int = 0
)

/**
 * Full immutable snapshot of a Minesweeper game.
 *
 * [minesPlaced] is false on a freshly dealt board; the controller's
 * `placeMines` transitions it to true and populates [neighborCount]s.
 * [revealedCount] tracks safe-cell reveals for the solved check.
 * [explodedPos] is non-null once a mine cell has been revealed (game lost).
 */
data class MinesweeperState(
    val config: MinesweeperConfig,
    val cells: List<MinesweeperCell>,
    val minesPlaced: Boolean = false,
    val flagCount: Int = 0,
    val revealedCount: Int = 0,
    val explodedPos: GridPos? = null
) {
    /** Total non-mine cells; revealing all of these wins the game. */
    val totalSafeCells: Int get() = config.rows * config.cols - config.mines

    /** Returns the cell at [row],[col] (row-major index). */
    fun cellAt(row: Int, col: Int): MinesweeperCell = cells[row * config.cols + col]

    /** Returns the cell at [pos]. */
    fun cellAt(pos: GridPos): MinesweeperCell = cellAt(pos.row, pos.col)

    companion object {
        /** Deals a blank board with every cell covered and no mines placed yet. */
        fun empty(config: MinesweeperConfig): MinesweeperState =
            MinesweeperState(
                config = config,
                cells = List(config.rows * config.cols) { MinesweeperCell() }
            )
    }
}
