package com.xanticious.androidgames.controller.games.yahtzee

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeCategory
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeConfig
import com.xanticious.androidgames.model.games.yahtzee.YahtzeePlayer
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeResult
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeState
import kotlin.random.Random

class YahtzeeController {

    fun configFor(difficulty: GameDifficulty): YahtzeeConfig = when (difficulty) {
        GameDifficulty.EASY -> YahtzeeConfig(difficulty = difficulty, aiRandomness = 0.45)
        GameDifficulty.MEDIUM -> YahtzeeConfig(difficulty = difficulty, aiRandomness = 0.18)
        GameDifficulty.HARD -> YahtzeeConfig(difficulty = difficulty, aiRandomness = 0.04)
    }

    fun rollDice(state: YahtzeeState, random: Random): YahtzeeState {
        if (state.rollsLeft <= 0 || state.result != null) return state
        val dice = state.dice.mapIndexed { index, value ->
            if (state.held.getOrElse(index) { false }) value else random.nextInt(1, 7)
        }
        return state.copy(dice = dice, rollsLeft = state.rollsLeft - 1)
    }

    fun toggleHeld(state: YahtzeeState, index: Int): YahtzeeState {
        if (state.rollsLeft == YahtzeeState.MaxRolls || state.result != null || index !in state.held.indices) return state
        return state.copy(held = state.held.mapIndexed { i, held -> if (i == index) !held else held })
    }

    fun scoreFor(category: YahtzeeCategory, dice: List<Int>, yahtzeeWild: Boolean = false): Int {
        val counts = countsByFace(dice)
        val total = dice.sum()
        val distinct = dice.toSet()
        val isYahtzee = counts.values.any { it == YahtzeeState.DiceCount }
        return when (category) {
            YahtzeeCategory.ONES,
            YahtzeeCategory.TWOS,
            YahtzeeCategory.THREES,
            YahtzeeCategory.FOURS,
            YahtzeeCategory.FIVES,
            YahtzeeCategory.SIXES -> dice.filter { it == category.upperFace }.sum()
            YahtzeeCategory.THREE_OF_A_KIND -> if (counts.values.any { it >= 3 }) total else 0
            YahtzeeCategory.FOUR_OF_A_KIND -> if (counts.values.any { it >= 4 }) total else 0
            YahtzeeCategory.FULL_HOUSE -> if (yahtzeeWild || counts.values.filter { it > 0 }.sorted() == listOf(2, 3)) 25 else 0
            YahtzeeCategory.SMALL_STRAIGHT -> if (yahtzeeWild || hasStraight(distinct, 4)) 30 else 0
            YahtzeeCategory.LARGE_STRAIGHT -> if (yahtzeeWild || hasStraight(distinct, 5)) 40 else 0
            YahtzeeCategory.YAHTZEE -> if (isYahtzee) 50 else 0
            YahtzeeCategory.CHANCE -> total
        }
    }

    fun applyScore(state: YahtzeeState, category: YahtzeeCategory): YahtzeeState {
        if (state.result != null || !availableCategories(state, state.currentPlayer).contains(category)) return state
        val card = scorecardFor(state, state.currentPlayer)
        val yahtzeeBonusEarned = yahtzeeBonusEarned(card, state.dice, state.config)
        val score = scoreFor(category, state.dice, yahtzeeWild = yahtzeeBonusEarned > 0)
        val newCard = card + (category to score)
        val scored = when (state.currentPlayer) {
            YahtzeePlayer.HUMAN -> state.copy(
                playerScorecard = newCard,
                playerYahtzeeBonus = state.playerYahtzeeBonus + yahtzeeBonusEarned
            )
            YahtzeePlayer.AI -> state.copy(
                aiScorecard = newCard,
                aiYahtzeeBonus = state.aiYahtzeeBonus + yahtzeeBonusEarned
            )
        }
        return advanceAfterScore(scored)
    }

    fun availableCategories(state: YahtzeeState, player: YahtzeePlayer): List<YahtzeeCategory> =
        scorecardFor(state, player).filterValues { it == null }.keys.toList()

    fun upperSubtotal(scorecard: Map<YahtzeeCategory, Int?>): Int =
        scorecard.asSequence().filter { it.key.isUpper }.sumOf { it.value ?: 0 }

    fun upperBonus(scorecard: Map<YahtzeeCategory, Int?>, config: YahtzeeConfig): Int =
        if (upperSubtotal(scorecard) >= config.upperBonusThreshold) config.upperBonusPoints else 0

    fun total(scorecard: Map<YahtzeeCategory, Int?>, yahtzeeBonus: Int, config: YahtzeeConfig): Int =
        scorecard.values.sumOf { it ?: 0 } + upperBonus(scorecard, config) + yahtzeeBonus

    fun gameOver(state: YahtzeeState): Boolean =
        state.playerScorecard.values.all { it != null } && state.aiScorecard.values.all { it != null }

    fun chooseAiHolds(state: YahtzeeState, random: Random): List<Boolean> {
        val available = availableCategories(state, YahtzeePlayer.AI)
        if (available.isEmpty() || state.rollsLeft <= 0) return List(YahtzeeState.DiceCount) { true }
        val counts = countsByFace(state.dice)
        if (random.nextDouble() < state.config.aiRandomness) {
            return state.dice.map { random.nextBoolean() }
        }
        val target = targetFace(state, available)
        val bestCount = counts.maxByOrNull { it.value }
        val keepFace = when (state.config.difficulty) {
            GameDifficulty.EASY -> bestCount?.key ?: target
            GameDifficulty.MEDIUM -> if (counts[target].orZero() >= 2) target else bestCount?.key ?: target
            GameDifficulty.HARD -> hardKeepFace(state, available, target)
        }
        val straightTarget = available.any { it == YahtzeeCategory.SMALL_STRAIGHT || it == YahtzeeCategory.LARGE_STRAIGHT }
        if (straightTarget && state.config.difficulty == GameDifficulty.HARD && counts.values.maxOrNull().orZero() < 3) {
            val straightSet = bestStraightKeep(state.dice)
            return state.dice.map { it in straightSet }
        }
        return state.dice.map { it == keepFace }
    }

    fun chooseAiCategory(state: YahtzeeState, random: Random): YahtzeeCategory {
        val available = availableCategories(state, YahtzeePlayer.AI)
        require(available.isNotEmpty()) { "AI has no open Yahtzee categories" }
        if (random.nextDouble() < state.config.aiRandomness) return available[random.nextInt(available.size)]
        return available.maxBy { category -> aiCategoryValue(state, category) }
    }

    fun playAiTurn(state: YahtzeeState, random: Random): YahtzeeState {
        var next = state.copy(
            currentPlayer = YahtzeePlayer.AI,
            held = List(YahtzeeState.DiceCount) { false },
            rollsLeft = YahtzeeState.MaxRolls
        )
        repeat(state.config.aiRolls) { rollIndex ->
            next = rollDice(next, random)
            if (rollIndex < state.config.aiRolls - 1) {
                next = next.copy(held = chooseAiHolds(next, random))
            }
        }
        return applyScore(next, chooseAiCategory(next, random))
    }

    private fun advanceAfterScore(state: YahtzeeState): YahtzeeState {
        if (gameOver(state)) return state.copy(result = resultFor(state))
        return when (state.currentPlayer) {
            YahtzeePlayer.HUMAN -> state.copy(
                currentPlayer = YahtzeePlayer.AI,
                held = List(YahtzeeState.DiceCount) { false },
                rollsLeft = YahtzeeState.MaxRolls
            )
            YahtzeePlayer.AI -> state.copy(
                currentPlayer = YahtzeePlayer.HUMAN,
                round = (state.round + 1).coerceAtMost(state.config.totalRounds),
                held = List(YahtzeeState.DiceCount) { false },
                rollsLeft = YahtzeeState.MaxRolls
            )
        }
    }

    private fun resultFor(state: YahtzeeState): YahtzeeResult {
        val playerTotal = total(state.playerScorecard, state.playerYahtzeeBonus, state.config)
        val aiTotal = total(state.aiScorecard, state.aiYahtzeeBonus, state.config)
        return when {
            playerTotal > aiTotal -> YahtzeeResult.HUMAN_WIN
            aiTotal > playerTotal -> YahtzeeResult.AI_WIN
            else -> YahtzeeResult.DRAW
        }
    }

    private fun scorecardFor(state: YahtzeeState, player: YahtzeePlayer): Map<YahtzeeCategory, Int?> =
        when (player) {
            YahtzeePlayer.HUMAN -> state.playerScorecard
            YahtzeePlayer.AI -> state.aiScorecard
        }

    private fun yahtzeeBonusEarned(scorecard: Map<YahtzeeCategory, Int?>, dice: List<Int>, config: YahtzeeConfig): Int {
        val yahtzeeAlreadyScored = scorecard[YahtzeeCategory.YAHTZEE] == 50
        val currentRollIsYahtzee = countsByFace(dice).values.any { it == YahtzeeState.DiceCount }
        return if (yahtzeeAlreadyScored && currentRollIsYahtzee) config.yahtzeeBonusPoints else 0
    }

    private fun aiCategoryValue(state: YahtzeeState, category: YahtzeeCategory): Double {
        val wild = yahtzeeBonusEarned(state.aiScorecard, state.dice, state.config) > 0
        val rawScore = scoreFor(category, state.dice, wild)
        val baseWeight = when (category) {
            YahtzeeCategory.YAHTZEE -> 1.45
            YahtzeeCategory.LARGE_STRAIGHT -> 1.25
            YahtzeeCategory.SMALL_STRAIGHT -> 1.05
            YahtzeeCategory.FULL_HOUSE -> 1.05
            YahtzeeCategory.THREE_OF_A_KIND,
            YahtzeeCategory.FOUR_OF_A_KIND -> 0.95
            YahtzeeCategory.CHANCE -> 0.65
            else -> 1.0
        }
        val upperPressure = category.upperFace?.let { face ->
            val neededAverage = (state.config.upperBonusThreshold - upperSubtotal(state.aiScorecard)).coerceAtLeast(0) / 6.0
            val faceScore = state.dice.count { it == face } * face
            (faceScore - neededAverage) * when (state.config.difficulty) {
                GameDifficulty.EASY -> 0.15
                GameDifficulty.MEDIUM -> 0.55
                GameDifficulty.HARD -> 0.9
            }
        } ?: 0.0
        val zeroPenalty = if (rawScore == 0) when (category) {
            YahtzeeCategory.CHANCE -> 0.0
            YahtzeeCategory.ONES, YahtzeeCategory.TWOS -> 2.0
            else -> 14.0
        } else 0.0
        return rawScore * baseWeight + upperPressure - zeroPenalty + endgameWasteValue(state, category, rawScore)
    }

    private fun endgameWasteValue(state: YahtzeeState, category: YahtzeeCategory, rawScore: Int): Double {
        val remaining = availableCategories(state, YahtzeePlayer.AI).size
        if (remaining > 4 || rawScore > 0) return 0.0
        return when (category) {
            YahtzeeCategory.ONES -> 5.0
            YahtzeeCategory.TWOS -> 4.0
            YahtzeeCategory.CHANCE -> -10.0
            else -> 0.0
        }
    }

    private fun targetFace(state: YahtzeeState, available: List<YahtzeeCategory>): Int {
        val upperFaces = available.mapNotNull { it.upperFace }
        if (upperFaces.isEmpty()) return countsByFace(state.dice).maxByOrNull { it.value }?.key ?: 6
        return upperFaces.maxBy { face -> state.dice.count { it == face } * face + face }
    }

    private fun hardKeepFace(state: YahtzeeState, available: List<YahtzeeCategory>, fallback: Int): Int {
        val counts = countsByFace(state.dice)
        val yahtzeeOpen = YahtzeeCategory.YAHTZEE in available
        val kindOpen = available.any { it == YahtzeeCategory.THREE_OF_A_KIND || it == YahtzeeCategory.FOUR_OF_A_KIND }
        if (yahtzeeOpen || kindOpen) return counts.maxByOrNull { it.value * 10 + it.key }?.key ?: fallback
        return targetFace(state, available)
    }

    private fun bestStraightKeep(dice: List<Int>): Set<Int> {
        val sets = listOf(setOf(1, 2, 3, 4, 5), setOf(2, 3, 4, 5, 6), setOf(1, 2, 3, 4), setOf(2, 3, 4, 5), setOf(3, 4, 5, 6))
        val distinct = dice.toSet()
        return sets.maxBy { target -> target.count { it in distinct } }
    }

    private fun countsByFace(dice: List<Int>): Map<Int, Int> =
        (1..6).associateWith { face -> dice.count { it == face } }

    private fun hasStraight(distinct: Set<Int>, length: Int): Boolean =
        (1..(7 - length)).any { start -> (start until start + length).all { it in distinct } }

    private fun Int?.orZero(): Int = this ?: 0
}
