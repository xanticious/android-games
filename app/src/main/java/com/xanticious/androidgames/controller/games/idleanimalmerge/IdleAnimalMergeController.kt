package com.xanticious.androidgames.controller.games.idleanimalmerge

import com.xanticious.androidgames.model.games.idleanimalmerge.Animal
import com.xanticious.androidgames.model.games.idleanimalmerge.AnimalMergeGameState
import com.xanticious.androidgames.model.games.idleanimalmerge.AnimalType
import com.xanticious.androidgames.model.games.idleanimalmerge.MergeResult
import kotlin.random.Random

/**
 * Pure Animal Merge rules engine — zero Android/Compose imports.
 *
 * Responsibilities:
 *  - The catalog of 100 animal types across 10 tiers
 *  - Weighted spawn probability based on the player's current collection
 *  - Animal placement, merge, and release logic
 *  - Passive coin income per elapsed time
 *  - Daily extra-spawn purchase
 */
class IdleAnimalMergeController {

    // ─── Animal Catalog ───────────────────────────────────────────────────────

    val allAnimalTypes: List<AnimalType> = buildList {
        val tiers = listOf(
            listOf("mouse"        to ("Mouse"         to "🐭"),
                   "rabbit"       to ("Rabbit"        to "🐰"),
                   "sparrow"      to ("Sparrow"       to "🐦"),
                   "frog"         to ("Frog"          to "🐸"),
                   "beetle"       to ("Beetle"        to "🪲"),
                   "snail"        to ("Snail"         to "🐌"),
                   "goldfish"     to ("Goldfish"      to "🐠"),
                   "caterpillar"  to ("Caterpillar"   to "🐛"),
                   "cricket"      to ("Cricket"       to "🦗"),
                   "ladybug"      to ("Ladybug"       to "🐞")),
            listOf("squirrel"     to ("Squirrel"      to "🐿️"),
                   "hedgehog"     to ("Hedgehog"      to "🦔"),
                   "duck"         to ("Duck"          to "🦆"),
                   "crow"         to ("Crow"          to "🐦‍⬛"),
                   "chipmunk"     to ("Chipmunk"      to "🐹"),
                   "pigeon"       to ("Pigeon"        to "🕊️"),
                   "toad"         to ("Toad"          to "🐊"),
                   "otter"        to ("Otter"         to "🦦"),
                   "crab"         to ("Crab"          to "🦀"),
                   "parrot"       to ("Parrot"        to "🦜")),
            listOf("fox"          to ("Fox"           to "🦊"),
                   "raccoon"      to ("Raccoon"       to "🦝"),
                   "goose"        to ("Goose"         to "🪿"),
                   "deer"         to ("Deer"          to "🦌"),
                   "badger"       to ("Badger"        to "🦡"),
                   "heron"        to ("Heron"         to "🦢"),
                   "skunk"        to ("Skunk"         to "🦨"),
                   "seal"         to ("Seal"          to "🦭"),
                   "porcupine"    to ("Porcupine"     to "🦔"),
                   "jay"          to ("Jay"           to "🐦")),
            listOf("wolf"         to ("Wolf"          to "🐺"),
                   "boar"         to ("Boar"          to "🐗"),
                   "swan"         to ("Swan"          to "🦢"),
                   "lynx"         to ("Lynx"          to "🐱"),
                   "hawk"         to ("Hawk"          to "🦅"),
                   "peacock"      to ("Peacock"       to "🦚"),
                   "python"       to ("Python"        to "🐍"),
                   "iguana"       to ("Iguana"        to "🦎"),
                   "vulture"      to ("Vulture"       to "🦅"),
                   "crane"        to ("Crane"         to "🕊️")),
            listOf("bear"         to ("Bear"          to "🐻"),
                   "elk"          to ("Elk"           to "🫎"),
                   "eagle"        to ("Eagle"         to "🦅"),
                   "bison"        to ("Bison"         to "🦬"),
                   "leopard"      to ("Leopard"       to "🐆"),
                   "crocodile"    to ("Crocodile"     to "🐊"),
                   "gorilla"      to ("Gorilla"       to "🦍"),
                   "flamingo"     to ("Flamingo"      to "🦩"),
                   "komodo"       to ("Komodo"        to "🦎"),
                   "pelican"      to ("Pelican"       to "🐦")),
            listOf("lion"         to ("Lion"          to "🦁"),
                   "tiger"        to ("Tiger"         to "🐯"),
                   "cheetah"      to ("Cheetah"       to "🐆"),
                   "panda"        to ("Panda"         to "🐼"),
                   "polar-bear"   to ("Polar Bear"    to "🐻‍❄️"),
                   "hippo"        to ("Hippo"         to "🦛"),
                   "rhino"        to ("Rhino"         to "🦏"),
                   "moose"        to ("Moose"         to "🫎"),
                   "jaguar"       to ("Jaguar"        to "🐆"),
                   "condor"       to ("Condor"        to "🦅")),
            listOf("orca"         to ("Orca"          to "🐋"),
                   "elephant"     to ("Elephant"      to "🐘"),
                   "giraffe"      to ("Giraffe"       to "🦒"),
                   "narwhal"      to ("Narwhal"       to "🦄"),
                   "walrus"       to ("Walrus"        to "🦭"),
                   "giant-squid"  to ("Giant Squid"   to "🦑"),
                   "manta-ray"    to ("Manta Ray"     to "🐟"),
                   "albatross"    to ("Albatross"     to "🕊️"),
                   "pangolin"     to ("Pangolin"      to "🐾"),
                   "bald-eagle"   to ("Bald Eagle"    to "🦅")),
            listOf("mammoth"      to ("Mammoth"       to "🦣"),
                   "sabertooth"   to ("Sabertooth"    to "🐱"),
                   "dire-wolf"    to ("Dire Wolf"     to "🐺"),
                   "giant-sloth"  to ("Giant Sloth"   to "🦥"),
                   "megalodon"    to ("Megalodon"     to "🦈"),
                   "pterodactyl"  to ("Pterodactyl"   to "🦕"),
                   "t-rex"        to ("T-Rex"         to "🦖"),
                   "triceratops"  to ("Triceratops"   to "🦕"),
                   "brachiosaurus" to ("Brachiosaurus" to "🦕"),
                   "mosasaurus"   to ("Mosasaurus"    to "🐊")),
            listOf("chimera"      to ("Chimera"       to "🔥"),
                   "basilisk"     to ("Basilisk"      to "🐍"),
                   "kraken"       to ("Kraken"        to "🐙"),
                   "leviathan"    to ("Leviathan"     to "🌊"),
                   "thunderbird"  to ("Thunderbird"   to "⚡"),
                   "gryphon"      to ("Gryphon"       to "🦅"),
                   "wyvern"       to ("Wyvern"        to "🐉"),
                   "kitsune"      to ("Kitsune"       to "🦊"),
                   "fenrir"       to ("Fenrir"        to "🐺"),
                   "hydra"        to ("Hydra"         to "🐍")),
            listOf("dragon"       to ("Dragon"        to "🐉"),
                   "phoenix"      to ("Phoenix"       to "🔥"),
                   "unicorn"      to ("Unicorn"       to "🦄"),
                   "pegasus"      to ("Pegasus"       to "🐴"),
                   "celestial-whale" to ("Celestial Whale" to "🌊"),
                   "astral-tiger" to ("Astral Tiger"  to "⭐"),
                   "void-serpent" to ("Void Serpent"  to "🌑"),
                   "cosmic-eagle" to ("Cosmic Eagle"  to "🌟"),
                   "seraphim"     to ("Seraphim"      to "✨"),
                   "apex"         to ("Apex"          to "👑"))
        )
        tiers.forEachIndexed { tierIndex, animals ->
            animals.forEach { (id, nameEmoji) ->
                add(AnimalType(id = id, name = nameEmoji.first, tier = tierIndex + 1, emoji = nameEmoji.second))
            }
        }
    }

    private val animalsByTier: Map<Int, List<AnimalType>> =
        allAnimalTypes.groupBy { it.tier }

    private val animalById: Map<String, AnimalType> =
        allAnimalTypes.associateBy { it.id }

    // ─── Spawn Probability ────────────────────────────────────────────────────

    /**
     * Returns the weighted spawn distribution across available tiers.
     *
     * Let H = highest tier currently in the field.
     * - Tiers 1..H are always available once H ≥ 1.
     * - Tier H+1 unlocks when the player owns ≥ 2 animals of tier H.
     * - If the field is empty, only tier 1 is available.
     * - Raw weight for tier T = max(0, 100 − 15 × (T − 1)), normalized.
     */
    fun spawnWeights(state: AnimalMergeGameState): Map<Int, Double> {
        val highestTier = state.field.maxOfOrNull { it.type.tier } ?: 0
        val availableTiers = mutableSetOf<Int>()
        if (highestTier == 0) {
            availableTiers.add(1)
        } else {
            for (t in 1..highestTier) availableTiers.add(t)
            val tierHCount = state.field.count { it.type.tier == highestTier }
            if (tierHCount >= 2 && highestTier < 10) availableTiers.add(highestTier + 1)
        }
        val rawWeights = availableTiers.associateWith { t ->
            maxOf(0, 100 - 15 * (t - 1)).toDouble()
        }
        val totalWeight = rawWeights.values.sum()
        return if (totalWeight <= 0.0) mapOf(1 to 1.0)
        else rawWeights.mapValues { (_, w) -> w / totalWeight }
    }

    /**
     * Picks a random [AnimalType] to spawn, respecting the current field composition.
     * Picks a tier from the weighted distribution, then a random animal of that tier.
     */
    fun pickSpawnType(state: AnimalMergeGameState, random: Random = Random.Default): AnimalType {
        val weights = spawnWeights(state)
        val roll = random.nextDouble()
        var cumulative = 0.0
        val selectedTier = weights.entries.sortedBy { it.key }.firstOrNull { (_, w) ->
            cumulative += w
            roll < cumulative
        }?.key ?: weights.keys.first()
        val tierAnimals = animalsByTier[selectedTier] ?: animalsByTier[1]!!
        return tierAnimals[random.nextInt(tierAnimals.size)]
    }

    // ─── Placement ────────────────────────────────────────────────────────────

    /**
     * Attempts to place [type] into the field (or the queue if the field is full).
     * Returns the updated state; the arrival type is moved to [AnimalMergeGameState.pendingArrival]
     * if the field is full but the queue can still accept it, or discarded if both are full.
     */
    fun placeAnimal(state: AnimalMergeGameState, type: AnimalType): AnimalMergeGameState {
        val occupiedSlots = state.field.map { it.fieldSlot }.toSet()
        val freeSlot = (0 until AnimalMergeGameState.MAX_FIELD_SLOTS).firstOrNull { it !in occupiedSlots }
        val discovered = state.discoveredIds + type.id
        return if (freeSlot != null) {
            val animal = Animal(
                instanceId = state.nextInstanceId,
                type = type,
                fieldSlot = freeSlot
            )
            state.copy(
                field = state.field + animal,
                nextInstanceId = state.nextInstanceId + 1,
                discoveredIds = discovered,
                pendingArrival = null
            )
        } else if (state.queue.size < AnimalMergeGameState.MAX_QUEUE) {
            state.copy(
                queue = state.queue + type,
                discoveredIds = discovered,
                pendingArrival = null
            )
        } else {
            // Field AND queue both full — keep as pending arrival (caller handles FIELD_FULL phase)
            state.copy(
                discoveredIds = discovered,
                pendingArrival = type
            )
        }
    }

    // ─── Selection & Merging ──────────────────────────────────────────────────

    /**
     * Handles a tap on an animal tile.
     *
     * - Nothing selected → select this animal.
     * - Tapping already-selected animal → deselect.
     * - Tapping a different animal of the *same type* → merge.
     * - Tapping a different animal of a *different type* → change selection.
     *
     * Returns the updated state, plus a [MergeResult] if a merge occurred.
     */
    fun tapAnimal(
        state: AnimalMergeGameState,
        instanceId: Int,
        random: Random = Random.Default
    ): Pair<AnimalMergeGameState, MergeResult?> {
        val tapped = state.field.firstOrNull { it.instanceId == instanceId }
            ?: return state to null
        val selected = state.field.firstOrNull { it.instanceId == state.selectedInstanceId }
        return when {
            selected == null -> state.copy(selectedInstanceId = instanceId) to null
            selected.instanceId == instanceId -> state.copy(selectedInstanceId = null) to null
            selected.type.id == tapped.type.id -> {
                val result = performMerge(state, selected, tapped, random)
                result.first to result.second
            }
            else -> state.copy(selectedInstanceId = instanceId) to null
        }
    }

    private fun performMerge(
        state: AnimalMergeGameState,
        a: Animal,
        b: Animal,
        random: Random
    ): Pair<AnimalMergeGameState, MergeResult> {
        val nextTierAnimals = animalsByTier[a.type.tier + 1]
        val produced = if (nextTierAnimals != null) {
            nextTierAnimals.first()  // first animal of the next tier
        } else {
            a.type  // already tier 10, stays the same type
        }
        val coinsEarned = a.type.tier * 50L
        val newAnimal = Animal(
            instanceId = state.nextInstanceId,
            type = produced,
            fieldSlot = a.fieldSlot   // produced animal takes the first consumed animal's slot
        )
        val fieldWithoutConsumed = state.field.filter {
            it.instanceId != a.instanceId && it.instanceId != b.instanceId
        }
        val discovered = state.discoveredIds + produced.id
        val newCoins = state.coins + coinsEarned

        // After merge, attempt to pull from queue into the freed slot (b's slot)
        val (finalField, finalQueue) = if (state.queue.isNotEmpty()) {
            val fromQueue = state.queue.first()
            val queueAnimal = Animal(
                instanceId = state.nextInstanceId + 1,
                type = fromQueue,
                fieldSlot = b.fieldSlot
            )
            (fieldWithoutConsumed + newAnimal + queueAnimal) to state.queue.drop(1)
        } else {
            (fieldWithoutConsumed + newAnimal) to state.queue
        }
        val idIncrement = if (state.queue.isNotEmpty()) 2 else 1
        val mergeResult = MergeResult(
            consumed = a to b,
            produced = newAnimal,
            coinsEarned = coinsEarned
        )
        return state.copy(
            field = finalField,
            queue = finalQueue,
            coins = newCoins,
            totalCoinsEarned = state.totalCoinsEarned + coinsEarned,
            discoveredIds = discovered,
            selectedInstanceId = null,
            nextInstanceId = state.nextInstanceId + idIncrement
        ) to mergeResult
    }

    // ─── Release ──────────────────────────────────────────────────────────────

    /**
     * Releases an animal from the field, earning a small coin bonus.
     * If there is a queued animal, it fills the freed slot.
     */
    fun releaseAnimal(state: AnimalMergeGameState, instanceId: Int): AnimalMergeGameState {
        val animal = state.field.firstOrNull { it.instanceId == instanceId } ?: return state
        val releaseBonus = animal.type.tier * 5L
        val fieldWithout = state.field.filter { it.instanceId != instanceId }
        val (finalField, finalQueue) = if (state.queue.isNotEmpty()) {
            val fromQueue = state.queue.first()
            val queueAnimal = Animal(
                instanceId = state.nextInstanceId,
                type = fromQueue,
                fieldSlot = animal.fieldSlot
            )
            (fieldWithout + queueAnimal) to state.queue.drop(1)
        } else {
            fieldWithout to state.queue
        }
        val selectedAfter = if (state.selectedInstanceId == instanceId) null else state.selectedInstanceId
        val idIncrement = if (state.queue.isNotEmpty()) 1 else 0
        return state.copy(
            field = finalField,
            queue = finalQueue,
            coins = state.coins + releaseBonus,
            totalCoinsEarned = state.totalCoinsEarned + releaseBonus,
            selectedInstanceId = selectedAfter,
            nextInstanceId = state.nextInstanceId + idIncrement
        )
    }

    // ─── Timer & Passive Income ───────────────────────────────────────────────

    /**
     * Advances the game by [deltaSeconds] seconds:
     * 1. Decrements the spawn countdown, returning a new spawn type when it hits 0.
     * 2. Accumulates passive coin income (tier² × 0.5 coins/minute per animal).
     *
     * Returns the updated state and an optional [AnimalType] to be spawned.
     */
    fun tickSeconds(state: AnimalMergeGameState, deltaSeconds: Double): Pair<AnimalMergeGameState, AnimalType?> {
        // Passive coins
        val incomePerMinute = state.field.sumOf { it.type.tier.toLong() * it.type.tier * 0.5 }
        val incomePerSecond = incomePerMinute / 60.0
        val newAccumulator = state.coinAccumulatorSeconds + deltaSeconds * incomePerSecond
        val earnedCoins = newAccumulator.toLong()
        val remainingAccumulator = newAccumulator - earnedCoins

        // Spawn countdown
        val newTimer = state.secondsUntilNextSpawn - deltaSeconds.toLong()
        return if (newTimer <= 0) {
            state.copy(
                secondsUntilNextSpawn = AnimalMergeGameState.SPAWN_INTERVAL_SECONDS + newTimer,
                coins = state.coins + earnedCoins,
                totalCoinsEarned = state.totalCoinsEarned + earnedCoins,
                coinAccumulatorSeconds = remainingAccumulator
            ) to null  // caller calls pickSpawnType then placeAnimal
        } else {
            state.copy(
                secondsUntilNextSpawn = newTimer,
                coins = state.coins + earnedCoins,
                totalCoinsEarned = state.totalCoinsEarned + earnedCoins,
                coinAccumulatorSeconds = remainingAccumulator
            ) to null
        }
    }

    /**
     * Ticks the countdown and emits a spawn trigger if the timer expired.
     * Returns (updatedState, spawnType?) where spawnType is non-null when it's time to spawn.
     */
    fun tickAndSpawn(
        state: AnimalMergeGameState,
        deltaSeconds: Double,
        random: Random = Random.Default
    ): Pair<AnimalMergeGameState, AnimalType?> {
        val incomePerMinute = state.field.sumOf { it.type.tier.toLong() * it.type.tier * 0.5 }
        val incomePerSecond = incomePerMinute / 60.0
        val newAccumulator = state.coinAccumulatorSeconds + deltaSeconds * incomePerSecond
        val earnedCoins = newAccumulator.toLong()
        val remainingAccumulator = newAccumulator - earnedCoins

        val newTimer = state.secondsUntilNextSpawn - deltaSeconds.toLong()
        val spawnType: AnimalType?
        val finalTimer: Long
        if (newTimer <= 0) {
            spawnType = pickSpawnType(state, random)
            finalTimer = AnimalMergeGameState.SPAWN_INTERVAL_SECONDS + newTimer
        } else {
            spawnType = null
            finalTimer = newTimer
        }
        return state.copy(
            secondsUntilNextSpawn = finalTimer,
            coins = state.coins + earnedCoins,
            totalCoinsEarned = state.totalCoinsEarned + earnedCoins,
            coinAccumulatorSeconds = remainingAccumulator
        ) to spawnType
    }

    // ─── Daily Extra Spawn ────────────────────────────────────────────────────

    /** Cost for one extra spawn (once per day). */
    fun dailySpawnCost(state: AnimalMergeGameState): Long {
        val highestTier = state.field.maxOfOrNull { it.type.tier } ?: 1
        return 200L * highestTier
    }

    fun canBuyDailySpawn(state: AnimalMergeGameState, currentEpochDay: Int): Boolean =
        state.coins >= dailySpawnCost(state) && state.lastDailySpawnEpochDay != currentEpochDay

    fun buyDailySpawn(
        state: AnimalMergeGameState,
        currentEpochDay: Int,
        random: Random = Random.Default
    ): Pair<AnimalMergeGameState, AnimalType?> {
        if (!canBuyDailySpawn(state, currentEpochDay)) return state to null
        val cost = dailySpawnCost(state)
        val spawn = pickSpawnType(state, random)
        return state.copy(
            coins = state.coins - cost,
            lastDailySpawnEpochDay = currentEpochDay
        ) to spawn
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** True when both the field and the queue are completely full. */
    fun isAtCapacity(state: AnimalMergeGameState): Boolean =
        state.field.size >= AnimalMergeGameState.MAX_FIELD_SLOTS &&
        state.queue.size >= AnimalMergeGameState.MAX_QUEUE

    /** Number of free field slots (ignoring the queue). */
    fun freeFieldSlots(state: AnimalMergeGameState): Int =
        AnimalMergeGameState.MAX_FIELD_SLOTS - state.field.size

    /** Passive coins per minute for the current field. */
    fun passiveIncomePerMinute(state: AnimalMergeGameState): Double =
        state.field.sumOf { it.type.tier.toLong() * it.type.tier * 0.5 }
}
