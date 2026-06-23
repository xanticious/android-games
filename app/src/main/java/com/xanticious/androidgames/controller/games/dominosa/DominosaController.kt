package com.xanticious.androidgames.controller.games.dominosa

import com.xanticious.androidgames.model.games.dominosa.DominosaPair
import com.xanticious.androidgames.model.games.dominosa.DominoPlacement
import com.xanticious.androidgames.model.games.dominosa.DominosaState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure Dominosa rules: puzzle generation, placement, removal, undo, hint, and
 * solve-check. No Android or Compose imports — the full rule set is JVM unit-testable.
 *
 * A size-[n] puzzle uses the complete domino set 0..n, fills an (n+1)×(n+2) grid,
 * and contains (n+1)(n+2)/2 dominoes. The player must partition every cell into
 * 1×2 dominoes so each value-pair appears exactly once.
 */
class DominosaController {

    /** Valid puzzle sizes N, smallest (easiest) first. */
    val sizes: List<Int> = listOf(3, 4, 5, 6, 7, 8)

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Generates a new Dominosa puzzle of size [n] using [random] for
     * reproducibility.
     *
     * Strategy: build a deterministic tiling skeleton (the set of cell pairs
     * that will host each domino), shuffle all dominoes of the set, assign one
     * to each pair with a randomly chosen orientation, then erase the boundaries
     * to leave only pip numbers. The hidden reference solution is retained in the
     * returned state for hint and solve-check purposes.
     *
     * The tiling skeleton tiles the grid perfectly:
     * - When [n]+2 (cols) is even, every row is tiled fully horizontally.
     * - When [n]+2 (cols) is odd, the first (cols−1) columns of every row are
     *   tiled horizontally and the final column is tiled vertically (possible
     *   because rows = n+1 is even whenever cols is odd).
     */
    fun newGame(n: Int, random: Random = Random.Default): DominosaState {
        val rows = n + 1
        val cols = n + 2
        val cellPairs = buildTilingSkeleton(rows, cols)

        val allDominoes = mutableListOf<DominosaPair>()
        for (a in 0..n) for (b in a..n) allDominoes.add(DominosaPair(a, b))
        val shuffled = allDominoes.shuffled(random)

        check(shuffled.size == cellPairs.size) { "Domino/pair count mismatch for n=$n" }

        val grid = Array(rows) { IntArray(cols) }
        val solution = ArrayList<DominoPlacement>(shuffled.size)

        for (i in cellPairs.indices) {
            val (c1, c2) = cellPairs[i]
            val domino = shuffled[i]
            // Doubles look the same in both orientations; skip a random call.
            val aFirst = domino.a == domino.b || random.nextBoolean()
            grid[c1.row][c1.col] = if (aFirst) domino.a else domino.b
            grid[c2.row][c2.col] = if (aFirst) domino.b else domino.a
            solution.add(DominoPlacement(c1, c2))
        }

        return DominosaState(
            n = n,
            grid = grid.map { it.toList() },
            solution = solution,
            placed = emptyList(),
            selected = null
        )
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Handles a cell tap using the two-tap placement flow:
     * - Tap an occupied cell → remove its domino.
     * - Tap the currently selected cell → deselect.
     * - Tap a free cell with a different cell already selected and adjacent → place domino.
     * - Tap a free cell with a non-adjacent cell selected → change selection to new cell.
     * - Tap a free cell with nothing selected → select it.
     */
    fun tapCell(state: DominosaState, pos: GridPos): DominosaState {
        val sel = state.selected
        return when {
            state.isOccupied(pos) -> removeDomino(state.copy(selected = null), pos)
            pos == sel -> state.copy(selected = null)
            sel != null && isAdjacent(sel, pos) -> placeDomino(state, sel, pos)
            sel != null -> state.copy(selected = pos)
            else -> state.copy(selected = pos)
        }
    }

    /**
     * Finalises a drag gesture from [from] to [to].
     * Places a domino if the cells are adjacent and both free; otherwise no-op.
     */
    fun dragPlaceDomino(state: DominosaState, from: GridPos, to: GridPos): DominosaState {
        if (from == to || !isAdjacent(from, to)) return state.copy(selected = null)
        return placeDomino(state.copy(selected = null), from, to)
    }

    /**
     * Places a domino covering cells [a] and [b] if they are orthogonally adjacent
     * and both free. Returns the unchanged state on any constraint violation.
     */
    fun placeDomino(state: DominosaState, a: GridPos, b: GridPos): DominosaState {
        if (!isAdjacent(a, b)) return state
        if (state.isOccupied(a) || state.isOccupied(b)) return state
        val c1 = if (a.row < b.row || (a.row == b.row && a.col < b.col)) a else b
        val c2 = if (c1 == a) b else a
        return state.copy(
            placed = state.placed + DominoPlacement(c1, c2),
            selected = null,
            history = state.history + listOf(state.placed)
        )
    }

    /** Removes the domino covering [cell]. Returns the unchanged state if the cell is free. */
    fun removeDomino(state: DominosaState, cell: GridPos): DominosaState {
        val placement = state.dominoAt(cell) ?: return state
        return state.copy(
            placed = state.placed - placement,
            selected = null,
            history = state.history + listOf(state.placed)
        )
    }

    /** Reverts the most recent placement or removal; no-op when history is empty. */
    fun undo(state: DominosaState): DominosaState {
        val prev = state.history.lastOrNull() ?: return state
        return state.copy(
            placed = prev,
            selected = null,
            history = state.history.dropLast(1)
        )
    }

    /**
     * Applies one hint: places the first solution domino whose both cells are
     * still free. Returns the state unchanged when every solution domino already
     * overlaps a player placement or an occupied cell.
     */
    fun hint(state: DominosaState): DominosaState {
        val candidate = state.solution.firstOrNull { sol ->
            !state.placed.contains(sol) &&
                !state.isOccupied(sol.cell1) &&
                !state.isOccupied(sol.cell2)
        } ?: return state
        return placeDomino(state, candidate.cell1, candidate.cell2)
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * True when every cell is covered and the multiset of placed value-pairs
     * equals the complete domino set {(a,b) | 0 ≤ a ≤ b ≤ n} with no duplicates.
     */
    fun isSolved(state: DominosaState): Boolean {
        if (state.placed.size * 2 != state.rows * state.cols) return false
        val placed = state.placed.map { p ->
            DominosaPair.of(state.pipAt(p.cell1), state.pipAt(p.cell2))
        }.sorted()
        val expected = (0..state.n).flatMap { a ->
            (a..state.n).map { b -> DominosaPair(a, b) }
        }.sorted()
        return placed == expected
    }

    /** True when any two placed dominoes share the same value-pair. */
    fun hasConflict(state: DominosaState): Boolean =
        conflictPlacements(state).isNotEmpty()

    /**
     * Returns all [DominoPlacement]s involved in a duplicate value-pair conflict.
     * A domino appears in this set only when at least one other placed domino
     * has the same pip pair.
     */
    fun conflictPlacements(state: DominosaState): Set<DominoPlacement> {
        val byPair = mutableMapOf<DominosaPair, MutableList<DominoPlacement>>()
        for (p in state.placed) {
            val pair = DominosaPair.of(state.pipAt(p.cell1), state.pipAt(p.cell2))
            byPair.getOrPut(pair) { mutableListOf() }.add(p)
        }
        return byPair.values.asSequence()
            .filter { it.size > 1 }
            .flatten()
            .toSet()
    }

    /** Number of dominoes the player has placed so far. */
    fun placedCount(state: DominosaState): Int = state.placed.size

    /** Total number of dominoes in this puzzle. */
    fun totalCount(state: DominosaState): Int = state.solution.size

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun isAdjacent(a: GridPos, b: GridPos): Boolean =
        (a.row == b.row && abs(a.col - b.col) == 1) ||
            (a.col == b.col && abs(a.row - b.row) == 1)

    /**
     * Builds the deterministic tiling skeleton: a list of adjacent cell pairs
     * that together cover every cell in the [rows]×[cols] grid exactly once.
     */
    private fun buildTilingSkeleton(rows: Int, cols: Int): List<Pair<GridPos, GridPos>> {
        val pairs = ArrayList<Pair<GridPos, GridPos>>((rows * cols) / 2)
        if (cols % 2 == 0) {
            for (r in 0 until rows) {
                var c = 0
                while (c < cols) {
                    pairs.add(GridPos(r, c) to GridPos(r, c + 1))
                    c += 2
                }
            }
        } else {
            // Tile the first (cols−1) columns horizontally, last column vertically.
            for (r in 0 until rows) {
                var c = 0
                while (c < cols - 1) {
                    pairs.add(GridPos(r, c) to GridPos(r, c + 1))
                    c += 2
                }
            }
            var r = 0
            while (r < rows) {
                pairs.add(GridPos(r, cols - 1) to GridPos(r + 1, cols - 1))
                r += 2
            }
        }
        return pairs
    }
}
