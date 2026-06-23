package com.xanticious.androidgames.model.games.matchthree

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * The six gem types. Each maps to a distinct color and shape so colour-blind
 * players can distinguish them. The ordinal order determines which types are
 * active when [MatchThreeState.gemTypes] is less than six.
 */
enum class GemType {
    AQUA,   // circle       — PuzzleHueTeal
    TEAL,   // diamond      — PuzzleHueBlue
    CORAL,  // triangle     — PuzzleHueOrange
    GREEN,  // hexagon      — PuzzleHueGreen
    SAND,   // star         — PuzzleHueYellow
    DEEP    // rounded-square — PuzzleHueViolet
}

/** Selects which of the two Match Three variants is active. */
enum class MatchThreeVariant { ZEN, ARCADE }

/**
 * Immutable 2-D gem grid. [gems] is stored row-major (index = row * cols + col).
 * `null` means a cell is empty — this is only a transient state during cascade
 * resolution inside the controller; the board returned to the view always has
 * every cell filled.
 */
data class MatchThreeBoard(
    val rows: Int,
    val cols: Int,
    val gems: List<GemType?>
) {
    fun get(pos: GridPos): GemType? =
        if (pos.row in 0 until rows && pos.col in 0 until cols)
            gems[pos.row * cols + pos.col]
        else null

    fun isValid(pos: GridPos): Boolean =
        pos.row in 0 until rows && pos.col in 0 until cols

    companion object {
        fun empty(rows: Int, cols: Int): MatchThreeBoard =
            MatchThreeBoard(rows, cols, List(rows * cols) { null })
    }
}

/** Full result returned by [com.xanticious.androidgames.controller.games.matchthree.MatchThreeController.applySwap]. */
data class SwapResult(
    /** True when the swap produced at least one match and the board was updated. */
    val valid: Boolean,
    /** The resolved board after all cascades. Equals the pre-swap board when [valid] is false. */
    val board: MatchThreeBoard,
    /** Total individual gem cells cleared across all cascade rounds. */
    val totalCleared: Int,
    /** Number of cascade rounds (1 = the initial match only, 2+ = chain reactions). */
    val cascadeCount: Int
)

/** Full result returned by [com.xanticious.androidgames.controller.games.matchthree.MatchThreeController.resolveCascades]. */
data class CascadeResult(
    val board: MatchThreeBoard,
    val totalCleared: Int,
    val cascadeCount: Int
)

/**
 * All mutable game state that the view renders. Kept in the composable as a
 * `remember { mutableStateOf(...) }` and updated atomically each move.
 */
data class MatchThreeState(
    val board: MatchThreeBoard,
    /** Currently selected cell awaiting a partner for the swap. */
    val selected: GridPos? = null,
    /** How many of the six gem types are active (4, 5, or 6). */
    val gemTypes: Int = 6,
    /** Lifetime gem cells cleared this session. */
    val totalCleared: Int = 0,
    /** ZEN: lifetime gem count; ARCADE: valid swaps = score. */
    val swapCount: Int = 0,
    /** Best cascade chain length seen this session. */
    val cascadeBest: Int = 0
)
