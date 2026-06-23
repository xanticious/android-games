package com.xanticious.androidgames.games.letterdrop

import com.xanticious.androidgames.controller.games.letterdrop.LetterDropController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.letterdrop.LetterDropEvent
import com.xanticious.androidgames.model.games.letterdrop.LetterDropState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LetterDropControllerTest {
    private val controller = LetterDropController()
    private val wordData = WordData(listOf("cat", "cats", "dog", "dogs", "rat", "rats", "cart", "carts"))

    @Test
    fun step_advancesTilesByDt() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, false)
        val state = LetterDropState.initial().copy(
            tiles = listOf(
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('a', 0.5f, 0.2f, 0, false)
            )
        )
        val result = controller.step(state, config, dt = 0.1f, Random.Default)
        assertTrue(result.state.tiles[0].y > 0.2f)
    }

    @Test
    fun step_spawnsNewTileAfterInterval() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, false)
        val state = LetterDropState.initial().copy(spawnTimer = 2.5f)
        val result = controller.step(state, config, dt = 0.1f, Random.Default)
        assertEquals(1, result.state.tiles.size)
    }

    @Test
    fun step_detectsOverflowWhenTileReachesTop() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, false)
        val state = LetterDropState.initial().copy(
            tiles = listOf(
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('a', 0.5f, 0.95f, 0, false)
            )
        )
        val result = controller.step(state, config, dt = 0.1f, Random.Default)
        assertEquals(LetterDropEvent.OVERFLOW, result.event)
        assertTrue(result.state.gameOver)
    }

    @Test
    fun queueTile_addsLetterToEntry() {
        val state = LetterDropState.initial().copy(
            tiles = listOf(
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('c', 0.5f, 0.5f, 0, false)
            )
        )
        val result = controller.queueTile(state, 0)
        assertEquals("c", result.currentEntry)
        assertTrue(result.tiles[0].isQueued)
    }

    @Test
    fun backspace_removesLastLetter() {
        val state = LetterDropState.initial().copy(
            tiles = listOf(
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('c', 0.5f, 0.5f, 0, true),
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('a', 0.6f, 0.6f, 1, true)
            ),
            currentEntry = "ca"
        )
        val result = controller.backspace(state)
        assertEquals("c", result.currentEntry)
        assertFalse(result.tiles[1].isQueued)
    }

    @Test
    fun clear_resetsEntryAndUnqueuesAllTiles() {
        val state = LetterDropState.initial().copy(
            tiles = listOf(
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('c', 0.5f, 0.5f, 0, true),
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('a', 0.6f, 0.6f, 1, true)
            ),
            currentEntry = "ca"
        )
        val result = controller.clear(state)
        assertEquals("", result.currentEntry)
        assertFalse(result.tiles.any { it.isQueued })
    }

    @Test
    fun submitWord_validWord_clearsTilesAndScores() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, false)
        val state = LetterDropState.initial().copy(
            tiles = listOf(
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('c', 0.5f, 0.5f, 0, true),
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('a', 0.6f, 0.6f, 1, true),
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('t', 0.7f, 0.7f, 2, true)
            ),
            currentEntry = "cat"
        )
        val result = controller.submitWord(state, config, wordData)
        assertEquals(LetterDropEvent.WORD_CLEARED, result.event)
        assertEquals(0, result.state.tiles.size)
        assertEquals(10, result.state.score)
        assertEquals(1, result.state.totalWordsFormed)
    }

    @Test
    fun submitWord_invalidWord_clearsEntry() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, false)
        val state = LetterDropState.initial().copy(
            tiles = listOf(
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('x', 0.5f, 0.5f, 0, true),
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('y', 0.6f, 0.6f, 1, true),
                com.xanticious.androidgames.model.games.letterdrop.FallingTile('z', 0.7f, 0.7f, 2, true)
            ),
            currentEntry = "xyz"
        )
        val result = controller.submitWord(state, config, wordData)
        assertEquals(LetterDropEvent.NONE, result.event)
        assertEquals("", result.state.currentEntry)
        assertEquals(0, result.state.score)
    }

    @Test
    fun scoreWord_scalesWithLength() {
        assertEquals(10, controller.scoreWord("cat"))
        assertEquals(20, controller.scoreWord("dogs"))
        assertEquals(40, controller.scoreWord("carts"))
    }
}
