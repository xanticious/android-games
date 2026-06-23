package com.xanticious.androidgames.games.memory

import com.xanticious.androidgames.state.games.memory.MemoryPhase
import com.xanticious.androidgames.state.games.memory.MemoryStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryStateMachineTest {
    private fun machine() = MemoryStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun firstCardFlipped_movesToSecondFlip() {
        val m = machine()
        m.firstCardFlipped()
        assertEquals(MemoryPhase.SECOND_FLIP, m.phase.value)
    }

    @Test
    fun secondCardFlipped_movesToResolving() {
        val m = machine()
        m.firstCardFlipped()
        m.secondCardFlipped()
        assertEquals(MemoryPhase.RESOLVING, m.phase.value)
    }

    @Test
    fun resolvingAiTurnStarted_movesToAiTurn() {
        val m = machine()
        m.firstCardFlipped()
        m.secondCardFlipped()
        m.aiTurnStarted()
        assertEquals(MemoryPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun aiCardsChosen_movesToResolving() {
        val m = machine()
        m.aiTurnStarted()
        m.aiCardsChosen()
        assertEquals(MemoryPhase.RESOLVING, m.phase.value)
    }

    @Test
    fun gameFinished_movesToGameOver() {
        val m = machine()
        m.aiTurnStarted()
        m.gameFinished()
        assertEquals(MemoryPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun rematch_fromGameOver_movesToFirstFlip() {
        val m = machine()
        m.aiTurnStarted()
        m.gameFinished()
        m.rematch()
        assertEquals(MemoryPhase.FIRST_FLIP, m.phase.value)
    }
}
