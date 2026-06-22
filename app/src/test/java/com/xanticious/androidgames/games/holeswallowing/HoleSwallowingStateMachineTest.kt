package com.xanticious.androidgames.games.holeswallowing

import com.xanticious.androidgames.state.games.holeswallowing.HoleSwallowingPhase
import com.xanticious.androidgames.state.games.holeswallowing.HoleSwallowingStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class HoleSwallowingStateMachineTest {

    private fun machine() = HoleSwallowingStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun idle_startLevel_movesToPlaying() {
        val m = machine()
        m.startLevel()
        assertEquals(HoleSwallowingPhase.PLAYING, m.phase.value)
    }

    @Test
    fun playing_targetReached_movesToLevelComplete() {
        val m = machine()
        m.startLevel()
        m.targetReached()
        assertEquals(HoleSwallowingPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun playing_timerExpired_movesToGameOver() {
        val m = machine()
        m.startLevel()
        m.timerExpired()
        assertEquals(HoleSwallowingPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun levelComplete_replay_movesToIdle() {
        val m = machine()
        m.startLevel()
        m.targetReached()
        m.replay()
        assertEquals(HoleSwallowingPhase.IDLE, m.phase.value)
    }

    @Test
    fun gameOver_replay_movesToIdle() {
        val m = machine()
        m.startLevel()
        m.timerExpired()
        m.replay()
        assertEquals(HoleSwallowingPhase.IDLE, m.phase.value)
    }
}
