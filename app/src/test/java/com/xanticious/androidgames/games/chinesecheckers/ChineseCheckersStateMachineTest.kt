package com.xanticious.androidgames.games.chinesecheckers

import com.xanticious.androidgames.controller.games.chinesecheckers.ChineseCheckersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersSide
import com.xanticious.androidgames.state.games.chinesecheckers.ChineseCheckersPhase
import com.xanticious.androidgames.state.games.chinesecheckers.ChineseCheckersStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.random.Random

class ChineseCheckersStateMachineTest {
    private fun machine() = ChineseCheckersStateMachine(
        difficulty = GameDifficulty.MEDIUM,
        scope = CoroutineScope(Dispatchers.Unconfined),
        random = Random(3)
    )

    @Test
    fun initialPhase_startsAtPlayerTurn() {
        assertEquals(ChineseCheckersPhase.PLAYER_TURN, machine().phase.value)
    }

    @Test
    fun selectPeg_playerMarble_updatesSelectedPeg() {
        val m = machine()
        val move = ChineseCheckersController().legalMoves(m.state.value).first()
        m.selectPeg(move.from)
        assertEquals(move.from, m.state.value.selectedPeg)
    }

    @Test
    fun chooseDestination_afterPlayerMove_runsAiAndReturnsToPlayer() {
        val m = machine()
        val move = ChineseCheckersController().legalMoves(m.state.value).first()
        m.selectPeg(move.from)
        m.chooseDestination(move.to)
        assertEquals(ChineseCheckersPhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun chooseDestination_afterAiResponse_currentPlayerIsPlayer() {
        val m = machine()
        val move = ChineseCheckersController().legalMoves(m.state.value).first()
        m.selectPeg(move.from)
        m.chooseDestination(move.to)
        assertEquals(ChineseCheckersSide.PLAYER, m.state.value.currentPlayer)
    }
}
