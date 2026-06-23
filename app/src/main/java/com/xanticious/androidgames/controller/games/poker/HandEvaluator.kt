package com.xanticious.androidgames.controller.games.poker

import com.xanticious.androidgames.model.games.cards.Card

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
 * Comparable hand rank.
 *
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
 * where it contributes a high card of 5 (the lowest possible straight).
 */
object HandEvaluator {

    /** Evaluate a five-card hand. Input must contain exactly 5 cards. */
    fun evaluate(hand: List<Card>): HandRank {
        require(hand.size == 5) { "Expected 5 cards, got ${hand.size}" }
        val values = hand.map { it.rank.highValue }.sortedDescending()
        val valueCounts = buildCounts(values)
        val isFlush = hand.map { it.suit }.toSet().size == 1
        val isWheel = values.toSet() == setOf(14, 2, 3, 4, 5)
        val isNormalStraight = values.toSet().size == 5 && (values.first() - values.last() == 4)
        val isStraight = isNormalStraight || isWheel
        val straightHigh = if (isWheel) 5 else values.first()
        return classifyHand(values, valueCounts, isFlush, isStraight, isNormalStraight, straightHigh)
    }

    /** Human-readable category name for [hand]. */
    fun displayName(hand: List<Card>): String = evaluate(hand).category.displayName

    /**
     * Find the index/indices of the best hand(s) among the given active hands.
     * Returns all indices that share the top [HandRank] (split-pot support).
     */
    fun findWinners(hands: Map<Int, List<Card>>): List<Int> {
        if (hands.isEmpty()) return emptyList()
        val ranked = hands.mapValues { (_, cards) -> evaluate(cards) }
        val best = ranked.values.maxOrNull() ?: return emptyList()
        return ranked.entries.filter { it.value.compareTo(best) == 0 }.map { it.key }
    }

    // ---- private helpers ----

    private fun buildCounts(values: List<Int>): Map<Int, Int> =
        values.groupBy { it }.mapValues { it.value.size }

    private fun classifyHand(
        values: List<Int>,
        counts: Map<Int, Int>,
        isFlush: Boolean,
        isStraight: Boolean,
        isNormalStraight: Boolean,
        straightHigh: Int
    ): HandRank {
        val maxCount = counts.values.maxOrNull() ?: 0
        if (isFlush && isNormalStraight && values.first() == 14) return royalFlush()
        if (isFlush && isStraight) return straightFlush(straightHigh)
        if (maxCount == 4) return fourOfAKind(counts)
        if (maxCount == 3 && counts.values.contains(2)) return fullHouse(counts)
        if (isFlush) return flush(values)
        if (isStraight) return straight(straightHigh)
        if (maxCount == 3) return threeOfAKind(counts)
        if (counts.values.count { it == 2 } == 2) return twoPair(counts)
        if (maxCount == 2) return onePair(counts)
        return highCard(values)
    }

    private fun royalFlush() = HandRank(HandCategory.ROYAL_FLUSH, listOf(14))

    private fun straightFlush(high: Int) = HandRank(HandCategory.STRAIGHT_FLUSH, listOf(high))

    private fun fourOfAKind(counts: Map<Int, Int>): HandRank {
        val quad = counts.entries.first { it.value == 4 }.key
        val kicker = counts.entries.first { it.value == 1 }.key
        return HandRank(HandCategory.FOUR_OF_A_KIND, listOf(quad, kicker))
    }

    private fun fullHouse(counts: Map<Int, Int>): HandRank {
        val trip = counts.entries.first { it.value == 3 }.key
        val pair = counts.entries.first { it.value == 2 }.key
        return HandRank(HandCategory.FULL_HOUSE, listOf(trip, pair))
    }

    private fun flush(values: List<Int>) = HandRank(HandCategory.FLUSH, values)

    private fun straight(high: Int) = HandRank(HandCategory.STRAIGHT, listOf(high))

    private fun threeOfAKind(counts: Map<Int, Int>): HandRank {
        val trip = counts.entries.first { it.value == 3 }.key
        val kickers = counts.entries.filter { it.value == 1 }.map { it.key }.sortedDescending()
        return HandRank(HandCategory.THREE_OF_A_KIND, listOf(trip) + kickers)
    }

    private fun twoPair(counts: Map<Int, Int>): HandRank {
        val pairs = counts.entries.filter { it.value == 2 }.map { it.key }.sortedDescending()
        val kicker = counts.entries.first { it.value == 1 }.key
        return HandRank(HandCategory.TWO_PAIR, listOf(pairs[0], pairs[1], kicker))
    }

    private fun onePair(counts: Map<Int, Int>): HandRank {
        val pairVal = counts.entries.first { it.value == 2 }.key
        val kickers = counts.entries.filter { it.value == 1 }.map { it.key }.sortedDescending()
        return HandRank(HandCategory.ONE_PAIR, listOf(pairVal) + kickers)
    }

    private fun highCard(values: List<Int>) = HandRank(HandCategory.HIGH_CARD, values)
}
