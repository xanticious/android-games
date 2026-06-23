package com.xanticious.androidgames.model.games.pathfinder

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * Distinct shape glyph assigned to each color pair so the puzzle is playable
 * without relying on color alone (color-blind accessibility).
 */
enum class EndpointGlyph {
    CIRCLE, SQUARE, TRIANGLE, DIAMOND, PENTAGON, HEXAGON, CROSS, STAR;

    companion object {
        fun forPair(pairIndex: Int): EndpointGlyph =
            entries[pairIndex % entries.size]
    }
}

/** One colored dot endpoint on the Pathfinder grid. Two endpoints share the same [pairIndex]. */
data class PathfinderEndpoint(
    val pos: GridPos,
    val pairIndex: Int
)

/**
 * The current drawn route for one color pair.
 * [cells] is ordered from the start endpoint toward the finish; it is empty
 * until the player begins drawing.
 */
data class PathfinderPath(
    val pairIndex: Int,
    val cells: List<GridPos> = emptyList()
)

/**
 * Immutable snapshot of one Pathfinder game.
 *
 * [endpoints] holds exactly two entries per [pairIndex].
 * [paths] holds the player's current routes (one entry per pair, may be empty).
 * [solutionPaths] is the hidden reference solution produced by the generator;
 *   it is never shown to the player but kept so tests can verify generation.
 */
data class PathfinderState(
    val size: Int,
    val endpoints: List<PathfinderEndpoint>,
    val paths: List<PathfinderPath>,
    val solutionPaths: List<PathfinderPath> = emptyList(),
    val activePairIndex: Int? = null,
    val requireFullCoverage: Boolean = true,
    val history: List<List<PathfinderPath>> = emptyList()
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
    fun pathFor(pairIndex: Int): PathfinderPath =
        paths.firstOrNull { it.pairIndex == pairIndex } ?: PathfinderPath(pairIndex)

    /** Total cells covered by all drawn routes. */
    val filledCellCount: Int
        get() = paths.sumOf { it.cells.size }

    /** Count of pairs whose route correctly connects both endpoints. */
    val connectedPairCount: Int
        get() = (0 until numPairs).count { pairConnected(it) }

    /** True if [pairIndex]'s route runs from one endpoint to the other. */
    fun pairConnected(pairIndex: Int): Boolean {
        val path = pathFor(pairIndex)
        if (path.cells.size < 2) return false
        val (ep1, ep2) = endpointsFor(pairIndex)
        return (path.cells.first() == ep1 && path.cells.last() == ep2) ||
            (path.cells.first() == ep2 && path.cells.last() == ep1)
    }
}
