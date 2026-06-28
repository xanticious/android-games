package com.xanticious.androidgames.games.idlebounce

import com.xanticious.androidgames.state.games.idlebounce.IdleBouncePhase
import com.xanticious.androidgames.state.games.idlebounce.IdleBounceStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class IdleBounceStateMachineTest {
    private fun machine() = IdleBounceStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        assertEquals(IdleBouncePhase.IDLE, machine().phase.value)
    }

    @Test
    fun startGame_transitionsToSetup() {
        val machine = machine()
        machine.startGame()
        assertEquals(IdleBouncePhase.SETUP, machine.phase.value)
    }

    @Test
    fun openHowToPlay_fromSetup_transitionsToHowToPlay() {
        val machine = machine()
        machine.startGame()
        machine.openHowToPlay()
        assertEquals(IdleBouncePhase.HOW_TO_PLAY, machine.phase.value)
    }

    @Test
    fun backToSetup_fromHowToPlay_transitionsToSetup() {
        val machine = machine()
        machine.startGame()
        machine.openHowToPlay()
        machine.backToSetup()
        assertEquals(IdleBouncePhase.SETUP, machine.phase.value)
    }

    @Test
    fun confirmStart_fromSetup_transitionsToPlaying() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        assertEquals(IdleBouncePhase.PLAYING, machine.phase.value)
    }

    @Test
    fun openUpgradeMenu_fromPlaying_transitionsToUpgradeMenuOpen() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.openUpgradeMenu()
        assertEquals(IdleBouncePhase.UPGRADE_MENU_OPEN, machine.phase.value)
    }

    @Test
    fun closeUpgradeMenu_fromUpgradeMenuOpen_transitionsToPlaying() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.openUpgradeMenu()
        machine.closeUpgradeMenu()
        assertEquals(IdleBouncePhase.PLAYING, machine.phase.value)
    }

    @Test
    fun upgradePurchased_fromUpgradeMenuOpen_transitionsToPlaying() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.openUpgradeMenu()
        machine.upgradePurchased()
        assertEquals(IdleBouncePhase.PLAYING, machine.phase.value)
    }
}
