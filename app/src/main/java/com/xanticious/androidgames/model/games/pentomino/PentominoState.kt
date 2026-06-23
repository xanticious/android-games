package com.xanticious.androidgames.model.games.pentomino

import com.xanticious.androidgames.model.games.puzzle.GridPos

/** The twelve free pentomino pieces, named by convention F–Z. */
enum class PentominoPiece { F, I, L, N, P, T, U, V, W, X, Y, Z }

/** Rectangular board configurations; all hold exactly 60 cells (12 pieces × 5). */
enum class BoardSize(val rows: Int, val cols: Int) {
    SIX_BY_TEN(6, 10),
    FIVE_BY_TWELVE(5, 12),
    FOUR_BY_FIFTEEN(4, 15),
    THREE_BY_TWENTY(3, 20);

    val label: String get() = "${rows}×${cols}"
}

/**
 * Immutable Pentomino game state (`design/puzzle-games/pentomino`).
 *
 * All board intelligence lives in `PentominoController`. This class is a pure
 * value carrier: it records which pieces have been placed and where, plus an
 * undo stack of previous placement maps.
 *
 * [placements] maps each placed piece to the list of board cells it occupies.
 * [cellMap] is the inverse (computed, not stored): board cell → piece occupying it.
 */
data class PentominoState(
    val boardSize: BoardSize = BoardSize.SIX_BY_TEN,
    val allowFlip: Boolean = true,
    val placements: Map<PentominoPiece, List<GridPos>> = emptyMap(),
    val history: List<Map<PentominoPiece, List<GridPos>>> = emptyList()
) {
    val placedPieces: Set<PentominoPiece> get() = placements.keys
    val remainingPieces: List<PentominoPiece>
        get() = PentominoPiece.entries.filter { it !in placedPieces }
    val cellMap: Map<GridPos, PentominoPiece>
        get() = placements.entries.flatMap { (piece, cells) -> cells.map { it to piece } }.toMap()
    val canUndo: Boolean get() = history.isNotEmpty()
    val placedCount: Int get() = placements.size
}
