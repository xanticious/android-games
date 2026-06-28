package com.xanticious.androidgames.model.games.reversi

import com.xanticious.androidgames.model.GameDifficulty

const val REVERSI_BOARD_SIZE = 8

enum class Disc { BLACK, WHITE, EMPTY }

data class ReversiPosition(val row: Int, val col: Int)

data class ReversiMove(
    val position: ReversiPosition,
    val flips: List<ReversiPosition>
)

data class ReversiConfig(
    val difficulty: GameDifficulty,
    val playerDisc: Disc = Disc.BLACK,
    val aiDisc: Disc = Disc.WHITE,
    val showHints: Boolean = true,
    val randomSeed: Int = 0
)

data class ReversiResult(
    val blackCount: Int,
    val whiteCount: Int,
    val winner: Disc
)

data class ReversiState(
    val board: List<List<Disc>>,
    val currentPlayer: Disc,
    val config: ReversiConfig,
    val blackCount: Int,
    val whiteCount: Int,
    val result: ReversiResult? = null,
    val lastMove: ReversiPosition? = null,
    val lastPass: Disc? = null
) {
    companion object {
        fun initial(config: ReversiConfig): ReversiState {
            val board = List(REVERSI_BOARD_SIZE) { row ->
                List(REVERSI_BOARD_SIZE) { col ->
                    when {
                        row == 3 && col == 3 -> Disc.WHITE
                        row == 3 && col == 4 -> Disc.BLACK
                        row == 4 && col == 3 -> Disc.BLACK
                        row == 4 && col == 4 -> Disc.WHITE
                        else -> Disc.EMPTY
                    }
                }
            }
            return fromBoard(board = board, currentPlayer = Disc.BLACK, config = config)
        }

        fun fromBoard(
            board: List<List<Disc>>,
            currentPlayer: Disc,
            config: ReversiConfig,
            result: ReversiResult? = null,
            lastMove: ReversiPosition? = null,
            lastPass: Disc? = null
        ): ReversiState {
            val black = board.sumOf { row -> row.count { it == Disc.BLACK } }
            val white = board.sumOf { row -> row.count { it == Disc.WHITE } }
            return ReversiState(board, currentPlayer, config, black, white, result, lastMove, lastPass)
        }
    }
}
