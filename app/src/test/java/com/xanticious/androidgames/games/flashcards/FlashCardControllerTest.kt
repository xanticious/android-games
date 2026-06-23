package com.xanticious.androidgames.games.flashcards

import com.xanticious.androidgames.controller.games.flashcards.FlashCardController
import com.xanticious.androidgames.model.games.flashcards.FlashCard
import com.xanticious.androidgames.model.games.flashcards.FlashCardDuration
import com.xanticious.androidgames.model.games.flashcards.FlashCardMode
import com.xanticious.androidgames.model.games.flashcards.FlashCardPack
import com.xanticious.androidgames.model.games.flashcards.FlashCardProgress
import com.xanticious.androidgames.model.games.flashcards.FlashCardSettings
import com.xanticious.androidgames.model.games.flashcards.SchoolLevel
import com.xanticious.androidgames.model.games.flashcards.ShowSide
import com.xanticious.androidgames.model.games.flashcards.SubjectTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashCardControllerTest {
    private val controller = FlashCardController()

    private fun card(id: String) = FlashCard(id = id, front = "Q$id", back = "A$id")

    private fun pack(vararg ids: String) = FlashCardPack(
        id = "pack1",
        name = "Test Pack",
        description = "",
        schoolLevel = SchoolLevel.HIGH_SCHOOL,
        subject = SubjectTag.OTHER,
        frontLabel = "Question",
        backLabel = "Answer",
        cards = ids.map { card(it) }
    )

    private fun settings(
        mode: FlashCardMode = FlashCardMode.QUIZ,
        duration: FlashCardDuration = FlashCardDuration.FullDeck
    ) = FlashCardSettings(
        packId = "pack1",
        showSide = ShowSide.FRONT,
        duration = duration,
        mode = mode
    )

    @Test
    fun startSession_quiz_buildsFullDeckSequence() {
        val p = pack("a", "b", "c")
        val session = controller.startSession(p, settings(FlashCardMode.QUIZ), emptyMap(), 0L)
        assertEquals(listOf("a", "b", "c"), session.cards.map { it.id })
    }

    @Test
    fun drawNextCard_quiz_advancesIndex() {
        val p = pack("a", "b", "c")
        val session = controller.startSession(p, settings(FlashCardMode.QUIZ), emptyMap(), 0L)
        val (updated, card) = controller.drawNextCard(session, emptyMap())
        val recorded = controller.recordResult(updated, card!!.id, correct = true)
        assertEquals(1, recorded.currentIndex)
    }

    @Test
    fun recordResult_correct_incrementsCorrectCount() {
        val progress = controller.updateProgress(null, "a", correct = true, nowMs = 0L)
        assertEquals(1, progress.correctCount)
    }

    @Test
    fun focusedWeight_neverAttempted_returnsOne() {
        val weight = controller.focusedWeight(null)
        assertEquals(1.0f, weight, 0.001f)
    }

    @Test
    fun focusedWeight_allCorrect_returnsMinimum() {
        val progress = FlashCardProgress(
            cardId = "a", totalShown = 10, correctCount = 10, incorrectCount = 0,
            lastShownTimestamp = 0L
        )
        val weight = controller.focusedWeight(progress)
        assertEquals(0.05f, weight, 0.001f)
    }

    @Test
    fun focusedWeight_halfCorrect_returnsHalf() {
        val progress = FlashCardProgress(
            cardId = "a", totalShown = 10, correctCount = 5, incorrectCount = 5,
            lastShownTimestamp = 0L
        )
        val weight = controller.focusedWeight(progress)
        assertEquals(0.5f, weight, 0.001f)
    }

    @Test
    fun isSessionComplete_fullDeck_whenIndexEqualsSize() {
        val p = pack("a", "b")
        val session = controller.startSession(p, settings(FlashCardMode.QUIZ, FlashCardDuration.FullDeck), emptyMap(), 0L)
            .copy(currentIndex = 2)
        assertTrue(controller.isSessionComplete(session, elapsedMs = 0L))
    }

    @Test
    fun isSessionComplete_numberOfCards_whenIndexReachesLimit() {
        val p = pack("a", "b", "c", "d")
        val session = controller.startSession(
            p, settings(FlashCardMode.QUIZ, FlashCardDuration.NumberOfCards(3)), emptyMap(), 0L
        ).copy(currentIndex = 3)
        assertTrue(controller.isSessionComplete(session, elapsedMs = 0L))
    }

    @Test
    fun isSessionComplete_numberOfMinutes_whenElapsedExceedsLimit() {
        val p = pack("a")
        val session = controller.startSession(
            p, settings(FlashCardMode.QUIZ, FlashCardDuration.NumberOfMinutes(1)), emptyMap(), 0L
        )
        assertFalse(controller.isSessionComplete(session, elapsedMs = 30_000L))
        assertTrue(controller.isSessionComplete(session, elapsedMs = 60_000L))
    }
}
