package com.xanticious.androidgames.controller.games.flashcards

import com.xanticious.androidgames.model.games.flashcards.FlashCard
import com.xanticious.androidgames.model.games.flashcards.FlashCardDuration
import com.xanticious.androidgames.model.games.flashcards.FlashCardMode
import com.xanticious.androidgames.model.games.flashcards.FlashCardPack
import com.xanticious.androidgames.model.games.flashcards.FlashCardProgress
import com.xanticious.androidgames.model.games.flashcards.FlashCardSession
import com.xanticious.androidgames.model.games.flashcards.FlashCardSettings

/**
 * Pure Flash Cards rules: session building, card drawing, and progress tracking.
 *
 * This class has no Android or Compose imports and is fully JVM unit-testable. It is
 * responsible for translating [FlashCardSettings] and [FlashCardPack] data into a running
 * [FlashCardSession], and for advancing that session as the player interacts with each card.
 */
class FlashCardController {

    /**
     * Builds an initial [FlashCardSession] from the pack and settings.
     *
     * - QUIZ: full deck in original order
     * - SHUFFLED: full deck in shuffled order
     * - RANDOM / FOCUSED: pool is the full deck; individual draws are handled by [drawNextCard]
     */
    fun startSession(
        pack: FlashCardPack,
        settings: FlashCardSettings,
        progressMap: Map<String, FlashCardProgress>,
        nowMs: Long
    ): FlashCardSession {
        val cards = when (settings.mode) {
            FlashCardMode.QUIZ -> pack.cards.toList()
            FlashCardMode.SHUFFLED -> pack.cards.shuffled()
            FlashCardMode.RANDOM, FlashCardMode.FOCUSED -> pack.cards.toList()
        }
        return FlashCardSession(
            settings = settings,
            cards = cards,
            currentIndex = 0,
            correctCardIds = emptyList(),
            incorrectCardIds = emptyList(),
            startTimeMs = nowMs,
            isComplete = false
        )
    }

    /**
     * Returns the updated session and the next [FlashCard] to show, or `null` when the session
     * is exhausted.
     *
     * - QUIZ / SHUFFLED: iterate sequentially through [FlashCardSession.cards]
     * - RANDOM: uniform random pick from the full pool, excluding the card at the previous index
     * - FOCUSED: weighted random pick (see [focusedWeight]), excluding the most-recently-shown card
     */
    fun drawNextCard(
        session: FlashCardSession,
        progressMap: Map<String, FlashCardProgress>,
        random: kotlin.random.Random = kotlin.random.Random.Default
    ): Pair<FlashCardSession, FlashCard?> {
        if (session.isComplete) return session to null

        return when (session.settings.mode) {
            FlashCardMode.QUIZ, FlashCardMode.SHUFFLED -> {
                val card = session.cards.getOrNull(session.currentIndex)
                session to card
            }
            FlashCardMode.RANDOM -> {
                val pool = eligiblePool(session)
                if (pool.isEmpty()) return session.copy(isComplete = true) to null
                val card = pool[random.nextInt(pool.size)]
                session to card
            }
            FlashCardMode.FOCUSED -> {
                val pool = eligiblePool(session)
                if (pool.isEmpty()) return session.copy(isComplete = true) to null
                val card = weightedPick(pool, progressMap, random)
                session to card
            }
        }
    }

    /**
     * Records whether the player got the card right or wrong, advances the index, and marks the
     * session complete when the deck is exhausted (for QUIZ/SHUFFLED) or the duration is met.
     */
    fun recordResult(
        session: FlashCardSession,
        cardId: String,
        correct: Boolean
    ): FlashCardSession {
        val correctIds = if (correct) session.correctCardIds + cardId else session.correctCardIds
        val incorrectIds = if (!correct) session.incorrectCardIds + cardId else session.incorrectCardIds
        return session.copy(
            currentIndex = session.currentIndex + 1,
            correctCardIds = correctIds,
            incorrectCardIds = incorrectIds
        )
    }

    /**
     * Creates or updates a [FlashCardProgress] entry for a single card.
     */
    fun updateProgress(
        progress: FlashCardProgress?,
        cardId: String,
        correct: Boolean,
        nowMs: Long
    ): FlashCardProgress {
        val base = progress ?: FlashCardProgress(
            cardId = cardId,
            totalShown = 0,
            correctCount = 0,
            incorrectCount = 0,
            lastShownTimestamp = 0L
        )
        return base.copy(
            totalShown = base.totalShown + 1,
            correctCount = if (correct) base.correctCount + 1 else base.correctCount,
            incorrectCount = if (!correct) base.incorrectCount + 1 else base.incorrectCount,
            lastShownTimestamp = nowMs
        )
    }

    /**
     * Weight for FOCUSED mode:
     * - 1.0 if the card has never been shown
     * - Otherwise max(0.05, incorrectCount / totalShown)
     */
    fun focusedWeight(progress: FlashCardProgress?): Float {
        if (progress == null || progress.totalShown == 0) return 1.0f
        return maxOf(0.05f, progress.incorrectCount.toFloat() / progress.totalShown.toFloat())
    }

    /**
     * Returns true when the session should end, based on the configured [FlashCardDuration].
     */
    fun isSessionComplete(session: FlashCardSession, elapsedMs: Long): Boolean {
        return when (val dur = session.settings.duration) {
            is FlashCardDuration.FullDeck -> session.currentIndex >= session.cards.size
            is FlashCardDuration.NumberOfCards -> session.currentIndex >= dur.n
            is FlashCardDuration.NumberOfMinutes -> elapsedMs >= dur.m * 60_000L
        }
    }

    // --- private helpers ---

    /** Cards eligible for the next RANDOM/FOCUSED draw (all cards except the most-recently-shown). */
    private fun eligiblePool(session: FlashCardSession): List<FlashCard> {
        val lastShownId = if (session.currentIndex > 0) {
            val idx = (session.currentIndex - 1) % session.cards.size
            session.cards.getOrNull(idx)?.id
        } else null
        return if (lastShownId != null) session.cards.filter { it.id != lastShownId }
        else session.cards.toList()
    }

    /** Picks one card from [pool] with probability proportional to [focusedWeight]. */
    private fun weightedPick(
        pool: List<FlashCard>,
        progressMap: Map<String, FlashCardProgress>,
        random: kotlin.random.Random
    ): FlashCard {
        val weights = pool.map { focusedWeight(progressMap[it.id]) }
        val total = weights.sum()
        var pick = random.nextFloat() * total
        for ((index, card) in pool.withIndex()) {
            pick -= weights[index]
            if (pick <= 0f) return card
        }
        return pool.last()
    }
}
