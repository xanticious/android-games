package com.xanticious.androidgames.games.nonogram

import com.xanticious.androidgames.controller.games.nonogram.NonogramController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.nonogram.CellState
import com.xanticious.androidgames.model.games.nonogram.NonogramState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NonogramControllerTest {

    private val ctrl = NonogramController()

    // ── lineClue ──────────────────────────────────────────────────────────────

    @Test
    fun lineClue_twoRunsWithGap_returnsCorrectLengths() {
        val result = ctrl.lineClue(listOf(true, true, false, true))
        assertEquals(listOf(2, 1), result)
    }

    @Test
    fun lineClue_allFalse_returnsEmptyList() {
        val result = ctrl.lineClue(listOf(false, false, false))
        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun lineClue_allTrue_returnsSingleRun() {
        val result = ctrl.lineClue(listOf(true, true, true))
        assertEquals(listOf(3), result)
    }

    @Test
    fun lineClue_singleTrue_returnsOne() {
        val result = ctrl.lineClue(listOf(false, true, false))
        assertEquals(listOf(1), result)
    }

    @Test
    fun lineClue_trailingRun_counted() {
        val result = ctrl.lineClue(listOf(false, true, true))
        assertEquals(listOf(2), result)
    }

    @Test
    fun lineClue_leadingRun_counted() {
        val result = ctrl.lineClue(listOf(true, false, false))
        assertEquals(listOf(1), result)
    }

    // ── newGame ───────────────────────────────────────────────────────────────

    @Test
    fun newGame_sameSeed_producesSamePuzzle() {
        val a = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(42))
        val b = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(42))
        assertEquals(a.solution, b.solution)
    }

    @Test
    fun newGame_differentSeed_producesDistinctPuzzle() {
        val a = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(1))
        val b = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(2))
        assertNotEquals(a.solution, b.solution)
    }

    @Test
    fun newGame_solutionSatisfiesOwnClues() {
        val state = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(7))
        val n = state.size
        for (r in 0 until n) {
            val filled = (0 until n).map { c -> state.solution[r * n + c] }
            assertEquals(state.rowClues[r], ctrl.lineClue(filled))
        }
        for (c in 0 until n) {
            val filled = (0 until n).map { r -> state.solution[r * n + c] }
            assertEquals(state.colClues[c], ctrl.lineClue(filled))
        }
    }

    @Test
    fun newGame_cellsStartEmpty() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        assertTrue(state.cells.all { it == CellState.EMPTY })
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @Test
    fun toggle_emptyCell_becomesFilled() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val next = ctrl.toggle(state, GridPos(0, 0))
        assertEquals(CellState.FILLED, next.cells[0])
    }

    @Test
    fun toggle_filledCell_becomesEmpty() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val filled = ctrl.toggle(state, GridPos(0, 0))
        val cleared = ctrl.toggle(filled, GridPos(0, 0))
        assertEquals(CellState.EMPTY, cleared.cells[0])
    }

    @Test
    fun toggle_markedCell_becomesFilled() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val marked = ctrl.toggleMark(state, GridPos(1, 1))
        val toggled = ctrl.toggle(marked, GridPos(1, 1))
        assertEquals(CellState.FILLED, toggled.cells[1 * 5 + 1])
    }

    @Test
    fun toggle_outOfBounds_returnsUnchanged() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val next = ctrl.toggle(state, GridPos(10, 10))
        assertEquals(state, next)
    }

    @Test
    fun toggle_savesHistoryEntry() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val next = ctrl.toggle(state, GridPos(0, 0))
        assertEquals(1, next.history.size)
    }

    // ── toggleMark ────────────────────────────────────────────────────────────

    @Test
    fun toggleMark_emptyCell_becomesMarked() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val next = ctrl.toggleMark(state, GridPos(2, 2))
        assertEquals(CellState.MARKED, next.cells[2 * 5 + 2])
    }

    @Test
    fun toggleMark_markedCell_becomesEmpty() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val marked = ctrl.toggleMark(state, GridPos(0, 1))
        val cleared = ctrl.toggleMark(marked, GridPos(0, 1))
        assertEquals(CellState.EMPTY, cleared.cells[1])
    }

    // ── isSolved ─────────────────────────────────────────────────────────────

    @Test
    fun isSolved_allCellsMatchSolution_returnsTrue() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(3))
        val solved = state.copy(
            cells = state.solution.map { if (it) CellState.FILLED else CellState.EMPTY }
        )
        assertTrue(ctrl.isSolved(solved))
    }

    @Test
    fun isSolved_emptyCells_returnsFalseWhenSolutionHasFills() {
        val state = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(3))
        val hasFills = state.solution.any { it }
        if (hasFills) assertFalse(ctrl.isSolved(state))
    }

    @Test
    fun isSolved_partialFill_returnsFalse() {
        val state = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(5))
        val partial = ctrl.toggle(state, GridPos(0, 0))
        assertFalse(ctrl.isSolved(partial))
    }

    @Test
    fun isSolved_marksOnEmptySolutionCells_returnsFalse() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(3))
        // Fill the solution cells but also add an extra filled cell in a solution-empty spot
        val solutionFilled = state.solution.map { if (it) CellState.FILLED else CellState.EMPTY }
        val extraFillIdx = state.solution.indexOfFirst { !it }
        if (extraFillIdx >= 0) {
            val mismatch = solutionFilled.toMutableList().also { it[extraFillIdx] = CellState.FILLED }
            val wrong = state.copy(cells = mismatch)
            assertFalse(ctrl.isSolved(wrong))
        }
    }

    // ── undo ──────────────────────────────────────────────────────────────────

    @Test
    fun undo_afterToggle_restoresPreviousCells() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val toggled = ctrl.toggle(state, GridPos(0, 0))
        val undone = ctrl.undo(toggled)
        assertEquals(state.cells, undone.cells)
    }

    @Test
    fun undo_emptyHistory_returnsUnchanged() {
        val state = ctrl.newGame(5, GameDifficulty.EASY, Random(1))
        val undone = ctrl.undo(state)
        assertEquals(state, undone)
    }

    // ── isRowSatisfied / isColSatisfied ──────────────────────────────────────

    @Test
    fun isRowSatisfied_solutionCellsFilled_returnsTrue() {
        val state = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(8))
        val solved = state.copy(
            cells = state.solution.map { if (it) CellState.FILLED else CellState.EMPTY }
        )
        for (r in 0 until 5) assertTrue(ctrl.isRowSatisfied(solved, r))
    }

    @Test
    fun isColSatisfied_solutionCellsFilled_returnsTrue() {
        val state = ctrl.newGame(5, GameDifficulty.MEDIUM, Random(8))
        val solved = state.copy(
            cells = state.solution.map { if (it) CellState.FILLED else CellState.EMPTY }
        )
        for (c in 0 until 5) assertTrue(ctrl.isColSatisfied(solved, c))
    }

    @Test
    fun isRowSatisfied_emptyCells_returnsTrueWhenClueIsEmpty() {
        val allFalse = NonogramState(
            size = 3,
            solution = List(9) { false },
            cells = List(9) { CellState.EMPTY },
            rowClues = List(3) { emptyList() },
            colClues = List(3) { emptyList() }
        )
        assertTrue(ctrl.isRowSatisfied(allFalse, 0))
    }
}
