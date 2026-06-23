package com.xanticious.androidgames.controller.games.idleplayerpiano

import com.xanticious.androidgames.model.games.idleplayerpiano.PianoGameState
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoNote
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoTickResult
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoUpgrade
import com.xanticious.androidgames.model.games.idleplayerpiano.TargetSequence
import kotlin.random.Random

/**
 * Pure Player Piano rules engine — zero Android/Compose imports.
 *
 * Responsibilities:
 *  - The upgrade catalog with prerequisites and stat bonuses
 *  - Bias-weighted note selection (with optional Mechanical Memory no-repeat rule)
 *  - Pedal Mechanism 5 % auto-advance logic
 *  - Sequence-match detection and coin rewards
 *  - Sequence-length milestone unlocking
 *  - Per-frame tick (advance accumulator, play notes if threshold crossed)
 *  - Upgrade purchases with prerequisite and cost validation
 */
class IdlePlayerPianoController {

    // ─── Upgrade Catalog ──────────────────────────────────────────────────────

    fun allUpgrades(): List<PianoUpgrade> = listOf(
        PianoUpgrade(
            id = "tuned-strings",
            name = "Tuned Strings",
            description = "+5 % correct-note bias",
            cost = 50L,
            requires = emptyList(),
            biasBonus = 0.05f
        ),
        PianoUpgrade(
            id = "better-hammers",
            name = "Better Hammers",
            description = "+5 % bias, notes 10 % faster",
            cost = 200L,
            requires = listOf("tuned-strings"),
            biasBonus = 0.05f,
            speedMultiplier = 1.10f
        ),
        PianoUpgrade(
            id = "music-roll-library",
            name = "Music Roll Library",
            description = "+10 % bias",
            cost = 500L,
            requires = listOf("tuned-strings"),
            biasBonus = 0.10f
        ),
        PianoUpgrade(
            id = "mechanical-memory",
            name = "Mechanical Memory",
            description = "Piano never immediately repeats a note",
            cost = 1_000L,
            requires = listOf("better-hammers"),
            mechanicalMemory = true
        ),
        PianoUpgrade(
            id = "harmonic-resonance",
            name = "Harmonic Resonance",
            description = "+10 % bias, notes 10 % faster",
            cost = 3_000L,
            requires = listOf("music-roll-library"),
            biasBonus = 0.10f,
            speedMultiplier = 1.10f
        ),
        PianoUpgrade(
            id = "pedal-mechanism",
            name = "Pedal Mechanism",
            description = "5 % chance to auto-advance sequence position",
            cost = 8_000L,
            requires = listOf("mechanical-memory"),
            pedalMechanism = true
        ),
        PianoUpgrade(
            id = "refined-roll",
            name = "Refined Roll",
            description = "+15 % bias",
            cost = 20_000L,
            requires = listOf("harmonic-resonance"),
            biasBonus = 0.15f
        ),
        PianoUpgrade(
            id = "maestro-calibration",
            name = "Maestro Calibration",
            description = "+20 % bias, notes 20 % faster",
            cost = 100_000L,
            requires = listOf("refined-roll", "pedal-mechanism"),
            biasBonus = 0.20f,
            speedMultiplier = 1.20f
        )
    )

    // ─── Initial State ────────────────────────────────────────────────────────

    fun initialState(random: Random = Random.Default): PianoGameState {
        val upgrades = allUpgrades()
        val target = generateTarget(2, random)
        return PianoGameState(
            coins = 0L,
            bias = 0f,
            tickIntervalSeconds = PianoGameState.BASE_TICK_INTERVAL,
            matchesCompleted = 0,
            sequenceLength = 2,
            target = target,
            progressIndex = 0,
            recentNotes = emptyList(),
            lastNote = null,
            upgrades = upgrades,
            tickAccumulator = 0f,
            totalNotesPlayed = 0L,
            celebrating = false,
            hasMechanicalMemory = false,
            hasPedalMechanism = false
        )
    }

    // ─── Target Sequence ──────────────────────────────────────────────────────

    /** Generates a random sequence of [length] notes, avoiding consecutive identical notes. */
    fun generateTarget(length: Int, random: Random = Random.Default): TargetSequence {
        val notes = mutableListOf<PianoNote>()
        var last = -1
        repeat(length) {
            var pitch: Int
            do { pitch = random.nextInt(PianoGameState.NOTE_COUNT) } while (pitch == last)
            notes.add(PianoNote(pitch))
            last = pitch
        }
        return TargetSequence(notes)
    }

    // ─── Sequence Length Milestones ───────────────────────────────────────────

    /**
     * Returns the sequence length corresponding to [matchesCompleted].
     * Length increases at the thresholds specified in the design doc.
     */
    fun sequenceLengthFor(matchesCompleted: Int): Int = when {
        matchesCompleted < 20 -> 2
        matchesCompleted < 75 -> 3
        matchesCompleted < 200 -> 4
        matchesCompleted < 500 -> 5
        matchesCompleted < 1_200 -> 6
        matchesCompleted < 3_000 -> 7
        else -> 8
    }

    fun coinsForMatch(sequenceLength: Int): Long = when (sequenceLength) {
        2 -> 10L
        3 -> 30L
        4 -> 100L
        5 -> 300L
        6 -> 800L
        7 -> 2_000L
        else -> 5_000L
    }

    // ─── Note Selection ───────────────────────────────────────────────────────

    /**
     * Picks the next note the piano should play.
     *
     * If [hasMechanicalMemory] the piano never immediately repeats [lastNote].
     * Bias math:
     *   P(correct) = 1/N + bias × (1 − 1/N)
     *   P(other)   = (1 − P(correct)) / (N − 1)
     * where N = number of eligible notes (8, or 7 with Mechanical Memory if the
     * next correct note happens to equal the last note — in that case the bias is
     * temporarily ignored for that specific note to avoid dead-lock, and we pick
     * the second note in the target instead, or fall back to uniform).
     */
    fun pickNote(
        state: PianoGameState,
        nextTargetNote: PianoNote,
        random: Random = Random.Default
    ): PianoNote {
        val n = PianoGameState.NOTE_COUNT
        val eligiblePitches = (0 until n).filter { pitch ->
            !state.hasMechanicalMemory || state.lastNote == null || pitch != state.lastNote.pitch
        }
        if (eligiblePitches.isEmpty()) return nextTargetNote  // safety fallback

        val correctIsEligible = nextTargetNote.pitch in eligiblePitches
        val pCorrect = if (correctIsEligible) {
            val e = eligiblePitches.size.toFloat()
            1f / e + state.bias * (1f - 1f / e)
        } else 0f

        val roll = random.nextFloat()
        if (correctIsEligible && roll < pCorrect) return nextTargetNote

        // Pick from incorrect eligible pitches
        val others = eligiblePitches.filter { it != nextTargetNote.pitch }
        return if (others.isEmpty()) PianoNote(eligiblePitches.random(random))
        else PianoNote(others.random(random))
    }

    // ─── Per-Frame Tick ───────────────────────────────────────────────────────

    /**
     * Advances the piano simulation by [dtSeconds].
     *
     * Returns the updated state and a [PianoTickResult] describing what happened.
     * May return [PianoTickResult.Nothing] if the accumulator hasn't filled yet.
     */
    fun tick(
        state: PianoGameState,
        dtSeconds: Float,
        random: Random = Random.Default
    ): Pair<PianoGameState, PianoTickResult> {
        val newAccumulator = state.tickAccumulator + dtSeconds
        if (newAccumulator < state.tickIntervalSeconds) {
            return state.copy(tickAccumulator = newAccumulator) to PianoTickResult.Nothing
        }

        val overflow = newAccumulator - state.tickIntervalSeconds
        val nextTargetNote = state.target.notes[state.progressIndex]

        // Pedal Mechanism: 5 % chance to auto-advance without playing a note
        if (state.hasPedalMechanism && random.nextFloat() < 0.05f) {
            val newProgress = state.progressIndex + 1
            if (newProgress >= state.target.notes.size) {
                return handleSequenceMatch(state, overflow, random)
            }
            return state.copy(
                progressIndex = newProgress,
                tickAccumulator = overflow
            ) to PianoTickResult.NotePlayed(nextTargetNote, progressAdvanced = true)
        }

        val note = pickNote(state, nextTargetNote, random)
        val progressAdvanced = note.pitch == nextTargetNote.pitch
        val newProgress = if (progressAdvanced) state.progressIndex + 1 else 0

        val updatedRecent = (state.recentNotes + note).takeLast(PianoGameState.TICKER_SIZE)

        if (progressAdvanced && newProgress >= state.target.notes.size) {
            return handleSequenceMatch(
                state.copy(recentNotes = updatedRecent, lastNote = note, tickAccumulator = overflow),
                overflow,
                random
            )
        }

        return state.copy(
            progressIndex = newProgress,
            recentNotes = updatedRecent,
            lastNote = note,
            tickAccumulator = overflow,
            totalNotesPlayed = state.totalNotesPlayed + 1
        ) to PianoTickResult.NotePlayed(note, progressAdvanced)
    }

    private fun handleSequenceMatch(
        state: PianoGameState,
        overflow: Float,
        random: Random
    ): Pair<PianoGameState, PianoTickResult> {
        val newMatchesCompleted = state.matchesCompleted + 1
        val coinsEarned = coinsForMatch(state.sequenceLength)
        val newSequenceLength = sequenceLengthFor(newMatchesCompleted)
        val newTarget = generateTarget(newSequenceLength, random)

        val result = PianoTickResult.SequenceMatched(
            coinsEarned = coinsEarned,
            newTarget = newTarget,
            newSequenceLength = newSequenceLength,
            matchesCompleted = newMatchesCompleted
        )
        return state.copy(
            coins = state.coins + coinsEarned,
            matchesCompleted = newMatchesCompleted,
            sequenceLength = newSequenceLength,
            target = newTarget,
            progressIndex = 0,
            tickAccumulator = overflow,
            totalNotesPlayed = state.totalNotesPlayed + 1,
            celebrating = true
        ) to result
    }

    // ─── Upgrade Purchases ────────────────────────────────────────────────────

    /** Returns the list of upgrade IDs whose prerequisites are all purchased. */
    fun availableUpgradeIds(upgrades: List<PianoUpgrade>): Set<String> {
        val purchasedIds = upgrades.filter { it.purchased }.map { it.id }.toSet()
        return upgrades
            .filter { !it.purchased && it.requires.all { req -> req in purchasedIds } }
            .map { it.id }
            .toSet()
    }

    /**
     * Attempts to purchase the upgrade identified by [upgradeId].
     * Returns updated state; unchanged if insufficient funds or prerequisites unmet.
     */
    fun purchaseUpgrade(state: PianoGameState, upgradeId: String): PianoGameState {
        val upgrade = state.upgrades.firstOrNull { it.id == upgradeId } ?: return state
        if (upgrade.purchased) return state
        if (state.coins < upgrade.cost) return state
        val available = availableUpgradeIds(state.upgrades)
        if (upgradeId !in available) return state

        val newUpgrades = state.upgrades.map {
            if (it.id == upgradeId) it.copy(purchased = true) else it
        }
        val newBias = (newUpgrades.filter { it.purchased }.sumOf { it.biasBonus.toDouble() }.toFloat())
            .coerceAtMost(PianoGameState.MAX_BIAS)
        val baseInterval = PianoGameState.BASE_TICK_INTERVAL
        val speedMult = newUpgrades.filter { it.purchased }.fold(1f) { acc, u -> acc * u.speedMultiplier }
        val newInterval = (baseInterval / speedMult).coerceAtLeast(0.1f)
        val newMechanicalMemory = newUpgrades.any { it.purchased && it.mechanicalMemory }
        val newPedalMechanism = newUpgrades.any { it.purchased && it.pedalMechanism }

        return state.copy(
            coins = state.coins - upgrade.cost,
            upgrades = newUpgrades,
            bias = newBias,
            tickIntervalSeconds = newInterval,
            hasMechanicalMemory = newMechanicalMemory,
            hasPedalMechanism = newPedalMechanism
        )
    }

    /** Clears [celebrating] flag (called by the view after the animation finishes). */
    fun clearCelebration(state: PianoGameState): PianoGameState = state.copy(celebrating = false)
}
