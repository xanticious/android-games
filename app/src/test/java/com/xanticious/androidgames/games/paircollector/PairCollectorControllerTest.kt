package com.xanticious.androidgames.games.paircollector

import com.xanticious.androidgames.controller.games.paircollector.PairCollectorController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.paircollector.PairCollectorGameState
import com.xanticious.androidgames.model.games.paircollector.PairCollectorOutcome
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairCollectorControllerTest {
    private val controller = PairCollectorController()

    @Test
    fun generateRound_cardCount_matchesSetting() {
        val round = controller.generateRound(cardCount = 20, previousDuplicate = null, random = Random(1))
        assertEquals(20, round.cards.size)
    }

    @Test
    fun generateRound_duplicate_appearsExactlyTwice() {
        val round = controller.generateRound(cardCount = 16, previousDuplicate = null, random = Random(2))
        assertEquals(2, round.cards.count { it == round.duplicate })
    }

    @Test
    fun generateRound_previousDuplicate_isNotRepeated() {
        val first = controller.generateRound(cardCount = 30, previousDuplicate = null, random = Random(3))
        val second = controller.generateRound(cardCount = 30, previousDuplicate = first.duplicate, random = Random(4))
        assertFalse(first.duplicate == second.duplicate)
    }

    @Test
    fun isDuplicatePair_twoDuplicateIndexes_returnsTrue() {
        val round = controller.generateRound(cardCount = 10, previousDuplicate = null, random = Random(5))
        val indexes = round.cards.indices.filter { round.cards[it] == round.duplicate }
        assertTrue(controller.isDuplicatePair(round, indexes[0], indexes[1]))
    }

    @Test
    fun addStrike_thirdStrike_setsDefeat() {
        val config = controller.configFor(GameDifficulty.MEDIUM)
        val round = controller.generateRound(cardCount = config.cardCount, previousDuplicate = null, random = Random(6))
        val state = PairCollectorGameState.initial(round).copy(strikes = 2)
        assertEquals(PairCollectorOutcome.DEFEAT, controller.addStrike(state, config).outcome)
    }

    @Test
    fun completeRound_tenthRound_setsVictory() {
        val config = controller.configFor(GameDifficulty.MEDIUM)
        val round = controller.generateRound(cardCount = config.cardCount, previousDuplicate = null, random = Random(7))
        val next = controller.generateRound(cardCount = config.cardCount, previousDuplicate = round.duplicate, random = Random(8))
        val state = PairCollectorGameState.initial(round).copy(completedRounds = 9, roundNumber = 10)
        assertEquals(PairCollectorOutcome.VICTORY, controller.completeRound(state, config, next).outcome)
    }
}
