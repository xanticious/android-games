package com.xanticious.androidgames.model.games.hearts

import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit

/** The four seats. [SOUTH] is always the human player. */
enum class Seat { SOUTH, WEST, NORTH, EAST }

/**
 * Passing direction rotates LEFT → RIGHT → ACROSS → HOLD over a four-hand cycle.
 * LEFT means pass clockwise (to the player on your left when facing the table centre).
 */
enum class PassDirection { LEFT, RIGHT, ACROSS, HOLD }

/** Returns the next pass direction in the four-hand rotation. */
fun PassDirection.next(): PassDirection = when (this) {
    PassDirection.LEFT   -> PassDirection.RIGHT
    PassDirection.RIGHT  -> PassDirection.ACROSS
    PassDirection.ACROSS -> PassDirection.HOLD
    PassDirection.HOLD   -> PassDirection.LEFT
}

/** The seat that receives cards passed from [from] in [this] direction. */
fun PassDirection.targetFor(from: Seat): Seat = when (this) {
    PassDirection.LEFT   -> from.leftSeat()
    PassDirection.RIGHT  -> from.rightSeat()
    PassDirection.ACROSS -> from.acrossSeat()
    PassDirection.HOLD   -> from
}

/** Next seat clockwise (used for LEFT passing and trick-play order). */
fun Seat.leftSeat(): Seat = when (this) {
    Seat.SOUTH -> Seat.WEST
    Seat.WEST  -> Seat.NORTH
    Seat.NORTH -> Seat.EAST
    Seat.EAST  -> Seat.SOUTH
}

/** Previous seat clockwise (counter-clockwise — used for RIGHT passing). */
fun Seat.rightSeat(): Seat = when (this) {
    Seat.SOUTH -> Seat.EAST
    Seat.EAST  -> Seat.NORTH
    Seat.NORTH -> Seat.WEST
    Seat.WEST  -> Seat.SOUTH
}

/** Seat directly across the table. */
fun Seat.acrossSeat(): Seat = when (this) {
    Seat.SOUTH -> Seat.NORTH
    Seat.NORTH -> Seat.SOUTH
    Seat.EAST  -> Seat.WEST
    Seat.WEST  -> Seat.EAST
}

/** Short display name shown to the human (SOUTH = "You"). */
fun Seat.displayName(): String = when (this) {
    Seat.SOUTH -> "You"
    Seat.WEST  -> "West"
    Seat.NORTH -> "North"
    Seat.EAST  -> "East"
}

/** A card played to the current trick, paired with the seat that played it. */
data class TrickCard(val card: Card, val seat: Seat)

/** Immutable per-game configuration (chosen in Settings). */
data class HeartsConfig(
    /** Score threshold — first player to reach this triggers end-of-game scoring. */
    val gameEndScore: Int = 100,
    /** Jack of Diamonds variant: −10 pts to whoever takes J♦. */
    val jackOfDiamonds: Boolean = false
)

/**
 * Complete, immutable Hearts game state. All mutations are performed by the
 * controller, which returns a new copy.
 */
data class HeartsGameState(
    /** Cards currently held by each player (shrinks as tricks are played). */
    val hands: Map<Seat, List<Card>>,
    /** Cumulative penalty scores across all completed hands. */
    val scores: Map<Seat, Int>,
    /** Points each seat scored in the most recently completed hand (shown in status). */
    val handScores: Map<Seat, Int>,
    /** All cards won in tricks during the current hand, keyed by winner. */
    val tricksTaken: Map<Seat, List<Card>>,
    /** Cards played to the trick currently in progress (0–4 entries). */
    val currentTrick: List<TrickCard>,
    /** Seat that leads the current trick (or will lead the next one). */
    val leadSeat: Seat,
    /** Whether a heart has been played to any trick this hand. */
    val heartsBroken: Boolean,
    /** Pass direction for the current hand. */
    val passDirection: PassDirection,
    /** 1-based hand counter (increments after each hand is scored). */
    val handNumber: Int,
    /** Cards the human player has tapped to send in the pass phase (≤ 3). */
    val selectedCards: Set<Card>,
    /** 1-based trick counter within the current hand; resets when a new hand starts. */
    val trickNumber: Int,
    val config: HeartsConfig,
    val gameOver: Boolean = false,
    /** Seat holding the lowest cumulative score when the game ends. */
    val gameWinner: Seat? = null
) {
    companion object {
        val QUEEN_OF_SPADES  = Card(Rank.QUEEN, Suit.SPADES)
        val TWO_OF_CLUBS     = Card(Rank.TWO,   Suit.CLUBS)
        val JACK_OF_DIAMONDS = Card(Rank.JACK,  Suit.DIAMONDS)

        fun empty(config: HeartsConfig = HeartsConfig()): HeartsGameState = HeartsGameState(
            hands         = Seat.entries.associateWith { emptyList() },
            scores        = Seat.entries.associateWith { 0 },
            handScores    = Seat.entries.associateWith { 0 },
            tricksTaken   = Seat.entries.associateWith { emptyList() },
            currentTrick  = emptyList(),
            leadSeat      = Seat.SOUTH,
            heartsBroken  = false,
            passDirection = PassDirection.LEFT,
            handNumber    = 1,
            selectedCards = emptySet(),
            trickNumber   = 1,
            config        = config
        )
    }
}
