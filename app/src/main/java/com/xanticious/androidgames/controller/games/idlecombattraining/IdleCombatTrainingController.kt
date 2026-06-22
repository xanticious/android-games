package com.xanticious.androidgames.controller.games.idlecombattraining

import com.xanticious.androidgames.model.games.idlecombattraining.CombatStep
import com.xanticious.androidgames.model.games.idlecombattraining.CombatStepEvent
import com.xanticious.androidgames.model.games.idlecombattraining.CombatUpgrade
import com.xanticious.androidgames.model.games.idlecombattraining.Dummy
import com.xanticious.androidgames.model.games.idlecombattraining.IdleCombatState
import com.xanticious.androidgames.model.games.idlecombattraining.LastMoveResult
import com.xanticious.androidgames.model.games.idlecombattraining.Move
import com.xanticious.androidgames.model.games.idlecombattraining.MoveId
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

class IdleCombatTrainingController(private val random: Random = Random.Default) {
    fun step(state: IdleCombatState, dt: Float): CombatStep {
        val newTimer = state.moveTimer + dt
        if (newTimer < state.moveInterval) {
            return CombatStep(state.copy(moveTimer = newTimer), CombatStepEvent.NONE, 0L)
        }

        val attempts = (newTimer / state.moveInterval).toInt().coerceAtLeast(1)
        val remainder = newTimer - attempts * state.moveInterval
        var nextState = state.copy(moveTimer = remainder)
        var lastEvent = CombatStepEvent.NONE
        var totalCoinsEarned = 0L

        repeat(attempts) {
            val moveIndex = nextUnlockedMoveIndex(nextState.moves, nextState.currentMoveIndex)
            val move = nextState.moves[moveIndex]
            val hitChance = (move.baseHitChance + calcHitChanceBonus(nextState.upgrades)).coerceIn(0f, 0.99f)
            val roll = random.nextFloat()
            val hit = roll < hitChance
            val crit = hit && hasUpgrade(nextState.upgrades, "iron-fist") && roll < hitChance * 0.25f
            val nextMoveIndex = (moveIndex + 1) % nextState.moves.size

            if (!hit) {
                nextState = nextState.copy(
                    currentMoveIndex = nextMoveIndex,
                    hitStreak = 0,
                    lastMoveResult = LastMoveResult.MISS
                )
                nextState = nextState.copy(
                    moves = updateUnlockedMoves(
                        moves = nextState.moves,
                        upgrades = nextState.upgrades,
                        dummiesDefeated = nextState.dummiesDefeated,
                        hitStreak = nextState.hitStreak
                    )
                )
                if (lastEvent != CombatStepEvent.DUMMY_DEFEATED) {
                    lastEvent = CombatStepEvent.MISS_FIRED
                }
                return@repeat
            }

            val damage = (move.baseDamage + calcDamageBonus(nextState.upgrades)) * if (crit) 2 else 1
            val actualDamage = damage.toLong().coerceAtMost(nextState.dummy.hp)
            val remainingHp = nextState.dummy.hp - damage
            val newHitStreak = nextState.hitStreak + 1
            val result = if (crit) LastMoveResult.CRITICAL_HIT else LastMoveResult.HIT

            if (remainingHp <= 0L) {
                val reward = nextState.dummy.reward
                totalCoinsEarned += reward
                nextState = nextState.copy(
                    coins = nextState.coins + reward,
                    dummy = generateDummy(nextState.dummy.number + 1),
                    dummiesDefeated = nextState.dummiesDefeated + 1,
                    currentMoveIndex = nextMoveIndex,
                    hitStreak = newHitStreak,
                    lastMoveResult = result,
                    totalDamageDealt = nextState.totalDamageDealt + actualDamage
                )
                nextState = nextState.copy(
                    moves = updateUnlockedMoves(
                        moves = nextState.moves,
                        upgrades = nextState.upgrades,
                        dummiesDefeated = nextState.dummiesDefeated,
                        hitStreak = nextState.hitStreak
                    )
                )
                lastEvent = CombatStepEvent.DUMMY_DEFEATED
            } else {
                nextState = nextState.copy(
                    dummy = nextState.dummy.copy(hp = remainingHp),
                    currentMoveIndex = nextMoveIndex,
                    hitStreak = newHitStreak,
                    lastMoveResult = result,
                    totalDamageDealt = nextState.totalDamageDealt + actualDamage
                )
                nextState = nextState.copy(
                    moves = updateUnlockedMoves(
                        moves = nextState.moves,
                        upgrades = nextState.upgrades,
                        dummiesDefeated = nextState.dummiesDefeated,
                        hitStreak = nextState.hitStreak
                    )
                )
                if (lastEvent != CombatStepEvent.DUMMY_DEFEATED) {
                    lastEvent = CombatStepEvent.HIT_LANDED
                }
            }
        }

        return CombatStep(nextState, lastEvent, totalCoinsEarned)
    }

    fun buyUpgrade(state: IdleCombatState, upgradeId: String): IdleCombatState? {
        val upgrade = state.upgrades.firstOrNull { it.id == upgradeId } ?: return null
        if (upgrade.purchased || state.coins < upgrade.cost) return null
        val purchasedIds = state.upgrades.asSequence().filter { it.purchased }.map { it.id }.toSet()
        if (!upgrade.requires.all { it in purchasedIds }) return null

        val updatedUpgrades = state.upgrades.map {
            if (it.id == upgradeId) it.copy(purchased = true) else it
        }
        val updatedMoves = updateUnlockedMoves(
            moves = state.moves,
            upgrades = updatedUpgrades,
            dummiesDefeated = state.dummiesDefeated,
            hitStreak = state.hitStreak
        )
        return state.copy(
            coins = state.coins - upgrade.cost,
            upgrades = updatedUpgrades,
            moves = updatedMoves,
            moveInterval = calcMoveInterval(updatedUpgrades)
        )
    }

    internal fun generateDummy(number: Int): Dummy = when (number) {
        1 -> Dummy(number = 1, maxHp = 20L, hp = 20L, reward = 10L)
        in 2..5 -> {
            val hp = 30L + (number - 2L) * 7L
            val reward = 15L + (number - 2L) * 6L
            Dummy(number = number, maxHp = hp, hp = hp, reward = reward)
        }
        in 6..15 -> {
            val hp = 80L + (number - 6L) * 12L
            val reward = 50L + (number - 6L) * 15L
            Dummy(number = number, maxHp = hp, hp = hp, reward = reward)
        }
        in 16..30 -> {
            val hp = 250L + (number - 16L) * 23L
            val reward = 250L + (number - 16L) * 36L
            Dummy(number = number, maxHp = hp, hp = hp, reward = reward)
        }
        else -> {
            val exponent = (number - 30).toDouble()
            val hp = (800.0 * 1.4.pow(exponent)).roundToLong().coerceAtLeast(800L)
            val reward = (800.0 * 1.3.pow(exponent)).roundToLong().coerceAtLeast(800L)
            Dummy(number = number, maxHp = hp, hp = hp, reward = reward)
        }
    }

    internal fun updateUnlockedMoves(
        moves: List<Move>,
        upgrades: List<CombatUpgrade>,
        dummiesDefeated: Int,
        hitStreak: Int
    ): List<Move> {
        val purchasedIds = upgrades.asSequence().filter { it.purchased }.map { it.id }.toSet()
        return moves.map { move ->
            val unlocked = when (move.id) {
                MoveId.CLUMSY_JAB -> true
                MoveId.FRONT_KICK -> dummiesDefeated >= 5
                MoveId.PALM_STRIKE -> "basic-footwork" in purchasedIds || "stance-training" in purchasedIds
                MoveId.SPINNING_KICK -> dummiesDefeated >= 20 || "advanced-kata" in purchasedIds
                MoveId.ELBOW_DROP -> "speed-drills" in purchasedIds
                MoveId.COMBO_FINISHER -> "focus-technique" in purchasedIds || hitStreak >= 3
            }
            move.copy(unlocked = unlocked)
        }
    }

    internal fun calcHitChanceBonus(upgrades: List<CombatUpgrade>): Float {
        var bonus = 0f
        if (hasUpgrade(upgrades, "basic-footwork")) bonus += 0.10f
        if (hasUpgrade(upgrades, "stance-training")) bonus += 0.05f
        if (hasUpgrade(upgrades, "masters-discipline")) bonus += 0.15f
        return bonus
    }

    internal fun calcDamageBonus(upgrades: List<CombatUpgrade>): Int {
        var bonus = 0
        if (hasUpgrade(upgrades, "stance-training")) bonus += 1
        if (hasUpgrade(upgrades, "strength-conditioning")) bonus += 3
        return bonus
    }

    internal fun calcMoveInterval(upgrades: List<CombatUpgrade>): Float =
        if (hasUpgrade(upgrades, "speed-drills")) 1f / 1.5f else 1f

    private fun hasUpgrade(upgrades: List<CombatUpgrade>, upgradeId: String): Boolean =
        upgrades.any { it.id == upgradeId && it.purchased }

    private fun nextUnlockedMoveIndex(moves: List<Move>, startIndex: Int): Int {
        for (offset in moves.indices) {
            val index = (startIndex + offset) % moves.size
            if (moves[index].unlocked) {
                return index
            }
        }
        return startIndex.coerceIn(0, moves.lastIndex)
    }
}
