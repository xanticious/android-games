package com.xanticious.androidgames.games.minesweeper

import com.xanticious.androidgames.controller.games.minesweeper.MinesweeperController
import com.xanticious.androidgames.model.games.minesweeper.CellMark
import com.xanticious.androidgames.model.games.minesweeper.MinesweeperConfig
import com.xanticious.androidgames.model.games.minesweeper.MinesweeperState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MinesweeperControllerTest {

    private val controller = MinesweeperController()
    private val seed = Random(42)

    private fun smallGame(rows: Int = 5, cols: Int = 5, mines: Int = 5): MinesweeperState {
        val state = controller.newGame(rows, cols, mines)
        return controller.placeMines(state, GridPos(2, 2), seed)
    }

    // ── Board setup ──────────────────────────────────────────────────────────

    @Test
    fun newGame_allCellsCovered() {
        val state = controller.newGame(9, 9, 10)
        assertTrue(state.cells.all { it.mark == CellMark.COVERED })
    }

    @Test
    fun newGame_minesNotPlacedYet() {
        val state = controller.newGame(9, 9, 10)
        assertFalse(state.minesPlaced)
    }

    @Test
    fun newGame_correctConfig() {
        val state = controller.newGame(9, 9, 10)
        assertEquals(MinesweeperConfig(9, 9, 10), state.config)
    }

    @Test
    fun placeMines_mineCountIsCorrect() {
        val state = smallGame()
        assertEquals(5, state.cells.count { it.hasMine })
    }

    @Test
    fun placeMines_minesPlacedFlagSet() {
        val state = smallGame()
        assertTrue(state.minesPlaced)
    }

    @Test
    fun placeMines_safeCellHasNoMine() {
        val safe = GridPos(2, 2)
        val state = smallGame()
        assertFalse(state.cellAt(safe).hasMine)
    }

    @Test
    fun placeMines_safeZoneNeighborHasNoMine() {
        // All 8 neighbors of the safe cell should be mine-free on a 5x5/5 board.
        val safe = GridPos(2, 2)
        val state = smallGame()
        val neighbors = controller.neighbors8(2, 2, 5, 5)
        val anyNeighborIsMine = neighbors.any { (r, c) -> state.cellAt(r, c).hasMine }
        assertFalse(anyNeighborIsMine)
    }

    @Test
    fun placeMines_allCellsStillCovered() {
        val state = smallGame()
        assertTrue(state.cells.all { it.mark == CellMark.COVERED })
    }

    // ── Neighbor counts ───────────────────────────────────────────────────────

    @Test
    fun neighborCount_centerOfAllMines_isEight() {
        // 3x3 board: 8 mines around the centre, centre safe.
        val base = controller.newGame(3, 3, 8)
        val state = controller.placeMines(base, GridPos(1, 1), Random(1))
        assertEquals(8, controller.neighborMineCount(state, GridPos(1, 1)))
    }

    @Test
    fun neighborCount_cornerCell_correctCount() {
        // 3x3/1: mine must be away from safe zone (center safe zone covers all).
        // Use a 5x5 board for a reliable corner test.
        val base = controller.newGame(5, 5, 1)
        // Place with safe at (4,4) so mine can land near (0,0).
        val state = controller.placeMines(base, GridPos(4, 4), Random(99))
        val topLeft = state.cellAt(GridPos(0, 0))
        // neighborCount should equal number of mines adjacent to (0,0).
        val expected = controller.neighbors8(0, 0, 5, 5)
            .count { (r, c) -> state.cellAt(r, c).hasMine }
        assertEquals(expected, topLeft.neighborCount)
    }

    // ── Reveal / flood-fill ───────────────────────────────────────────────────

    @Test
    fun reveal_coveredCell_becomesRevealed() {
        val state = smallGame()
        val revealed = controller.reveal(state, GridPos(2, 2))
        assertEquals(CellMark.REVEALED, revealed.cellAt(GridPos(2, 2)).mark)
    }

    @Test
    fun reveal_alreadyRevealedCell_unchanged() {
        val state = smallGame()
        val once = controller.reveal(state, GridPos(2, 2))
        val twice = controller.reveal(once, GridPos(2, 2))
        assertEquals(once.cells, twice.cells)
    }

    @Test
    fun reveal_flaggedCell_unchanged() {
        val state = smallGame()
        val flagged = controller.toggleFlag(state, GridPos(2, 2))
        val attempted = controller.reveal(flagged, GridPos(2, 2))
        assertEquals(CellMark.FLAGGED, attempted.cellAt(GridPos(2, 2)).mark)
    }

    @Test
    fun reveal_zeroCell_floodOpensNeighbors() {
        // Build a 5x5 board with no mines — all neighbors of any cell are 0.
        val base = controller.newGame(5, 5, 0)
        val state = controller.placeMines(base, GridPos(0, 0), Random(0))
        val revealed = controller.reveal(state, GridPos(2, 2))
        // All 25 cells should be revealed (flood from 0-count cell).
        assertTrue(revealed.cells.all { it.mark == CellMark.REVEALED })
    }

    @Test
    fun reveal_incrementsRevealedCount() {
        val state = smallGame()
        val revealed = controller.reveal(state, GridPos(2, 2))
        assertTrue(revealed.revealedCount >= 1)
    }

    @Test
    fun reveal_mineCell_setsExplodedPos() {
        // Find a mine and reveal it directly.
        val state = smallGame()
        val minePos = state.cells.indexOfFirst { it.hasMine }
        val pos = GridPos(minePos / 5, minePos % 5)
        val result = controller.reveal(state, pos)
        assertEquals(pos, result.explodedPos)
    }

    // ── Flag toggle ───────────────────────────────────────────────────────────

    @Test
    fun toggleFlag_covered_becomesFlag() {
        val state = smallGame()
        val flagged = controller.toggleFlag(state, GridPos(0, 0))
        assertEquals(CellMark.FLAGGED, flagged.cellAt(GridPos(0, 0)).mark)
    }

    @Test
    fun toggleFlag_flagged_becomesCovered() {
        val state = smallGame()
        val flagged = controller.toggleFlag(state, GridPos(0, 0))
        val unflagged = controller.toggleFlag(flagged, GridPos(0, 0))
        assertEquals(CellMark.COVERED, unflagged.cellAt(GridPos(0, 0)).mark)
    }

    @Test
    fun toggleFlag_revealed_isNoop() {
        val state = smallGame()
        val revealed = controller.reveal(state, GridPos(2, 2))
        val attempted = controller.toggleFlag(revealed, GridPos(2, 2))
        assertEquals(CellMark.REVEALED, attempted.cellAt(GridPos(2, 2)).mark)
    }

    @Test
    fun toggleFlag_incrementsFlagCount() {
        val state = smallGame()
        val flagged = controller.toggleFlag(state, GridPos(0, 0))
        assertEquals(1, flagged.flagCount)
    }

    @Test
    fun toggleFlag_decrementsFlagCount() {
        val state = smallGame()
        val flagged = controller.toggleFlag(state, GridPos(0, 0))
        val unflagged = controller.toggleFlag(flagged, GridPos(0, 0))
        assertEquals(0, unflagged.flagCount)
    }

    // ── Win / loss ────────────────────────────────────────────────────────────

    @Test
    fun hitMine_noMineRevealed_isFalse() {
        val state = smallGame()
        assertFalse(controller.hitMine(state))
    }

    @Test
    fun hitMine_afterMineReveal_isTrue() {
        val state = smallGame()
        val mineIdx = state.cells.indexOfFirst { it.hasMine }
        val pos = GridPos(mineIdx / 5, mineIdx % 5)
        val result = controller.reveal(state, pos)
        assertTrue(controller.hitMine(result))
    }

    @Test
    fun isSolved_freshBoard_isFalse() {
        val state = smallGame()
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun isSolved_allSafeCellsRevealed_isTrue() {
        // Zero-mine board: reveal any cell → flood opens all → solved.
        val base = controller.newGame(3, 3, 0)
        val state = controller.placeMines(base, GridPos(1, 1), Random(0))
        val revealed = controller.reveal(state, GridPos(1, 1))
        assertTrue(controller.isSolved(revealed))
    }

    @Test
    fun firstClick_safe_neverMine() {
        // Run many seeds to confirm first-click safety.
        val safe = GridPos(0, 0)
        repeat(50) { seed ->
            val base = controller.newGame(9, 9, 10)
            val state = controller.placeMines(base, safe, Random(seed.toLong()))
            assertFalse("seed $seed: first cell has mine", state.cellAt(safe).hasMine)
        }
    }

    @Test
    fun explodedPos_isNullOnFreshBoard() {
        assertNull(controller.newGame(9, 9, 10).explodedPos)
    }

    @Test
    fun explodedPos_setToCorrectPosition() {
        val state = smallGame()
        val mineIdx = state.cells.indexOfFirst { it.hasMine }
        val pos = GridPos(mineIdx / 5, mineIdx % 5)
        val result = controller.reveal(state, pos)
        assertEquals(pos, result.explodedPos)
        assertNotNull(result.explodedPos)
    }
}
