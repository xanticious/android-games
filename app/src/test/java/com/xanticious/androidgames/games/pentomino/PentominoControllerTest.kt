package com.xanticious.androidgames.games.pentomino

import com.xanticious.androidgames.controller.games.pentomino.PentominoController
import com.xanticious.androidgames.model.games.pentomino.BoardSize
import com.xanticious.androidgames.model.games.pentomino.PentominoPiece
import com.xanticious.androidgames.model.games.pentomino.PentominoState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PentominoControllerTest {

    private val controller = PentominoController()

    // ---- shape integrity ----

    @Test
    fun eachPentomino_canonicalOrientation_hasExactlyFiveCells() {
        PentominoPiece.entries.forEach { piece ->
            val first = controller.orientations(piece, allowFlip = true).first()
            assertEquals("$piece must have 5 cells", 5, first.size)
        }
    }

    // ---- rotation identity ----

    @Test
    fun iPentomino_fourRotations_returnsToOriginalShape() {
        val start = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        var rotated = start
        repeat(4) { rotated = testRotate90(rotated) }
        assertEquals(start, rotated)
    }

    // ---- unique orientation counts ----

    @Test
    fun xPentomino_hasExactlyOneUniqueOrientation() {
        assertEquals(1, controller.orientations(PentominoPiece.X, allowFlip = true).size)
    }

    @Test
    fun iPentomino_withoutFlip_hasExactlyTwoOrientations() {
        assertEquals(2, controller.orientations(PentominoPiece.I, allowFlip = false).size)
    }

    // ---- canPlace ----

    @Test
    fun canPlace_validAnchor_returnsTrue() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        val orientation = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        assertTrue(controller.canPlace(state, orientation, GridPos(0, 0)))
    }

    @Test
    fun canPlace_anchorWouldExceedBoardWidth_returnsFalse() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        // I horizontal is 5 wide; anchoring at col 6 would place cells in cols 6–10
        val orientation = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        assertFalse(controller.canPlace(state, orientation, GridPos(0, 6)))
    }

    @Test
    fun canPlace_overlapsExistingPiece_returnsFalse() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        val iOrientation = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        val withI = controller.place(state, PentominoPiece.I, iOrientation, GridPos(0, 0))
        // T at (0,0) would overlap I
        val tOrientation = controller.orientations(PentominoPiece.T, allowFlip = true).first()
        assertFalse(controller.canPlace(withI, tOrientation, GridPos(0, 0)))
    }

    // ---- place / remove ----

    @Test
    fun place_pieceAppears_inCellMap() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        val orientation = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        val next = controller.place(state, PentominoPiece.I, orientation, GridPos(0, 0))
        assertTrue(GridPos(0, 0) in next.cellMap)
        assertEquals(PentominoPiece.I, next.cellMap[GridPos(0, 0)])
    }

    @Test
    fun remove_pieceDisappears_fromCellMap() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        val orientation = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        val withI = controller.place(state, PentominoPiece.I, orientation, GridPos(0, 0))
        val removed = controller.remove(withI, PentominoPiece.I)
        assertFalse(GridPos(0, 0) in removed.cellMap)
    }

    // ---- undo ----

    @Test
    fun undo_afterPlace_restoresPreviousPlacements() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        val orientation = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        val withI = controller.place(state, PentominoPiece.I, orientation, GridPos(0, 0))
        val undone = controller.undo(withI)
        assertTrue(undone.placements.isEmpty())
    }

    @Test
    fun undo_onEmptyHistory_returnsUnchangedState() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        assertEquals(state, controller.undo(state))
    }

    // ---- isSolved ----

    @Test
    fun isSolved_emptyBoard_returnsFalse() {
        assertFalse(controller.isSolved(controller.newGame(BoardSize.SIX_BY_TEN, true)))
    }

    @Test
    fun isSolved_partialBoard_returnsFalse() {
        val state = controller.newGame(BoardSize.SIX_BY_TEN, allowFlip = true)
        val orientation = controller.orientations(PentominoPiece.I, allowFlip = false).first()
        val withOne = controller.place(state, PentominoPiece.I, orientation, GridPos(0, 0))
        assertFalse(controller.isSolved(withOne))
    }

    @Test
    fun isSolved_allTwelvePiecesFillingAllCells_returnsTrue() {
        // Direct placement map: 12 groups of 5 consecutive cells in row-major order
        // covering all 60 cells of a 6×10 board (shape validity is enforced by
        // canPlace at game time; isSolved only checks full coverage).
        val solution = mapOf(
            PentominoPiece.F to rowCells(0, 0, 5),
            PentominoPiece.I to rowCells(0, 5, 5),
            PentominoPiece.L to rowCells(1, 0, 5),
            PentominoPiece.N to rowCells(1, 5, 5),
            PentominoPiece.P to rowCells(2, 0, 5),
            PentominoPiece.T to rowCells(2, 5, 5),
            PentominoPiece.U to rowCells(3, 0, 5),
            PentominoPiece.V to rowCells(3, 5, 5),
            PentominoPiece.W to rowCells(4, 0, 5),
            PentominoPiece.X to rowCells(4, 5, 5),
            PentominoPiece.Y to rowCells(5, 0, 5),
            PentominoPiece.Z to rowCells(5, 5, 5)
        )
        val state = PentominoState(boardSize = BoardSize.SIX_BY_TEN, placements = solution)
        assertTrue(controller.isSolved(state))
    }

    // ---- helpers ----

    /** Duplicate of the controller's rotate90 for black-box testing. */
    private fun testRotate90(cells: List<GridPos>): List<GridPos> {
        val rotated = cells.map { GridPos(it.col, -it.row) }
        val minRow = rotated.minOf { it.row }
        val minCol = rotated.minOf { it.col }
        return rotated.map { GridPos(it.row - minRow, it.col - minCol) }
            .sortedWith(compareBy({ it.row }, { it.col }))
    }

    private fun rowCells(row: Int, startCol: Int, count: Int): List<GridPos> =
        (startCol until startCol + count).map { c -> GridPos(row, c) }
}
