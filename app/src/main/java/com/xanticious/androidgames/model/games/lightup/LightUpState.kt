package com.xanticious.androidgames.model.games.lightup

import com.xanticious.androidgames.model.games.puzzle.GridPos

/** One cell in the Light Up grid. */
data class LightUpCell(
    val isWall: Boolean,
    val wallNumber: Int? = null,    // null = unnumbered; 0..4 = adjacent-bulb clue
    val hasBulb: Boolean = false,   // player-placed bulb (white cells only)
    val hasMark: Boolean = false    // player annotation X-mark (white cells only)
)

/**
 * Immutable snapshot of a Light Up (Akari) puzzle
 * (`design/puzzle-games/light-up/light-up-design.md`).
 *
 * [cells] is row-major: index = row × cols + col.
 * [solutionBulbs] holds the flat indices of every bulb in the canonical solution
 * and is used only for hints — it is never shown to the player directly.
 */
data class LightUpState(
    val rows: Int,
    val cols: Int,
    val cells: List<LightUpCell>,
    val solutionBulbs: Set<Int> = emptySet(),
    val history: List<List<LightUpCell>> = emptyList()
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    fun cellAt(pos: GridPos): LightUpCell = cells[pos.row * cols + pos.col]

    fun inBounds(pos: GridPos): Boolean =
        pos.row in 0 until rows && pos.col in 0 until cols
}
