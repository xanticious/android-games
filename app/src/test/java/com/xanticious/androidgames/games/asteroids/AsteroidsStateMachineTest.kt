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
    fun startGame_movesToSpawning() {
        val m = machine()
        m.startGame()
        assertEquals(AsteroidsPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun fieldReady_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        assertEquals(AsteroidsPhase.PLAYING, m.phase.value)
    }

    @Test
    fun playerHitWithLives_movesToRespawning() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        m.playerHitWithLives()
        assertEquals(AsteroidsPhase.RESPAWNING, m.phase.value)
    }

    @Test
    fun respawnComplete_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        m.playerHitWithLives()
        m.respawnComplete()
        assertEquals(AsteroidsPhase.PLAYING, m.phase.value)
    }

    @Test
    fun playerDied_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        m.playerDied()
        assertEquals(AsteroidsPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun allBeaconsCollected_movesToLevelComplete() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        m.allBeaconsCollected()
        assertEquals(AsteroidsPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun nextLevel_movesFromLevelCompleteToSpawning() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        m.allBeaconsCollected()
        m.nextLevel()
        assertEquals(AsteroidsPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun fieldReadyAfterNextLevel_movesToPlaying() {
        val m = machine()
        m.startGame()
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
        m.fieldReady()
        m.playerDied()
        m.retry()
        assertEquals(AsteroidsPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun fullFlow_respawnTwiceThenDie_reachesGameOver() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        m.playerHitWithLives()
        m.respawnComplete()
        m.playerHitWithLives()
        m.respawnComplete()
        m.playerDied()
        assertEquals(AsteroidsPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun fullFlow_twoLevelsThenGameOver_reachesGameOver() {
        val m = machine()
        m.startGame()
        m.fieldReady()
        m.allBeaconsCollected()
        m.nextLevel()
        m.fieldReady()
        m.playerDied()
        assertEquals(AsteroidsPhase.GAME_OVER, m.phase.value)
    }
}
