package com.xanticious.androidgames.controller.games.morsecode

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.morsecode.Phrase
import com.xanticious.androidgames.model.games.morsecode.difficultyConfig
import kotlin.random.Random

/**
 * Deterministic, seed-based phrase selector from a local word bank.
 * No network, no Android imports — fully unit-testable.
 *
 * Same [seed] + [difficulty] always produces the same [Phrase].
 */
object PhraseBank {

    private val EASY_WORDS = listOf(
        "THE", "AND", "FOR", "ARE", "BUT", "NOT", "YOU", "ALL", "CAN", "OUT",
        "ONE", "OUR", "DAY", "GET", "HAS", "HIM", "HIS", "HOW", "ITS", "NOW",
        "SEE", "TWO", "WAY", "WHO", "DID", "SET", "NEW", "TRY", "YES", "ASK"
    )

    private val MEDIUM_WORDS = listOf(
        "MORSE", "CODE", "SEND", "WAVE", "LEARN", "SKILL", "PRESS", "SHORT",
        "LONG", "FAST", "SLOW", "BEEP", "TONE", "LIGHT", "SOUND", "SPEED",
        "ABOVE", "BELOW", "TRAIN", "RADIO", "SHARP", "CLEAR", "BRAVE", "VOICE",
        "CRAFT", "STEAM", "TRACK", "REACH", "BLEND", "STORM"
    )

    private val HARD_WORDS = listOf(
        "SIGNAL", "RHYTHM", "DECODE", "ENCODE", "LETTER", "PATTERN", "QUICKLY",
        "COMPLEX", "EXAMPLE", "STATION", "CONTACT", "JOURNEY", "TRUMPET",
        "GLIMMER", "FLUTTER", "CRYSTAL", "SCRATCH", "ANCIENT", "CAPTAIN", "BALANCE"
    )

    fun pick(seed: Long, difficulty: GameDifficulty): Phrase {
        val config = difficultyConfig(difficulty)
        val rng = Random(seed)
        val pool = when (difficulty) {
            GameDifficulty.EASY   -> EASY_WORDS
            GameDifficulty.MEDIUM -> MEDIUM_WORDS
            GameDifficulty.HARD   -> HARD_WORDS
        }
        val words = List(config.phraseLength) { pool[rng.nextInt(pool.size)] }
        return Phrase(words)
    }
}
