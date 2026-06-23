package com.xanticious.androidgames.model.games.idleplayerpiano

/** One of the 8 piano notes (0 = C, 1 = D, …, 7 = C′). */
data class PianoNote(val pitch: Int) {
    val label: String get() = NOTE_NAMES[pitch.coerceIn(0, 7)]

    companion object {
        val NOTE_NAMES = listOf("C", "D", "E", "F", "G", "A", "B", "C′")
    }
}

/** The current target sequence the piano is trying to reproduce. */
data class TargetSequence(val notes: List<PianoNote>)

/**
 * One purchasable upgrade.
 *
 * [requires] is a list of upgrade IDs that must be purchased first.
 * [biasBonus] adds to the correct-note probability.
 * [speedMultiplier] multiplies ticks per second (stacks multiplicatively).
 * [pedalMechanism] enables a 5 % chance to advance progress without a matching note.
 * [mechanicalMemory] prevents the piano from immediately repeating the last note.
 */
data class PianoUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val cost: Long,
    val requires: List<String>,
    val purchased: Boolean = false,
    val biasBonus: Float = 0f,
    val speedMultiplier: Float = 1f,
    val pedalMechanism: Boolean = false,
    val mechanicalMemory: Boolean = false
)

/**
 * Full snapshot of the Player Piano game.
 *
 * [bias] is the probability boost toward the next correct note (0.0 = uniform, 0.90 = cap).
 * [tickIntervalSeconds] is the delay between automatically played notes.
 * [matchesCompleted] counts how many full sequence matches the piano has achieved.
 * [sequenceLength] is the current target length (2 – 8).
 * [target] is the note sequence the piano must reproduce.
 * [progressIndex] is how many leading notes of [target] have been matched consecutively.
 * [recentNotes] holds the last [TICKER_SIZE] notes played (newest last).
 * [lastNote] is the most recently played note (used by Mechanical Memory).
 * [upgrades] is the ordered upgrade catalog with purchased flags.
 * [tickAccumulator] counts fractional seconds toward the next note tick.
 * [totalNotesPlayed] is a lifetime counter.
 * [celebrating] is set true briefly after each sequence match.
 * [hasMechanicalMemory] and [hasPedalMechanism] are derived from upgrades but cached for speed.
 */
data class PianoGameState(
    val coins: Long,
    val bias: Float,
    val tickIntervalSeconds: Float,
    val matchesCompleted: Int,
    val sequenceLength: Int,
    val target: TargetSequence,
    val progressIndex: Int,
    val recentNotes: List<PianoNote>,
    val lastNote: PianoNote?,
    val upgrades: List<PianoUpgrade>,
    val tickAccumulator: Float,
    val totalNotesPlayed: Long,
    val celebrating: Boolean,
    val hasMechanicalMemory: Boolean,
    val hasPedalMechanism: Boolean
) {
    companion object {
        const val MAX_BIAS = 0.90f
        const val NOTE_COUNT = 8
        const val TICKER_SIZE = 8
        const val BASE_TICK_INTERVAL = 1.0f
    }
}

/** Result emitted by [com.xanticious.androidgames.controller.games.idleplayerpiano.IdlePlayerPianoController.tick]. */
sealed class PianoTickResult {
    /** The piano played a note; [progressAdvanced] is true if it matched the next target note. */
    data class NotePlayed(val note: PianoNote, val progressAdvanced: Boolean) : PianoTickResult()
    /** The current target sequence was fully matched. */
    data class SequenceMatched(
        val coinsEarned: Long,
        val newTarget: TargetSequence,
        val newSequenceLength: Int,
        val matchesCompleted: Int
    ) : PianoTickResult()
    /** No note was played this delta (accumulator hasn't filled yet). */
    data object Nothing : PianoTickResult()
}
