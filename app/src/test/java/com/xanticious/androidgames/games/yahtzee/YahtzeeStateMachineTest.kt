package com.xanticious.androidgames.games.yahtzee

import com.xanticious.androidgames.state.games.yahtzee.YahtzeePhase
import com.xanticious.androidgames.state.games.yahtzee.YahtzeeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class YahtzeeStateMachineTest {
    private fun machine() = YahtzeeStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startMatch_movesToPlayerRolling() {
        val m = machine()
        m.startMatch()
        assertEquals(YahtzeePhase.PLAYER_ROLLING, m.phase.value)
    }

    @Test
    fun diceRolled_noRollsLeft_movesToPlayerScoring() {
        val m = machine()
        m.startMatch()
        m.diceRolled(0)
        assertEquals(YahtzeePhase.PLAYER_SCORING, m.phase.value)
    }

    @Test
    fun categorySelected_gameContinues_movesToAiTurn() {
        val m = machine()
        m.startMatch()
        m.categorySelected(gameOver = false)
        assertEquals(YahtzeePhase.AI_TURN, m.phase.value)
    }

    @Test
    fun aiFinished_gameContinues_movesToPlayerRolling() {
        val m = machine()
        m.startMatch()
        m.categorySelected(gameOver = false)
        m.aiFinished(gameOver = false)
        assertEquals(YahtzeePhase.PLAYER_ROLLING, m.phase.value)
    }

    @Test
    fun categorySelected_gameOver_movesToGameOver() {
        val m = machine()
        m.startMatch()
        m.categorySelected(gameOver = true)
        assertEquals(YahtzeePhase.GAME_OVER, m.phase.value)
    }
}
