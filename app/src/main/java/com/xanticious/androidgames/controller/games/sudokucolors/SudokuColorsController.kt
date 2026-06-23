package com.xanticious.androidgames.controller.games.sudokucolors

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.sudokucolors.SudokuColorsCellData
import com.xanticious.androidgames.model.games.sudokucolors.SudokuColorsState
import kotlin.random.Random

/**
 * Pure Sudoku Colors rules: grid generation, placement, pencil marks, conflict
 * detection, undo and the solved check.
 *
 * No Android or Compose imports — the entire rule set is JVM unit-testable.
 *
 * Generation pipeline:
 *   1. [generateSolvedGrid] — backtracking with randomised candidate order,
 *      produces a full valid 9×9 grid.
 *   2. [removeClues] — removes cells one-by-one in a random order; each removal
 *      is kept only when a [countSolutions] call (with MRV heuristic, stopping at
 *      limit 2) confirms the puzzle remains uniquely solvable.
 */
class SudokuColorsController {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Deals a fresh, uniquely-solvable puzzle for [difficulty] using [random] for
     * reproducibility.  Passing the same seed produces the same puzzle.
     */
    fun newGame(
        difficulty: GameDifficulty,
        random: Random = Random.Default
    ): SudokuColorsState {
        val solution = generateSolvedGrid(random)
        val targetGivens = when (difficulty) {
            GameDifficulty.EASY -> 45
            GameDifficulty.MEDIUM -> 35
            GameDifficulty.HARD -> 27
        }
        val givenGrid = removeClues(solution, targetGivens, random)
        val cells = givenGrid.mapIndexed { i, v ->
            SudokuColorsCellData(value = v, isGiven = v != 0)
        }
        return SudokuColorsState(cells = cells, solution = solution.toList())
    }

    /**
     * Toggles selection of [pos].  Tapping the already-selected cell deselects it.
     */
    fun selectCell(state: SudokuColorsState, pos: GridPos): SudokuColorsState =
        state.copy(selectedPos = if (state.selectedPos == pos) null else pos)

    /**
     * Places [colorIndex] (1–9) in the cell at [pos], or clears it if the cell
     * already holds that color (tap-to-clear).  No-ops on given cells.
     * Automatically recomputes conflict flags for the entire board.
     */
    fun setValue(
        state: SudokuColorsState,
        pos: GridPos,
        colorIndex: Int
    ): SudokuColorsState {
        val idx = indexOf(pos)
        if (idx !in state.cells.indices) return state
        val cell = state.cells[idx]
        if (cell.isGiven) return state

        val newValue = if (cell.value == colorIndex) 0 else colorIndex
        val newCells = state.cells.toMutableList()
        newCells[idx] = cell.copy(value = newValue, pencilMarks = emptySet())
        val withConflicts = detectConflicts(newCells)
        return state.copy(
            cells = withConflicts,
            history = state.history + listOf(state.cells),
            moves = state.moves + 1
        )
    }

    /**
     * Adds or removes [colorIndex] (1–9) from the pencil marks of the cell at
     * [pos].  No-ops on given cells or cells that already have a placed value.
     */
    fun togglePencilMark(
        state: SudokuColorsState,
        pos: GridPos,
        colorIndex: Int
    ): SudokuColorsState {
        val idx = indexOf(pos)
        if (idx !in state.cells.indices) return state
        val cell = state.cells[idx]
        if (cell.isGiven || cell.value != 0) return state

        val newMarks =
            if (colorIndex in cell.pencilMarks) cell.pencilMarks - colorIndex
            else cell.pencilMarks + colorIndex
        val newCells = state.cells.toMutableList()
        newCells[idx] = cell.copy(pencilMarks = newMarks)
        return state.copy(
            cells = newCells,
            history = state.history + listOf(state.cells),
            moves = state.moves + 1
        )
    }

    /** Reverts the most recent mutating action, if any. */
    fun undo(state: SudokuColorsState): SudokuColorsState {
        val previous = state.history.lastOrNull() ?: return state
        return state.copy(cells = previous, history = state.history.dropLast(1))
    }

    /** Toggles pencil-mark input mode. */
    fun togglePencilMode(state: SudokuColorsState): SudokuColorsState =
        state.copy(pencilMode = !state.pencilMode)

    /** Toggles the digit-overlay accessibility option. */
    fun toggleShowDigits(state: SudokuColorsState): SudokuColorsState =
        state.copy(showDigits = !state.showDigits)

    /**
     * Returns true when every cell matches the unique solution — i.e. the puzzle
     * is completely and correctly filled.
     */
    fun isSolved(state: SudokuColorsState): Boolean =
        state.cells.indices.all { i -> state.cells[i].value == state.solution[i] }

    /**
     * Returns the set of [GridPos] cells that share a row, column, or 3×3 box
     * with [pos] (the "peers").  [pos] itself is excluded.
     */
    fun peersOf(pos: GridPos): Set<GridPos> {
        val result = mutableSetOf<GridPos>()
        for (c in 0 until 9) result.add(GridPos(pos.row, c))
        for (r in 0 until 9) result.add(GridPos(r, pos.col))
        val br = (pos.row / 3) * 3
        val bc = (pos.col / 3) * 3
        for (r in br until br + 3)
            for (c in bc until bc + 3)
                result.add(GridPos(r, c))
        result.remove(pos)
        return result
    }

    // ── Conflict detection ────────────────────────────────────────────────────

    /**
     * Returns a new cell list with [SudokuColorsCellData.isConflict] set for every
     * cell whose non-zero value appears more than once in its row, column, or box.
     */
    fun detectConflicts(cells: List<SudokuColorsCellData>): List<SudokuColorsCellData> {
        val conflicts = BooleanArray(81)

        for (row in 0 until 9) {
            val seen = mutableMapOf<Int, MutableList<Int>>()
            for (col in 0 until 9) {
                val idx = row * 9 + col
                val v = cells[idx].value
                if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(idx)
            }
            seen.values.filter { it.size > 1 }.flatten().forEach { conflicts[it] = true }
        }

        for (col in 0 until 9) {
            val seen = mutableMapOf<Int, MutableList<Int>>()
            for (row in 0 until 9) {
                val idx = row * 9 + col
                val v = cells[idx].value
                if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(idx)
            }
            seen.values.filter { it.size > 1 }.flatten().forEach { conflicts[it] = true }
        }

        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                val seen = mutableMapOf<Int, MutableList<Int>>()
                for (r in 0 until 3) {
                    for (c in 0 until 3) {
                        val idx = (boxRow * 3 + r) * 9 + (boxCol * 3 + c)
                        val v = cells[idx].value
                        if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(idx)
                    }
                }
                seen.values.filter { it.size > 1 }.flatten().forEach { conflicts[it] = true }
            }
        }

        return cells.mapIndexed { i, cell -> cell.copy(isConflict = conflicts[i]) }
    }

    // ── Grid generation ───────────────────────────────────────────────────────

    private fun generateSolvedGrid(random: Random): List<Int> {
        val grid = IntArray(81)
        check(fillGrid(grid, random)) { "Sudoku grid generation failed — should never happen" }
        return grid.toList()
    }

    /** Backtracking fill with randomised candidate order. */
    private fun fillGrid(grid: IntArray, random: Random): Boolean {
        val pos = grid.indexOfFirst { it == 0 }
        if (pos == -1) return true
        val row = pos / 9
        val col = pos % 9
        val used = computeUsed(grid, row, col)
        val candidates = (1..9).filter { !used[it] }.shuffled(random)
        for (v in candidates) {
            grid[pos] = v
            if (fillGrid(grid, random)) return true
            grid[pos] = 0
        }
        return false
    }

    /**
     * Removes cells from [solution] until [targetGivens] cells remain, ensuring
     * the puzzle stays uniquely solvable after each removal.  Returns the
     * resulting clue grid (0 = empty, 1–9 = given value).
     */
    private fun removeClues(
        solution: List<Int>,
        targetGivens: Int,
        random: Random
    ): List<Int> {
        val grid = solution.toIntArray()
        val indices = (0 until 81).shuffled(random)
        var givens = 81

        for (idx in indices) {
            if (givens <= targetGivens) break
            val saved = grid[idx]
            grid[idx] = 0
            val test = grid.copyOf()
            if (countSolutions(test, 2) == 1) {
                givens--
            } else {
                grid[idx] = saved
            }
        }
        return grid.toList()
    }

    /**
     * Counts the number of solutions for [grid], stopping once [limit] solutions
     * are found.  Uses the MRV (minimum remaining values) heuristic to pick the
     * next empty cell, which keeps the search tree small enough for fast
     * uniqueness checking.
     *
     * The grid is mutated in-place during search and restored on backtrack, so
     * callers must pass a copy when they need the original preserved.
     */
    private fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val pos = findBestEmpty(grid)
        if (pos == -1) return 1
        val row = pos / 9
        val col = pos % 9
        val used = computeUsed(grid, row, col)
        var count = 0
        for (v in 1..9) {
            if (!used[v]) {
                grid[pos] = v
                count += countSolutions(grid, limit)
                grid[pos] = 0
                if (count >= limit) return count
            }
        }
        return count
    }

    /** Picks the empty cell with the fewest legal candidates (MRV heuristic). */
    private fun findBestEmpty(grid: IntArray): Int {
        var bestPos = -1
        var bestCount = 10
        for (pos in grid.indices) {
            if (grid[pos] != 0) continue
            val row = pos / 9
            val col = pos % 9
            val used = computeUsed(grid, row, col)
            val count = (1..9).count { !used[it] }
            if (count < bestCount) {
                bestCount = count
                bestPos = pos
                if (count == 0) return pos
            }
        }
        return bestPos
    }

    /** Returns a 10-element boolean array where index v is true if v is already used. */
    private fun computeUsed(grid: IntArray, row: Int, col: Int): BooleanArray {
        val used = BooleanArray(10)
        for (c in 0 until 9) { val v = grid[row * 9 + c]; if (v > 0) used[v] = true }
        for (r in 0 until 9) { val v = grid[r * 9 + col]; if (v > 0) used[v] = true }
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3)
            for (c in bc until bc + 3) { val v = grid[r * 9 + c]; if (v > 0) used[v] = true }
        return used
    }

    private fun indexOf(pos: GridPos): Int = pos.row * 9 + pos.col
}
