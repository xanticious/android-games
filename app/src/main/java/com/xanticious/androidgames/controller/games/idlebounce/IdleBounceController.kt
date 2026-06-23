package com.xanticious.androidgames.controller.games.idlebounce

import com.xanticious.androidgames.model.games.idlebounce.Ball
import com.xanticious.androidgames.model.games.idlebounce.BounceUpgrade
import com.xanticious.androidgames.model.games.idlebounce.IdleBounceGameState
import com.xanticious.androidgames.model.games.idlebounce.IdleBounceStep
import com.xanticious.androidgames.model.games.idlebounce.IdleBounceStepEvent
import com.xanticious.androidgames.model.games.idlebounce.Layer
import com.xanticious.androidgames.model.games.idlebounce.LayerType
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

class IdleBounceController(private val random: Random = Random.Default) {
    fun step(state: IdleBounceGameState, dt: Float): IdleBounceStep {
        val newTimer = state.bounceTimer + dt
        val hitInterval = state.hitInterval
        if (newTimer < hitInterval) {
            return IdleBounceStep(
                state = state.copy(
                    bounceTimer = newTimer,
                    bedrockReached = state.bedrockReached || state.currentLayer.type == LayerType.BEDROCK
                ),
                event = IdleBounceStepEvent.NONE,
                coinsEarned = 0L
            )
        }

        val hits = (newTimer / hitInterval).toInt().coerceAtLeast(1)
        val remainder = newTimer - hits * hitInterval
        val ricochetEnabled = upgradeLevel(state.upgrades, "ricochet") > 0
        val luckyStrikeEnabled = upgradeLevel(state.upgrades, "lucky-strike") > 0
        val drillTipEnabled = upgradeLevel(state.upgrades, "drill-tip") > 0

        var nextState = state.copy(
            bounceTimer = remainder,
            bedrockReached = state.bedrockReached || state.currentLayer.type == LayerType.BEDROCK
        )
        var pendingCarryDamage = nextState.carryOverDamage
        var lastEvent = IdleBounceStepEvent.NONE
        var totalCoinsEarned = 0L

        repeat(hits) {
            var damage = nextState.ball.power
            if (ricochetEnabled) {
                damage *= 2L
            }

            val criticalHit = luckyStrikeEnabled && random.nextFloat() < 0.05f
            if (criticalHit) {
                damage *= 3L
            }

            damage += pendingCarryDamage
            pendingCarryDamage = 0L

            val remainingHp = nextState.currentLayer.hp - damage
            if (remainingHp > 0L) {
                nextState = nextState.copy(
                    currentLayer = nextState.currentLayer.copy(hp = remainingHp),
                    carryOverDamage = 0L,
                    lastHitCritical = criticalHit,
                    bedrockReached = nextState.bedrockReached || nextState.currentLayer.type == LayerType.BEDROCK
                )
                if (lastEvent != IdleBounceStepEvent.LAYER_DESTROYED) {
                    lastEvent = IdleBounceStepEvent.BALL_HIT
                }
            } else {
                val overflow = (-remainingHp).coerceAtLeast(0L)
                val destroyedLayer = nextState.currentLayer
                val nextDepth = nextState.depth + 1
                val nextLayer = generateLayer(nextDepth, nextState.prestigeMultiplier)
                totalCoinsEarned += destroyedLayer.reward
                pendingCarryDamage = if (drillTipEnabled) overflow else 0L
                nextState = nextState.copy(
                    coins = nextState.coins + destroyedLayer.reward,
                    depth = nextDepth,
                    currentLayer = nextLayer,
                    carryOverDamage = pendingCarryDamage,
                    lastHitCritical = criticalHit,
                    totalLayersDestroyed = nextState.totalLayersDestroyed + 1,
                    bedrockReached = nextState.bedrockReached || nextLayer.type == LayerType.BEDROCK
                )
                lastEvent = IdleBounceStepEvent.LAYER_DESTROYED
            }
        }

        return IdleBounceStep(nextState, lastEvent, totalCoinsEarned)
    }

    fun upgradeCost(state: IdleBounceGameState, upgradeId: String): Long {
        val upgrade = state.upgrades.firstOrNull { it.id == upgradeId } ?: return Long.MAX_VALUE
        if (upgrade.maxLevel >= 0 && upgrade.level >= upgrade.maxLevel) {
            return Long.MAX_VALUE
        }
        val baseCost = baseUpgradeCosts[upgradeId] ?: return Long.MAX_VALUE
        val cost = baseCost.toDouble() * 1.5.pow(upgrade.level.toDouble())
        return if (cost >= Long.MAX_VALUE.toDouble()) Long.MAX_VALUE else cost.roundToLong()
    }

    fun buyUpgrade(state: IdleBounceGameState, upgradeId: String): IdleBounceGameState? {
        val upgradeIndex = state.upgrades.indexOfFirst { it.id == upgradeId }
        if (upgradeIndex == -1) return null

        val upgrade = state.upgrades[upgradeIndex]
        val cost = upgradeCost(state, upgradeId)
        if (cost == Long.MAX_VALUE || state.coins < cost) return null
        if (upgrade.maxLevel >= 0 && upgrade.level >= upgrade.maxLevel) return null

        val updatedUpgrades = state.upgrades.toMutableList().apply {
            this[upgradeIndex] = upgrade.copy(level = upgrade.level + 1)
        }

        return state.copy(
            coins = state.coins - cost,
            upgrades = updatedUpgrades,
            ball = recalcBall(updatedUpgrades)
        )
    }

    fun canPrestige(state: IdleBounceGameState): Boolean = state.bedrockReached

    fun prestige(state: IdleBounceGameState): IdleBounceGameState? {
        if (!canPrestige(state)) return null
        val newMultiplier = state.prestigeMultiplier * 1.1f
        val upgrades = state.upgrades
        return IdleBounceGameState.initial(
            prestigeMultiplier = newMultiplier,
            prestigeCount = state.prestigeCount + 1
        ).copy(
            currentLayer = generateLayer(depth = 0, prestigeMultiplier = newMultiplier),
            upgrades = upgrades,
            ball = recalcBall(upgrades)
        )
    }

    internal fun generateLayer(depth: Int, prestigeMultiplier: Float): Layer {
        val scale = (1.0 + depth * 0.05) * prestigeMultiplier.toDouble()
        val (type, baseHp, baseReward) = when (depth) {
            in 0..4 -> Triple(LayerType.DIRT, randomIn(10L, 50L), randomIn(1L, 5L))
            in 5..14 -> Triple(LayerType.GRAVEL, randomIn(50L, 150L), randomIn(5L, 20L))
            in 15..29 -> Triple(LayerType.STONE, randomIn(150L, 500L), randomIn(20L, 80L))
            in 30..49 -> {
                if (random.nextFloat() < 0.25f) {
                    Triple(LayerType.ORE_VEIN, randomIn(300L, 800L), randomIn(80L, 250L) * 2L)
                } else {
                    Triple(LayerType.STONE, randomIn(150L, 500L), randomIn(20L, 80L))
                }
            }
            in 50..79 -> Triple(LayerType.DEEP_ROCK, randomIn(800L, 2000L), randomIn(250L, 700L))
            else -> Triple(LayerType.BEDROCK, randomIn(2000L, 5000L), randomIn(700L, 2000L))
        }

        val scaledHp = (baseHp * scale).roundToLong().coerceAtLeast(1L)
        val scaledReward = (baseReward * scale).roundToLong().coerceAtLeast(1L)
        return Layer(type = type, maxHp = scaledHp, hp = scaledHp, reward = scaledReward)
    }

    internal fun recalcBall(upgrades: List<BounceUpgrade>): Ball {
        val powerLevel = upgradeLevel(upgrades, "bounce-power")
        val speedLevel = upgradeLevel(upgrades, "bounce-speed")
        return Ball(
            power = (powerLevel + 1).toLong(),
            hitsPerSecond = 1f + speedLevel * 0.5f
        )
    }

    private fun upgradeLevel(upgrades: List<BounceUpgrade>, upgradeId: String): Int =
        upgrades.firstOrNull { it.id == upgradeId }?.level ?: 0

    private fun randomIn(from: Long, until: Long): Long =
        if (from >= until) from else from + random.nextLong(until - from + 1)

    private companion object {
        val baseUpgradeCosts = mapOf(
            "bounce-power" to 10L,
            "bounce-speed" to 25L,
            "ricochet" to 500L,
            "lucky-strike" to 1_000L,
            "drill-tip" to 5_000L
        )
    }
}
