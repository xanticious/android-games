package com.xanticious.androidgames.controller.games.battleships

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.battleships.BattleshipsBoard
import com.xanticious.androidgames.model.games.battleships.BattleshipsCell
import com.xanticious.androidgames.model.games.battleships.BattleshipsCellMarker
import com.xanticious.androidgames.model.games.battleships.BattleshipsConfig
import com.xanticious.androidgames.model.games.battleships.BattleshipsFireOutcome
import com.xanticious.androidgames.model.games.battleships.BattleshipsFireStep
import com.xanticious.androidgames.model.games.battleships.BattleshipsGridCell
import com.xanticious.androidgames.model.games.battleships.BattleshipsOrientation
import com.xanticious.androidgames.model.games.battleships.BattleshipsPlacementStep
import com.xanticious.androidgames.model.games.battleships.BattleshipsShip
import com.xanticious.androidgames.model.games.battleships.BattleshipsShipDefinition
import com.xanticious.androidgames.model.games.battleships.BattleshipsShotReport
import kotlin.math.abs
import kotlin.random.Random

/** Pure Battleships rules, placement, firing, sunk detection and local AI. */
class BattleshipsController {

    fun configFor(difficulty: GameDifficulty): BattleshipsConfig = BattleshipsConfig(difficulty = difficulty)

    fun emptyBoard(config: BattleshipsConfig): BattleshipsBoard = BattleshipsBoard.empty(config.gridSize)

    fun randomFleetBoard(config: BattleshipsConfig, random: Random): BattleshipsBoard {
        var board = emptyBoard(config)
        for (ship in config.fleet) {
            var placed = false
            repeat(1_000) {
                if (placed) return@repeat
                val orientation = if (random.nextBoolean()) BattleshipsOrientation.HORIZONTAL else BattleshipsOrientation.VERTICAL
                val row = random.nextInt(config.gridSize)
                val column = random.nextInt(config.gridSize)
                val step = placeShip(board, config, ship, BattleshipsCell(row, column), orientation)
                if (step.accepted) {
                    board = step.board
                    placed = true
                }
            }
            require(placed) { "Unable to place ${ship.displayName} on ${config.gridSize}x${config.gridSize} board" }
        }
        return board
    }

    fun placeShip(
        board: BattleshipsBoard,
        config: BattleshipsConfig,
        ship: BattleshipsShipDefinition,
        start: BattleshipsCell,
        orientation: BattleshipsOrientation
    ): BattleshipsPlacementStep {
        val cells = shipCells(start, ship.length, orientation)
        val invalidReason = placementError(board, config, cells)
        if (invalidReason.isNotEmpty()) return BattleshipsPlacementStep(board, accepted = false, message = invalidReason)

        val shipId = ship.name.lowercase()
        val newFleet = board.fleet + BattleshipsShip(shipId, ship, cells)
        val newCells = board.cells.map { row ->
            row.map { cell -> if (cells.contains(cell.coordinate)) cell.copy(shipId = shipId) else cell }
        }
        return BattleshipsPlacementStep(board.copy(cells = newCells, fleet = newFleet), accepted = true)
    }

    fun validatePlacement(
        board: BattleshipsBoard,
        config: BattleshipsConfig,
        ship: BattleshipsShipDefinition,
        start: BattleshipsCell,
        orientation: BattleshipsOrientation
    ): Boolean = placementError(board, config, shipCells(start, ship.length, orientation)).isEmpty()

    fun fire(board: BattleshipsBoard, target: BattleshipsCell): BattleshipsFireStep {
        val oldCell = board.cellAt(target) ?: return BattleshipsFireStep(
            board = board,
            report = BattleshipsShotReport(target, BattleshipsFireOutcome.INVALID),
            accepted = false
        )
        if (oldCell.wasFiredAt) {
            return BattleshipsFireStep(
                board = board,
                report = BattleshipsShotReport(target, BattleshipsFireOutcome.ALREADY_TARGETED, oldCell.shipId),
                accepted = false
            )
        }

        val newMarker = if (oldCell.hasShip) BattleshipsCellMarker.HIT else BattleshipsCellMarker.MISS
        val newCells = board.cells.mapIndexed { row, cells ->
            if (row != target.row) cells else cells.mapIndexed { column, cell ->
                if (column == target.column) cell.copy(marker = newMarker) else cell
            }
        }
        val newBoard = board.copy(cells = newCells)
        val ship = board.fleet.firstOrNull { it.id == oldCell.shipId }
        val sunk = ship != null && ship.cells.all { newBoard.cellAt(it)?.marker == BattleshipsCellMarker.HIT }
        val outcome = when {
            !oldCell.hasShip -> BattleshipsFireOutcome.MISS
            sunk -> BattleshipsFireOutcome.SUNK
            else -> BattleshipsFireOutcome.HIT
        }
        return BattleshipsFireStep(
            board = newBoard,
            report = BattleshipsShotReport(
                coordinate = target,
                outcome = outcome,
                shipId = ship?.id ?: "",
                shipName = ship?.definition?.displayName ?: "",
                allShipsSunk = allShipsSunk(newBoard)
            ),
            accepted = true
        )
    }

    fun allShipsSunk(board: BattleshipsBoard): Boolean =
        board.fleet.isNotEmpty() && board.fleet.all { ship -> ship.cells.all { board.cellAt(it)?.marker == BattleshipsCellMarker.HIT } }

    fun remainingShips(board: BattleshipsBoard): Int =
        board.fleet.count { ship -> ship.cells.any { board.cellAt(it)?.marker != BattleshipsCellMarker.HIT } }

    fun aiTarget(board: BattleshipsBoard, difficulty: GameDifficulty, random: Random): BattleshipsCell? = when (difficulty) {
        GameDifficulty.EASY -> unfiredCells(board).randomOrNull(random)
        GameDifficulty.MEDIUM -> mediumTarget(board, random)
        GameDifficulty.HARD -> hardTarget(board, random)
    }

    private fun shipCells(start: BattleshipsCell, length: Int, orientation: BattleshipsOrientation): List<BattleshipsCell> =
        List(length) { offset ->
            when (orientation) {
                BattleshipsOrientation.HORIZONTAL -> start.copy(column = start.column + offset)
                BattleshipsOrientation.VERTICAL -> start.copy(row = start.row + offset)
            }
        }

    private fun placementError(board: BattleshipsBoard, config: BattleshipsConfig, cells: List<BattleshipsCell>): String {
        if (cells.any { it.row !in 0 until config.gridSize || it.column !in 0 until config.gridSize }) return "Ship must stay inside the grid."
        if (cells.any { board.cellAt(it)?.hasShip == true }) return "Ships cannot overlap."
        if (config.noTouchingShips && cells.any { touchesExistingShip(board, it) }) return "Ships cannot touch."
        return ""
    }

    private fun touchesExistingShip(board: BattleshipsBoard, cell: BattleshipsCell): Boolean =
        (-1..1).any { dr ->
            (-1..1).any { dc -> board.cellAt(BattleshipsCell(cell.row + dr, cell.column + dc))?.hasShip == true }
        }

    private fun unfiredCells(board: BattleshipsBoard): List<BattleshipsCell> = board.cells.flatten()
        .filter { it.marker == BattleshipsCellMarker.UNKNOWN }
        .map { it.coordinate }

    private fun liveHitCells(board: BattleshipsBoard): List<BattleshipsCell> = board.fleet
        .filter { ship -> ship.cells.any { board.cellAt(it)?.marker != BattleshipsCellMarker.HIT } }
        .flatMap { ship -> ship.cells.filter { board.cellAt(it)?.marker == BattleshipsCellMarker.HIT } }

    private fun mediumTarget(board: BattleshipsBoard, random: Random): BattleshipsCell? {
        val targetCandidates = adjacentUnknowns(board, liveHitCells(board))
        if (targetCandidates.isNotEmpty()) return targetCandidates.random(random)
        val parity = unfiredCells(board).filter { (it.row + it.column) % 2 == 0 }
        return (if (parity.isNotEmpty()) parity else unfiredCells(board)).randomOrNull(random)
    }

    private fun hardTarget(board: BattleshipsBoard, random: Random): BattleshipsCell? {
        orientedCandidates(board).takeIf { it.isNotEmpty() }?.let { return it.random(random) }
        adjacentUnknowns(board, liveHitCells(board)).takeIf { it.isNotEmpty() }?.let { return it.random(random) }
        return probabilityTargets(board, random)
    }

    private fun adjacentUnknowns(board: BattleshipsBoard, cells: List<BattleshipsCell>): List<BattleshipsCell> = cells
        .flatMap { cell -> cardinalNeighbors(cell) }
        .filter { board.cellAt(it)?.marker == BattleshipsCellMarker.UNKNOWN }
        .distinct()

    private fun orientedCandidates(board: BattleshipsBoard): List<BattleshipsCell> {
        val hits = liveHitCells(board)
        val candidates = mutableSetOf<BattleshipsCell>()
        for (rowGroup in hits.groupBy { it.row }.values.filter { it.size >= 2 }) {
            val sorted = rowGroup.sortedBy { it.column }
            candidates += BattleshipsCell(sorted.first().row, sorted.first().column - 1)
            candidates += BattleshipsCell(sorted.last().row, sorted.last().column + 1)
        }
        for (columnGroup in hits.groupBy { it.column }.values.filter { it.size >= 2 }) {
            val sorted = columnGroup.sortedBy { it.row }
            candidates += BattleshipsCell(sorted.first().row - 1, sorted.first().column)
            candidates += BattleshipsCell(sorted.last().row + 1, sorted.last().column)
        }
        return candidates.filter { board.cellAt(it)?.marker == BattleshipsCellMarker.UNKNOWN }.distinct()
    }

    private fun probabilityTargets(board: BattleshipsBoard, random: Random): BattleshipsCell? {
        val unknown = unfiredCells(board)
        if (unknown.isEmpty()) return null
        val remainingLengths = board.fleet
            .filter { ship -> ship.cells.any { board.cellAt(it)?.marker != BattleshipsCellMarker.HIT } }
            .map { it.definition.length }
        val scores = unknown.associateWith { cell ->
            remainingLengths.sumOf { length -> placementCountThrough(board, cell, length) } + if ((cell.row + cell.column) % 2 == 0) 1 else 0
        }
        val best = scores.values.maxOrNull() ?: return unknown.random(random)
        return scores.filterValues { it == best }.keys.random(random)
    }

    private fun placementCountThrough(board: BattleshipsBoard, cell: BattleshipsCell, length: Int): Int {
        var score = 0
        for (orientation in BattleshipsOrientation.entries) {
            for (offset in 0 until length) {
                val start = when (orientation) {
                    BattleshipsOrientation.HORIZONTAL -> BattleshipsCell(cell.row, cell.column - offset)
                    BattleshipsOrientation.VERTICAL -> BattleshipsCell(cell.row - offset, cell.column)
                }
                val cells = shipCells(start, length, orientation)
                if (cells.contains(cell) && cells.all { possibleUnknownOrHit(board, it) }) score++
            }
        }
        return score
    }

    private fun possibleUnknownOrHit(board: BattleshipsBoard, cell: BattleshipsCell): Boolean {
        val gridCell = board.cellAt(cell) ?: return false
        return gridCell.marker != BattleshipsCellMarker.MISS
    }

    private fun cardinalNeighbors(cell: BattleshipsCell): List<BattleshipsCell> = listOf(
        BattleshipsCell(cell.row - 1, cell.column),
        BattleshipsCell(cell.row + 1, cell.column),
        BattleshipsCell(cell.row, cell.column - 1),
        BattleshipsCell(cell.row, cell.column + 1)
    )
}
