package com.xanticious.androidgames.controller.games.taprhythm

import com.xanticious.androidgames.controller.games.rhythm.RhythmJudge
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.rhythm.Judgment
import com.xanticious.androidgames.model.games.rhythm.ScoreState
import com.xanticious.androidgames.model.games.rhythm.TimingWindows
import com.xanticious.androidgames.model.games.taprhythm.Beam
import com.xanticious.androidgames.model.games.taprhythm.Edge
import com.xanticious.androidgames.model.games.taprhythm.EdgeJudgment
import com.xanticious.androidgames.model.games.taprhythm.HealthState
import com.xanticious.androidgames.model.games.taprhythm.RunResult

/**
 * Pure controller helpers for Tap Rhythm edge judging, health management, and
 * run-result assembly. No Android or UI imports; fully unit-testable.
 */
object TapRhythmController {

    /**
     * Judge one [edge] of [beam] at [tapTimeMs] using [windows].
     *
     * For the PRESS edge, the target time is [Beam.startMs].
     * For the RELEASE edge, the target time is [Beam.startMs] + [Beam.durationMs].
     */
    fun judgeEdge(
        beam: Beam,
        edge: Edge,
        tapTimeMs: Long,
        windows: TimingWindows
    ): EdgeJudgment {
        val targetMs = when (edge) {
            Edge.PRESS   -> beam.startMs
            Edge.RELEASE -> beam.startMs + beam.durationMs
        }
        val delta = tapTimeMs - targetMs
        return EdgeJudgment(beam, edge, RhythmJudge.judge(delta, windows))
    }

    /**
     * Apply a [judgment]'s health delta to [health], clamped to [0, max].
     *
     * Perfect edges restore a little health; Miss edges drain a significant amount.
     */
    fun updateHealth(health: HealthState, judgment: Judgment): HealthState {
        val delta = when (judgment) {
            Judgment.PERFECT -> HealthState.DELTA_PERFECT
            Judgment.GREAT   -> HealthState.DELTA_GREAT
            Judgment.GOOD    -> HealthState.DELTA_GOOD
            Judgment.MISS    -> HealthState.DELTA_MISS
        }
        return health.copy(current = (health.current + delta).coerceIn(0f, health.max))
    }

    /**
     * Fold one [judgment] into the running [scoreState] via [RhythmJudge.accumulate].
     * Each edge (PRESS and RELEASE) is scored independently.
     */
    fun updateScore(scoreState: ScoreState, judgment: Judgment): ScoreState =
        RhythmJudge.accumulate(scoreState, judgment)

    /**
     * Returns the [TimingWindows] for [difficulty] at [elapsedMs], scaled by the
     * generator's [GenParams.windowScale] at that point in the ramp.
     */
    fun windowsFor(difficulty: GameDifficulty, elapsedMs: Long): TimingWindows {
        val scale = BeamGenerator.paramsAt(difficulty, elapsedMs).windowScale
        return TimingWindows().scaledBy(scale)
    }

    /** Build a [RunResult] from the final game state. */
    fun buildResult(
        survivedMs: Long,
        scoreState: ScoreState,
        seed: Long
    ): RunResult = RunResult(
        survivedMs = survivedMs,
        score      = scoreState.score,
        maxStreak  = scoreState.maxCombo,
        counts     = scoreState.counts,
        seed       = seed
    )
}
