package com.xanticious.androidgames.games.rhythm

import com.xanticious.androidgames.controller.games.rhythm.RhythmJudge
import com.xanticious.androidgames.model.games.rhythm.Judgment
import com.xanticious.androidgames.model.games.rhythm.ScoreState
import com.xanticious.androidgames.model.games.rhythm.TimingWindows
import com.xanticious.androidgames.model.games.rhythm.accuracyOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmJudgeTest {
    private val windows = TimingWindows()

    @Test
    fun judge_zeroDelta_isPerfect() {
        assertEquals(Judgment.PERFECT, RhythmJudge.judge(0, windows))
    }

    @Test
    fun judge_withinGreatWindow_isGreat() {
        assertEquals(Judgment.GREAT, RhythmJudge.judge(45, windows))
    }

    @Test
    fun judge_lateBeyondGood_isMiss() {
        assertEquals(Judgment.MISS, RhythmJudge.judge(150, windows))
    }

    @Test
    fun judge_isSymmetricForEarlyAndLate() {
        assertEquals(RhythmJudge.judge(-45, windows), RhythmJudge.judge(45, windows))
    }

    @Test
    fun multiplier_climbsWithCombo() {
        assertEquals(1, RhythmJudge.multiplierFor(0))
        assertEquals(2, RhythmJudge.multiplierFor(10))
        assertEquals(3, RhythmJudge.multiplierFor(25))
        assertEquals(4, RhythmJudge.multiplierFor(60))
    }

    @Test
    fun accumulate_perfect_extendsComboAndScores() {
        val s = RhythmJudge.accumulate(ScoreState(), Judgment.PERFECT)
        assertEquals(1, s.combo)
        assertEquals(100L, s.score)
    }

    @Test
    fun accumulate_miss_resetsCombo() {
        var s = ScoreState(combo = 12, maxCombo = 12)
        s = RhythmJudge.accumulate(s, Judgment.MISS)
        assertEquals(0, s.combo)
    }

    @Test
    fun accumulate_miss_preservesMaxCombo() {
        var s = ScoreState(combo = 12, maxCombo = 12)
        s = RhythmJudge.accumulate(s, Judgment.MISS)
        assertEquals(12, s.maxCombo)
    }

    @Test
    fun accumulate_appliesMultiplierAtHighCombo() {
        val s = RhythmJudge.accumulate(ScoreState(combo = 49, maxCombo = 49), Judgment.PERFECT)
        assertEquals(400L, s.score)
    }

    @Test
    fun accuracy_allPerfect_isOne() {
        assertEquals(1f, accuracyOf(mapOf(Judgment.PERFECT to 5)), 1e-4f)
    }

    @Test
    fun accuracy_allMiss_isZero() {
        assertEquals(0f, accuracyOf(mapOf(Judgment.MISS to 5)), 1e-4f)
    }

    @Test
    fun accuracy_mixed_isBetweenZeroAndOne() {
        val acc = accuracyOf(mapOf(Judgment.PERFECT to 2, Judgment.MISS to 2))
        assertTrue(acc > 0f && acc < 1f)
    }

    @Test
    fun timingWindows_scaledByTightensWindows() {
        val tight = TimingWindows().scaledBy(0.5f)
        assertTrue(tight.perfectMs < TimingWindows().perfectMs)
    }
}
