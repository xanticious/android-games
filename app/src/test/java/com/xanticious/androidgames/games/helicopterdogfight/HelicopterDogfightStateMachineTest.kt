package com.xanticious.androidgames.games.helicopterdogfight

import com.xanticious.androidgames.state.games.helicopterdogfight.HelicopterDogfightPhase
import com.xanticious.androidgames.state.games.helicopterdogfight.HelicopterDogfightStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class HelicopterDogfightStateMachineTest {

    private fun machine() = HelicopterDogfightStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(HelicopterDogfightPhase.IDLE, m.phase.value)
    }

    @Test
    fun gameStart_movesToSpawning() {
        val m = machine()
        m.startGame()
        assertEquals(HelicopterDogfightPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun screenReady_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.screenReady()
        assertEquals(HelicopterDogfightPhase.PLAYING, m.phase.value)
    }

    @Test
    fun allEnemiesDestroyed_movesToScreenClear() {
        val m = machine()
        m.startGame()
        m.screenReady()
        m.allEnemiesDestroyed()
        assertEquals(HelicopterDogfightPhase.SCREEN_CLEAR, m.phase.value)
    }

    @Test
    fun allClearDelayOver_movesToScrolling() {
        val m = machine()
        m.startGame()
        m.screenReady()
        m.allEnemiesDestroyed()
        m.allClearDelayOver()
        assertEquals(HelicopterDogfightPhase.SCROLLING, m.phase.value)
    }

    @Test
    fun nextScreenReady_movesToSpawning() {
        val m = machine()
        m.startGame()
        m.screenReady()
        m.allEnemiesDestroyed()
        m.allClearDelayOver()
        m.nextScreenReady()
        assertEquals(HelicopterDogfightPhase.SPAWNING, m.phase.value)
    }

    @Test
    fun playerCrashed_movesToRespawning() {
        val m = machine()
        m.startGame()
        m.screenReady()
        m.playerCrashed()
        assertEquals(HelicopterDogfightPhase.RESPAWNING, m.phase.value)
    }

    @Test
    fun respawnComplete_movesBackToPlaying() {
        val m = machine()
        m.startGame()
        m.screenReady()
        m.playerCrashed()
        m.respawnComplete()
        assertEquals(HelicopterDogfightPhase.PLAYING, m.phase.value)
    }

    @Test
    fun playerDied_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.screenReady()
        m.playerDied()
        assertEquals(HelicopterDogfightPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun fullLoop_twoScreens_endsAtPlaying() {
        val m = machine()
        m.startGame()           // IDLE → SPAWNING
        m.screenReady()         // SPAWNING → PLAYING
        m.allEnemiesDestroyed() // PLAYING → SCREEN_CLEAR
        m.allClearDelayOver()   // SCREEN_CLEAR → SCROLLING
        m.nextScreenReady()     // SCROLLING → SPAWNING
        m.screenReady()         // SPAWNING → PLAYING
        assertEquals(HelicopterDogfightPhase.PLAYING, m.phase.value)
    }

    @Test
    fun respawnThenClearScreen_reachesScreenClear() {
        val m = machine()
        m.startGame()
        m.screenReady()
        m.playerCrashed()       // PLAYING → RESPAWNING
        m.respawnComplete()     // RESPAWNING → PLAYING
        m.allEnemiesDestroyed() // PLAYING → SCREEN_CLEAR
        assertEquals(HelicopterDogfightPhase.SCREEN_CLEAR, m.phase.value)
    }
}
