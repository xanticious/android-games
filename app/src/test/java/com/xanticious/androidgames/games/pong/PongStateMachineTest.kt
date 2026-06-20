package com.xanticious.androidgames.games.pong

import com.xanticious.androidgames.state.games.pong.PongPhase
import com.xanticious.androidgames.state.games.pong.PongStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class PongStateMachineTest {
    private fun machine() = PongStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startMatch_movesToServing() {
        val m = machine()
        m.startMatch()
        assertEquals(PongPhase.SERVING, m.phase.value)
    }

    @Test
    fun serveThenPoint_movesToPointScored() {
        val m = machine()
        m.startMatch()
        m.ballServed()
        m.pointEnded()
        assertEquals(PongPhase.POINT_SCORED, m.phase.value)
    }

    @Test
    fun setEndedThenMatchEnded_reachesMatchOver() {
        val m = machine()
        m.startMatch()
        m.ballServed()
        m.pointEnded()
        m.setEnded()
        m.matchEnded()
        assertEquals(PongPhase.MATCH_OVER, m.phase.value)
    }
}
