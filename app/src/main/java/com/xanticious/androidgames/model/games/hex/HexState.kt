package com.xanticious.androidgames.model.games.hex

/** Cell contents for the Hex rhombus board. */
enum class HexCell { EMPTY, PLAYER, AI }

/** Match result. Hex never draws; a filled valid board always has a winner. */
enum class HexResult { IN_PROGRESS, PLAYER_WON, AI_WON }

data class HexMove(val row: Int, val col: Int)

data class HexConfig(
    val boardSize: Int,
    val difficulty: com.xanticious.androidgames.model.GameDifficulty
)

data class HexState(
    val boardSize: Int,
    val board: List<HexCell>,
    val currentPlayer: HexCell,
    val result: HexResult,
    val lastMove: HexMove?
) {
    init {
        require(boardSize > 1) { "Hex board must be at least 2x2." }
        require(board.size == boardSize * boardSize) { "Board cell count must match board size." }
        require(currentPlayer == HexCell.PLAYER || currentPlayer == HexCell.AI) { "Current player must be a side." }
    }

    fun cell(row: Int, col: Int): HexCell = board[row * boardSize + col]

    companion object {
        fun initial(config: HexConfig): HexState = HexState(
            boardSize = config.boardSize,
            board = List(config.boardSize * config.boardSize) { HexCell.EMPTY },
            currentPlayer = HexCell.PLAYER,
            result = HexResult.IN_PROGRESS,
            lastMove = null
        )
    }
}

enum class HexEvent { NONE, INVALID_MOVE, PLAYER_WON, AI_WON }

data class HexStep(val state: HexState, val event: HexEvent)
