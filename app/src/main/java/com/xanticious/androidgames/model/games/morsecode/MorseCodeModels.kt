package com.xanticious.androidgames.model.games.morsecode

import com.xanticious.androidgames.model.GameDifficulty

/**
 * Game-specific model types for the Morse Code single-button sender game.
 * Pure model layer: no Android, no UI imports.
 */

/** A phrase to be keyed out one word at a time. */
data class Phrase(val words: List<String>)

/** Raw input from a single key-button event: hold duration and the silence gap after release. */
data class KeyStroke(val pressMs: Long, val gapMs: Long)

/** A single successfully-decoded letter within a word, with elapsed time and mistake count. */
data class LetterAttempt(val expected: Char, val timeMs: Long, val mistakes: Int)

/** A completed word, including all retries and the letters keyed in the final successful pass. */
data class WordAttempt(
    val word: String,
    val totalMs: Long,
    val retries: Int,
    val letters: List<LetterAttempt>
)

/** End-of-phrase statistics computed by [com.xanticious.androidgames.controller.games.morsecode.StatsCalculator]. */
data class PhraseStats(
    val wpm: Float,
    val distinctLetters: Int,
    val easiestLetter: Char,
    val hardestLetter: Char,
    val fastestWord: Pair<String, Long>,
    val slowestWord: Pair<String, Long>
)

/** Target WPM and phrase length for a given difficulty level. */
data class DifficultyConfig(val wpmTarget: Int, val phraseLength: Int)

fun difficultyConfig(difficulty: GameDifficulty): DifficultyConfig = when (difficulty) {
    GameDifficulty.EASY   -> DifficultyConfig(wpmTarget = 5,  phraseLength = 3)
    GameDifficulty.MEDIUM -> DifficultyConfig(wpmTarget = 13, phraseLength = 4)
    GameDifficulty.HARD   -> DifficultyConfig(wpmTarget = 20, phraseLength = 5)
}

/** Training Mode is off by default; when on, the Morse code strip shows under each active letter. */
const val TRAINING_MODE_DEFAULT: Boolean = false

/** Penalty delay applied after a mistake (ms). */
const val PENALTY_DELAY_MS: Long = 1_500L
