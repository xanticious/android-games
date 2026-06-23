package com.xanticious.androidgames.controller.games.tictactoe

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.tictactoe.Mark
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeConfig
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeResult
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeState
import kotlin.random.Random

class TicTacToeController {
    private val lines = listOf(
        listOf(0, 1, 2),
        listOf(3, 4, 5),
        listOf(6, 7, 8),
        listOf(0, 3, 6),
        listOf(1, 4, 7),
        listOf(2, 5, 8),
        listOf(0, 4, 8),
        listOf(2, 4, 6)
    )

    fun configFor(difficulty: GameDifficulty): TicTacToeConfig = TicTacToeConfig(difficulty = difficulty)

    fun legalMoves(state: TicTacToeState): List<Int> = state.board
        .mapIndexedNotNull { index, mark -> if (mark == Mark.EMPTY) index else null }

    fun isLegalMove(state: TicTacToeState, cell: Int): Boolean =
        state.result == TicTacToeResult.InProgress && cell in state.board.indices && state.board[cell] == Mark.EMPTY

    fun applyMove(state: TicTacToeState, cell: Int): TicTacToeState {
        if (!isLegalMove(state, cell)) return state
        val board = state.board.toMutableList().also { it[cell] = state.currentMark }
        val result = evaluate(board)
        val nextMark = if (result == TicTacToeResult.InProgress) state.currentMark.opponent() else state.currentMark
        return state.copy(board = board, currentMark = nextMark, result = result, lastMove = cell)
    }

    fun chooseAiMove(state: TicTacToeState, random: Random = Random.Default): Int {
        val moves = legalMoves(state)
        if (moves.isEmpty()) return -1
        return when (state.config.difficulty) {
            GameDifficulty.EASY -> moves.random(random)
            GameDifficulty.MEDIUM -> immediateWin(state, state.config.aiMark)
                ?: immediateWin(state, state.config.playerMark)
                ?: moves.random(random)
            GameDifficulty.HARD -> bestMinimaxMove(state)
        }
    }

    fun evaluate(board: List<Mark>): TicTacToeResult {
        val winningLine = lines.firstOrNull { line ->
            val mark = board[line.first()]
            mark != Mark.EMPTY && line.all { board[it] == mark }
        }
        if (winningLine != null) {
            return TicTacToeResult.Win(winner = board[winningLine.first()], winningCells = winningLine)
        }
        return if (board.none { it == Mark.EMPTY }) TicTacToeResult.Draw else TicTacToeResult.InProgress
    }

    fun isDraw(state: TicTacToeState): Boolean = state.result == TicTacToeResult.Draw

    fun winner(state: TicTacToeState): TicTacToeResult.Win? = state.result as? TicTacToeResult.Win

    private fun immediateWin(state: TicTacToeState, mark: Mark): Int? = legalMoves(state).firstOrNull { move ->
        val board = state.board.toMutableList().also { it[move] = mark }
        val result = evaluate(board)
        result is TicTacToeResult.Win && result.winner == mark
    }

    private fun bestMinimaxMove(state: TicTacToeState): Int {
        val aiMark = state.config.aiMark
        val playerMark = state.config.playerMark
        val cache = mutableMapOf<Pair<List<Mark>, Mark>, Int>()
        return legalMoves(state).maxWithOrNull(
            compareBy<Int> { move ->
                val next = state.withMoveForSearch(move)
                minimax(next.board, next.currentMark, aiMark, playerMark, depth = 1, cache = cache)
            }.thenBy { preferredMoveRank(it) }
        ) ?: -1
    }

    private fun TicTacToeState.withMoveForSearch(cell: Int): TicTacToeState = applyMove(this, cell)

    private fun minimax(
        board: List<Mark>,
        currentMark: Mark,
        aiMark: Mark,
        playerMark: Mark,
        depth: Int,
        cache: MutableMap<Pair<List<Mark>, Mark>, Int>
    ): Int {
        when (val result = evaluate(board)) {
            TicTacToeResult.Draw -> return 0
            TicTacToeResult.InProgress -> Unit
            is TicTacToeResult.Win -> return when (result.winner) {
                aiMark -> 10 - depth
                playerMark -> depth - 10
                else -> 0
            }
        }

        val key = board to currentMark
        cache[key]?.let { return it }
        val moves = board.mapIndexedNotNull { index, mark -> if (mark == Mark.EMPTY) index else null }
        val score = if (currentMark == aiMark) {
            moves.maxOf { move ->
                val next = board.toMutableList().also { it[move] = currentMark }
                minimax(next, currentMark.opponent(), aiMark, playerMark, depth + 1, cache)
            }
        } else {
            moves.minOf { move ->
                val next = board.toMutableList().also { it[move] = currentMark }
                minimax(next, currentMark.opponent(), aiMark, playerMark, depth + 1, cache)
            }
        }
        cache[key] = score
        return score
    }

    private fun preferredMoveRank(move: Int): Int = when (move) {
        4 -> 3
        0, 2, 6, 8 -> 2
        else -> 1
    }
}
