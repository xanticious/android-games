package com.xanticious.androidgames.games.missilecommand

import com.xanticious.androidgames.state.games.missilecommand.MissileCommandPhase
import com.xanticious.androidgames.state.games.missilecommand.MissileCommandStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class MissileCommandStateMachineTest {

    private fun machine() = MissileCommandStateMachine(CoroutineScope(Dispatchers.Unconfined))

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(MissileCommandPhase.IDLE, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // IDLE → WAVE_INTRO
    // -------------------------------------------------------------------------

    @Test
    fun gameStarted_fromIdle_movesToWaveIntro() {
        val m = machine()
        m.gameStarted()
        assertEquals(MissileCommandPhase.WAVE_INTRO, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // WAVE_INTRO → PLAYING
    // -------------------------------------------------------------------------

    @Test
    fun waveBegun_fromWaveIntro_movesToPlaying() {
        val m = machine()
        m.gameStarted()
        m.waveBegun()
        assertEquals(MissileCommandPhase.PLAYING, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // PLAYING → WAVE_TALLY
    // -------------------------------------------------------------------------

    @Test
    fun waveCleared_fromPlaying_movesToWaveTally() {
        val m = machine()
        m.gameStarted()
        m.waveBegun()
        m.waveCleared()
        assertEquals(MissileCommandPhase.WAVE_TALLY, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // PLAYING → GAME_OVER
    // -------------------------------------------------------------------------

    @Test
    fun allCitiesDestroyed_fromPlaying_movesToGameOver() {
        val m = machine()
        m.gameStarted()
        m.waveBegun()
        m.allCitiesDestroyed()
        assertEquals(MissileCommandPhase.GAME_OVER, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // WAVE_TALLY → WAVE_INTRO
    // -------------------------------------------------------------------------

    @Test
    fun tallyComplete_fromWaveTally_movesToWaveIntro() {
        val m = machine()
        m.gameStarted()
        m.waveBegun()
        m.waveCleared()
        m.tallyComplete()
        assertEquals(MissileCommandPhase.WAVE_INTRO, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // WAVE_TALLY → GAME_OVER (player won)
    // -------------------------------------------------------------------------

    @Test
    fun gameWon_fromWaveTally_movesToGameOver() {
        val m = machine()
        m.gameStarted()
        m.waveBegun()
        m.waveCleared()
        m.gameWon()
        assertEquals(MissileCommandPhase.GAME_OVER, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // GAME_OVER → WAVE_INTRO (replay)
    // -------------------------------------------------------------------------

    @Test
    fun replay_fromGameOverAfterCityDestruction_movesToWaveIntro() {
        val m = machine()
        m.gameStarted()
        m.waveBegun()
        m.allCitiesDestroyed()
        m.replay()
        assertEquals(MissileCommandPhase.WAVE_INTRO, m.phase.value)
    }

    @Test
    fun replay_fromGameOverAfterWin_movesToWaveIntro() {
        val m = machine()
        m.gameStarted()
        m.waveBegun()
        m.waveCleared()
        m.gameWon()
        m.replay()
        assertEquals(MissileCommandPhase.WAVE_INTRO, m.phase.value)
    }

    // -------------------------------------------------------------------------
    // Full wave cycle
    // -------------------------------------------------------------------------

    @Test
    fun fullWaveCycle_endBackAtPlaying() {
        val m = machine()
        m.gameStarted()   // → WAVE_INTRO
        m.waveBegun()     // → PLAYING
        m.waveCleared()   // → WAVE_TALLY
        m.tallyComplete() // → WAVE_INTRO
        m.waveBegun()     // → PLAYING
        assertEquals(MissileCommandPhase.PLAYING, m.phase.value)
    }
}
