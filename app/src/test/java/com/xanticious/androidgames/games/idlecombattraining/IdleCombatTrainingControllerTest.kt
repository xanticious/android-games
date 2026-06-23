package com.xanticious.androidgames.games.idlecombattraining

import com.xanticious.androidgames.controller.games.idlecombattraining.IdleCombatTrainingController
import com.xanticious.androidgames.model.games.idlecombattraining.IdleCombatState
import com.xanticious.androidgames.model.games.idlecombattraining.LastMoveResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdleCombatTrainingControllerTest {
    private val controller = IdleCombatTrainingController(Random(seed = 42))

    @Test
    fun step_shortDt_doesNotFireMove() {
        val step = controller.step(IdleCombatState.initial(), 0.25f)
        assertEquals(com.xanticious.androidgames.model.games.idlecombattraining.CombatStepEvent.NONE, step.event)
    }

    @Test
    fun step_fullInterval_firesMoveAttempt() {
        val step = controller.step(IdleCombatState.initial(), 1f)
        assertNotEquals(com.xanticious.androidgames.model.games.idlecombattraining.CombatStepEvent.NONE, step.event)
    }

    @Test
    fun step_whenHitLands_decreasesDummyHp() {
        val guaranteedHit = IdleCombatState.initial().copy(
            moves = IdleCombatState.initial().moves.mapIndexed { index, move ->
                if (index == 0) move.copy(baseHitChance = 1f, unlocked = true) else move.copy(unlocked = false)
            }
        )
        val step = controller.step(guaranteedHit, 1f)
        assertTrue(step.state.dummy.hp < guaranteedHit.dummy.hp)
    }

    @Test
    fun step_whenDummyHpReachesZero_returnsDummyDefeated() {
        val state = IdleCombatState.initial().copy(
            dummy = IdleCombatState.initial().dummy.copy(hp = 1L, maxHp = 1L),
            moves = IdleCombatState.initial().moves.mapIndexed { index, move ->
                if (index == 0) move.copy(baseHitChance = 1f, unlocked = true) else move.copy(unlocked = false)
            }
        )
        val step = controller.step(state, 1f)
        assertEquals(com.xanticious.androidgames.model.games.idlecombattraining.CombatStepEvent.DUMMY_DEFEATED, step.event)
    }

    @Test
    fun step_dummyDefeated_incrementsDummiesDefeated() {
        val state = IdleCombatState.initial().copy(
            dummy = IdleCombatState.initial().dummy.copy(hp = 1L, maxHp = 1L),
            moves = IdleCombatState.initial().moves.mapIndexed { index, move ->
                if (index == 0) move.copy(baseHitChance = 1f, unlocked = true) else move.copy(unlocked = false)
            }
        )
        val step = controller.step(state, 1f)
        assertEquals(1, step.state.dummiesDefeated)
    }

    @Test
    fun step_dummyDefeated_awardsCoins() {
        val state = IdleCombatState.initial().copy(
            dummy = IdleCombatState.initial().dummy.copy(hp = 1L, maxHp = 1L, reward = 25L),
            moves = IdleCombatState.initial().moves.mapIndexed { index, move ->
                if (index == 0) move.copy(baseHitChance = 1f, unlocked = true) else move.copy(unlocked = false)
            }
        )
        val step = controller.step(state, 1f)
        assertEquals(25L, step.coinsEarned)
    }

    @Test
    fun step_miss_resetsHitStreak() {
        val state = IdleCombatState.initial().copy(
            hitStreak = 3,
            moves = IdleCombatState.initial().moves.mapIndexed { index, move ->
                if (index == 0) move.copy(baseHitChance = 0f, unlocked = true) else move.copy(unlocked = false)
            }
        )
        val step = controller.step(state, 1f)
        assertEquals(0, step.state.hitStreak)
    }

    @Test
    fun buyUpgrade_whenAffordable_purchasesUpgrade() {
        val upgraded = controller.buyUpgrade(IdleCombatState.initial().copy(coins = 50L), "basic-footwork")
        assertNotNull(upgraded)
    }

    @Test
    fun buyUpgrade_whenNotAffordable_returnsNull() {
        assertNull(controller.buyUpgrade(IdleCombatState.initial(), "basic-footwork"))
    }

    @Test
    fun buyUpgrade_whenPrereqNotMet_returnsNull() {
        assertNull(controller.buyUpgrade(IdleCombatState.initial().copy(coins = 200L), "stance-training"))
    }

    @Test
    fun buyUpgrade_whenAlreadyPurchased_returnsNull() {
        val state = IdleCombatState.initial().copy(
            coins = 999L,
            upgrades = IdleCombatState.initial().upgrades.map {
                if (it.id == "basic-footwork") it.copy(purchased = true) else it
            }
        )
        assertNull(controller.buyUpgrade(state, "basic-footwork"))
    }

    @Test
    fun buyUpgrade_basicFootwork_increasesHitChance() {
        val upgraded = controller.buyUpgrade(IdleCombatState.initial().copy(coins = 50L), "basic-footwork")
        assertEquals(0.10f, controller.calcHitChanceBonus(upgraded?.upgrades.orEmpty()))
    }

    @Test
    fun buyUpgrade_speedDrills_decreasesMoveInterval() {
        val state = IdleCombatState.initial().copy(
            coins = 500L,
            upgrades = IdleCombatState.initial().upgrades.map {
                when (it.id) {
                    "basic-footwork", "stance-training" -> it.copy(purchased = true)
                    else -> it
                }
            }
        )
        val upgraded = controller.buyUpgrade(state, "speed-drills")
        assertTrue((upgraded?.moveInterval ?: 1f) < 1f)
    }

    @Test
    fun generateDummy_number1_hasCorrectHp() {
        assertEquals(20L, controller.generateDummy(1).hp)
    }

    @Test
    fun generateDummy_number31plus_scalesHp() {
        assertTrue(controller.generateDummy(31).hp > 800L)
    }

    @Test
    fun updateUnlockedMoves_after5Dummies_frontKickUnlocked() {
        val moves = controller.updateUnlockedMoves(
            IdleCombatState.initial().moves,
            IdleCombatState.initial().upgrades,
            dummiesDefeated = 5,
            hitStreak = 0
        )
        assertTrue(moves.first { it.id == com.xanticious.androidgames.model.games.idlecombattraining.MoveId.FRONT_KICK }.unlocked)
    }

    @Test
    fun updateUnlockedMoves_hitStreak3_comboFinisherUnlocked() {
        val moves = controller.updateUnlockedMoves(
            IdleCombatState.initial().moves,
            IdleCombatState.initial().upgrades,
            dummiesDefeated = 0,
            hitStreak = 3
        )
        assertTrue(moves.first { it.id == com.xanticious.androidgames.model.games.idlecombattraining.MoveId.COMBO_FINISHER }.unlocked)
    }
}
