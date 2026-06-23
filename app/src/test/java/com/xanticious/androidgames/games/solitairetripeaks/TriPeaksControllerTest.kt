package com.xanticious.androidgames.games.solitairetripeaks

import com.xanticious.androidgames.controller.games.solitairetripeaks.TriPeaksController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitairetripeaks.BoardCard
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksBoard
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksConfig
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TriPeaksControllerTest {

    private val controller = TriPeaksController()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun classicConfig(rankWrap: Boolean = true) = TriPeaksConfig(
        variant = TriPeaksVariant.CLASSIC,
        rankWrap = rankWrap,
    )

    private fun timedConfig() = TriPeaksConfig(
        variant = TriPeaksVariant.TIMED,
        timerStartSeconds = 60f,
        timerDrainRate = 1.0f,
        timePerClear = 2.0f,
        timePerComboStep = 0.5f,
    )

    /**
     * Build a minimal board with all 28 positions removed except [position],
     * which holds [card] (exposed because its coverers are removed), and a
     * waste pile whose top is [wasteCard].
     */
    private fun boardWithOneCard(
        position: Int,
        card: Card,
        wasteCard: Card,
        config: TriPeaksConfig = classicConfig(),
    ): TriPeaksBoard {
        val boardCards = (0 until 28).map { i ->
            if (i == position) {
                BoardCard(card = card.faceUp(), position = i, removed = false)
            } else {
                BoardCard(card = Card(Rank.TWO, Suit.CLUBS), position = i, removed = true)
            }
        }
        return TriPeaksBoard(
            boardCards = boardCards,
            stock = emptyList(),
            waste = listOf(wasteCard.faceUp()),
            score = 0,
            combo = 0,
            config = config,
        )
    }

    private fun emptyBoard(config: TriPeaksConfig = classicConfig()): TriPeaksBoard {
        val boardCards = (0 until 28).map { i ->
            BoardCard(card = Card(Rank.TWO, Suit.CLUBS), position = i, removed = true)
        }
        return TriPeaksBoard(
            boardCards = boardCards,
            stock = emptyList(),
            waste = listOf(Card(Rank.ACE, Suit.CLUBS).faceUp()),
            score = 0,
            combo = 0,
            config = config,
        )
    }

    // -------------------------------------------------------------------------
    // configFor
    // -------------------------------------------------------------------------

    @Test
    fun configFor_classicEasy_hasRankWrap() {
        val config = controller.configFor(TriPeaksVariant.CLASSIC, GameDifficulty.EASY)
        assertTrue(config.rankWrap)
    }

    @Test
    fun configFor_classicHard_noRankWrap() {
        val config = controller.configFor(TriPeaksVariant.CLASSIC, GameDifficulty.HARD)
        assertFalse(config.rankWrap)
    }

    @Test
    fun configFor_timedMedium_hasPositiveDrainRate() {
        val config = controller.configFor(TriPeaksVariant.TIMED, GameDifficulty.MEDIUM)
        assertTrue(config.timerDrainRate > 0f)
    }

    @Test
    fun configFor_timedEasy_startsLongerThanHard() {
        val easy = controller.configFor(TriPeaksVariant.TIMED, GameDifficulty.EASY)
        val hard = controller.configFor(TriPeaksVariant.TIMED, GameDifficulty.HARD)
        assertTrue(easy.timerStartSeconds > hard.timerStartSeconds)
    }

    // -------------------------------------------------------------------------
    // deal
    // -------------------------------------------------------------------------

    @Test
    fun deal_classic_produces28BoardCards() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        assertEquals(28, board.boardCards.size)
    }

    @Test
    fun deal_classic_stockPlusWastePlusBoardEquals52() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        assertEquals(52, board.boardCards.size + board.stock.size + board.waste.size)
    }

    @Test
    fun deal_classic_baseRowCardsAreFaceUp() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        for (i in 18..27) {
            assertTrue("Position $i should be face-up", board.boardCards[i].card.faceUp)
        }
    }

    @Test
    fun deal_classic_tipCardsAreFaceDown() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        // Tips (row 0) start face-down because they are covered.
        assertFalse(board.boardCards[0].card.faceUp)
        assertFalse(board.boardCards[1].card.faceUp)
        assertFalse(board.boardCards[2].card.faceUp)
    }

    @Test
    fun deal_timed_setsInitialTimer() {
        val config = timedConfig()
        val board = controller.deal(1L, config)
        assertEquals(config.timerStartSeconds, board.timerSeconds, 0.001f)
    }

    @Test
    fun deal_differentSeeds_produceDifferentBoards() {
        val config = classicConfig()
        val b1 = controller.deal(1L, config)
        val b2 = controller.deal(2L, config)
        val same = b1.boardCards.zip(b2.boardCards).all { (a, b) -> a.card == b.card }
        assertFalse(same)
    }

    // -------------------------------------------------------------------------
    // isExposed
    // -------------------------------------------------------------------------

    @Test
    fun isExposed_baseRowPosition_isExposed() {
        val board = controller.deal(42L, classicConfig())
        assertTrue(controller.isExposed(board, 18))
    }

    @Test
    fun isExposed_tipBeforeCoveringRowRemoved_notExposed() {
        val board = controller.deal(42L, classicConfig())
        assertFalse(controller.isExposed(board, 0))
    }

    @Test
    fun isExposed_tipAfterAllCoveringCardsRemoved_isExposed() {
        // Build board where positions 3 and 4 are removed → tip 0 exposed.
        val config = classicConfig()
        val rawBoard = controller.deal(42L, config)
        val updatedCards = rawBoard.boardCards.map { bc ->
            if (bc.position == 3 || bc.position == 4) bc.copy(removed = true)
            else bc
        }
        val board = rawBoard.copy(boardCards = updatedCards)
        assertTrue(controller.isExposed(board, 0))
    }

    @Test
    fun isExposed_removedCard_returnsFalse() {
        val board = controller.deal(42L, classicConfig())
        // Mark position 18 as removed.
        val updatedCards = board.boardCards.map {
            if (it.position == 18) it.copy(removed = true) else it
        }
        val b2 = board.copy(boardCards = updatedCards)
        assertFalse(controller.isExposed(b2, 18))
    }

    // -------------------------------------------------------------------------
    // isAdjacentRank (rank adjacency + wrap)
    // -------------------------------------------------------------------------

    @Test
    fun isAdjacentRank_sevenAndEight_adjacent() {
        assertTrue(controller.isAdjacentRank(Rank.SEVEN, Rank.EIGHT, rankWrap = false))
    }

    @Test
    fun isAdjacentRank_sevenAndNine_notAdjacent() {
        assertFalse(controller.isAdjacentRank(Rank.SEVEN, Rank.NINE, rankWrap = false))
    }

    @Test
    fun isAdjacentRank_aceAndKing_adjacentWithWrap() {
        assertTrue(controller.isAdjacentRank(Rank.ACE, Rank.KING, rankWrap = true))
    }

    @Test
    fun isAdjacentRank_kingAndAce_adjacentWithWrap() {
        assertTrue(controller.isAdjacentRank(Rank.KING, Rank.ACE, rankWrap = true))
    }

    @Test
    fun isAdjacentRank_aceAndKing_notAdjacentWithoutWrap() {
        assertFalse(controller.isAdjacentRank(Rank.ACE, Rank.KING, rankWrap = false))
    }

    @Test
    fun isAdjacentRank_aceAndTwo_adjacentAlways() {
        assertTrue(controller.isAdjacentRank(Rank.ACE, Rank.TWO, rankWrap = false))
    }

    @Test
    fun isAdjacentRank_queenAndKing_adjacent() {
        assertTrue(controller.isAdjacentRank(Rank.QUEEN, Rank.KING, rankWrap = false))
    }

    // -------------------------------------------------------------------------
    // canPlay
    // -------------------------------------------------------------------------

    @Test
    fun canPlay_exposedCardOneHigher_returnsTrue() {
        val board = boardWithOneCard(
            position = 25, // base row, exposed
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
        )
        assertTrue(controller.canPlay(board, 25))
    }

    @Test
    fun canPlay_exposedCardOneLower_returnsTrue() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.SIX, Suit.SPADES),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
        )
        assertTrue(controller.canPlay(board, 25))
    }

    @Test
    fun canPlay_exposedCardTwoAway_returnsFalse() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.NINE, Suit.SPADES),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
        )
        assertFalse(controller.canPlay(board, 25))
    }

    @Test
    fun canPlay_coveredCard_returnsFalse() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        // Position 0 (tip) is covered by 3 and 4 which are not removed.
        assertFalse(controller.canPlay(board, 0))
    }

    @Test
    fun canPlay_aceOnKingWithWrap_returnsTrue() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.ACE, Suit.SPADES),
            wasteCard = Card(Rank.KING, Suit.CLUBS),
            config = classicConfig(rankWrap = true),
        )
        assertTrue(controller.canPlay(board, 25))
    }

    @Test
    fun canPlay_aceOnKingWithoutWrap_returnsFalse() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.ACE, Suit.SPADES),
            wasteCard = Card(Rank.KING, Suit.CLUBS),
            config = classicConfig(rankWrap = false),
        )
        assertFalse(controller.canPlay(board, 25))
    }

    // -------------------------------------------------------------------------
    // playCard — scoring and combo
    // -------------------------------------------------------------------------

    @Test
    fun playCard_firstCardInChain_addsBasePoints() {
        val config = classicConfig()
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
            config = config,
        ).copy(combo = 0)
        val result = controller.playCard(board, 25)
        // combo becomes 1; score = 100 * 1 = 100
        assertEquals(100, result.score)
        assertEquals(1, result.combo)
    }

    @Test
    fun playCard_secondCardInChain_doublesPoints() {
        val config = classicConfig()
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
            config = config,
        ).copy(combo = 1, score = 100) // already played one card
        val result = controller.playCard(board, 25)
        // combo becomes 2; score += 100 * 2 = 200
        assertEquals(300, result.score)
        assertEquals(2, result.combo)
    }

    @Test
    fun playCard_movedCardAppearsOnWaste() {
        val card = Card(Rank.EIGHT, Suit.HEARTS)
        val board = boardWithOneCard(position = 25, card = card, wasteCard = Card(Rank.SEVEN, Suit.CLUBS))
        val result = controller.playCard(board, 25)
        assertEquals(card.faceUp(), result.wasteTop)
    }

    @Test
    fun playCard_removesCardFromBoard() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
        )
        val result = controller.playCard(board, 25)
        assertTrue(result.boardCards[25].removed)
    }

    // -------------------------------------------------------------------------
    // draw — resets combo
    // -------------------------------------------------------------------------

    @Test
    fun draw_fromNonEmptyStock_placesCardOnWaste() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        val topOfStock = board.stock.first().rank
        val result = controller.draw(board)
        assertEquals(topOfStock, result.wasteTop?.rank)
    }

    @Test
    fun draw_resetsCombo() {
        val config = classicConfig()
        val board = controller.deal(42L, config).copy(combo = 5)
        val result = controller.draw(board)
        assertEquals(0, result.combo)
    }

    @Test
    fun draw_reducesStockByOne() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        val before = board.stock.size
        val result = controller.draw(board)
        assertEquals(before - 1, result.stock.size)
    }

    @Test
    fun draw_emptyStock_returnsUnchangedBoard() {
        val config = classicConfig()
        val board = controller.deal(42L, config).copy(stock = emptyList())
        val result = controller.draw(board)
        assertEquals(board.waste, result.waste)
    }

    // -------------------------------------------------------------------------
    // isWon / isLost
    // -------------------------------------------------------------------------

    @Test
    fun isWon_allCardsRemoved_returnsTrue() {
        val board = emptyBoard()
        assertTrue(controller.isWon(board))
    }

    @Test
    fun isWon_oneCardRemaining_returnsFalse() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        assertFalse(controller.isWon(board))
    }

    @Test
    fun isLost_stockEmptyAndNoMoves_returnsTrue() {
        val config = classicConfig()
        // Build a board with one exposed card that can't match the waste.
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.TEN, Suit.SPADES),
            wasteCard = Card(Rank.TWO, Suit.CLUBS), // rank gap > 1
            config = config,
        ).copy(stock = emptyList())
        assertTrue(controller.isLost(board))
    }

    @Test
    fun isLost_stockNonEmpty_returnsFalse() {
        val config = classicConfig()
        val board = controller.deal(42L, config)
        // Has stock cards, so not yet lost even if no immediate moves.
        assertFalse("Should not be lost while stock is non-empty", controller.isLost(board))
    }

    @Test
    fun isLost_timedTimerExpired_returnsTrue() {
        val config = timedConfig()
        val board = controller.deal(1L, config).copy(timerSeconds = 0f)
        assertTrue(controller.isLost(board))
    }

    @Test
    fun isLost_timedTimerPositive_returnsFalse() {
        val config = timedConfig()
        val board = controller.deal(1L, config).copy(timerSeconds = 10f)
        // Fresh board with stock cards — not lost
        assertFalse(controller.isLost(board))
    }

    // -------------------------------------------------------------------------
    // tick (TIMED variant)
    // -------------------------------------------------------------------------

    @Test
    fun tick_drainsTimerByRateTimesDelta() {
        val config = timedConfig() // drain = 1.0
        val board = controller.deal(1L, config).copy(timerSeconds = 30f)
        val result = controller.tick(board, 5f)
        assertEquals(25f, result.timerSeconds, 0.001f)
    }

    @Test
    fun tick_clampsTimerAtZero() {
        val config = timedConfig()
        val board = controller.deal(1L, config).copy(timerSeconds = 2f)
        val result = controller.tick(board, 100f)
        assertEquals(0f, result.timerSeconds, 0.001f)
    }

    @Test
    fun tick_classicVariant_doesNotChangeTimer() {
        val config = classicConfig()
        val board = controller.deal(1L, config).copy(timerSeconds = 0f)
        val result = controller.tick(board, 5f)
        assertEquals(0f, result.timerSeconds, 0.001f)
    }

    // -------------------------------------------------------------------------
    // Timer bonus on card clear (TIMED)
    // -------------------------------------------------------------------------

    @Test
    fun playCard_timedVariant_addsTimeBonus() {
        val config = timedConfig() // timePerClear=2, timePerComboStep=0.5
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
            config = config,
        ).copy(timerSeconds = 20f, combo = 0)
        val result = controller.playCard(board, 25)
        // bonus = 2.0 + 0.5 * (1-1) = 2.0
        assertEquals(22f, result.timerSeconds, 0.001f)
    }

    @Test
    fun playCard_timedVariantWithCombo_addsExtraTimeBonus() {
        val config = timedConfig() // timePerClear=2, timePerComboStep=0.5
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
            config = config,
        ).copy(timerSeconds = 20f, combo = 3) // newCombo becomes 4
        val result = controller.playCard(board, 25)
        // bonus = 2.0 + 0.5 * (4-1) = 2.0 + 1.5 = 3.5
        assertEquals(23.5f, result.timerSeconds, 0.001f)
    }

    @Test
    fun playCard_timedVariant_timerCappedAtStart() {
        val config = timedConfig() // timerStart=60
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
            config = config,
        ).copy(timerSeconds = 59.5f, combo = 0)
        val result = controller.playCard(board, 25)
        // Would be 61.5, capped at 60
        assertEquals(60f, result.timerSeconds, 0.001f)
    }

    // -------------------------------------------------------------------------
    // winBonus
    // -------------------------------------------------------------------------

    @Test
    fun winBonus_stockRemaining_calculatesCorrectBonus() {
        val config = classicConfig() // stockBonusPerCard = 50
        val board = emptyBoard(config).copy(
            stock = List(5) { Card(Rank.TWO, Suit.CLUBS) }
        )
        assertEquals(250, controller.winBonus(board))
    }

    @Test
    fun winBonus_emptyStock_isZero() {
        val board = emptyBoard().copy(stock = emptyList())
        assertEquals(0, controller.winBonus(board))
    }

    // -------------------------------------------------------------------------
    // hasAnyMove
    // -------------------------------------------------------------------------

    @Test
    fun hasAnyMove_noMovesAvailable_returnsFalse() {
        val config = classicConfig()
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.TEN, Suit.SPADES),
            wasteCard = Card(Rank.TWO, Suit.CLUBS),
            config = config,
        ).copy(stock = emptyList())
        assertFalse(controller.hasAnyMove(board))
    }

    @Test
    fun hasAnyMove_moveAvailable_returnsTrue() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
        )
        assertTrue(controller.hasAnyMove(board))
    }

    // -------------------------------------------------------------------------
    // hintPosition
    // -------------------------------------------------------------------------

    @Test
    fun hintPosition_whenMoveExists_returnsPosition() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.EIGHT, Suit.HEARTS),
            wasteCard = Card(Rank.SEVEN, Suit.CLUBS),
        )
        assertEquals(25, controller.hintPosition(board))
    }

    @Test
    fun hintPosition_noMoves_returnsNull() {
        val board = boardWithOneCard(
            position = 25,
            card = Card(Rank.TEN, Suit.SPADES),
            wasteCard = Card(Rank.TWO, Suit.CLUBS),
        ).copy(stock = emptyList())
        assertNull(controller.hintPosition(board))
    }

    // -------------------------------------------------------------------------
    // Face-up flip on exposure
    // -------------------------------------------------------------------------

    @Test
    fun playCard_removingCoverer_flipsNewlyExposedCardFaceUp() {
        val config = classicConfig()
        // Build a board where position 19 (base row) is the only non-removed card,
        // covering position 10 which is also present but face-down.
        // All other coverers of 10 (position 20) are removed.
        val rawBoard = controller.deal(42L, config)
        val updated = rawBoard.boardCards.map { bc ->
            when (bc.position) {
                // Keep 19 and 10; remove everything else.
                19 -> bc.copy(removed = false)
                10 -> bc.copy(removed = false, card = bc.card.faceDown())
                else -> bc.copy(removed = true)
            }
        }
        val board = rawBoard.copy(
            boardCards = updated,
            waste = listOf(rawBoard.boardCards[19].card.copy(rank = Rank.SEVEN, faceUp = true)
                .let { it.copy(rank = Rank.SEVEN) }),
        )
        // Force waste top to match for playability.
        val boardReady = board.copy(
            waste = listOf(Card(Rank.SEVEN, Suit.CLUBS)),
            boardCards = board.boardCards.map { bc ->
                if (bc.position == 19) bc.copy(card = Card(Rank.EIGHT, Suit.HEARTS, faceUp = true))
                else bc
            }
        )
        val result = controller.playCard(boardReady, 19)
        assertTrue("Position 10 should be flipped face-up", result.boardCards[10].card.faceUp)
    }
}
