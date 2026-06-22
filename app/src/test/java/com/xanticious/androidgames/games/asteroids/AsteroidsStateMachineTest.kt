package com.xanticious.androidgames.games.asteroids

import com.xanticious.androidgames.state.games.asteroids.AsteroidsPhase
import com.xanticious.androidgames.state.games.asteroids.AsteroidsStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class AsteroidsStateMachineTest {
    private fun machine() = AsteroidsStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(AsteroidsPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_movesToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(AsteroidsPhase.SETUP, m.phase.value)
    }

    @Test
    fun openHowToPlay_fromSetup_movesToHowToPlay() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        assertEquals(AsteroidsPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun backToSetup_fromHowToPlay_movesToSetup() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        m.backToSetup()
        assertEquals(AsteroidsPhase.SETUP, m.phase.value)
    }

    @Test
    fun confirmConfig_movesToSpawning() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        assertEquals(AsteroidsPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun fieldReady_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        assertEquals(AsteroidsPhase.PLAYING, m.phase.value)
    }

    @Test
    fun gameEnded_fromPlaying_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        m.gameEnded()
        assertEquals(AsteroidsPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun allBeaconsCollected_movesToLevelComplete() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        m.allBeaconsCollected()
        assertEquals(AsteroidsPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun nextLevel_movesFromLevelCompleteToSpawning() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        m.allBeaconsCollected()
        m.nextLevel()
        assertEquals(AsteroidsPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun gameEnded_fromLevelComplete_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        m.allBeaconsCollected()
        m.gameEnded()
        assertEquals(AsteroidsPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun fieldReadyAfterNextLevel_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        m.allBeaconsCollected()
        m.nextLevel()
        m.fieldReady()
        assertEquals(AsteroidsPhase.PLAYING, m.phase.value)
    }

    @Test
    fun retry_movesFromGameOverToSpawning() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        m.gameEnded()
        m.retry()
        assertEquals(AsteroidsPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun fullFlow_twoLevelsThenGameOver_reachesGameOver() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.fieldReady()
        m.allBeaconsCollected()
        m.nextLevel()
        m.fieldReady()
        m.gameEnded()
        assertEquals(AsteroidsPhase.GAME_OVER, m.phase.value)
    }
}
