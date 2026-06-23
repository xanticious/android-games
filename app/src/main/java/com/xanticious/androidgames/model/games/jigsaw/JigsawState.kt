package com.xanticious.androidgames.model.games.jigsaw

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * Immutable Jigsaw puzzle state (`design/puzzle-games/jigsaw`).
 *
 * DEVIATION FROM DESIGN DOC: The design specified 50 bundled Unsplash photos. This
 * repo ships no image assets and the sandbox cannot download them, so all Jigsaw
 * "pictures" are procedurally drawn via Compose Canvas using ui/theme/Color.kt
 * tokens. Eight distinct scenic/geometric motifs are keyed by [imageId] (0–7).
 * The jigsaw mechanic (piece bank, snap-only-correct placement, board crop rendering)
 * is otherwise fully faithful to the design.
 */

/** One piece in the jigsaw; its correct board position is fixed at creation. */
data class JigsawPiece(
    val id: Int,
    val correctRow: Int,
    val correctCol: Int
)

/**
 * Full game state: the image being assembled, grid dimensions, all pieces, the
 * scrollable bank and which board cells have been filled.
 */
data class JigsawState(
    val imageId: Int,
    val rows: Int,
    val cols: Int,
    val pieces: List<JigsawPiece>,
    /** Piece ids remaining in the scrollable bank, in display order. */
    val bankOrder: List<Int>,
    /** Cells filled so far: GridPos(row,col) → piece id. */
    val placedPieces: Map<GridPos, Int> = emptyMap()
) {
    val placedCount: Int get() = placedPieces.size
    val totalPieces: Int get() = rows * cols
}
