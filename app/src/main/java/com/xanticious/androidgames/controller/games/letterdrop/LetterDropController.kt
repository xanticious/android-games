package com.xanticious.androidgames.controller.games.letterdrop

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.letterdrop.FallingTile
import com.xanticious.androidgames.model.games.letterdrop.LetterDropConfig
import com.xanticious.androidgames.model.games.letterdrop.LetterDropEvent
import com.xanticious.androidgames.model.games.letterdrop.LetterDropState
import com.xanticious.androidgames.model.games.letterdrop.LetterDropStep
import kotlin.random.Random

class LetterDropController {

    private val balancedWeights = mapOf(
        'e' to 12, 't' to 9, 'a' to 8, 'o' to 8, 'i' to 7, 'n' to 7, 's' to 6, 'h' to 6,
        'r' to 6, 'd' to 4, 'l' to 4, 'c' to 3, 'u' to 3, 'm' to 2, 'w' to 2, 'f' to 2,
        'g' to 2, 'y' to 2, 'p' to 2, 'b' to 2, 'v' to 1, 'k' to 1, 'j' to 1, 'x' to 1,
        'q' to 1, 'z' to 1
    )

    private val vowelRichWeights = mapOf(
        'a' to 15, 'e' to 15, 'i' to 12, 'o' to 12, 'u' to 8,
        't' to 5, 'n' to 5, 's' to 4, 'h' to 4, 'r' to 4, 'd' to 3, 'l' to 3, 'c' to 2,
        'm' to 2, 'w' to 2, 'f' to 2, 'g' to 2, 'y' to 2, 'p' to 2, 'b' to 2, 'v' to 1,
        'k' to 1, 'j' to 1, 'x' to 1, 'q' to 1, 'z' to 1
    )

    private val overflowY = 0.1f

    fun configFor(difficulty: GameDifficulty, minWordLength: Int, vowelRich: Boolean): LetterDropConfig = when (difficulty) {
        GameDifficulty.EASY -> LetterDropConfig(
            dropSpeed = 0.08f, spawnInterval = 2.5f, minWordLength = minWordLength, vowelRichness = if (vowelRich) 1f else 0f
        )
        GameDifficulty.MEDIUM -> LetterDropConfig(
            dropSpeed = 0.12f, spawnInterval = 2.0f, minWordLength = minWordLength, vowelRichness = if (vowelRich) 1f else 0f
        )
        GameDifficulty.HARD -> LetterDropConfig(
            dropSpeed = 0.18f, spawnInterval = 1.5f, minWordLength = minWordLength, vowelRichness = if (vowelRich) 1f else 0f
        )
    }

    fun step(state: LetterDropState, config: LetterDropConfig, dt: Float, random: Random): LetterDropStep {
        if (state.gameOver) return LetterDropStep(state, LetterDropEvent.NONE)

        var s = state.copy(elapsedSeconds = state.elapsedSeconds + dt, spawnTimer = state.spawnTimer + dt)

        val tiles = s.tiles.map { tile ->
            if (!tile.isQueued) tile.copy(y = tile.y + config.dropSpeed * dt) else tile
        }
        s = s.copy(tiles = tiles)

        if (s.spawnTimer >= config.spawnInterval) {
            val letter = randomLetter(config, random)
            val x = 0.1f + random.nextFloat() * 0.8f
            val newTile = FallingTile(letter, x, 0f, s.nextTileId, false)
            s = s.copy(
                tiles = s.tiles + newTile,
                nextTileId = s.nextTileId + 1,
                spawnTimer = 0f
            )
        }

        val overflow = tiles.any { it.y > 1f - overflowY }
        if (overflow) {
            return LetterDropStep(s.copy(gameOver = true), LetterDropEvent.OVERFLOW)
        }

        return LetterDropStep(s, LetterDropEvent.NONE)
    }

    fun queueTile(state: LetterDropState, tileId: Int): LetterDropState {
        val tiles = state.tiles.map { if (it.id == tileId && !it.isQueued) it.copy(isQueued = true) else it }
        val addedLetter = tiles.firstOrNull { it.id == tileId }?.letter ?: return state
        return state.copy(tiles = tiles, currentEntry = state.currentEntry + addedLetter)
    }

    fun backspace(state: LetterDropState): LetterDropState {
        if (state.currentEntry.isEmpty()) return state
        val newEntry = state.currentEntry.dropLast(1)
        val queuedTiles = state.tiles.filter { it.isQueued }
        if (queuedTiles.isEmpty()) return state.copy(currentEntry = newEntry)
        val lastQueuedId = queuedTiles.last().id
        val tiles = state.tiles.map { if (it.id == lastQueuedId) it.copy(isQueued = false) else it }
        return state.copy(tiles = tiles, currentEntry = newEntry)
    }

    fun clear(state: LetterDropState): LetterDropState {
        val tiles = state.tiles.map { if (it.isQueued) it.copy(isQueued = false) else it }
        return state.copy(tiles = tiles, currentEntry = "")
    }

    fun submitWord(state: LetterDropState, config: LetterDropConfig, wordData: WordData): LetterDropStep {
        val word = state.currentEntry.lowercase()
        if (word.length < config.minWordLength) {
            return LetterDropStep(clear(state), LetterDropEvent.NONE)
        }
        if (!wordData.isValidWord(word)) {
            return LetterDropStep(clear(state), LetterDropEvent.NONE)
        }

        val queuedIds = state.tiles.filter { it.isQueued }.map { it.id }.toSet()
        val remainingTiles = state.tiles.filterNot { queuedIds.contains(it.id) }
        val wordScore = scoreWord(word)
        val newLongest = if (word.length > state.longestWord.length) word else state.longestWord

        val newState = state.copy(
            tiles = remainingTiles,
            currentEntry = "",
            score = state.score + wordScore,
            longestWord = newLongest,
            totalWordsFormed = state.totalWordsFormed + 1
        )
        return LetterDropStep(newState, LetterDropEvent.WORD_CLEARED)
    }

    fun scoreWord(word: String): Int = when (word.length) {
        3 -> 10
        4 -> 20
        5 -> 40
        6 -> 70
        7 -> 110
        else -> 160 + (word.length - 8) * 60
    }

    private fun randomLetter(config: LetterDropConfig, random: Random): Char {
        val weights = if (config.vowelRichness > 0.5f) vowelRichWeights else balancedWeights
        val total = weights.values.sum()
        var pick = random.nextInt(total)
        for ((char, weight) in weights) {
            if (pick < weight) return char
            pick -= weight
        }
        return 'e'
    }
}
