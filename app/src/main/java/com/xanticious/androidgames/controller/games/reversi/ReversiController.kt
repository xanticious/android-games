package com.xanticious.androidgames.controller.games.reversi

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.reversi.Disc
import com.xanticious.androidgames.model.games.reversi.REVERSI_BOARD_SIZE
import com.xanticious.androidgames.model.games.reversi.ReversiConfig
import com.xanticious.androidgames.model.games.reversi.ReversiMove
import com.xanticious.androidgames.model.games.reversi.ReversiPosition
import com.xanticious.androidgames.model.games.reversi.ReversiResult
import com.xanticious.androidgames.model.games.reversi.ReversiState
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ReversiController {
    private val directions = listOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1, 0 to 1,
        1 to -1, 1 to 0, 1 to 1
    )

    private val positionalWeights = listOf(
        listOf(120, -20, 20, 5, 5, 20, -20, 120),
        listOf(-20, -40, -5, -5, -5, -5, -40, -20),
        listOf(20, -5, 15, 3, 3, 15, -5, 20),
        listOf(5, -5, 3, 3, 3, 3, -5, 5),
        listOf(5, -5, 3, 3, 3, 3, -5, 5),
        listOf(20, -5, 15, 3, 3, 15, -5, 20),
        listOf(-20, -40, -5, -5, -5, -5, -40, -20),
        listOf(120, -20, 20, 5, 5, 20, -20, 120)
    )

    fun configFor(difficulty: GameDifficulty, randomSeed: Int = 0): ReversiConfig =
        ReversiConfig(difficulty = difficulty, randomSeed = randomSeed)

    fun initialState(config: ReversiConfig): ReversiState = ReversiState.initial(config)

    fun legalMoves(state: ReversiState, player: Disc = state.currentPlayer): List<ReversiMove> {
        if (player == Disc.EMPTY || state.result != null) return emptyList()
        return (0 until REVERSI_BOARD_SIZE).flatMap { row ->
            (0 until REVERSI_BOARD_SIZE).mapNotNull { col ->
                val flips = flipsFor(state.board, row, col, player)
                if (flips.isEmpty()) null else ReversiMove(ReversiPosition(row, col), flips)
            }
        }
    }

    fun applyMove(state: ReversiState, row: Int, col: Int): ReversiState {
        val move = legalMoves(state).firstOrNull { it.position.row == row && it.position.col == col } ?: return state
        val mutableBoard = state.board.map { it.toMutableList() }.toMutableList()
        mutableBoard[row][col] = state.currentPlayer
        move.flips.forEach { mutableBoard[it.row][it.col] = state.currentPlayer }
        val moved = ReversiState.fromBoard(
            board = mutableBoard.map { it.toList() },
            currentPlayer = state.currentPlayer.opponent(),
            config = state.config,
            lastMove = move.position,
            lastPass = null
        )
        return skipUnavailableTurns(moved, passedPlayer = null)
    }

    fun skipUnavailableTurns(state: ReversiState, passedPlayer: Disc? = null): ReversiState {
        val blackMoves = legalMoves(state.copy(currentPlayer = Disc.BLACK), Disc.BLACK)
        val whiteMoves = legalMoves(state.copy(currentPlayer = Disc.WHITE), Disc.WHITE)
        if (state.boardFull() || (blackMoves.isEmpty() && whiteMoves.isEmpty())) {
            return state.copy(result = resultFor(state))
        }
        val activeMoves = if (state.currentPlayer == Disc.BLACK) blackMoves else whiteMoves
        return if (activeMoves.isEmpty()) {
            state.copy(currentPlayer = state.currentPlayer.opponent(), lastPass = passedPlayer ?: state.currentPlayer)
        } else {
            state.copy(lastPass = passedPlayer)
        }
    }

    fun chooseAiMove(state: ReversiState, random: Random = Random(state.config.randomSeed)): ReversiMove? {
        val moves = legalMoves(state, state.config.aiDisc)
        if (moves.isEmpty()) return null
        return when (state.config.difficulty) {
            GameDifficulty.EASY -> moves.random(random)
            GameDifficulty.MEDIUM -> bestByScore(moves, random) { move ->
                move.flips.size * 6 + positionalWeights[move.position.row][move.position.col]
            }
            GameDifficulty.HARD -> bestByScore(moves, random) { move ->
                val next = applyMove(state.copy(currentPlayer = state.config.aiDisc), move.position.row, move.position.col)
                minimax(next, state.config.aiDisc, depth = 4, alpha = Int.MIN_VALUE / 2, beta = Int.MAX_VALUE / 2)
            }
        }
    }

    fun resultFor(state: ReversiState): ReversiResult {
        val winner = when {
            state.blackCount > state.whiteCount -> Disc.BLACK
            state.whiteCount > state.blackCount -> Disc.WHITE
            else -> Disc.EMPTY
        }
        return ReversiResult(state.blackCount, state.whiteCount, winner)
    }

    private fun flipsFor(board: List<List<Disc>>, row: Int, col: Int, player: Disc): List<ReversiPosition> {
        if (!inside(row, col) || board[row][col] != Disc.EMPTY || player == Disc.EMPTY) return emptyList()
        return directions.flatMap { (dr, dc) -> flipsInDirection(board, row, col, dr, dc, player) }
    }

    private fun flipsInDirection(
        board: List<List<Disc>>,
        row: Int,
        col: Int,
        dr: Int,
        dc: Int,
        player: Disc
    ): List<ReversiPosition> {
        val opponent = player.opponent()
        val flips = mutableListOf<ReversiPosition>()
        var r = row + dr
        var c = col + dc
        while (inside(r, c) && board[r][c] == opponent) {
            flips += ReversiPosition(r, c)
            r += dr
            c += dc
        }
        return if (flips.isNotEmpty() && inside(r, c) && board[r][c] == player) flips else emptyList()
    }

    private fun bestByScore(moves: List<ReversiMove>, random: Random, score: (ReversiMove) -> Int): ReversiMove {
        val scored = moves.map { it to score(it) }
        val bestScore = scored.maxOf { it.second }
        return scored.filter { it.second == bestScore }.map { it.first }.random(random)
    }

    private fun minimax(state: ReversiState, maximizingDisc: Disc, depth: Int, alpha: Int, beta: Int): Int {
        if (depth == 0 || state.result != null) return evaluate(state, maximizingDisc)
        val moves = legalMoves(state, state.currentPlayer)
        if (moves.isEmpty()) {
            val skipped = skipUnavailableTurns(state)
            return if (skipped.currentPlayer == state.currentPlayer) evaluate(skipped, maximizingDisc)
            else minimax(skipped, maximizingDisc, depth - 1, alpha, beta)
        }
        var a = alpha
        var b = beta
        return if (state.currentPlayer == maximizingDisc) {
            var value = Int.MIN_VALUE / 2
            for (move in moves) {
                value = max(value, minimax(applyMove(state, move.position.row, move.position.col), maximizingDisc, depth - 1, a, b))
                a = max(a, value)
                if (a >= b) break
            }
            value
        } else {
            var value = Int.MAX_VALUE / 2
            for (move in moves) {
                value = min(value, minimax(applyMove(state, move.position.row, move.position.col), maximizingDisc, depth - 1, a, b))
                b = min(b, value)
                if (a >= b) break
            }
            value
        }
    }

    private fun evaluate(state: ReversiState, disc: Disc): Int {
        val opponent = disc.opponent()
        val discCount = state.count(disc) - state.count(opponent)
        val mobility = legalMoves(state, disc).size - legalMoves(state, opponent).size
        var position = 0
        for (row in state.board.indices) {
            for (col in state.board[row].indices) {
                position += when (state.board[row][col]) {
                    disc -> positionalWeights[row][col]
                    opponent -> -positionalWeights[row][col]
                    else -> 0
                }
            }
        }
        val resultBonus = state.result?.let { result ->
            when (result.winner) {
                disc -> 10_000
                opponent -> -10_000
                else -> 0
            }
        } ?: 0
        return resultBonus + discCount * 4 + mobility * 12 + position
    }

    private fun ReversiState.count(disc: Disc): Int = when (disc) {
        Disc.BLACK -> blackCount
        Disc.WHITE -> whiteCount
        Disc.EMPTY -> board.sumOf { row -> row.count { it == Disc.EMPTY } }
    }

    private fun ReversiState.boardFull(): Boolean = board.none { row -> row.any { it == Disc.EMPTY } }

    private fun inside(row: Int, col: Int): Boolean =
        row in 0 until REVERSI_BOARD_SIZE && col in 0 until REVERSI_BOARD_SIZE

    private fun Disc.opponent(): Disc = when (this) {
        Disc.BLACK -> Disc.WHITE
        Disc.WHITE -> Disc.BLACK
        Disc.EMPTY -> Disc.EMPTY
    }
}
