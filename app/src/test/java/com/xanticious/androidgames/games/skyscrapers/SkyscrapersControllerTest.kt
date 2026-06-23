package com.xanticious.androidgames.games.skyscrapers

import com.xanticious.androidgames.controller.games.skyscrapers.ClueState
import com.xanticious.androidgames.controller.games.skyscrapers.SkyscrapersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.skyscrapers.SkyscrapersState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SkyscrapersControllerTest {

    private val controller = SkyscrapersController()

    // -------------------------------------------------------------------------
    // visibleCount
    // -------------------------------------------------------------------------

    @Test
    fun visibleCount_ascending_allVisible() {
        assertEquals(4, controller.visibleCount(listOf(1, 2, 3, 4)))
    }

    @Test
    fun visibleCount_tallestFirst_oneVisible() {
        assertEquals(1, controller.visibleCount(listOf(4, 1, 2, 3)))
    }

    @Test
    fun visibleCount_mixed_threeVisible() {
        assertEquals(3, controller.visibleCount(listOf(2, 1, 3, 4)))
    }

    @Test
    fun visibleCount_singleElement_oneVisible() {
        assertEquals(1, controller.visibleCount(listOf(5)))
    }

    @Test
    fun visibleCount_descending_oneVisible() {
        assertEquals(1, controller.visibleCount(listOf(5, 4, 3, 2, 1)))
    }

    // -------------------------------------------------------------------------
    // newGame / Latin square
    // -------------------------------------------------------------------------

    @Test
    fun newGame_solutionIsLatinSquare() {
        val state = controller.newGame(5, GameDifficulty.MEDIUM, Random(42))
        val n = 5
        val expected = (1..n).toSet()
        for (r in 0 until n) {
            val row = (0 until n).map { c -> state.solution[r * n + c] }.toSet()
            assertEquals(expected, row)
        }
        for (c in 0 until n) {
            val col = (0 until n).map { r -> state.solution[r * n + c] }.toSet()
            assertEquals(expected, col)
        }
    }

    @Test
    fun newGame_solutionSatisfiesOwnClues() {
        val state = controller.newGame(4, GameDifficulty.MEDIUM, Random(7))
        val n = 4
        // Build a fully-filled state from the solution to check all clues.
        val solvedState = state.copy(grid = state.solution, givens = emptySet(), pencilMarks = emptyMap())
        val statuses = controller.clueStatuses(solvedState)
        assertTrue(statuses.top.all { it == ClueState.SATISFIED })
        assertTrue(statuses.bottom.all { it == ClueState.SATISFIED })
        assertTrue(statuses.left.all { it == ClueState.SATISFIED })
        assertTrue(statuses.right.all { it == ClueState.SATISFIED })
    }

    @Test
    fun newGame_sameSeed_samePuzzle() {
        val s1 = controller.newGame(5, GameDifficulty.MEDIUM, Random(99))
        val s2 = controller.newGame(5, GameDifficulty.MEDIUM, Random(99))
        assertEquals(s1.solution, s2.solution)
        assertEquals(s1.clues, s2.clues)
    }

    @Test
    fun newGame_differentSeeds_differentPuzzles() {
        val s1 = controller.newGame(5, GameDifficulty.MEDIUM, Random(1))
        val s2 = controller.newGame(5, GameDifficulty.MEDIUM, Random(2))
        assertNotEquals(s1.solution, s2.solution)
    }

    @Test
    fun newGame_easyDifficulty_hasGivens() {
        val state = controller.newGame(4, GameDifficulty.EASY, Random(5))
        assertTrue(state.givens.isNotEmpty())
    }

    @Test
    fun newGame_hardDifficulty_noGivens() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(5))
        assertTrue(state.givens.isEmpty())
    }

    @Test
    fun newGame_givensMatchSolution() {
        val state = controller.newGame(5, GameDifficulty.EASY, Random(3))
        for (idx in state.givens) {
            assertEquals(state.solution[idx], state.grid[idx])
        }
    }

    // -------------------------------------------------------------------------
    // setHeight / togglePencil / undo
    // -------------------------------------------------------------------------

    @Test
    fun setHeight_fillsEmptyCell() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(11))
        val pos = GridPos(0, 0)
        val newState = controller.setHeight(state, pos, 3)
        assertEquals(3, newState.grid[0])
    }

    @Test
    fun setHeight_clearsCellWithZero() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(11))
        val pos = GridPos(0, 0)
        val filled = controller.setHeight(state, pos, 3)
        val cleared = controller.setHeight(filled, pos, 0)
        assertEquals(0, cleared.grid[0])
    }

    @Test
    fun setHeight_doesNotModifyGiven() {
        val state = controller.newGame(4, GameDifficulty.EASY, Random(22))
        val givenIdx = state.givens.first()
        val givenPos = GridPos(givenIdx / 4, givenIdx % 4)
        val originalValue = state.grid[givenIdx]
        val newState = controller.setHeight(state, givenPos, if (originalValue == 1) 2 else 1)
        assertEquals(originalValue, newState.grid[givenIdx])
    }

    @Test
    fun setHeight_addsToHistory() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(11))
        val newState = controller.setHeight(state, GridPos(0, 0), 2)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun togglePencil_addsMark() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(33))
        val pos = GridPos(0, 0)
        val newState = controller.togglePencil(state, pos, 2)
        assertTrue(newState.pencilMarks[0]?.contains(2) == true)
    }

    @Test
    fun togglePencil_removesExistingMark() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(33))
        val pos = GridPos(0, 0)
        val withMark = controller.togglePencil(state, pos, 2)
        val withoutMark = controller.togglePencil(withMark, pos, 2)
        assertFalse(withoutMark.pencilMarks[0]?.contains(2) == true)
    }

    @Test
    fun undo_revertsLastChange() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(44))
        val after = controller.setHeight(state, GridPos(0, 0), 3)
        val undone = controller.undo(after)
        assertEquals(state.grid, undone.grid)
    }

    @Test
    fun undo_onEmptyHistory_returnsUnchanged() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(55))
        val same = controller.undo(state)
        assertEquals(state.grid, same.grid)
    }

    // -------------------------------------------------------------------------
    // conflictIndices
    // -------------------------------------------------------------------------

    @Test
    fun conflictIndices_noDuplicates_empty() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(66))
        val withValue = controller.setHeight(state, GridPos(0, 0), 1)
        val withDiff = controller.setHeight(withValue, GridPos(0, 1), 2)
        assertTrue(controller.conflictIndices(withDiff).isEmpty())
    }

    @Test
    fun conflictIndices_rowDuplicate_flagsBoth() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(77))
        val after1 = controller.setHeight(state, GridPos(0, 0), 1)
        val after2 = controller.setHeight(after1, GridPos(0, 1), 1)
        val conflicts = controller.conflictIndices(after2)
        assertTrue(conflicts.contains(0))
        assertTrue(conflicts.contains(1))
    }

    // -------------------------------------------------------------------------
    // isSolved
    // -------------------------------------------------------------------------

    @Test
    fun isSolved_fullCorrectGrid_returnsTrue() {
        val state = controller.newGame(4, GameDifficulty.MEDIUM, Random(88))
        val solvedState = state.copy(grid = state.solution, givens = emptySet(), pencilMarks = emptyMap())
        assertTrue(controller.isSolved(solvedState))
    }

    @Test
    fun isSolved_emptyGrid_returnsFalse() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(88))
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun isSolved_withRowDuplicate_returnsFalse() {
        val state = controller.newGame(4, GameDifficulty.MEDIUM, Random(88))
        val n = 4
        val brokenGrid = state.solution.toMutableList()
        // force a duplicate in row 0
        brokenGrid[0] = brokenGrid[1]
        val broken = state.copy(grid = brokenGrid, givens = emptySet())
        assertFalse(controller.isSolved(broken))
    }

    @Test
    fun isSolved_solutionWithWrongClue_returnsFalse() {
        val state = controller.newGame(4, GameDifficulty.MEDIUM, Random(88))
        // Manually corrupt one clue so the otherwise-valid grid fails clue check.
        val badClues = state.clues.copy(top = state.clues.top.toMutableList().also { it[0] = 0 })
        val wrongClueState = state.copy(
            grid = state.solution,
            clues = badClues,
            givens = emptySet(),
            pencilMarks = emptyMap()
        )
        assertFalse(controller.isSolved(wrongClueState))
    }

    // -------------------------------------------------------------------------
    // selectCell / remainingCells
    // -------------------------------------------------------------------------

    @Test
    fun selectCell_selectsCell() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(99))
        val pos = GridPos(1, 2)
        val selected = controller.selectCell(state, pos)
        assertEquals(state.indexOf(pos), selected.selectedIndex)
    }

    @Test
    fun selectCell_deselectsAlreadySelected() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(99))
        val pos = GridPos(1, 2)
        val selected = controller.selectCell(state, pos)
        val deselected = controller.selectCell(selected, pos)
        assertEquals(-1, deselected.selectedIndex)
    }

    @Test
    fun remainingCells_emptiesDecreaseOnFill() {
        val state = controller.newGame(4, GameDifficulty.HARD, Random(11))
        val before = controller.remainingCells(state)
        val filled = controller.setHeight(state, GridPos(0, 0), 1)
        assertEquals(before - 1, controller.remainingCells(filled))
    }
}
