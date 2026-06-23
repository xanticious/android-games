package com.xanticious.androidgames.model.games.spades

import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Suit

/**
 * Spades — partnership trick-taking game (design/card-games/spades).
 *
 * Pure data model: no Android, Compose, or UI imports. All mutations live in
 * `controller/games/spades/SpadesController`.
 *
 * Seat layout:
 *   NORTH (AI partner of SOUTH)
 * WEST (AI)  [trick]  EAST (AI)
 *   SOUTH (human)
 *
 * Partnerships: US = SOUTH + NORTH, THEM = WEST + EAST.
 * Turn order: SOUTH → WEST → NORTH → EAST → SOUTH …
 */
enum class SpadesPlayer(val displayName: String) {
    SOUTH("You"),
    WEST("West"),
    NORTH("Partner"),
    EAST("East");

    val team: SpadesTeam
        get() = when (this) {
            SOUTH, NORTH -> SpadesTeam.US
            WEST, EAST   -> SpadesTeam.THEM
        }

    /** Next seat clockwise. */
    val next: SpadesPlayer
        get() = entries[(ordinal + 1) % 4]

    /** Seat across the table (partner). */
    val partner: SpadesPlayer
        get() = entries[(ordinal + 2) % 4]
}

enum class SpadesTeam { US, THEM }

/**
 * A player's bid for the hand.
 * [isNil] = true when the player bids zero tricks for the Nil bonus/penalty.
 * When `amount == 0` and `isNil` is false (not valid in normal play), the
 * player has effectively bid 0 tricks as part of the team contract with no Nil
 * bonus — this edge case doesn't arise under standard rules.
 */
data class SpadesBid(val amount: Int, val isNil: Boolean = amount == 0)

/** One card played in a trick, paired with the seat that played it. */
data class SpadesTrickCard(val player: SpadesPlayer, val card: Card)

/**
 * The trick currently in progress (or the last completed trick before
 * resolution). [plays] grow from 0 to 4 as each seat contributes.
 */
data class SpadesTrick(
    val leadPlayer: SpadesPlayer,
    val plays: List<SpadesTrickCard> = emptyList()
) {
    val isComplete: Boolean get() = plays.size == 4

    /** Suit of the first card played; null until the first card is played. */
    val ledSuit: Suit? get() = plays.firstOrNull()?.card?.suit

    /** The seat whose turn it is to play next. */
    val nextPlayer: SpadesPlayer
        get() {
            var p = leadPlayer
            repeat(plays.size) { p = p.next }
            return p
        }
}

data class SpadesTeamScore(val score: Int = 0, val bags: Int = 0)

/** Per-player Nil outcome for a completed hand. */
data class NilResult(val player: SpadesPlayer, val succeeded: Boolean)

/** Delta summary from the most recently scored hand, shown in the status strip. */
data class SpadesHandResult(
    val usScoreDelta: Int,
    val themScoreDelta: Int,
    val nilResults: List<NilResult> = emptyList()
)

/**
 * Complete, immutable Spades game state. All mutations are performed by
 * [com.xanticious.androidgames.controller.games.spades.SpadesController],
 * which returns a new copy.
 */
data class SpadesGameState(
    val round: Int = 1,
    val hands: Map<SpadesPlayer, List<Card>> = emptyMap(),
    val bids: Map<SpadesPlayer, SpadesBid> = emptyMap(),
    /** The trick currently in progress; null between hands or before play starts. */
    val currentTrick: SpadesTrick? = null,
    val completedTricks: List<SpadesTrick> = emptyList(),
    val tricksWon: Map<SpadesPlayer, Int> = SpadesPlayer.entries.associateWith { 0 },
    val teamScores: Map<SpadesTeam, SpadesTeamScore> = mapOf(
        SpadesTeam.US   to SpadesTeamScore(),
        SpadesTeam.THEM to SpadesTeamScore()
    ),
    /** True once the first spade has been played as an off-suit discard. */
    val spadesBroken: Boolean = false,
    /** The player whose bid is awaited; null when all four have bid. */
    val currentBidder: SpadesPlayer? = SpadesPlayer.SOUTH,
    val gameEndScore: Int = 500,
    val nilAllowed: Boolean = true,
    val bagPenaltyEnabled: Boolean = true,
    val lastHandResult: SpadesHandResult? = null,
    val winner: SpadesTeam? = null
)
