package com.xanticious.androidgames.games.scrabble

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.scrabble.*
import com.xanticious.androidgames.state.games.scrabble.ScrabblePhase
import com.xanticious.androidgames.state.games.scrabble.ScrabbleStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ScrabbleStateMachineTest {

    private val testWordData = WordData(
        listOf("CAT", "DOG", "CATS", "DOGS", "AT", "TO", "GO")
    )

    private fun machine() = ScrabbleStateMachine(
        CoroutineScope(Dispatchers.Unconfined),
        Random(42)
    )

    @Test
    fun initialPhase_isSetup() {
        val m = machine()
        assertEquals(ScrabblePhase.SETUP, m.phase.value)
    }

    @Test
    fun startGame_movesToPlaying() {
        val m = machine()
        m.startGame(ScrabbleDifficulty.MEDIUM, testWordData)
        assertEquals(ScrabblePhase.PLAYING, m.phase.value)
    }

    @Test
    fun startGame_dealsRacks() {
        val m = machine()
        m.startGame(ScrabbleDifficulty.MEDIUM, testWordData)
        val state = m.gameState.value
        assertEquals(7, state.playerRack.size)
        assertEquals(7, state.aiRack.size)
        assertEquals(86, state.bag.size)
    }

    @Test
    fun startGame_humanGoesFirst() {
        val m = machine()
        m.startGame(ScrabbleDifficulty.MEDIUM, testWordData)
        val state = m.gameState.value
        assertEquals(ScrabbleGameState.Player.HUMAN, state.currentTurn)
    }

    @Test
    fun placeTile_addsTentative() {
        val m = machine()
        m.startGame(ScrabbleDifficulty.MEDIUM, testWordData)
        val tile = m.gameState.value.playerRack.first()
        m.userPlaceTile(Position(7, 7), tile)
        val state = m.gameState.value
        assertEquals(1, state.tentativeTiles.size)
        assertEquals(6, state.playerRack.size)
    }

    @Test
    fun recallTiles_returnsTentativeToRack() {
        val m = machine()
        m.startGame(ScrabbleDifficulty.MEDIUM, testWordData)
        val tile = m.gameState.value.playerRack.first()
        m.userPlaceTile(Position(7, 7), tile)
        m.userRecallTiles()
        val state = m.gameState.value
        assertTrue(state.tentativeTiles.isEmpty())
        assertEquals(7, state.playerRack.size)
    }

    @Test
    fun shuffleRack_changesOrder() {
        val m = machine()
        m.startGame(ScrabbleDifficulty.MEDIUM, testWordData)
        val before = m.gameState.value.playerRack.toList()
        m.userShuffleRack()
        val after = m.gameState.value.playerRack
        assertEquals(before.size, after.size)
        assertEquals(before.toSet(), after.toSet())
    }

    @Test
    fun pass_switchesToAi() {
        val m = machine()
        m.startGame(ScrabbleDifficulty.MEDIUM, testWordData)
        m.pass()
        assertEquals(ScrabblePhase.AI_THINKING, m.phase.value)
    }
}
