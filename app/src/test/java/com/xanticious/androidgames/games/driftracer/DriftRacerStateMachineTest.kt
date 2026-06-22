package com.xanticious.androidgames.games.driftracer

import com.xanticious.androidgames.state.games.driftracer.DriftRacerPhase
import com.xanticious.androidgames.state.games.driftracer.DriftRacerStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DriftRacerStateMachineTest {

    private fun machine() = DriftRacerStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startRace_movesToCountdown() {
        val m = machine()
        m.startRace()
        assertEquals(DriftRacerPhase.COUNTDOWN, m.phase.value)
    }

    @Test
    fun countdownComplete_movesToRacing() {
        val m = machine()
        m.startRace()
        m.countdownComplete()
        assertEquals(DriftRacerPhase.RACING, m.phase.value)
    }

    @Test
    fun raceFinished_movesToRaceFinished() {
        val m = machine()
        m.startRace()
        m.countdownComplete()
        m.raceFinished()
        assertEquals(DriftRacerPhase.RACE_FINISHED, m.phase.value)
    }

    @Test
    fun retry_movesBackToCountdown() {
        val m = machine()
        m.startRace()
        m.countdownComplete()
        m.raceFinished()
        m.retry()
        assertEquals(DriftRacerPhase.COUNTDOWN, m.phase.value)
    }
}
