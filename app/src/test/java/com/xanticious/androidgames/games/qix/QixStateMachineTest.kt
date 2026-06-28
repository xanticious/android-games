package com.xanticious.androidgames.games.qix

import com.xanticious.androidgames.state.games.qix.QixPhase
import com.xanticious.androidgames.state.games.qix.QixStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class QixStateMachineTest {

    private fun machine() = QixStateMachine(CoroutineScope(Dispatchers.Unconfined))

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun initialPhase_isIdle() {
        assertEquals(QixPhase.IDLE, machine().phase.value)
    }

    // ─── Idle → Playing ──────────────────────────────────────────────────────

    @Test
    fun startGame_transitionsFromIdleToPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(QixPhase.PLAYING, m.phase.value)
    }

    // ─── Playing → LifeLost ───────────────────────────────────────────────────

    @Test
    fun collisionOccurred_fromPlaying_transitionsToLifeLost() {
        val m = machine()
        m.startGame()
        m.collisionOccurred()
        assertEquals(QixPhase.LIFE_LOST, m.phase.value)
    }

    // ─── LifeLost → Playing (respawn) ────────────────────────────────────────

    @Test
    fun respawned_fromLifeLost_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.collisionOccurred()
        m.respawned()
        assertEquals(QixPhase.PLAYING, m.phase.value)
    }

    // ─── LifeLost → GameOver ─────────────────────────────────────────────────

    @Test
    fun livesExhausted_fromLifeLost_transitionsToGameOver() {
        val m = machine()
        m.startGame()
        m.collisionOccurred()
        m.livesExhausted()
        assertEquals(QixPhase.GAME_OVER, m.phase.value)
    }

    // ─── Playing → LevelComplete ─────────────────────────────────────────────

    @Test
    fun levelAchieved_fromPlaying_transitionsToLevelComplete() {
        val m = machine()
        m.startGame()
        m.levelAchieved()
        assertEquals(QixPhase.LEVEL_COMPLETE, m.phase.value)
    }

    // ─── Playing → GameOver (direct path, last life on death) ────────────────

    @Test
    fun livesExhausted_directlyFromPlaying_transitionsToGameOver() {
        val m = machine()
        m.startGame()
        m.livesExhausted()
        assertEquals(QixPhase.GAME_OVER, m.phase.value)
    }

    // ─── Multiple collisions and respawns ─────────────────────────────────────

    @Test
    fun multipleRespawns_returnToPlayingEachTime() {
        val m = machine()
        m.startGame()
        repeat(2) {
            m.collisionOccurred()
            m.respawned()
        }
        assertEquals(QixPhase.PLAYING, m.phase.value)
    }
}
