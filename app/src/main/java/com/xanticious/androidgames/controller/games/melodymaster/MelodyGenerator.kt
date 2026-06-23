package com.xanticious.androidgames.controller.games.melodymaster

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.melodymaster.MelodyDifficultyConfig
import com.xanticious.androidgames.model.games.melodymaster.MelodyTrack
import com.xanticious.androidgames.model.games.melodymaster.MelodyTrackRequest
import com.xanticious.androidgames.model.games.melodymaster.Scale
import com.xanticious.androidgames.model.games.rhythm.Note
import com.xanticious.androidgames.model.games.rhythm.TimingWindows
import com.xanticious.androidgames.model.games.rhythm.Track
import kotlin.math.abs
import kotlin.random.Random

/**
 * Maps [GameDifficulty] to a [MelodyDifficultyConfig].
 *
 * Design table (melody-master-design.md):
 *   Easy   → 3 lanes, 70–90 BPM,   low density,    quarter only,  no chords,  wide windows
 *   Normal → 4 lanes, 90–110 BPM,  medium density, +eighth,       rare chords
 *   Hard   → 5 lanes, 110–140 BPM, high density,   +eighth,       occasional chords, tight windows
 *
 * EASY→Easy, MEDIUM→Normal, HARD→Hard per the task spec.
 */
object MelodyConfig {
    fun configFor(difficulty: GameDifficulty): MelodyDifficultyConfig = when (difficulty) {
        GameDifficulty.EASY -> MelodyDifficultyConfig(
            laneCount = 3,
            bpmRange = 70..90,
            noteDensity = 0.45f,
            stepBias = 0.75f,
            subdivision = 1,
            chordChance = 0.0f,
            bars = 16,
            windows = TimingWindows(),          // perfect=30, great=60, good=100
            leadTimeMs = 2000L
        )
        GameDifficulty.MEDIUM -> MelodyDifficultyConfig(
            laneCount = 4,
            bpmRange = 90..110,
            noteDensity = 0.55f,
            stepBias = 0.65f,
            subdivision = 2,
            chordChance = 0.08f,
            bars = 20,
            windows = TimingWindows().scaledBy(0.85f),
            leadTimeMs = 1700L
        )
        GameDifficulty.HARD -> MelodyDifficultyConfig(
            laneCount = 5,
            bpmRange = 110..140,
            noteDensity = 0.65f,
            stepBias = 0.55f,
            subdivision = 2,
            chordChance = 0.15f,
            bars = 24,
            windows = TimingWindows().scaledBy(0.70f),
            leadTimeMs = 1400L
        )
    }
}

/**
 * Pure, deterministic melody generator.
 *
 * Algorithm (per melody-master-design.md §Note Generation):
 * 1. Pick BPM from the difficulty's range using the seed.
 * 2. Pick scale (from request preference or randomly).
 * 3. For each beat/subdivision slot decide note vs rest by noteDensity.
 * 4. Choose each note's scale degree (0–6) with a step-bias toward small melodic steps.
 * 5. Quantize: count degree usage, pick the top [laneCount] most-used degrees,
 *    sort them ascending by pitch, and map them to lanes 0..[laneCount-1].
 *    Degrees outside the top set snap to the nearest mapped lane.
 * 6. Apply chord chance: occasionally add an adjacent-lane note at the same hit time.
 * 7. Tag the final bar as the closing flourish.
 * 8. Convert to [Note] sorted by hitTimeMs (then lane within same time for chords).
 *
 * Guarantees: same seed → identical output; all lanes in [0, laneCount); hit times
 * monotonically non-decreasing; flourishStartMs is within track duration.
 */
object MelodyGenerator {

    fun generate(request: MelodyTrackRequest): MelodyTrack {
        val config = MelodyConfig.configFor(request.difficulty)
        val rng = Random(request.seed)

        // ── Step 1 & 2: pick tempo and scale ─────────────────────────────────
        val bpm = config.bpmRange.random(rng)
        val scale = request.scalePreference ?: if (rng.nextBoolean()) Scale.MAJOR else Scale.MINOR

        val beatMs = 60_000L / bpm
        val slotMs = beatMs / config.subdivision
        val beatsPerBar = 4
        val totalSlots = config.bars * beatsPerBar * config.subdivision
        val flourishStartSlot = (config.bars - 1) * beatsPerBar * config.subdivision
        val flourishStartMs = flourishStartSlot * slotMs

        // ── Step 3 & 4: generate raw degree events ────────────────────────────
        data class SlotDegree(val slot: Int, val degree: Int)

        val rawEvents = mutableListOf<SlotDegree>()
        var prevDegree = 2
        for (slot in 0 until totalSlots) {
            if (rng.nextFloat() < config.noteDensity) {
                val degree = pickDegree(prevDegree, config.stepBias, rng)
                rawEvents.add(SlotDegree(slot, degree))
                prevDegree = degree
            }
        }

        // ── Step 5: quantize degrees → lanes ─────────────────────────────────
        val degreeCounts = IntArray(7)
        for (e in rawEvents) degreeCounts[e.degree]++

        // Top-N most-used degrees, sorted ascending by pitch (= lane order low→high)
        val laneMapping: List<Int> = if (rawEvents.isEmpty()) {
            (0 until config.laneCount).toList()
        } else {
            (0 until 7)
                .sortedByDescending { degreeCounts[it] }
                .take(config.laneCount)
                .sorted()
        }

        fun degreeToLane(degree: Int): Int {
            val exact = laneMapping.indexOf(degree)
            if (exact >= 0) return exact
            // Snap to nearest mapped degree
            return laneMapping.indices.minByOrNull { abs(laneMapping[it] - degree) } ?: 0
        }

        // ── Step 6: build final note events with optional chords ──────────────
        data class NoteEvent(val slot: Int, val lane: Int)

        val noteEvents = mutableListOf<NoteEvent>()
        for (e in rawEvents) {
            val lane = degreeToLane(e.degree)
            noteEvents.add(NoteEvent(e.slot, lane))
            if (config.chordChance > 0f && rng.nextFloat() < config.chordChance) {
                val chordLane = if (lane < config.laneCount - 1) lane + 1 else lane - 1
                noteEvents.add(NoteEvent(e.slot, chordLane))
            }
        }

        // ── Step 7 & 8: convert to Note[], deduplicate same-slot/same-lane, sort ──
        val notes = noteEvents
            .map { Note(lane = it.lane, hitTimeMs = it.slot * slotMs) }
            .distinctBy { Pair(it.hitTimeMs, it.lane) }
            .sortedWith(compareBy({ it.hitTimeMs }, { it.lane }))

        val track = Track(bpm = bpm, notes = notes)
        return MelodyTrack(track = track, flourishStartMs = flourishStartMs, seed = request.seed)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Pick the next scale degree (0–6). With probability [stepBias], take a
     * small melodic step (delta in -2..+2); otherwise pick any degree uniformly.
     */
    private fun pickDegree(prev: Int, stepBias: Float, rng: Random): Int =
        if (rng.nextFloat() < stepBias) {
            val delta = rng.nextInt(5) - 2   // -2, -1, 0, 1, 2
            (prev + delta).coerceIn(0, 6)
        } else {
            rng.nextInt(7)
        }
}
