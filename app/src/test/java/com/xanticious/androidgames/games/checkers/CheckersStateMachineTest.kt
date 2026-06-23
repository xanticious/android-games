package com.xanticious.androidgames.games.checkers

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.checkers.CheckersSquare
import com.xanticious.androidgames.state.games.checkers.CheckersPhase
import com.xanticious.androidgames.state.games.checkers.CheckersStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class CheckersStateMachineTest {
    private fun machine() = CheckersStateMachine(scope = CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(CheckersPhase.IDLE, m.phase.value)
    }

    @Test
    fun startMatch_redPlayer_movesToPlayerTurn() {
        val m = machine()
        m.startMatch(GameDifficulty.MEDIUM)
        assertEquals(CheckersPhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun playerMove_turnEnds_movesToAiTurn() {
        val m = machine()
        m.startMatch(GameDifficulty.MEDIUM)
        m.tapSquare(CheckersSquare(5, 0))
        m.tapSquare(CheckersSquare(4, 1))
        assertEquals(CheckersPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun aiMove_turnEnds_movesToPlayerTurn() {
        val m = machine()
        m.startMatch(GameDifficulty.MEDIUM)
        m.tapSquare(CheckersSquare(5, 0))
        m.tapSquare(CheckersSquare(4, 1))
        m.playAiTurn(Random(3))
        assertEquals(CheckersPhase.PLAYER_TURN, m.phase.value)
    }
}
