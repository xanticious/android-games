package com.xanticious.androidgames.model.games.morse

/**
 * Shared International Morse alphabet and timing, per
 * `design/common/morse-code.md`. Used by both Morse-code games (sender and
 * decoder) as the single source of truth. v1 covers the 26 letters and the word
 * space; digits and punctuation are out of scope.
 *
 * Pure model layer: no Android, no UI imports.
 */

/** A single Morse element: a short `DIT` (`.`) or a long `DAH` (`-`). */
enum class Symbol { DIT, DAH }

/** Render a symbol sequence as a glyph string, e.g. `[DAH, DIT] -> "-."`. */
fun List<Symbol>.glyphs(): String = joinToString("") { if (it == Symbol.DIT) "." else "-" }

/** International Morse for the 26 letters (uppercase keys). */
val MORSE: Map<Char, List<Symbol>> = mapOf(
    'A' to listOf(Symbol.DIT, Symbol.DAH),
    'B' to listOf(Symbol.DAH, Symbol.DIT, Symbol.DIT, Symbol.DIT),
    'C' to listOf(Symbol.DAH, Symbol.DIT, Symbol.DAH, Symbol.DIT),
    'D' to listOf(Symbol.DAH, Symbol.DIT, Symbol.DIT),
    'E' to listOf(Symbol.DIT),
    'F' to listOf(Symbol.DIT, Symbol.DIT, Symbol.DAH, Symbol.DIT),
    'G' to listOf(Symbol.DAH, Symbol.DAH, Symbol.DIT),
    'H' to listOf(Symbol.DIT, Symbol.DIT, Symbol.DIT, Symbol.DIT),
    'I' to listOf(Symbol.DIT, Symbol.DIT),
    'J' to listOf(Symbol.DIT, Symbol.DAH, Symbol.DAH, Symbol.DAH),
    'K' to listOf(Symbol.DAH, Symbol.DIT, Symbol.DAH),
    'L' to listOf(Symbol.DIT, Symbol.DAH, Symbol.DIT, Symbol.DIT),
    'M' to listOf(Symbol.DAH, Symbol.DAH),
    'N' to listOf(Symbol.DAH, Symbol.DIT),
    'O' to listOf(Symbol.DAH, Symbol.DAH, Symbol.DAH),
    'P' to listOf(Symbol.DIT, Symbol.DAH, Symbol.DAH, Symbol.DIT),
    'Q' to listOf(Symbol.DAH, Symbol.DAH, Symbol.DIT, Symbol.DAH),
    'R' to listOf(Symbol.DIT, Symbol.DAH, Symbol.DIT),
    'S' to listOf(Symbol.DIT, Symbol.DIT, Symbol.DIT),
    'T' to listOf(Symbol.DAH),
    'U' to listOf(Symbol.DIT, Symbol.DIT, Symbol.DAH),
    'V' to listOf(Symbol.DIT, Symbol.DIT, Symbol.DIT, Symbol.DAH),
    'W' to listOf(Symbol.DIT, Symbol.DAH, Symbol.DAH),
    'X' to listOf(Symbol.DAH, Symbol.DIT, Symbol.DIT, Symbol.DAH),
    'Y' to listOf(Symbol.DAH, Symbol.DIT, Symbol.DAH, Symbol.DAH),
    'Z' to listOf(Symbol.DAH, Symbol.DAH, Symbol.DIT, Symbol.DIT)
)

/** Reverse lookup: a symbol pattern → its letter, or null if not a valid code. */
fun letterFor(symbols: List<Symbol>): Char? =
    MORSE.entries.firstOrNull { it.value == symbols }?.key

/**
 * Morse timing derived from a base dit unit `U` (ms). All other durations are
 * fixed multiples of `U`, per the standard ratios in the common doc.
 */
data class MorseTiming(val ditUnitMs: Int) {
    val ditMs: Int get() = ditUnitMs
    val dahMs: Int get() = ditUnitMs * 3
    val elementGapMs: Int get() = ditUnitMs
    val letterGapMs: Int get() = ditUnitMs * 3
    val wordGapMs: Int get() = ditUnitMs * 7
}

/** Standard PARIS-based conversion from words-per-minute to the dit unit `U`. */
fun wpmToUnitMs(wpm: Int): Int = 1200 / wpm
