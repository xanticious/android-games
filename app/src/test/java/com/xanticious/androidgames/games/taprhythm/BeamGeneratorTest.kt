package com.xanticious.androidgames.games.taprhythm

import com.xanticious.androidgames.controller.games.taprhythm.BeamGenerator
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.taprhythm.Beam
import com.xanticious.androidgames.model.games.taprhythm.Lane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeamGeneratorTest {

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun generateUpTo_sameSeed_identicalBeams_easy() {
        val a = BeamGenerator.generateUpTo(GameDifficulty.EASY, 42L, 15_000L)
        val b = BeamGenerator.generateUpTo(GameDifficulty.EASY, 42L, 15_000L)
        assertEquals("Same seed must produce identical beams", a, b)
    }

    @Test
    fun generateUpTo_sameSeed_identicalBeams_medium() {
        val a = BeamGenerator.generateUpTo(GameDifficulty.MEDIUM, 99L, 20_000L)
        val b = BeamGenerator.generateUpTo(GameDifficulty.MEDIUM, 99L, 20_000L)
        assertEquals(a, b)
    }

    @Test
    fun generateUpTo_sameSeed_identicalBeams_hard() {
        val a = BeamGenerator.generateUpTo(GameDifficulty.HARD, 12345L, 10_000L)
        val b = BeamGenerator.generateUpTo(GameDifficulty.HARD, 12345L, 10_000L)
        assertEquals(a, b)
    }

    @Test
    fun generateUpTo_differentSeeds_differentBeams() {
        val a = BeamGenerator.generateUpTo(GameDifficulty.EASY, 1L, 15_000L)
        val b = BeamGenerator.generateUpTo(GameDifficulty.EASY, 2L, 15_000L)
        assertFalse("Different seeds should produce different beam streams", a == b)
    }

    // ── Parameter escalation ──────────────────────────────────────────────────

    @Test
    fun paramsAt_fallSpeed_increasesWithElapsedTime_easy() {
        val early = BeamGenerator.paramsAt(GameDifficulty.EASY, 0L)
        val later = BeamGenerator.paramsAt(GameDifficulty.EASY, 60_000L)
        assertTrue(
            "fallSpeed should increase: ${early.fallSpeed} < ${later.fallSpeed}",
            later.fallSpeed > early.fallSpeed
        )
    }

    @Test
    fun paramsAt_fallSpeed_increasesWithElapsedTime_hard() {
        val early = BeamGenerator.paramsAt(GameDifficulty.HARD, 0L)
        val later = BeamGenerator.paramsAt(GameDifficulty.HARD, 30_000L)
        assertTrue(later.fallSpeed > early.fallSpeed)
    }

    @Test
    fun paramsAt_beamDensity_increasesMonotonically_easy() {
        val t0  = BeamGenerator.paramsAt(GameDifficulty.EASY, 0L)
        val t30 = BeamGenerator.paramsAt(GameDifficulty.EASY, 30_000L)
        val t90 = BeamGenerator.paramsAt(GameDifficulty.EASY, 90_000L)
        assertTrue("Density at 30s should exceed density at 0s", t30.beamDensity > t0.beamDensity)
        assertTrue("Density at 90s should exceed density at 30s", t90.beamDensity > t30.beamDensity)
    }

    @Test
    fun paramsAt_overlapChance_increasesMonotonically_normal() {
        val t0  = BeamGenerator.paramsAt(GameDifficulty.MEDIUM, 0L)
        val t60 = BeamGenerator.paramsAt(GameDifficulty.MEDIUM, 60_000L)
        assertTrue("Overlap chance should increase over time", t60.overlapChance > t0.overlapChance)
    }

    @Test
    fun paramsAt_windowScale_decreasesMonotonically_hard() {
        val early = BeamGenerator.paramsAt(GameDifficulty.HARD, 0L)
        val later = BeamGenerator.paramsAt(GameDifficulty.HARD, 30_000L)
        assertTrue(
            "windowScale (timing tolerance) should shrink over time",
            later.windowScale < early.windowScale
        )
    }

    @Test
    fun paramsAt_easy_initialOverlapChanceIsZero() {
        val params = BeamGenerator.paramsAt(GameDifficulty.EASY, 0L)
        assertEquals(0f, params.overlapChance, 0.001f)
    }

    // ── Beam count monotonicity (density proof) ───────────────────────────────

    @Test
    fun generateUpTo_easy_latterWindowHasMoreOrEqualBeams() {
        val halfMs = 30_000L
        val all = BeamGenerator.generateUpTo(GameDifficulty.EASY, 7L, halfMs * 2)
        val firstHalf  = all.count { it.startMs < halfMs }
        val secondHalf = all.count { it.startMs >= halfMs }
        assertTrue(
            "Later beams should be at least as dense as early ones: first=$firstHalf, second=$secondHalf",
            secondHalf >= firstHalf
        )
    }

    // ── Easy no-overlap before threshold ──────────────────────────────────────

    @Test
    fun generateUpTo_easy_noLaneOverlapBeforeThreshold() {
        val threshold = BeamGenerator.EASY_NO_OVERLAP_UNTIL_MS
        val beams = BeamGenerator.generateUpTo(GameDifficulty.EASY, 77L, threshold)
        val leftBeams  = beams.filter { it.lane == Lane.LEFT  }
        val rightBeams = beams.filter { it.lane == Lane.RIGHT }

        for (left in leftBeams) {
            val leftEnd = left.startMs + left.durationMs
            for (right in rightBeams) {
                val rightEnd = right.startMs + right.durationMs
                val overlap = left.startMs < rightEnd && right.startMs < leftEnd
                assertFalse(
                    "Easy beams overlap before threshold: " +
                        "LEFT[${ left.startMs}..${ leftEnd}] vs RIGHT[${right.startMs}..$rightEnd]",
                    overlap
                )
            }
        }
    }

    @Test
    fun generateUpTo_easy_noLaneOverlapBeforeThreshold_differentSeed() {
        val threshold = BeamGenerator.EASY_NO_OVERLAP_UNTIL_MS
        val beams = BeamGenerator.generateUpTo(GameDifficulty.EASY, 12345L, threshold)
        val leftBeams  = beams.filter { it.lane == Lane.LEFT  }
        val rightBeams = beams.filter { it.lane == Lane.RIGHT }

        for (left in leftBeams) {
            val leftEnd = left.startMs + left.durationMs
            for (right in rightBeams) {
                val rightEnd = right.startMs + right.durationMs
                assertFalse(
                    "Overlap detected: LEFT[${left.startMs}..$leftEnd] / RIGHT[${right.startMs}..$rightEnd]",
                    left.startMs < rightEnd && right.startMs < leftEnd
                )
            }
        }
    }

    // ── Beam stream properties ────────────────────────────────────────────────

    @Test
    fun generateUpTo_allBeams_startTimesWithinRange() {
        val maxMs = 20_000L
        val beams = BeamGenerator.generateUpTo(GameDifficulty.MEDIUM, 55L, maxMs)
        beams.forEach { beam ->
            assertTrue("Beam startMs ${beam.startMs} should be < $maxMs", beam.startMs < maxMs)
            assertTrue("Beam startMs ${beam.startMs} should be >= 0", beam.startMs >= 0L)
        }
    }

    @Test
    fun generateUpTo_allBeams_sortedByStartTime() {
        val beams = BeamGenerator.generateUpTo(GameDifficulty.HARD, 99L, 10_000L)
        beams.zipWithNext { a, b ->
            assertTrue(
                "Beams should be sorted by startMs: ${a.startMs} > ${b.startMs}",
                a.startMs <= b.startMs
            )
        }
    }

    @Test
    fun generateUpTo_allBeams_durationsArePositive() {
        val beams = BeamGenerator.generateUpTo(GameDifficulty.EASY, 42L, 15_000L)
        beams.forEach { beam ->
            assertTrue("Beam durationMs should be > 0", beam.durationMs > 0)
        }
    }

    @Test
    fun generateUpTo_nonEmpty() {
        val beams = BeamGenerator.generateUpTo(GameDifficulty.EASY, 1L, 5_000L)
        assertTrue("Should generate at least one beam in 5 seconds", beams.isNotEmpty())
    }

    @Test
    fun generateUpTo_lanesAreOnlyLeftOrRight() {
        val beams = BeamGenerator.generateUpTo(GameDifficulty.HARD, 1L, 10_000L)
        beams.forEach { beam ->
            assertTrue(beam.lane == Lane.LEFT || beam.lane == Lane.RIGHT)
        }
    }

    // ── colorFor ─────────────────────────────────────────────────────────────

    @Test
    fun colorFor_shortDuration_returnsShortBucket() {
        assertEquals(
            com.xanticious.androidgames.model.games.taprhythm.ColorBucket.SHORT,
            BeamGenerator.colorFor(300L)
        )
    }

    @Test
    fun colorFor_mediumDuration_returnsMediumBucket() {
        assertEquals(
            com.xanticious.androidgames.model.games.taprhythm.ColorBucket.MEDIUM,
            BeamGenerator.colorFor(750L)
        )
    }

    @Test
    fun colorFor_longDuration_returnsLongBucket() {
        assertEquals(
            com.xanticious.androidgames.model.games.taprhythm.ColorBucket.LONG,
            BeamGenerator.colorFor(1800L)
        )
    }
}
