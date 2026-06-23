package com.xanticious.androidgames.controller.games.jigsaw

import com.xanticious.androidgames.model.games.jigsaw.JigsawPiece
import com.xanticious.androidgames.model.games.jigsaw.JigsawState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import kotlin.random.Random

/**
 * Pure Jigsaw rules: generation, placement, removal and solve-check.
 *
 * No Android or Compose imports — the entire rule set is JVM unit-testable.
 * Piece generation is seedable via [kotlin.random.Random] for deterministic tests.
 */
class JigsawController {

    /** Available procedural image ids (0–7); view layer maps each id to a draw fn. */
    val imageIds: List<Int> = (0 until 8).toList()

    /** Available (rows, cols) grid sizes keyed by label. */
    val gridSizes: List<Pair<Int, Int>> = listOf(
        3 to 4,   // 12 pieces — Easy
        4 to 5,   // 20 pieces — Medium
        5 to 6    // 30 pieces — Hard
    )

    /**
     * Deals a fresh game: creates [rows]×[cols] pieces and shuffles them into the
     * bank. The same [random] seed always produces the same bank order.
     */
    fun newGame(imageId: Int, rows: Int, cols: Int, random: Random = Random.Default): JigsawState {
        val pieces = (0 until rows * cols).map { i ->
            JigsawPiece(id = i, correctRow = i / cols, correctCol = i % cols)
        }
        val bankOrder = pieces.map { it.id }.shuffled(random)
        return JigsawState(
            imageId = imageId,
            rows = rows,
            cols = cols,
            pieces = pieces,
            bankOrder = bankOrder,
            placedPieces = emptyMap()
        )
    }

    /**
     * Attempts to place [pieceId] at ([row], [col]).
     *
     * Succeeds **only** when ([row], [col]) is the piece's correct home (snap-only-
     * correct rule from the design doc). Returns the updated state on success, or
     * `null` on rejection (wrong cell or cell already occupied).
     */
    fun placePiece(state: JigsawState, pieceId: Int, row: Int, col: Int): JigsawState? {
        val piece = state.pieces.firstOrNull { it.id == pieceId } ?: return null
        if (piece.correctRow != row || piece.correctCol != col) return null
        val pos = GridPos(row, col)
        if (state.placedPieces.containsKey(pos)) return null
        return state.copy(
            bankOrder = state.bankOrder.filter { it != pieceId },
            placedPieces = state.placedPieces + (pos to pieceId)
        )
    }

    /**
     * Removes the placed piece at ([row], [col]) and returns it to the end of the
     * bank. Returns the state unchanged if the cell is empty.
     */
    fun removePiece(state: JigsawState, row: Int, col: Int): JigsawState {
        val pos = GridPos(row, col)
        val pieceId = state.placedPieces[pos] ?: return state
        return state.copy(
            bankOrder = state.bankOrder + pieceId,
            placedPieces = state.placedPieces - pos
        )
    }

    /** Returns `true` when every board cell has its correct piece. */
    fun isSolved(state: JigsawState): Boolean =
        state.placedCount == state.totalPieces

    /** Counts placed pieces (for progress display). */
    fun placedCount(state: JigsawState): Int = state.placedCount
}
