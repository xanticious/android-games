package com.xanticious.androidgames.games.idleplayerpiano

import com.xanticious.androidgames.controller.games.idleplayerpiano.IdlePlayerPianoController
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoGameState
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoNote
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoTickResult
import com.xanticious.androidgames.model.games.idleplayerpiano.TargetSequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdlePlayerPianoControllerTest {

    private val controller = IdlePlayerPianoController()
    private val rng = Random(42)

    // ── Upgrades ──────────────────────────────────────────────────────────────

    @Test
    fun allUpgrades_hasEightEntries() {
        assertEquals(8, controller.allUpgrades().size)
    }

    @Test
    fun allUpgrades_allIdsUnique() {
        val ids = controller.allUpgrades().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    // ── Initial State ─────────────────────────────────────────────────────────

    @Test
    fun initialState_coinsAreZero() {
        val state = controller.initialState(rng)
        assertEquals(0L, state.coins)
    }

    @Test
    fun initialState_sequenceLengthIsTwo() {
        val state = controller.initialState(rng)
        assertEquals(2, state.sequenceLength)
    }

    @Test
    fun initialState_biasIsZero() {
        val state = controller.initialState(rng)
        assertEquals(0f, state.bias, 1e-6f)
    }

    @Test
    fun initialState_targetHasTwoNotes() {
        val state = controller.initialState(rng)
        assertEquals(2, state.target.notes.size)
    }

    // ── Target Generation ─────────────────────────────────────────────────────

    @Test
    fun generateTarget_hasRequestedLength() {
        val target = controller.generateTarget(5, rng)
        assertEquals(5, target.notes.size)
    }

    @Test
    fun generateTarget_allPitchesInRange() {
        val target = controller.generateTarget(8, rng)
        target.notes.forEach { note ->
            assertTrue(note.pitch in 0 until PianoGameState.NOTE_COUNT)
        }
    }

    @Test
    fun generateTarget_noConsecutiveDuplicates() {
        repeat(20) {
            val target = controller.generateTarget(8, rng)
            for (i in 1 until target.notes.size) {
                assertFalse(
                    "Consecutive duplicate at $i",
                    target.notes[i].pitch == target.notes[i - 1].pitch
                )
            }
        }
    }

    // ── Sequence Length Milestones ────────────────────────────────────────────

    @Test
    fun sequenceLengthFor_zeroMatches_returns2() {
        assertEquals(2, controller.sequenceLengthFor(0))
    }

    @Test
    fun sequenceLengthFor_20Matches_returns3() {
        assertEquals(3, controller.sequenceLengthFor(20))
    }

    @Test
    fun sequenceLengthFor_3000Matches_returns8() {
        assertEquals(8, controller.sequenceLengthFor(3_000))
    }

    @Test
    fun sequenceLengthFor_largeNumber_neverExceeds8() {
        assertEquals(8, controller.sequenceLengthFor(100_000))
    }

    // ── Coin Rewards ──────────────────────────────────────────────────────────

    @Test
    fun coinsForMatch_length2_returns10() {
        assertEquals(10L, controller.coinsForMatch(2))
    }

    @Test
    fun coinsForMatch_length8_returns5000() {
        assertEquals(5_000L, controller.coinsForMatch(8))
    }

    // ── Tick — Nothing ────────────────────────────────────────────────────────

    @Test
    fun tick_smallDelta_returnsNothing() {
        val state = controller.initialState(rng)
        val (_, result) = controller.tick(state, 0.01f, rng)
        assertEquals(PianoTickResult.Nothing, result)
    }

    // ── Tick — Note Played ────────────────────────────────────────────────────

    @Test
    fun tick_fullInterval_playsNote() {
        val state = controller.initialState(rng)
        val (_, result) = controller.tick(state, PianoGameState.BASE_TICK_INTERVAL, rng)
        assertTrue(result is PianoTickResult.NotePlayed)
    }

    @Test
    fun tick_notePlayed_addedToRecentNotes() {
        val state = controller.initialState(rng)
        val (newState, result) = controller.tick(state, PianoGameState.BASE_TICK_INTERVAL, rng)
        assertTrue(result is PianoTickResult.NotePlayed)
        assertEquals(1, newState.recentNotes.size)
    }

    @Test
    fun tick_wrongNote_resetsProgress() {
        // Force a state where the next target note is C (pitch 0),
        // but force the RNG to play a different note.
        val fixedRng = Random(0)
        val target = TargetSequence(listOf(PianoNote(0), PianoNote(1)))  // C, D
        val state = controller.initialState(rng).copy(
            target = target,
            progressIndex = 0,
            bias = 0f  // uniform — possible to get wrong note
        )
        // Run many ticks until we get a wrong note result (progress doesn't advance)
        var found = false
        var current = state
        repeat(50) {
            val (newState, result) = controller.tick(current, 1.0f, fixedRng)
            current = newState.copy(tickAccumulator = 0f)
            if (result is PianoTickResult.NotePlayed && !result.progressAdvanced) {
                assertEquals(0, newState.progressIndex)
                found = true
                return@repeat
            }
        }
        assertTrue("Expected at least one wrong note in 50 ticks", found)
    }

    @Test
    fun tick_correctNote_advancesProgress() {
        // Force correct note by setting bias = 1.0 (effectively capped but high enough)
        val target = TargetSequence(listOf(PianoNote(3), PianoNote(5)))  // F, A
        val state = controller.initialState(rng).copy(
            target = target,
            progressIndex = 0,
            bias = 0.89f,
            hasMechanicalMemory = false
        )
        var advanced = false
        var current = state
        repeat(20) {
            val (newState, result) = controller.tick(current, 1.0f, rng)
            current = newState.copy(tickAccumulator = 0f)
            if (result is PianoTickResult.NotePlayed && result.progressAdvanced) {
                advanced = true
                return@repeat
            }
        }
        assertTrue("Expected at least one correct note with high bias", advanced)
    }

    // ── Tick — Sequence Matched ───────────────────────────────────────────────

    @Test
    fun tick_fullMatch_incrementsMatchesCompleted() {
        // Build a 2-note target and feed exactly those notes via a deterministic RNG
        val note0 = PianoNote(2)
        val note1 = PianoNote(4)
        val target = TargetSequence(listOf(note0, note1))
        var state = controller.initialState(rng).copy(
            target = target,
            progressIndex = 0,
            bias = 1.0f.coerceAtMost(PianoGameState.MAX_BIAS),
            hasMechanicalMemory = false
        )
        // With max bias, each tick should produce the correct next note.
        // Progress: 0→1 on tick 1, then match on tick 2.
        var matched = false
        var current = state
        repeat(10) {
            if (matched) return@repeat
            val (newState, result) = controller.tick(current, 1.0f, rng)
            current = newState.copy(tickAccumulator = 0f)
            if (result is PianoTickResult.SequenceMatched) {
                assertEquals(1, result.matchesCompleted)
                matched = true
            }
        }
        assertTrue("Expected a sequence match within 10 ticks with max bias", matched)
    }

    @Test
    fun tick_sequenceMatch_awardsCoins() {
        val note0 = PianoNote(1)
        val note1 = PianoNote(3)
        val target = TargetSequence(listOf(note0, note1))
        var current = controller.initialState(rng).copy(
            target = target,
            progressIndex = 0,
            bias = PianoGameState.MAX_BIAS,
            hasMechanicalMemory = false
        )
        repeat(10) {
            val (newState, result) = controller.tick(current, 1.0f, rng)
            current = newState.copy(tickAccumulator = 0f)
            if (result is PianoTickResult.SequenceMatched) {
                assertTrue(result.coinsEarned >= 10L)
                return  // pass
            }
        }
    }

    @Test
    fun tick_recentNotes_cappedAtTickerSize() {
        var state = controller.initialState(rng)
        repeat(PianoGameState.TICKER_SIZE + 3) {
            val (newState, _) = controller.tick(state, 1.0f, rng)
            state = newState.copy(tickAccumulator = 0f)
        }
        assertTrue(state.recentNotes.size <= PianoGameState.TICKER_SIZE)
    }

    // ── Upgrade Purchases ─────────────────────────────────────────────────────

    @Test
    fun availableUpgradeIds_initialState_onlyTunedStrings() {
        val upgrades = controller.allUpgrades()
        val available = controller.availableUpgradeIds(upgrades)
        assertEquals(setOf("tuned-strings"), available)
    }

    @Test
    fun purchaseUpgrade_deductsCoins() {
        val state = controller.initialState(rng).copy(coins = 1_000L)
        val updated = controller.purchaseUpgrade(state, "tuned-strings")
        assertEquals(1_000L - 50L, updated.coins)
    }

    @Test
    fun purchaseUpgrade_marksPurchased() {
        val state = controller.initialState(rng).copy(coins = 1_000L)
        val updated = controller.purchaseUpgrade(state, "tuned-strings")
        assertTrue(updated.upgrades.first { it.id == "tuned-strings" }.purchased)
    }

    @Test
    fun purchaseUpgrade_insufficientCoins_unchanged() {
        val state = controller.initialState(rng).copy(coins = 0L)
        val updated = controller.purchaseUpgrade(state, "tuned-strings")
        assertFalse(updated.upgrades.first { it.id == "tuned-strings" }.purchased)
    }

    @Test
    fun purchaseUpgrade_prerequisiteNotMet_unchanged() {
        val state = controller.initialState(rng).copy(coins = 9_999L)
        val updated = controller.purchaseUpgrade(state, "better-hammers")  // needs tuned-strings
        assertFalse(updated.upgrades.first { it.id == "better-hammers" }.purchased)
    }

    @Test
    fun purchaseUpgrade_increasesStatedBias() {
        val state = controller.initialState(rng).copy(coins = 9_999L)
        val updated = controller.purchaseUpgrade(state, "tuned-strings")
        assertEquals(0.05f, updated.bias, 1e-5f)
    }

    @Test
    fun purchaseUpgrade_increasesSpeedWithBetterHammers() {
        // Buy tuned-strings first, then better-hammers
        var state = controller.initialState(rng).copy(coins = 9_999L)
        state = controller.purchaseUpgrade(state, "tuned-strings")
        state = controller.purchaseUpgrade(state, "better-hammers")
        assertTrue(state.tickIntervalSeconds < PianoGameState.BASE_TICK_INTERVAL)
    }

    @Test
    fun purchaseUpgrade_biasCapAt90Percent() {
        // Manually set bias to 0.89 and buy a +0.05 upgrade — should clamp to 0.90
        var state = controller.initialState(rng).copy(coins = 999_999L)
        // Buy all upgrades in order to simulate stacked bias > 0.9
        state = controller.purchaseUpgrade(state, "tuned-strings")
        state = controller.purchaseUpgrade(state, "music-roll-library")
        state = controller.purchaseUpgrade(state, "better-hammers")
        state = controller.purchaseUpgrade(state, "harmonic-resonance")
        state = controller.purchaseUpgrade(state, "refined-roll")
        assertTrue(state.bias <= PianoGameState.MAX_BIAS + 1e-5f)
    }

    // ── Mechanical Memory ─────────────────────────────────────────────────────

    @Test
    fun pickNote_withMechanicalMemory_neverRepeatsLastNote() {
        var state = controller.initialState(rng).copy(
            hasMechanicalMemory = true,
            lastNote = PianoNote(0)
        )
        val target = TargetSequence(listOf(PianoNote(2), PianoNote(3)))
        state = state.copy(target = target)
        repeat(50) {
            val note = controller.pickNote(state, target.notes[0], rng)
            assertFalse("Mechanical Memory should not repeat C", note.pitch == 0)
        }
    }

    // ── Clear Celebration ─────────────────────────────────────────────────────

    @Test
    fun clearCelebration_setsCelebratingFalse() {
        val state = controller.initialState(rng).copy(celebrating = true)
        val result = controller.clearCelebration(state)
        assertFalse(result.celebrating)
    }
}
