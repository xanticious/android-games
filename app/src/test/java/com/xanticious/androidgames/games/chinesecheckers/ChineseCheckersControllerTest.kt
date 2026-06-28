package com.xanticious.androidgames.games.chinesecheckers

import com.xanticious.androidgames.controller.games.chinesecheckers.ChineseCheckersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersCoordinate
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersMove
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChineseCheckersControllerTest {
    private val controller = ChineseCheckersController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun adjacency_neighboringHoles_areAdjacent() {
        assertTrue(controller.areAdjacent(c(0, 0, 0), c(1, -1, 0)))
    }

    @Test
    fun legalMoves_emptyAdjacentHole_includesSingleStep() {
        val state = controller.emptyState(config).copy(occupancy = mapOf(c(0, 0, 0) to ChineseCheckersSide.PLAYER))
        assertTrue(controller.legalMovesForPeg(state, c(0, 0, 0)).any { it.to == c(1, -1, 0) && !it.isHop })
    }

    @Test
    fun applyMove_singleStep_movesPegToDestination() {
        val state = controller.emptyState(config).copy(occupancy = mapOf(c(0, 0, 0) to ChineseCheckersSide.PLAYER))
        val moved = controller.applyMove(state, ChineseCheckersMove(c(0, 0, 0), c(1, -1, 0), listOf(c(0, 0, 0), c(1, -1, 0))))
        assertEquals(ChineseCheckersSide.PLAYER, moved.occupancy[c(1, -1, 0)])
    }

    @Test
    fun legalMoves_occupiedNeighborAndEmptyBeyond_includesHop() {
        val state = controller.emptyState(config).copy(
            occupancy = mapOf(c(0, 0, 0) to ChineseCheckersSide.PLAYER, c(1, -1, 0) to ChineseCheckersSide.AI)
        )
        assertTrue(controller.legalMovesForPeg(state, c(0, 0, 0)).any { it.to == c(2, -2, 0) && it.isHop })
    }

    @Test
    fun legalMoves_twoHopLane_includesChainedDoubleHop() {
        val state = controller.emptyState(config).copy(
            occupancy = mapOf(
                c(0, 0, 0) to ChineseCheckersSide.PLAYER,
                c(1, -1, 0) to ChineseCheckersSide.AI,
                c(3, -3, 0) to ChineseCheckersSide.AI
            )
        )
        assertTrue(controller.legalMovesForPeg(state, c(0, 0, 0)).any { it.path == listOf(c(0, 0, 0), c(2, -2, 0), c(4, -4, 0)) })
    }

    @Test
    fun winningSide_playerTargetFilled_returnsPlayer() {
        val state = controller.emptyState(config)
        val filled = state.copy(occupancy = state.targetRegions.getValue(ChineseCheckersSide.PLAYER).associateWith { ChineseCheckersSide.PLAYER })
        assertEquals(ChineseCheckersSide.PLAYER, controller.winningSide(filled))
    }

    @Test
    fun chooseAiMove_initialBoard_returnsLegalMove() {
        val state = controller.initialState(config).copy(currentPlayer = ChineseCheckersSide.AI)
        val move = controller.chooseAiMove(state, Random(7))
        assertTrue(controller.legalMoves(state, ChineseCheckersSide.AI).contains(move))
    }

    @Test
    fun initialState_starBoard_hasOneHundredTwentyOneHoles() {
        assertEquals(121, controller.initialState(config).holes.size)
    }

    @Test
    fun initialState_eachHome_hasTenHoles() {
        assertEquals(10, controller.initialState(config).homeRegions.getValue(ChineseCheckersSide.PLAYER).size)
    }

    private fun c(x: Int, y: Int, z: Int) = ChineseCheckersCoordinate(x, y, z)
}
