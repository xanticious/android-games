package com.xanticious.androidgames.games.sudokucolors

import com.xanticious.androidgames.controller.games.sudokucolors.SudokuColorsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuColorsControllerTest {

    private val controller = SudokuColorsController()

    // ── Solution validity ──────────────────────────────────────────────────

    @Test
    fun solution_hasNoRowDuplicates() {
        val state = controller.newGame(GameDifficulty.EASY, Random(42))
        for (row in 0 until 9) {
            val rowValues = (0 until 9).map { col -> state.solution[row * 9 + col] }
            assertEquals("Row $row missing values", (1..9).toSet(), rowValues.toSet())
        }
    }

    @Test
    fun solution_hasNoColDuplicates() {
        val state = controller.newGame(GameDifficulty.EASY, Random(42))
        for (col in 0 until 9) {
            val colValues = (0 until 9).map { row -> state.solution[row * 9 + col] }
            assertEquals("Col $col missing values", (1..9).toSet(), colValues.toSet())
        }
    }

    @Test
    fun solution_hasNoBoxDuplicates() {
        val state = controller.newGame(GameDifficulty.EASY, Random(42))
        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                val boxValues = mutableListOf<Int>()
                for (r in 0 until 3)
                    for (c in 0 until 3)
                        boxValues.add(state.solution[(boxRow * 3 + r) * 9 + (boxCol * 3 + c)])
                assertEquals(
                    "Box ($boxRow,$boxCol) missing values",
                    (1..9).toSet(),
                    boxValues.toSet()
                )
            }
        }
    }

    // ── Givens are immutable ───────────────────────────────────────────────

    @Test
    fun setValue_given_isIgnored() {
        val state = controller.newGame(GameDifficulty.EASY, Random(7))
        val givenIdx = state.cells.indexOfFirst { it.isGiven }
        assertTrue("Expected at least one given", givenIdx >= 0)
        val pos = GridPos(givenIdx / 9, givenIdx % 9)
        val originalValue = state.cells[givenIdx].value
        val differentColor = (1..9).first { it != originalValue }
        val after = controller.setValue(state, pos, differentColor)
        assertEquals(originalValue, after.cells[givenIdx].value)
    }

    // ── Conflict detection ─────────────────────────────────────────────────

    @Test
    fun setValue_conflictingValue_isFlagged() {
        val state = controller.newGame(GameDifficulty.EASY, Random(1))
        // Find the first row that has at least two non-given cells
        var rowWithTwoEmpty = -1
        var col1 = -1
        var col2 = -1
        outer@ for (row in 0 until 9) {
            val empties = (0 until 9).filter { !state.cells[row * 9 + it].isGiven }
            if (empties.size >= 2) {
                rowWithTwoEmpty = row
                col1 = empties[0]
                col2 = empties[1]
                break@outer
            }
        }
        assertTrue("Expected a row with 2+ empty cells", rowWithTwoEmpty >= 0)

        // Use the solution value of col1 as the conflicting value
        val conflictValue = state.solution[rowWithTwoEmpty * 9 + col1]
        val s1 = controller.setValue(state, GridPos(rowWithTwoEmpty, col1), conflictValue)
        val s2 = controller.setValue(s1, GridPos(rowWithTwoEmpty, col2), conflictValue)

        assertTrue(s2.cells[rowWithTwoEmpty * 9 + col1].isConflict)
        assertTrue(s2.cells[rowWithTwoEmpty * 9 + col2].isConflict)
    }

    @Test
    fun setValue_nonConflicting_isNotFlagged() {
        val state = controller.newGame(GameDifficulty.MEDIUM, Random(5))
        val emptyIdx = state.cells.indexOfFirst { !it.isGiven }
        assertTrue("Expected at least one empty cell", emptyIdx >= 0)
        val pos = GridPos(emptyIdx / 9, emptyIdx % 9)
        // Use the solution value — by definition it conflicts with nothing
        val correctValue = state.solution[emptyIdx]
        val after = controller.setValue(state, pos, correctValue)
        assertFalse(after.cells[emptyIdx].isConflict)
    }

    // ── Pencil marks ───────────────────────────────────────────────────────

    @Test
    fun togglePencilMark_addsAndRemovesMark() {
        val state = controller.newGame(GameDifficulty.EASY, Random(3))
        val emptyIdx = state.cells.indexOfFirst { !it.isGiven }
        val pos = GridPos(emptyIdx / 9, emptyIdx % 9)
        val s1 = controller.togglePencilMark(state, pos, 3)
        assertTrue(3 in s1.cells[emptyIdx].pencilMarks)
        val s2 = controller.togglePencilMark(s1, pos, 3)
        assertFalse(3 in s2.cells[emptyIdx].pencilMarks)
    }

    @Test
    fun togglePencilMark_given_isIgnored() {
        val state = controller.newGame(GameDifficulty.EASY, Random(9))
        val givenIdx = state.cells.indexOfFirst { it.isGiven }
        val pos = GridPos(givenIdx / 9, givenIdx % 9)
        val after = controller.togglePencilMark(state, pos, 1)
        assertTrue(after.cells[givenIdx].pencilMarks.isEmpty())
    }

    @Test
    fun setValue_clearsPencilMarks() {
        val state = controller.newGame(GameDifficulty.EASY, Random(11))
        val emptyIdx = state.cells.indexOfFirst { !it.isGiven }
        val pos = GridPos(emptyIdx / 9, emptyIdx % 9)
        val withMark = controller.togglePencilMark(state, pos, 2)
        val withValue = controller.setValue(withMark, pos, 5)
        assertTrue(withValue.cells[emptyIdx].pencilMarks.isEmpty())
    }

    // ── Undo ───────────────────────────────────────────────────────────────

    @Test
    fun undo_revertsLastChange() {
        val state = controller.newGame(GameDifficulty.EASY, Random(13))
        val emptyIdx = state.cells.indexOfFirst { !it.isGiven }
        val pos = GridPos(emptyIdx / 9, emptyIdx % 9)
        val after = controller.setValue(state, pos, 4)
        assertEquals(4, after.cells[emptyIdx].value)
        val undone = controller.undo(after)
        assertEquals(0, undone.cells[emptyIdx].value)
    }

    @Test
    fun undo_onEmptyHistory_returnsUnchanged() {
        val state = controller.newGame(GameDifficulty.EASY, Random(17))
        assertFalse(state.canUndo)
        val after = controller.undo(state)
        assertEquals(state.cells, after.cells)
    }

    // ── isSolved ───────────────────────────────────────────────────────────

    @Test
    fun isSolved_emptyBoard_returnsFalse() {
        val state = controller.newGame(GameDifficulty.EASY, Random(19))
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun isSolved_partialBoard_returnsFalse() {
        val state = controller.newGame(GameDifficulty.MEDIUM, Random(23))
        val emptyIdx = state.cells.indexOfFirst { !it.isGiven }
        val pos = GridPos(emptyIdx / 9, emptyIdx % 9)
        val partial = controller.setValue(state, pos, state.solution[emptyIdx])
        assertFalse(controller.isSolved(partial))
    }

    @Test
    fun isSolved_fullySolvedPuzzle_returnsTrue() {
        var state = controller.newGame(GameDifficulty.EASY, Random(29))
        state.cells.forEachIndexed { i, cell ->
            if (!cell.isGiven) {
                val pos = GridPos(i / 9, i % 9)
                state = controller.setValue(state, pos, state.solution[i])
            }
        }
        assertTrue(controller.isSolved(state))
    }

    // ── Determinism ────────────────────────────────────────────────────────

    @Test
    fun newGame_sameSeed_producesSamePuzzle() {
        val s1 = controller.newGame(GameDifficulty.MEDIUM, Random(37))
        val s2 = controller.newGame(GameDifficulty.MEDIUM, Random(37))
        assertEquals(s1.solution, s2.solution)
        assertEquals(s1.cells.map { it.value }, s2.cells.map { it.value })
    }

    @Test
    fun newGame_differentSeeds_produceDifferentPuzzles() {
        val s1 = controller.newGame(GameDifficulty.MEDIUM, Random(41))
        val s2 = controller.newGame(GameDifficulty.MEDIUM, Random(43))
        assertNotEquals(s1.solution, s2.solution)
    }

    // ── Difficulty: given count ────────────────────────────────────────────

    @Test
    fun newGame_easy_hasMoreGivensThanHard() {
        val easy = controller.newGame(GameDifficulty.EASY, Random(47))
        val hard = controller.newGame(GameDifficulty.HARD, Random(47))
        val easyGivens = easy.cells.count { it.isGiven }
        val hardGivens = hard.cells.count { it.isGiven }
        assertTrue("Easy ($easyGivens) should have more givens than Hard ($hardGivens)", easyGivens > hardGivens)
    }

    // ── Peers ─────────────────────────────────────────────────────────────

    @Test
    fun peersOf_center_has20Peers() {
        val peers = controller.peersOf(GridPos(4, 4))
        assertEquals(20, peers.size)
    }

    @Test
    fun peersOf_doesNotIncludeSelf() {
        val pos = GridPos(3, 3)
        assertFalse(pos in controller.peersOf(pos))
    }
}
