package com.xanticious.androidgames.games.zilch

import com.xanticious.androidgames.controller.games.zilch.ZilchController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.zilch.ZilchPlayer
import com.xanticious.androidgames.model.games.zilch.ZilchResult
import com.xanticious.androidgames.model.games.zilch.ZilchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ZilchControllerTest {
    private val controller = ZilchController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun scoreSelection_singleOne_scoresOneHundred() {
        assertEquals(100, controller.scoreSelection(listOf(1)).points)
    }

    @Test
    fun scoreSelection_singleFive_scoresFifty() {
        assertEquals(50, controller.scoreSelection(listOf(5)).points)
    }

    @Test
    fun scoreSelection_threeFives_scoresFiveHundred() {
        assertEquals(500, controller.scoreSelection(listOf(5, 5, 5)).points)
    }

    @Test
    fun scoreSelection_threeOnes_scoresOneThousand() {
        assertEquals(1_000, controller.scoreSelection(listOf(1, 1, 1)).points)
    }

    @Test
    fun scoreSelection_straight_scoresFifteenHundred() {
        assertEquals(1_500, controller.scoreSelection(listOf(1, 2, 3, 4, 5, 6)).points)
    }

    @Test
    fun scoreSelection_threePairs_scoresFifteenHundred() {
        assertEquals(1_500, controller.scoreSelection(listOf(2, 2, 3, 3, 4, 4)).points)
    }

    @Test
    fun scoreSelection_twoTriples_scoresTwentyFiveHundred() {
        assertEquals(2_500, controller.scoreSelection(listOf(2, 2, 2, 6, 6, 6)).points)
    }

    @Test
    fun detectZilch_nonScoringRoll_returnsTrue() {
        assertTrue(controller.detectZilch(listOf(2, 3, 4, 6)))
    }

    @Test
    fun bank_playerTurn_addsTurnPointsToPlayerBank() {
        val state = ZilchState.initial(config).copy(turnPoints = 650)
        assertEquals(650, controller.bank(state).playerBanked)
    }

    @Test
    fun setAside_allSixScoringDice_enablesHotDice() {
        val state = ZilchState.initial(config).copy(dice = listOf(1, 2, 3, 4, 5, 6))
        assertTrue(controller.setAside(state, setOf(0, 1, 2, 3, 4, 5)).locked.all { it })
    }

    @Test
    fun rollDice_afterHotDice_resetsLockedDice() {
        val state = ZilchState.initial(config).copy(dice = listOf(1, 2, 3, 4, 5, 6), locked = List(6) { true }, turnPoints = 1_500)
        assertFalse(controller.rollDice(state, Random(7)).locked.all { it })
    }

    @Test
    fun bank_reachingTarget_setsPlayerWin() {
        val state = ZilchState.initial(config.copy(winningTarget = 500)).copy(turnPoints = 500)
        assertEquals(ZilchResult.PLAYER_WIN, controller.bank(state).result)
    }

    @Test
    fun aiTurn_whenAiZilches_returnsControlToPlayer() {
        val state = ZilchState.initial(config).copy(currentPlayer = ZilchPlayer.AI)
        assertEquals(ZilchPlayer.PLAYER, controller.aiTurn(state, Random(2)).currentPlayer)
    }
}
