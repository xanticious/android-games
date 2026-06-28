package com.xanticious.androidgames.games.dominoes

import com.xanticious.androidgames.controller.games.dominoes.DominoesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.dominoes.DominoTile
import com.xanticious.androidgames.model.games.dominoes.DominoesConfig
import com.xanticious.androidgames.model.games.dominoes.DominoesEnd
import com.xanticious.androidgames.model.games.dominoes.DominoesMove
import com.xanticious.androidgames.model.games.dominoes.DominoesPlayer
import com.xanticious.androidgames.model.games.dominoes.DominoesRuleset
import com.xanticious.androidgames.model.games.dominoes.DominoesState
import com.xanticious.androidgames.model.games.dominoes.PlayedDomino
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DominoesControllerTest {
    private val controller = DominoesController()
    private val config = DominoesConfig(targetScore = 100, ruleset = DominoesRuleset.DRAW)

    private fun state(
        playerHand: List<DominoTile>,
        aiHand: List<DominoTile> = listOf(DominoTile.of(1, 1)),
        boneyard: List<DominoTile> = emptyList(),
        line: List<PlayedDomino> = listOf(PlayedDomino(DominoTile.of(2, 4), 2, 4)),
        currentPlayer: DominoesPlayer = DominoesPlayer.PLAYER
    ) = DominoesState(
        boneyard = boneyard,
        playerHand = playerHand,
        aiHand = aiHand,
        line = line,
        leftOpen = line.firstOrNull()?.leftPips,
        rightOpen = line.lastOrNull()?.rightPips,
        currentPlayer = currentPlayer,
        playerScore = 0,
        aiScore = 0,
        config = config,
        result = com.xanticious.androidgames.model.games.dominoes.DominoesResult.NONE,
        roundOver = false,
        lastMessage = "test"
    )

    @Test
    fun deal_sameSeed_isDeterministic() {
        assertEquals(controller.deal(Random(17), config), controller.deal(Random(17), config))
    }

    @Test
    fun deal_doubleSixSet_accountsForAllTiles() {
        val dealt = controller.deal(Random(23), config)
        assertEquals(28, dealt.playerHand.size + dealt.aiHand.size + dealt.boneyard.size + dealt.line.size)
    }

    @Test
    fun legalMoves_againstOpenEnds_returnsMatchingEnds() {
        val moves = controller.legalMoves(state(playerHand = listOf(DominoTile.of(0, 1), DominoTile.of(4, 5), DominoTile.of(6, 6))))
        assertEquals(listOf(DominoesMove(DominoTile.of(4, 5), DominoesEnd.RIGHT)), moves)
    }

    @Test
    fun applyMove_onLeft_updatesOpenEnds() {
        val next = controller.applyMove(
            state(playerHand = listOf(DominoTile.of(1, 3)), line = listOf(PlayedDomino(DominoTile.of(3, 5), 3, 5))),
            DominoesMove(DominoTile.of(1, 3), DominoesEnd.LEFT)
        )
        assertEquals(listOf(1, 5), next.openEnds)
    }

    @Test
    fun applyMove_emptyHand_endsRound() {
        val next = controller.applyMove(
            state(playerHand = listOf(DominoTile.of(5, 6)), line = listOf(PlayedDomino(DominoTile.of(5, 1), 5, 1))),
            DominoesMove(DominoTile.of(5, 6), DominoesEnd.LEFT)
        )
        assertTrue(next.roundOver)
    }

    @Test
    fun pass_blockedRound_scoresLowerPipHand() {
        val blocked = state(
            playerHand = listOf(DominoTile.of(0, 0)),
            aiHand = listOf(DominoTile.of(1, 1)),
            line = listOf(PlayedDomino(DominoTile.of(6, 6), 6, 6))
        )
        assertEquals(2, controller.pass(blocked).playerScore)
    }

    @Test
    fun aiDecision_withLegalMove_returnsLegalPlay() {
        val aiState = state(
            playerHand = listOf(DominoTile.of(0, 0)),
            aiHand = listOf(DominoTile.of(4, 6)),
            currentPlayer = DominoesPlayer.AI
        )
        assertTrue(controller.aiDecision(aiState, GameDifficulty.HARD, Random(4)).move in controller.legalMoves(aiState, DominoesPlayer.AI))
    }

    @Test
    fun aiDecision_withoutLegalMove_returnsPass() {
        val aiState = state(
            playerHand = listOf(DominoTile.of(0, 0)),
            aiHand = listOf(DominoTile.of(5, 6)),
            currentPlayer = DominoesPlayer.AI
        )
        assertNull(controller.aiDecision(aiState, GameDifficulty.EASY, Random(9)).move)
    }
}
