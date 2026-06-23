package com.xanticious.androidgames.controller.games.morsecode

import com.xanticious.androidgames.model.games.morse.MorseTiming
import com.xanticious.androidgames.model.games.morse.Symbol
import com.xanticious.androidgames.model.games.morse.letterFor
import com.xanticious.androidgames.model.games.morsecode.KeyStroke

/**
 * Pure decoder: converts a stream of [KeyStroke]s from the single tap button
 * into a list of decoded Morse letters.
 *
 * Press classification (hold duration vs 2×U):
 *   - hold < 2U  → DIT
 *   - hold ≥ 2U  → DAH
 *
 * Gap classification (silence after release vs U multiples):
 *   - gap < 2U       → still within the current letter (accumulate more symbols)
 *   - 2U ≤ gap < 5U  → letter boundary (commit collected symbols as one letter)
 *   - gap ≥ 5U       → word boundary (also commits the current letter)
 *
 * Any un-committed symbols after the last stroke are committed as the final letter.
 * Pure JVM, no Android imports — fully unit-testable.
 */
object MorseKeyer {

    fun decode(strokes: List<KeyStroke>, timing: MorseTiming): List<Char> {
        val result = mutableListOf<Char>()
        val currentSymbols = mutableListOf<Symbol>()
        val threshold2U = timing.ditUnitMs * 2L
        val threshold5U = timing.ditUnitMs * 5L

        for (stroke in strokes) {
            val symbol = if (stroke.pressMs < threshold2U) Symbol.DIT else Symbol.DAH
            currentSymbols += symbol

            when {
                stroke.gapMs < threshold2U -> { /* same letter — accumulate */ }
                stroke.gapMs < threshold5U -> {
                    letterFor(currentSymbols)?.let { result += it }
                    currentSymbols.clear()
                }
                else -> {
                    // word boundary — commit current letter
                    letterFor(currentSymbols)?.let { result += it }
                    currentSymbols.clear()
                }
            }
        }

        // Commit any symbols remaining after the last stroke
        if (currentSymbols.isNotEmpty()) {
            letterFor(currentSymbols)?.let { result += it }
        }

        return result
    }
}
