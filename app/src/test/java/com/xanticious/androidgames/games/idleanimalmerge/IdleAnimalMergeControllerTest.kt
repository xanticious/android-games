package com.xanticious.androidgames.games.idleanimalmerge

import com.xanticious.androidgames.controller.games.idleanimalmerge.IdleAnimalMergeController
import com.xanticious.androidgames.model.games.idleanimalmerge.Animal
import com.xanticious.androidgames.model.games.idleanimalmerge.AnimalMergeGameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdleAnimalMergeControllerTest {

    private val controller = IdleAnimalMergeController()
    private val rng = Random(42)

    // ── Catalog ───────────────────────────────────────────────────────────────

    @Test
    fun catalog_has100Animals() {
        assertEquals(100, controller.allAnimalTypes.size)
    }

    @Test
    fun catalog_tenTiers_tenAnimalsEach() {
        for (tier in 1..10) {
            val count = controller.allAnimalTypes.count { it.tier == tier }
            assertEquals("Tier $tier should have 10 animals", 10, count)
        }
    }

    @Test
    fun catalog_allIdsAreUnique() {
        val ids = controller.allAnimalTypes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    // ── Spawn Weights ─────────────────────────────────────────────────────────

    @Test
    fun spawnWeights_emptyField_onlyTier1Available() {
        val state = AnimalMergeGameState.initial()
        val weights = controller.spawnWeights(state)
        assertEquals(setOf(1), weights.keys)
    }

    @Test
    fun spawnWeights_sumToOne_forEmptyField() {
        val state = AnimalMergeGameState.initial()
        val weights = controller.spawnWeights(state)
        assertEquals(1.0, weights.values.sum(), 1e-9)
    }

    @Test
    fun spawnWeights_tier2Unlocked_whenTwoTier1InField() {
        val tier1 = controller.allAnimalTypes.first { it.tier == 1 }
        val a = Animal(0, tier1, 0)
        val b = Animal(1, tier1, 1)
        val state = AnimalMergeGameState.initial().copy(field = listOf(a, b))
        val weights = controller.spawnWeights(state)
        assertTrue("Tier 2 should be available", 2 in weights.keys)
    }

    @Test
    fun spawnWeights_tier2NotUnlocked_withOneTier1() {
        val tier1 = controller.allAnimalTypes.first { it.tier == 1 }
        val a = Animal(0, tier1, 0)
        val state = AnimalMergeGameState.initial().copy(field = listOf(a))
        val weights = controller.spawnWeights(state)
        assertFalse("Tier 2 should not yet be available", 2 in weights.keys)
    }

    // ── Placement ─────────────────────────────────────────────────────────────

    @Test
    fun placeAnimal_emptyField_addsToField() {
        val state = AnimalMergeGameState.initial()
        val type = controller.allAnimalTypes.first()
        val result = controller.placeAnimal(state, type)
        assertEquals(1, result.field.size)
        assertNull(result.pendingArrival)
    }

    @Test
    fun placeAnimal_fullField_goesToQueue() {
        val tier1 = controller.allAnimalTypes.first { it.tier == 1 }
        val fullField = (0 until AnimalMergeGameState.MAX_FIELD_SLOTS).map { slot ->
            Animal(slot, tier1, slot)
        }
        val state = AnimalMergeGameState.initial().copy(field = fullField, nextInstanceId = fullField.size)
        val result = controller.placeAnimal(state, tier1)
        assertEquals(1, result.queue.size)
        assertEquals(AnimalMergeGameState.MAX_FIELD_SLOTS, result.field.size)
    }

    @Test
    fun placeAnimal_fullFieldAndQueue_setsPendingArrival() {
        val tier1 = controller.allAnimalTypes.first { it.tier == 1 }
        val fullField = (0 until AnimalMergeGameState.MAX_FIELD_SLOTS).map { slot ->
            Animal(slot, tier1, slot)
        }
        val fullQueue = List(AnimalMergeGameState.MAX_QUEUE) { tier1 }
        val state = AnimalMergeGameState.initial().copy(
            field = fullField,
            queue = fullQueue,
            nextInstanceId = fullField.size
        )
        val result = controller.placeAnimal(state, tier1)
        assertNotNull(result.pendingArrival)
    }

    @Test
    fun placeAnimal_addsToDiscoveredIds() {
        val state = AnimalMergeGameState.initial()
        val type = controller.allAnimalTypes.first()
        val result = controller.placeAnimal(state, type)
        assertTrue(type.id in result.discoveredIds)
    }

    // ── Tap / Select / Merge ──────────────────────────────────────────────────

    @Test
    fun tapAnimal_noSelection_selectsAnimal() {
        val tier1 = controller.allAnimalTypes.first { it.tier == 1 }
        val animal = Animal(0, tier1, 0)
        val state = AnimalMergeGameState.initial().copy(field = listOf(animal))
        val (newState, merge) = controller.tapAnimal(state, 0)
        assertEquals(0, newState.selectedInstanceId)
        assertNull(merge)
    }

    @Test
    fun tapAnimal_tapSelectedAnimal_deselects() {
        val tier1 = controller.allAnimalTypes.first { it.tier == 1 }
        val animal = Animal(0, tier1, 0)
        val state = AnimalMergeGameState.initial().copy(field = listOf(animal), selectedInstanceId = 0)
        val (newState, merge) = controller.tapAnimal(state, 0)
        assertNull(newState.selectedInstanceId)
        assertNull(merge)
    }

    @Test
    fun tapAnimal_matchingTypes_producesHigherTier() {
        val tier1Type = controller.allAnimalTypes.first { it.tier == 1 }
        val a = Animal(0, tier1Type, 0)
        val b = Animal(1, tier1Type, 1)
        val state = AnimalMergeGameState.initial().copy(
            field = listOf(a, b),
            selectedInstanceId = 0,
            nextInstanceId = 2
        )
        val (newState, merge) = controller.tapAnimal(state, 1)
        assertNotNull(merge)
        assertEquals(2, merge!!.produced.type.tier)
    }

    @Test
    fun tapAnimal_mergeEarnsMergeCoins() {
        val tier1Type = controller.allAnimalTypes.first { it.tier == 1 }
        val a = Animal(0, tier1Type, 0)
        val b = Animal(1, tier1Type, 1)
        val state = AnimalMergeGameState.initial().copy(
            field = listOf(a, b),
            selectedInstanceId = 0,
            nextInstanceId = 2
        )
        val (newState, merge) = controller.tapAnimal(state, 1)
        assertNotNull(merge)
        val expectedCoins = tier1Type.tier * 50L  // 50
        assertEquals(expectedCoins, merge!!.coinsEarned)
        assertEquals(expectedCoins, newState.coins)
    }

    @Test
    fun tapAnimal_differentTypes_changesSelection() {
        val typeA = controller.allAnimalTypes.first { it.tier == 1 }
        val typeB = controller.allAnimalTypes.first { it.tier == 1 && it.id != typeA.id }
        val a = Animal(0, typeA, 0)
        val b = Animal(1, typeB, 1)
        val state = AnimalMergeGameState.initial().copy(
            field = listOf(a, b),
            selectedInstanceId = 0
        )
        val (newState, merge) = controller.tapAnimal(state, 1)
        assertEquals(1, newState.selectedInstanceId)
        assertNull(merge)
    }

    // ── Release ────────────────────────────────────────────────────────────────

    @Test
    fun releaseAnimal_removesFromField() {
        val type = controller.allAnimalTypes.first { it.tier == 1 }
        val animal = Animal(0, type, 0)
        val state = AnimalMergeGameState.initial().copy(field = listOf(animal))
        val result = controller.releaseAnimal(state, 0)
        assertTrue(result.field.isEmpty())
    }

    @Test
    fun releaseAnimal_earnsTierBonus() {
        val type = controller.allAnimalTypes.first { it.tier == 3 }
        val animal = Animal(0, type, 0)
        val state = AnimalMergeGameState.initial().copy(field = listOf(animal))
        val result = controller.releaseAnimal(state, 0)
        assertEquals(3 * 5L, result.coins)  // tier * 5
    }

    @Test
    fun releaseAnimal_pullsFromQueue_intoFreedSlot() {
        val type = controller.allAnimalTypes.first { it.tier == 1 }
        val queueType = controller.allAnimalTypes.first { it.tier == 2 }
        val fullField = (0 until AnimalMergeGameState.MAX_FIELD_SLOTS).map { slot ->
            Animal(slot, type, slot)
        }
        val state = AnimalMergeGameState.initial().copy(
            field = fullField,
            queue = listOf(queueType),
            nextInstanceId = fullField.size
        )
        val result = controller.releaseAnimal(state, 0)
        assertEquals(AnimalMergeGameState.MAX_FIELD_SLOTS, result.field.size)
        assertTrue(result.queue.isEmpty())
        assertTrue(result.field.any { it.type.id == queueType.id })
    }

    // ── Tick & Passive Income ─────────────────────────────────────────────────

    @Test
    fun tickAndSpawn_doesNotSpawn_beforeTimerExpires() {
        val state = AnimalMergeGameState.initial()
        val (_, spawnType) = controller.tickAndSpawn(state, 1.0)
        assertNull(spawnType)
    }

    @Test
    fun tickAndSpawn_spawnsWhenTimerExpires() {
        val state = AnimalMergeGameState.initial().copy(secondsUntilNextSpawn = 1L)
        val (_, spawnType) = controller.tickAndSpawn(state, 1.0, rng)
        assertNotNull(spawnType)
    }

    @Test
    fun tickAndSpawn_passiveCoins_accumulateCorrectly() {
        val tier2 = controller.allAnimalTypes.first { it.tier == 2 }
        val animal = Animal(0, tier2, 0)
        // tier 2: 2^2 * 0.5 = 2 coins/min = 2/60 coins/s
        val state = AnimalMergeGameState.initial().copy(
            field = listOf(animal),
            secondsUntilNextSpawn = 9999L
        )
        // After 60 seconds: expect 2 coins (integer part of 2*60/60 = 2)
        val (updated, _) = controller.tickAndSpawn(state, 60.0)
        assertEquals(2L, updated.coins)
    }

    @Test
    fun tickAndSpawn_resetsTimerAfterSpawn() {
        val state = AnimalMergeGameState.initial().copy(secondsUntilNextSpawn = 1L)
        val (updated, _) = controller.tickAndSpawn(state, 1.0, rng)
        assertTrue(updated.secondsUntilNextSpawn > 0)
    }

    // ── Capacity ──────────────────────────────────────────────────────────────

    @Test
    fun isAtCapacity_false_forEmptyState() {
        assertFalse(controller.isAtCapacity(AnimalMergeGameState.initial()))
    }

    @Test
    fun isAtCapacity_true_whenFieldAndQueueFull() {
        val type = controller.allAnimalTypes.first()
        val fullField = (0 until AnimalMergeGameState.MAX_FIELD_SLOTS).map { slot ->
            Animal(slot, type, slot)
        }
        val fullQueue = List(AnimalMergeGameState.MAX_QUEUE) { type }
        val state = AnimalMergeGameState.initial().copy(field = fullField, queue = fullQueue)
        assertTrue(controller.isAtCapacity(state))
    }

    // ── Daily Spawn ───────────────────────────────────────────────────────────

    @Test
    fun canBuyDailySpawn_true_whenEnoughCoinsAndNewDay() {
        val state = AnimalMergeGameState.initial().copy(coins = 9_999L, lastDailySpawnEpochDay = 0)
        assertTrue(controller.canBuyDailySpawn(state, currentEpochDay = 1))
    }

    @Test
    fun canBuyDailySpawn_false_whenAlreadyUsedToday() {
        val state = AnimalMergeGameState.initial().copy(coins = 9_999L, lastDailySpawnEpochDay = 5)
        assertFalse(controller.canBuyDailySpawn(state, currentEpochDay = 5))
    }

    @Test
    fun buyDailySpawn_deductsCoins_andReturnsSpawnType() {
        val state = AnimalMergeGameState.initial().copy(coins = 9_999L, lastDailySpawnEpochDay = 0)
        val (updated, spawn) = controller.buyDailySpawn(state, currentEpochDay = 1, rng)
        assertNotNull(spawn)
        assertTrue(updated.coins < 9_999L)
        assertEquals(1, updated.lastDailySpawnEpochDay)
    }
}
