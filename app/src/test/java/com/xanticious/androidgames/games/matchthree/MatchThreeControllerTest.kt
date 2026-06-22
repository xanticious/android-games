package com.xanticious.androidgames.games.matchthree

import com.xanticious.androidgames.controller.games.matchthree.MatchThreeController
import com.xanticious.androidgames.model.games.matchthree.GemType
import com.xanticious.androidgames.model.games.matchthree.MatchThreeBoard
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MatchThreeControllerTest {

    private val controller = MatchThreeController()

    // ── newBoard ──────────────────────────────────────────────────────────────

    @Test
    fun newBoard_noInitialMatches() {
        val board = controller.newBoard(8, 8, 6, Random(99))
        assertTrue(controller.findMatches(board).isEmpty())
    }

    @Test
    fun newBoard_sameSeed_sameBoard() {
        val board1 = controller.newBoard(8, 8, 6, Random(12345))
        val board2 = controller.newBoard(8, 8, 6, Random(12345))
        assertEquals(board1, board2)
    }

    @Test
    fun newBoard_differentSeeds_differentBoards() {
        val board1 = controller.newBoard(8, 8, 6, Random(1))
        val board2 = controller.newBoard(8, 8, 6, Random(2))
        assertFalse(board1 == board2)
    }

    @Test
    fun newBoard_correctDimensions() {
        val board = controller.newBoard(6, 7, 4, Random(0))
        assertEquals(6, board.rows)
        assertEquals(7, board.cols)
        assertEquals(42, board.gems.size)
    }

    @Test
    fun newBoard_onlyActiveGemTypes() {
        val board = controller.newBoard(8, 8, 4, Random(42))
        val activeTypes = GemType.entries.take(4).toSet()
        assertTrue(board.gems.all { it in activeTypes })
    }

    // ── findMatches ───────────────────────────────────────────────────────────

    @Test
    fun findMatches_detectsHorizontalThreeInARow() {
        // 3×3 board: row 0 = AQUA,AQUA,AQUA; rest = TEAL
        val gems = MutableList<GemType?>(9) { GemType.TEAL }
        gems[0] = GemType.AQUA; gems[1] = GemType.AQUA; gems[2] = GemType.AQUA
        val board = MatchThreeBoard(3, 3, gems)
        val matches = controller.findMatches(board)
        assertTrue(matches.containsAll(setOf(GridPos(0, 0), GridPos(0, 1), GridPos(0, 2))))
    }

    @Test
    fun findMatches_detectsVerticalThreeInAColumn() {
        // 3×3 board: col 0 = AQUA,AQUA,AQUA; rest = TEAL
        val gems = MutableList<GemType?>(9) { GemType.TEAL }
        gems[0] = GemType.AQUA; gems[3] = GemType.AQUA; gems[6] = GemType.AQUA
        val board = MatchThreeBoard(3, 3, gems)
        val matches = controller.findMatches(board)
        assertTrue(matches.containsAll(setOf(GridPos(0, 0), GridPos(1, 0), GridPos(2, 0))))
    }

    @Test
    fun findMatches_noMatchOnCleanBoard() {
        val board = controller.newBoard(8, 8, 6, Random(7))
        assertTrue(controller.findMatches(board).isEmpty())
    }

    @Test
    fun findMatches_pairIsNotAMatch() {
        // 1-row, 4-col board: only 2 AQUA adjacent — must not be a match (need 3+)
        val gems: List<GemType?> = listOf(GemType.AQUA, GemType.AQUA, GemType.TEAL, GemType.CORAL)
        val board = MatchThreeBoard(1, 4, gems)
        assertTrue(controller.findMatches(board).isEmpty())
    }

    // ── hasValidMove ─────────────────────────────────────────────────────────

    @Test
    fun hasValidMove_freshBoard_returnsTrue() {
        // A freshly generated board should always have at least one valid move
        val board = controller.newBoard(8, 8, 6, Random(5))
        assertTrue(controller.hasValidMove(board))
    }

    @Test
    fun hasValidMove_deadBoard_returnsFalse() {
        // (row + col) % 3 pattern: every adjacent swap creates at most 2 in a row,
        // never 3, so no swap is valid (proven by exhaustion in the design).
        val types = GemType.entries.take(3)
        val gems = (0 until 4).flatMap { row ->
            (0 until 4).map { col -> types[(row + col) % 3] }
        }
        val board = MatchThreeBoard(4, 4, gems)
        assertFalse(controller.hasValidMove(board))
    }

    // ── areAdjacent ───────────────────────────────────────────────────────────

    @Test
    fun areAdjacent_horizontalNeighbour_returnsTrue() {
        assertTrue(controller.areAdjacent(GridPos(0, 0), GridPos(0, 1)))
    }

    @Test
    fun areAdjacent_verticalNeighbour_returnsTrue() {
        assertTrue(controller.areAdjacent(GridPos(0, 0), GridPos(1, 0)))
    }

    @Test
    fun areAdjacent_diagonal_returnsFalse() {
        assertFalse(controller.areAdjacent(GridPos(0, 0), GridPos(1, 1)))
    }

    @Test
    fun areAdjacent_sameCell_returnsFalse() {
        assertFalse(controller.areAdjacent(GridPos(2, 2), GridPos(2, 2)))
    }

    // ── applySwap ─────────────────────────────────────────────────────────────

    @Test
    fun applySwap_producingMatch_isValidAndClearsAtLeastThree() {
        // Row 0: TEAL,AQUA,TEAL,TEAL — swap (0,0)↔(0,1) → AQUA,TEAL,TEAL,TEAL = 3 TEAL match
        val gems: List<GemType?> = listOf(
            GemType.TEAL, GemType.AQUA, GemType.TEAL, GemType.TEAL,   // row 0
            GemType.AQUA, GemType.CORAL, GemType.AQUA, GemType.CORAL, // row 1
            GemType.CORAL, GemType.TEAL, GemType.CORAL, GemType.AQUA, // row 2
            GemType.TEAL, GemType.AQUA, GemType.TEAL, GemType.CORAL   // row 3
        )
        val board = MatchThreeBoard(4, 4, gems)
        val result = controller.applySwap(board, GridPos(0, 0), GridPos(0, 1), gemTypes = 6, random = Random(42))
        assertTrue(result.valid)
        assertTrue(result.totalCleared >= 3)
        assertTrue(result.cascadeCount >= 1)
    }

    @Test
    fun applySwap_producingNoMatch_isInvalidAndBoardUnchanged() {
        // Every row is a rotation of AQUA,TEAL,CORAL,GREEN — no adjacent swap
        // of the first two cells produces 3-in-a-row
        val gems: List<GemType?> = listOf(
            GemType.AQUA, GemType.TEAL, GemType.CORAL, GemType.GREEN,
            GemType.TEAL, GemType.CORAL, GemType.GREEN, GemType.AQUA,
            GemType.CORAL, GemType.GREEN, GemType.AQUA, GemType.TEAL,
            GemType.GREEN, GemType.AQUA, GemType.TEAL, GemType.CORAL
        )
        val board = MatchThreeBoard(4, 4, gems)
        val result = controller.applySwap(board, GridPos(0, 0), GridPos(0, 1), gemTypes = 6, random = Random(42))
        assertFalse(result.valid)
        assertEquals(board, result.board)
    }

    @Test
    fun applySwap_nonAdjacentCells_isInvalid() {
        val board = controller.newBoard(8, 8, 6, Random(1))
        val result = controller.applySwap(board, GridPos(0, 0), GridPos(3, 3), gemTypes = 6, random = Random(1))
        assertFalse(result.valid)
    }

    // ── resolveCascades ───────────────────────────────────────────────────────

    /**
     * Board where clearing row 1 (four AQUA gems) causes col 0's three TEAL gems
     * to drop together — producing a second cascade round.
     *
     * Layout (4 cols × 5 rows):
     * ```
     *       col: 0     1     2     3
     * row 0:   TEAL  CORAL GREEN AQUA
     * row 1:   AQUA  AQUA  AQUA  AQUA   ← initial horizontal match (4 in a row)
     * row 2:   TEAL  CORAL GREEN CORAL
     * row 3:   TEAL  GREEN CORAL GREEN
     * row 4:   CORAL TEAL  AQUA  TEAL
     * ```
     * After row 1 is cleared: col 0 = TEAL,_,TEAL,TEAL,CORAL → drops → _,TEAL,TEAL,TEAL,CORAL
     * → three TEAL vertically = cascade!
     */
    @Test
    fun resolveCascades_chainReaction_cascadeCountAtLeast2() {
        val gems: List<GemType?> = listOf(
            GemType.TEAL,  GemType.CORAL, GemType.GREEN, GemType.AQUA,  // row 0
            GemType.AQUA,  GemType.AQUA,  GemType.AQUA,  GemType.AQUA,  // row 1 – match
            GemType.TEAL,  GemType.CORAL, GemType.GREEN, GemType.CORAL, // row 2
            GemType.TEAL,  GemType.GREEN, GemType.CORAL, GemType.GREEN, // row 3
            GemType.CORAL, GemType.TEAL,  GemType.AQUA,  GemType.TEAL   // row 4
        )
        val board = MatchThreeBoard(rows = 5, cols = 4, gems = gems)
        val result = controller.resolveCascades(board, gemTypes = 4, random = Random(42))
        assertTrue("Expected cascadeCount >= 2, was ${result.cascadeCount}", result.cascadeCount >= 2)
        assertTrue("Expected totalCleared >= 7 (4 + 3), was ${result.totalCleared}", result.totalCleared >= 7)
    }

    @Test
    fun resolveCascades_singleMatch_cascadeCountIs1() {
        // Only one horizontal match; no further matches after drop (isolated row).
        val gems: List<GemType?> = listOf(
            GemType.AQUA,  GemType.AQUA,  GemType.AQUA,  GemType.TEAL,
            GemType.TEAL,  GemType.CORAL, GemType.TEAL,  GemType.CORAL,
            GemType.CORAL, GemType.TEAL,  GemType.CORAL, GemType.AQUA
        )
        val board = MatchThreeBoard(rows = 3, cols = 4, gems = gems)
        val result = controller.resolveCascades(board, gemTypes = 3, random = Random(7))
        assertEquals(1, result.cascadeCount)
        assertEquals(3, result.totalCleared)
    }

    @Test
    fun resolveCascades_noMatchBoard_isUnchanged() {
        val board = controller.newBoard(6, 6, 6, Random(11))
        val result = controller.resolveCascades(board, gemTypes = 6, random = Random(11))
        assertEquals(board, result.board)
        assertEquals(0, result.totalCleared)
        assertEquals(0, result.cascadeCount)
    }

    // ── reshuffle ─────────────────────────────────────────────────────────────

    @Test
    fun reshuffle_fromDeadBoard_yieldsBoardWithValidMove() {
        val types = GemType.entries.take(3)
        val gems = (0 until 4).flatMap { row ->
            (0 until 4).map { col -> types[(row + col) % 3] }
        }
        val dead = MatchThreeBoard(4, 4, gems)
        assertFalse("Pre-condition: dead board has no valid move", controller.hasValidMove(dead))
        val reshuffled = controller.reshuffle(dead, Random(42))
        assertTrue(controller.hasValidMove(reshuffled))
    }

    @Test
    fun reshuffle_resultHasNoInitialMatches() {
        val board = controller.newBoard(7, 7, 6, Random(3))
        val reshuffled = controller.reshuffle(board, Random(77))
        assertTrue(controller.findMatches(reshuffled).isEmpty())
    }

    // ── cascadeTimeBonus ──────────────────────────────────────────────────────

    @Test
    fun cascadeTimeBonus_singleRound_isZero() {
        assertEquals(0.0f, controller.cascadeTimeBonus(1))
    }

    @Test
    fun cascadeTimeBonus_twoRounds_isHalfSecond() {
        assertEquals(0.5f, controller.cascadeTimeBonus(2))
    }

    @Test
    fun cascadeTimeBonus_manyRounds_cappedAtThree() {
        assertTrue(controller.cascadeTimeBonus(100) <= 3.0f)
    }
}
