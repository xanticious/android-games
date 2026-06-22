package com.xanticious.androidgames.model.games.tictactoe

import com.xanticious.androidgames.model.GameDifficulty

enum class Mark {
    X,
    O,
    EMPTY;

    fun opponent(): Mark = when (this) {
        X -> O
        O -> X
        EMPTY -> EMPTY
    }
}

data class TicTacToeConfig(
    val difficulty: GameDifficulty,
    val playerMark: Mark = Mark.X,
    val aiMark: Mark = Mark.O,
    val boardSize: Int = 3,
    val winLength: Int = 3
) {
    init {
        require(playerMark != Mark.EMPTY) { "Player mark must be X or O." }
        require(aiMark != Mark.EMPTY) { "AI mark must be X or O." }
        require(playerMark != aiMark) { "Player and AI marks must differ." }
        require(boardSize == 3) { "Classic Tic Tac Toe uses a 3x3 board." }
        require(winLength == 3) { "Classic Tic Tac Toe requires three in a row." }
    }
}

sealed interface TicTacToeResult {
    data object InProgress : TicTacToeResult
    data object Draw : TicTacToeResult
    data class Win(val winner: Mark, val winningCells: List<Int>) : TicTacToeResult
}

data class TicTacToeState(
    val board: List<Mark>,
    val config: TicTacToeConfig,
    val currentMark: Mark,
    val result: TicTacToeResult,
    val lastMove: Int = -1
) {
    companion object {
        const val ClassicBoardSize = 3
        const val ClassicCellCount = ClassicBoardSize * ClassicBoardSize

        fun initial(config: TicTacToeConfig): TicTacToeState = TicTacToeState(
            board = List(ClassicCellCount) { Mark.EMPTY },
            config = config,
            currentMark = Mark.X,
            result = TicTacToeResult.InProgress
        )
    }
}
