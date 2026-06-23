package com.xanticious.androidgames.model.games.solitairetripeaks

/** Distinguishes the two TriPeaks game modes that share one controller. */
enum class TriPeaksVariant {
    /** Relaxed, no time pressure. Win by clearing all 28 board cards. */
    CLASSIC,

    /** Countdown timer: clearing cards adds time, idling drains it. */
    TIMED;
}
