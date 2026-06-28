package com.xanticious.androidgames.games.idlefarmers

import com.xanticious.androidgames.state.games.idlefarmers.IdleFarmersPhase
import com.xanticious.androidgames.state.games.idlefarmers.IdleFarmersStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class IdleFarmersStateMachineTest {
    private fun machine() = IdleFarmersStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(IdleFarmersPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_movesToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(IdleFarmersPhase.SETUP, m.phase.value)
    }

    @Test
    fun openHowToPlay_fromSetup_movesToHowToPlay() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        assertEquals(IdleFarmersPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun backToSetup_fromHowToPlay_returnsToSetup() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        m.backToSetup()
        assertEquals(IdleFarmersPhase.SETUP, m.phase.value)
    }

    @Test
    fun startPlaying_fromSetup_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.startPlaying()
        assertEquals(IdleFarmersPhase.PLAYING, m.phase.value)
    }

    @Test
    fun openUpgrades_fromPlaying_movesToUpgradeMenuOpen() {
        val m = machine()
        m.startGame()
        m.startPlaying()
        m.openUpgrades()
        assertEquals(IdleFarmersPhase.UPGRADE_MENU_OPEN, m.phase.value)
    }

    @Test
    fun closeUpgrades_fromUpgradeMenuOpen_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.startPlaying()
        m.openUpgrades()
        m.closeUpgrades()
        assertEquals(IdleFarmersPhase.PLAYING, m.phase.value)
    }

    @Test
    fun eventTriggered_fromPlaying_movesToEventActive() {
        val m = machine()
        m.startGame()
        m.startPlaying()
        m.eventTriggered()
        assertEquals(IdleFarmersPhase.EVENT_ACTIVE, m.phase.value)
    }

    @Test
    fun eventResolved_fromEventActive_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.startPlaying()
        m.eventTriggered()
        m.eventResolved()
        assertEquals(IdleFarmersPhase.PLAYING, m.phase.value)
    }

    @Test
    fun fullFlow_startToPlayingWithEvent() {
        val m = machine()
        m.startGame()
        m.startPlaying()
        m.eventTriggered()
        m.eventResolved()
        assertEquals(IdleFarmersPhase.PLAYING, m.phase.value)
    }
}
