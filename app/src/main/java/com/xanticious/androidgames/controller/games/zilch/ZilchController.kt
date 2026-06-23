package com.xanticious.androidgames.controller.games.zilch

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.zilch.ZilchAiDecision
import com.xanticious.androidgames.model.games.zilch.ZilchConfig
import com.xanticious.androidgames.model.games.zilch.ZilchPlayer
import com.xanticious.androidgames.model.games.zilch.ZilchResult
import com.xanticious.androidgames.model.games.zilch.ZilchScore
import com.xanticious.androidgames.model.games.zilch.ZilchState
import kotlin.random.Random

class ZilchController {

    fun configFor(difficulty: GameDifficulty): ZilchConfig = when (difficulty) {
        GameDifficulty.EASY -> ZilchConfig(difficulty = difficulty, aiBankThreshold = 350, aiHotDiceBankThreshold = 600, aiMinimumRemainingDiceToPush = 4)
        GameDifficulty.MEDIUM -> ZilchConfig(difficulty = difficulty, aiBankThreshold = 650, aiHotDiceBankThreshold = 1_000, aiMinimumRemainingDiceToPush = 3)
        GameDifficulty.HARD -> ZilchConfig(difficulty = difficulty, aiBankThreshold = 900, aiHotDiceBankThreshold = 1_400, aiMinimumRemainingDiceToPush = 2)
    }

    fun rollDice(state: ZilchState, random: Random = Random.Default): ZilchState {
        if (state.result != null) return state
        val hotDice = state.locked.all { it }
        val rollingMask = state.locked.map { locked -> hotDice || !locked }
        val nextDice = state.dice.mapIndexed { index, value -> if (rollingMask[index]) random.nextInt(1, 7) else value }
        val nextLocked = if (hotDice) List(ZilchState.DICE_COUNT) { false } else state.locked
        val rolledDice = nextDice.filterIndexed { index, _ -> rollingMask[index] }
        return if (detectZilch(rolledDice)) {
            endTurn(
                state.copy(
                    dice = nextDice,
                    locked = nextLocked,
                    selected = List(ZilchState.DICE_COUNT) { false },
                    turnPoints = 0,
                    lastRollZilch = true,
                    message = "Zilch! No scoring dice, turn points lost."
                )
            )
        } else {
            state.copy(
                dice = nextDice,
                locked = nextLocked,
                selected = List(ZilchState.DICE_COUNT) { false },
                lastRollZilch = false,
                message = "Choose scoring dice, then bank or roll again."
            )
        }
    }

    fun toggleSelection(state: ZilchState, index: Int): ZilchState {
        if (index !in 0 until ZilchState.DICE_COUNT || state.locked[index] || state.result != null) return state
        return state.copy(selected = state.selected.mapIndexed { i, selected -> if (i == index) !selected else selected })
    }

    fun setAside(state: ZilchState, selectedIndices: Set<Int> = state.selectedIndices): ZilchState {
        if (selectedIndices.isEmpty() || state.result != null) return state.copy(message = "Select at least one scoring die or combo.")
        if (selectedIndices.any { it !in 0 until ZilchState.DICE_COUNT || state.locked[it] }) {
            return state.copy(message = "Only newly rolled dice can be set aside.")
        }
        val selectedDice = selectedIndices.sorted().map { state.dice[it] }
        val score = scoreSelection(selectedDice)
        if (!score.valid) return state.copy(message = "That selection does not score.")
        val nextLocked = state.locked.mapIndexed { index, locked -> locked || index in selectedIndices }
        val hotDice = nextLocked.all { it }
        return state.copy(
            locked = nextLocked,
            selected = List(ZilchState.DICE_COUNT) { false },
            turnPoints = state.turnPoints + score.points,
            lastRollZilch = false,
            message = if (hotDice) "${score.description}: +${score.points}. Hot dice! Roll all six again or bank." else "${score.description}: +${score.points}."
        )
    }

    fun bank(state: ZilchState): ZilchState {
        if (state.result != null || state.turnPoints <= 0) return state
        val playerBanked = if (state.currentPlayer == ZilchPlayer.PLAYER) state.playerBanked + state.turnPoints else state.playerBanked
        val aiBanked = if (state.currentPlayer == ZilchPlayer.AI) state.aiBanked + state.turnPoints else state.aiBanked
        val result = when {
            playerBanked >= state.config.winningTarget -> ZilchResult.PLAYER_WIN
            aiBanked >= state.config.winningTarget -> ZilchResult.AI_WIN
            else -> null
        }
        val bankedBy = if (state.currentPlayer == ZilchPlayer.PLAYER) "You" else "AI"
        val bankMessage = "$bankedBy banked ${state.turnPoints}."
        return if (result != null) {
            state.copy(
                playerBanked = playerBanked,
                aiBanked = aiBanked,
                turnPoints = 0,
                locked = List(ZilchState.DICE_COUNT) { false },
                selected = List(ZilchState.DICE_COUNT) { false },
                result = result,
                message = bankMessage
            )
        } else {
            endTurn(
                state.copy(
                    playerBanked = playerBanked,
                    aiBanked = aiBanked,
                    turnPoints = 0,
                    locked = List(ZilchState.DICE_COUNT) { false },
                    selected = List(ZilchState.DICE_COUNT) { false },
                    message = bankMessage
                )
            )
        }
    }

    fun scoreSelection(dice: List<Int>): ZilchScore {
        if (dice.isEmpty() || dice.any { it !in 1..6 }) return ZilchScore(0, valid = false)
        val counts = countsFor(dice)
        if (dice.size == 6) {
            if ((1..6).all { counts[it] == 1 }) return ZilchScore(1_500, valid = true, description = "Straight")
            if (counts.count { it == 2 } == 3) return ZilchScore(1_500, valid = true, description = "Three pairs")
            if (counts.count { it == 3 } == 2) return ZilchScore(2_500, valid = true, description = "Two triples")
        }

        var points = 0
        val parts = mutableListOf<String>()
        val remaining = counts.copyOf()
        for (face in 1..6) {
            val count = remaining[face]
            if (count >= 3) {
                val setPoints = when (count) {
                    3 -> if (face == 1) 1_000 else face * 100
                    4 -> 1_000
                    5 -> 2_000
                    else -> 3_000
                }
                points += setPoints
                remaining[face] = 0
                parts += when (count) {
                    3 -> "three ${face}s"
                    4 -> "four ${face}s"
                    5 -> "five ${face}s"
                    else -> "six ${face}s"
                }
            }
        }
        points += remaining[1] * 100
        if (remaining[1] > 0) parts += "${remaining[1]} one${if (remaining[1] == 1) "" else "s"}"
        points += remaining[5] * 50
        if (remaining[5] > 0) parts += "${remaining[5]} five${if (remaining[5] == 1) "" else "s"}"
        remaining[1] = 0
        remaining[5] = 0

        val valid = points > 0 && remaining.all { it == 0 }
        return ZilchScore(points = if (valid) points else 0, valid = valid, description = if (valid) parts.joinToString(" + ") else "")
    }

    fun detectZilch(dice: List<Int>): Boolean {
        if (dice.isEmpty()) return true
        val counts = countsFor(dice)
        return counts[1] == 0 && counts[5] == 0 && counts.none { it >= 3 } &&
            !((1..6).all { counts[it] == 1 }) && counts.count { it == 2 } != 3 && counts.count { it == 3 } != 2
    }

    fun aiTurn(state: ZilchState, random: Random = Random.Default): ZilchState {
        var current = if (state.currentPlayer == ZilchPlayer.AI) state else state.copy(currentPlayer = ZilchPlayer.AI)
        var rolls = 0
        while (current.result == null && current.currentPlayer == ZilchPlayer.AI && rolls < 12) {
            current = rollDice(current, random)
            rolls += 1
            if (current.currentPlayer != ZilchPlayer.AI || current.lastRollZilch || current.result != null) break
            val decision = aiDecision(current)
            current = setAside(current, decision.selectedIndices)
            if (current.turnPoints <= 0) break
            if (decision.shouldBank || current.aiBanked + current.turnPoints >= current.config.winningTarget) {
                current = bank(current)
            }
        }
        return current
    }

    fun aiDecision(state: ZilchState): ZilchAiDecision {
        val rolledIndices = state.dice.indices.filter { !state.locked[it] }
        val selected = bestScoringIndices(state.dice, rolledIndices)
        val projectedScore = state.turnPoints + scoreSelection(selected.sorted().map { state.dice[it] }).points
        val willHaveHotDice = state.locked.mapIndexed { index, locked -> locked || index in selected }.all { it }
        val remainingAfterKeep = state.locked.indices.count { index -> !state.locked[index] && index !in selected }.let { if (it == 0) 6 else it }
        val distanceToTarget = state.config.winningTarget - state.aiBanked
        val pressureBonus = when (state.config.difficulty) {
            GameDifficulty.EASY -> 0
            GameDifficulty.MEDIUM -> if (state.aiBanked < state.playerBanked) 100 else 0
            GameDifficulty.HARD -> if (state.aiBanked < state.playerBanked) 250 else -100
        }
        val threshold = if (willHaveHotDice) state.config.aiHotDiceBankThreshold else state.config.aiBankThreshold
        val shouldBank = projectedScore >= distanceToTarget || projectedScore + pressureBonus >= threshold || remainingAfterKeep < state.config.aiMinimumRemainingDiceToPush
        return ZilchAiDecision(selectedIndices = selected, shouldBank = shouldBank)
    }

    private fun endTurn(state: ZilchState): ZilchState = state.copy(
        currentPlayer = if (state.currentPlayer == ZilchPlayer.PLAYER) ZilchPlayer.AI else ZilchPlayer.PLAYER,
        locked = List(ZilchState.DICE_COUNT) { false },
        selected = List(ZilchState.DICE_COUNT) { false }
    )

    private fun bestScoringIndices(dice: List<Int>, candidateIndices: List<Int>): Set<Int> {
        var best = emptySet<Int>()
        var bestScore = 0
        val totalMasks = 1 shl candidateIndices.size
        for (mask in 1 until totalMasks) {
            val indices = candidateIndices.filterIndexed { bit, _ -> mask and (1 shl bit) != 0 }.toSet()
            val score = scoreSelection(indices.sorted().map { dice[it] })
            if (score.valid && score.points > bestScore) {
                best = indices
                bestScore = score.points
            }
        }
        return best
    }

    private fun countsFor(dice: List<Int>): IntArray {
        val counts = IntArray(7)
        dice.forEach { value -> if (value in 1..6) counts[value] += 1 }
        return counts
    }
}
