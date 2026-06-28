package com.xanticious.androidgames.controller.games.skyscrapers

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.skyscrapers.SkyscrapersClues
import com.xanticious.androidgames.model.games.skyscrapers.SkyscrapersSnapshot
import com.xanticious.androidgames.model.games.skyscrapers.SkyscrapersState
import kotlin.random.Random

/** Per-clue evaluation result used by the view to colour the edge strips. */
enum class ClueState { PENDING, SATISFIED, VIOLATED }

/** All four sides' clue states, parallel to [SkyscrapersClues]. */
data class ClueStatuses(
    val top: List<ClueState>,
    val bottom: List<ClueState>,
    val left: List<ClueState>,
    val right: List<ClueState>
)

/**
 * Pure Skyscrapers rules: Latin-square generation, clue derivation, height
 * entry, pencil marks, conflict detection, and solve check.
 *
 * No Android or Compose imports — the whole rule set is JVM unit-testable.
 * A [Random] is accepted by [newGame] so tests can pass a seeded instance for
 * deterministic results (`design/puzzle-games/skyscrapers`).
 */
class SkyscrapersController {

    /** Playable grid sizes offered in Settings, ascending. */
    val sizes: List<Int> = listOf(4, 5, 6, 7)

    // -------------------------------------------------------------------------
    // Core rule: visibility
    // -------------------------------------------------------------------------

    /**
     * Returns how many buildings are visible looking from the start of [line]
     * toward the end. A building is visible if it is strictly taller than every
     * building before it.
     */
    fun visibleCount(line: List<Int>): Int {
        var count = 0
        var maxHeight = 0
        for (h in line) {
            if (h > maxHeight) {
                count++
                maxHeight = h
            }
        }
        return count
    }

    // -------------------------------------------------------------------------
    // Game generation
    // -------------------------------------------------------------------------

    /**
     * Deals a fresh Skyscrapers puzzle of the given [size].
     *
     * 1. Builds a random Latin square via cyclic base + row/column/value shuffles.
     * 2. Derives all four edge clue arrays from the solution.
     * 3. Pre-fills a difficulty-scaled number of givens so even beginners have
     *    footholds.
     */
    fun newGame(
        size: Int,
        difficulty: GameDifficulty,
        random: Random = Random.Default
    ): SkyscrapersState {
        val solution = generateLatinSquare(size, random)
        val clues = deriveClues(solution, size)
        val givenCount = when (difficulty) {
            GameDifficulty.EASY -> size + 2
            GameDifficulty.MEDIUM -> (size / 2) + 1
            GameDifficulty.HARD -> 0
        }.coerceAtLeast(0)
        val allIndices = (0 until size * size).toMutableList().also { it.shuffle(random) }
        val givens = allIndices.take(givenCount).toSet()
        val grid = List(size * size) { idx -> if (idx in givens) solution[idx] else 0 }
        return SkyscrapersState(
            size = size,
            solution = solution,
            clues = clues,
            grid = grid,
            givens = givens
        )
    }

    // -------------------------------------------------------------------------
    // Cell interaction
    // -------------------------------------------------------------------------

    /** Selects [pos]; deselects if it is already selected. */
    fun selectCell(state: SkyscrapersState, pos: GridPos): SkyscrapersState {
        val idx = state.indexOf(pos)
        val newSel = if (state.selectedIndex == idx) -1 else idx
        return state.copy(selectedIndex = newSel)
    }

    /**
     * Sets the player-entered [height] (1..size) at [pos], or clears the cell
     * when [height] is 0. Given cells are never modified. Clears pencil marks
     * for the affected cell in all cases to keep the state consistent.
     */
    fun setHeight(state: SkyscrapersState, pos: GridPos, height: Int): SkyscrapersState {
        val idx = state.indexOf(pos)
        if (idx in state.givens) return state
        if (height !in 0..state.size) return state
        val snapshot = SkyscrapersSnapshot(state.grid, state.pencilMarks)
        val newGrid = state.grid.toMutableList().also { it[idx] = height }
        val newMarks = state.pencilMarks - idx
        return state.copy(
            grid = newGrid,
            pencilMarks = newMarks,
            history = state.history + snapshot
        )
    }

    /**
     * Toggles a pencil-mark candidate [height] at [pos]. No-op when the cell
     * already has a definite height or is a given.
     */
    fun togglePencil(state: SkyscrapersState, pos: GridPos, height: Int): SkyscrapersState {
        val idx = state.indexOf(pos)
        if (idx in state.givens) return state
        if (height !in 1..state.size) return state
        if (state.grid[idx] != 0) return state
        val snapshot = SkyscrapersSnapshot(state.grid, state.pencilMarks)
        val current = state.pencilMarks[idx] ?: emptySet()
        val newSet = if (height in current) current - height else current + height
        val newMarks = if (newSet.isEmpty()) state.pencilMarks - idx else state.pencilMarks + (idx to newSet)
        return state.copy(pencilMarks = newMarks, history = state.history + snapshot)
    }

    /** Reverts the most recent [setHeight] or [togglePencil] action, if any. */
    fun undo(state: SkyscrapersState): SkyscrapersState {
        val snap = state.history.lastOrNull() ?: return state
        return state.copy(
            grid = snap.grid,
            pencilMarks = snap.pencilMarks,
            history = state.history.dropLast(1)
        )
    }

    // -------------------------------------------------------------------------
    // Conflict / clue status
    // -------------------------------------------------------------------------

    /**
     * Returns the flat indices of cells whose value creates a duplicate in the
     * same row or column.
     */
    fun conflictIndices(state: SkyscrapersState): Set<Int> {
        val n = state.size
        val result = mutableSetOf<Int>()
        for (r in 0 until n) {
            val vals = (0 until n).map { c -> state.grid[r * n + c] }.filter { it != 0 }
            val dupes = vals.groupBy { it }.filter { it.value.size > 1 }.keys
            if (dupes.isNotEmpty()) {
                for (c in 0 until n) {
                    if (state.grid[r * n + c] in dupes) result += r * n + c
                }
            }
        }
        for (c in 0 until n) {
            val vals = (0 until n).map { r -> state.grid[r * n + c] }.filter { it != 0 }
            val dupes = vals.groupBy { it }.filter { it.value.size > 1 }.keys
            if (dupes.isNotEmpty()) {
                for (r in 0 until n) {
                    if (state.grid[r * n + c] in dupes) result += r * n + c
                }
            }
        }
        return result
    }

    /**
     * Evaluates every edge clue against the current player grid.
     * A clue is PENDING when its line still has empty cells, SATISFIED when the
     * visible count matches, and VIOLATED when the line is full but the count
     * does not match.
     */
    fun clueStatuses(state: SkyscrapersState): ClueStatuses {
        val n = state.size
        fun lineStatus(line: List<Int>, clue: Int): ClueState {
            if (line.any { it == 0 }) return ClueState.PENDING
            return if (visibleCount(line) == clue) ClueState.SATISFIED else ClueState.VIOLATED
        }
        val top = (0 until n).map { c ->
            lineStatus((0 until n).map { r -> state.grid[r * n + c] }, state.clues.top[c])
        }
        val bottom = (0 until n).map { c ->
            lineStatus((0 until n).map { r -> state.grid[r * n + c] }.reversed(), state.clues.bottom[c])
        }
        val left = (0 until n).map { r ->
            lineStatus((0 until n).map { c -> state.grid[r * n + c] }, state.clues.left[r])
        }
        val right = (0 until n).map { r ->
            lineStatus((0 until n).map { c -> state.grid[r * n + c] }.reversed(), state.clues.right[r])
        }
        return ClueStatuses(top, bottom, left, right)
    }

    // -------------------------------------------------------------------------
    // Solve check
    // -------------------------------------------------------------------------

    /**
     * Returns true when the grid is a complete valid Latin square (each row and
     * column contains 1..N exactly once) AND every edge clue is satisfied.
     */
    fun isSolved(state: SkyscrapersState): Boolean {
        val n = state.size
        if (state.grid.any { it == 0 }) return false
        val expected = (1..n).toSet()
        for (r in 0 until n) {
            if ((0 until n).map { c -> state.grid[r * n + c] }.toSet() != expected) return false
        }
        for (c in 0 until n) {
            if ((0 until n).map { r -> state.grid[r * n + c] }.toSet() != expected) return false
        }
        val statuses = clueStatuses(state)
        return statuses.top.all { it == ClueState.SATISFIED } &&
                statuses.bottom.all { it == ClueState.SATISFIED } &&
                statuses.left.all { it == ClueState.SATISFIED } &&
                statuses.right.all { it == ClueState.SATISFIED }
    }

    /** Number of empty (0) cells remaining. */
    fun remainingCells(state: SkyscrapersState): Int = state.grid.count { it == 0 }

    // -------------------------------------------------------------------------
    // Internal generation helpers
    // -------------------------------------------------------------------------

    /**
     * Generates a uniformly-random Latin square of size [n] by:
     * 1. Starting with the cyclic base: cell(r, c) = (r + c) % n + 1.
     * 2. Applying a random row permutation.
     * 3. Applying a random column permutation.
     * 4. Relabelling values with a random permutation of 1..n.
     */
    private fun generateLatinSquare(n: Int, random: Random): List<Int> {
        val rowPerm = (0 until n).toMutableList().also { it.shuffle(random) }
        val colPerm = (0 until n).toMutableList().also { it.shuffle(random) }
        val valPerm = (1..n).toMutableList().also { it.shuffle(random) }
        val result = IntArray(n * n)
        for (r in 0 until n) {
            for (c in 0 until n) {
                val baseVal = (rowPerm[r] + colPerm[c]) % n + 1
                result[r * n + c] = valPerm[baseVal - 1]
            }
        }
        return result.toList()
    }

    private fun deriveClues(solution: List<Int>, n: Int): SkyscrapersClues {
        val top = (0 until n).map { c ->
            visibleCount((0 until n).map { r -> solution[r * n + c] })
        }
        val bottom = (0 until n).map { c ->
            visibleCount((0 until n).map { r -> solution[r * n + c] }.reversed())
        }
        val left = (0 until n).map { r ->
            visibleCount((0 until n).map { c -> solution[r * n + c] })
        }
        val right = (0 until n).map { r ->
            visibleCount((0 until n).map { c -> solution[r * n + c] }.reversed())
        }
        return SkyscrapersClues(top, bottom, left, right)
    }
}
