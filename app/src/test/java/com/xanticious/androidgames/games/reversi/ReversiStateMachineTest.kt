package com.xanticious.androidgames.games.reversi

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.state.games.reversi.ReversiPhase
import com.xanticious.androidgames.state.games.reversi.ReversiStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class ReversiStateMachineTest {
    private fun machine() = ReversiStateMachine(
        difficulty = GameDifficulty.EASY,
        random = Random(1),
        scope = CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun initialPhase_startsWithPlayerTurn() {
        assertEquals(ReversiPhase.PLAYER_TURN, machine().phase.value)
    }

    @Test
    fun playerMoveSelected_validOpening_entersAiTurn() {
        val m = machine()
        m.playerMoveSelected(2, 3)
        assertEquals(ReversiPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun performAiTurn_afterPlayerMove_returnsToPlayerTurn() {
        val m = machine()
        m.playerMoveSelected(2, 3)
        m.performAiTurn()
        assertEquals(ReversiPhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun reset_afterMove_restoresPlayerTurn() {
        val m = machine()
        m.playerMoveSelected(2, 3)
        m.reset()
        assertEquals(ReversiPhase.PLAYER_TURN, m.phase.value)
    }
}
