package com.xanticious.androidgames.games.bubblespop

import com.xanticious.androidgames.model.games.bubblespop.BubblesVariant
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopPhase
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class BubblesPopStateMachineTest {

    private fun turnBased() = BubblesPopStateMachine(
        BubblesVariant.TURN_BASED, CoroutineScope(Dispatchers.Unconfined)
    )

    private fun arcade() = BubblesPopStateMachine(
        BubblesVariant.ARCADE, CoroutineScope(Dispatchers.Unconfined)
    )

    private fun snake() = BubblesPopStateMachine(
        BubblesVariant.SNAKE_ARCADE, CoroutineScope(Dispatchers.Unconfined)
    )

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun initial_phase_isIdle() {
        assertEquals(BubblesPopPhase.IDLE, turnBased().phase.value)
    }

    @Test
    fun initial_arcade_phase_isIdle() {
        assertEquals(BubblesPopPhase.IDLE, arcade().phase.value)
    }

    // ─── Turn-based transitions ───────────────────────────────────────────────

    @Test
    fun startGame_turnBased_movesToAim() {
        val m = turnBased()
        m.startGame()
        assertEquals(BubblesPopPhase.AIM, m.phase.value)
    }

    @Test
    fun bubbleFired_fromAim_movesToFire() {
        val m = turnBased()
        m.startGame()
        m.bubbleFired()
        assertEquals(BubblesPopPhase.FIRE, m.phase.value)
    }

    @Test
    fun bubbleResolved_fromFire_movesToAim() {
        val m = turnBased()
        m.startGame()
        m.bubbleFired()
        m.bubbleResolved()
        assertEquals(BubblesPopPhase.AIM, m.phase.value)
    }

    @Test
    fun clusterCleared_fromAim_movesToLevelComplete() {
        val m = turnBased()
        m.startGame()
        m.clusterCleared()
        assertEquals(BubblesPopPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun clusterCleared_fromFire_movesToLevelComplete() {
        val m = turnBased()
        m.startGame()
        m.bubbleFired()
        m.clusterCleared()
        assertEquals(BubblesPopPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun gameOver_fromAim_movesToGameOver() {
        val m = turnBased()
        m.startGame()
        m.gameOver()
        assertEquals(BubblesPopPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun gameOver_fromFire_movesToGameOver() {
        val m = turnBased()
        m.startGame()
        m.bubbleFired()
        m.gameOver()
        assertEquals(BubblesPopPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun nextLevel_turnBased_fromLevelComplete_movesToAim() {
        val m = turnBased()
        m.startGame()
        m.clusterCleared()
        m.nextLevel()
        assertEquals(BubblesPopPhase.AIM, m.phase.value)
    }

    @Test
    fun resetGame_turnBased_fromGameOver_movesToIdle() {
        val m = turnBased()
        m.startGame()
        m.gameOver()
        m.resetGame()
        assertEquals(BubblesPopPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_afterReset_movesToAim() {
        val m = turnBased()
        m.startGame()
        m.gameOver()
        m.resetGame()
        m.startGame()
        assertEquals(BubblesPopPhase.AIM, m.phase.value)
    }

    // ─── Arcade transitions ───────────────────────────────────────────────────

    @Test
    fun startGame_arcade_movesToPlaying() {
        val m = arcade()
        m.startGame()
        assertEquals(BubblesPopPhase.PLAYING, m.phase.value)
    }

    @Test
    fun lifeLost_arcade_fromPlaying_movesToLifeLost() {
        val m = arcade()
        m.startGame()
        m.lifeLost()
        assertEquals(BubblesPopPhase.LIFE_LOST, m.phase.value)
    }

    @Test
    fun lifeReset_arcade_fromLifeLost_movesToPlaying() {
        val m = arcade()
        m.startGame()
        m.lifeLost()
        m.lifeReset()
        assertEquals(BubblesPopPhase.PLAYING, m.phase.value)
    }

    @Test
    fun clusterCleared_arcade_movesToLevelComplete() {
        val m = arcade()
        m.startGame()
        m.clusterCleared()
        assertEquals(BubblesPopPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun nextLevel_arcade_movesToPlaying() {
        val m = arcade()
        m.startGame()
        m.clusterCleared()
        m.nextLevel()
        assertEquals(BubblesPopPhase.PLAYING, m.phase.value)
    }

    @Test
    fun gameOver_arcade_fromPlaying_movesToGameOver() {
        val m = arcade()
        m.startGame()
        m.gameOver()
        assertEquals(BubblesPopPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun gameOver_arcade_fromLifeLost_movesToGameOver() {
        val m = arcade()
        m.startGame()
        m.lifeLost()
        m.gameOver()
        assertEquals(BubblesPopPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun resetGame_arcade_fromGameOver_movesToIdle() {
        val m = arcade()
        m.startGame()
        m.gameOver()
        m.resetGame()
        assertEquals(BubblesPopPhase.IDLE, m.phase.value)
    }

    // ─── Snake-arcade transitions ─────────────────────────────────────────────

    @Test
    fun startGame_snake_movesToPlaying() {
        val m = snake()
        m.startGame()
        assertEquals(BubblesPopPhase.PLAYING, m.phase.value)
    }

    @Test
    fun lifeLost_snake_movesToLifeLost() {
        val m = snake()
        m.startGame()
        m.lifeLost()
        assertEquals(BubblesPopPhase.LIFE_LOST, m.phase.value)
    }

    @Test
    fun lifeReset_snake_movesToPlaying() {
        val m = snake()
        m.startGame()
        m.lifeLost()
        m.lifeReset()
        assertEquals(BubblesPopPhase.PLAYING, m.phase.value)
    }

    @Test
    fun clusterCleared_snake_movesToLevelComplete() {
        val m = snake()
        m.startGame()
        m.clusterCleared()
        assertEquals(BubblesPopPhase.LEVEL_COMPLETE, m.phase.value)
    }

    @Test
    fun nextLevel_snake_movesToPlaying() {
        val m = snake()
        m.startGame()
        m.clusterCleared()
        m.nextLevel()
        assertEquals(BubblesPopPhase.PLAYING, m.phase.value)
    }

    @Test
    fun resetGame_snake_fromIdle_staysIdle() {
        val m = snake()
        m.resetGame()
        assertEquals(BubblesPopPhase.IDLE, m.phase.value)
    }
}
