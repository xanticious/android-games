package com.xanticious.androidgames.games.loveletter

import com.xanticious.androidgames.controller.games.loveletter.LoveLetterController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.loveletter.LoveLetterCard
import com.xanticious.androidgames.model.games.loveletter.LoveLetterGame
import com.xanticious.androidgames.model.games.loveletter.LoveLetterPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LoveLetterControllerTest {

    private val controller = LoveLetterController()

    // ── Helper factories ────────────────────────────────────────────────────────

    private fun twoPlayerGame(
        p0Hand: List<LoveLetterCard> = listOf(LoveLetterCard.GUARD),
        p1Hand: List<LoveLetterCard> = listOf(LoveLetterCard.PRIEST),
        deck: List<LoveLetterCard> = listOf(LoveLetterCard.BARON),
        currentPlayerIndex: Int = 0
    ): LoveLetterGame {
        val players = listOf(
            LoveLetterPlayer("You", isHuman = true, hand = p0Hand),
            LoveLetterPlayer("Bot A", isHuman = false, hand = p1Hand)
        )
        return LoveLetterGame(
            players = players,
            deck = deck,
            burnedCard = LoveLetterCard.HANDMAID,
            revealedBurnCards = emptyList(),
            currentPlayerIndex = currentPlayerIndex,
            roundNumber = 1,
            tokensToWin = 7,
            difficulty = GameDifficulty.MEDIUM
        )
    }

    private fun threePlayerGame(
        p0Hand: List<LoveLetterCard> = listOf(LoveLetterCard.GUARD),
        p1Hand: List<LoveLetterCard> = listOf(LoveLetterCard.PRIEST),
        p2Hand: List<LoveLetterCard> = listOf(LoveLetterCard.BARON),
        currentPlayerIndex: Int = 0
    ): LoveLetterGame {
        val players = listOf(
            LoveLetterPlayer("You", isHuman = true, hand = p0Hand),
            LoveLetterPlayer("Bot A", isHuman = false, hand = p1Hand),
            LoveLetterPlayer("Bot B", isHuman = false, hand = p2Hand)
        )
        return LoveLetterGame(
            players = players,
            deck = listOf(LoveLetterCard.HANDMAID, LoveLetterCard.PRINCE),
            burnedCard = LoveLetterCard.KING,
            revealedBurnCards = emptyList(),
            currentPlayerIndex = currentPlayerIndex,
            roundNumber = 1,
            tokensToWin = 5,
            difficulty = GameDifficulty.MEDIUM
        )
    }

    // ── Deck ────────────────────────────────────────────────────────────────────

    @Test
    fun fullDeck_has16Cards() {
        assertEquals(16, controller.fullDeck.size)
    }

    @Test
    fun fullDeck_hasCorrectGuardCount() {
        assertEquals(5, controller.fullDeck.count { it == LoveLetterCard.GUARD })
    }

    @Test
    fun fullDeck_hasOnePrincess() {
        assertEquals(1, controller.fullDeck.count { it == LoveLetterCard.PRINCESS })
    }

    // ── startRound ──────────────────────────────────────────────────────────────

    @Test
    fun startRound_twoPlayers_burnsOneAndRevealsThree() {
        val base = controller.initialGame(2, GameDifficulty.EASY, 0L)
        val round = controller.startRound(base, seed = 1L)
        assertEquals(3, round.revealedBurnCards.size)
        // 16 - 1 burn - 3 revealed - 2 dealt = 10 in deck
        assertEquals(10, round.deck.size)
    }

    @Test
    fun startRound_threePlayers_noRevealedCards() {
        val base = controller.initialGame(3, GameDifficulty.EASY, 0L)
        val round = controller.startRound(base, seed = 2L)
        assertEquals(0, round.revealedBurnCards.size)
        // 16 - 1 burn - 3 dealt = 12 in deck
        assertEquals(12, round.deck.size)
    }

    @Test
    fun startRound_eachPlayerHasOneCard() {
        val base = controller.initialGame(3, GameDifficulty.EASY, 0L)
        val round = controller.startRound(base, seed = 3L)
        round.players.forEach { player ->
            assertEquals(1, player.hand.size)
        }
    }

    @Test
    fun startRound_resetsEliminationAndProtection() {
        val base = controller.initialGame(2, GameDifficulty.EASY, 0L)
        val firstRound = controller.startRound(base, seed = 4L)
        // Manually eliminate player 1
        val modified = firstRound.copy(
            players = firstRound.players.mapIndexed { i, p -> if (i == 1) p.copy(isEliminated = true) else p }
        )
        val nextRound = controller.startRound(controller.nextRound(modified), seed = 5L)
        assertFalse(nextRound.players[1].isEliminated)
        assertFalse(nextRound.players[1].isProtected)
    }

    // ── drawCard ────────────────────────────────────────────────────────────────

    @Test
    fun drawCard_addsCardToCurrentPlayersHand() {
        val g = twoPlayerGame(deck = listOf(LoveLetterCard.BARON))
        val after = controller.drawCard(g)
        assertEquals(2, after.currentPlayer.hand.size)
    }

    @Test
    fun drawCard_removesTopCardFromDeck() {
        val g = twoPlayerGame(deck = listOf(LoveLetterCard.BARON, LoveLetterCard.KING))
        val after = controller.drawCard(g)
        assertEquals(1, after.deck.size)
    }

    @Test
    fun drawCard_clearsHandmaidProtection() {
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD), isProtected = true),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.PRIEST))
            ),
            deck = listOf(LoveLetterCard.BARON)
        )
        val after = controller.drawCard(g)
        assertFalse(after.currentPlayer.isProtected)
    }

    // ── mustPlayCountess ────────────────────────────────────────────────────────

    @Test
    fun countess_heldWithKing_mustPlayCountess() {
        val hand = listOf(LoveLetterCard.COUNTESS, LoveLetterCard.KING)
        assertTrue(controller.mustPlayCountess(hand))
    }

    @Test
    fun countess_heldWithPrince_mustPlayCountess() {
        val hand = listOf(LoveLetterCard.COUNTESS, LoveLetterCard.PRINCE)
        assertTrue(controller.mustPlayCountess(hand))
    }

    @Test
    fun countess_heldWithoutRoyalty_notForced() {
        val hand = listOf(LoveLetterCard.COUNTESS, LoveLetterCard.PRIEST)
        assertFalse(controller.mustPlayCountess(hand))
    }

    @Test
    fun countess_notInHand_notForced() {
        val hand = listOf(LoveLetterCard.KING, LoveLetterCard.PRINCE)
        assertFalse(controller.mustPlayCountess(hand))
    }

    // ── Guard effect ────────────────────────────────────────────────────────────

    @Test
    fun guard_correctGuess_eliminatesTarget() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.GUARD, LoveLetterCard.PRIEST),
            p1Hand = listOf(LoveLetterCard.BARON)
        )
        val after = controller.playCard(g, LoveLetterCard.GUARD, targetPlayerIndex = 1, guardGuess = LoveLetterCard.BARON)
        assertTrue(after.players[1].isEliminated)
    }

    @Test
    fun guard_incorrectGuess_doesNotEliminateTarget() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.GUARD, LoveLetterCard.PRIEST),
            p1Hand = listOf(LoveLetterCard.BARON)
        )
        val after = controller.playCard(g, LoveLetterCard.GUARD, targetPlayerIndex = 1, guardGuess = LoveLetterCard.PRINCE)
        assertFalse(after.players[1].isEliminated)
    }

    @Test
    fun guard_correctGuess_discardedCardMovesToTargetDiscards() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.GUARD, LoveLetterCard.PRIEST),
            p1Hand = listOf(LoveLetterCard.KING)
        )
        val after = controller.playCard(g, LoveLetterCard.GUARD, targetPlayerIndex = 1, guardGuess = LoveLetterCard.KING)
        assertTrue(after.players[1].discards.contains(LoveLetterCard.KING))
        assertTrue(after.players[1].hand.isEmpty())
    }

    @Test
    fun guard_againstProtectedTarget_noEffect() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.GUARD, LoveLetterCard.PRIEST),
            p1Hand = listOf(LoveLetterCard.BARON)
        ).copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD, LoveLetterCard.PRIEST)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.BARON), isProtected = true)
            )
        )
        val after = controller.playCard(g, LoveLetterCard.GUARD, targetPlayerIndex = 1, guardGuess = LoveLetterCard.BARON)
        assertFalse(after.players[1].isEliminated)
    }

    // ── Priest effect ───────────────────────────────────────────────────────────

    @Test
    fun priest_revealTargetHand_doesNotEliminate() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.PRIEST, LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRINCESS)
        )
        val after = controller.playCard(g, LoveLetterCard.PRIEST, targetPlayerIndex = 1)
        assertFalse(after.players[1].isEliminated)
        assertTrue(after.lastEffect.contains("Princess"))
    }

    // ── Baron effect ────────────────────────────────────────────────────────────

    @Test
    fun baron_playerHasHigherCard_targetEliminated() {
        // Player holds KING (6), Bot holds GUARD (1)
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.BARON, LoveLetterCard.KING),
            p1Hand = listOf(LoveLetterCard.GUARD)
        )
        val after = controller.playCard(g, LoveLetterCard.BARON, targetPlayerIndex = 1)
        assertTrue(after.players[1].isEliminated)
        assertFalse(after.players[0].isEliminated)
    }

    @Test
    fun baron_playerHasLowerCard_playerEliminated() {
        // Player holds GUARD (1), Bot holds PRINCESS (8)
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.BARON, LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRINCESS)
        )
        val after = controller.playCard(g, LoveLetterCard.BARON, targetPlayerIndex = 1)
        assertTrue(after.players[0].isEliminated)
        assertFalse(after.players[1].isEliminated)
    }

    @Test
    fun baron_tieCards_noElimination() {
        // Both hold PRIEST (2) — tie
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.BARON, LoveLetterCard.PRIEST),
            p1Hand = listOf(LoveLetterCard.PRIEST)
        )
        val after = controller.playCard(g, LoveLetterCard.BARON, targetPlayerIndex = 1)
        assertFalse(after.players[0].isEliminated)
        assertFalse(after.players[1].isEliminated)
        assertTrue(after.lastEffect.contains("ties"))
    }

    // ── Handmaid effect ─────────────────────────────────────────────────────────

    @Test
    fun handmaid_protectsPlayer_isProtectedBecomesTrue() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.HANDMAID, LoveLetterCard.GUARD)
        )
        val after = controller.playCard(g, LoveLetterCard.HANDMAID)
        assertTrue(after.players[0].isProtected)
    }

    @Test
    fun handmaid_protectsFromGuard_noElimination() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.GUARD, LoveLetterCard.PRIEST),
            p1Hand = listOf(LoveLetterCard.BARON)
        ).copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD, LoveLetterCard.PRIEST)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.BARON), isProtected = true)
            )
        )
        val after = controller.playCard(g, LoveLetterCard.GUARD, targetPlayerIndex = 1, guardGuess = LoveLetterCard.BARON)
        assertFalse(after.players[1].isEliminated)
    }

    // ── Prince effect ───────────────────────────────────────────────────────────

    @Test
    fun prince_normalCard_targetRedrawsFromDeck() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.PRINCE, LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRIEST),
            deck = listOf(LoveLetterCard.KING)
        )
        val after = controller.playCard(g, LoveLetterCard.PRINCE, targetPlayerIndex = 1)
        assertFalse(after.players[1].isEliminated)
        assertTrue(after.players[1].discards.contains(LoveLetterCard.PRIEST))
        assertEquals(listOf(LoveLetterCard.KING), after.players[1].hand)
    }

    @Test
    fun prince_targetHoldsPrincess_targetEliminated() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.PRINCE, LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRINCESS),
            deck = listOf(LoveLetterCard.KING)
        )
        val after = controller.playCard(g, LoveLetterCard.PRINCE, targetPlayerIndex = 1)
        assertTrue(after.players[1].isEliminated)
        assertTrue(after.players[1].discards.contains(LoveLetterCard.PRINCESS))
    }

    @Test
    fun prince_targetSelf_playerRedrawsCard() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.PRINCE, LoveLetterCard.GUARD),
            deck = listOf(LoveLetterCard.KING)
        )
        val after = controller.playCard(g, LoveLetterCard.PRINCE, targetPlayerIndex = 0)
        assertFalse(after.players[0].isEliminated)
        assertTrue(after.players[0].discards.contains(LoveLetterCard.GUARD))
        assertEquals(listOf(LoveLetterCard.KING), after.players[0].hand)
    }

    @Test
    fun prince_emptyDeck_targetTakesBurnedCard() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.PRINCE, LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRIEST),
            deck = emptyList()
        ).copy(burnedCard = LoveLetterCard.BARON)
        val after = controller.playCard(g, LoveLetterCard.PRINCE, targetPlayerIndex = 1)
        assertFalse(after.players[1].isEliminated)
        assertEquals(listOf(LoveLetterCard.BARON), after.players[1].hand)
        assertNull(after.burnedCard)
    }

    // ── King effect ─────────────────────────────────────────────────────────────

    @Test
    fun king_swapsHandsWithTarget() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.KING, LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRINCESS)
        )
        val after = controller.playCard(g, LoveLetterCard.KING, targetPlayerIndex = 1)
        assertEquals(listOf(LoveLetterCard.PRINCESS), after.players[0].hand)
        assertEquals(listOf(LoveLetterCard.GUARD), after.players[1].hand)
    }

    @Test
    fun king_againstProtectedTarget_noSwap() {
        val g = LoveLetterGame(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.KING, LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.PRINCESS), isProtected = true)
            ),
            deck = emptyList(),
            burnedCard = null,
            revealedBurnCards = emptyList(),
            currentPlayerIndex = 0,
            roundNumber = 1,
            tokensToWin = 7,
            difficulty = GameDifficulty.MEDIUM
        )
        val after = controller.playCard(g, LoveLetterCard.KING, targetPlayerIndex = 1)
        // Hands should be unchanged
        assertEquals(listOf(LoveLetterCard.GUARD), after.players[0].hand)
        assertEquals(listOf(LoveLetterCard.PRINCESS), after.players[1].hand)
    }

    // ── Countess effect ─────────────────────────────────────────────────────────

    @Test
    fun countess_whenPlayed_isDiscarded() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.COUNTESS, LoveLetterCard.KING)
        )
        val after = controller.playCard(g, LoveLetterCard.COUNTESS)
        assertTrue(after.players[0].discards.contains(LoveLetterCard.COUNTESS))
        assertEquals(listOf(LoveLetterCard.KING), after.players[0].hand)
    }

    // ── Princess effect ─────────────────────────────────────────────────────────

    @Test
    fun princess_whenPlayed_eliminatesSelf() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.PRINCESS, LoveLetterCard.GUARD)
        )
        val after = controller.playCard(g, LoveLetterCard.PRINCESS)
        assertTrue(after.players[0].isEliminated)
    }

    @Test
    fun princess_whenPlayed_handIsEmpty() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.PRINCESS, LoveLetterCard.GUARD)
        )
        val after = controller.playCard(g, LoveLetterCard.PRINCESS)
        assertTrue(after.players[0].hand.isEmpty())
    }

    // ── Round end resolution ────────────────────────────────────────────────────

    @Test
    fun checkRoundOver_onePlayerRemaining_isTrue() {
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = emptyList(), isEliminated = true)
            )
        )
        assertTrue(controller.checkRoundOver(g))
    }

    @Test
    fun checkRoundOver_deckEmpty_isTrue() {
        val g = twoPlayerGame(deck = emptyList())
        assertTrue(controller.checkRoundOver(g))
    }

    @Test
    fun checkRoundOver_deckNotEmptyAndMultiplePlayers_isFalse() {
        val g = twoPlayerGame(deck = listOf(LoveLetterCard.GUARD))
        assertFalse(controller.checkRoundOver(g))
    }

    @Test
    fun roundWinner_oneActivePlayer_returnsThatPlayer() {
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = emptyList(), isEliminated = true)
            )
        )
        assertEquals(0, controller.roundWinnerIndex(g))
    }

    @Test
    fun roundWinner_deckEmpty_highestCardWins() {
        // Player 0 holds GUARD(1), Player 1 holds PRINCESS(8) → Player 1 wins
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRINCESS),
            deck = emptyList()
        )
        assertEquals(1, controller.roundWinnerIndex(g))
    }

    @Test
    fun roundWinner_tieOnHandCard_higherDiscardSumWins() {
        // Both hold BARON(3), but player 1 has more discarded value
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.BARON),
            p1Hand = listOf(LoveLetterCard.BARON),
            deck = emptyList()
        ).copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.BARON), discards = listOf(LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.BARON), discards = listOf(LoveLetterCard.PRINCE))
            )
        )
        // Player 1 has PRINCE(5) in discards vs GUARD(1) for player 0 → player 1 wins
        assertEquals(1, controller.roundWinnerIndex(g))
    }

    // ── Token awarding ──────────────────────────────────────────────────────────

    @Test
    fun awardToken_incrementsWinnerTokenCount() {
        val g = twoPlayerGame()
        val after = controller.awardToken(g, winnerIndex = 1)
        assertEquals(1, after.players[1].tokens)
        assertEquals(0, after.players[0].tokens)
    }

    @Test
    fun gameWinner_playerReachesTokenTarget_returnsIndex() {
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, tokens = 7),
                LoveLetterPlayer("Bot A", isHuman = false, tokens = 3)
            )
        )
        assertEquals(0, controller.gameWinner(g))
    }

    @Test
    fun gameWinner_noOneAtTarget_returnsNull() {
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, tokens = 3),
                LoveLetterPlayer("Bot A", isHuman = false, tokens = 5)
            )
        )
        assertNull(controller.gameWinner(g))
    }

    // ── Turn advancement ────────────────────────────────────────────────────────

    @Test
    fun advanceTurn_fromPlayer0_movesToPlayer1() {
        val g = twoPlayerGame(currentPlayerIndex = 0)
        val after = controller.advanceTurn(g)
        assertEquals(1, after.currentPlayerIndex)
    }

    @Test
    fun advanceTurn_wrapsAroundToPlayer0() {
        val g = twoPlayerGame(currentPlayerIndex = 1)
        val after = controller.advanceTurn(g)
        assertEquals(0, after.currentPlayerIndex)
    }

    @Test
    fun advanceTurn_skipsEliminatedPlayers() {
        val g = threePlayerGame(currentPlayerIndex = 0).copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = emptyList(), isEliminated = true),
                LoveLetterPlayer("Bot B", isHuman = false, hand = listOf(LoveLetterCard.BARON))
            )
        )
        val after = controller.advanceTurn(g)
        assertEquals(2, after.currentPlayerIndex)
    }

    // ── AI determinism ──────────────────────────────────────────────────────────

    @Test
    fun aiTurn_fixedSeed_isDeterministic() {
        val g = twoPlayerGame(
            p0Hand = listOf(LoveLetterCard.GUARD),
            p1Hand = listOf(LoveLetterCard.PRIEST),
            deck = listOf(LoveLetterCard.BARON, LoveLetterCard.HANDMAID),
            currentPlayerIndex = 1
        )
        val result1 = controller.takeAiTurn(g, Random(42L))
        val result2 = controller.takeAiTurn(g, Random(42L))
        assertEquals(result1.lastEffect, result2.lastEffect)
    }

    @Test
    fun aiChooseCard_mustPlayCountess_alwaysReturnsCountess() {
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.COUNTESS, LoveLetterCard.KING))
            ),
            currentPlayerIndex = 1
        )
        val card = controller.aiChooseCard(g, Random(0L))
        assertEquals(LoveLetterCard.COUNTESS, card)
    }

    @Test
    fun aiChooseCard_avoidsPrincess_unlessForcedToPlay() {
        // Give AI only Princess + one other card; it should pick the other card
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.PRINCESS, LoveLetterCard.GUARD))
            ),
            currentPlayerIndex = 1
        )
        val card = controller.aiChooseCard(g, Random(0L))
        assertEquals(LoveLetterCard.GUARD, card)
    }

    // ── validTargets ────────────────────────────────────────────────────────────

    @Test
    fun validTargets_guard_excludesSelf() {
        val g = twoPlayerGame()
        val targets = controller.validTargets(g, LoveLetterCard.GUARD, 0)
        assertFalse(targets.contains(0))
    }

    @Test
    fun validTargets_guard_excludesProtectedOpponent() {
        val g = twoPlayerGame().copy(
            players = listOf(
                LoveLetterPlayer("You", isHuman = true, hand = listOf(LoveLetterCard.GUARD)),
                LoveLetterPlayer("Bot A", isHuman = false, hand = listOf(LoveLetterCard.PRIEST), isProtected = true)
            )
        )
        assertTrue(controller.validTargets(g, LoveLetterCard.GUARD, 0).isEmpty())
    }

    @Test
    fun validTargets_prince_includesSelf() {
        val g = twoPlayerGame()
        val targets = controller.validTargets(g, LoveLetterCard.PRINCE, 0)
        assertTrue(targets.contains(0))
    }

    // ── tokensToWin ─────────────────────────────────────────────────────────────

    @Test
    fun tokensToWin_twoPlayers_isSeven() {
        assertEquals(7, controller.tokensToWin(2))
    }

    @Test
    fun tokensToWin_threePlayers_isFive() {
        assertEquals(5, controller.tokensToWin(3))
    }

    @Test
    fun tokensToWin_fourPlayers_isFour() {
        assertEquals(4, controller.tokensToWin(4))
    }
}
