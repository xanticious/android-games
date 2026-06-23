package com.xanticious.androidgames.games.taprhythm

import com.xanticious.androidgames.state.games.taprhythm.TapRhythmPhase
import com.xanticious.androidgames.state.games.taprhythm.TapRhythmStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class TapRhythmStateMachineTest {

    private fun machine() = TapRhythmStateMachine(CoroutineScope(Dispatchers.Unconfined))

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialPhase_isIdle() {
        assertEquals(TapRhythmPhase.IDLE, machine().phase.value)
    }

    // ── Idle → Settings ───────────────────────────────────────────────────────

    @Test
    fun openGame_fromIdle_transitionsToSettings() {
        val m = machine()
        m.openGame()
        assertEquals(TapRhythmPhase.SETTINGS, m.phase.value)
    }

    // ── Settings → HowToPlay ─────────────────────────────────────────────────

    @Test
    fun openHowToPlay_fromSettings_transitionsToHowToPlay() {
        val m = machine()
        m.openGame()
        m.openHowToPlay()
        assertEquals(TapRhythmPhase.HOW_TO_PLAY, m.phase.value)
    }

    // ── HowToPlay → Settings ─────────────────────────────────────────────────

    @Test
    fun backToSettings_fromHowToPlay_transitionsToSettings() {
        val m = machine()
        m.openGame()
        m.openHowToPlay()
        m.backToSettings()
        assertEquals(TapRhythmPhase.SETTINGS, m.phase.value)
    }

    // ── Settings → CountIn ────────────────────────────────────────────────────

    @Test
    fun confirmConfig_fromSettings_transitionsToCountIn() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        assertEquals(TapRhythmPhase.COUNT_IN, m.phase.value)
    }

    // ── CountIn → Playing ─────────────────────────────────────────────────────

    @Test
    fun countInFinished_fromCountIn_transitionsToPlaying() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        assertEquals(TapRhythmPhase.PLAYING, m.phase.value)
    }

    // ── Playing → Results (death) ─────────────────────────────────────────────

    @Test
    fun healthDepleted_fromPlaying_transitionsToResults() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        m.healthDepleted()
        assertEquals(TapRhythmPhase.RESULTS, m.phase.value)
    }

    // ── Results → CountIn (Replay) ────────────────────────────────────────────

    @Test
    fun replay_fromResults_transitionsToCountIn() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        m.healthDepleted()
        m.replay()
        assertEquals(TapRhythmPhase.COUNT_IN, m.phase.value)
    }

    // ── Results → Settings (NewRun) ───────────────────────────────────────────

    @Test
    fun newRun_fromResults_transitionsToSettings() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        m.healthDepleted()
        m.newRun()
        assertEquals(TapRhythmPhase.SETTINGS, m.phase.value)
    }

    // ── Results → Idle (Menu) ─────────────────────────────────────────────────

    @Test
    fun menu_fromResults_transitionsToIdle() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        m.healthDepleted()
        m.menu()
        assertEquals(TapRhythmPhase.IDLE, m.phase.value)
    }

    // ── Full round-trip via HowToPlay ─────────────────────────────────────────

    @Test
    fun fullFlowViaHowToPlay_reachesPlaying() {
        val m = machine()
        m.openGame()
        m.openHowToPlay()
        m.backToSettings()
        m.confirmConfig()
        m.countInFinished()
        assertEquals(TapRhythmPhase.PLAYING, m.phase.value)
    }

    // ── Replay then die again ─────────────────────────────────────────────────

    @Test
    fun replayThenDie_transitionsToResultsAgain() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        m.healthDepleted()
        m.replay()
        m.countInFinished()
        m.healthDepleted()
        assertEquals(TapRhythmPhase.RESULTS, m.phase.value)
    }

    // ── NewRun then play again ────────────────────────────────────────────────

    @Test
    fun newRun_thenConfirmAndPlay_reachesPlaying() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        m.healthDepleted()
        m.newRun()
        m.confirmConfig()
        m.countInFinished()
        assertEquals(TapRhythmPhase.PLAYING, m.phase.value)
    }

    // ── Menu returns to Idle ──────────────────────────────────────────────────

    @Test
    fun menuFromResults_thenOpenGameAgain_reachesSettings() {
        val m = machine()
        m.openGame()
        m.confirmConfig()
        m.countInFinished()
        m.healthDepleted()
        m.menu()
        m.openGame()
        assertEquals(TapRhythmPhase.SETTINGS, m.phase.value)
    }
}
