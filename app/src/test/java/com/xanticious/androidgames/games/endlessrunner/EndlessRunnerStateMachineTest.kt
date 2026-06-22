package com.xanticious.androidgames.games.endlessrunner

import com.xanticious.androidgames.state.games.endlessrunner.EndlessRunnerStateMachine
import com.xanticious.androidgames.state.games.endlessrunner.RunnerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class EndlessRunnerStateMachineTest {

    private fun machine() = EndlessRunnerStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        assertEquals(RunnerPhase.IDLE, machine().phase.value)
    }

    @Test
    fun startRun_transitionsFromIdleToRunning() {
        val m = machine()
        m.startRun()
        assertEquals(RunnerPhase.RUNNING, m.phase.value)
    }

    @Test
    fun runnerDied_transitionsFromRunningToDead() {
        val m = machine()
        m.startRun()
        m.runnerDied()
        assertEquals(RunnerPhase.DEAD, m.phase.value)
    }

    @Test
    fun restart_transitionsFromDeadToIdle() {
        val m = machine()
        m.startRun()
        m.runnerDied()
        m.restart()
        assertEquals(RunnerPhase.IDLE, m.phase.value)
    }
}
