package com.xanticious.androidgames.model.games.flashcards

enum class SchoolLevel { ELEMENTARY, MIDDLE_SCHOOL, HIGH_SCHOOL, COLLEGE }

enum class SubjectTag {
    GEOGRAPHY, MATH, SCIENCE, LANGUAGE, HISTORY, VOCABULARY, OTHER,
    BIOLOGY, CHEMISTRY, PSYCHOLOGY, ECONOMICS, COMPUTER_SCIENCE, PHILOSOPHY,
    ASTRONOMY, ANTHROPOLOGY, ANATOMY, ZOOLOGY, AGRICULTURE, COMMUNICATIONS
}

enum class FlashCardMode { QUIZ, SHUFFLED, RANDOM, FOCUSED }

enum class ShowSide { FRONT, BACK, RANDOM }

/**
 * Controls how long a Flash Cards session runs.
 *
 * - [FullDeck]: intended only for [FlashCardMode.QUIZ]; the session ends after every card in the
 *   pack has been shown exactly once.
 * - [NumberOfCards]: the session ends after [n] cards have been drawn, regardless of mode.
 * - [NumberOfMinutes]: the session ends once [m] minutes have elapsed, regardless of mode.
 */
sealed class FlashCardDuration {
    data object FullDeck : FlashCardDuration()
    data class NumberOfCards(val n: Int) : FlashCardDuration()
    data class NumberOfMinutes(val m: Int) : FlashCardDuration()
}

data class FlashCard(
    val id: String,
    val front: String,
    val back: String
)

data class FlashCardPack(
    val id: String,
    val name: String,
    val description: String,
    val schoolLevel: SchoolLevel,
    val subject: SubjectTag,
    val frontLabel: String,
    val backLabel: String,
    val cards: List<FlashCard>
)

data class FlashCardSettings(
    val packId: String,
    val showSide: ShowSide,
    val duration: FlashCardDuration,
    val mode: FlashCardMode
)

data class FlashCardProgress(
    val cardId: String,
    val totalShown: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val lastShownTimestamp: Long
)

data class FlashCardSession(
    val settings: FlashCardSettings,
    /** Ordered sequence of cards for this session (pre-built for QUIZ/SHUFFLED). */
    val cards: List<FlashCard>,
    val currentIndex: Int,
    val correctCardIds: List<String>,
    val incorrectCardIds: List<String>,
    val startTimeMs: Long,
    val isComplete: Boolean
)
