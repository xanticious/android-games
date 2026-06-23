package com.xanticious.androidgames.model.games.poker

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card

/** High-level phase of a Five-Card Draw poker session, exposed to the view. */
enum class PokerPhase {
    IDLE,
    ANTEING,
    DEALING,
    BETTING_ROUND_1,
    DRAWING,
    BETTING_ROUND_2,
    SHOWDOWN,
    AWARD_POT,
    SESSION_OVER
}

/** One seat at the table. Index 0 is always the human player. */
data class PokerPlayer(
    val index: Int,
    val name: String,
    val bankroll: Int,
    val hand: List<Card> = emptyList(),
    /** Chips committed by this player during the current betting round. */
    val currentRoundBet: Int = 0,
    val isFolded: Boolean = false,
    val isAllIn: Boolean = false,
    val isHuman: Boolean = false,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    /** True once this player has had at least one chance to act in the current round. */
    val hasActed: Boolean = false
)

/** Summary of the winning hand(s) after a showdown or fold-out. */
data class HandResult(
    val winnerIndices: List<Int>,
    val potWon: Int,
    /** Maps player index -> display name of that player's best hand (empty when fold-out). */
    val handNames: Map<Int, String>
)

/**
 * Full immutable snapshot of the poker game. Replaced atomically by each
 * controller function so the view always observes a consistent state.
 */
data class PokerGameState(
    val players: List<PokerPlayer>,
    val deck: List<Card> = emptyList(),
    val pot: Int = 0,
    /** Highest per-round bet committed by any player so far in the current betting round. */
    val currentBet: Int = 0,
    /** Seat index of the player who acts next. */
    val activePlayerIndex: Int = 0,
    /** Seat index of the current dealer (used to determine act order). */
    val dealerIndex: Int = 0,
    val anteAmount: Int = 5,
    /** Card indices (0-4) within the human's hand that are marked for discard. */
    val selectedDiscards: Set<Int> = emptySet(),
    val handResult: HandResult? = null,
    /** True when the human has run out of chips and the session should end. */
    val sessionOver: Boolean = false,
    val statusMessage: String = ""
) {
    val humanPlayer: PokerPlayer get() = players[0]

    /** How many chips the active player still needs to put in to match [currentBet]. */
    val toCall: Int
        get() {
            val p = players.getOrNull(activePlayerIndex) ?: return 0
            return (currentBet - p.currentRoundBet).coerceAtLeast(0)
        }

    /** All players who have not yet folded. */
    val activePlayers: List<PokerPlayer>
        get() = players.filter { !it.isFolded }
}
