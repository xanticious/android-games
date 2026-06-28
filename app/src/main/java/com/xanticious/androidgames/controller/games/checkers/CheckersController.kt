package com.xanticious.androidgames.controller.games.checkers

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.checkers.CheckersConfig
import com.xanticious.androidgames.model.games.checkers.CheckersMove
import com.xanticious.androidgames.model.games.checkers.CheckersPiece
import com.xanticious.androidgames.model.games.checkers.CheckersResult
import com.xanticious.androidgames.model.games.checkers.CheckersSide
import com.xanticious.androidgames.model.games.checkers.CheckersSquare
import com.xanticious.androidgames.model.games.checkers.CheckersState
import com.xanticious.androidgames.model.games.checkers.CheckersWinReason
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class CheckersController {
    private val boardRange = 0 until CheckersState.BOARD_SIZE

    fun configFor(difficulty: GameDifficulty): CheckersConfig = CheckersConfig(difficulty = difficulty)

    fun legalMoves(state: CheckersState): List<CheckersMove> {
        if (state.result != null) return emptyList()
        val forced = state.continuingCaptureFrom
        if (forced != null) return captureMovesFor(state, forced)

        val captures = mutableListOf<CheckersMove>()
        forEachPiece(state, state.currentPlayer) { square, _ -> captures += captureMovesFor(state, square) }
        if (captures.isNotEmpty()) return captures

        val moves = mutableListOf<CheckersMove>()
        forEachPiece(state, state.currentPlayer) { square, _ -> moves += quietMovesFor(state, square) }
        return moves
    }

    fun legalMovesForPiece(state: CheckersState, square: CheckersSquare): List<CheckersMove> =
        legalMoves(state).filter { it.from == square }

    fun selectSquare(state: CheckersState, square: CheckersSquare): CheckersState {
        val mustContinue = state.continuingCaptureFrom
        if (mustContinue != null && mustContinue != square) return state.copy(message = "Continue the forced jump")
        val piece = pieceAt(state, square)
        return if (piece?.side == state.currentPlayer && legalMovesForPiece(state, square).isNotEmpty()) {
            state.copy(selectedSquare = square, message = "Choose a destination")
        } else {
            state.copy(message = "Select a ${state.currentPlayer.name.lowercase()} piece with a legal move")
        }
    }

    fun clearSelection(state: CheckersState): CheckersState =
        if (state.continuingCaptureFrom == null) state.copy(selectedSquare = null, message = turnMessage(state.currentPlayer)) else state

    fun applyMove(state: CheckersState, move: CheckersMove): CheckersState {
        val legal = legalMoves(state).firstOrNull { it == move } ?: return state.copy(message = "Illegal move")
        val movingPiece = pieceAt(state, legal.from) ?: return state.copy(message = "Illegal move")
        val promoted = shouldPromote(movingPiece, legal.to)
        val placedPiece = if (promoted) movingPiece.copy(isKing = true) else movingPiece
        val afterBoard = setPiece(
            setPiece(
                legal.captured?.let { setPiece(state.board, it, null) } ?: state.board,
                legal.from,
                null
            ),
            legal.to,
            placedPiece
        )
        val redCaptures = state.redCaptures + if (legal.captured != null && movingPiece.side == CheckersSide.RED) 1 else 0
        val blackCaptures = state.blackCaptures + if (legal.captured != null && movingPiece.side == CheckersSide.BLACK) 1 else 0
        val jumpedState = state.copy(
            board = afterBoard,
            selectedSquare = legal.to,
            continuingCaptureFrom = legal.to,
            redCaptures = redCaptures,
            blackCaptures = blackCaptures,
            message = if (promoted) "King promoted" else "Jump again if possible"
        )

        val moreCaptures = if (legal.isCapture) captureMovesFor(jumpedState, legal.to) else emptyList()
        if (moreCaptures.isNotEmpty()) {
            return jumpedState.copy(message = "Forced jump continues")
        }

        val next = opponent(state.currentPlayer)
        val turnEnded = jumpedState.copy(
            currentPlayer = next,
            selectedSquare = null,
            continuingCaptureFrom = null,
            message = turnMessage(next)
        )
        return finishIfGameOver(turnEnded)
    }

    fun applySelectedMove(state: CheckersState, destination: CheckersSquare): CheckersState {
        val selected = state.selectedSquare ?: return selectSquare(state, destination)
        val move = legalMovesForPiece(state, selected).firstOrNull { it.to == destination }
        return if (move == null) {
            val piece = pieceAt(state, destination)
            if (piece?.side == state.currentPlayer) selectSquare(state, destination) else state.copy(message = "Choose a highlighted destination")
        } else {
            applyMove(state, move)
        }
    }

    fun chooseAiMove(state: CheckersState, random: Random = Random.Default): CheckersMove? {
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        return when (state.config.difficulty) {
            GameDifficulty.EASY -> moves[random.nextInt(moves.size)]
            GameDifficulty.MEDIUM -> bestByScore(moves, state, random) { moveScore(state, it) }
            GameDifficulty.HARD -> {
                val depth = 5
                bestByScore(moves, state, random) { move ->
                    minimax(applyMove(state, move), depth - 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, state.currentPlayer)
                }
            }
        }
    }

    fun finishIfGameOver(state: CheckersState): CheckersState {
        if (state.result != null) return state
        val redPieces = countPieces(state, CheckersSide.RED)
        val blackPieces = countPieces(state, CheckersSide.BLACK)
        val result = when {
            redPieces == 0 -> CheckersResult(CheckersSide.BLACK, CheckersWinReason.NO_PIECES)
            blackPieces == 0 -> CheckersResult(CheckersSide.RED, CheckersWinReason.NO_PIECES)
            legalMoves(state.copy(selectedSquare = null, continuingCaptureFrom = null)).isEmpty() ->
                CheckersResult(opponent(state.currentPlayer), CheckersWinReason.NO_LEGAL_MOVES)
            else -> null
        }
        return state.copy(result = result, message = result?.let { "${it.winner.name.lowercase().replaceFirstChar { c -> c.uppercase() }} wins" } ?: state.message)
    }

    fun opponent(side: CheckersSide): CheckersSide = when (side) {
        CheckersSide.RED -> CheckersSide.BLACK
        CheckersSide.BLACK -> CheckersSide.RED
    }

    private fun quietMovesFor(state: CheckersState, square: CheckersSquare): List<CheckersMove> {
        val piece = pieceAt(state, square) ?: return emptyList()
        val moves = mutableListOf<CheckersMove>()
        if (piece.isKing) {
            diagonalDirections().forEach { (dr, dc) ->
                var row = square.row + dr
                var col = square.col + dc
                while (row in boardRange && col in boardRange && pieceAt(state, row, col) == null) {
                    moves += CheckersMove(square, CheckersSquare(row, col))
                    row += dr
                    col += dc
                }
            }
        } else {
            forwardDirections(piece.side).forEach { (dr, dc) ->
                val to = CheckersSquare(square.row + dr, square.col + dc)
                if (isPlayable(to) && pieceAt(state, to) == null) moves += CheckersMove(square, to)
            }
        }
        return moves
    }

    private fun captureMovesFor(state: CheckersState, square: CheckersSquare): List<CheckersMove> {
        val piece = pieceAt(state, square) ?: return emptyList()
        val moves = mutableListOf<CheckersMove>()
        if (piece.isKing) {
            diagonalDirections().forEach { (dr, dc) ->
                var row = square.row + dr
                var col = square.col + dc
                var captured: CheckersSquare? = null
                while (row in boardRange && col in boardRange) {
                    val current = pieceAt(state, row, col)
                    if (current == null) {
                        val jumped = captured
                        if (jumped != null) moves += CheckersMove(square, CheckersSquare(row, col), jumped)
                    } else if (current.side == piece.side || captured != null) {
                        break
                    } else {
                        captured = CheckersSquare(row, col)
                    }
                    row += dr
                    col += dc
                }
            }
        } else {
            forwardDirections(piece.side).forEach { (dr, dc) ->
                val over = CheckersSquare(square.row + dr, square.col + dc)
                val to = CheckersSquare(square.row + dr * 2, square.col + dc * 2)
                val jumped = pieceAt(state, over)
                if (isPlayable(to) && jumped != null && jumped.side != piece.side && pieceAt(state, to) == null) {
                    moves += CheckersMove(square, to, over)
                }
            }
        }
        return moves
    }

    private fun minimax(state: CheckersState, depth: Int, alpha: Int, beta: Int, maximizingSide: CheckersSide): Int {
        val resolved = finishIfGameOver(state)
        val result = resolved.result
        if (depth <= 0 || result != null) {
            return result?.let { if (it.winner == maximizingSide) 100_000 + depth else -100_000 - depth } ?: evaluate(resolved, maximizingSide)
        }
        val moves = legalMoves(resolved)
        if (moves.isEmpty()) return evaluate(resolved, maximizingSide)
        var a = alpha
        var b = beta
        return if (resolved.currentPlayer == maximizingSide) {
            var best = Int.MIN_VALUE / 2
            moves.forEach { move ->
                best = max(best, minimax(applyMove(resolved, move), depth - 1, a, b, maximizingSide))
                a = max(a, best)
                if (b <= a) return best
            }
            best
        } else {
            var best = Int.MAX_VALUE / 2
            moves.forEach { move ->
                best = min(best, minimax(applyMove(resolved, move), depth - 1, a, b, maximizingSide))
                b = min(b, best)
                if (b <= a) return best
            }
            best
        }
    }

    private fun evaluate(state: CheckersState, side: CheckersSide): Int {
        var score = 0
        for (row in boardRange) {
            for (col in boardRange) {
                val piece = pieceAt(state, row, col) ?: continue
                val value = if (piece.isKing) 175 else 100 + promotionPressure(piece.side, row)
                score += if (piece.side == side) value else -value
            }
        }
        return score + (legalMoves(state.copy(currentPlayer = side, continuingCaptureFrom = null)).size * 2) -
            (legalMoves(state.copy(currentPlayer = opponent(side), continuingCaptureFrom = null)).size * 2)
    }

    private fun moveScore(state: CheckersState, move: CheckersMove): Int {
        val piece = pieceAt(state, move.from) ?: return 0
        return (if (move.isCapture) 1_000 else 0) +
            (if (!piece.isKing && shouldPromote(piece, move.to)) 500 else 0) +
            (if (piece.isKing) 50 else promotionPressure(piece.side, move.to.row))
    }

    private fun bestByScore(
        moves: List<CheckersMove>,
        state: CheckersState,
        random: Random,
        scorer: (CheckersMove) -> Int
    ): CheckersMove {
        val scored = moves.map { it to scorer(it) }
        val bestScore = scored.maxOf { it.second }
        val bestMoves = scored.filter { it.second == bestScore }.map { it.first }
        return bestMoves[random.nextInt(bestMoves.size)]
    }

    private fun promotionPressure(side: CheckersSide, row: Int): Int = when (side) {
        CheckersSide.RED -> (7 - row) * 4
        CheckersSide.BLACK -> row * 4
    }

    private fun shouldPromote(piece: CheckersPiece, to: CheckersSquare): Boolean = !piece.isKing && when (piece.side) {
        CheckersSide.RED -> to.row == 0
        CheckersSide.BLACK -> to.row == CheckersState.BOARD_SIZE - 1
    }

    private fun forwardDirections(side: CheckersSide): List<Pair<Int, Int>> = when (side) {
        CheckersSide.RED -> listOf(-1 to -1, -1 to 1)
        CheckersSide.BLACK -> listOf(1 to -1, 1 to 1)
    }

    private fun diagonalDirections(): List<Pair<Int, Int>> = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

    private fun isPlayable(square: CheckersSquare): Boolean =
        square.row in boardRange && square.col in boardRange && CheckersState.isDarkSquare(square.row, square.col)

    private fun pieceAt(state: CheckersState, square: CheckersSquare): CheckersPiece? = pieceAt(state, square.row, square.col)

    private fun pieceAt(state: CheckersState, row: Int, col: Int): CheckersPiece? =
        if (row in boardRange && col in boardRange) state.board[row][col] else null

    private fun setPiece(board: List<List<CheckersPiece?>>, square: CheckersSquare, piece: CheckersPiece?): List<List<CheckersPiece?>> =
        board.mapIndexed { row, cells ->
            if (row != square.row) cells else cells.mapIndexed { col, current -> if (col == square.col) piece else current }
        }

    private fun forEachPiece(state: CheckersState, side: CheckersSide, block: (CheckersSquare, CheckersPiece) -> Unit) {
        for (row in boardRange) {
            for (col in boardRange) {
                val piece = pieceAt(state, row, col)
                if (piece?.side == side) block(CheckersSquare(row, col), piece)
            }
        }
    }

    private fun countPieces(state: CheckersState, side: CheckersSide): Int {
        var count = 0
        forEachPiece(state, side) { _, _ -> count++ }
        return count
    }

    private fun turnMessage(side: CheckersSide): String = "${side.name.lowercase().replaceFirstChar { it.uppercase() }} to move"
}
