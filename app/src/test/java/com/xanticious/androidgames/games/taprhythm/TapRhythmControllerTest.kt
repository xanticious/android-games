package com.xanticious.androidgames.games.taprhythm

import com.xanticious.androidgames.controller.games.taprhythm.BeamGenerator
import com.xanticious.androidgames.controller.games.taprhythm.TapRhythmController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.rhythm.Judgment
import com.xanticious.androidgames.model.games.rhythm.ScoreState
import com.xanticious.androidgames.model.games.rhythm.TimingWindows
import com.xanticious.androidgames.model.games.taprhythm.Beam
import com.xanticious.androidgames.model.games.taprhythm.ColorBucket
import com.xanticious.androidgames.model.games.taprhythm.Edge
import com.xanticious.androidgames.model.games.taprhythm.HealthState
import com.xanticious.androidgames.model.games.taprhythm.Lane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapRhythmControllerTest {

    private val defaultWindows = TimingWindows()

    private fun beam(startMs: Long = 1_000L, durationMs: Long = 500L) =
        Beam(Lane.LEFT, startMs, durationMs, ColorBucket.MEDIUM)

    // ── judgeEdge – PRESS ─────────────────────────────────────────────────────

    @Test
    fun judgeEdge_perfectPress_returnsPerfect() {
        val b = beam(startMs = 1_000L)
        val ej = TapRhythmController.judgeEdge(b, Edge.PRESS, 1_000L, defaultWindows)
        assertEquals(Judgment.PERFECT, ej.judgment)
    }

    @Test
    fun judgeEdge_pressWithin30ms_returnsPerfect() {
        val b = beam(startMs = 1_000L)
        val ej = TapRhythmController.judgeEdge(b, Edge.PRESS, 1_025L, defaultWindows)
        assertEquals(Judgment.PERFECT, ej.judgment)
    }

    @Test
    fun judgeEdge_pressWithin60ms_returnsGreat() {
        val b = beam(startMs = 1_000L)
        val ej = TapRhythmController.judgeEdge(b, Edge.PRESS, 1_045L, defaultWindows)
        assertEquals(Judgment.GREAT, ej.judgment)
    }

    @Test
    fun judgeEdge_pressWithin100ms_returnsGood() {
        val b = beam(startMs = 1_000L)
        val ej = TapRhythmController.judgeEdge(b, Edge.PRESS, 1_080L, defaultWindows)
        assertEquals(Judgment.GOOD, ej.judgment)
    }

    @Test
    fun judgeEdge_pressOutside100ms_returnsMiss() {
        val b = beam(startMs = 1_000L)
        val ej = TapRhythmController.judgeEdge(b, Edge.PRESS, 1_200L, defaultWindows)
        assertEquals(Judgment.MISS, ej.judgment)
    }

    @Test
    fun judgeEdge_earlyPress_alsoJudgedByAbsDelta() {
        val b = beam(startMs = 1_000L)
        val ej = TapRhythmController.judgeEdge(b, Edge.PRESS, 950L, defaultWindows) // 50ms early
        assertEquals(Judgment.GREAT, ej.judgment)
    }

    // ── judgeEdge – RELEASE ───────────────────────────────────────────────────

    @Test
    fun judgeEdge_perfectRelease_returnsPerfect() {
        val b = beam(startMs = 1_000L, durationMs = 500L)
        // Release target = 1000 + 500 = 1500
        val ej = TapRhythmController.judgeEdge(b, Edge.RELEASE, 1_500L, defaultWindows)
        assertEquals(Judgment.PERFECT, ej.judgment)
    }

    @Test
    fun judgeEdge_releaseJudgedOnReleaseTime_notStartTime() {
        val b = beam(startMs = 1_000L, durationMs = 800L)
        // Release target = 1800ms. Pressing at 1000 should be MISS for release edge.
        val ej = TapRhythmController.judgeEdge(b, Edge.RELEASE, 1_000L, defaultWindows)
        assertEquals(
            "Release edge target is 1800ms; tapping at 1000ms should be MISS",
            Judgment.MISS, ej.judgment
        )
    }

    @Test
    fun judgeEdge_lateRelease_returnsMiss() {
        val b = beam(startMs = 1_000L, durationMs = 500L)
        val ej = TapRhythmController.judgeEdge(b, Edge.RELEASE, 1_800L, defaultWindows)
        assertEquals(Judgment.MISS, ej.judgment)
    }

    @Test
    fun judgeEdge_edgeJudgment_carriesBeamAndEdgeRef() {
        val b = beam(startMs = 2_000L)
        val ej = TapRhythmController.judgeEdge(b, Edge.PRESS, 2_000L, defaultWindows)
        assertEquals(b, ej.beam)
        assertEquals(Edge.PRESS, ej.edge)
    }

    // ── updateHealth ─────────────────────────────────────────────────────────

    @Test
    fun updateHealth_onPerfect_restoresHealth() {
        val h = HealthState(current = 0.5f)
        val updated = TapRhythmController.updateHealth(h, Judgment.PERFECT)
        assertTrue(updated.current > 0.5f)
    }

    @Test
    fun updateHealth_onMiss_drainsHealth() {
        val h = HealthState(current = 1.0f)
        val updated = TapRhythmController.updateHealth(h, Judgment.MISS)
        assertTrue(updated.current < 1.0f)
    }

    @Test
    fun updateHealth_onGood_drainsHealth() {
        val h = HealthState(current = 0.8f)
        val updated = TapRhythmController.updateHealth(h, Judgment.GOOD)
        assertTrue(updated.current < 0.8f)
    }

    @Test
    fun updateHealth_onGreat_restoresOrHoldsHealth() {
        val h = HealthState(current = 0.7f)
        val updated = TapRhythmController.updateHealth(h, Judgment.GREAT)
        assertTrue(updated.current >= 0.7f)
    }

    @Test
    fun updateHealth_depleted_clampedToZero() {
        val h = HealthState(current = 0.05f)
        val updated = TapRhythmController.updateHealth(h, Judgment.MISS)
        assertEquals(0f, updated.current, 0.0001f)
    }

    @Test
    fun updateHealth_full_clampedToMax() {
        val h = HealthState(current = HealthState.MAX)
        val updated = TapRhythmController.updateHealth(h, Judgment.PERFECT)
        assertEquals(HealthState.MAX, updated.current, 0.0001f)
    }

    @Test
    fun updateHealth_repeatedMisses_eventuallyDepletesHealth() {
        var h = HealthState()
        repeat(20) { h = TapRhythmController.updateHealth(h, Judgment.MISS) }
        assertTrue("Repeated misses should deplete health", h.isDead)
    }

    // ── HealthState ───────────────────────────────────────────────────────────

    @Test
    fun healthState_isDead_whenCurrentIsZero() {
        assertTrue(HealthState(current = 0f).isDead)
    }

    @Test
    fun healthState_isNotDead_whenCurrentIsPositive() {
        assertFalse(HealthState(current = 0.01f).isDead)
    }

    @Test
    fun healthState_fraction_clampedTo0_1() {
        assertEquals(1f, HealthState(current = 2f, max = 1f).fraction, 0.0001f)
        assertEquals(0f, HealthState(current = -1f, max = 1f).fraction, 0.0001f)
    }

    // ── updateScore ───────────────────────────────────────────────────────────

    @Test
    fun updateScore_perfect_incrementsCombo() {
        val state = ScoreState()
        val updated = TapRhythmController.updateScore(state, Judgment.PERFECT)
        assertEquals(1, updated.combo)
    }

    @Test
    fun updateScore_miss_resetsCombo() {
        val state = ScoreState(combo = 10, maxCombo = 10)
        val updated = TapRhythmController.updateScore(state, Judgment.MISS)
        assertEquals(0, updated.combo)
    }

    @Test
    fun updateScore_miss_preservesMaxCombo() {
        val state = ScoreState(combo = 15, maxCombo = 15)
        val updated = TapRhythmController.updateScore(state, Judgment.MISS)
        assertEquals(15, updated.maxCombo)
    }

    @Test
    fun updateScore_perfectEdges_incrementsScore() {
        val state = ScoreState()
        val updated = TapRhythmController.updateScore(state, Judgment.PERFECT)
        assertTrue("Perfect should add score", updated.score > 0)
    }

    // ── windowsFor ───────────────────────────────────────────────────────────

    @Test
    fun windowsFor_hard_tighterThanEasy_atSameTime() {
        val hardWindows = TapRhythmController.windowsFor(GameDifficulty.HARD,   0L)
        val easyWindows = TapRhythmController.windowsFor(GameDifficulty.EASY, 0L)
        assertTrue(
            "Hard should have tighter windows than Easy",
            hardWindows.perfectMs < easyWindows.perfectMs
        )
    }

    @Test
    fun windowsFor_sameWindowsTightenWithElapsedTime_hard() {
        val early = TapRhythmController.windowsFor(GameDifficulty.HARD, 0L)
        val later = TapRhythmController.windowsFor(GameDifficulty.HARD, 30_000L)
        assertTrue(
            "Windows should tighten as time passes: ${early.perfectMs} -> ${later.perfectMs}",
            later.perfectMs <= early.perfectMs
        )
    }

    // ── buildResult ──────────────────────────────────────────────────────────

    @Test
    fun buildResult_survivedMs_matchesInput() {
        val score = ScoreState(score = 500, combo = 0, maxCombo = 20)
        val result = TapRhythmController.buildResult(12_345L, score, 99L)
        assertEquals(12_345L, result.survivedMs)
    }

    @Test
    fun buildResult_seed_matchesInput() {
        val score = ScoreState()
        val result = TapRhythmController.buildResult(0L, score, 777L)
        assertEquals(777L, result.seed)
    }

    @Test
    fun buildResult_maxStreak_fromMaxCombo() {
        val score = ScoreState(score = 100, combo = 5, maxCombo = 30)
        val result = TapRhythmController.buildResult(5_000L, score, 1L)
        assertEquals(30, result.maxStreak)
    }
}
