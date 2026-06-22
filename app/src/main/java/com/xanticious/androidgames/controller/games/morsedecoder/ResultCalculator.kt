package com.xanticious.androidgames.controller.games.morsedecoder

import com.xanticious.androidgames.model.games.morsedecoder.DecoderResult
import com.xanticious.androidgames.model.games.morsedecoder.LetterOutcome

/**
 * Computes end-of-game statistics from the per-letter outcomes.
 *
 * All logic is pure: no Android imports, no state, fully unit-testable.
 */
object ResultCalculator {

    /**
     * Summarise a completed run.
     *
     * - **accuracy**: fraction of letters solved on the very first guess.
     * - **totalMs**: sum of per-letter times.
     * - **longestStreak**: longest consecutive run of first-try-correct letters.
     * - **hardestLetter**: letter with the most wrong guesses; first occurrence
     *   wins a tie.  Returns '?' when [outcomes] is empty.
     * - **seed**: the run seed, echoed through so the Results panel can display
     *   it for replay.
     */
    fun summarize(outcomes: List<LetterOutcome>, seed: Long): DecoderResult {
        if (outcomes.isEmpty()) {
            return DecoderResult(
                accuracy = 0f,
                totalMs = 0L,
                longestStreak = 0,
                hardestLetter = '?',
                seed = seed
            )
        }

        val firstTryCorrect = outcomes.count { it.wrongGuesses == 0 }
        val accuracy = firstTryCorrect.toFloat() / outcomes.size
        val totalMs = outcomes.sumOf { it.timeMs }

        var longestStreak = 0
        var currentStreak = 0
        for (outcome in outcomes) {
            if (outcome.wrongGuesses == 0) {
                currentStreak++
                if (currentStreak > longestStreak) longestStreak = currentStreak
            } else {
                currentStreak = 0
            }
        }

        val hardestLetter = outcomes.maxByOrNull { it.wrongGuesses }?.answer ?: '?'

        return DecoderResult(
            accuracy = accuracy,
            totalMs = totalMs,
            longestStreak = longestStreak,
            hardestLetter = hardestLetter,
            seed = seed
        )
    }
}
