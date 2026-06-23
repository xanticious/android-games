package com.xanticious.androidgames.model.games.cribbage

import com.xanticious.androidgames.model.games.cards.Card

/**
 * Immutable model types for Cribbage. Zero Android/Compose imports.
 *
 * Scoring convention: card value for fifteens uses min(rank.value, 10),
 * i.e. A=1, 2-9 face value, T/J/Q/K=10.
 */

/** Which player seat a given role refers to. */
enum class CribbagePlayer { HUMAN, AI }

/** High-level phase of a cribbage hand as exposed to the view. */
enum class CribbagePhase {
    DEALING,
    DISCARDING,
    CUTTING,
    PLAYING,
    SHOW_NON_DEALER,
    SHOW_DEALER,
    SHOW_CRIB,
    GAME_OVER
}

/** A scored event in the pegging (Play) phase. */
data class PeggingScore(
    val points: Int,
    val description: String
)

/** One card played during the pegging phase, with the running count after it was played. */
data class PlayedCard(
    val card: Card,
    val playedBy: CribbagePlayer,
    val countAfter: Int
)

/** Everything about a single crib hand in progress. */
data class CribbageState(
    val humanScore: Int = 0,
    val aiScore: Int = 0,
    val targetScore: Int = 121,
    val dealer: CribbagePlayer = CribbagePlayer.AI,
    val humanHand: List<Card> = emptyList(),
    val aiHand: List<Card> = emptyList(),
    val crib: List<Card> = emptyList(),
    val starter: Card? = null,
    val phase: CribbagePhase = CribbagePhase.DEALING,
    /** Cards played so far in the current pegging sequence (reset on 31 / go). */
    val playPile: List<PlayedCard> = emptyList(),
    /** Running pegging count (0-31). Resets after 31 or a "go" round. */
    val pegCount: Int = 0,
    /** Cards human still has available to play during pegging. */
    val humanPlayHand: List<Card> = emptyList(),
    /** Cards AI still has available to play during pegging. */
    val aiPlayHand: List<Card> = emptyList(),
    /** Whose turn it is to play a card during pegging. */
    val pegTurn: CribbagePlayer = CribbagePlayer.HUMAN,
    /** Set to true when the current player has said "Go" and cannot play. */
    val humanSaidGo: Boolean = false,
    val aiSaidGo: Boolean = false,
    /** Human's selected discard indices (during DISCARDING phase). */
    val humanDiscardSelection: List<Int> = emptyList(),
    /** Breakdown text of the last scoring event (for display). */
    val lastScoreBreakdown: String = "",
    /** Points awarded last event (for display). */
    val lastScorePoints: Int = 0,
    /** Winner once GAME_OVER. */
    val winner: CribbagePlayer? = null,
    /** Accumulated show scores per section (for post-hand summary). */
    val showBreakdown: List<String> = emptyList()
)
