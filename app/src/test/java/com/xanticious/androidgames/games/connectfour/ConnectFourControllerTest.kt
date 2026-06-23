package com.xanticious.androidgames.games.connectfour

import com.xanticious.androidgames.controller.games.connectfour.ConnectFourController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.connectfour.ConnectFourResult
import com.xanticious.androidgames.model.games.connectfour.ConnectFourState
import com.xanticious.androidgames.model.games.connectfour.Disc
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectFourControllerTest {
    private val controller = ConnectFourController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    private fun drop(state: ConnectFourState, column: Int, disc: Disc): ConnectFourState =
        controller.dropDisc(state, config, column, disc).state

    @Test
    fun dropDisc_emptyColumn_landsOnBottomRow() {
        val state = drop(ConnectFourState.initial(config), column = 3, disc = Disc.PLAYER)
        assertEquals(Disc.PLAYER, state.grid[config.rows - 1][3])
    }

    @Test
    fun dropDisc_stackedColumn_appliesGravity() {
        val first = drop(ConnectFourState.initial(config), column = 3, disc = Disc.PLAYER)
        val second = drop(first, column = 3, disc = Disc.AI)
        assertEquals(Disc.AI, second.grid[config.rows - 2][3])
    }

    @Test
    fun dropDisc_horizontalFour_setsPlayerWin() {
        val state = listOf(0, 1, 2, 3).fold(ConnectFourState.initial(config)) { acc, column -> drop(acc, column, Disc.PLAYER) }
        assertEquals(ConnectFourResult.PLAYER_WIN, state.result)
    }

    @Test
    fun dropDisc_verticalFour_setsPlayerWin() {
        val state = List(4) { 0 }.fold(ConnectFourState.initial(config)) { acc, column -> drop(acc, column, Disc.PLAYER) }
        assertEquals(ConnectFourResult.PLAYER_WIN, state.result)
    }

    @Test
    fun dropDisc_descendingDiagonalFour_setsPlayerWin() {
        var state = ConnectFourState.initial(config)
        state = drop(state, 0, Disc.PLAYER)
        state = drop(state, 1, Disc.AI)
        state = drop(state, 1, Disc.PLAYER)
        state = drop(state, 2, Disc.AI)
        state = drop(state, 2, Disc.AI)
        state = drop(state, 2, Disc.PLAYER)
        state = drop(state, 3, Disc.AI)
        state = drop(state, 3, Disc.AI)
        state = drop(state, 3, Disc.AI)
        state = drop(state, 3, Disc.PLAYER)
        assertEquals(ConnectFourResult.PLAYER_WIN, state.result)
    }

    @Test
    fun dropDisc_ascendingDiagonalFour_setsPlayerWin() {
        var state = ConnectFourState.initial(config)
        state = drop(state, 0, Disc.AI)
        state = drop(state, 0, Disc.AI)
        state = drop(state, 0, Disc.AI)
        state = drop(state, 0, Disc.PLAYER)
        state = drop(state, 1, Disc.AI)
        state = drop(state, 1, Disc.AI)
        state = drop(state, 1, Disc.PLAYER)
        state = drop(state, 2, Disc.AI)
        state = drop(state, 2, Disc.PLAYER)
        state = drop(state, 3, Disc.PLAYER)
        assertEquals(ConnectFourResult.PLAYER_WIN, state.result)
    }

    @Test
    fun resultFor_fullBoardWithoutFour_returnsDraw() {
        val grid = listOf(
            listOf(Disc.AI, Disc.AI, Disc.PLAYER, Disc.AI, Disc.PLAYER, Disc.AI, Disc.AI),
            listOf(Disc.PLAYER, Disc.AI, Disc.PLAYER, Disc.PLAYER, Disc.AI, Disc.PLAYER, Disc.PLAYER),
            listOf(Disc.AI, Disc.PLAYER, Disc.PLAYER, Disc.PLAYER, Disc.AI, Disc.PLAYER, Disc.AI),
            listOf(Disc.PLAYER, Disc.AI, Disc.AI, Disc.PLAYER, Disc.AI, Disc.AI, Disc.PLAYER),
            listOf(Disc.PLAYER, Disc.AI, Disc.PLAYER, Disc.AI, Disc.PLAYER, Disc.AI, Disc.PLAYER),
            listOf(Disc.AI, Disc.AI, Disc.PLAYER, Disc.PLAYER, Disc.AI, Disc.AI, Disc.AI)
        )
        val state = ConnectFourState.initial(config).copy(grid = grid, moveCount = 42)
        assertEquals(ConnectFourResult.DRAW, controller.resultFor(state, config))
    }

    @Test
    fun aiMove_mediumImmediateThreat_blocksPlayer() {
        val threatened = listOf(0, 1, 2).fold(ConnectFourState.initial(config)) { acc, column -> drop(acc, column, Disc.PLAYER) }
        val move = controller.aiMove(threatened, config, GameDifficulty.MEDIUM, Random(4))
        assertEquals(3, move)
    }
}
