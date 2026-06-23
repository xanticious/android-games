package com.xanticious.androidgames.model.games.deckbuildersuperhero

/**
 * Immutable snapshot of the Super Villain.
 *
 * The team must reduce [hp] to zero before [schemeProgress] reaches
 * [schemeTotal] (one advance per round).
 */
data class Villain(
    val name: String,
    val maxHp: Int,
    val hp: Int,
    val schemeTotal: Int,
    val schemeProgress: Int
) {
    val isDefeated: Boolean get() = hp <= 0
    val schemeCompleted: Boolean get() = schemeProgress >= schemeTotal
}
