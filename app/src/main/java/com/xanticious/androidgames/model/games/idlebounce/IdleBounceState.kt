package com.xanticious.androidgames.model.games.idlebounce

enum class LayerType { DIRT, GRAVEL, STONE, ORE_VEIN, DEEP_ROCK, BEDROCK }

data class Layer(val type: LayerType, val maxHp: Long, val hp: Long, val reward: Long)

data class Ball(val power: Long, val hitsPerSecond: Float)

data class BounceUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val level: Int,
    val maxLevel: Int
)

data class IdleBounceGameState(
    val coins: Long,
    val depth: Int,
    val currentLayer: Layer,
    val ball: Ball,
    val upgrades: List<BounceUpgrade>,
    val prestigeMultiplier: Float,
    val prestigeCount: Int,
    val bounceTimer: Float,
    val bedrockReached: Boolean,
    val carryOverDamage: Long,
    val lastHitCritical: Boolean,
    val totalLayersDestroyed: Int
) {
    val hitInterval: Float get() = 1f / ball.hitsPerSecond.coerceAtLeast(0.1f)

    companion object {
        val INITIAL_UPGRADES = listOf(
            BounceUpgrade("bounce-power", "Bounce Power", "+1 damage per hit", level = 0, maxLevel = 50),
            BounceUpgrade("bounce-speed", "Bounce Speed", "+0.5 hits/second", level = 0, maxLevel = 30),
            BounceUpgrade("ricochet", "Ricochet", "Ball hits twice per bounce", level = 0, maxLevel = 1),
            BounceUpgrade("lucky-strike", "Lucky Strike", "5% chance of 3× damage", level = 0, maxLevel = 1),
            BounceUpgrade("drill-tip", "Drill Tip", "Excess damage carries to next layer", level = 0, maxLevel = 1)
        )

        fun initial(prestigeMultiplier: Float = 1f, prestigeCount: Int = 0): IdleBounceGameState =
            IdleBounceGameState(
                coins = 0L,
                depth = 0,
                currentLayer = Layer(LayerType.DIRT, maxHp = 20L, hp = 20L, reward = 2L),
                ball = Ball(power = 1L, hitsPerSecond = 1f),
                upgrades = INITIAL_UPGRADES,
                prestigeMultiplier = prestigeMultiplier,
                prestigeCount = prestigeCount,
                bounceTimer = 0f,
                bedrockReached = false,
                carryOverDamage = 0L,
                lastHitCritical = false,
                totalLayersDestroyed = 0
            )
    }
}

enum class IdleBounceStepEvent { NONE, BALL_HIT, LAYER_DESTROYED }

data class IdleBounceStep(
    val state: IdleBounceGameState,
    val event: IdleBounceStepEvent,
    val coinsEarned: Long
)
