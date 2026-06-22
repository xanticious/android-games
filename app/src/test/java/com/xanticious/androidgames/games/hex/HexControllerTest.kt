package com.xanticious.androidgames.games.hex

import com.xanticious.androidgames.controller.games.hex.HexController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.hex.HexCell
import com.xanticious.androidgames.model.games.hex.HexEvent
import com.xanticious.androidgames.model.games.hex.HexMove
import com.xanticious.androidgames.model.games.hex.HexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HexControllerTest {
    private val controller = HexController(Random(1))
    private val config = controller.configFor(GameDifficulty.EASY)

    @Test
    fun placeStone_completedLeftRightChain_playerWins() {
        var state = HexState.initial(config.copy(boardSize = 3))
        state = controller.placeStone(state, 0, 0, HexCell.PLAYER).state
        state = controller.placeStone(state, 1, 0, HexCell.AI).state
        state = controller.placeStone(state, 0, 1, HexCell.PLAYER).state
        state = controller.placeStone(state, 1, 1, HexCell.AI).state
        val result = controller.placeStone(state, 0, 2, HexCell.PLAYER)
        assertEquals(HexEvent.PLAYER_WON, result.event)
    }

    @Test
    fun hasConnection_incompleteLeftRightChain_returnsFalse() {
        val state = HexState.initial(config.copy(boardSize = 3)).copy(
            board = listOf(
                HexCell.PLAYER, HexCell.EMPTY, HexCell.PLAYER,
                HexCell.EMPTY, HexCell.AI, HexCell.EMPTY,
                HexCell.EMPTY, HexCell.EMPTY, HexCell.EMPTY
            )
        )
        assertFalse(controller.hasConnection(state, HexCell.PLAYER))
    }

    @Test
    fun neighbors_centerCell_returnsSixHexAdjacentCells() {
        val expected = setOf(HexMove(0, 1), HexMove(0, 2), HexMove(1, 0), HexMove(1, 2), HexMove(2, 0), HexMove(2, 1))
        assertEquals(expected, controller.neighbors(3, 1, 1).toSet())
    }

    @Test
    fun aiMove_availableCells_returnsLegalEmptyCell() {
        val state = HexState.initial(config.copy(boardSize = 3)).copy(
            board = listOf(
                HexCell.PLAYER, HexCell.EMPTY, HexCell.EMPTY,
                HexCell.EMPTY, HexCell.AI, HexCell.EMPTY,
                HexCell.EMPTY, HexCell.EMPTY, HexCell.EMPTY
            ),
            currentPlayer = HexCell.AI
        )
        val move = controller.aiMove(state, config.copy(boardSize = 3), Random(2))
        assertTrue(move in controller.legalMoves(state))
    }
}
