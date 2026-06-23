package com.xanticious.androidgames.controller.games.memory

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.memory.MemoryCard
import com.xanticious.androidgames.model.games.memory.MemoryConfig
import com.xanticious.androidgames.model.games.memory.MemoryOutcome
import com.xanticious.androidgames.model.games.memory.MemoryPlayer
import com.xanticious.androidgames.model.games.memory.MemoryResolve
import com.xanticious.androidgames.model.games.memory.MemoryState
import com.xanticious.androidgames.model.games.memory.MemoryStep
import kotlin.random.Random

class MemoryController {
    fun configFor(difficulty: GameDifficulty): MemoryConfig = when (difficulty) {
        GameDifficulty.EASY -> MemoryConfig(rows = 4, columns = 4, difficulty = difficulty, memoryAccuracy = 0.25f)
        GameDifficulty.MEDIUM -> MemoryConfig(rows = 4, columns = 5, difficulty = difficulty, memoryAccuracy = 0.65f)
        GameDifficulty.HARD -> MemoryConfig(rows = 6, columns = 6, difficulty = difficulty, memoryAccuracy = 0.95f)
    }

    fun deal(config: MemoryConfig, seed: Int): MemoryState {
        require((config.rows * config.columns) % 2 == 0) { "Memory board must have an even number of cards." }
        val shuffledSymbols = (0 until config.pairCount).flatMap { listOf(it, it) }.shuffled(Random(seed))
        val cards = shuffledSymbols.mapIndexed { index, symbolId -> MemoryCard(id = index, symbolId = symbolId) }
        return MemoryState.empty(config).copy(cards = cards)
    }

    fun flipCard(state: MemoryState, index: Int): MemoryState {
        if (state.result != MemoryOutcome.IN_PROGRESS) return state
        if (state.flippedIndices.size >= 2) return state
        val card = state.cards.getOrNull(index) ?: return state
        if (card.faceUp || card.matched) return state
        val cards = state.cards.replaceAt(index, card.copy(faceUp = true))
        return state.copy(
            cards = cards,
            flippedIndices = state.flippedIndices + index,
            seenSymbolByIndex = state.seenSymbolByIndex + (index to card.symbolId)
        )
    }

    fun resolveFlipped(state: MemoryState): MemoryStep {
        val firstIndex = state.flippedIndices.getOrNull(0) ?: return MemoryStep(state, MemoryResolve.NONE)
        val secondIndex = state.flippedIndices.getOrNull(1) ?: return MemoryStep(state, MemoryResolve.NONE)
        val first = state.cards.getOrNull(firstIndex) ?: return MemoryStep(state, MemoryResolve.NONE)
        val second = state.cards.getOrNull(secondIndex) ?: return MemoryStep(state, MemoryResolve.NONE)
        val moves = state.moves + 1
        return if (first.symbolId == second.symbolId) {
            val cards = state.cards
                .replaceAt(firstIndex, first.copy(faceUp = true, matched = true, matchedBy = state.currentPlayer))
                .replaceAt(secondIndex, second.copy(faceUp = true, matched = true, matchedBy = state.currentPlayer))
            val humanPairs = state.humanPairs + if (state.currentPlayer == MemoryPlayer.HUMAN) 1 else 0
            val aiPairs = state.aiPairs + if (state.currentPlayer == MemoryPlayer.AI) 1 else 0
            val outcome = outcomeFor(cards, humanPairs, aiPairs)
            MemoryStep(
                state = state.copy(
                    cards = cards,
                    flippedIndices = emptyList(),
                    humanPairs = humanPairs,
                    aiPairs = aiPairs,
                    moves = moves,
                    result = outcome
                ),
                resolve = if (outcome == MemoryOutcome.IN_PROGRESS) MemoryResolve.MATCH else MemoryResolve.GAME_OVER
            )
        } else {
            MemoryStep(state.copy(moves = moves), MemoryResolve.MISS)
        }
    }

    fun hideUnmatched(state: MemoryState): MemoryState {
        val firstIndex = state.flippedIndices.getOrNull(0) ?: return state
        val secondIndex = state.flippedIndices.getOrNull(1) ?: return state
        val cards = state.cards
            .replaceAt(firstIndex, state.cards[firstIndex].copy(faceUp = false))
            .replaceAt(secondIndex, state.cards[secondIndex].copy(faceUp = false))
        return state.copy(
            cards = cards,
            flippedIndices = emptyList(),
            currentPlayer = state.currentPlayer.opponent(),
            turnNumber = state.turnNumber + 1
        )
    }

    fun aiChooseCards(state: MemoryState, memoryAccuracy: Float, random: Random = Random.Default): Pair<Int, Int>? {
        val hiddenUnmatched = state.cards.filter { !it.faceUp && !it.matched }.map { it.id }
        if (hiddenUnmatched.size < 2) return null

        rememberedPair(state, memoryAccuracy.coerceIn(0f, 1f), random)?.let { return it }

        val unknown = hiddenUnmatched.filter { it !in state.seenSymbolByIndex }
        val first = unknown.randomOrNull(random) ?: hiddenUnmatched.random(random)
        val secondCandidates = hiddenUnmatched.filter { it != first }
        val matchingKnown = state.seenSymbolByIndex[first]?.let { symbol ->
            secondCandidates.firstOrNull { state.seenSymbolByIndex[it] == symbol }
        }
        val second = matchingKnown ?: (secondCandidates.filter { it !in state.seenSymbolByIndex }.randomOrNull(random)
            ?: secondCandidates.random(random))
        return first to second
    }

    private fun rememberedPair(state: MemoryState, memoryAccuracy: Float, random: Random): Pair<Int, Int>? =
        state.seenSymbolByIndex.asSequence()
            .filter { (index, _) -> state.cards.getOrNull(index)?.let { !it.matched && !it.faceUp } == true }
            .groupBy({ it.value }, { it.key })
            .values
            .mapNotNull { indexes -> if (indexes.size >= 2) indexes.take(2) else null }
            .filter { random.nextFloat() <= memoryAccuracy }
            .map { it[0] to it[1] }
            .firstOrNull()

    private fun outcomeFor(cards: List<MemoryCard>, humanPairs: Int, aiPairs: Int): MemoryOutcome {
        if (cards.any { !it.matched }) return MemoryOutcome.IN_PROGRESS
        return when {
            humanPairs > aiPairs -> MemoryOutcome.HUMAN_WIN
            aiPairs > humanPairs -> MemoryOutcome.AI_WIN
            else -> MemoryOutcome.TIE
        }
    }

    private fun MemoryPlayer.opponent(): MemoryPlayer = when (this) {
        MemoryPlayer.HUMAN -> MemoryPlayer.AI
        MemoryPlayer.AI -> MemoryPlayer.HUMAN
    }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
        mapIndexed { i, item -> if (i == index) value else item }
}
