package com.xanticious.androidgames.controller.games.poker

import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank

/** Hand categories ordered from weakest (0) to strongest (9). */
enum class HandCategory(val rank: Int, val displayName: String) {
    HIGH_CARD(0, "High Card"),
    ONE_PAIR(1, "One Pair"),
    TWO_PAIR(2, "Two Pair"),
    THREE_OF_A_KIND(3, "Three of a Kind"),
    STRAIGHT(4, "Straight"),
    FLUSH(5, "Flush"),
    FULL_HOUSE(6, "Full House"),
    FOUR_OF_A_KIND(7, "Four of a Kind"),
    STRAIGHT_FLUSH(8, "Straight Flush"),
    ROYAL_FLUSH(9, "Royal Flush")
}

/**
 * Comparable representation of a five-card hand.
 * Compare [category] first; break ties element-by-element through [tiebreakers]
 * (most-significant discriminator first — e.g. pair rank, then kicker1, kicker2 …).
 */
data class HandRank(
    val category: HandCategory,
    val tiebreakers: List<Int>
) : Comparable<HandRank> {
    override fun compareTo(other: HandRank): Int {
        val catCmp = category.rank.compareTo(other.category.rank)
        if (catCmp != 0) return catCmp
        for (i in tiebreakers.indices) {
            val cmp = tiebreakers[i].compareTo(other.tiebreakers.getOrElse(i) { 0 })
            if (cmp != 0) return cmp
        }
        return 0
    }
}

/**
 * Pure five-card hand evaluator.
 *
 * Ace is always treated as high (14) except in the "wheel" straight A-2-3-4-5,
 * where it contributes a high card of 5 (the lowest straight).
 */
object HandEvaluator {

    /**
     * Evaluate a five-card hand and return its [HandRank].
     * The input list must contain exactly 5 cards.
     */
    fun evaluate(hand: List<Card>): HandRank {
        require(hand.size == 5) { "Hand must contain exactly 5 cards, got ${hand.size}" }

        // Use highValue so ACE = 14.
        val values = hand.map { it.rank.highValue }.sortedDescending()
        val suits = hand.map { it.suit }
        val valueCounts: Map<Int, Int> = values.groupBy { it }.mapValues { it.value.size }
        val counts: List<Int> = valueCounts.values.sortedDescending()

        val isFlush = suits.toSet().size == 1
        // Wheel: A-2-3-4-5 — Ace acts as 1, high-card of the straight is 5.
        val isWheel = values.toSet() == setOf(14, 2, 3, 4, 5)
        // Normal straight: 5 distinct values that span exactly 4.
        val isNormalStraight = values.toSet().size == 5 && (values.first() - values.last() == 4)
        val isStraight = isNormalStraight || isWheel
        val straightHigh = if (isWheel) 5 else values.first()

        return when {
            // Royal Flush: A-K-Q-J-10 same suit.
            isFlush && isNormalStraight && values.first() == 14 ->
                HandRank(HandCategory.ROYAL_FLUSH, listOf(14))

            // Straight Flush (non-royal).
            isFlush && isStraight ->
                HandRank(HandCategory.STRAIGHT_FLUSH, listOf(straightHigh))

            // Four of a Kind: tie-break by quad rank then kicker.
            counts == listOf(4, 1) -> {
                val quadVal = valueCounts.entries.first { it.value == 4 }.key
                val kicker = valueCounts.entries.first { it.value == 1 }.key
                HandRank(HandCategory.FOUR_OF_A_KIND, listOf(quadVal, kicker))
            }

            // Full House: tie-break by trip rank then pair rank.
            counts == listOf(3, 2) -> {
                val tripVal = valueCounts.entries.first { it.value == 3 }.key
                val pairVal = valueCounts.entries.first { it.value == 2 }.key
                HandRank(HandCategory.FULL_HOUSE, listOf(tripVal, pairVal))
            }

            // Flush: tie-break by all five card values high-to-low.
            isFlush ->
                HandRank(HandCategory.FLUSH, values)

            // Straight (including wheel).
            isStraight ->
                HandRank(HandCategory.STRAIGHT, listOf(straightHigh))

            // Three of a Kind: tie-break by trip rank then two kickers.
            counts == listOf(3, 1, 1) -> {
                val tripVal = valueCounts.entries.first { it.value == 3 }.key
                val kickers = valueCounts.entries
                    .filter { it.value == 1 }
                    .map { it.key }
                    .sortedDescending()
                HandRank(HandCategory.THREE_OF_A_KIND, listOf(tripVal) + kickers)
            }

            // Two Pair: tie-break by high pair, low pair, then kicker.
            counts == listOf(2, 2, 1) -> {
                val pairs = valueCounts.entries
                    .filter { it.value == 2 }
                    .map { it.key }
                    .sortedDescending()
                val kicker = valueCounts.entries.first { it.value == 1 }.key
                HandRank(HandCategory.TWO_PAIR, listOf(pairs[0], pairs[1], kicker))
            }

            // One Pair: tie-break by pair rank then three kickers high-to-low.
            counts == listOf(2, 1, 1, 1) -> {
                val pairVal = valueCounts.entries.first { it.value == 2 }.key
                val kickers = valueCounts.entries
                    .filter { it.value == 1 }
                    .map { it.key }
                    .sortedDescending()
                HandRank(HandCategory.ONE_PAIR, listOf(pairVal) + kickers)
            }

            // High Card: all five values high-to-low.
            else ->
                HandRank(HandCategory.HIGH_CARD, values)
        }
    }

    /** Human-readable category name for [hand]. */
    fun displayName(hand: List<Card>): String = evaluate(hand).category.displayName

    /**
     * Rank multiple active hands and return the indices of the winner(s).
     * Returns all indices that share the best [HandRank] (split-pot ties).
     */
    fun findWinners(hands: Map<Int, List<Card>>): List<Int> {
        if (hands.isEmpty()) return emptyList()
        val ranked = hands.mapValues { (_, cards) -> evaluate(cards) }
        val best = ranked.values.maxOrNull() ?: return emptyList()
        return ranked.entries.filter { it.value.compareTo(best) == 0 }.map { it.key }
    }
}
