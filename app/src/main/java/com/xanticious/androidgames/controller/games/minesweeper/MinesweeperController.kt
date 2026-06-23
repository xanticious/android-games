package com.xanticious.androidgames.controller.games.minesweeper

import com.xanticious.androidgames.model.games.minesweeper.CellMark
import com.xanticious.androidgames.model.games.minesweeper.MinesweeperCell
import com.xanticious.androidgames.model.games.minesweeper.MinesweeperConfig
import com.xanticious.androidgames.model.games.minesweeper.MinesweeperState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import kotlin.random.Random

/**
 * Pure Minesweeper rules: board generation, reveal (flood-fill), flag toggle, and
 * win/loss checks. No Android or Compose imports — fully JVM unit-testable.
 *
 * Usage pattern (matches `design/puzzle-games/mine-sweeper`):
 * 1. `newGame(rows, cols, mines)` → blank covered board.
 * 2. On the first user tap: `placeMines(state, firstPos, random)` → mines placed,
 *    neighbor counts computed, first tap always safe.
 * 3. `reveal(state, pos)` on every tap → flood-fill for zero cells.
 * 4. Check `hitMine` / `isSolved` after each reveal to drive the phase machine.
 */
class MinesweeperController {

    // ── Board construction ────────────────────────────────────────────────────

    /** Returns a blank, fully-covered board with no mines placed yet. */
    fun newGame(rows: Int, cols: Int, mines: Int): MinesweeperState =
        MinesweeperState.empty(MinesweeperConfig(rows, cols, mines))

    /**
     * Places [config.mines] mines at random, guaranteeing [safe] and all its
     * 8-neighbors are mine-free (first-click safety). Falls back to excluding
     * only [safe] itself when the board is too small for a full safe zone.
     * Computes and stores neighbor counts for every non-mine cell.
     *
     * @param state A board where [MinesweeperState.minesPlaced] is false.
     * @param safe  The cell the player tapped first.
     * @param random Seedable for deterministic tests.
     */
    fun placeMines(state: MinesweeperState, safe: GridPos, random: Random = Random.Default): MinesweeperState {
        val config = state.config
        val rows = config.rows
        val cols = config.cols
        val safeSet = safeZone(safe, rows, cols)

        val candidates = (0 until rows * cols)
            .filter { it !in safeSet }
            .toMutableList()

        // Fall back to excluding only the first cell if safe zone is too large.
        val pool = if (candidates.size >= config.mines) candidates else {
            val safeIdx = safe.row * cols + safe.col
            (0 until rows * cols).filter { it != safeIdx }.toMutableList()
        }

        // Partial Fisher-Yates to pick mine positions.
        val mineIndices = mutableSetOf<Int>()
        repeat(config.mines) { i ->
            val j = i + random.nextInt(pool.size - i)
            val tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp
            mineIndices += pool[i]
        }

        val cellsWithMines = state.cells.mapIndexed { idx, cell ->
            cell.copy(hasMine = idx in mineIndices)
        }

        val cellsWithCounts = cellsWithMines.mapIndexed { idx, cell ->
            if (cell.hasMine) cell
            else {
                val r = idx / cols
                val c = idx % cols
                val count = neighbors8(r, c, rows, cols)
                    .count { (nr, nc) -> cellsWithMines[nr * cols + nc].hasMine }
                cell.copy(neighborCount = count)
            }
        }

        return state.copy(cells = cellsWithCounts, minesPlaced = true)
    }

    // ── Gameplay moves ────────────────────────────────────────────────────────

    /**
     * Reveals [pos]. If the cell has neighborCount == 0, flood-reveals all
     * reachable zero-and-bordering cells (8-directional BFS). Reveals a mine
     * cell without cascading; the caller should check [hitMine] afterwards.
     *
     * Returns the state unchanged if [pos] is not COVERED.
     */
    fun reveal(state: MinesweeperState, pos: GridPos): MinesweeperState {
        val config = state.config
        val index = pos.row * config.cols + pos.col
        if (state.cells[index].mark != CellMark.COVERED) return state

        val cells = state.cells.toMutableList()
        var revealedCount = state.revealedCount
        var explodedPos = state.explodedPos

        val queue = ArrayDeque<GridPos>()
        queue.add(pos)

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val idx = cur.row * config.cols + cur.col
            if (cells[idx].mark != CellMark.COVERED) continue

            cells[idx] = cells[idx].copy(mark = CellMark.REVEALED)

            if (cells[idx].hasMine) {
                // Mine hit — record position and stop cascading.
                explodedPos = cur
                break
            }

            revealedCount++

            if (cells[idx].neighborCount == 0) {
                neighbors8(cur.row, cur.col, config.rows, config.cols).forEach { (nr, nc) ->
                    if (cells[nr * config.cols + nc].mark == CellMark.COVERED) {
                        queue.add(GridPos(nr, nc))
                    }
                }
            }
        }

        return state.copy(cells = cells, revealedCount = revealedCount, explodedPos = explodedPos)
    }

    /**
     * Cycles COVERED → FLAGGED → COVERED for [pos]. No-op if the cell is
     * already REVEALED.
     */
    fun toggleFlag(state: MinesweeperState, pos: GridPos): MinesweeperState {
        val index = pos.row * state.config.cols + pos.col
        val cell = state.cells[index]
        return when (cell.mark) {
            CellMark.COVERED -> {
                val newCells = state.cells.toMutableList()
                    .also { it[index] = cell.copy(mark = CellMark.FLAGGED) }
                state.copy(cells = newCells, flagCount = state.flagCount + 1)
            }
            CellMark.FLAGGED -> {
                val newCells = state.cells.toMutableList()
                    .also { it[index] = cell.copy(mark = CellMark.COVERED) }
                state.copy(cells = newCells, flagCount = state.flagCount - 1)
            }
            CellMark.REVEALED -> state
        }
    }

    // ── Win / loss checks ─────────────────────────────────────────────────────

    /** True when all safe cells have been revealed (win condition). */
    fun isSolved(state: MinesweeperState): Boolean =
        state.revealedCount == state.totalSafeCells

    /** True when a mine has been revealed (loss condition). */
    fun hitMine(state: MinesweeperState): Boolean = state.explodedPos != null

    /** Neighbor mine count for a cell — useful for unit testing adjacency logic. */
    fun neighborMineCount(state: MinesweeperState, pos: GridPos): Int =
        state.cellAt(pos).neighborCount

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun safeZone(safe: GridPos, rows: Int, cols: Int): Set<Int> {
        val set = mutableSetOf<Int>()
        for (dr in -1..1) for (dc in -1..1) {
            val r = safe.row + dr
            val c = safe.col + dc
            if (r in 0 until rows && c in 0 until cols) set += r * cols + c
        }
        return set
    }

    internal fun neighbors8(row: Int, col: Int, rows: Int, cols: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = row + dr
            val nc = col + dc
            if (nr in 0 until rows && nc in 0 until cols) result += nr to nc
        }
        return result
    }
}
