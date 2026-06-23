package com.xanticious.androidgames.controller.games.nonogram

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.nonogram.CellState
import com.xanticious.androidgames.model.games.nonogram.NonogramState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import kotlin.random.Random

/**
 * Pure nonogram rules: puzzle generation, cell toggling, clue satisfaction checks,
 * undo and the solved check. No Android or Compose imports — fully JVM unit-testable.
 */
class NonogramController {

    /** Board sizes offered in settings, smallest (easiest) first. */
    val sizes: List<Int> = listOf(5, 10, 15, 20)

    /**
     * Generates a new [size]×[size] puzzle. Fill probability is tuned by [difficulty]
     * to control puzzle complexity; [random] is injectable for deterministic tests.
     */
    fun newGame(
        size: Int,
        difficulty: GameDifficulty = GameDifficulty.MEDIUM,
        random: Random = Random.Default
    ): NonogramState {
        val density = when (difficulty) {
            GameDifficulty.EASY -> 0.45f
            GameDifficulty.MEDIUM -> 0.55f
            GameDifficulty.HARD -> 0.65f
        }
        val solution = List(size * size) { random.nextFloat() < density }
        val rowClues = (0 until size).map { r ->
            lineClue((0 until size).map { c -> solution[r * size + c] })
        }
        val colClues = (0 until size).map { c ->
            lineClue((0 until size).map { r -> solution[r * size + c] })
        }
        return NonogramState(
            size = size,
            solution = solution,
            cells = List(size * size) { CellState.EMPTY },
            rowClues = rowClues,
            colClues = colClues
        )
    }

    /**
     * Returns the run-lengths of consecutive `true` entries in [filled].
     * An all-false line returns an empty list; callers display it as "0".
     *
     * Example: [true, true, false, true] → [2, 1]
     */
    fun lineClue(filled: List<Boolean>): List<Int> {
        val runs = mutableListOf<Int>()
        var count = 0
        for (f in filled) {
            if (f) {
                count++
            } else if (count > 0) {
                runs += count
                count = 0
            }
        }
        if (count > 0) runs += count
        return runs
    }

    /**
     * Cycles the cell at [pos]: EMPTY→FILLED, FILLED→EMPTY.
     * A MARKED cell is also cleared to EMPTY so a mis-placed ✕ can be corrected
     * with a tap instead of requiring a long-press.
     * Pushes the previous cells snapshot onto [NonogramState.history].
     */
    fun toggle(state: NonogramState, pos: GridPos): NonogramState {
        val index = pos.row * state.size + pos.col
        if (index !in state.cells.indices) return state
        val next = when (state.cells[index]) {
            CellState.FILLED -> CellState.EMPTY
            else -> CellState.FILLED
        }
        val newCells = state.cells.toMutableList().also { it[index] = next }
        return state.copy(cells = newCells, history = state.history + listOf(state.cells))
    }

    /**
     * Toggles the ✕ annotation at [pos]: MARKED→EMPTY, any other state→MARKED.
     * Pushes the previous cells snapshot onto [NonogramState.history].
     */
    fun toggleMark(state: NonogramState, pos: GridPos): NonogramState {
        val index = pos.row * state.size + pos.col
        if (index !in state.cells.indices) return state
        val next = if (state.cells[index] == CellState.MARKED) CellState.EMPTY else CellState.MARKED
        val newCells = state.cells.toMutableList().also { it[index] = next }
        return state.copy(cells = newCells, history = state.history + listOf(state.cells))
    }

    /**
     * Returns `true` when every cell's FILLED status exactly matches the
     * [NonogramState.solution]. ✕ marks are advisory and ignored.
     */
    fun isSolved(state: NonogramState): Boolean =
        state.cells.indices.all { i -> (state.cells[i] == CellState.FILLED) == state.solution[i] }

    /** Reverts the last action; no-ops when history is empty. */
    fun undo(state: NonogramState): NonogramState {
        val previous = state.history.lastOrNull() ?: return state
        return state.copy(cells = previous, history = state.history.dropLast(1))
    }

    /** Row clues from the puzzle solution (the [NonogramState.rowClues] field). */
    fun rowClues(state: NonogramState): List<List<Int>> = state.rowClues

    /** Column clues from the puzzle solution (the [NonogramState.colClues] field). */
    fun colClues(state: NonogramState): List<List<Int>> = state.colClues

    /**
     * Returns `true` when the player's FILLED marks in [row] produce a run-length
     * sequence that exactly matches the puzzle clue for that row.
     */
    fun isRowSatisfied(state: NonogramState, row: Int): Boolean {
        val n = state.size
        val filled = (0 until n).map { c -> state.cells[row * n + c] == CellState.FILLED }
        return lineClue(filled) == state.rowClues[row]
    }

    /**
     * Returns `true` when the player's FILLED marks in [col] produce a run-length
     * sequence that exactly matches the puzzle clue for that column.
     */
    fun isColSatisfied(state: NonogramState, col: Int): Boolean {
        val n = state.size
        val filled = (0 until n).map { r -> state.cells[r * n + col] == CellState.FILLED }
        return lineClue(filled) == state.colClues[col]
    }
}
