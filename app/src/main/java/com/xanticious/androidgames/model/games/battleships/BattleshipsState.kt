package com.xanticious.androidgames.model.games.battleships

import com.xanticious.androidgames.model.GameDifficulty

/** Row/column coordinate on a Battleships grid. Row 0 is the top, column 0 is the left. */
data class BattleshipsCell(val row: Int, val column: Int)

enum class BattleshipsOrientation { HORIZONTAL, VERTICAL }

enum class BattleshipsCellMarker { UNKNOWN, HIT, MISS }

enum class BattleshipsTurn { PLAYER, AI }

enum class BattleshipsResult { NONE, PLAYER_WIN, AI_WIN }

enum class BattleshipsFireOutcome { HIT, MISS, SUNK, ALREADY_TARGETED, INVALID }

/** Classic fleet: carrier, battleship, cruiser, submarine, destroyer. */
enum class BattleshipsShipDefinition(val displayName: String, val length: Int) {
    CARRIER("Carrier", 5),
    BATTLESHIP("Battleship", 4),
    CRUISER("Cruiser", 3),
    SUBMARINE("Submarine", 3),
    DESTROYER("Destroyer", 2)
}

data class BattleshipsShip(
    val id: String,
    val definition: BattleshipsShipDefinition,
    val cells: List<BattleshipsCell>
)

data class BattleshipsGridCell(
    val coordinate: BattleshipsCell,
    val shipId: String = "",
    val marker: BattleshipsCellMarker = BattleshipsCellMarker.UNKNOWN
) {
    val hasShip: Boolean get() = shipId.isNotEmpty()
    val wasFiredAt: Boolean get() = marker != BattleshipsCellMarker.UNKNOWN
}

data class BattleshipsBoard(
    val size: Int,
    val cells: List<List<BattleshipsGridCell>>,
    val fleet: List<BattleshipsShip>
) {
    fun cellAt(cell: BattleshipsCell): BattleshipsGridCell? =
        cells.getOrNull(cell.row)?.getOrNull(cell.column)

    companion object {
        fun empty(size: Int): BattleshipsBoard = BattleshipsBoard(
            size = size,
            cells = List(size) { row -> List(size) { column -> BattleshipsGridCell(BattleshipsCell(row, column)) } },
            fleet = emptyList()
        )
    }
}

data class BattleshipsConfig(
    val gridSize: Int = 10,
    val fleet: List<BattleshipsShipDefinition> = BattleshipsShipDefinition.entries,
    val noTouchingShips: Boolean = false,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM
)

data class BattleshipsPlacementProgress(
    val nextShipIndex: Int,
    val orientation: BattleshipsOrientation
) {
    val isComplete: Boolean get() = nextShipIndex >= BattleshipsShipDefinition.entries.size
}

data class BattleshipsShotReport(
    val coordinate: BattleshipsCell,
    val outcome: BattleshipsFireOutcome,
    val shipId: String = "",
    val shipName: String = "",
    val allShipsSunk: Boolean = false
)

data class BattleshipsFireStep(
    val board: BattleshipsBoard,
    val report: BattleshipsShotReport,
    val accepted: Boolean
)

data class BattleshipsPlacementStep(
    val board: BattleshipsBoard,
    val accepted: Boolean,
    val message: String = ""
)

data class BattleshipsState(
    val playerBoard: BattleshipsBoard,
    val aiBoard: BattleshipsBoard,
    val placement: BattleshipsPlacementProgress,
    val currentTurn: BattleshipsTurn,
    val config: BattleshipsConfig,
    val result: BattleshipsResult,
    val turnNumber: Int,
    val playerShots: Int,
    val aiShots: Int,
    val lastShot: BattleshipsShotReport?
) {
    val isGameOver: Boolean get() = result != BattleshipsResult.NONE

    companion object {
        fun initial(config: BattleshipsConfig = BattleshipsConfig()): BattleshipsState = BattleshipsState(
            playerBoard = BattleshipsBoard.empty(config.gridSize),
            aiBoard = BattleshipsBoard.empty(config.gridSize),
            placement = BattleshipsPlacementProgress(0, BattleshipsOrientation.HORIZONTAL),
            currentTurn = BattleshipsTurn.PLAYER,
            config = config,
            result = BattleshipsResult.NONE,
            turnNumber = 1,
            playerShots = 0,
            aiShots = 0,
            lastShot = null
        )
    }
}
