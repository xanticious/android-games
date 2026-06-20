package com.xanticious.androidgames.games.snakes2d

import com.xanticious.androidgames.state.games.snakes2d.SnakesPhase
import com.xanticious.androidgames.state.games.snakes2d.SnakesStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class SnakesStateMachineTest {

    private fun machine() = SnakesStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(SnakesPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_transitionsFromIdleToPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(SnakesPhase.PLAYING, m.phase.value)
    }

    @Test
    fun collision_transitionsFromPlayingToDead() {
        val m = machine()
        m.startGame()
        m.collision()
        assertEquals(SnakesPhase.DEAD, m.phase.value)
    }

    @Test
    fun restart_transitionsFromDeadToPlaying() {
        val m = machine()
        m.startGame()
        m.collision()
        m.restart()
        assertEquals(SnakesPhase.PLAYING, m.phase.value)
    }

    @Test
    fun multipleCollisionsThenRestart_landOnPlaying() {
        val m = machine()
        m.startGame()
        m.collision()
        m.restart()
        m.collision()
        m.restart()
        assertEquals(SnakesPhase.PLAYING, m.phase.value)
    }

    @Test
    fun phaseAfterStartGame_isPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(SnakesPhase.PLAYING, m.phase.value)
    }

    @Test
    fun phaseAfterCollision_isDead() {
        val m = machine()
        m.startGame()
        m.collision()
        assertEquals(SnakesPhase.DEAD, m.phase.value)
    }

    @Test
    fun phaseAfterRestart_isPlaying() {
        val m = machine()
        m.startGame()
        m.collision()
        m.restart()
        assertEquals(SnakesPhase.PLAYING, m.phase.value)
    }
}
