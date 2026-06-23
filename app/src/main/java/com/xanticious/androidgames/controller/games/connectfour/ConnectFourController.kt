package com.xanticious.androidgames.controller.games.connectfour

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.connectfour.ConnectFourCell
import com.xanticious.androidgames.model.games.connectfour.ConnectFourConfig
import com.xanticious.androidgames.model.games.connectfour.ConnectFourMove
import com.xanticious.androidgames.model.games.connectfour.ConnectFourResult
import com.xanticious.androidgames.model.games.connectfour.ConnectFourState
import com.xanticious.androidgames.model.games.connectfour.ConnectFourStep
import com.xanticious.androidgames.model.games.connectfour.Disc
import kotlin.math.abs
import kotlin.random.Random

/** Pure Connect Four rules and local AI. */
class ConnectFourController {

    fun configFor(difficulty: GameDifficulty): ConnectFourConfig = when (difficulty) {
        GameDifficulty.EASY -> ConnectFourConfig(aiSearchDepth = 1)
        GameDifficulty.MEDIUM -> ConnectFourConfig(aiSearchDepth = 2)
        GameDifficulty.HARD -> ConnectFourConfig(aiSearchDepth = 5)
    }

    fun legalColumns(state: ConnectFourState): List<Int> =
        state.grid.firstOrNull()?.indices?.filter { column -> state.grid[0][column] == Disc.EMPTY } ?: emptyList()

    fun dropDisc(
        state: ConnectFourState,
        config: ConnectFourConfig,
        column: Int,
        disc: Disc = state.currentTurn
    ): ConnectFourStep {
        if (state.isGameOver || disc == Disc.EMPTY || column !in 0 until config.columns) {
            return ConnectFourStep(state, accepted = false)
        }
        val row = landingRow(state, config, column) ?: return ConnectFourStep(state, accepted = false)
        val newGrid = state.grid.mapIndexed { r, cells ->
            if (r == row) cells.mapIndexed { c, value -> if (c == column) disc else value } else cells
        }
        val line = winningLine(newGrid, config, disc)
        val result = when {
            line.isNotEmpty() && disc == Disc.PLAYER -> ConnectFourResult.PLAYER_WIN
            line.isNotEmpty() && disc == Disc.AI -> ConnectFourResult.AI_WIN
            newGrid.all { cells -> cells.all { it != Disc.EMPTY } } -> ConnectFourResult.DRAW
            else -> ConnectFourResult.NONE
        }
        val nextTurn = when {
            result != ConnectFourResult.NONE -> disc
            disc == Disc.PLAYER -> Disc.AI
            else -> Disc.PLAYER
        }
        return ConnectFourStep(
            state = state.copy(
                grid = newGrid,
                currentTurn = nextTurn,
                moveCount = state.moveCount + 1,
                lastMove = ConnectFourMove(column = column, row = row, disc = disc),
                result = result,
                winningLine = line
            ),
            accepted = true
        )
    }

    fun landingRow(state: ConnectFourState, config: ConnectFourConfig, column: Int): Int? {
        if (column !in 0 until config.columns) return null
        return (config.rows - 1 downTo 0).firstOrNull { row -> state.grid[row][column] == Disc.EMPTY }
    }

    fun resultFor(state: ConnectFourState, config: ConnectFourConfig): ConnectFourResult {
        val playerLine = winningLine(state.grid, config, Disc.PLAYER)
        if (playerLine.isNotEmpty()) return ConnectFourResult.PLAYER_WIN
        val aiLine = winningLine(state.grid, config, Disc.AI)
        if (aiLine.isNotEmpty()) return ConnectFourResult.AI_WIN
        return if (legalColumns(state).isEmpty()) ConnectFourResult.DRAW else ConnectFourResult.NONE
    }

    fun winningLine(grid: List<List<Disc>>, config: ConnectFourConfig, disc: Disc): List<ConnectFourCell> {
        if (disc == Disc.EMPTY) return emptyList()
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (row in 0 until config.rows) {
            for (column in 0 until config.columns) {
                if (grid[row][column] != disc) continue
                for ((dr, dc) in directions) {
                    val cells = (0 until config.connectLength).map { i ->
                        ConnectFourCell(row = row + dr * i, column = column + dc * i)
                    }
                    if (cells.all { it.row in 0 until config.rows && it.column in 0 until config.columns && grid[it.row][it.column] == disc }) {
                        return cells
                    }
                }
            }
        }
        return emptyList()
    }

    fun aiMove(
        state: ConnectFourState,
        config: ConnectFourConfig,
        difficulty: GameDifficulty,
        random: Random = Random.Default
    ): Int? = when (difficulty) {
        GameDifficulty.EASY -> legalColumns(state).randomOrNull(random)
        GameDifficulty.MEDIUM -> tacticalMove(state, config, random)
        GameDifficulty.HARD -> minimaxMove(state, config, random)
    }

    fun aiMove(
        state: ConnectFourState,
        config: ConnectFourConfig,
        random: Random = Random.Default
    ): Int? = when {
        config.aiSearchDepth <= 1 -> legalColumns(state).randomOrNull(random)
        config.aiSearchDepth == 2 -> tacticalMove(state, config, random)
        else -> minimaxMove(state, config, random)
    }

    private fun tacticalMove(state: ConnectFourState, config: ConnectFourConfig, random: Random): Int? {
        val legal = legalColumns(state)
        if (legal.isEmpty()) return null
        immediateWinningMove(state, config, Disc.AI)?.let { return it }
        immediateWinningMove(state, config, Disc.PLAYER)?.let { return it }
        val center = config.columns / 2
        val bestDistance = legal.minOf { abs(it - center) }
        return legal.filter { abs(it - center) == bestDistance }.random(random)
    }

    private fun immediateWinningMove(state: ConnectFourState, config: ConnectFourConfig, disc: Disc): Int? =
        legalColumns(state).firstOrNull { column ->
            val stepped = dropDisc(state, config, column, disc).state
            stepped.result == if (disc == Disc.AI) ConnectFourResult.AI_WIN else ConnectFourResult.PLAYER_WIN
        }

    private fun minimaxMove(state: ConnectFourState, config: ConnectFourConfig, random: Random): Int? {
        val legal = legalColumns(state)
        if (legal.isEmpty()) return null
        immediateWinningMove(state, config, Disc.AI)?.let { return it }
        immediateWinningMove(state, config, Disc.PLAYER)?.let { return it }
        var bestScore = Int.MIN_VALUE
        val bestMoves = mutableListOf<Int>()
        for (column in orderedColumns(legal, config)) {
            val next = dropDisc(state, config, column, Disc.AI).state
            val score = minimax(next, config, config.aiSearchDepth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, maximizing = false)
            when {
                score > bestScore -> {
                    bestScore = score
                    bestMoves.clear()
                    bestMoves.add(column)
                }
                score == bestScore -> bestMoves.add(column)
            }
        }
        return bestMoves.randomOrNull(random)
    }

    private fun minimax(
        state: ConnectFourState,
        config: ConnectFourConfig,
        depth: Int,
        alphaStart: Int,
        betaStart: Int,
        maximizing: Boolean
    ): Int {
        if (state.result != ConnectFourResult.NONE || depth == 0) {
            return terminalOrHeuristicScore(state, config, depth)
        }
        val legal = orderedColumns(legalColumns(state), config)
        if (legal.isEmpty()) return 0
        var alpha = alphaStart
        var beta = betaStart
        return if (maximizing) {
            var best = Int.MIN_VALUE + 1
            for (column in legal) {
                val next = dropDisc(state, config, column, Disc.AI).state
                best = maxOf(best, minimax(next, config, depth - 1, alpha, beta, maximizing = false))
                alpha = maxOf(alpha, best)
                if (beta <= alpha) break
            }
            best
        } else {
            var best = Int.MAX_VALUE - 1
            for (column in legal) {
                val next = dropDisc(state, config, column, Disc.PLAYER).state
                best = minOf(best, minimax(next, config, depth - 1, alpha, beta, maximizing = true))
                beta = minOf(beta, best)
                if (beta <= alpha) break
            }
            best
        }
    }

    private fun terminalOrHeuristicScore(state: ConnectFourState, config: ConnectFourConfig, depth: Int): Int = when (state.result) {
        ConnectFourResult.AI_WIN -> 100_000 + depth
        ConnectFourResult.PLAYER_WIN -> -100_000 - depth
        ConnectFourResult.DRAW -> 0
        ConnectFourResult.NONE -> heuristicScore(state, config)
    }

    private fun heuristicScore(state: ConnectFourState, config: ConnectFourConfig): Int {
        val center = config.columns / 2
        var score = state.grid.sumOf { row -> if (row[center] == Disc.AI) 4 else if (row[center] == Disc.PLAYER) -4 else 0 }
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (row in 0 until config.rows) {
            for (column in 0 until config.columns) {
                for ((dr, dc) in directions) {
                    val cells = (0 until config.connectLength).map { i -> row + dr * i to column + dc * i }
                    if (cells.any { (r, c) -> r !in 0 until config.rows || c !in 0 until config.columns }) continue
                    val discs = cells.map { (r, c) -> state.grid[r][c] }
                    val ai = discs.count { it == Disc.AI }
                    val player = discs.count { it == Disc.PLAYER }
                    val empty = discs.count { it == Disc.EMPTY }
                    score += windowScore(ai, player, empty)
                }
            }
        }
        return score
    }

    private fun windowScore(ai: Int, player: Int, empty: Int): Int = when {
        ai == 4 -> 100_000
        player == 4 -> -100_000
        ai == 3 && empty == 1 -> 90
        player == 3 && empty == 1 -> -120
        ai == 2 && empty == 2 -> 15
        player == 2 && empty == 2 -> -20
        ai == 1 && empty == 3 -> 2
        player == 1 && empty == 3 -> -2
        else -> 0
    }

    private fun orderedColumns(columns: List<Int>, config: ConnectFourConfig): List<Int> {
        val center = config.columns / 2
        return columns.sortedWith(compareBy({ abs(it - center) }, { it }))
    }
}
