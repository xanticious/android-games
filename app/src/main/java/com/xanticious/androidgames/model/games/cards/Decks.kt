package com.xanticious.androidgames.model.games.cards

import kotlin.random.Random

/**
 * Factory helpers for standard playing-card decks. Deterministic shuffles take
 * an explicit [seed] (or a [Random]) so deals can be reproduced in tests and in
 * "always solvable" game modes.
 */
object Decks {

    /** The 52 cards of a standard deck, face up, in suit-then-rank order. */
    val standard52: List<Card> =
        Suit.entries.flatMap { suit -> Rank.entries.map { rank -> Card(rank, suit) } }

    /** A fresh, unshuffled 52-card deck (face up). */
    fun standard(): List<Card> = standard52

    /** A 52-card deck shuffled with the given [random]. */
    fun shuffled(random: Random): List<Card> = standard52.shuffled(random)

    /** A 52-card deck shuffled deterministically from [seed]. */
    fun shuffled(seed: Long): List<Card> = shuffled(Random(seed))

    /** A 52-card deck dealt all face down, shuffled deterministically. */
    fun shuffledFaceDown(seed: Long): List<Card> = shuffled(seed).map { it.faceDown() }
}

/**
 * Deals [count] cards off the front of [this] list, returning the dealt cards
 * and the remaining stock. Returns the whole list when fewer than [count]
 * remain.
 */
fun List<Card>.deal(count: Int): Pair<List<Card>, List<Card>> {
    val n = count.coerceIn(0, size)
    return take(n) to drop(n)
}
