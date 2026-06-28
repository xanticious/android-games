package com.xanticious.androidgames.games.logicgrid

import com.xanticious.androidgames.controller.games.logicgrid.LogicGridController
import com.xanticious.androidgames.controller.games.logicgrid.LogicGridSize
import com.xanticious.androidgames.model.games.logicgrid.CellMark
import com.xanticious.androidgames.model.games.logicgrid.LogicGridClue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LogicGridControllerTest {

    private val controller = LogicGridController()
    private val easySize = LogicGridSize(3, 4)
    private val medSize = LogicGridSize(4, 4)
    private val hardSize = LogicGridSize(5, 5)

    // -------------------------------------------------------------------------
    // Generation correctness
    // -------------------------------------------------------------------------

    @Test
    fun generatedSolution_isConsistentWithDirectClues() {
        val state = controller.newGame(easySize, Random(1))
        for (clue in state.clues.filterIsInstance<LogicGridClue.Direct>()) {
            assertEquals(
                "Direct clue entity mismatch",
                state.solution[clue.catA][clue.itemA],
                state.solution[clue.catB][clue.itemB]
            )
        }
    }

    @Test
    fun generatedSolution_isConsistentWithNegativeClues() {
        val state = controller.newGame(easySize, Random(2))
        for (clue in state.clues.filterIsInstance<LogicGridClue.Negative>()) {
            assertTrue(
                "Negative clue violated: entity should differ",
                state.solution[clue.catA][clue.itemA] != state.solution[clue.catB][clue.itemB]
            )
        }
    }

    @Test
    fun clueSet_admitsExactlyOneSolution_easy() {
        val state = controller.newGame(easySize, Random(3))
        assertEquals(1, controller.countSolutions(state.numCats, state.numItems, state.clues))
    }

    @Test
    fun clueSet_admitsExactlyOneSolution_medium() {
        val state = controller.newGame(medSize, Random(4))
        assertEquals(1, controller.countSolutions(state.numCats, state.numItems, state.clues))
    }

    @Test
    fun clueSet_admitsExactlyOneSolution_hard() {
        val state = controller.newGame(hardSize, Random(5))
        assertEquals(1, controller.countSolutions(state.numCats, state.numItems, state.clues))
    }

    @Test
    fun sameSeed_producesSamePuzzle() {
        val a = controller.newGame(easySize, Random(42))
        val b = controller.newGame(easySize, Random(42))
        assertEquals(a.solution, b.solution)
        assertEquals(a.clues, b.clues)
    }

    @Test
    fun solution_isValidPermutations() {
        val state = controller.newGame(easySize, Random(7))
        val m = state.numItems
        for (cat in 0 until state.numCats) {
            assertEquals("Cat $cat should be a permutation", (0 until m).toList(), state.solution[cat].sorted())
        }
    }

    @Test
    fun solutionCat0_isIdentity() {
        val state = controller.newGame(hardSize, Random(99))
        val expected = (0 until state.numItems).toList()
        assertEquals(expected, state.solution[0])
    }

    // -------------------------------------------------------------------------
    // Mark and auto-eliminate
    // -------------------------------------------------------------------------

    @Test
    fun mark_blank_storesBlank() {
        val state = controller.newGame(easySize, Random(10))
        val next = controller.mark(state, 0, 0, 1, 0, CellMark.BLANK)
        assertEquals(CellMark.BLANK, next.getMark(0, 0, 1, 0))
    }

    @Test
    fun mark_yes_storesYes() {
        val state = controller.newGame(easySize, Random(11))
        val next = controller.mark(state, 0, 0, 1, 1, CellMark.YES)
        assertEquals(CellMark.YES, next.getMark(0, 0, 1, 1))
    }

    @Test
    fun mark_incrementsMoveCount() {
        val state = controller.newGame(easySize, Random(12))
        val next = controller.mark(state, 0, 0, 1, 0, CellMark.YES)
        assertEquals(1, next.moveCount)
    }

    @Test
    fun autoEliminate_yes_marksRowNOs() {
        val state = controller.newGame(easySize, Random(13)).copy(autoEliminate = true)
        val m = state.numItems
        val next = controller.mark(state, 0, 0, 1, 0, CellMark.YES)
        // All other (0,0) vs (1,j) cells should be NO
        for (j in 1 until m) {
            assertEquals(
                "Expected NO at (0,0)-(1,$j) after YES at (0,0)-(1,0)",
                CellMark.NO,
                next.getMark(0, 0, 1, j)
            )
        }
    }

    @Test
    fun autoEliminate_yes_marksColNOs() {
        val state = controller.newGame(easySize, Random(14)).copy(autoEliminate = true)
        val m = state.numItems
        val next = controller.mark(state, 0, 0, 1, 0, CellMark.YES)
        // All other (0,i) vs (1,0) cells should be NO
        for (i in 1 until m) {
            assertEquals(
                "Expected NO at (0,$i)-(1,0) after YES at (0,0)-(1,0)",
                CellMark.NO,
                next.getMark(0, i, 1, 0)
            )
        }
    }

    @Test
    fun autoEliminate_off_doesNotFillNOs() {
        val state = controller.newGame(easySize, Random(15)).copy(autoEliminate = false)
        val next = controller.mark(state, 0, 0, 1, 0, CellMark.YES)
        assertEquals(CellMark.BLANK, next.getMark(0, 0, 1, 1))
    }

    @Test
    fun autoEliminate_doesNotOverwriteExistingMark() {
        val state = controller.newGame(easySize, Random(16)).copy(autoEliminate = true)
        // Pre-mark a cell manually
        val preMarked = controller.mark(state, 0, 0, 1, 2, CellMark.YES)
        // Now mark YES at (0,0,1,0): auto-eliminate should not overwrite the existing YES
        val next = controller.mark(preMarked, 0, 0, 1, 0, CellMark.YES)
        assertEquals(CellMark.YES, next.getMark(0, 0, 1, 2))
    }

    @Test
    fun mark_isCommutativeAcrossCategoryOrder() {
        val state = controller.newGame(easySize, Random(17))
        val a = controller.mark(state, 0, 1, 1, 2, CellMark.YES)
        val b = controller.mark(state, 1, 2, 0, 1, CellMark.YES)
        assertEquals(a.getMark(0, 1, 1, 2), b.getMark(0, 1, 1, 2))
    }

    // -------------------------------------------------------------------------
    // Contradiction detection
    // -------------------------------------------------------------------------

    @Test
    fun hasContradiction_noMarks_isFalse() {
        val state = controller.newGame(easySize, Random(20))
        assertFalse(controller.hasContradiction(state))
    }

    @Test
    fun hasContradiction_twoYesInRow_isTrue() {
        val state = controller.newGame(easySize, Random(21))
        // Mark two YES in the same row (0,0) vs cat1
        val s1 = controller.mark(state, 0, 0, 1, 0, CellMark.YES)
        val s2 = controller.mark(s1, 0, 0, 1, 1, CellMark.YES)
        assertTrue(controller.hasContradiction(s2))
    }

    @Test
    fun hasContradiction_twoYesInCol_isTrue() {
        val state = controller.newGame(easySize, Random(22))
        // Mark two YES in the same column: both rows point to same col
        val s1 = controller.mark(state, 0, 0, 1, 0, CellMark.YES)
        val s2 = controller.mark(s1, 0, 1, 1, 0, CellMark.YES)
        assertTrue(controller.hasContradiction(s2))
    }

    @Test
    fun hasContradiction_oneYesPerRowCol_isFalse() {
        val state = controller.newGame(easySize, Random(23))
        val sol = state.solution
        // Mark correct YES for entire (cat0, cat1) submatrix
        var s = state
        for (iA in 0 until state.numItems) {
            val iB = sol[1].indexOf(sol[0][iA])
            s = controller.mark(s, 0, iA, 1, iB, CellMark.YES)
        }
        assertFalse(controller.hasContradiction(s))
    }

    // -------------------------------------------------------------------------
    // isSolved
    // -------------------------------------------------------------------------

    @Test
    fun isSolved_emptyMarks_isFalse() {
        val state = controller.newGame(easySize, Random(30))
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun isSolved_partialMarks_isFalse() {
        val state = controller.newGame(easySize, Random(31))
        val sol = state.solution
        // Fill only the first sub-matrix
        var s = state
        for (iA in 0 until state.numItems) {
            val iB = sol[1].indexOf(sol[0][iA])
            s = controller.mark(s, 0, iA, 1, iB, CellMark.YES)
        }
        assertFalse(controller.isSolved(s))
    }

    @Test
    fun isSolved_correctFullSolution_isTrue() {
        val state = controller.newGame(easySize, Random(32))
        val sol = state.solution
        var s = state
        // Fill every sub-matrix with correct YES marks
        for (cA in 0 until state.numCats) {
            for (cB in cA + 1 until state.numCats) {
                for (iA in 0 until state.numItems) {
                    val entity = sol[cA][iA]
                    val iB = sol[cB].indexOf(entity)
                    s = controller.mark(s, cA, iA, cB, iB, CellMark.YES)
                }
            }
        }
        assertTrue(controller.isSolved(s))
    }

    @Test
    fun isSolved_wrongYesMark_isFalse() {
        val state = controller.newGame(easySize, Random(33))
        val sol = state.solution
        var s = state
        // Fill all sub-matrices correctly
        for (cA in 0 until state.numCats) {
            for (cB in cA + 1 until state.numCats) {
                for (iA in 0 until state.numItems) {
                    val entity = sol[cA][iA]
                    val iB = sol[cB].indexOf(entity)
                    s = controller.mark(s, cA, iA, cB, iB, CellMark.YES)
                }
            }
        }
        // Corrupt one YES: swap two entries in sub-matrix (0,1)
        val correctIB0 = sol[1].indexOf(sol[0][0])
        val correctIB1 = sol[1].indexOf(sol[0][1])
        s = controller.mark(s, 0, 0, 1, correctIB0, CellMark.BLANK)
        s = controller.mark(s, 0, 0, 1, correctIB1, CellMark.YES)
        assertFalse(controller.isSolved(s))
    }

    // -------------------------------------------------------------------------
    // Solver utility
    // -------------------------------------------------------------------------

    @Test
    fun countSolutions_emptyClues_returnsMultiple() {
        // No clues → many solutions possible (all permutations)
        val result = controller.countSolutions(3, 4, emptyList(), limit = 2)
        assertEquals(2, result) // stops at limit
    }

    @Test
    fun countSolutions_fullDirectClues_returnsOne() {
        val state = controller.newGame(easySize, Random(40))
        val sol = state.solution
        // Build the complete set of direct clues (one per entity per pair)
        val fullClues = buildList {
            for (cA in 0 until state.numCats) {
                for (cB in cA + 1 until state.numCats) {
                    for (entity in 0 until state.numItems) {
                        val iA = sol[cA].indexOf(entity)
                        val iB = sol[cB].indexOf(entity)
                        add(LogicGridClue.Direct(cA, iA, cB, iB))
                    }
                }
            }
        }
        assertEquals(1, controller.countSolutions(state.numCats, state.numItems, fullClues))
    }
}
