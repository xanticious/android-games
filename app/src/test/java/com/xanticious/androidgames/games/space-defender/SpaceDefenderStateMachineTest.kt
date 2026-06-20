package com.xanticious.androidgames.games.spacedefender

import com.xanticious.androidgames.state.games.spacedefender.SpaceDefenderPhase
import com.xanticious.androidgames.state.games.spacedefender.SpaceDefenderStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceDefenderStateMachineTest {

    private fun machine() = SpaceDefenderStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(SpaceDefenderPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_transitionsToWaveIntro() {
        val m = machine()
        m.startGame()
        assertEquals(SpaceDefenderPhase.WAVE_INTRO, m.phase.value)
    }

    @Test
    fun introComplete_transitionsToPlaying() {
        val m = machine()
        m.startGame()
        m.introComplete()
        assertEquals(SpaceDefenderPhase.PLAYING, m.phase.value)
    }

    @Test
    fun allEnemiesDestroyed_transitionsToWaveComplete() {
        val m = machine()
        m.startGame()
        m.introComplete()
        m.allEnemiesDestroyed()
        assertEquals(SpaceDefenderPhase.WAVE_COMPLETE, m.phase.value)
    }

    @Test
    fun nextWave_transitionsFromWaveCompleteToWaveIntro() {
        val m = machine()
        m.startGame()
        m.introComplete()
        m.allEnemiesDestroyed()
        m.nextWave()
        assertEquals(SpaceDefenderPhase.WAVE_INTRO, m.phase.value)
    }

    @Test
    fun playerDied_transitionsToGameOver() {
        val m = machine()
        m.startGame()
        m.introComplete()
        m.playerDied()
        assertEquals(SpaceDefenderPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun fullLoop_twoWaves_endsAtPlaying() {
        val m = machine()
        m.startGame()       // IDLE → WAVE_INTRO
        m.introComplete()   // WAVE_INTRO → PLAYING
        m.allEnemiesDestroyed() // PLAYING → WAVE_COMPLETE
        m.nextWave()        // WAVE_COMPLETE → WAVE_INTRO
        m.introComplete()   // WAVE_INTRO → PLAYING
        assertEquals(SpaceDefenderPhase.PLAYING, m.phase.value)
    }

    @Test
    fun playerDied_fromPlaying_reachesGameOver() {
        val m = machine()
        m.startGame()
        m.introComplete()
        m.playerDied()
        assertEquals(SpaceDefenderPhase.GAME_OVER, m.phase.value)
    }
}
