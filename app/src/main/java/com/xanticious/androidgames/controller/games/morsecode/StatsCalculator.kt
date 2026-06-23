package com.xanticious.androidgames.controller.games.morsecode

import com.xanticious.androidgames.model.games.morsecode.PhraseStats
import com.xanticious.androidgames.model.games.morsecode.WordAttempt

/**
 * Pure statistics calculator. Given the completed word log for a phrase, returns
 * a [PhraseStats] summary. No Android imports — fully unit-testable.
 *
 * WPM uses the standard PARIS formula: 1 word = 5 characters.
 *   wpm = (totalChars / 5) / (totalMs / 60_000)
 *       = totalChars × 12_000 / totalMs
 *
 * Timing includes retry delays, so error-prone words appear slower (by design).
 *
 * Easiest / hardest letter: scored by (totalMistakes × 100_000 + avgTimeMs);
 * lower score = easier.
 */
object StatsCalculator {

    fun summarize(words: List<WordAttempt>): PhraseStats {
        val totalChars = words.sumOf { it.word.length }
        val totalMs = words.sumOf { it.totalMs }
        val wpm = if (totalMs > 0L) (totalChars * 12_000f) / totalMs else 0f

        // Accumulate per-letter statistics across all word attempts
        data class LetterStat(var totalMs: Long = 0L, var totalMistakes: Int = 0, var count: Int = 0)
        val letterStats = mutableMapOf<Char, LetterStat>()
        for (word in words) {
            for (attempt in word.letters) {
                val stat = letterStats.getOrPut(attempt.expected) { LetterStat() }
                stat.totalMs += attempt.timeMs
                stat.totalMistakes += attempt.mistakes
                stat.count++
            }
        }

        val distinctLetters = letterStats.size

        val fallback = 'E'
        val easiestLetter: Char
        val hardestLetter: Char
        if (letterStats.isEmpty()) {
            easiestLetter = fallback
            hardestLetter = fallback
        } else {
            fun score(s: LetterStat): Long = s.totalMistakes * 100_000L + (s.totalMs / s.count.coerceAtLeast(1))
            easiestLetter = letterStats.minByOrNull { score(it.value) }?.key ?: fallback
            hardestLetter = letterStats.maxByOrNull { score(it.value) }?.key ?: fallback
        }

        val fastestWord = words.minByOrNull { it.totalMs }?.let { it.word to it.totalMs } ?: ("" to 0L)
        val slowestWord = words.maxByOrNull { it.totalMs }?.let { it.word to it.totalMs } ?: ("" to 0L)

        return PhraseStats(
            wpm = wpm,
            distinctLetters = distinctLetters,
            easiestLetter = easiestLetter,
            hardestLetter = hardestLetter,
            fastestWord = fastestWord,
            slowestWord = slowestWord
        )
    }
}
