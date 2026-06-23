package com.xanticious.androidgames.model.games.solitairetripeaks

/**
 * Immutable configuration for a TriPeaks session.
 *
 * Difficulty maps to timer speed for TIMED and to deal options for CLASSIC.
 * All numeric defaults reflect Standard difficulty (Medium).
 */
data class TriPeaksConfig(
    val variant: TriPeaksVariant,
    /** When true, Ace wraps between King and Two. */
    val rankWrap: Boolean = true,
    // --- scoring ---
    /** Base points per card cleared (multiplied by current combo). */
    val basePointsPerCard: Int = 100,
    /** Bonus points per remaining stock card on win. */
    val stockBonusPerCard: Int = 50,
    // --- timed-only fields (ignored by CLASSIC) ---
    /** Starting countdown in seconds. */
    val timerStartSeconds: Float = 60f,
    /** Seconds drained per real second. */
    val timerDrainRate: Float = 1.0f,
    /** Bonus seconds added for each card cleared. */
    val timePerClear: Float = 2.0f,
    /** Extra bonus seconds per combo step beyond the first. */
    val timePerComboStep: Float = 0.5f,
)
