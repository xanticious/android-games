package com.xanticious.androidgames.games.pipes

import com.xanticious.androidgames.controller.games.pipes.PipesController
import com.xanticious.androidgames.model.games.pipes.PipeType
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PipesControllerTest {

    private val controller = PipesController()

    // ── Rotation helpers ──────────────────────────────────────────────────────

    @Test
    fun rotateCW_up_oneStep_givesRight() {
        assertEquals(Direction.RIGHT, controller.rotateCW(Direction.UP, 1))
    }

    @Test
    fun rotateCW_right_oneStep_givesDown() {
        assertEquals(Direction.DOWN, controller.rotateCW(Direction.RIGHT, 1))
    }

    @Test
    fun rotateCW_fourSteps_returnsOriginal() {
        assertEquals(Direction.UP, controller.rotateCW(Direction.UP, 4))
    }

    // ── Rotating a piece 4 times returns to original ─────────────────────────

    @Test
    fun rotate4x_endPiece_returnsOriginalRotation() {
        val state = controller.generateSolution(5, Random(1))
        // Find an END cell
        val pos = (0 until 5).flatMap { r -> (0 until 5).map { c -> GridPos(r, c) } }
            .first { p -> state.cells[p.row * 5 + p.col].type == PipeType.END }
        var s = state
        repeat(4) { s = controller.rotate(s, pos) }
        assertEquals(state.rotations[pos.row * 5 + pos.col], s.rotations[pos.row * 5 + pos.col])
    }

    @Test
    fun rotate4x_elbowPiece_returnsOriginalRotation() {
        val state = controller.generateSolution(5, Random(2))
        val pos = (0 until 5).flatMap { r -> (0 until 5).map { c -> GridPos(r, c) } }
            .firstOrNull { p -> state.cells[p.row * 5 + p.col].type == PipeType.ELBOW }
            ?: return // no elbow in this seed; skip
        var s = state
        repeat(4) { s = controller.rotate(s, pos) }
        assertEquals(state.rotations[pos.row * 5 + pos.col], s.rotations[pos.row * 5 + pos.col])
    }

    // ── Connector matching for adjacent cells ─────────────────────────────────

    @Test
    fun connectorsAt_lineHorizontal_hasLeftAndRight() {
        val state = controller.generateSolution(5, Random(3))
        // For LINE cells at rotation=1 or 3, connectors are LEFT+RIGHT
        val pos = (0 until 5).flatMap { r -> (0 until 5).map { c -> GridPos(r, c) } }
            .firstOrNull { p ->
                val idx = p.row * 5 + p.col
                state.cells[idx].type == PipeType.LINE &&
                    (state.rotations[idx] == 1 || state.rotations[idx] == 3)
            } ?: return
        val dirs = controller.connectorsAt(state, pos)
        assertTrue(Direction.LEFT in dirs && Direction.RIGHT in dirs)
    }

    @Test
    fun connectedCells_solvedState_includesAllCells() {
        val solution = controller.generateSolution(5, Random(10))
        val connected = controller.connectedCells(solution)
        assertEquals(25, connected.size)
    }

    @Test
    fun connectedCells_sourceAlwaysConnected() {
        val state = controller.newGame(5, Random(7))
        val connected = controller.connectedCells(state)
        assertTrue(state.sourcePos in connected)
    }

    // ── Generated (unrotated) solution is solved ──────────────────────────────

    @Test
    fun generateSolution_5x5_isSolved() {
        val solution = controller.generateSolution(5, Random(42))
        assertTrue(controller.isSolved(solution))
    }

    @Test
    fun generateSolution_8x8_isSolved() {
        val solution = controller.generateSolution(8, Random(99))
        assertTrue(controller.isSolved(solution))
    }

    @Test
    fun generateSolution_allCellsHaveAtLeastOneConnector() {
        val solution = controller.generateSolution(5, Random(5))
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val dirs = controller.connectorsAt(solution, GridPos(r, c))
                assertTrue("cell ($r,$c) has no connectors", dirs.isNotEmpty())
            }
        }
    }

    // ── A known wrong rotation is not solved ──────────────────────────────────

    @Test
    fun rotate_anyCell_makesNotSolved() {
        val solution = controller.generateSolution(5, Random(42))
        // Rotating any non-CROSS cell by 1 should break the solution
        val pos = (0 until 5).flatMap { r -> (0 until 5).map { c -> GridPos(r, c) } }
            .first { p -> solution.cells[p.row * 5 + p.col].type != PipeType.CROSS }
        val broken = controller.rotate(solution, pos)
        assertFalse(controller.isSolved(broken))
    }

    @Test
    fun isSolved_shuffledPuzzle_isFalse() {
        // newGame applies random rotations; it's astronomically unlikely to be solved
        val puzzle = controller.newGame(8, Random(123))
        assertFalse(controller.isSolved(puzzle))
    }

    // ── Same seed → same puzzle ───────────────────────────────────────────────

    @Test
    fun newGame_sameSeed_sameRotations() {
        val a = controller.newGame(5, Random(77))
        val b = controller.newGame(5, Random(77))
        assertEquals(a.rotations, b.rotations)
    }

    @Test
    fun newGame_sameSeed_sameCellTypes() {
        val a = controller.newGame(5, Random(55))
        val b = controller.newGame(5, Random(55))
        assertEquals(a.cells, b.cells)
    }

    @Test
    fun newGame_differentSeeds_differentRotations() {
        val a = controller.newGame(5, Random(1))
        val b = controller.newGame(5, Random(2))
        assertNotEquals(a.rotations, b.rotations)
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    @Test
    fun rotate_thenUndo_restoresRotation() {
        val state = controller.generateSolution(5, Random(42))
        val pos = GridPos(0, 0)
        val original = state.rotations[0]
        val rotated = controller.rotate(state, pos)
        val undone = controller.undo(rotated)
        assertEquals(original, undone.rotations[0])
    }

    @Test
    fun undo_emptyHistory_returnsSameState() {
        val state = controller.generateSolution(5, Random(42))
        val undone = controller.undo(state)
        assertEquals(state.rotations, undone.rotations)
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    fun reset_afterRotations_restoresInitialRotations() {
        val puzzle = controller.newGame(5, Random(33))
        var s = puzzle
        repeat(10) { s = controller.rotate(s, GridPos(2, 2)) }
        val resetState = controller.reset(s)
        assertEquals(puzzle.initialRotations, resetState.rotations)
    }

    // ── Connectivity ──────────────────────────────────────────────────────────

    @Test
    fun isSolved_afterRestoringAllSolutionRotations_isTrue() {
        val solution = controller.generateSolution(5, Random(11))
        // Shuffle then manually restore to solution rotations
        val puzzle = controller.newGame(5, Random(11))
        // Re-generate solution with same seed to get correct rotations
        val restored = puzzle.copy(rotations = solution.rotations)
        assertTrue(controller.isSolved(restored))
    }
}
