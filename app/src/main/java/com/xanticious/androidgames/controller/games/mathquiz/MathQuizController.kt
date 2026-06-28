package com.xanticious.androidgames.controller.games.mathquiz

import com.xanticious.androidgames.model.games.mathquiz.MathAnswerResult
import com.xanticious.androidgames.model.games.mathquiz.MathDifficulty
import com.xanticious.androidgames.model.games.mathquiz.MathOperation
import com.xanticious.androidgames.model.games.mathquiz.MathQuestion
import com.xanticious.androidgames.model.games.mathquiz.MathQuizConfig
import com.xanticious.androidgames.model.games.mathquiz.MathQuizSession
import com.xanticious.androidgames.model.games.mathquiz.MathQuizSettings
import com.xanticious.androidgames.model.games.mathquiz.MathTimingMode

/**
 * Pure Math Quiz rules. No Android or Compose imports — fully JVM unit-testable.
 */
class MathQuizController {

    /**
     * Picks a random active operation + difficulty combo from [settings] and
     * generates a matching question.
     */
    fun generateQuestion(
        settings: MathQuizSettings,
        random: kotlin.random.Random = kotlin.random.Random
    ): MathQuestion {
        val combos = settings.operations.flatMap { op ->
            settings.difficulties.map { diff -> op to diff }
        }
        val (operation, difficulty) = combos[random.nextInt(combos.size)]
        return when (operation) {
            MathOperation.ADDITION, MathOperation.SUBTRACTION ->
                generateAddSubQuestion(operation, difficulty, random)
            MathOperation.MULTIPLICATION ->
                generateMultiplicationQuestion(difficulty, random)
            MathOperation.DIVISION ->
                generateDivisionQuestion(difficulty, random)
        }
    }

    /**
     * Validates [userAnswer] against [question], applies speed bonuses, and
     * returns a [MathAnswerResult].
     *
     * Scoring:
     * - Correct: 10 pts base
     * - First attempt correct: +5 bonus
     * - Answer within 1.5 s: +10 speed bonus (replaces 3-second bonus)
     * - Answer within 3 s: +5 speed bonus
     */
    fun checkAnswer(
        question: MathQuestion,
        userAnswer: String,
        answerTimeMs: Long,
        isFirstAttempt: Boolean
    ): MathAnswerResult {
        val normalised = userAnswer.trim()
        val correct = normalised.equals(question.correctAnswer, ignoreCase = true)
        if (!correct) {
            return MathAnswerResult(
                isCorrect = false,
                pointsAwarded = 0,
                correctAnswer = question.correctAnswer
            )
        }
        var points = 10
        if (isFirstAttempt) points += 5
        points += when {
            answerTimeMs <= 1_500L -> 10
            answerTimeMs <= 3_000L -> 5
            else -> 0
        }
        return MathAnswerResult(
            isCorrect = true,
            pointsAwarded = points,
            correctAnswer = question.correctAnswer
        )
    }

    /**
     * Returns `true` when the session has reached its target question count
     * (UNTIMED / STOPWATCH). COUNTDOWN expiry is tracked externally via
     * [MathQuizSession.elapsedMs].
     */
    fun isSessionComplete(session: MathQuizSession): Boolean {
        return when (session.settings.timingMode) {
            MathTimingMode.UNTIMED, MathTimingMode.STOPWATCH ->
                session.currentIndex >= session.settings.questionCount
            MathTimingMode.COUNTDOWN ->
                session.elapsedMs >= session.settings.countdownMinutes * 60_000L
        }
    }

    /**
     * Returns an updated [MathQuizSession] reflecting [result]: increments the
     * question index, correct count (if applicable), and total score.
     */
    fun recordAnswer(session: MathQuizSession, result: MathAnswerResult): MathQuizSession {
        val newCorrectCount = if (result.isCorrect) session.correctCount + 1 else session.correctCount
        return session.copy(
            currentIndex = session.currentIndex + 1,
            correctCount = newCorrectCount,
            totalScore = session.totalScore + result.pointsAwarded
        )
    }

    /** Returns a [MathQuizConfig] derived from [settings]. */
    fun configFor(settings: MathQuizSettings): MathQuizConfig = MathQuizConfig(
        maxQuestions = settings.questionCount,
        countdownMs = settings.countdownMinutes * 60_000L
    )

    // ── Private generation helpers ────────────────────────────────────────────

    private fun generateAddSubQuestion(
        operation: MathOperation,
        difficulty: MathDifficulty,
        random: kotlin.random.Random
    ): MathQuestion {
        val canBeNegative = difficulty == MathDifficulty.HARD || difficulty == MathDifficulty.EXPERT
        val useThreeOperands = difficulty == MathDifficulty.EXPERT && random.nextBoolean()

        val (a, b, c) = when (difficulty) {
            MathDifficulty.EASY -> Triple(random.nextInt(1, 11), random.nextInt(1, 11), null)
            MathDifficulty.MEDIUM -> Triple(random.nextInt(10, 100), random.nextInt(10, 100), null)
            MathDifficulty.HARD -> Triple(random.nextInt(100, 1000), random.nextInt(100, 1000), null)
            MathDifficulty.EXPERT -> {
                val x = random.nextInt(1000, 10000)
                val y = random.nextInt(1000, 10000)
                val z = if (useThreeOperands) random.nextInt(1000, 10000) else null
                Triple(x, y, z)
            }
        }

        // For EASY/MEDIUM enforce non-negative results by swapping so a >= b
        val (first, second) = if (!canBeNegative && operation == MathOperation.SUBTRACTION) {
            if (a >= b) a to b else b to a
        } else {
            a to b
        }

        val answer: Int
        val expression: String
        if (c != null && operation == MathOperation.ADDITION) {
            answer = first + second + c
            expression = "$first + $second + $c"
        } else if (c != null && operation == MathOperation.SUBTRACTION) {
            answer = first - second - c
            expression = "$first \u2212 $second \u2212 $c"
        } else {
            answer = if (operation == MathOperation.ADDITION) first + second else first - second
            val opSymbol = if (operation == MathOperation.ADDITION) "+" else "\u2212"
            expression = "$first $opSymbol $second"
        }

        // For EASY addition cap sum at 20 by clamping operands
        val (finalExpression, finalAnswer) = if (
            difficulty == MathDifficulty.EASY &&
            operation == MathOperation.ADDITION &&
            answer > 20
        ) {
            val safeA = random.nextInt(1, 11)
            val safeB = random.nextInt(1, 21 - safeA)
            "$safeA + $safeB" to (safeA + safeB)
        } else {
            expression to answer
        }

        // For MEDIUM addition cap sum at 200
        val (finalExpression2, finalAnswer2) = if (
            difficulty == MathDifficulty.MEDIUM &&
            operation == MathOperation.ADDITION &&
            finalAnswer > 200
        ) {
            val safeA = random.nextInt(10, 100)
            val safeB = random.nextInt(10, 201 - safeA)
            "$safeA + $safeB" to (safeA + safeB)
        } else {
            finalExpression to finalAnswer
        }

        return MathQuestion(
            id = "q_${System.nanoTime()}",
            expression = finalExpression2,
            correctAnswer = finalAnswer2.toString(),
            operation = operation,
            difficulty = difficulty,
            canHaveNegativeAnswer = canBeNegative,
            canHaveFractionalAnswer = false
        )
    }

    private fun generateMultiplicationQuestion(
        difficulty: MathDifficulty,
        random: kotlin.random.Random
    ): MathQuestion {
        val (a, b) = when (difficulty) {
            MathDifficulty.EASY -> random.nextInt(1, 6) to random.nextInt(1, 6)
            MathDifficulty.MEDIUM -> random.nextInt(1, 13) to random.nextInt(1, 13)
            MathDifficulty.HARD -> random.nextInt(1, 26) to random.nextInt(1, 26)
            MathDifficulty.EXPERT -> random.nextInt(1, 51) to random.nextInt(1, 51)
        }
        return MathQuestion(
            id = "q_${System.nanoTime()}",
            expression = "$a \u00D7 $b",
            correctAnswer = (a * b).toString(),
            operation = MathOperation.MULTIPLICATION,
            difficulty = difficulty,
            canHaveNegativeAnswer = false,
            canHaveFractionalAnswer = false
        )
    }

    private fun generateDivisionQuestion(
        difficulty: MathDifficulty,
        random: kotlin.random.Random
    ): MathQuestion {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                // a ÷ b with no remainder: b ∈ 1..5, quotient ∈ 1..5
                val b = random.nextInt(1, 6)
                val quotient = random.nextInt(1, 6)
                val a = b * quotient
                MathQuestion(
                    id = "q_${System.nanoTime()}",
                    expression = "$a \u00F7 $b",
                    correctAnswer = quotient.toString(),
                    operation = MathOperation.DIVISION,
                    difficulty = difficulty,
                    canHaveNegativeAnswer = false,
                    canHaveFractionalAnswer = false
                )
            }
            MathDifficulty.MEDIUM -> {
                // a ÷ b with no remainder: b ∈ 1..12, quotient ∈ 1..12
                val b = random.nextInt(1, 13)
                val quotient = random.nextInt(1, 13)
                val a = b * quotient
                MathQuestion(
                    id = "q_${System.nanoTime()}",
                    expression = "$a \u00F7 $b",
                    correctAnswer = quotient.toString(),
                    operation = MathOperation.DIVISION,
                    difficulty = difficulty,
                    canHaveNegativeAnswer = false,
                    canHaveFractionalAnswer = false
                )
            }
            MathDifficulty.HARD -> {
                // may have remainder; b ∈ 1..25, a ∈ 1..625
                val b = random.nextInt(1, 26)
                val a = random.nextInt(1, b * 25 + 1)
                val quotient = a / b
                val remainder = a % b
                val answer = if (remainder > 0) "$quotient R $remainder" else quotient.toString()
                MathQuestion(
                    id = "q_${System.nanoTime()}",
                    expression = "$a \u00F7 $b",
                    correctAnswer = answer,
                    operation = MathOperation.DIVISION,
                    difficulty = difficulty,
                    canHaveNegativeAnswer = false,
                    canHaveFractionalAnswer = remainder > 0
                )
            }
            MathDifficulty.EXPERT -> {
                // 2-digit divisor; may have remainder
                val b = random.nextInt(10, 100)
                val a = random.nextInt(b, b * 100 + 1)
                val quotient = a / b
                val remainder = a % b
                val answer = if (remainder > 0) "$quotient R $remainder" else quotient.toString()
                MathQuestion(
                    id = "q_${System.nanoTime()}",
                    expression = "$a \u00F7 $b",
                    correctAnswer = answer,
                    operation = MathOperation.DIVISION,
                    difficulty = difficulty,
                    canHaveNegativeAnswer = false,
                    canHaveFractionalAnswer = remainder > 0
                )
            }
        }
    }
}
