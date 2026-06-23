package com.xanticious.androidgames.view.common.puzzle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * Drawing context handed to [PuzzleBoard]'s `drawCell` lambda for one cell.
 *
 * It delegates every [DrawScope] drawing primitive to the underlying canvas and
 * adds the geometry a cell renderer needs: [pos] (which cell) and helpers to get
 * the cell's pixel [rect]/[center]/[size]. Keeping geometry here lets each game's
 * cell renderer stay tiny and free of layout maths.
 */
class PuzzleCellScope(
    private val draw: DrawScope,
    val originX: Float,
    val originY: Float,
    /** Side length of every (square) cell in pixels. */
    val cellSize: Float
) : DrawScope by draw {

    /** The cell currently being drawn; updated by [PuzzleBoard] before each call. */
    var current: GridPos = GridPos(0, 0)

    /** Top-left pixel offset of [current]. */
    val topLeft: Offset
        get() = Offset(originX + current.col * cellSize, originY + current.row * cellSize)

    /** Centre pixel offset of [current]. */
    val center: Offset
        get() = Offset(originX + (current.col + 0.5f) * cellSize, originY + (current.row + 0.5f) * cellSize)

    /** Square [Size] of one cell. */
    val cellSquare: Size
        get() = Size(cellSize, cellSize)

    /** Pixel [Rect] covering [current], optionally inset by [inset] pixels on each side. */
    fun rect(inset: Float = 0f): Rect {
        val tl = topLeft
        return Rect(tl.x + inset, tl.y + inset, tl.x + cellSize - inset, tl.y + cellSize - inset)
    }
}
