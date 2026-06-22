package com.xanticious.androidgames.model.games.melodymaster

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.rhythm.TimingWindows
import com.xanticious.androidgames.model.games.rhythm.Track

/**
 * Per-game data model for Melody Master. Builds on the shared rhythm types in
 * `model/games/rhythm/RhythmHighway.kt`; no Android or UI imports here.
 */

/** Musical scale to use when generating a track. */
enum class Scale { MAJOR, MINOR }

/**
 * Tunable parameters that feed [com.xanticious.androidgames.controller.games.melodymaster.MelodyGenerator].
 * All values are pure data; generation is deterministic from the seed.
 */
data class GenerationParams(
    val bpm: Int,
    val scale: Scale,
    /** Probability (0..1) that any given beat slot contains a note rather than a rest. */
    val noteDensity: Float,
    /** Probability (0..1) that the next scale degree stays within ±2 steps of the current. */
    val stepBias: Float,
    /** Subdivision: 1=quarter, 2=eighth, 4=sixteenth. */
    val subdivision: Int,
    /** Probability (0..1) that a note slot spawns a 2-lane chord. */
    val chordChance: Float,
    val bars: Int
)

/** Input to the generator. Seed makes the output reproducible. */
data class MelodyTrackRequest(
    val difficulty: GameDifficulty,
    val seed: Long,
    /** Explicit scale choice; the generator picks randomly when null. */
    val scalePreference: Scale? = null
)

/** Per-difficulty highway configuration (generation params + timing + scroll). */
data class MelodyDifficultyConfig(
    /** How many of the 5 highway lanes are active for this difficulty. */
    val laneCount: Int,
    val bpmRange: IntRange,
    val noteDensity: Float,
    val stepBias: Float,
    /** Subdivision multiplier: 1=quarter, 2=eighth, 4=sixteenth. */
    val subdivision: Int,
    val chordChance: Float,
    val bars: Int,
    val windows: TimingWindows,
    /** How far ahead (ms) gems appear before their hit time. */
    val leadTimeMs: Long
)

/**
 * Generator result: the playable [track] plus metadata about the closing
 * flourish phrase (the final bar). The view uses [flourishStartMs] to determine
 * whether the player landed all flourish notes for the Perfect Finish bonus.
 */
data class MelodyTrack(
    val track: Track,
    /** Song-clock time (ms) at which the closing flourish begins. */
    val flourishStartMs: Long,
    val seed: Long
)
