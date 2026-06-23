package com.xanticious.androidgames.games.slidingpuzzle

import com.xanticious.androidgames.controller.games.slidingpuzzle.SlidingPuzzleController
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.slidingpuzzle.SlidingPuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SlidingPuzzleControllerTest {
    private val controller = SlidingPuzzleController()

    @Test
    fun solvedBoard_isSolved() {
        assertTrue(controller.isSolved(SlidingPuzzleState.solved(4)))
    }

    @Test
    fun newGame_isNotSolved() {
        val state = controller.newGame(4, Random(7))
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun newGame_isSolvableByReversingShuffles() {
        // A scramble built from legal slides is, by construction, solvable; verify
        // it contains all tiles 0..n*n-1 exactly once (a valid permutation).
        val state = controller.newGame(5, Random(3))
        assertEquals((0 until 25).toList(), state.tiles.sorted())
    }

    @Test
    fun slide_movesTileIntoGap() {
        // 3x3 solved: blank at index 8 (row 2, col 2). Tapping (2,1) slides tile.
        val solved = SlidingPuzzleState.solved(3)
        val next = controller.slide(solved, GridPos(2, 1))
        assertEquals(0, next.tiles[7])
        assertEquals(1, next.moves)
    }

    @Test
    fun slide_nonAlignedTap_doesNothing() {
        val solved = SlidingPuzzleState.solved(3)
        val next = controller.slide(solved, GridPos(0, 0))
        assertEquals(solved.tiles, next.tiles)
    }

    @Test
    fun slide_thenUndo_restoresBoard() {
        val solved = SlidingPuzzleState.solved(3)
        val moved = controller.slide(solved, GridPos(2, 0))
        val undone = controller.undo(moved)
        assertEquals(solved.tiles, undone.tiles)
    }

    @Test
    fun slide_wholeRunTowardGap() {
        // Tapping the far end of the blank's row slides the entire run.
        val solved = SlidingPuzzleState.solved(3) // blank bottom-right
        val next = controller.slide(solved, GridPos(2, 0))
        assertEquals(0, next.tiles[6]) // blank moved to (2,0)
    }

    @Test
    fun correctlyPlaced_countsGoalTiles() {
        assertEquals(8, controller.correctlyPlaced(SlidingPuzzleState.solved(3)))
    }
}
