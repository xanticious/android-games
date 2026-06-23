package com.xanticious.androidgames.controller.games.solitairetripeaks

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.deal
import com.xanticious.androidgames.model.games.solitairetripeaks.BoardCard
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksBoard
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksConfig
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksVariant
import kotlin.random.Random

/**
 * Pure controller for TriPeaks Solitaire (both CLASSIC and TIMED variants).
 *
 * Every function is a pure transformation: it takes the current [TriPeaksBoard]
 * and returns a new one. No Android, Compose, or coroutine imports — fully
 * JVM-testable.
 *
 * ## Board layout (28 positions)
 * ```
 * Row 0 (peak tips):   [0]        [1]        [2]
 * Row 1:             [3][4]     [5][6]     [7][8]
 * Row 2:           [9][10][11][12][13][14][15][16][17]
 * Row 3 (base):  [18][19][20][21][22][23][24][25][26][27]
 * ```
 * A card is **exposed** (playable) when every card that covers it has been removed.
 * Base-row cards (18–27) are always exposed at deal time.
 */
class TriPeaksController {

    // -------------------------------------------------------------------------
    // Static coverage table: COVERERS[i] lists positions that must be removed
    // before position i becomes exposed.
    // -------------------------------------------------------------------------
    private val coverers: Array<IntArray> = arrayOf(
        // Row 0 — covered by Row 1
        intArrayOf(3, 4),   // 0
        intArrayOf(5, 6),   // 1
        intArrayOf(7, 8),   // 2
        // Row 1 — covered by Row 2
        intArrayOf(9, 10),  // 3
        intArrayOf(10, 11), // 4
        intArrayOf(12, 13), // 5
        intArrayOf(13, 14), // 6
        intArrayOf(15, 16), // 7
        intArrayOf(16, 17), // 8
        // Row 2 — covered by Row 3 (base)
        intArrayOf(18, 19), // 9
        intArrayOf(19, 20), // 10
        intArrayOf(20, 21), // 11
        intArrayOf(21, 22), // 12
        intArrayOf(22, 23), // 13
        intArrayOf(23, 24), // 14
        intArrayOf(24, 25), // 15
        intArrayOf(25, 26), // 16
        intArrayOf(26, 27), // 17
        // Row 3 (base) — nothing covers these
        intArrayOf(), // 18
        intArrayOf(), // 19
        intArrayOf(), // 20
        intArrayOf(), // 21
        intArrayOf(), // 22
        intArrayOf(), // 23
        intArrayOf(), // 24
        intArrayOf(), // 25
        intArrayOf(), // 26
        intArrayOf(), // 27
    )

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    fun configFor(variant: TriPeaksVariant, difficulty: GameDifficulty): TriPeaksConfig =
        when (variant) {
            TriPeaksVariant.CLASSIC -> when (difficulty) {
                GameDifficulty.EASY -> TriPeaksConfig(
                    variant = variant,
                    rankWrap = true,
                    basePointsPerCard = 100,
                    stockBonusPerCard = 50,
                )
                GameDifficulty.MEDIUM -> TriPeaksConfig(
                    variant = variant,
                    rankWrap = true,
                    basePointsPerCard = 100,
                    stockBonusPerCard = 50,
                )
                GameDifficulty.HARD -> TriPeaksConfig(
                    variant = variant,
                    rankWrap = false, // Ace does NOT wrap to King on hard
                    basePointsPerCard = 100,
                    stockBonusPerCard = 25,
                )
            }
            TriPeaksVariant.TIMED -> when (difficulty) {
                GameDifficulty.EASY -> TriPeaksConfig(
                    variant = variant,
                    rankWrap = true,
                    timerStartSeconds = 90f,
                    timerDrainRate = 0.8f,
                    timePerClear = 3.0f,
                    timePerComboStep = 0.75f,
                )
                GameDifficulty.MEDIUM -> TriPeaksConfig(
                    variant = variant,
                    rankWrap = true,
                    timerStartSeconds = 60f,
                    timerDrainRate = 1.0f,
                    timePerClear = 2.0f,
                    timePerComboStep = 0.5f,
                )
                GameDifficulty.HARD -> TriPeaksConfig(
                    variant = variant,
                    rankWrap = true,
                    timerStartSeconds = 45f,
                    timerDrainRate = 1.2f,
                    timePerClear = 1.5f,
                    timePerComboStep = 0.3f,
                )
            }
        }

    // -------------------------------------------------------------------------
    // Deal
    // -------------------------------------------------------------------------

    /** Deals a fresh board from [seed]. The first 28 shuffled cards go face-down
     *  to the peaks; one additional card is flipped face-up to start the waste
     *  pile; the remaining cards form the stock face-down. */
    fun deal(seed: Long, config: TriPeaksConfig): TriPeaksBoard =
        deal(Random(seed), config)

    fun deal(random: Random, config: TriPeaksConfig): TriPeaksBoard {
        val deck = Decks.shuffled(random)
        val (boardRaw, afterBoard) = deck.deal(28)
        val (wasteStart, stock) = afterBoard.deal(1)

        val boardCards = boardRaw.mapIndexed { index, card ->
            // Base row (18–27) start face-up; the rest start face-down.
            val faceUp = index >= 18
            BoardCard(card = card.copy(faceUp = faceUp), position = index)
        }

        return TriPeaksBoard(
            boardCards = boardCards,
            stock = stock.map { it.faceDown() },
            waste = wasteStart.map { it.faceUp() },
            score = 0,
            combo = 0,
            config = config,
            timerSeconds = if (config.variant == TriPeaksVariant.TIMED) config.timerStartSeconds else 0f,
        )
    }

    // -------------------------------------------------------------------------
    // Exposure
    // -------------------------------------------------------------------------

    /** Returns true if the card at [position] is currently exposed (playable). */
    fun isExposed(board: TriPeaksBoard, position: Int): Boolean {
        if (position < 0 || position >= 28) return false
        val removed = board.boardCards[position].removed
        if (removed) return false
        return coverers[position].all { coverPos -> board.boardCards[coverPos].removed }
    }

    // -------------------------------------------------------------------------
    // Rank adjacency
    // -------------------------------------------------------------------------

    /**
     * Returns true if [cardRank] is exactly one step above or below [wasteRank].
     * With [rankWrap] enabled, Ace neighbours King and Two.
     */
    fun isAdjacentRank(cardRank: Rank, wasteRank: Rank, rankWrap: Boolean): Boolean {
        val cv = cardRank.value
        val wv = wasteRank.value
        if (kotlin.math.abs(cv - wv) == 1) return true
        if (rankWrap) {
            // Ace (1) wraps with King (13)
            if (cv == 1 && wv == 13) return true
            if (cv == 13 && wv == 1) return true
        }
        return false
    }

    // -------------------------------------------------------------------------
    // Move validation
    // -------------------------------------------------------------------------

    /** Returns true if the card at [position] can legally be played onto the waste. */
    fun canPlay(board: TriPeaksBoard, position: Int): Boolean {
        val wasteTop = board.wasteTop ?: return false
        if (!isExposed(board, position)) return false
        val boardCard = board.boardCards[position]
        return isAdjacentRank(boardCard.card.rank, wasteTop.rank, board.config.rankWrap)
    }

    /** Returns true if any board card can currently be played. */
    fun hasAnyMove(board: TriPeaksBoard): Boolean =
        board.boardCards.indices.any { canPlay(board, it) }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Plays the card at [position] onto the waste, extending the combo chain
     * and adding to the score. Throws if the move is not legal.
     */
    fun playCard(board: TriPeaksBoard, position: Int): TriPeaksBoard {
        require(canPlay(board, position)) {
            "Position $position is not a legal play (waste=${board.wasteTop?.rank}, exposed=${isExposed(board, position)})"
        }
        val newCombo = board.combo + 1
        val pointsEarned = board.config.basePointsPerCard * newCombo
        val playedCard = board.boardCards[position].card.faceUp()

        // Flip face-up any newly exposed cards after this removal.
        val updatedBoard = board.boardCards.map { bc ->
            when {
                bc.position == position -> bc.copy(removed = true)
                // A card becomes face-up once it's exposed (coverers removed).
                !bc.removed && !bc.card.faceUp -> {
                    val nowExposed = coverers[bc.position].all { cp ->
                        if (cp == position) true else board.boardCards[cp].removed
                    }
                    if (nowExposed) bc.copy(card = bc.card.faceUp()) else bc
                }
                else -> bc
            }
        }

        return board.copy(
            boardCards = updatedBoard,
            waste = board.waste + playedCard,
            score = board.score + pointsEarned,
            combo = newCombo,
            timerSeconds = timerSecondsAfterClear(board, newCombo),
        )
    }

    /** Draws the top card from the stock and places it face-up on the waste, resetting the combo. */
    fun draw(board: TriPeaksBoard): TriPeaksBoard {
        if (board.stock.isEmpty()) return board
        val drawn = board.stock.first().faceUp()
        return board.copy(
            stock = board.stock.drop(1),
            waste = board.waste + drawn,
            combo = 0,
        )
    }

    // -------------------------------------------------------------------------
    // Win / lose
    // -------------------------------------------------------------------------

    /** All 28 board cards cleared → win. */
    fun isWon(board: TriPeaksBoard): Boolean =
        board.boardCards.all { it.removed }

    /**
     * Lost when: no legal moves AND stock is empty.
     * For TIMED: also lost if timer has expired.
     */
    fun isLost(board: TriPeaksBoard): Boolean {
        if (board.config.variant == TriPeaksVariant.TIMED && board.timerSeconds <= 0f) return true
        return board.stock.isEmpty() && !hasAnyMove(board)
    }

    /**
     * Calculates final win bonus: remaining stock cards × config bonus per card.
     */
    fun winBonus(board: TriPeaksBoard): Int = board.stock.size * board.config.stockBonusPerCard

    // -------------------------------------------------------------------------
    // Timer (TIMED variant only)
    // -------------------------------------------------------------------------

    /**
     * Advances the countdown by [dtSeconds] real-world seconds.
     * Clamps to [0, config.timerStartSeconds]. No-op for CLASSIC.
     */
    fun tick(board: TriPeaksBoard, dtSeconds: Float): TriPeaksBoard {
        if (board.config.variant != TriPeaksVariant.TIMED) return board
        val newTimer = (board.timerSeconds - board.config.timerDrainRate * dtSeconds)
            .coerceAtLeast(0f)
        return board.copy(timerSeconds = newTimer)
    }

    // -------------------------------------------------------------------------
    // Hint
    // -------------------------------------------------------------------------

    /** Returns the position of one playable card, or null if none exists. */
    fun hintPosition(board: TriPeaksBoard): Int? =
        board.boardCards.firstOrNull { !it.removed && canPlay(board, it.position) }?.position

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun timerSecondsAfterClear(board: TriPeaksBoard, newCombo: Int): Float {
        if (board.config.variant != TriPeaksVariant.TIMED) return board.timerSeconds
        val bonus = board.config.timePerClear + board.config.timePerComboStep * (newCombo - 1)
        return (board.timerSeconds + bonus).coerceAtMost(board.config.timerStartSeconds)
    }
}
