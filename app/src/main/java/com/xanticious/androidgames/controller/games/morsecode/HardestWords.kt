package com.xanticious.androidgames.controller.games.morsecode

import com.xanticious.androidgames.model.games.morsecode.WordAttempt

/**
 * Picks the [n] hardest words from a completed phrase attempt.
 *
 * Difficulty score = retries × 10_000 + totalMs (retries are weighted heavily so
 * a word that needed any restart always outranks a fluent word of the same wall-clock
 * duration).
 *
 * If fewer than [n] words are available, all words are returned.
 * Pure JVM, no Android imports.
 */
object HardestWords {

    fun pick(words: List<WordAttempt>, n: Int = 5): List<String> =
        words.asSequence()
            .sortedByDescending { it.retries * 10_000L + it.totalMs }
            .take(n)
            .map { it.word }
            .toList()
}
