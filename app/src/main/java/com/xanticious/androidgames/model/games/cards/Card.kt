package com.xanticious.androidgames.model.games.cards

/**
 * Shared playing-card primitives for every game in the Card category.
 *
 * Pure Kotlin — no Android, Compose or UI imports — so card logic stays
 * unit-testable on the JVM. Rendering lives in
 * `view/common/cards/PlayingCardView.kt`; game-specific rules live in each
 * game's own controller.
 *
 * Rank values follow their printed pip count (Ace = 1 … King = 13). Games that
 * treat the Ace as high read [Rank.highValue] or add their own offset; this
 * model stays neutral so it can back Klondike (Ace low) and Poker (Ace high)
 * alike.
 */
enum class CardColor { RED, BLACK }

enum class Suit(val symbol: String, val color: CardColor) {
    CLUBS("\u2663", CardColor.BLACK),
    DIAMONDS("\u2666", CardColor.RED),
    HEARTS("\u2665", CardColor.RED),
    SPADES("\u2660", CardColor.BLACK);
}

enum class Rank(val value: Int, val label: String) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    /** Ace counted high (14); every other rank keeps its [value]. */
    val highValue: Int get() = if (this == ACE) 14 else value
}

/**
 * A single immutable playing card. [faceUp] travels with the card so piles can
 * mix hidden and revealed cards (e.g. Klondike tableau) without a parallel list.
 */
data class Card(
    val rank: Rank,
    val suit: Suit,
    val faceUp: Boolean = true
) {
    val color: CardColor get() = suit.color

    /** Short human label such as `A\u2660` or `10\u2665`. */
    val label: String get() = "${rank.label}${suit.symbol}"

    fun faceUp(): Card = if (faceUp) this else copy(faceUp = true)
    fun faceDown(): Card = if (!faceUp) this else copy(faceUp = false)
}
