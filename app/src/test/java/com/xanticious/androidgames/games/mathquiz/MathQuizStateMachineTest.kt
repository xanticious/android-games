package com.xanticious.androidgames.games.mathquiz

import com.xanticious.androidgames.state.games.mathquiz.MathQuizPhase
import com.xanticious.androidgames.state.games.mathquiz.MathQuizStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathQuizStateMachineTest {
    private fun machine() = MathQuizStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(MathQuizPhase.IDLE, m.phase.value)
    }

    @Test
    fun openGame_movesToSettings() {
        val m = machine()
        m.openGame()
        assertEquals(MathQuizPhase.SETTINGS, m.phase.value)
    }

    @Test
    fun back_fromSettings_movesToIdle() {
        val m = machine()
        m.openGame()
        m.back()
        assertEquals(MathQuizPhase.IDLE, m.phase.value)
    }

    @Test
    fun startSession_movesToGeneratingQuestion() {
        val m = machine()
        m.openGame()
        m.startSession()
        assertEquals(MathQuizPhase.GENERATING_QUESTION, m.phase.value)
    }

    @Test
    fun questionReady_movesToAwaitingAnswer() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        assertEquals(MathQuizPhase.AWAITING_ANSWER, m.phase.value)
    }

    @Test
    fun answerCorrect_movesToShowingFeedback() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerCorrect()
        assertEquals(MathQuizPhase.SHOWING_FEEDBACK, m.phase.value)
    }

    @Test
    fun answerIncorrect_movesToShowingFeedback() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerIncorrect()
        assertEquals(MathQuizPhase.SHOWING_FEEDBACK, m.phase.value)
    }

    @Test
    fun answerCorrect_setsLastAnswerCorrect_true() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerCorrect()
        assertTrue(m.lastAnswerCorrect)
    }

    @Test
    fun answerIncorrect_setsLastAnswerCorrect_false() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerIncorrect()
        assertFalse(m.lastAnswerCorrect)
    }

    @Test
    fun countdownExpired_fromAwaitingAnswer_movesToResults() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.countdownExpired()
        assertEquals(MathQuizPhase.RESULTS, m.phase.value)
    }

    @Test
    fun feedbackDone_withMoreQuestions_movesToGeneratingQuestion() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerCorrect()
        m.feedbackDone(hasMore = true)
        assertEquals(MathQuizPhase.GENERATING_QUESTION, m.phase.value)
    }

    @Test
    fun feedbackDone_sessionComplete_movesToResults() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerCorrect()
        m.feedbackDone(hasMore = false)
        assertEquals(MathQuizPhase.RESULTS, m.phase.value)
    }

    @Test
    fun playAgain_fromResults_movesToGeneratingQuestion() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerCorrect()
        m.feedbackDone(hasMore = false)
        m.playAgain()
        assertEquals(MathQuizPhase.GENERATING_QUESTION, m.phase.value)
    }

    @Test
    fun adjustSettings_fromResults_movesToSettings() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerCorrect()
        m.feedbackDone(hasMore = false)
        m.adjustSettings()
        assertEquals(MathQuizPhase.SETTINGS, m.phase.value)
    }

    @Test
    fun fullRoundTrip_playAgain_thenFeedbackDone_movesToResults() {
        val m = machine()
        m.openGame()
        m.startSession()
        m.questionReady()
        m.answerIncorrect()
        m.feedbackDone(hasMore = false)
        m.playAgain()
        m.questionReady()
        m.answerCorrect()
        m.feedbackDone(hasMore = false)
        assertEquals(MathQuizPhase.RESULTS, m.phase.value)
    }
}
