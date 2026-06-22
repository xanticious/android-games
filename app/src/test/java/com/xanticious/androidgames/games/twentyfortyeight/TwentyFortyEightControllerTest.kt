package com.xanticious.androidgames.games.twentyfortyeight

import com.xanticious.androidgames.controller.games.twentyfortyeight.TwentyFortyEightController
import com.xanticious.androidgames.controller.games.twentyfortyeight.abbreviate
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.twentyfortyeight.TwentyFortyEightState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TwentyFortyEightControllerTest {
    private val controller = TwentyFortyEightController()

    // -------------------------------------------------------------------------
    // mergeLine — canonical merge cases
    // -------------------------------------------------------------------------

    @Test
    fun mergeLine_simpleLeftMerge() {
        val (result, score, changed) = controller.mergeLine(listOf(2L, 2L, 0L, 0L))
        assertEquals(listOf(4L, 0L, 0L, 0L), result)
        assertEquals(4L, score)
        assertTrue(changed)
    }

    @Test
    fun mergeLine_noDoubleMerge() {
        // [2,2,2,2] → [4,4,0,0] — each tile merges at most once per move
        val (result, score, _) = controller.mergeLine(listOf(2L, 2L, 2L, 2L))
        assertEquals(listOf(4L, 4L, 0L, 0L), result)
        assertEquals(8L, score)
    }

    @Test
    fun mergeLine_slideWithoutMerge() {
        val (result, score, changed) = controller.mergeLine(listOf(0L, 0L, 2L, 4L))
        assertEquals(listOf(2L, 4L, 0L, 0L), result)
        assertEquals(0L, score)
        assertTrue(changed)
    }

    @Test
    fun mergeLine_alreadyPacked_noChange() {
        val (result, score, changed) = controller.mergeLine(listOf(2L, 4L, 8L, 16L))
        assertEquals(listOf(2L, 4L, 8L, 16L), result)
        assertEquals(0L, score)
        assertFalse(changed)
    }

    @Test
    fun mergeLine_mergeInMiddle() {
        // [2,4,4,8] → [2,8,8,0]? No: leading edge: pick 2, then 4==4 merge to 8, then 8 → [2,8,8,0]
        val (result, _, _) = controller.mergeLine(listOf(2L, 4L, 4L, 8L))
        assertEquals(listOf(2L, 8L, 8L, 0L), result)
    }

    @Test
    fun mergeLine_singleElement() {
        val (result, score, changed) = controller.mergeLine(listOf(0L, 0L, 0L, 4L))
        assertEquals(listOf(4L, 0L, 0L, 0L), result)
        assertEquals(0L, score)
        assertTrue(changed)
    }

    @Test
    fun mergeLine_allZero_noChange() {
        val (result, score, changed) = controller.mergeLine(listOf(0L, 0L, 0L, 0L))
        assertEquals(listOf(0L, 0L, 0L, 0L), result)
        assertEquals(0L, score)
        assertFalse(changed)
    }

    // -------------------------------------------------------------------------
    // move — direction tests using a known board
    // -------------------------------------------------------------------------

    private fun boardOf(size: Int, vararg rows: Long): TwentyFortyEightState =
        TwentyFortyEightState(size = size, tiles = rows.toList())

    @Test
    fun move_left_slidesMergesFromLeft() {
        // 2x2 board: [2,2 / 0,0] left → [4,0 / 0,0]
        val state = boardOf(2, 2L, 2L, 0L, 0L)
        val result = controller.move(state, Direction.LEFT)
        assertEquals(listOf(4L, 0L, 0L, 0L), result.state.tiles)
        assertEquals(4L, result.gainedScore)
        assertTrue(result.moved)
    }

    @Test
    fun move_right_slidesMergesFromRight() {
        // 2x2: [2,2 / 0,0] right → [0,4 / 0,0]
        val state = boardOf(2, 2L, 2L, 0L, 0L)
        val result = controller.move(state, Direction.RIGHT)
        assertEquals(listOf(0L, 4L, 0L, 0L), result.state.tiles)
        assertEquals(4L, result.gainedScore)
        assertTrue(result.moved)
    }

    @Test
    fun move_up_slidesMergesFromTop() {
        // 2x2: [2,0 / 2,0] up → [4,0 / 0,0]
        val state = boardOf(2, 2L, 0L, 2L, 0L)
        val result = controller.move(state, Direction.UP)
        assertEquals(listOf(4L, 0L, 0L, 0L), result.state.tiles)
        assertEquals(4L, result.gainedScore)
        assertTrue(result.moved)
    }

    @Test
    fun move_down_slidesMergesFromBottom() {
        // 2x2: [2,0 / 2,0] down → [0,0 / 4,0]
        val state = boardOf(2, 2L, 0L, 2L, 0L)
        val result = controller.move(state, Direction.DOWN)
        assertEquals(listOf(0L, 0L, 4L, 0L), result.state.tiles)
        assertEquals(4L, result.gainedScore)
        assertTrue(result.moved)
    }

    @Test
    fun move_noChange_movedIsFalse() {
        // Row already packed right, moving right again changes nothing
        val state = boardOf(2, 0L, 4L, 0L, 2L)
        val result = controller.move(state, Direction.RIGHT)
        assertFalse(result.moved)
        assertEquals(0L, result.gainedScore)
    }

    @Test
    fun move_scoreAccumulates() {
        val state = boardOf(2, 2L, 2L, 4L, 4L)
        val result = controller.move(state, Direction.LEFT)
        assertEquals(4L + 8L, result.gainedScore)
        assertEquals(4L + 8L, result.state.score)
    }

    @Test
    fun move_bestTileUpdated() {
        val state = boardOf(2, 8L, 8L, 0L, 0L)
        val result = controller.move(state, Direction.LEFT)
        assertEquals(16L, result.state.bestTile)
    }

    @Test
    fun move_snapshotRecordedOnChange() {
        val state = boardOf(2, 2L, 2L, 0L, 0L)
        val result = controller.move(state, Direction.LEFT)
        assertNotNull(result.state.undoSnapshot)
        assertEquals(state.tiles, result.state.undoSnapshot?.tiles)
    }

    @Test
    fun move_snapshotNotRecordedWhenNoChange() {
        val state = boardOf(2, 0L, 4L, 0L, 2L)
        val result = controller.move(state, Direction.RIGHT)
        assertNull(result.state.undoSnapshot)
    }

    // -------------------------------------------------------------------------
    // spawnTile
    // -------------------------------------------------------------------------

    @Test
    fun spawnTile_placesNonZeroInEmptyCell() {
        val state = TwentyFortyEightState(size = 4, tiles = List(16) { 0L })
        val next = controller.spawnTile(state, Random(1))
        assertEquals(15, next.tiles.count { it == 0L })
        assertTrue(next.tiles.any { it == 2L || it == 4L })
    }

    @Test
    fun spawnTile_fullBoard_returnsUnchanged() {
        val state = TwentyFortyEightState(size = 2, tiles = listOf(2L, 4L, 8L, 16L))
        val next = controller.spawnTile(state, Random.Default)
        assertEquals(state.tiles, next.tiles)
    }

    @Test
    fun spawnTile_seedDeterminesPosition() {
        val state = TwentyFortyEightState(size = 4, tiles = List(16) { 0L })
        val a = controller.spawnTile(state, Random(42))
        val b = controller.spawnTile(state, Random(42))
        assertEquals(a.tiles, b.tiles)
    }

    // -------------------------------------------------------------------------
    // hasWon / canMove
    // -------------------------------------------------------------------------

    @Test
    fun hasWon_returnsTrueWhen2048Present() {
        val state = TwentyFortyEightState(size = 2, tiles = listOf(2048L, 0L, 0L, 0L))
        assertTrue(controller.hasWon(state))
    }

    @Test
    fun hasWon_returnsFalseBelow2048() {
        val state = TwentyFortyEightState(size = 2, tiles = listOf(1024L, 512L, 256L, 128L))
        assertFalse(controller.hasWon(state))
    }

    @Test
    fun canMove_returnsTrueWithEmptyCell() {
        val state = TwentyFortyEightState(size = 2, tiles = listOf(2L, 4L, 8L, 0L))
        assertTrue(controller.canMove(state))
    }

    @Test
    fun canMove_returnsTrueWithAdjacentEqualTiles() {
        // Full board, but (0,0)==(0,1)
        val state = TwentyFortyEightState(size = 2, tiles = listOf(2L, 2L, 4L, 8L))
        assertTrue(controller.canMove(state))
    }

    @Test
    fun canMove_returnsFalseWhenNoMovePossible() {
        // Checkerboard — no equal neighbours, no empty cells
        val state = TwentyFortyEightState(size = 2, tiles = listOf(2L, 4L, 4L, 2L))
        // (0,0)=2, (0,1)=4 — different; (1,0)=4, (1,1)=2 — different
        // vertical: (0,0)=2, (1,0)=4 — different; (0,1)=4, (1,1)=2 — different
        assertFalse(controller.canMove(state))
    }

    @Test
    fun canMove_verticalEqualNeighbours() {
        val state = TwentyFortyEightState(size = 2, tiles = listOf(2L, 4L, 2L, 8L))
        assertTrue(controller.canMove(state))
    }

    // -------------------------------------------------------------------------
    // newGame
    // -------------------------------------------------------------------------

    @Test
    fun newGame_spawnsTwoTiles() {
        val state = controller.newGame(4, Random(7))
        assertEquals(2, state.tiles.count { it != 0L })
    }

    @Test
    fun newGame_allTilesAreValidPowersOfTwo() {
        val state = controller.newGame(4, Random(3))
        state.tiles.filter { it != 0L }.forEach { value ->
            assertTrue(value == 2L || value == 4L)
        }
    }

    @Test
    fun newGame_sizeIsCorrect() {
        val state = controller.newGame(6, Random.Default)
        assertEquals(6, state.size)
        assertEquals(36, state.tiles.size)
    }

    // -------------------------------------------------------------------------
    // undo
    // -------------------------------------------------------------------------

    @Test
    fun undo_restoresPreviousTiles() {
        val state = boardOf(2, 2L, 2L, 0L, 0L)
        val moved = controller.move(state, Direction.LEFT).state
        val undone = controller.undo(moved)
        assertEquals(state.tiles, undone.tiles)
    }

    @Test
    fun undo_restoresPreviousScore() {
        val state = boardOf(2, 2L, 2L, 0L, 0L)
        val moved = controller.move(state, Direction.LEFT).state
        val undone = controller.undo(moved)
        assertEquals(0L, undone.score)
    }

    @Test
    fun undo_noSnapshot_returnsUnchanged() {
        val state = boardOf(2, 2L, 4L, 8L, 16L)
        val result = controller.undo(state)
        assertEquals(state.tiles, result.tiles)
    }

    // -------------------------------------------------------------------------
    // abbreviate
    // -------------------------------------------------------------------------

    @Test
    fun abbreviate_small_returnsExact() {
        assertEquals("2", abbreviate(2L))
        assertEquals("512", abbreviate(512L))
        assertEquals("999", abbreviate(999L))
    }

    @Test
    fun abbreviate_thousands() {
        assertEquals("1k", abbreviate(1_000L))
        assertEquals("16k", abbreviate(16_384L))
        assertEquals("131k", abbreviate(131_072L))
    }

    @Test
    fun abbreviate_millions() {
        assertEquals("1m", abbreviate(1_000_000L))
        assertEquals("268m", abbreviate(268_435_456L))
    }

    @Test
    fun abbreviate_billions() {
        assertEquals("1b", abbreviate(1_000_000_000L))
        assertEquals("4b", abbreviate(4_294_967_296L))
    }

    @Test
    fun abbreviate_trillions() {
        assertEquals("1t", abbreviate(1_000_000_000_000L))
    }
}
