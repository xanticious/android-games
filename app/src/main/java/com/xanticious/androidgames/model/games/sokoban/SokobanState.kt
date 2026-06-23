package com.xanticious.androidgames.model.games.sokoban

import com.xanticious.androidgames.model.games.puzzle.GridPos

/** What a grid cell permanently is (walls never move, goals never move). */
enum class SokobanCell { WALL, FLOOR, GOAL }

/**
 * A point-in-time snapshot saved before each move so the player can undo.
 * Stores counters so undo fully restores the displayed state.
 */
data class SokobanSnapshot(
    val player: GridPos,
    val boxes: Set<GridPos>,
    val moves: Int,
    val pushes: Int
)

/**
 * The static description of one level, parsed once from its ASCII map.
 * Nothing here changes during play.
 */
data class SokobanLevel(
    val rows: Int,
    val cols: Int,
    /** Row-major flat cell list; size == rows × cols. */
    val cells: List<SokobanCell>,
    val goals: Set<GridPos>,
    val initialPlayer: GridPos,
    val initialBoxes: Set<GridPos>
)

/**
 * Live game state for an in-progress Sokoban level.
 *
 * [level] is the immutable board structure. The mutable "overlay" — player
 * position, box positions, counters and undo history — lives alongside it.
 */
data class SokobanState(
    val level: SokobanLevel,
    /** Zero-based index within the active level set, used for display. */
    val levelIndex: Int,
    val player: GridPos,
    val boxes: Set<GridPos>,
    val moves: Int = 0,
    val pushes: Int = 0,
    /** Set to true the first time the player uses undo; drives the star rating. */
    val usedUndo: Boolean = false,
    val history: List<SokobanSnapshot> = emptyList()
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    /** Returns the [SokobanCell] at [pos]; WALL for any out-of-bounds coordinate. */
    fun cell(pos: GridPos): SokobanCell =
        if (pos.row in 0 until level.rows && pos.col in 0 until level.cols)
            level.cells[pos.row * level.cols + pos.col]
        else SokobanCell.WALL
}
