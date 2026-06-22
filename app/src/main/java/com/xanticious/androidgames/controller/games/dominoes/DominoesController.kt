package com.xanticious.androidgames.controller.games.dominoes

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.dominoes.DominoTile
import com.xanticious.androidgames.model.games.dominoes.DominoesAiDecision
import com.xanticious.androidgames.model.games.dominoes.DominoesConfig
import com.xanticious.androidgames.model.games.dominoes.DominoesEnd
import com.xanticious.androidgames.model.games.dominoes.DominoesMove
import com.xanticious.androidgames.model.games.dominoes.DominoesPlayer
import com.xanticious.androidgames.model.games.dominoes.DominoesResult
import com.xanticious.androidgames.model.games.dominoes.DominoesRuleset
import com.xanticious.androidgames.model.games.dominoes.DominoesState
import com.xanticious.androidgames.model.games.dominoes.PlayedDomino
import kotlin.math.abs
import kotlin.random.Random

/** Pure Draw Dominoes / Fives rules and local AI. */
class DominoesController {
    fun configFor(difficulty: GameDifficulty): DominoesConfig = when (difficulty) {
        GameDifficulty.EASY -> DominoesConfig(targetScore = 50, ruleset = DominoesRuleset.DRAW)
        GameDifficulty.MEDIUM -> DominoesConfig(targetScore = 100, ruleset = DominoesRuleset.FIVES)
        GameDifficulty.HARD -> DominoesConfig(targetScore = 150, ruleset = DominoesRuleset.FIVES)
    }

    fun fullSet(): List<DominoTile> = (0..6).flatMap { left -> (left..6).map { right -> DominoTile.of(left, right) } }

    fun deal(random: Random, config: DominoesConfig = DominoesConfig()): DominoesState {
        val shuffled = fullSet().shuffled(random)
        val playerHand = shuffled.take(config.handSize)
        val aiHand = shuffled.drop(config.handSize).take(config.handSize)
        val boneyard = shuffled.drop(config.handSize * 2)
        val starter = chooseStarter(playerHand, aiHand, config)
        val starterTile = starter.second
        val line = listOf(PlayedDomino(starterTile, starterTile.low, starterTile.high))
        val openingScore = fivesScore(starterTile.low, starterTile.high, config)
        val playerScore = if (starter.first == DominoesPlayer.PLAYER) openingScore else 0
        val aiScore = if (starter.first == DominoesPlayer.AI) openingScore else 0
        return DominoesState(
            boneyard = boneyard,
            playerHand = if (starter.first == DominoesPlayer.PLAYER) playerHand - starterTile else playerHand,
            aiHand = if (starter.first == DominoesPlayer.AI) aiHand - starterTile else aiHand,
            line = line,
            leftOpen = starterTile.low,
            rightOpen = starterTile.high,
            currentPlayer = opponent(starter.first),
            playerScore = playerScore,
            aiScore = aiScore,
            config = config,
            result = winnerFromScores(playerScore, aiScore, config),
            roundOver = false,
            lastMessage = "${starter.first.label()} opened with $starterTile.",
            moveCount = 1
        )
    }

    fun legalMoves(state: DominoesState, player: DominoesPlayer = state.currentPlayer): List<DominoesMove> {
        if (state.isGameOver || state.roundOver) return emptyList()
        val hand = handFor(state, player)
        if (state.line.isEmpty()) return hand.flatMap { tile -> listOf(DominoesMove(tile, DominoesEnd.LEFT)) }
        val left = state.leftOpen
        val right = state.rightOpen
        return hand.flatMap { tile ->
            buildList {
                if (left != null && tile.matches(left)) add(DominoesMove(tile, DominoesEnd.LEFT))
                if (right != null && tile.matches(right) && right != left) add(DominoesMove(tile, DominoesEnd.RIGHT))
            }
        }
    }

    fun applyMove(state: DominoesState, move: DominoesMove): DominoesState {
        if (state.isGameOver || state.roundOver || move !in legalMoves(state)) return state.copy(lastMessage = "Illegal move.")
        val played = orient(move.tile, move.end, state)
        val nextLine = if (move.end == DominoesEnd.LEFT) listOf(played) + state.line else state.line + played
        val nextHand = handFor(state, state.currentPlayer) - move.tile
        val nextLeft = nextLine.first().leftPips
        val nextRight = nextLine.last().rightPips
        val fives = fivesScore(nextLeft, nextRight, state.config)
        var playerScore = state.playerScore + if (state.currentPlayer == DominoesPlayer.PLAYER) fives else 0
        var aiScore = state.aiScore + if (state.currentPlayer == DominoesPlayer.AI) fives else 0
        val withoutTile = replaceHand(state, state.currentPlayer, nextHand).copy(
            line = nextLine,
            leftOpen = nextLeft,
            rightOpen = nextRight,
            playerScore = playerScore,
            aiScore = aiScore,
            moveCount = state.moveCount + 1
        )
        if (nextHand.isEmpty()) {
            val roundPoints = remainingPips(handFor(withoutTile, opponent(state.currentPlayer)))
            if (state.currentPlayer == DominoesPlayer.PLAYER) playerScore += roundPoints else aiScore += roundPoints
            return withoutTile.copy(
                playerScore = playerScore,
                aiScore = aiScore,
                currentPlayer = state.currentPlayer,
                result = winnerFromScores(playerScore, aiScore, state.config),
                roundOver = winnerFromScores(playerScore, aiScore, state.config) == DominoesResult.NONE,
                lastMessage = "${state.currentPlayer.label()} emptied their hand for $roundPoints points."
            )
        }
        return withoutTile.copy(
            currentPlayer = opponent(state.currentPlayer),
            result = winnerFromScores(playerScore, aiScore, state.config),
            lastMessage = buildString {
                append("${state.currentPlayer.label()} played ${move.tile}.")
                if (fives > 0) append(" Fives scores $fives.")
            }
        )
    }

    fun drawUntilPlayable(state: DominoesState): DominoesState {
        if (state.isGameOver || state.roundOver || legalMoves(state).isNotEmpty()) return state
        var boneyard = state.boneyard
        var hand = handFor(state, state.currentPlayer)
        var drawn = 0
        while (boneyard.isNotEmpty()) {
            val tile = boneyard.first()
            boneyard = boneyard.drop(1)
            hand = hand + tile
            drawn += 1
            val probe = replaceHand(state.copy(boneyard = boneyard), state.currentPlayer, hand)
            if (legalMoves(probe).isNotEmpty()) {
                return probe.copy(lastMessage = "${state.currentPlayer.label()} drew $drawn tile${if (drawn == 1) "" else "s"}.")
            }
        }
        return pass(replaceHand(state.copy(boneyard = boneyard), state.currentPlayer, hand))
    }

    fun pass(state: DominoesState): DominoesState {
        if (state.isGameOver || state.roundOver) return state
        if (legalMoves(state).isNotEmpty()) return state.copy(lastMessage = "Play a matching tile before passing.")
        if (state.boneyard.isNotEmpty()) return drawUntilPlayable(state)
        val other = opponent(state.currentPlayer)
        val otherHasMove = legalMoves(state, other).isNotEmpty()
        return if (otherHasMove) {
            state.copy(currentPlayer = other, lastMessage = "${state.currentPlayer.label()} passed.")
        } else {
            scoreBlockedRound(state)
        }
    }

    fun aiDecision(state: DominoesState, difficulty: GameDifficulty, random: Random = Random.Default): DominoesAiDecision {
        if (state.currentPlayer != DominoesPlayer.AI || state.isGameOver || state.roundOver) return DominoesAiDecision(move = null)
        val readyState = if (state.currentPlayer == DominoesPlayer.AI && legalMoves(state).isEmpty()) drawUntilPlayable(state) else state
        val drawn = state.boneyard.size - readyState.boneyard.size
        val legal = legalMoves(readyState, DominoesPlayer.AI)
        if (legal.isEmpty()) return DominoesAiDecision(move = null, drewTiles = drawn)
        return DominoesAiDecision(move = chooseAiMove(readyState, legal, difficulty, random), drewTiles = drawn)
    }

    fun continueRound(state: DominoesState, random: Random): DominoesState = if (state.roundOver && !state.isGameOver) {
        val next = deal(random, state.config)
        val playerScore = state.playerScore + next.playerScore
        val aiScore = state.aiScore + next.aiScore
        next.copy(
            playerScore = playerScore,
            aiScore = aiScore,
            result = winnerFromScores(playerScore, aiScore, state.config)
        )
    } else state

    private fun chooseStarter(
        playerHand: List<DominoTile>,
        aiHand: List<DominoTile>,
        config: DominoesConfig
    ): Pair<DominoesPlayer, DominoTile> {
        val owned = playerHand.map { DominoesPlayer.PLAYER to it } + aiHand.map { DominoesPlayer.AI to it }
        val pool = if (config.highestDoubleStarts) owned.filter { it.second.isDouble }.ifEmpty { owned } else owned
        return pool.maxWith(compareBy<Pair<DominoesPlayer, DominoTile>> { it.second.pipTotal }.thenBy { it.second.high })
    }

    private fun orient(tile: DominoTile, end: DominoesEnd, state: DominoesState): PlayedDomino {
        if (state.line.isEmpty()) return PlayedDomino(tile, tile.low, tile.high)
        return when (end) {
            DominoesEnd.LEFT -> {
                val match = state.leftOpen ?: tile.low
                PlayedDomino(tile, tile.otherSide(match), match)
            }
            DominoesEnd.RIGHT -> {
                val match = state.rightOpen ?: tile.low
                PlayedDomino(tile, match, tile.otherSide(match))
            }
        }
    }

    private fun scoreBlockedRound(state: DominoesState): DominoesState {
        val playerPips = remainingPips(state.playerHand)
        val aiPips = remainingPips(state.aiHand)
        val roundWinner = when {
            playerPips < aiPips -> DominoesPlayer.PLAYER
            aiPips < playerPips -> DominoesPlayer.AI
            else -> null
        }
        val points = abs(playerPips - aiPips)
        val playerScore = state.playerScore + if (roundWinner == DominoesPlayer.PLAYER) points else 0
        val aiScore = state.aiScore + if (roundWinner == DominoesPlayer.AI) points else 0
        return state.copy(
            playerScore = playerScore,
            aiScore = aiScore,
            result = winnerFromScores(playerScore, aiScore, state.config),
            roundOver = winnerFromScores(playerScore, aiScore, state.config) == DominoesResult.NONE,
            lastMessage = when (roundWinner) {
                DominoesPlayer.PLAYER -> "Round blocked. You win $points points."
                DominoesPlayer.AI -> "Round blocked. AI wins $points points."
                null -> "Round blocked with equal pips."
            }
        )
    }

    private fun chooseAiMove(
        state: DominoesState,
        legal: List<DominoesMove>,
        difficulty: GameDifficulty,
        random: Random
    ): DominoesMove = when (difficulty) {
        GameDifficulty.EASY -> legal.first()
        GameDifficulty.MEDIUM -> legal.maxWith(compareBy<DominoesMove> { scoreAfterMove(state, it) }.thenBy { it.tile.pipTotal })
        GameDifficulty.HARD -> legal.maxWith(compareBy<DominoesMove> { scoreAfterMove(state, it) * 3 + it.tile.pipTotal + flexibilityAfterMove(state, it) }.thenBy { it.tile.high })
            .let { best -> legal.filter { scoreAfterMove(state, it) == scoreAfterMove(state, best) }.randomOrNull(random) ?: best }
    }

    private fun scoreAfterMove(state: DominoesState, move: DominoesMove): Int {
        val played = orient(move.tile, move.end, state)
        val left = if (move.end == DominoesEnd.LEFT) played.leftPips else state.leftOpen ?: played.leftPips
        val right = if (move.end == DominoesEnd.RIGHT) played.rightPips else state.rightOpen ?: played.rightPips
        return fivesScore(left, right, state.config)
    }

    private fun flexibilityAfterMove(state: DominoesState, move: DominoesMove): Int {
        val next = applyMove(state, move).copy(currentPlayer = DominoesPlayer.AI)
        return legalMoves(next, DominoesPlayer.AI).size
    }

    private fun fivesScore(left: Int, right: Int, config: DominoesConfig): Int {
        val total = left + right
        return if (config.ruleset == DominoesRuleset.FIVES && total > 0 && total % 5 == 0) total else 0
    }

    private fun winnerFromScores(playerScore: Int, aiScore: Int, config: DominoesConfig): DominoesResult = when {
        playerScore >= config.targetScore && playerScore >= aiScore -> DominoesResult.PLAYER_WIN
        aiScore >= config.targetScore -> DominoesResult.AI_WIN
        else -> DominoesResult.NONE
    }

    private fun handFor(state: DominoesState, player: DominoesPlayer): List<DominoTile> =
        if (player == DominoesPlayer.PLAYER) state.playerHand else state.aiHand

    private fun replaceHand(state: DominoesState, player: DominoesPlayer, hand: List<DominoTile>): DominoesState =
        if (player == DominoesPlayer.PLAYER) state.copy(playerHand = hand) else state.copy(aiHand = hand)

    private fun opponent(player: DominoesPlayer): DominoesPlayer =
        if (player == DominoesPlayer.PLAYER) DominoesPlayer.AI else DominoesPlayer.PLAYER

    private fun remainingPips(hand: List<DominoTile>): Int = hand.sumOf { it.pipTotal }

    private fun DominoesPlayer.label(): String = if (this == DominoesPlayer.PLAYER) "You" else "AI"
}
