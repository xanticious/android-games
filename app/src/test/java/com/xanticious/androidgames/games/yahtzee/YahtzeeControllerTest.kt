package com.xanticious.androidgames.games.yahtzee

import com.xanticious.androidgames.controller.games.yahtzee.YahtzeeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeCategory
import com.xanticious.androidgames.model.games.yahtzee.YahtzeePlayer
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.random.Random

class YahtzeeControllerTest {
    private val controller = YahtzeeController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun scoreFor_fullHouse_scoresTwentyFive() {
        assertEquals(25, controller.scoreFor(YahtzeeCategory.FULL_HOUSE, listOf(2, 2, 3, 3, 3)))
    }

    @Test
    fun scoreFor_smallStraight_scoresThirty() {
        assertEquals(30, controller.scoreFor(YahtzeeCategory.SMALL_STRAIGHT, listOf(1, 2, 3, 4, 6)))
    }

    @Test
    fun scoreFor_largeStraight_scoresForty() {
        assertEquals(40, controller.scoreFor(YahtzeeCategory.LARGE_STRAIGHT, listOf(2, 3, 4, 5, 6)))
    }

    @Test
    fun scoreFor_yahtzee_scoresFifty() {
        assertEquals(50, controller.scoreFor(YahtzeeCategory.YAHTZEE, listOf(5, 5, 5, 5, 5)))
    }

    @Test
    fun upperBonus_atThreshold_scoresThirtyFive() {
        val card = mapOf(
            YahtzeeCategory.ONES to 3,
            YahtzeeCategory.TWOS to 6,
            YahtzeeCategory.THREES to 9,
            YahtzeeCategory.FOURS to 12,
            YahtzeeCategory.FIVES to 15,
            YahtzeeCategory.SIXES to 18
        )
        assertEquals(35, controller.upperBonus(card, config))
    }

    @Test
    fun rollDice_heldDice_preservesHeldValues() {
        val state = YahtzeeState.initial(config).copy(
            dice = listOf(6, 1, 3, 2, 4),
            held = listOf(true, false, true, false, false)
        )
        val rolled = controller.rollDice(state, Random(7))
        assertEquals(listOf(6, 3), listOf(rolled.dice[0], rolled.dice[2]))
    }

    @Test
    fun applyScore_extraYahtzee_addsBonus() {
        val state = YahtzeeState.initial(config).copy(
            dice = listOf(6, 6, 6, 6, 6),
            rollsLeft = 0,
            playerScorecard = YahtzeeCategory.entries.associateWith { category -> if (category == YahtzeeCategory.YAHTZEE) 50 else null }
        )
        val scored = controller.applyScore(state, YahtzeeCategory.CHANCE)
        assertEquals(100, scored.playerYahtzeeBonus)
    }

    @Test
    fun gameOver_afterThirteenCategories_setsResult() {
        val almostFilled = YahtzeeCategory.entries.associateWith { category -> if (category == YahtzeeCategory.CHANCE) null else 0 }
        val state = YahtzeeState.initial(config).copy(
            dice = listOf(1, 2, 3, 4, 5),
            currentPlayer = YahtzeePlayer.AI,
            playerScorecard = YahtzeeCategory.entries.associateWith { 0 },
            aiScorecard = almostFilled
        )
        val scored = controller.applyScore(state, YahtzeeCategory.CHANCE)
        assertNotNull(scored.result)
    }
}
