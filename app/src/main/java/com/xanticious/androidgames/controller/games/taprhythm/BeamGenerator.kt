package com.xanticious.androidgames.controller.games.taprhythm

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.taprhythm.Beam
import com.xanticious.androidgames.model.games.taprhythm.ColorBucket
import com.xanticious.androidgames.model.games.taprhythm.GenParams
import com.xanticious.androidgames.model.games.taprhythm.Lane
import kotlin.random.Random

/**
 * Endless, deterministic beam generator for Tap Rhythm.
 *
 * The only public API consumed by the view and tests:
 *   - [paramsAt]      — generation parameters at a given elapsed time
 *   - [generateUpTo]  — all beams from t=0 up to [maxMs] (pure, same seed ⇒ same output)
 *   - [colorFor]      — bucket a duration into a [ColorBucket]
 *
 * Generation guarantees:
 *   1. Deterministic: identical (difficulty, seed, maxMs) always yields identical beams.
 *   2. Density / fall-speed escalate monotonically with elapsed time.
 *   3. EASY mode enforces zero lane-overlap before [EASY_NO_OVERLAP_UNTIL_MS].
 */
object BeamGenerator {

    /** Easy mode: no simultaneous lane activity before this threshold (30 s). */
    const val EASY_NO_OVERLAP_UNTIL_MS = 30_000L

    // Minimum silence between lanes when Easy overlap-prevention fires (ms).
    private const val MIN_INTER_LANE_GAP_MS = 200L

    // Duration ranges per bucket (ms)
    private const val SHORT_MIN = 150L
    private const val SHORT_MAX = 450L
    private const val MEDIUM_MIN = 500L
    private const val MEDIUM_MAX = 1000L
    private const val LONG_MIN = 1200L
    private const val LONG_MAX = 2500L

    // ── Difficulty configuration ──────────────────────────────────────────────

    private data class DiffConfig(
        val initialFallSpeed: Float,
        val maxFallSpeed: Float,
        val initialMinGapMs: Long,
        val initialMaxGapMs: Long,
        val finalMinGapMs: Long,
        val finalMaxGapMs: Long,
        val initialOverlapChance: Float,
        val maxOverlapChance: Float,
        val initialDurationMix: Float,
        val finalDurationMix: Float,
        val initialWindowScale: Float,
        val finalWindowScale: Float,
        val rampDurationMs: Long
    )

    private val configs: Map<GameDifficulty, DiffConfig> = mapOf(
        GameDifficulty.EASY to DiffConfig(
            initialFallSpeed = 200f,    maxFallSpeed = 450f,
            initialMinGapMs = 800L,     initialMaxGapMs = 2000L,
            finalMinGapMs = 300L,       finalMaxGapMs = 700L,
            initialOverlapChance = 0f,  maxOverlapChance = 0.4f,
            initialDurationMix = 0.15f, finalDurationMix = 0.5f,
            initialWindowScale = 1.4f,  finalWindowScale = 0.9f,
            rampDurationMs = 180_000L
        ),
        GameDifficulty.MEDIUM to DiffConfig(
            initialFallSpeed = 350f,    maxFallSpeed = 650f,
            initialMinGapMs = 500L,     initialMaxGapMs = 1400L,
            finalMinGapMs = 150L,       finalMaxGapMs = 500L,
            initialOverlapChance = 0.15f, maxOverlapChance = 0.75f,
            initialDurationMix = 0.3f,  finalDurationMix = 0.6f,
            initialWindowScale = 1.1f,  finalWindowScale = 0.7f,
            rampDurationMs = 120_000L
        ),
        GameDifficulty.HARD to DiffConfig(
            initialFallSpeed = 500f,    maxFallSpeed = 850f,
            initialMinGapMs = 250L,     initialMaxGapMs = 800L,
            finalMinGapMs = 80L,        finalMaxGapMs = 250L,
            initialOverlapChance = 0.35f, maxOverlapChance = 0.9f,
            initialDurationMix = 0.4f,  finalDurationMix = 0.7f,
            initialWindowScale = 0.9f,  finalWindowScale = 0.55f,
            rampDurationMs = 60_000L
        )
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /** Generation parameters at [elapsedMs] for [difficulty]. Pure / no side effects. */
    fun paramsAt(difficulty: GameDifficulty, elapsedMs: Long): GenParams {
        val cfg = configs[difficulty] ?: configs.getValue(GameDifficulty.MEDIUM)
        val t = (elapsedMs.toFloat() / cfg.rampDurationMs.toFloat()).coerceIn(0f, 1f)

        val minGap = lerp(cfg.initialMinGapMs.toFloat(), cfg.finalMinGapMs.toFloat(), t).toLong()
        val maxGap = lerp(cfg.initialMaxGapMs.toFloat(), cfg.finalMaxGapMs.toFloat(), t).toLong()

        // beamDensity: 0 = max gap (sparse), 1 = min gap (dense)
        val initialAvgGap = (cfg.initialMinGapMs + cfg.initialMaxGapMs) / 2f
        val currentAvgGap = ((minGap + maxGap) / 2f).coerceAtLeast(1f)
        val beamDensity = (1f - currentAvgGap / initialAvgGap).coerceIn(0f, 1f)

        return GenParams(
            fallSpeed     = lerp(cfg.initialFallSpeed, cfg.maxFallSpeed, t),
            beamDensity   = beamDensity,
            overlapChance = lerp(cfg.initialOverlapChance, cfg.maxOverlapChance, t),
            durationMix   = lerp(cfg.initialDurationMix, cfg.finalDurationMix, t),
            windowScale   = lerp(cfg.initialWindowScale, cfg.finalWindowScale, t)
        )
    }

    /**
     * Generates all beams with [startMs] in [0, [maxMs]) for the given [difficulty]
     * and [seed]. Deterministic: same arguments always return an equal list.
     */
    fun generateUpTo(difficulty: GameDifficulty, seed: Long, maxMs: Long): List<Beam> {
        val rng = Random(seed)
        val cfg = configs[difficulty] ?: configs.getValue(GameDifficulty.MEDIUM)
        val beams = mutableListOf<Beam>()

        // Per-lane next-available times. RIGHT gets a small head-start offset.
        val nextMs = longArrayOf(
            sampleGap(rng, cfg, 0L),
            sampleGap(rng, cfg, 0L) + 350L
        )

        while (true) {
            val t = if (nextMs[0] <= nextMs[1]) nextMs[0] else nextMs[1]
            if (t >= maxMs) break

            val primaryIdx = if (nextMs[0] <= nextMs[1]) 0 else 1
            val otherIdx = 1 - primaryIdx
            val primaryLane = if (primaryIdx == 0) Lane.LEFT else Lane.RIGHT
            val otherLane = if (otherIdx == 0) Lane.LEFT else Lane.RIGHT
            val bothDue = nextMs[otherIdx] <= t

            // Always spawn the primary lane
            val primaryDur = sampleDuration(rng, paramsAt(difficulty, t))
            val primaryGap = sampleGap(rng, cfg, t)
            beams.add(Beam(primaryLane, t, primaryDur, colorFor(primaryDur)))
            nextMs[primaryIdx] = t + primaryDur + primaryGap

            when {
                // Easy early-game: block the other lane until this beam finishes
                difficulty == GameDifficulty.EASY && t < EASY_NO_OVERLAP_UNTIL_MS -> {
                    nextMs[otherIdx] = maxOf(
                        nextMs[otherIdx],
                        t + primaryDur + MIN_INTER_LANE_GAP_MS
                    )
                }
                // Both lanes became available at the same tick → consider overlap
                bothDue -> {
                    if (rng.nextFloat() < paramsAt(difficulty, t).overlapChance) {
                        val secDur = sampleDuration(rng, paramsAt(difficulty, t))
                        val secGap = sampleGap(rng, cfg, t)
                        beams.add(Beam(otherLane, t, secDur, colorFor(secDur)))
                        nextMs[otherIdx] = t + secDur + secGap
                    }
                    // else: other lane stays at t; becomes primary on next iteration
                }
                else -> Unit
            }
        }

        return beams.sortedBy { it.startMs }
    }

    /** Map a beam duration in milliseconds to its [ColorBucket]. */
    fun colorFor(durationMs: Long): ColorBucket = when {
        durationMs <= SHORT_MAX  -> ColorBucket.SHORT
        durationMs <= MEDIUM_MAX -> ColorBucket.MEDIUM
        else                     -> ColorBucket.LONG
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Samples a beam duration. Consumes exactly 2 RNG values regardless of bucket
     * so the sequence is consistent.
     */
    private fun sampleDuration(rng: Random, params: GenParams): Long {
        val bucketRoll = rng.nextFloat()
        val durationRoll = rng.nextFloat()
        val shortThreshold = 0.1f + 0.5f * (1f - params.durationMix)
        val mediumThreshold = shortThreshold + 0.3f
        return when {
            bucketRoll < shortThreshold  ->
                SHORT_MIN  + (durationRoll * (SHORT_MAX  - SHORT_MIN)).toLong()
            bucketRoll < mediumThreshold ->
                MEDIUM_MIN + (durationRoll * (MEDIUM_MAX - MEDIUM_MIN)).toLong()
            else                         ->
                LONG_MIN   + (durationRoll * (LONG_MAX   - LONG_MIN)).toLong()
        }
    }

    /** Samples an inter-beam gap based on the difficulty config at [elapsedMs]. */
    private fun sampleGap(rng: Random, cfg: DiffConfig, elapsedMs: Long): Long {
        val t = (elapsedMs.toFloat() / cfg.rampDurationMs.toFloat()).coerceIn(0f, 1f)
        val minGap = lerp(cfg.initialMinGapMs.toFloat(), cfg.finalMinGapMs.toFloat(), t).toLong()
        val maxGap = lerp(cfg.initialMaxGapMs.toFloat(), cfg.finalMaxGapMs.toFloat(), t).toLong()
        return minGap + (rng.nextFloat() * (maxGap - minGap).toFloat()).toLong()
    }
}
