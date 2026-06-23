package com.xanticious.androidgames.games.melodymaster

import com.xanticious.androidgames.state.games.melodymaster.MelodyMasterPhase
import com.xanticious.androidgames.state.games.melodymaster.MelodyMasterStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class MelodyMasterStateMachineTest {

    private fun machine() = MelodyMasterStateMachine(CoroutineScope(Dispatchers.Unconfined))

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialPhase_isIdle() {
        assertEquals(MelodyMasterPhase.IDLE, machine().phase.value)
    }

    // ── Idle → Setup ──────────────────────────────────────────────────────────

    @Test
    fun startGame_fromIdle_movesToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(MelodyMasterPhase.SETUP, m.phase.value)
    }

    // ── Setup → HowToPlay ────────────────────────────────────────────────────

    @Test
    fun openHowToPlay_fromSetup_movesToHowToPlay() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        assertEquals(MelodyMasterPhase.HOW_TO_PLAY, m.phase.value)
    }

    // ── HowToPlay → Setup ────────────────────────────────────────────────────

    @Test
    fun backToSetup_fromHowToPlay_movesToSetup() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        m.backToSetup()
        assertEquals(MelodyMasterPhase.SETUP, m.phase.value)
    }

    // ── Setup → CountIn ──────────────────────────────────────────────────────

    @Test
    fun confirmConfig_fromSetup_movesToCountIn() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        assertEquals(MelodyMasterPhase.COUNT_IN, m.phase.value)
    }

    // ── CountIn → Playing ────────────────────────────────────────────────────

    @Test
    fun countInFinished_fromCountIn_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.countInFinished()
        assertEquals(MelodyMasterPhase.PLAYING, m.phase.value)
    }

    // ── Playing → Results ────────────────────────────────────────────────────

    @Test
    fun trackFinished_fromPlaying_movesToResults() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.countInFinished()
        m.trackFinished()
        assertEquals(MelodyMasterPhase.RESULTS, m.phase.value)
    }

    // ── Results → CountIn (Replay) ───────────────────────────────────────────

    @Test
    fun replay_fromResults_movesToCountIn() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.countInFinished()
        m.trackFinished()
        m.replay()
        assertEquals(MelodyMasterPhase.COUNT_IN, m.phase.value)
    }

    // ── Results → Setup (NewTrack) ───────────────────────────────────────────

    @Test
    fun newTrack_fromResults_movesToSetup() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.countInFinished()
        m.trackFinished()
        m.newTrack()
        assertEquals(MelodyMasterPhase.SETUP, m.phase.value)
    }

    // ── Results → Idle (Menu) ────────────────────────────────────────────────

    @Test
    fun menu_fromResults_movesToIdle() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.countInFinished()
        m.trackFinished()
        m.menu()
        assertEquals(MelodyMasterPhase.IDLE, m.phase.value)
    }

    // ── Full round-trip via HowToPlay ─────────────────────────────────────────

    @Test
    fun fullFlowViaHowToPlay_reachesPlaying() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        m.backToSetup()
        m.confirmConfig()
        m.countInFinished()
        assertEquals(MelodyMasterPhase.PLAYING, m.phase.value)
    }

    // ── Replay then finish again ──────────────────────────────────────────────

    @Test
    fun replayThenFinish_movesToResultsAgain() {
        val m = machine()
        m.startGame()
        m.confirmConfig()
        m.countInFinished()
        m.trackFinished()
        m.replay()
        m.countInFinished()
        m.trackFinished()
        assertEquals(MelodyMasterPhase.RESULTS, m.phase.value)
    }
}
