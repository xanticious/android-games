package com.xanticious.androidgames.games.dotart

import com.xanticious.androidgames.state.games.dotart.DotArtPhase
import com.xanticious.androidgames.state.games.dotart.DotArtStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DotArtStateMachineTest {
    private fun machine() = DotArtStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startCanvas_movesToConnect() {
        val m = machine()
        m.startCanvas()
        assertEquals(DotArtPhase.CONNECT, m.phase.value)
    }

    @Test
    fun phase1Complete_movesToFill() {
        val m = machine()
        m.startCanvas()
        m.phase1Complete()
        assertEquals(DotArtPhase.FILL, m.phase.value)
    }

    @Test
    fun phase2Complete_movesToBrush() {
        val m = machine()
        m.startCanvas()
        m.phase1Complete()
        m.phase2Complete()
        assertEquals(DotArtPhase.BRUSH, m.phase.value)
    }

    @Test
    fun drawingDone_movesToFinished() {
        val m = machine()
        m.startCanvas()
        m.phase1Complete()
        m.phase2Complete()
        m.drawingDone()
        assertEquals(DotArtPhase.FINISHED, m.phase.value)
    }

    @Test
    fun backToMenu_fromFinished_movesToIdle() {
        val m = machine()
        m.startCanvas()
        m.phase1Complete()
        m.phase2Complete()
        m.drawingDone()
        m.backToMenu()
        assertEquals(DotArtPhase.IDLE, m.phase.value)
    }
}
