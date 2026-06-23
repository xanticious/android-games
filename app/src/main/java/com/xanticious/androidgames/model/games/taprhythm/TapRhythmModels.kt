package com.xanticious.androidgames.model.games.taprhythm

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.rhythm.Judgment

/** The two independent hold lanes, one per thumb. */
enum class Lane { LEFT, RIGHT }

/** Duration-at-a-glance color bucket for a beam. Exact duration is on-screen length. */
enum class ColorBucket { SHORT, MEDIUM, LONG }

/**
 * A sustain note in the Tap Rhythm highway. The player presses when [startMs] crosses
 * the catch line and releases at [startMs] + [durationMs].
 */
data class Beam(
    val lane: Lane,
    val startMs: Long,
    val durationMs: Long,
    val color: ColorBucket
)

/** Which edge of a beam is being judged. */
enum class Edge { PRESS, RELEASE }

/** Result of judging one edge of one beam. */
data class EdgeJudgment(
    val beam: Beam,
    val edge: Edge,
    val judgment: Judgment
)

/** Parameters for a single run. */
data class RunRequest(
    val difficulty: GameDifficulty,
    val seed: Long
)

/** Summary produced when the player dies. */
data class RunResult(
    val survivedMs: Long,
    val score: Long,
    val maxStreak: Int,
    val counts: Map<Judgment, Int>,
    val seed: Long
)

/**
 * Live generation parameters at a given point in the run. All axes escalate
 * monotonically with elapsed survival time.
 *
 * @param fallSpeed   Beam descent speed in abstract units (view maps to screen dp/s).
 * @param beamDensity How tightly beams pack within a lane [0 = sparse … 1 = dense].
 * @param overlapChance Probability of simultaneous two-lane activity [0 … 1].
 * @param durationMix Mix toward longer holds [0 = all short … 1 = all long].
 * @param windowScale Multiplier on the shared timing windows (< 1 tightens them).
 */
data class GenParams(
    val fallSpeed: Float,
    val beamDensity: Float,
    val overlapChance: Float,
    val durationMix: Float,
    val windowScale: Float
)

/** Player health pool. Reaching zero triggers game-over (death). */
data class HealthState(
    val current: Float = MAX,
    val max: Float = MAX
) {
    val isDead: Boolean get() = current <= 0f
    val fraction: Float get() = (current / max).coerceIn(0f, 1f)

    companion object {
        const val MAX = 1.0f

        // Per-edge health deltas
        const val DELTA_PERFECT = 0.02f
        const val DELTA_GREAT = 0.005f
        const val DELTA_GOOD = -0.02f
        const val DELTA_MISS = -0.12f
    }
}
