package com.xanticious.androidgames.games.flashcards

import com.xanticious.androidgames.state.games.flashcards.FlashCardPhase
import com.xanticious.androidgames.state.games.flashcards.FlashCardStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class FlashCardStateMachineTest {
    private fun machine() = FlashCardStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun openGame_movesToPackPicker() {
        val m = machine()
        m.openGame()
        assertEquals(FlashCardPhase.PACK_PICKER, m.phase.value)
    }

    @Test
    fun packSelected_movesToSettings() {
        val m = machine()
        m.openGame()
        m.packSelected()
        assertEquals(FlashCardPhase.SETTINGS, m.phase.value)
    }

    @Test
    fun back_fromPackPicker_movesToIdle() {
        val m = machine()
        m.openGame()
        m.back()
        assertEquals(FlashCardPhase.IDLE, m.phase.value)
    }

    @Test
    fun back_fromSettings_movesToPackPicker() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.back()
        assertEquals(FlashCardPhase.PACK_PICKER, m.phase.value)
    }

    @Test
    fun startSession_movesToDrawingCard() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        assertEquals(FlashCardPhase.DRAWING_CARD, m.phase.value)
    }

    @Test
    fun cardReady_movesToShowingFront() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        assertEquals(FlashCardPhase.SHOWING_FRONT, m.phase.value)
    }

    @Test
    fun flipTapped_movesToShowingBack() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.flipTapped()
        assertEquals(FlashCardPhase.SHOWING_BACK, m.phase.value)
    }

    @Test
    fun gotIt_movesToRecordingResult() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.flipTapped()
        m.gotIt()
        assertEquals(FlashCardPhase.RECORDING_RESULT, m.phase.value)
    }

    @Test
    fun oops_movesToRecordingResult() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.flipTapped()
        m.oops()
        assertEquals(FlashCardPhase.RECORDING_RESULT, m.phase.value)
    }

    @Test
    fun moreCards_movesToDrawingCard() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.flipTapped()
        m.gotIt()
        m.moreCards()
        assertEquals(FlashCardPhase.DRAWING_CARD, m.phase.value)
    }

    @Test
    fun sessionComplete_movesToResults() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.flipTapped()
        m.gotIt()
        m.sessionComplete()
        assertEquals(FlashCardPhase.RESULTS, m.phase.value)
    }

    @Test
    fun sessionEnded_fromShowingFront_movesToResults() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.sessionEnded()
        assertEquals(FlashCardPhase.RESULTS, m.phase.value)
    }

    @Test
    fun playAgain_fromResults_movesToDrawingCard() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.sessionEnded()
        m.playAgain()
        assertEquals(FlashCardPhase.DRAWING_CARD, m.phase.value)
    }

    @Test
    fun reviewMissed_fromResults_movesToDrawingCard() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.sessionEnded()
        m.reviewMissed()
        assertEquals(FlashCardPhase.DRAWING_CARD, m.phase.value)
    }

    @Test
    fun backToSettings_fromResults_movesToSettings() {
        val m = machine()
        m.openGame()
        m.packSelected()
        m.startSession()
        m.cardReady()
        m.sessionEnded()
        m.backToSettings()
        assertEquals(FlashCardPhase.SETTINGS, m.phase.value)
    }
}
