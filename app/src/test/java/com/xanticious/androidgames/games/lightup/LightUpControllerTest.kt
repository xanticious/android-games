package com.xanticious.androidgames.games.lightup

import com.xanticious.androidgames.controller.games.lightup.LightUpController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.lightup.LightUpCell
import com.xanticious.androidgames.model.games.lightup.LightUpState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LightUpControllerTest {

    private val controller = LightUpController()

    // ---- computeLit -------------------------------------------------------

    @Test
    fun computeLit_bulbLightsOwnCell() {
        val state = rowBoard(5, bulbAt = 2)
        assertTrue(GridPos(0, 2) in controller.computeLit(state))
    }

    @Test
    fun computeLit_bulbLightsEntireRowSegment() {
        // Bulb at col 2, no walls — should reach col 0.
        val state = rowBoard(5, bulbAt = 2)
        assertTrue(GridPos(0, 0) in controller.computeLit(state))
    }

    @Test
    fun computeLit_wallBlocksLightBeyondIt() {
        // Bulb at col 2, wall at col 3 — col 4 must stay dark.
        val cells = (0 until 5).map { LightUpCell(isWall = false) }.toMutableList()
        cells[2] = cells[2].copy(hasBulb = true)
        cells[3] = LightUpCell(isWall = true)
        val state = LightUpState(1, 5, cells)
        assertFalse(GridPos(0, 4) in controller.computeLit(state))
    }

    @Test
    fun computeLit_bulbLightsColumnUntilWall() {
        // 5-row single-column board; bulb at row 1, wall at row 3.
        val cells = (0 until 5).map { LightUpCell(isWall = false) }.toMutableList()
        cells[1] = cells[1].copy(hasBulb = true)
        cells[3] = LightUpCell(isWall = true)
        val state = LightUpState(5, 1, cells)
        // Row 0 (above bulb) should be lit.
        assertTrue(GridPos(0, 0) in controller.computeLit(state))
        // Row 4 (below wall) should be dark.
        assertFalse(GridPos(4, 0) in controller.computeLit(state))
    }

    @Test
    fun computeLit_noBulbs_noCellsLit() {
        val cells = (0 until 4).map { LightUpCell(isWall = false) }
        val state = LightUpState(1, 4, cells)
        assertTrue(controller.computeLit(state).isEmpty())
    }

    // ---- conflicts --------------------------------------------------------

    @Test
    fun conflicts_twoBulbsInSameRow_bothConflict() {
        // 1×3; bulbs at col 0 and col 2 see each other through col 1.
        val cells = listOf(
            LightUpCell(isWall = false, hasBulb = true),
            LightUpCell(isWall = false),
            LightUpCell(isWall = false, hasBulb = true)
        )
        val state = LightUpState(1, 3, cells)
        assertTrue(GridPos(0, 0) in controller.conflicts(state))
    }

    @Test
    fun conflicts_twoBulbsSeparatedByWall_noConflict() {
        // 1×3; wall at col 1 blocks line of sight.
        val cells = listOf(
            LightUpCell(isWall = false, hasBulb = true),
            LightUpCell(isWall = true),
            LightUpCell(isWall = false, hasBulb = true)
        )
        val state = LightUpState(1, 3, cells)
        assertTrue(controller.conflicts(state).isEmpty())
    }

    @Test
    fun conflicts_numberedWallSatisfied_noConflict() {
        // 1×2; wall(number=1) at col 1, bulb at col 0 → exactly 1 adjacent bulb.
        val cells = listOf(
            LightUpCell(isWall = false, hasBulb = true),
            LightUpCell(isWall = true, wallNumber = 1)
        )
        val state = LightUpState(1, 2, cells)
        assertFalse(GridPos(0, 1) in controller.conflicts(state))
    }

    @Test
    fun conflicts_numberedWallViolated_isConflict() {
        // Wall expects 1 bulb but has 0 adjacent bulbs.
        val cells = listOf(
            LightUpCell(isWall = false),
            LightUpCell(isWall = true, wallNumber = 1)
        )
        val state = LightUpState(1, 2, cells)
        assertTrue(GridPos(0, 1) in controller.conflicts(state))
    }

    @Test
    fun conflicts_unnumberedWall_neverConflicts() {
        // Unnumbered walls carry no constraint regardless of adjacent bulbs.
        val cells = listOf(
            LightUpCell(isWall = false, hasBulb = true),
            LightUpCell(isWall = true, wallNumber = null)
        )
        val state = LightUpState(1, 2, cells)
        assertFalse(GridPos(0, 1) in controller.conflicts(state))
    }

    // ---- isSolved ---------------------------------------------------------

    @Test
    fun isSolved_emptyBoard_false() {
        val cells = listOf(LightUpCell(isWall = false))
        assertFalse(controller.isSolved(LightUpState(1, 1, cells)))
    }

    @Test
    fun isSolved_singleBulbNoWalls_true() {
        val cells = listOf(LightUpCell(isWall = false, hasBulb = true))
        assertTrue(controller.isSolved(LightUpState(1, 1, cells)))
    }

    @Test
    fun isSolved_conflictingBulbs_false() {
        // Two bulbs see each other — conflicts prevent solving.
        val cells = listOf(
            LightUpCell(isWall = false, hasBulb = true),
            LightUpCell(isWall = false, hasBulb = true)
        )
        assertFalse(controller.isSolved(LightUpState(1, 2, cells)))
    }

    @Test
    fun isSolved_generatedSolutionPlaced_true() {
        // Generate a puzzle, apply every solution bulb, expect solved.
        var state = controller.newGame(GameDifficulty.EASY, Random(42))
        for (idx in state.solutionBulbs) {
            state = controller.toggleBulb(state, GridPos(idx / state.cols, idx % state.cols))
        }
        assertTrue(controller.isSolved(state))
    }

    @Test
    fun newGame_initialState_isNotSolved() {
        val state = controller.newGame(GameDifficulty.EASY, Random(7))
        assertFalse(controller.isSolved(state))
    }

    // ---- toggleBulb -------------------------------------------------------

    @Test
    fun toggleBulb_placesBulbOnEmptyCell() {
        val cells = listOf(LightUpCell(isWall = false))
        val state = LightUpState(1, 1, cells)
        val next = controller.toggleBulb(state, GridPos(0, 0))
        assertTrue(next.cellAt(GridPos(0, 0)).hasBulb)
    }

    @Test
    fun toggleBulb_removesBulbOnSecondTap() {
        val cells = listOf(LightUpCell(isWall = false, hasBulb = true))
        val state = LightUpState(1, 1, cells)
        val next = controller.toggleBulb(state, GridPos(0, 0))
        assertFalse(next.cellAt(GridPos(0, 0)).hasBulb)
    }

    @Test
    fun toggleBulb_clearsMarkWhenPlacingBulb() {
        val cells = listOf(LightUpCell(isWall = false, hasMark = true))
        val state = LightUpState(1, 1, cells)
        val next = controller.toggleBulb(state, GridPos(0, 0))
        assertFalse(next.cellAt(GridPos(0, 0)).hasMark)
    }

    @Test
    fun toggleBulb_wallCell_noOp() {
        val cells = listOf(LightUpCell(isWall = true))
        val state = LightUpState(1, 1, cells)
        val next = controller.toggleBulb(state, GridPos(0, 0))
        assertEquals(state, next)
    }

    // ---- toggleMark -------------------------------------------------------

    @Test
    fun toggleMark_placesMark() {
        val cells = listOf(LightUpCell(isWall = false))
        val state = LightUpState(1, 1, cells)
        val next = controller.toggleMark(state, GridPos(0, 0))
        assertTrue(next.cellAt(GridPos(0, 0)).hasMark)
    }

    @Test
    fun toggleMark_removesMark() {
        val cells = listOf(LightUpCell(isWall = false, hasMark = true))
        val state = LightUpState(1, 1, cells)
        val next = controller.toggleMark(state, GridPos(0, 0))
        assertFalse(next.cellAt(GridPos(0, 0)).hasMark)
    }

    @Test
    fun toggleMark_onBulbCell_replacesBulbWithMark() {
        val cells = listOf(LightUpCell(isWall = false, hasBulb = true))
        val state = LightUpState(1, 1, cells)
        val next = controller.toggleMark(state, GridPos(0, 0))
        assertFalse(next.cellAt(GridPos(0, 0)).hasBulb)
        assertTrue(next.cellAt(GridPos(0, 0)).hasMark)
    }

    // ---- undo -------------------------------------------------------------

    @Test
    fun undo_restoresPreviousState() {
        val cells = listOf(LightUpCell(isWall = false))
        val initial = LightUpState(1, 1, cells)
        val withBulb = controller.toggleBulb(initial, GridPos(0, 0))
        val undone = controller.undo(withBulb)
        assertEquals(initial.cells, undone.cells)
    }

    @Test
    fun undo_noHistory_returnsUnchanged() {
        val cells = listOf(LightUpCell(isWall = false))
        val state = LightUpState(1, 1, cells)
        assertEquals(state, controller.undo(state))
    }

    // ---- hint -------------------------------------------------------------

    @Test
    fun hint_placesSolutionBulb() {
        val state = controller.newGame(GameDifficulty.EASY, Random(13))
        val hinted = controller.hint(state)
        val newBulbs = (0 until hinted.cells.size).count { hinted.cells[it].hasBulb }
        assertEquals(1, newBulbs)
    }

    // ---- helpers ----------------------------------------------------------

    /** Creates a 1-row board of [width] white cells with a bulb at column [bulbAt]. */
    private fun rowBoard(width: Int, bulbAt: Int): LightUpState {
        val cells = (0 until width).map { LightUpCell(isWall = false) }.toMutableList()
        cells[bulbAt] = cells[bulbAt].copy(hasBulb = true)
        return LightUpState(1, width, cells)
    }
}
