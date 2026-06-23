package com.xanticious.androidgames.controller.games.hex

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.hex.HexCell
import com.xanticious.androidgames.model.games.hex.HexConfig
import com.xanticious.androidgames.model.games.hex.HexEvent
import com.xanticious.androidgames.model.games.hex.HexMove
import com.xanticious.androidgames.model.games.hex.HexResult
import com.xanticious.androidgames.model.games.hex.HexState
import com.xanticious.androidgames.model.games.hex.HexStep
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.random.Random

/** Pure Hex rules, connection detection, and deterministic local AI. */
class HexController(private val random: Random = Random(0)) {

    fun configFor(difficulty: GameDifficulty): HexConfig = when (difficulty) {
        GameDifficulty.EASY -> HexConfig(boardSize = 9, difficulty = difficulty)
        GameDifficulty.MEDIUM -> HexConfig(boardSize = 11, difficulty = difficulty)
        GameDifficulty.HARD -> HexConfig(boardSize = 13, difficulty = difficulty)
    }

    fun legalMoves(state: HexState): List<HexMove> = state.board.asSequence()
        .mapIndexed { index, cell -> index to cell }
        .filter { it.second == HexCell.EMPTY }
        .map { (index, _) -> HexMove(index / state.boardSize, index % state.boardSize) }
        .toList()

    fun placeStone(state: HexState, move: HexMove): HexStep = placeStone(state, move.row, move.col, state.currentPlayer)

    fun placeStone(state: HexState, row: Int, col: Int, side: HexCell): HexStep {
        if (state.result != HexResult.IN_PROGRESS || side == HexCell.EMPTY || !isInside(state.boardSize, row, col)) {
            return HexStep(state, HexEvent.INVALID_MOVE)
        }
        if (state.cell(row, col) != HexCell.EMPTY || side != state.currentPlayer) {
            return HexStep(state, HexEvent.INVALID_MOVE)
        }

        val index = row * state.boardSize + col
        val nextBoard = state.board.toMutableList().also { it[index] = side }
        val placed = state.copy(board = nextBoard, lastMove = HexMove(row, col))
        return when {
            hasConnection(placed, HexCell.PLAYER) -> HexStep(
                placed.copy(result = HexResult.PLAYER_WON),
                HexEvent.PLAYER_WON
            )
            hasConnection(placed, HexCell.AI) -> HexStep(
                placed.copy(result = HexResult.AI_WON),
                HexEvent.AI_WON
            )
            else -> HexStep(placed.copy(currentPlayer = opponent(side)), HexEvent.NONE)
        }
    }

    fun aiMove(state: HexState, config: HexConfig, random: Random = this.random): HexMove? {
        val moves = legalMoves(state)
        if (moves.isEmpty() || state.result != HexResult.IN_PROGRESS) return null
        return when (config.difficulty) {
            GameDifficulty.EASY -> moves[random.nextInt(moves.size)]
            GameDifficulty.MEDIUM -> tacticalMove(state, moves, random, bridgeWeight = 2)
            GameDifficulty.HARD -> tacticalMove(state, moves, random, bridgeWeight = 5)
        }
    }

    fun hasConnection(state: HexState, side: HexCell): Boolean {
        if (side == HexCell.EMPTY) return false
        val size = state.boardSize
        val visited = BooleanArray(size * size)
        val stack = ArrayDeque<HexMove>()

        for (i in 0 until size) {
            val start = if (side == HexCell.PLAYER) HexMove(i, 0) else HexMove(0, i)
            if (state.cell(start.row, start.col) == side) {
                visited[start.row * size + start.col] = true
                stack.add(start)
            }
        }

        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            if (side == HexCell.PLAYER && cell.col == size - 1) return true
            if (side == HexCell.AI && cell.row == size - 1) return true
            for (neighbor in neighbors(size, cell.row, cell.col)) {
                val index = neighbor.row * size + neighbor.col
                if (!visited[index] && state.cell(neighbor.row, neighbor.col) == side) {
                    visited[index] = true
                    stack.add(neighbor)
                }
            }
        }
        return false
    }

    fun neighbors(boardSize: Int, row: Int, col: Int): List<HexMove> {
        val deltas = listOf(-1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0)
        return deltas.map { (dr, dc) -> HexMove(row + dr, col + dc) }
            .filter { isInside(boardSize, it.row, it.col) }
    }

    private fun tacticalMove(state: HexState, moves: List<HexMove>, random: Random, bridgeWeight: Int): HexMove {
        immediateWinningMove(state, HexCell.AI)?.let { return it }
        immediateWinningMove(state, HexCell.PLAYER)?.let { return it }

        val ownDistance = shortestConnectionCost(state, HexCell.AI)
        val opponentDistance = shortestConnectionCost(state, HexCell.PLAYER)
        val bestScore = moves.maxOf { move ->
            val ownState = stateAfterVirtualMove(state, move, HexCell.AI)
            val blockState = stateAfterVirtualMove(state, move, HexCell.PLAYER)
            val ownGain = ownDistance - shortestConnectionCost(ownState, HexCell.AI)
            val blockGain = opponentDistance - shortestConnectionCost(blockState, HexCell.PLAYER)
            ownGain * 4 + blockGain * 5 + adjacentCount(state, move, HexCell.AI) * bridgeWeight + centerScore(state, move)
        }
        return moves.filter { move ->
            val ownState = stateAfterVirtualMove(state, move, HexCell.AI)
            val blockState = stateAfterVirtualMove(state, move, HexCell.PLAYER)
            val score = (ownDistance - shortestConnectionCost(ownState, HexCell.AI)) * 4 +
                (opponentDistance - shortestConnectionCost(blockState, HexCell.PLAYER)) * 5 +
                adjacentCount(state, move, HexCell.AI) * bridgeWeight + centerScore(state, move)
            score == bestScore
        }.let { tied -> tied[random.nextInt(tied.size)] }
    }

    private fun immediateWinningMove(state: HexState, side: HexCell): HexMove? = legalMoves(state).firstOrNull { move ->
        hasConnection(stateAfterVirtualMove(state, move, side), side)
    }

    private fun stateAfterVirtualMove(state: HexState, move: HexMove, side: HexCell): HexState {
        val board = state.board.toMutableList().also { it[move.row * state.boardSize + move.col] = side }
        return state.copy(board = board)
    }

    private fun adjacentCount(state: HexState, move: HexMove, side: HexCell): Int =
        neighbors(state.boardSize, move.row, move.col).count { state.cell(it.row, it.col) == side }

    private fun centerScore(state: HexState, move: HexMove): Int {
        val center = (state.boardSize - 1) / 2
        return state.boardSize - abs(move.row - center) - abs(move.col - center)
    }

    private fun shortestConnectionCost(state: HexState, side: HexCell): Int {
        val size = state.boardSize
        val distances = IntArray(size * size) { Int.MAX_VALUE / 4 }
        val queue = PriorityQueue<Pair<Int, HexMove>>(compareBy { it.first })
        for (i in 0 until size) {
            val move = if (side == HexCell.PLAYER) HexMove(i, 0) else HexMove(0, i)
            val cost = traversalCost(state.cell(move.row, move.col), side)
            distances[move.row * size + move.col] = cost
            queue.add(cost to move)
        }
        while (queue.isNotEmpty()) {
            val (cost, move) = queue.poll()
            val index = move.row * size + move.col
            if (cost != distances[index]) continue
            if (side == HexCell.PLAYER && move.col == size - 1) return cost
            if (side == HexCell.AI && move.row == size - 1) return cost
            for (neighbor in neighbors(size, move.row, move.col)) {
                val nextIndex = neighbor.row * size + neighbor.col
                val nextCost = cost + traversalCost(state.cell(neighbor.row, neighbor.col), side)
                if (nextCost < distances[nextIndex]) {
                    distances[nextIndex] = nextCost
                    queue.add(nextCost to neighbor)
                }
            }
        }
        return Int.MAX_VALUE / 8
    }

    private fun traversalCost(cell: HexCell, side: HexCell): Int = when (cell) {
        side -> 0
        HexCell.EMPTY -> 1
        else -> 1000
    }

    private fun opponent(side: HexCell): HexCell = if (side == HexCell.PLAYER) HexCell.AI else HexCell.PLAYER

    private fun isInside(boardSize: Int, row: Int, col: Int): Boolean = row in 0 until boardSize && col in 0 until boardSize
}
