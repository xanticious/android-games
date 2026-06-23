package com.xanticious.androidgames.model.games.gomoku

import com.xanticious.androidgames.model.GameDifficulty

enum class Stone {
    EMPTY,
    BLACK,
    WHITE;

    fun opponent(): Stone = when (this) {
        BLACK -> WHITE
        WHITE -> BLACK
        EMPTY -> EMPTY
    }
}

data class GomokuPoint(val row: Int, val col: Int)

enum class GomokuResult { IN_PROGRESS, BLACK_WON, WHITE_WON, DRAW }

data class GomokuConfig(
    val boardSize: Int = DefaultBoardSize,
    val aiStrength: GameDifficulty = GameDifficulty.MEDIUM
) {
    init {
        require(boardSize == DefaultBoardSize || boardSize == LargeBoardSize) { "Gomoku board size must be 15 or 19." }
    }

    companion object {
        const val DefaultBoardSize = 15
        const val LargeBoardSize = 19
    }
}

data class GomokuState(
    val board: List<List<Stone>>,
    val turn: Stone,
    val config: GomokuConfig,
    val lastMove: GomokuPoint? = null,
    val winningLine: List<GomokuPoint> = emptyList(),
    val result: GomokuResult = GomokuResult.IN_PROGRESS,
    val moveCount: Int = 0
) {
    init {
        require(board.size == config.boardSize) { "Board row count must match config." }
        require(board.all { it.size == config.boardSize }) { "Board column count must match config." }
        require(turn != Stone.EMPTY) { "Gomoku turn must be black or white." }
    }

    fun stoneAt(point: GomokuPoint): Stone = board[point.row][point.col]
    fun stoneAt(row: Int, col: Int): Stone = board[row][col]

    companion object {
        fun initial(config: GomokuConfig = GomokuConfig()): GomokuState = GomokuState(
            board = List(config.boardSize) { List(config.boardSize) { Stone.EMPTY } },
            turn = Stone.BLACK,
            config = config
        )
    }
}

data class GomokuPlacement(
    val state: GomokuState,
    val accepted: Boolean,
    val message: String = ""
)
