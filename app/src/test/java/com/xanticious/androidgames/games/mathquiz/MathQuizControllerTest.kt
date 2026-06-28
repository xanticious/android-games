package com.xanticious.androidgames.games.mathquiz

import com.xanticious.androidgames.controller.games.mathquiz.MathQuizController
import com.xanticious.androidgames.model.games.mathquiz.MathDifficulty
import com.xanticious.androidgames.model.games.mathquiz.MathOperation
import com.xanticious.androidgames.model.games.mathquiz.MathQuizSession
import com.xanticious.androidgames.model.games.mathquiz.MathQuizSettings
import com.xanticious.androidgames.model.games.mathquiz.MathTimingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathQuizControllerTest {
    private val controller = MathQuizController()

    private fun easyAddSettings() = MathQuizSettings(
        operations = setOf(MathOperation.ADDITION),
        difficulties = setOf(MathDifficulty.EASY),
        timingMode = MathTimingMode.UNTIMED
    )

    private fun mediumMultSettings() = MathQuizSettings(
        operations = setOf(MathOperation.MULTIPLICATION),
        difficulties = setOf(MathDifficulty.MEDIUM),
        timingMode = MathTimingMode.UNTIMED
    )

    private fun easyDivisionSettings() = MathQuizSettings(
        operations = setOf(MathOperation.DIVISION),
        difficulties = setOf(MathDifficulty.EASY),
        timingMode = MathTimingMode.UNTIMED
    )

    private fun session(
        settings: MathQuizSettings = easyAddSettings(),
        currentIndex: Int = 0,
        correctCount: Int = 0,
        totalScore: Int = 0
    ) = MathQuizSession(
        settings = settings,
        questions = emptyList(),
        currentIndex = currentIndex,
        correctCount = correctCount,
        totalScore = totalScore,
        startTimeMs = 0L
    )

    // ── generateQuestion ─────────────────────────────────────────────────────

    @Test
    fun generateQuestion_addition_easy_hasSmallOperands() {
        val q = controller.generateQuestion(easyAddSettings(), kotlin.random.Random(42))
        val parts = q.expression.split(" + ").map { it.trim().toInt() }
        assertTrue(parts.all { it in 1..10 })
    }

    @Test
    fun generateQuestion_multiplication_medium_usesTablesUpTo12() {
        val q = controller.generateQuestion(mediumMultSettings(), kotlin.random.Random(7))
        val parts = q.expression.split(" \u00D7 ").map { it.trim().toInt() }
        assertTrue(parts.all { it in 1..12 })
    }

    // ── checkAnswer ──────────────────────────────────────────────────────────

    @Test
    fun checkAnswer_correct_awards10Points() {
        val q = controller.generateQuestion(easyAddSettings(), kotlin.random.Random(1))
        val result = controller.checkAnswer(q, q.correctAnswer, answerTimeMs = 10_000L, isFirstAttempt = false)
        assertEquals(10, result.pointsAwarded)
    }

    @Test
    fun checkAnswer_incorrect_awards0Points() {
        val q = controller.generateQuestion(easyAddSettings(), kotlin.random.Random(1))
        val result = controller.checkAnswer(q, "9999", answerTimeMs = 1_000L, isFirstAttempt = true)
        assertEquals(0, result.pointsAwarded)
    }

    @Test
    fun checkAnswer_correct_fastAnswer_awardsSpeedBonus() {
        val q = controller.generateQuestion(easyAddSettings(), kotlin.random.Random(1))
        // First attempt + within 1.5 s → 10 + 5 + 10 = 25
        val result = controller.checkAnswer(q, q.correctAnswer, answerTimeMs = 1_000L, isFirstAttempt = true)
        assertEquals(25, result.pointsAwarded)
    }

    @Test
    fun checkAnswer_division_noRemainder_checksIntegerAnswer() {
        val q = controller.generateQuestion(easyDivisionSettings(), kotlin.random.Random(3))
        val result = controller.checkAnswer(q, q.correctAnswer, answerTimeMs = 5_000L, isFirstAttempt = true)
        assertTrue(result.isCorrect)
    }

    // ── isSessionComplete ────────────────────────────────────────────────────

    @Test
    fun isSessionComplete_questionCountReached_returnsTrue() {
        val settings = easyAddSettings().copy(questionCount = 5)
        val s = session(settings = settings, currentIndex = 5)
        assertTrue(controller.isSessionComplete(s))
    }

    @Test
    fun isSessionComplete_notReached_returnsFalse() {
        val settings = easyAddSettings().copy(questionCount = 5)
        val s = session(settings = settings, currentIndex = 3)
        assertFalse(controller.isSessionComplete(s))
    }

    // ── recordAnswer ─────────────────────────────────────────────────────────

    @Test
    fun recordAnswer_correct_incrementsCorrectCount() {
        val q = controller.generateQuestion(easyAddSettings(), kotlin.random.Random(1))
        val result = controller.checkAnswer(q, q.correctAnswer, 5_000L, true)
        val updated = controller.recordAnswer(session(), result)
        assertEquals(1, updated.correctCount)
    }

    @Test
    fun recordAnswer_updates_totalScore() {
        val q = controller.generateQuestion(easyAddSettings(), kotlin.random.Random(1))
        val result = controller.checkAnswer(q, q.correctAnswer, 5_000L, true)
        val updated = controller.recordAnswer(session(totalScore = 10), result)
        assertEquals(10 + result.pointsAwarded, updated.totalScore)
    }
}
