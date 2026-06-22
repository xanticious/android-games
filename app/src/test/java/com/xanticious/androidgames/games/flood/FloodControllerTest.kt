package com.xanticious.androidgames.games.flood

import com.xanticious.androidgames.controller.games.flood.FloodController
import com.xanticious.androidgames.model.games.flood.FloodState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FloodControllerTest {

    private val controller = FloodController()

    // ── Solved check ─────────────────────────────────────────────────────────

    @Test
    fun isSolved_uniformBoard_returnsTrue() {
        val state = FloodState(size = 3, colorCount = 2, grid = List(9) { 0 })
        assertTrue(controller.isSolved(state))
    }

    @Test
    fun isSolved_mixedBoard_returnsFalse() {
        val grid = listOf(0, 1, 0, 0, 0, 0, 0, 0, 0)
        val state = FloodState(size = 3, colorCount = 2, grid = grid)
        assertFalse(controller.isSolved(state))
    }

    // ── Fresh board from newGame ──────────────────────────────────────────────

    @Test
    fun newGame_freshBoard_isNotSolved() {
        val state = controller.newGame(size = 10, colorCount = 6, random = Random(42))
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun newGame_sameSeed_producesIdenticalBoard() {
        val s1 = controller.newGame(size = 10, colorCount = 4, random = Random(99))
        val s2 = controller.newGame(size = 10, colorCount = 4, random = Random(99))
        assertEquals(s1.grid, s2.grid)
    }

    @Test
    fun newGame_differentSeeds_produceDifferentBoards() {
        val s1 = controller.newGame(size = 10, colorCount = 4, random = Random(1))
        val s2 = controller.newGame(size = 10, colorCount = 4, random = Random(2))
        assertFalse(s1.grid == s2.grid)
    }

    @Test
    fun newGame_gridContainsOnlyValidColorIndices() {
        val colorCount = 5
        val state = controller.newGame(size = 10, colorCount = colorCount, random = Random(7))
        assertTrue(state.grid.all { it in 0 until colorCount })
    }

    // ── applyColor — region growth ────────────────────────────────────────────

    @Test
    fun applyColor_growsRegion_whenAdjacentCellsMatch() {
        // Board: columns of colors 0 | 1 | 2 (4×4)
        val grid = listOf(
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2
        )
        val state = FloodState(size = 4, colorCount = 3, grid = grid)
        assertEquals(4, controller.regionSize(state))  // left column

        val state2 = controller.applyColor(state, 1)
        assertEquals(8, controller.regionSize(state2))  // first two columns absorbed
    }

    @Test
    fun applyColor_cascade_absorbsTransitivelyConnectedCells() {
        // Full board becomes solved in two moves via cascading absorptions
        val grid = listOf(
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2
        )
        val state = FloodState(size = 4, colorCount = 3, grid = grid)
        val state2 = controller.applyColor(state, 1)
        val state3 = controller.applyColor(state2, 2)

        assertTrue(controller.isSolved(state3))
    }

    @Test
    fun applyColor_incrementsMoveCount() {
        val grid = listOf(0, 1, 0, 0, 0, 0, 0, 0, 0)
        val state = FloodState(size = 3, colorCount = 2, grid = grid)
        val state2 = controller.applyColor(state, 1)
        assertEquals(1, state2.moves)
    }

    @Test
    fun applyColor_sameColorAsRegion_isNoOp() {
        val grid = listOf(0, 1, 0, 0, 0, 0, 0, 0, 0)
        val state = FloodState(size = 3, colorCount = 2, grid = grid)
        val state2 = controller.applyColor(state, 0)  // already color 0
        assertEquals(state.grid, state2.grid)
        assertEquals(0, state2.moves)
    }

    @Test
    fun applyColor_solvesBoard_whenLastCellAbsorbed() {
        // 3×3: all color 0 except (2,2) is color 1
        val grid = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1)
        val state = FloodState(size = 3, colorCount = 2, grid = grid)
        val state2 = controller.applyColor(state, 1)
        assertTrue(controller.isSolved(state2))
    }

    // ── undo ─────────────────────────────────────────────────────────────────

    @Test
    fun undo_restoresPreviousGrid() {
        val grid = listOf(
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2
        )
        val state = FloodState(size = 4, colorCount = 3, grid = grid)
        val moved = controller.applyColor(state, 1)
        val undone = controller.undo(moved)
        assertEquals(grid, undone.grid)
    }

    @Test
    fun undo_decrementsMoveCount() {
        val grid = listOf(0, 1, 0, 0, 0, 0, 0, 0, 0)
        val state = FloodState(size = 3, colorCount = 2, grid = grid)
        val moved = controller.applyColor(state, 1)
        val undone = controller.undo(moved)
        assertEquals(0, undone.moves)
    }

    @Test
    fun undo_onEmptyHistory_returnsUnchangedState() {
        val state = FloodState(size = 3, colorCount = 2, grid = List(9) { 0 })
        val result = controller.undo(state)
        assertEquals(state, result)
    }

    @Test
    fun canUndo_isFalse_onFreshState() {
        val state = FloodState(size = 3, colorCount = 2, grid = List(9) { 0 })
        assertFalse(state.canUndo)
    }

    @Test
    fun canUndo_isTrue_afterOneMove() {
        val grid = listOf(0, 1, 0, 0, 0, 0, 0, 0, 0)
        val state = FloodState(size = 3, colorCount = 2, grid = grid)
        val moved = controller.applyColor(state, 1)
        assertTrue(moved.canUndo)
    }

    // ── regionSize ────────────────────────────────────────────────────────────

    @Test
    fun regionSize_uniformBoard_equalsAllCells() {
        val state = FloodState(size = 3, colorCount = 2, grid = List(9) { 0 })
        assertEquals(9, controller.regionSize(state))
    }

    @Test
    fun regionSize_isolatedTopLeft_equalsOne() {
        // (0,0) = color 0; all others = color 1
        val grid = listOf(0, 1, 1, 1, 1, 1, 1, 1, 1)
        val state = FloodState(size = 3, colorCount = 2, grid = grid)
        assertEquals(1, controller.regionSize(state))
    }

    // ── minMovesFor ───────────────────────────────────────────────────────────

    @Test
    fun minMovesFor_uniformBoard_returnsZero() {
        val grid = List(9) { 0 }
        assertEquals(0, controller.minMovesFor(grid, 3, 2))
    }

    @Test
    fun minMovesFor_twoColumnBoard_returnsTwo() {
        // 4×4 board with 3 color columns → min = 2
        val grid = listOf(
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2,
            0, 1, 2, 2
        )
        assertEquals(2, controller.minMovesFor(grid, 4, 3))
    }

    @Test
    fun minMovesFor_singleNonRegionCell_returnsOne() {
        // All color 0 except (2,2) = color 1
        val grid = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1)
        assertEquals(1, controller.minMovesFor(grid, 3, 2))
    }

    @Test
    fun par_equalsMinMovesPlusHandicap() {
        val state = FloodState(size = 3, colorCount = 2, grid = List(9) { 0 }, minMoves = 5, handicap = 4)
        assertEquals(9, state.par)
    }
}
