package com.xanticious.androidgames.model.games.mathquiz

import com.xanticious.androidgames.model.games.mathquiz.MathDifficulty.EASY
import com.xanticious.androidgames.model.games.mathquiz.MathDifficulty.MEDIUM
import com.xanticious.androidgames.model.games.mathquiz.MathOperation.ADDITION
import com.xanticious.androidgames.model.games.mathquiz.MathOperation.SUBTRACTION

enum class MathOperation { ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION }

enum class MathDifficulty { EASY, MEDIUM, HARD, EXPERT }

enum class MathTimingMode { UNTIMED, STOPWATCH, COUNTDOWN }

data class MathQuestion(
    val id: String,
    val expression: String,
    val correctAnswer: String,
    val operation: MathOperation,
    val difficulty: MathDifficulty,
    val canHaveNegativeAnswer: Boolean,
    val canHaveFractionalAnswer: Boolean
)

data class MathQuizSettings(
    val operations: Set<MathOperation>,
    val difficulties: Set<MathDifficulty>,
    val timingMode: MathTimingMode,
    val questionCount: Int = 20,
    val countdownMinutes: Int = 5
) {
    companion object {
        fun default() = MathQuizSettings(
            operations = setOf(ADDITION, SUBTRACTION),
            difficulties = setOf(EASY, MEDIUM),
            timingMode = MathTimingMode.UNTIMED
        )
    }
}

data class MathQuizSession(
    val settings: MathQuizSettings,
    val questions: List<MathQuestion>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val totalScore: Int = 0,
    val startTimeMs: Long,
    val elapsedMs: Long = 0,
    val isComplete: Boolean = false
)

data class MathAnswerResult(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val correctAnswer: String
)

data class MathQuizConfig(
    val maxQuestions: Int,
    val countdownMs: Long
)
