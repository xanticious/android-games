package com.xanticious.androidgames.games.memory

import com.xanticious.androidgames.controller.games.memory.MemoryController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.memory.MemoryOutcome
import com.xanticious.androidgames.model.games.memory.MemoryPlayer
import com.xanticious.androidgames.model.games.memory.MemoryResolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MemoryControllerTest {
    private val controller = MemoryController()
    private val config = controller.configFor(GameDifficulty.EASY)

    @Test
    fun deal_everySymbol_hasExactlyTwoCards() {
        val counts = controller.deal(config, seed = 7).cards.groupingBy { it.symbolId }.eachCount()
        assertTrue(counts.values.all { it == 2 })
    }

    @Test
    fun resolveFlipped_twoMatchingCards_marksPairMatched() {
        val state = matchingPairState()
        val resolved = controller.resolveFlipped(state)
        assertTrue(resolved.state.cards[0].matched && resolved.state.cards[1].matched)
    }

    @Test
    fun resolveFlipped_twoMatchingCards_scoresCurrentPlayer() {
        val state = matchingPairState()
        val resolved = controller.resolveFlipped(state)
        assertEquals(1, resolved.state.humanPairs)
    }

    @Test
    fun resolveFlipped_twoDifferentCards_reportsMiss() {
        val state = differentPairState()
        val resolved = controller.resolveFlipped(state)
        assertEquals(MemoryResolve.MISS, resolved.resolve)
    }

    @Test
    fun hideUnmatched_afterMiss_flipsCardsFaceDown() {
        val resolved = controller.resolveFlipped(differentPairState())
        val hidden = controller.hideUnmatched(resolved.state)
        assertTrue(!hidden.cards[0].faceUp && !hidden.cards[2].faceUp)
    }

    @Test
    fun resolveFlipped_allPairsMatched_setsGameOver() {
        val base = matchingPairState().copy(cards = matchingPairState().cards.take(2))
        val resolved = controller.resolveFlipped(base)
        assertEquals(MemoryOutcome.HUMAN_WIN, resolved.state.result)
    }

    @Test
    fun deal_sameSeed_isDeterministic() {
        val first = controller.deal(config, seed = 42).cards.map { it.symbolId }
        val second = controller.deal(config, seed = 42).cards.map { it.symbolId }
        assertEquals(first, second)
    }

    @Test
    fun aiChooseCards_knownPairWithFullAccuracy_returnsRememberedPair() {
        val state = controller.deal(config, seed = 1)
            .copy(seenSymbolByIndex = mapOf(0 to 3, 5 to 3))
        val choice = controller.aiChooseCards(state, memoryAccuracy = 1f, random = Random(1))
        assertEquals(0 to 5, choice)
    }

    private fun matchingPairState() = controller.deal(config, seed = 1).let { dealt ->
        val smallDeal = controller.deal(dealt.config.copy(rows = 1, columns = 2), seed = 1)
        controller.flipCard(controller.flipCard(smallDeal, 0), 1)
    }

    private fun differentPairState() = controller.deal(config, seed = 1).let { dealt ->
        val secondIndex = dealt.cards.indexOfFirst { it.symbolId != dealt.cards[0].symbolId }
        controller.flipCard(controller.flipCard(dealt, 0), secondIndex)
    }
}
