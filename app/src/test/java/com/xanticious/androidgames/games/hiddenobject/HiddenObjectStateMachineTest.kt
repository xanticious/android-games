package com.xanticious.androidgames.games.hiddenobject

import com.xanticious.androidgames.state.games.hiddenobject.HiddenObjectPhase
import com.xanticious.androidgames.state.games.hiddenobject.HiddenObjectStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenObjectStateMachineTest {
    private fun machine() = HiddenObjectStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(HiddenObjectPhase.SETUP, m.phase.value)
    }

    @Test
    fun confirmSettings_movesToGenerating() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        assertEquals(HiddenObjectPhase.GENERATING, m.phase.value)
    }

    @Test
    fun sceneReady_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        m.sceneReady()
        assertEquals(HiddenObjectPhase.PLAYING, m.phase.value)
    }

    @Test
    fun objectFound_movesToRoundComplete() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        m.sceneReady()
        m.objectFound()
        assertEquals(HiddenObjectPhase.ROUND_COMPLETE, m.phase.value)
    }

    @Test
    fun retry_fromGameOver_movesToGenerating() {
        val m = machine()
        m.startGame()
        m.confirmSettings()
        m.sceneReady()
        m.timerExpired()
        m.retry()
        assertEquals(HiddenObjectPhase.GENERATING, m.phase.value)
    }
}
