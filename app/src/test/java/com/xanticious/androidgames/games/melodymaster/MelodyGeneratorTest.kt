package com.xanticious.androidgames.games.melodymaster

import com.xanticious.androidgames.controller.games.melodymaster.MelodyConfig
import com.xanticious.androidgames.controller.games.melodymaster.MelodyGenerator
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.melodymaster.MelodyTrackRequest
import com.xanticious.androidgames.model.games.melodymaster.Scale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MelodyGeneratorTest {

    private fun easyRequest(seed: Long = 42L) =
        MelodyTrackRequest(GameDifficulty.EASY, seed)

    private fun mediumRequest(seed: Long = 42L) =
        MelodyTrackRequest(GameDifficulty.MEDIUM, seed)

    private fun hardRequest(seed: Long = 42L) =
        MelodyTrackRequest(GameDifficulty.HARD, seed)

    // ── Determinism ──────────────────────────────────────────────────────────

    @Test
    fun generate_sameSeed_producesIdenticalEasyTrack() {
        val a = MelodyGenerator.generate(easyRequest(99L))
        val b = MelodyGenerator.generate(easyRequest(99L))
        assertEquals(a.track, b.track)
    }

    @Test
    fun generate_sameSeed_producesIdenticalMediumTrack() {
        val a = MelodyGenerator.generate(mediumRequest(7777L))
        val b = MelodyGenerator.generate(mediumRequest(7777L))
        assertEquals(a.track, b.track)
    }

    @Test
    fun generate_sameSeed_producesIdenticalHardTrack() {
        val a = MelodyGenerator.generate(hardRequest(123456L))
        val b = MelodyGenerator.generate(hardRequest(123456L))
        assertEquals(a.track, b.track)
    }

    @Test
    fun generate_differentSeeds_produceDifferentTracks() {
        val a = MelodyGenerator.generate(easyRequest(1L))
        val b = MelodyGenerator.generate(easyRequest(2L))
        assertTrue("Different seeds should produce different tracks", a.track != b.track)
    }

    // ── Monotonically non-decreasing hit times ────────────────────────────────

    @Test
    fun generate_easy_hitTimesAreNonDecreasing() {
        val notes = MelodyGenerator.generate(easyRequest(42L)).track.notes
        assertTrue("No notes generated for easy", notes.isNotEmpty())
        notes.zipWithNext { a, b ->
            assertTrue(
                "Hit time ${b.hitTimeMs} < previous ${a.hitTimeMs}",
                b.hitTimeMs >= a.hitTimeMs
            )
        }
    }

    @Test
    fun generate_medium_hitTimesAreNonDecreasing() {
        val notes = MelodyGenerator.generate(mediumRequest(42L)).track.notes
        assertTrue(notes.isNotEmpty())
        notes.zipWithNext { a, b -> assertTrue(b.hitTimeMs >= a.hitTimeMs) }
    }

    @Test
    fun generate_hard_hitTimesAreNonDecreasing() {
        val notes = MelodyGenerator.generate(hardRequest(42L)).track.notes
        assertTrue(notes.isNotEmpty())
        notes.zipWithNext { a, b -> assertTrue(b.hitTimeMs >= a.hitTimeMs) }
    }

    // For Easy (no chords, subdivision=1), hit times must be STRICTLY increasing

    @Test
    fun generate_easy_hitTimesAreStrictlyIncreasing() {
        val notes = MelodyGenerator.generate(easyRequest(42L)).track.notes
        assertTrue(notes.isNotEmpty())
        notes.zipWithNext { a, b ->
            assertTrue(
                "Easy track hit times must be strictly increasing",
                b.hitTimeMs > a.hitTimeMs
            )
        }
    }

    // ── Lanes within difficulty lane count ────────────────────────────────────

    @Test
    fun generate_easy_allLanesWithinThreeLaneCount() {
        val laneCount = MelodyConfig.configFor(GameDifficulty.EASY).laneCount
        val notes = MelodyGenerator.generate(easyRequest(42L)).track.notes
        notes.forEach { note ->
            assertTrue(
                "Lane ${note.lane} out of range [0,$laneCount)",
                note.lane in 0 until laneCount
            )
        }
    }

    @Test
    fun generate_medium_allLanesWithinFourLaneCount() {
        val laneCount = MelodyConfig.configFor(GameDifficulty.MEDIUM).laneCount
        val notes = MelodyGenerator.generate(mediumRequest(42L)).track.notes
        notes.forEach { note ->
            assertTrue(
                "Lane ${note.lane} out of range [0,$laneCount)",
                note.lane in 0 until laneCount
            )
        }
    }

    @Test
    fun generate_hard_allLanesWithinFiveLaneCount() {
        val laneCount = MelodyConfig.configFor(GameDifficulty.HARD).laneCount
        val notes = MelodyGenerator.generate(hardRequest(42L)).track.notes
        notes.forEach { note ->
            assertTrue(
                "Lane ${note.lane} out of range [0,$laneCount)",
                note.lane in 0 until laneCount
            )
        }
    }

    // ── Flourish tagged ───────────────────────────────────────────────────────

    @Test
    fun generate_easy_flourishStartMsIsPositive() {
        val result = MelodyGenerator.generate(easyRequest(42L))
        assertTrue("flourishStartMs should be > 0", result.flourishStartMs > 0L)
    }

    @Test
    fun generate_flourishStartMs_isBeforeTrackEnd() {
        val result = MelodyGenerator.generate(hardRequest(42L))
        val lastNoteMs = result.track.notes.maxOf { it.hitTimeMs }
        assertTrue(
            "flourishStartMs ${result.flourishStartMs} should be before last note $lastNoteMs",
            result.flourishStartMs <= lastNoteMs
        )
    }

    @Test
    fun generate_flourishHasNotes_afterFlourishStartMs() {
        val result = MelodyGenerator.generate(hardRequest(42L))
        val flourishNotes = result.track.notes.filter { it.hitTimeMs >= result.flourishStartMs }
        assertTrue("Expected at least one note in the flourish bar", flourishNotes.isNotEmpty())
    }

    @Test
    fun generate_flourishSeedStoredInResult() {
        val seed = 55L
        val result = MelodyGenerator.generate(easyRequest(seed))
        assertEquals(seed, result.seed)
    }

    // ── Track not empty ───────────────────────────────────────────────────────

    @Test
    fun generate_easy_producesNonEmptyTrack() {
        val result = MelodyGenerator.generate(easyRequest(1L))
        assertTrue("Easy track should have notes", result.track.notes.isNotEmpty())
    }

    // ── BPM within configured range ───────────────────────────────────────────

    @Test
    fun generate_easy_bpmWithinEasyRange() {
        val config = MelodyConfig.configFor(GameDifficulty.EASY)
        val result = MelodyGenerator.generate(easyRequest(42L))
        assertTrue(result.track.bpm in config.bpmRange)
    }

    @Test
    fun generate_hard_bpmWithinHardRange() {
        val config = MelodyConfig.configFor(GameDifficulty.HARD)
        val result = MelodyGenerator.generate(hardRequest(42L))
        assertTrue(result.track.bpm in config.bpmRange)
    }

    // ── Scale preference ─────────────────────────────────────────────────────

    @Test
    fun generate_majorScalePreference_sameResultWithSameSeed() {
        val req1 = MelodyTrackRequest(GameDifficulty.EASY, 42L, Scale.MAJOR)
        val req2 = MelodyTrackRequest(GameDifficulty.EASY, 42L, Scale.MAJOR)
        assertEquals(MelodyGenerator.generate(req1).track, MelodyGenerator.generate(req2).track)
    }
}
