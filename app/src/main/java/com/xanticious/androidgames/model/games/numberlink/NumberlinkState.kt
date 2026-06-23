package com.xanticious.androidgames.model.games.numberlink

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * One numbered endpoint on the Numberlink grid.
 * Two endpoints share the same [pairIndex]; the player must connect them.
 */
data class NumberlinkEndpoint(
    val pos: GridPos,
    val pairIndex: Int
)

/**
 * The current drawn path for one color pair.
 * [cells] is ordered from the start endpoint toward the finish; it is empty
 * until the player begins drawing.
 */
data class NumberlinkPath(
    val pairIndex: Int,
    val cells: List<GridPos> = emptyList()
)

/**
 * Immutable snapshot of one Numberlink game.
 *
 * [endpoints] holds exactly two entries per [pairIndex].
 * [paths] holds the player's current drawing (one entry per pair, may be empty).
 * [solutionPaths] is the hidden reference solution produced by the generator;
 *   it is never shown to the player but is kept so tests can verify generation
 *   correctness without re-running the solver.
 */
data class NumberlinkState(
    val size: Int,
    val endpoints: List<NumberlinkEndpoint>,
    val paths: List<NumberlinkPath>,
    val solutionPaths: List<NumberlinkPath> = emptyList(),
    val activePairIndex: Int? = null,
    val requireFullCoverage: Boolean = true,
    val history: List<List<NumberlinkPath>> = emptyList()
) {
    val numPairs: Int
        get() = endpoints.asSequence().map { it.pairIndex }.distinct().count()

    val canUndo: Boolean
        get() = history.isNotEmpty()

    /** Returns the two endpoint positions for [pairIndex]. */
    fun endpointsFor(pairIndex: Int): Pair<GridPos, GridPos> {
        val eps = endpoints.filter { it.pairIndex == pairIndex }
        return eps[0].pos to eps[1].pos
    }

    /** Returns the current path for [pairIndex], defaulting to an empty path. */
    fun pathFor(pairIndex: Int): NumberlinkPath =
        paths.firstOrNull { it.pairIndex == pairIndex } ?: NumberlinkPath(pairIndex)

    /** Total cells covered by all drawn paths. */
    val filledCellCount: Int
        get() = paths.sumOf { it.cells.size }

    /** Count of pairs whose path correctly connects both endpoints. */
    val connectedPairCount: Int
        get() = (0 until numPairs).count { pairConnected(it) }

    /** True if [pairIndex]'s path runs from one endpoint to the other. */
    fun pairConnected(pairIndex: Int): Boolean {
        val path = pathFor(pairIndex)
        if (path.cells.size < 2) return false
        val (ep1, ep2) = endpointsFor(pairIndex)
        return (path.cells.first() == ep1 && path.cells.last() == ep2) ||
            (path.cells.first() == ep2 && path.cells.last() == ep1)
    }
}
