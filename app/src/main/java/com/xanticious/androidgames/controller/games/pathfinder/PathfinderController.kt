package com.xanticious.androidgames.controller.games.pathfinder

import com.xanticious.androidgames.model.games.pathfinder.PathfinderEndpoint
import com.xanticious.androidgames.model.games.pathfinder.PathfinderPath
import com.xanticious.androidgames.model.games.pathfinder.PathfinderState
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.step
import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure Pathfinder rules: puzzle generation, route manipulation, and solve checking.
 *
 * Generation strategy: find a Hamiltonian path through the N×N grid using a greedy
 * Warnsdorff walk (fewest-onward-moves heuristic with random tiebreaking), then divide
 * that path into [numPairs] segments. The segment endpoints become the puzzle's colored
 * dot pairs; the full segments are stored as [PathfinderState.solutionPaths] for testing.
 *
 * Distinguishes itself from Numberlink by supporting larger grids (up to 13×13) and
 * using color + glyph endpoint presentation. All functions are stateless and seedable
 * via [Random], so the same seed always produces the same puzzle.
 */
class PathfinderController {

    /** Grid sizes available in settings (Easy → Hard). */
    val sizes: List<Int> = listOf(5, 8, 10, 13)

    /** Default number of color pairs for a given grid [size]. */
    fun defaultNumPairs(size: Int): Int = size

    // ── Generation ───────────────────────────────────────────────────────────

    /**
     * Deals a guaranteed full-coverage Pathfinder puzzle of the given [size] with
     * [numPairs] color pairs. Pass an explicit [random] for deterministic output.
     */
    fun newGame(
        size: Int,
        numPairs: Int = defaultNumPairs(size),
        requireFullCoverage: Boolean = true,
        random: Random = Random.Default
    ): PathfinderState {
        require(size >= 3) { "size must be >= 3" }
        require(numPairs in 2..size * size / 2) { "numPairs out of range" }

        val hamiltonianPath = findHamiltonianPath(size, random)
            ?: error("Warnsdorff failed for size=$size — try a different seed")
        val segments = divideIntoSegments(hamiltonianPath, numPairs, random)

        val solutionPaths = segments.mapIndexed { idx, seg ->
            PathfinderPath(pairIndex = idx, cells = seg)
        }
        val endpoints = solutionPaths.flatMap { path ->
            listOf(
                PathfinderEndpoint(path.cells.first(), path.pairIndex),
                PathfinderEndpoint(path.cells.last(), path.pairIndex)
            )
        }
        val emptyPaths = solutionPaths.map { PathfinderPath(it.pairIndex) }

        return PathfinderState(
            size = size,
            endpoints = endpoints,
            paths = emptyPaths,
            solutionPaths = solutionPaths,
            requireFullCoverage = requireFullCoverage
        )
    }

    // ── Route manipulation ───────────────────────────────────────────────────

    /**
     * Begins or re-routes a path at [pos].
     *
     * - If [pos] is an endpoint: clears that pair's route and sets it active,
     *   with [pos] as the first (and only) cell.
     * - If [pos] is a cell on an existing drawn route: trims that route to [pos]
     *   (inclusive) and sets it active so drawing continues from there.
     * - Otherwise: no-op.
     */
    fun startPath(state: PathfinderState, pos: GridPos): PathfinderState {
        val ep = state.endpoints.firstOrNull { it.pos == pos }
        if (ep != null) {
            val newPath = PathfinderPath(ep.pairIndex, listOf(pos))
            return state.copy(
                paths = state.paths.map { if (it.pairIndex == ep.pairIndex) newPath else it },
                activePairIndex = ep.pairIndex
            )
        }

        val existingPath = state.paths.firstOrNull { pos in it.cells }
        if (existingPath != null) {
            val idx = existingPath.cells.indexOf(pos)
            val trimmed = existingPath.copy(cells = existingPath.cells.subList(0, idx + 1))
            return state.copy(
                paths = state.paths.map { if (it.pairIndex == existingPath.pairIndex) trimmed else it },
                activePairIndex = existingPath.pairIndex
            )
        }

        return state
    }

    /**
     * Extends the active route to [pos].
     *
     * Rules:
     * - [pos] must be orthogonally adjacent to the current route's last cell.
     * - If [pos] is already in the active route, the route is trimmed back to [pos]
     *   (loop prevention / auto-trim).
     * - If [pos] is an endpoint of a **different** pair, the move is blocked.
     * - If [pos] is occupied by another route, that route is trimmed up to (but not
     *   including) [pos] before [pos] is added to the active route (auto-trim on cross).
     */
    fun extendPath(state: PathfinderState, pos: GridPos): PathfinderState {
        val pairIndex = state.activePairIndex ?: return state
        val active = state.pathFor(pairIndex)
        val last = active.cells.lastOrNull() ?: return state

        if (pos == last) return state
        if (!isAdjacent(pos, last)) return state

        // Un-loop: pos is earlier in the active route → trim back to pos
        val loopIdx = active.cells.indexOf(pos)
        if (loopIdx >= 0) {
            val trimmed = active.copy(cells = active.cells.subList(0, loopIdx + 1))
            return state.copy(paths = state.paths.map { if (it.pairIndex == pairIndex) trimmed else it })
        }

        // Block: pos is an endpoint of a different pair
        val otherEp = state.endpoints.firstOrNull { it.pos == pos && it.pairIndex != pairIndex }
        if (otherEp != null) return state

        // Auto-trim: pos is in another drawn route → trim that route before pos
        var newPaths = state.paths
        val otherPath = state.paths.firstOrNull { it.pairIndex != pairIndex && pos in it.cells }
        if (otherPath != null) {
            val trimIdx = otherPath.cells.indexOf(pos)
            val trimmedOther = otherPath.copy(cells = otherPath.cells.subList(0, trimIdx))
            newPaths = newPaths.map { if (it.pairIndex == otherPath.pairIndex) trimmedOther else it }
        }

        val extended = active.copy(cells = active.cells + pos)
        return state.copy(paths = newPaths.map { if (it.pairIndex == pairIndex) extended else it })
    }

    /** Clears the drawn route for [pairIndex] (keeps it in the list but empty). */
    fun clearPath(state: PathfinderState, pairIndex: Int): PathfinderState =
        state.copy(
            paths = state.paths.map {
                if (it.pairIndex == pairIndex) PathfinderPath(pairIndex) else it
            },
            activePairIndex = null
        )

    /** Clears every drawn route, returning to a blank board. */
    fun resetAllPaths(state: PathfinderState): PathfinderState =
        state.copy(
            paths = state.paths.map { PathfinderPath(it.pairIndex) },
            activePairIndex = null,
            history = emptyList()
        )

    /**
     * Pushes the current [paths] snapshot onto [history] so [undo] can restore it.
     * Call this after a drag gesture completes.
     */
    fun recordHistory(state: PathfinderState): PathfinderState =
        state.copy(history = state.history + listOf(state.paths))

    /** Restores the most recent [history] snapshot, if any. */
    fun undo(state: PathfinderState): PathfinderState {
        val previous = state.history.lastOrNull() ?: return state
        return state.copy(
            paths = previous,
            activePairIndex = null,
            history = state.history.dropLast(1)
        )
    }

    // ── Solve check ──────────────────────────────────────────────────────────

    /**
     * Returns `true` when:
     * - Every pair's route runs exactly from one endpoint to its twin.
     * - All routes are non-overlapping (cells unique across all routes).
     * - If [PathfinderState.requireFullCoverage]: every cell is occupied.
     */
    fun isSolved(state: PathfinderState): Boolean {
        val allConnected = (0 until state.numPairs).all { state.pairConnected(it) }
        if (!allConnected) return false

        val allCells = state.paths.flatMap { it.cells }
        if (allCells.size != allCells.toSet().size) return false

        if (state.requireFullCoverage && allCells.size != state.size * state.size) return false

        return true
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun isAdjacent(a: GridPos, b: GridPos): Boolean {
        val dr = abs(a.row - b.row)
        val dc = abs(a.col - b.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    /**
     * Finds a Hamiltonian path through the [size]×[size] grid using the greedy
     * Warnsdorff heuristic. Tries multiple starting cells in random order and
     * returns the first complete path found, or `null` if every start fails.
     */
    private fun findHamiltonianPath(size: Int, random: Random): List<GridPos>? {
        val allCells = (0 until size).flatMap { r -> (0 until size).map { c -> GridPos(r, c) } }
        for (start in allCells.shuffled(random)) {
            val result = warnsdorffWalk(size, start, random)
            if (result != null) return result
        }
        return null
    }

    private fun warnsdorffWalk(size: Int, start: GridPos, random: Random): List<GridPos>? {
        val totalCells = size * size
        val visited = Array(size) { BooleanArray(size) }
        val path = mutableListOf<GridPos>()

        visited[start.row][start.col] = true
        path.add(start)

        while (path.size < totalCells) {
            val current = path.last()
            val candidates = Direction.entries
                .map { current.step(it) }
                .filter { it.row in 0 until size && it.col in 0 until size && !visited[it.row][it.col] }

            if (candidates.isEmpty()) break

            // Warnsdorff: step to the neighbour with fewest onward options; break ties randomly
            val best = candidates
                .map { n ->
                    val onward = Direction.entries.count { dir ->
                        val nn = n.step(dir)
                        nn.row in 0 until size && nn.col in 0 until size && !visited[nn.row][nn.col]
                    }
                    n to (onward * 997 + random.nextInt(997))
                }
                .minByOrNull { it.second }!!
                .first

            visited[best.row][best.col] = true
            path.add(best)
        }

        return if (path.size == totalCells) path else null
    }

    /**
     * Splits a [path] into exactly [numPairs] contiguous segments, each with at
     * least 2 cells. Split points are distributed roughly evenly with random jitter.
     */
    private fun divideIntoSegments(path: List<GridPos>, numPairs: Int, random: Random): List<List<GridPos>> {
        val n = path.size
        val baseStep = n.toDouble() / numPairs

        val splits = mutableListOf<Int>()
        for (i in 1 until numPairs) {
            val base = (i * baseStep).toInt()
            val prev = splits.lastOrNull() ?: 0
            val minSplit = prev + 2
            val maxSplit = n - 2 * (numPairs - i)
            val jitter = ((baseStep / 3).toInt()).coerceAtLeast(1)
            val candidate = (base + random.nextInt(-jitter, jitter + 1)).coerceIn(minSplit, maxSplit)
            splits.add(candidate)
        }

        val boundaries = listOf(0) + splits + listOf(n)
        return boundaries.zipWithNext().map { (start, end) -> path.subList(start, end).toList() }
    }
}
