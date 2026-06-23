package com.xanticious.androidgames.view.common.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleGridLine

/**
 * Generic square-celled puzzle board renderer (`design/common/puzzle-grid-board.md`).
 *
 * This composable owns only geometry and gesture mapping: it computes a uniform
 * cell size from the smaller of available width/height divided by the larger grid
 * dimension, centres the board, draws the shared board background and grid lines,
 * then defers every cell's appearance to [drawCell]. It contains no game logic —
 * the caller's `controller/` decides what each cell looks like and what a tap or
 * drag means.
 *
 * @param rows number of grid rows.
 * @param cols number of grid columns.
 * @param onCellTap invoked with the tapped cell, if any.
 * @param onCellLongPress invoked with the long-pressed cell (secondary action).
 * @param onCellDrag invoked continuously with each cell the pointer drags over.
 * @param onDragEnd invoked once a drag gesture finishes.
 * @param maxCellSize caps cell growth so tiny grids do not become comically large
 *   on tablets.
 * @param drawCell renders a single cell into [rect]; called for every cell each
 *   frame the board recomposes.
 */
@Composable
fun PuzzleBoard(
    rows: Int,
    cols: Int,
    modifier: Modifier = Modifier,
    onCellTap: ((GridPos) -> Unit)? = null,
    onCellLongPress: ((GridPos) -> Unit)? = null,
    onCellDrag: ((GridPos) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    drawGridLines: Boolean = true,
    maxCellSize: Dp = 96.dp,
    drawCell: PuzzleCellScope.() -> Unit
) {
    if (rows <= 0 || cols <= 0) return
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rows, cols, onCellTap, onCellLongPress) {
                    if (onCellTap == null && onCellLongPress == null) return@pointerInput
                    val maxPx = maxCellSize.toPx()
                    detectTapGestures(
                        onTap = { offset ->
                            onCellTap?.let { cb -> cellAt(offset, rows, cols, size.width.toFloat(), size.height.toFloat(), maxPx)?.let(cb) }
                        },
                        onLongPress = { offset ->
                            onCellLongPress?.let { cb -> cellAt(offset, rows, cols, size.width.toFloat(), size.height.toFloat(), maxPx)?.let(cb) }
                        }
                    )
                }
                .pointerInput(rows, cols, onCellDrag) {
                    if (onCellDrag == null) return@pointerInput
                    val maxPx = maxCellSize.toPx()
                    detectDragGestures(
                        onDragEnd = { onDragEnd?.invoke() },
                        onDragCancel = { onDragEnd?.invoke() }
                    ) { change, _ ->
                        cellAt(change.position, rows, cols, size.width.toFloat(), size.height.toFloat(), maxPx)?.let { onCellDrag(it) }
                    }
                }
        ) {
            val maxPx = maxCellSize.toPx()
            val cell = minOf(size.width / cols, size.height / rows, maxPx)
            val boardW = cell * cols
            val boardH = cell * rows
            val originX = (size.width - boardW) / 2f
            val originY = (size.height - boardH) / 2f

            drawRect(color = PuzzleBoard, topLeft = Offset(originX, originY), size = Size(boardW, boardH))

            val scope = PuzzleCellScope(this, originX, originY, cell)
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    scope.current = GridPos(r, c)
                    scope.drawCell()
                }
            }

            if (drawGridLines) {
                val stroke = (cell * 0.02f).coerceIn(1f, 3f)
                for (r in 0..rows) {
                    val y = originY + r * cell
                    drawLine(PuzzleGridLine, Offset(originX, y), Offset(originX + boardW, y), strokeWidth = stroke)
                }
                for (c in 0..cols) {
                    val x = originX + c * cell
                    drawLine(PuzzleGridLine, Offset(x, originY), Offset(x, originY + boardH), strokeWidth = stroke)
                }
            }
        }
    }
}

/** Maps a pointer [offset] (in pixels) to the grid cell under it, or null. */
private fun cellAt(offset: Offset, rows: Int, cols: Int, width: Float, height: Float, maxCellPx: Float = Float.MAX_VALUE): GridPos? {
    val cell = minOf(width / cols, height / rows, maxCellPx)
    val originX = (width - cell * cols) / 2f
    val originY = (height - cell * rows) / 2f
    val col = ((offset.x - originX) / cell).toInt()
    val row = ((offset.y - originY) / cell).toInt()
    return if (row in 0 until rows && col in 0 until cols) GridPos(row, col) else null
}
