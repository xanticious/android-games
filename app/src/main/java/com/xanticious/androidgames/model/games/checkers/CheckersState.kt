package com.xanticious.androidgames.model.games.checkers

import com.xanticious.androidgames.model.GameDifficulty

enum class CheckersSide { RED, BLACK }

data class CheckersSquare(val row: Int, val col: Int)

data class CheckersPiece(val side: CheckersSide, val isKing: Boolean = false)

data class CheckersMove(
    val from: CheckersSquare,
    val to: CheckersSquare,
    val captured: CheckersSquare? = null
) {
    val isCapture: Boolean get() = captured != null
}

enum class CheckersWinReason { NO_PIECES, NO_LEGAL_MOVES }

data class CheckersResult(
    val winner: CheckersSide,
    val reason: CheckersWinReason
)

data class CheckersConfig(
    val difficulty: GameDifficulty,
    val playerSide: CheckersSide = CheckersSide.RED,
    val trainingWheels: Boolean = true,
    val showCoordinates: Boolean = true,
    val moveConfirmation: Boolean = false
)

data class CheckersState(
    val board: List<List<CheckersPiece?>>,
    val currentPlayer: CheckersSide,
    val selectedSquare: CheckersSquare?,
    val config: CheckersConfig,
    val result: CheckersResult?,
    val continuingCaptureFrom: CheckersSquare? = null,
    val redCaptures: Int = 0,
    val blackCaptures: Int = 0,
    val message: String = "Red to move"
) {
    companion object {
        const val BOARD_SIZE = 8

        fun initial(config: CheckersConfig = CheckersConfig(GameDifficulty.MEDIUM)): CheckersState {
            val board = List(BOARD_SIZE) { row ->
                List<CheckersPiece?>(BOARD_SIZE) { col ->
                    when {
                        !isDarkSquare(row, col) -> null
                        row < 3 -> CheckersPiece(CheckersSide.BLACK)
                        row > 4 -> CheckersPiece(CheckersSide.RED)
                        else -> null
                    }
                }
            }
            return CheckersState(
                board = board,
                currentPlayer = CheckersSide.RED,
                selectedSquare = null,
                config = config,
                result = null
            )
        }

        fun empty(config: CheckersConfig = CheckersConfig(GameDifficulty.MEDIUM)): CheckersState = CheckersState(
            board = List(BOARD_SIZE) { List<CheckersPiece?>(BOARD_SIZE) { null } },
            currentPlayer = CheckersSide.RED,
            selectedSquare = null,
            config = config,
            result = null
        )

        fun isDarkSquare(row: Int, col: Int): Boolean = (row + col) % 2 == 1
    }
}
