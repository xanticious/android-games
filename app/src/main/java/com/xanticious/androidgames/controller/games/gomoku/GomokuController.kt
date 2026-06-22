package com.xanticious.androidgames.controller.games.gomoku

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.gomoku.GomokuConfig
import com.xanticious.androidgames.model.games.gomoku.GomokuPlacement
import com.xanticious.androidgames.model.games.gomoku.GomokuPoint
import com.xanticious.androidgames.model.games.gomoku.GomokuResult
import com.xanticious.androidgames.model.games.gomoku.GomokuState
import com.xanticious.androidgames.model.games.gomoku.Stone
import kotlin.math.abs
import kotlin.random.Random

class GomokuController {
    private val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    fun configFor(difficulty: GameDifficulty, boardSize: Int = GomokuConfig.DefaultBoardSize): GomokuConfig =
        GomokuConfig(boardSize = boardSize, aiStrength = difficulty)

    fun initialState(config: GomokuConfig = GomokuConfig()): GomokuState = GomokuState.initial(config)

    fun placeStone(state: GomokuState, row: Int, col: Int): GomokuPlacement {
        if (state.result != GomokuResult.IN_PROGRESS) {
            return GomokuPlacement(state, accepted = false, message = "Match is already over.")
        }
        if (!isInBounds(state.config.boardSize, row, col)) {
            return GomokuPlacement(state, accepted = false, message = "Choose a point on the board.")
        }
        if (state.board[row][col] != Stone.EMPTY) {
            return GomokuPlacement(state, accepted = false, message = "That point is occupied.")
        }

        val move = GomokuPoint(row, col)
        val board = state.board.mapIndexed { r, cells ->
            if (r == row) cells.mapIndexed { c, stone -> if (c == col) state.turn else stone } else cells
        }
        val line = winningLine(board, move, state.turn)
        val result = when {
            line.isNotEmpty() && state.turn == Stone.BLACK -> GomokuResult.BLACK_WON
            line.isNotEmpty() -> GomokuResult.WHITE_WON
            isFull(board) -> GomokuResult.DRAW
            else -> GomokuResult.IN_PROGRESS
        }
        val nextTurn = if (result == GomokuResult.IN_PROGRESS) state.turn.opponent() else state.turn
        return GomokuPlacement(
            state = state.copy(
                board = board,
                turn = nextTurn,
                lastMove = move,
                winningLine = line,
                result = result,
                moveCount = state.moveCount + 1
            ),
            accepted = true
        )
    }

    fun chooseAiMove(state: GomokuState, seed: Long): GomokuPoint? = chooseAiMove(state, Random(seed))

    fun chooseAiMove(state: GomokuState, random: Random): GomokuPoint? {
        if (state.result != GomokuResult.IN_PROGRESS || state.turn == Stone.EMPTY) return null
        val empty = emptyPoints(state.board)
        if (empty.isEmpty()) return null
        val center = state.config.boardSize / 2
        if (empty.size == state.config.boardSize * state.config.boardSize) return GomokuPoint(center, center)

        immediateWinningMove(state, state.turn, empty)?.let { return it }
        immediateWinningMove(state, state.turn.opponent(), empty)?.let { return it }

        val settings = AiSettings.forDifficulty(state.config.aiStrength)
        val candidates = candidatePoints(state.board, settings.candidateRadius).ifEmpty { empty }
        val scored = candidates.map { point ->
            val attack = evaluateMove(state.board, point, state.turn, settings)
            val defense = evaluateMove(state.board, point, state.turn.opponent(), settings)
            val centerScore = centerInfluence(point, state.config.boardSize)
            val forkScore = if (settings.forkWeight > 0) forkPotential(state.board, point, state.turn) * settings.forkWeight else 0
            point to (attack + (defense * settings.defenseWeight).toInt() + centerScore + forkScore)
        }
        val bestScore = scored.maxOf { it.second }
        val best = scored.filter { it.second == bestScore }.map { it.first }
        return best[random.nextInt(best.size)]
    }

    fun winningLine(board: List<List<Stone>>, move: GomokuPoint, stone: Stone, exactlyFive: Boolean = false): List<GomokuPoint> {
        if (stone == Stone.EMPTY) return emptyList()
        for ((dr, dc) in directions) {
            val points = collectLine(board, move, stone, dr, dc)
            if ((!exactlyFive && points.size >= 5) || (exactlyFive && points.size == 5)) return points
        }
        return emptyList()
    }

    private fun immediateWinningMove(state: GomokuState, stone: Stone, empty: List<GomokuPoint>): GomokuPoint? =
        empty.firstOrNull { point ->
            val board = boardWith(state.board, point, stone)
            winningLine(board, point, stone).isNotEmpty()
        }

    private fun evaluateMove(board: List<List<Stone>>, point: GomokuPoint, stone: Stone, settings: AiSettings): Int {
        val placed = boardWith(board, point, stone)
        return directions.sumOf { (dr, dc) ->
            val line = collectLine(placed, point, stone, dr, dc)
            val openEnds = openEnds(placed, line, dr, dc)
            lineScore(line.size, openEnds, settings)
        }
    }

    private fun lineScore(length: Int, openEnds: Int, settings: AiSettings): Int = when {
        length >= 5 -> 1_000_000
        length == 4 && openEnds == 2 -> 80_000 * settings.threatDepth
        length == 4 && openEnds == 1 -> 20_000 * settings.threatDepth
        length == 3 && openEnds == 2 -> settings.openThreeScore
        length == 3 && openEnds == 1 -> settings.openThreeScore / 3
        length == 2 && openEnds == 2 -> 400
        length == 2 && openEnds == 1 -> 120
        else -> 10 + openEnds
    }

    private fun forkPotential(board: List<List<Stone>>, point: GomokuPoint, stone: Stone): Int {
        val placed = boardWith(board, point, stone)
        return directions.count { (dr, dc) ->
            val line = collectLine(placed, point, stone, dr, dc)
            line.size >= 3 && openEnds(placed, line, dr, dc) == 2
        }
    }

    private fun centerInfluence(point: GomokuPoint, boardSize: Int): Int {
        val center = (boardSize - 1) / 2
        val distance = abs(point.row - center) + abs(point.col - center)
        return (boardSize - distance).coerceAtLeast(0)
    }

    private fun collectLine(board: List<List<Stone>>, move: GomokuPoint, stone: Stone, dr: Int, dc: Int): List<GomokuPoint> {
        val backward = collectDirection(board, move, stone, -dr, -dc).asReversed()
        val forward = collectDirection(board, move, stone, dr, dc)
        return backward + move + forward
    }

    private fun collectDirection(board: List<List<Stone>>, move: GomokuPoint, stone: Stone, dr: Int, dc: Int): List<GomokuPoint> {
        val points = mutableListOf<GomokuPoint>()
        var row = move.row + dr
        var col = move.col + dc
        while (isInBounds(board.size, row, col) && board[row][col] == stone) {
            points += GomokuPoint(row, col)
            row += dr
            col += dc
        }
        return points
    }

    private fun openEnds(board: List<List<Stone>>, line: List<GomokuPoint>, dr: Int, dc: Int): Int {
        val first = line.firstOrNull() ?: return 0
        val last = line.lastOrNull() ?: return 0
        val before = GomokuPoint(first.row - dr, first.col - dc)
        val after = GomokuPoint(last.row + dr, last.col + dc)
        return listOf(before, after).count { point ->
            isInBounds(board.size, point.row, point.col) && board[point.row][point.col] == Stone.EMPTY
        }
    }

    private fun candidatePoints(board: List<List<Stone>>, radius: Int): List<GomokuPoint> {
        val size = board.size
        val candidates = mutableSetOf<GomokuPoint>()
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board[row][col] != Stone.EMPTY) {
                    for (dr in -radius..radius) {
                        for (dc in -radius..radius) {
                            val nr = row + dr
                            val nc = col + dc
                            if (isInBounds(size, nr, nc) && board[nr][nc] == Stone.EMPTY) candidates += GomokuPoint(nr, nc)
                        }
                    }
                }
            }
        }
        return candidates.toList()
    }

    private fun emptyPoints(board: List<List<Stone>>): List<GomokuPoint> = board.indices.flatMap { row ->
        board[row].indices.mapNotNull { col -> if (board[row][col] == Stone.EMPTY) GomokuPoint(row, col) else null }
    }

    private fun boardWith(board: List<List<Stone>>, point: GomokuPoint, stone: Stone): List<List<Stone>> =
        board.mapIndexed { row, cells ->
            if (row == point.row) cells.mapIndexed { col, current -> if (col == point.col) stone else current } else cells
        }

    private fun isFull(board: List<List<Stone>>): Boolean = board.all { row -> row.all { it != Stone.EMPTY } }

    private fun isInBounds(size: Int, row: Int, col: Int): Boolean = row in 0 until size && col in 0 until size

    private data class AiSettings(
        val candidateRadius: Int,
        val defenseWeight: Double,
        val threatDepth: Int,
        val openThreeScore: Int,
        val forkWeight: Int
    ) {
        companion object {
            fun forDifficulty(difficulty: GameDifficulty): AiSettings = when (difficulty) {
                GameDifficulty.EASY -> AiSettings(1, 0.8, 1, 900, 0)
                GameDifficulty.MEDIUM -> AiSettings(2, 1.1, 2, 4_000, 2_000)
                GameDifficulty.HARD -> AiSettings(2, 1.35, 3, 8_000, 6_000)
            }
        }
    }
}
