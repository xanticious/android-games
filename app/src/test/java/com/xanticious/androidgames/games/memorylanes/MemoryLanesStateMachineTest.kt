package com.xanticious.androidgames.games.memorylanes

import com.xanticious.androidgames.state.games.memorylanes.MemoryLanesPhase
import com.xanticious.androidgames.state.games.memorylanes.MemoryLanesStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryLanesStateMachineTest {
    private fun machine() = MemoryLanesStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToRevealing() {
        val m = machine()
        m.startGame()
        assertEquals(MemoryLanesPhase.REVEALING, m.phase.value)
    }

    @Test
    fun revealComplete_movesToBuilding() {
        val m = machine()
        m.startGame()
        m.revealComplete()
        assertEquals(MemoryLanesPhase.BUILDING, m.phase.value)
    }

    @Test
    fun doneSubmitted_movesToValidating() {
        val m = machine()
        m.startGame()
        m.revealComplete()
        m.doneSubmitted()
        assertEquals(MemoryLanesPhase.VALIDATING, m.phase.value)
    }

    @Test
    fun sequenceWrong_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.revealComplete()
        m.doneSubmitted()
        m.sequenceWrong()
        assertEquals(MemoryLanesPhase.GAME_OVER, m.phase.value)
    }
}
