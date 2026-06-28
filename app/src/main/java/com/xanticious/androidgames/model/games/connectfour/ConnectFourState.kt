package com.xanticious.androidgames.model.games.connectfour

/** Contents of a Connect Four board slot. */
enum class Disc { EMPTY, PLAYER, AI }

/** Final outcome for a match, or [NONE] while play continues. */
enum class ConnectFourResult { NONE, PLAYER_WIN, AI_WIN, DRAW }

/** Immutable board coordinate. Row 0 is the top of the grid. */
data class ConnectFourCell(val row: Int, val column: Int)

/** Tuning values derived from the selected difficulty. */
data class ConnectFourConfig(
    val columns: Int = 7,
    val rows: Int = 6,
    val connectLength: Int = 4,
    val aiSearchDepth: Int = 1
)

/** Last legal drop applied to the board. */
data class ConnectFourMove(val column: Int, val row: Int, val disc: Disc)

/** Result of attempting to drop a disc. */
data class ConnectFourStep(
    val state: ConnectFourState,
    val accepted: Boolean
)

/** Complete immutable Connect Four match state. */
data class ConnectFourState(
    val grid: List<List<Disc>>,
    val currentTurn: Disc,
    val moveCount: Int,
    val lastMove: ConnectFourMove?,
    val result: ConnectFourResult,
    val winningLine: List<ConnectFourCell>
) {
    val isGameOver: Boolean get() = result != ConnectFourResult.NONE

    companion object {
        fun initial(config: ConnectFourConfig = ConnectFourConfig()): ConnectFourState = ConnectFourState(
            grid = List(config.rows) { List(config.columns) { Disc.EMPTY } },
            currentTurn = Disc.PLAYER,
            moveCount = 0,
            lastMove = null,
            result = ConnectFourResult.NONE,
            winningLine = emptyList()
        )
    }
}
