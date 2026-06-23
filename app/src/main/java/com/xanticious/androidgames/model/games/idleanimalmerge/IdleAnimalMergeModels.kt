package com.xanticious.androidgames.model.games.idleanimalmerge

/** One of the 100 distinct animal species, each belonging to one of 10 tiers. */
data class AnimalType(
    val id: String,
    val name: String,
    val tier: Int,
    val emoji: String
)

/** A live animal occupying a slot in the player's field. */
data class Animal(
    val instanceId: Int,
    val type: AnimalType,
    /** Index 0..19 in the field grid; -1 means the animal is in the arrival queue. */
    val fieldSlot: Int
)

data class MergeResult(
    val consumed: Pair<Animal, Animal>,
    val produced: Animal,
    val coinsEarned: Long
)

/**
 * Full snapshot of an Animal Merge session.
 *
 * [field] holds up to [MAX_FIELD_SLOTS] animals (one per grid cell).
 * [queue] holds up to [MAX_QUEUE] animals that arrived while the field was full.
 * [discoveredIds] tracks every type ever seen (bestiary).
 * [pendingArrival] is the latest spawned animal waiting to be placed into the field.
 * [secondsUntilNextSpawn] counts down real seconds from [SPAWN_INTERVAL_SECONDS].
 * [lastDailySpawnEpochDay] is the Julian day on which the paid extra spawn was last used.
 * [selectedInstanceId] is the animal the player tapped first in a merge attempt.
 */
data class AnimalMergeGameState(
    val field: List<Animal>,
    val queue: List<AnimalType>,
    val coins: Long,
    val totalCoinsEarned: Long,
    val discoveredIds: Set<String>,
    val pendingArrival: AnimalType?,
    val nextInstanceId: Int,
    val secondsUntilNextSpawn: Long,
    val lastDailySpawnEpochDay: Int,
    val selectedInstanceId: Int?,
    /** Accumulated fractional seconds used for smooth passive-coin ticking. */
    val coinAccumulatorSeconds: Double
) {
    companion object {
        const val MAX_FIELD_SLOTS = 20
        const val MAX_QUEUE = 3
        const val SPAWN_INTERVAL_SECONDS = 3600L

        fun initial(): AnimalMergeGameState = AnimalMergeGameState(
            field = emptyList(),
            queue = emptyList(),
            coins = 0L,
            totalCoinsEarned = 0L,
            discoveredIds = emptySet(),
            pendingArrival = null,
            nextInstanceId = 0,
            secondsUntilNextSpawn = SPAWN_INTERVAL_SECONDS,
            lastDailySpawnEpochDay = -1,
            selectedInstanceId = null,
            coinAccumulatorSeconds = 0.0
        )
    }
}
