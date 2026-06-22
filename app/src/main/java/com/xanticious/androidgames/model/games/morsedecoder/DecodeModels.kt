package com.xanticious.androidgames.model.games.morsedecoder

import com.xanticious.androidgames.model.GameDifficulty

/**
 * A single multiple-choice prompt for one letter of the target sentence.
 *
 * [options] always has exactly 5 elements and always contains [answer].
 * [disabled] grows as the player guesses wrong — the correct [answer] is
 * never added to [disabled], so the player can always finish.
 */
data class DecodePrompt(
    val answer: Char,
    val options: List<Char>,    // size 5, always contains answer
    val disabled: Set<Char>     // wrong guesses for this letter
)

/**
 * Result for one decoded letter: how many wrong guesses were made before
 * the player got it right, and how long it took.
 */
data class LetterOutcome(
    val answer: Char,
    val wrongGuesses: Int,
    val timeMs: Long
)

/**
 * End-of-game summary for display on the Results panel.
 *
 * [accuracy] = letters solved on first guess ÷ total letters (0..1).
 * [longestStreak] = longest run of consecutive first-try-correct letters.
 * [hardestLetter] = letter with the most wrong guesses (first occurrence wins
 * a tie, or '?' when outcomes is empty).
 */
data class DecoderResult(
    val accuracy: Float,
    val totalMs: Long,
    val longestStreak: Int,
    val hardestLetter: Char,
    val seed: Long
)

/**
 * Per-difficulty playback speed configuration.  The primary difficulty lever
 * is WPM → dit-unit length: faster WPM = smaller U = harder to hear.
 */
data class DecoderConfig(
    val wpm: Int,
    val difficulty: GameDifficulty
) {
    /** Base dit-unit length derived from the standard PARIS formula. */
    val ditUnitMs: Int get() = 1200 / wpm
}

/** Maps each [GameDifficulty] to its default [DecoderConfig]. */
fun decoderConfigFor(difficulty: GameDifficulty): DecoderConfig = when (difficulty) {
    GameDifficulty.EASY   -> DecoderConfig(wpm = 5,  difficulty = difficulty)
    GameDifficulty.MEDIUM -> DecoderConfig(wpm = 10, difficulty = difficulty)
    GameDifficulty.HARD   -> DecoderConfig(wpm = 18, difficulty = difficulty)
}
