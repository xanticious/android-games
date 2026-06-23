package com.xanticious.androidgames.model.games.solitairepyramid

import com.xanticious.androidgames.model.GameDifficulty

/**
 * Tuning values for a Pyramid Solitaire deal.
 *
 * [maxRedeals] controls how many times the waste pile may be recycled back into
 * the stock:  -1 = unlimited, 0 = no redeal allowed, >0 = that many redeals.
 */
data class PyramidConfig(
    val maxRedeals: Int,
    val showHints: Boolean = false
) {
    companion object {
        fun forDifficulty(difficulty: GameDifficulty): PyramidConfig = when (difficulty) {
            GameDifficulty.EASY   -> PyramidConfig(maxRedeals = -1)  // unlimited redeals
            GameDifficulty.MEDIUM -> PyramidConfig(maxRedeals = 2)   // 2 redeals
            GameDifficulty.HARD   -> PyramidConfig(maxRedeals = 0)   // no redeals
        }
    }
}
