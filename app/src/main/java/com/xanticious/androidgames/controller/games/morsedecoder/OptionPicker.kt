package com.xanticious.androidgames.controller.games.morsedecoder

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.morse.MORSE
import com.xanticious.androidgames.model.games.morse.Symbol
import kotlin.random.Random

/**
 * Builds the 5 multiple-choice options for a single letter.
 *
 * Rules:
 * - Always exactly 5 unique letters.
 * - Always contains [answer].
 * - Difficulty-biased distractors:
 *   - **Easy**: distractors are drawn from letters whose Morse codes are the
 *     most *dissimilar* to [answer] (large code distance) so confusable pairs
 *     rarely appear together.
 *   - **Medium**: random selection from remaining letters.
 *   - **Hard**: distractors are drawn from letters whose Morse codes are the
 *     most *similar* to [answer] (small code distance) to stress careful
 *     listening.
 *
 * Same [answer] + [seed] + [difficulty] always produces the same list (order
 * included), making runs fully reproducible.
 */
object OptionPicker {

    private val alphabet: List<Char> = MORSE.keys.sorted()

    /**
     * Build the 5 options for [answer].  [seed] controls both the candidate
     * pool sampling (for Hard/Easy, we keep the top 12 biased candidates and
     * shuffle within them) and the final shuffle order.
     */
    fun build(answer: Char, seed: Long, difficulty: GameDifficulty = GameDifficulty.MEDIUM): List<Char> {
        val rng = Random(seed xor answer.code.toLong())
        val others = alphabet.filter { it != answer.uppercaseChar() }

        val distractors: List<Char> = when (difficulty) {
            GameDifficulty.EASY -> {
                // Dissimilar: pick from the most code-distant letters first
                others
                    .sortedByDescending { morseDistance(answer, it) }
                    .take(12)
                    .shuffled(rng)
                    .take(4)
            }
            GameDifficulty.MEDIUM -> {
                others.shuffled(rng).take(4)
            }
            GameDifficulty.HARD -> {
                // Confusable: pick from the most code-similar letters first
                others
                    .sortedBy { morseDistance(answer, it) }
                    .take(12)
                    .shuffled(rng)
                    .take(4)
            }
        }

        return (distractors + answer.uppercaseChar()).shuffled(rng)
    }

    /**
     * Symbol-level distance between two letters' Morse codes.
     *
     * Counts mismatching symbols in the shared prefix plus the length
     * difference for unequal-length codes.  Lower = more confusable.
     */
    internal fun morseDistance(a: Char, b: Char): Int {
        val codeA = MORSE[a.uppercaseChar()] ?: return Int.MAX_VALUE / 2
        val codeB = MORSE[b.uppercaseChar()] ?: return Int.MAX_VALUE / 2
        val minLen = minOf(codeA.size, codeB.size)
        val mismatch = (0 until minLen).count { codeA[it] != codeB[it] }
        val lenDiff = codeA.size - codeB.size
        val absLenDiff = if (lenDiff < 0) -lenDiff else lenDiff
        return mismatch + absLenDiff
    }
}
