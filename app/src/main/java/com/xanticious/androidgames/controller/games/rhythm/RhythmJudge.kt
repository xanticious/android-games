package com.xanticious.androidgames.controller.games.rhythm

import com.xanticious.androidgames.model.games.rhythm.Judgment
import com.xanticious.androidgames.model.games.rhythm.ScoreState
import com.xanticious.androidgames.model.games.rhythm.TimingWindows
import com.xanticious.androidgames.model.games.rhythm.baseScore
import kotlin.math.abs

/**
 * Pure judging + scoring rules shared by the note-highway rhythm games, per
 * `design/common/rhythm-note-highway.md`. No Android imports; fully
 * unit-testable.
 */
object RhythmJudge {

    /**
     * Classify a signed timing delta (tap time − scheduled hit time, in ms)
     * into a [Judgment]. Anything outside the Good window is a [Judgment.MISS].
     */
    fun judge(deltaMs: Long, windows: TimingWindows): Judgment {
        val d = abs(deltaMs)
        return when {
            d <= windows.perfectMs -> Judgment.PERFECT
            d <= windows.greatMs -> Judgment.GREAT
            d <= windows.goodMs -> Judgment.GOOD
            else -> Judgment.MISS
        }
    }

    /**
     * Combo → multiplier ladder (×1 / ×2 / ×3 / ×4), capped at ×4, from the
     * shared table.
     */
    fun multiplierFor(combo: Int): Int = when {
        combo >= 50 -> 4
        combo >= 25 -> 3
        combo >= 10 -> 2
        else -> 1
    }

    /**
     * Fold one resolved [judgment] into the running [state]: a non-Miss extends
     * the combo and scores `base × multiplier`; a Miss resets the combo and
     * scores nothing. Returns the updated immutable state.
     */
    fun accumulate(state: ScoreState, judgment: Judgment): ScoreState {
        val newCombo = if (judgment == Judgment.MISS) 0 else state.combo + 1
        val gained = baseScore(judgment).toLong() * multiplierFor(newCombo)
        return ScoreState(
            score = state.score + gained,
            combo = newCombo,
            maxCombo = maxOf(state.maxCombo, newCombo),
            counts = state.counts + (judgment to (state.counts[judgment] ?: 0) + 1)
        )
    }
}
