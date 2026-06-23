package com.xanticious.androidgames.games.idleplayerpiano

import com.xanticious.androidgames.state.games.idleplayerpiano.IdlePlayerPianoPhase
import com.xanticious.androidgames.state.games.idleplayerpiano.IdlePlayerPianoStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class IdlePlayerPianoStateMachineTest {

    private fun machine() = IdlePlayerPianoStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(IdlePlayerPianoPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_movesToPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(IdlePlayerPianoPhase.PLAYING, m.phase.value)
    }

    @Test
    fun openHowToPlay_fromIdle_movesToHowToPlay() {
        val m = machine()
        m.openHowToPlay()
        assertEquals(IdlePlayerPianoPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun dismissHowToPlay_returnsToIdle() {
        val m = machine()
        m.openHowToPlay()
        m.dismissHowToPlay()
        assertEquals(IdlePlayerPianoPhase.IDLE, m.phase.value)
    }

    @Test
    fun sequenceCompleted_fromPlaying_movesToSequenceMatched() {
        val m = machine()
        m.startGame()
        m.sequenceCompleted()
        assertEquals(IdlePlayerPianoPhase.SEQUENCE_MATCHED, m.phase.value)
    }

    @Test
    fun celebrationDismissed_fromSequenceMatched_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.sequenceCompleted()
        m.celebrationDismissed()
        assertEquals(IdlePlayerPianoPhase.PLAYING, m.phase.value)
    }

    @Test
    fun openUpgradeMenu_fromPlaying_movesToUpgradeMenuOpen() {
        val m = machine()
        m.startGame()
        m.openUpgradeMenu()
        assertEquals(IdlePlayerPianoPhase.UPGRADE_MENU_OPEN, m.phase.value)
    }

    @Test
    fun closeUpgradeMenu_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.openUpgradeMenu()
        m.closeUpgradeMenu()
        assertEquals(IdlePlayerPianoPhase.PLAYING, m.phase.value)
    }

    @Test
    fun upgradePurchased_fromUpgradeMenuOpen_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.openUpgradeMenu()
        m.upgradePurchased()
        assertEquals(IdlePlayerPianoPhase.PLAYING, m.phase.value)
    }

    @Test
    fun openHowToPlay_fromPlaying_movesToHowToPlay() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        assertEquals(IdlePlayerPianoPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun fullFlow_twoMatchesThenUpgrade_remainsInPlaying() {
        val m = machine()
        m.startGame()
        m.sequenceCompleted()
        m.celebrationDismissed()
        m.sequenceCompleted()
        m.celebrationDismissed()
        m.openUpgradeMenu()
        m.upgradePurchased()
        assertEquals(IdlePlayerPianoPhase.PLAYING, m.phase.value)
    }
}
