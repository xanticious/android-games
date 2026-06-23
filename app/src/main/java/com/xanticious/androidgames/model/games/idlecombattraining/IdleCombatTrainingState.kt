package com.xanticious.androidgames.model.games.idlecombattraining

enum class MoveId {
    CLUMSY_JAB, FRONT_KICK, PALM_STRIKE, SPINNING_KICK, ELBOW_DROP, COMBO_FINISHER
}

data class Move(
    val id: MoveId,
    val name: String,
    val baseHitChance: Float,
    val baseDamage: Int,
    val unlocked: Boolean
)

data class Dummy(val number: Int, val maxHp: Long, val hp: Long, val reward: Long)

data class CombatUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val cost: Long,
    val requires: List<String>,
    val purchased: Boolean
)

enum class LastMoveResult { NONE, HIT, MISS, CRITICAL_HIT }

data class IdleCombatState(
    val coins: Long,
    val dummy: Dummy,
    val dummiesDefeated: Int,
    val moves: List<Move>,
    val upgrades: List<CombatUpgrade>,
    val currentMoveIndex: Int,
    val hitStreak: Int,
    val moveTimer: Float,
    val moveInterval: Float,
    val lastMoveResult: LastMoveResult,
    val totalDamageDealt: Long
) {
    companion object {
        val INITIAL_MOVES = listOf(
            Move(MoveId.CLUMSY_JAB, "Clumsy Jab", 0.30f, 2, true),
            Move(MoveId.FRONT_KICK, "Front Kick", 0.45f, 4, false),
            Move(MoveId.PALM_STRIKE, "Palm Strike", 0.55f, 5, false),
            Move(MoveId.SPINNING_KICK, "Spinning Kick", 0.40f, 8, false),
            Move(MoveId.ELBOW_DROP, "Elbow Drop", 0.60f, 7, false),
            Move(MoveId.COMBO_FINISHER, "Combo Finisher", 0.70f, 15, false)
        )

        val INITIAL_UPGRADES = listOf(
            CombatUpgrade("basic-footwork", "Basic Footwork", "+10% hit chance", 50L, emptyList(), false),
            CombatUpgrade("stance-training", "Stance Training", "+5% hit chance, +1 dmg", 150L, listOf("basic-footwork"), false),
            CombatUpgrade("strength-conditioning", "Strength Conditioning", "+3 damage", 100L, emptyList(), false),
            CombatUpgrade("speed-drills", "Speed Drills", "+0.5 hits/sec", 200L, listOf("stance-training"), false),
            CombatUpgrade("focus-technique", "Focus Technique", "Unlocks Combo Finisher", 500L, listOf("speed-drills"), false),
            CombatUpgrade("iron-fist", "Iron Fist", "Critical hits deal 2× damage", 400L, listOf("strength-conditioning"), false),
            CombatUpgrade("advanced-kata", "Advanced Kata", "Unlocks Spinning Kick", 300L, listOf("basic-footwork"), false),
            CombatUpgrade("masters-discipline", "Master's Discipline", "All hit chances +15%", 2000L, listOf("focus-technique", "iron-fist"), false)
        )

        fun initial(): IdleCombatState = IdleCombatState(
            coins = 0L,
            dummy = Dummy(1, 20L, 20L, 10L),
            dummiesDefeated = 0,
            moves = INITIAL_MOVES,
            upgrades = INITIAL_UPGRADES,
            currentMoveIndex = 0,
            hitStreak = 0,
            moveTimer = 0f,
            moveInterval = 1f,
            lastMoveResult = LastMoveResult.NONE,
            totalDamageDealt = 0L
        )
    }
}

enum class CombatStepEvent { NONE, MOVE_ATTEMPTED, HIT_LANDED, MISS_FIRED, DUMMY_DEFEATED }

data class CombatStep(
    val state: IdleCombatState,
    val event: CombatStepEvent,
    val coinsEarned: Long
)
