package com.xanticious.androidgames.games.gomoku

import com.xanticious.androidgames.controller.games.gomoku.GomokuController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.gomoku.GomokuConfig
import com.xanticious.androidgames.model.games.gomoku.GomokuPoint
import com.xanticious.androidgames.model.games.gomoku.GomokuResult
import com.xanticious.androidgames.model.games.gomoku.GomokuState
import com.xanticious.androidgames.model.games.gomoku.Stone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GomokuControllerTest {
    private val controller = GomokuController()
    private val config = GomokuConfig(aiStrength = GameDifficulty.HARD)

    @Test
    fun placeStone_horizontalFive_blackWins() {
        val state = stateWith((0..3).map { GomokuPoint(7, it) to Stone.BLACK }, Stone.BLACK)
        assertEquals(GomokuResult.BLACK_WON, controller.placeStone(state, 7, 4).state.result)
    }

    @Test
    fun placeStone_verticalFive_blackWins() {
        val state = stateWith((0..3).map { GomokuPoint(it, 7) to Stone.BLACK }, Stone.BLACK)
        assertEquals(GomokuResult.BLACK_WON, controller.placeStone(state, 4, 7).state.result)
    }

    @Test
    fun placeStone_downRightDiagonalFive_blackWins() {
        val state = stateWith((0..3).map { GomokuPoint(it, it) to Stone.BLACK }, Stone.BLACK)
        assertEquals(GomokuResult.BLACK_WON, controller.placeStone(state, 4, 4).state.result)
    }

    @Test
    fun placeStone_upRightDiagonalFive_blackWins() {
        val state = stateWith((0..3).map { GomokuPoint(10 - it, it) to Stone.BLACK }, Stone.BLACK)
        assertEquals(GomokuResult.BLACK_WON, controller.placeStone(state, 6, 4).state.result)
    }

    @Test
    fun winningLine_sixStoneLine_countsAsFiveOrMore() {
        val state = stateWith((0..5).map { GomokuPoint(7, it) to Stone.BLACK }, Stone.BLACK)
        assertEquals(6, controller.winningLine(state.board, GomokuPoint(7, 3), Stone.BLACK).size)
    }

    @Test
    fun winningLine_sixStoneLine_exactlyFiveRejectsOverline() {
        val state = stateWith((0..5).map { GomokuPoint(7, it) to Stone.BLACK }, Stone.BLACK)
        assertEquals(emptyList<GomokuPoint>(), controller.winningLine(state.board, GomokuPoint(7, 3), Stone.BLACK, exactlyFive = true))
    }

    @Test
    fun chooseAiMove_playerOpenFour_blocksAnEnd() {
        val state = stateWith((5..8).map { GomokuPoint(7, it) to Stone.BLACK }, Stone.WHITE)
        assertTrue(controller.chooseAiMove(state, seed = 3L) in setOf(GomokuPoint(7, 4), GomokuPoint(7, 9)))
    }

    @Test
    fun chooseAiMove_partialBoard_returnsEmptyCell() {
        val state = stateWith(listOf(GomokuPoint(7, 7) to Stone.BLACK, GomokuPoint(7, 8) to Stone.WHITE), Stone.WHITE)
        val move = controller.chooseAiMove(state, seed = 11L)
        assertEquals(Stone.EMPTY, move?.let { state.stoneAt(it) })
    }

    private fun stateWith(stones: List<Pair<GomokuPoint, Stone>>, turn: Stone): GomokuState {
        val board = MutableList(config.boardSize) { MutableList(config.boardSize) { Stone.EMPTY } }
        stones.forEach { (point, stone) -> board[point.row][point.col] = stone }
        return GomokuState(
            board = board.map { it.toList() },
            turn = turn,
            config = config,
            moveCount = stones.size
        )
    }
}
