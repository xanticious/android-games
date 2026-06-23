package com.xanticious.androidgames.model.games.rhythm

/**
 * Shared note-highway types for rhythm games (Melody Master, Tap Rhythm), per
 * `design/common/rhythm-note-highway.md`. Individual games pick lane count, note
 * sources, and theming; the timing/judging/scoring vocabulary below is common.
 *
 * Pure model layer: no Android, no UI imports.
 */

/** Timing judgment for a tap relative to a note's scheduled hit time. */
enum class Judgment { PERFECT, GREAT, GOOD, MISS }

/**
 * Symmetric timing windows (± milliseconds) for each judgment, from the common
 * rhythm doc. Higher difficulties shrink these via [scaledBy] (a factor < 1
 * tightens the windows).
 */
data class TimingWindows(
    val perfectMs: Long = 30,
    val greatMs: Long = 60,
    val goodMs: Long = 100
) {
    fun scaledBy(factor: Float): TimingWindows = TimingWindows(
        perfectMs = (perfectMs * factor).toLong().coerceAtLeast(1),
        greatMs = (greatMs * factor).toLong().coerceAtLeast(2),
        goodMs = (goodMs * factor).toLong().coerceAtLeast(3)
    )
}

/**
 * A scheduled note. [hitTimeMs] is when it crosses the hit line. A positive
 * [durationMs] marks a sustain/hold note (used by Tap Rhythm beams); 0 is a
 * plain tap (Melody Master).
 */
data class Note(val lane: Int, val hitTimeMs: Long, val durationMs: Long = 0)

/** A generated track: a tempo plus its scheduled notes (sorted by hit time). */
data class Track(val bpm: Int, val notes: List<Note>)

/** Final per-track summary shown on the results panel. */
data class TrackResult(
    val score: Long,
    val maxCombo: Int,
    val accuracy: Float,
    val counts: Map<Judgment, Int>
)

/**
 * Immutable running scoring state. Games fold judgments into this as notes (or,
 * for sustains, edges) are resolved, then read [toResult] at the end.
 */
data class ScoreState(
    val score: Long = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val counts: Map<Judgment, Int> = emptyMap()
) {
    fun toResult(): TrackResult = TrackResult(
        score = score,
        maxCombo = maxCombo,
        accuracy = accuracyOf(counts),
        counts = counts
    )
}

/**
 * Accuracy = achieved weighted value / maximum possible, where each resolved
 * judgment can earn up to a Perfect's base value. Returns 0 when nothing has
 * been judged yet.
 */
fun accuracyOf(counts: Map<Judgment, Int>): Float {
    val total = counts.values.sum()
    if (total == 0) return 0f
    val earned = counts.entries.sumOf { (j, n) -> baseScore(j).toLong() * n }
    val max = baseScore(Judgment.PERFECT).toLong() * total
    return (earned.toFloat() / max.toFloat()).coerceIn(0f, 1f)
}

/** Base (pre-multiplier) score for a judgment, from the shared scoring table. */
fun baseScore(judgment: Judgment): Int = when (judgment) {
    Judgment.PERFECT -> 100
    Judgment.GREAT -> 70
    Judgment.GOOD -> 40
    Judgment.MISS -> 0
}
