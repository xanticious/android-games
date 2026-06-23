package com.xanticious.androidgames.games.battleships

import com.xanticious.androidgames.controller.games.battleships.BattleshipsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.battleships.BattleshipsCell
import com.xanticious.androidgames.model.games.battleships.BattleshipsCellMarker
import com.xanticious.androidgames.model.games.battleships.BattleshipsFireOutcome
import com.xanticious.androidgames.model.games.battleships.BattleshipsOrientation
import com.xanticious.androidgames.model.games.battleships.BattleshipsShipDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BattleshipsControllerTest {
    private val controller = BattleshipsController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun placeShip_insideGrid_isAccepted() {
        val step = controller.placeShip(
            controller.emptyBoard(config), config,
            BattleshipsShipDefinition.DESTROYER, BattleshipsCell(0, 0), BattleshipsOrientation.HORIZONTAL
        )
        assertTrue(step.accepted)
    }

    @Test
    fun placeShip_offGrid_isRejected() {
        val step = controller.placeShip(
            controller.emptyBoard(config), config,
            BattleshipsShipDefinition.CARRIER, BattleshipsCell(0, 8), BattleshipsOrientation.HORIZONTAL
        )
        assertFalse(step.accepted)
    }

    @Test
    fun placeShip_overlapping_isRejected() {
        val first = controller.placeShip(
            controller.emptyBoard(config), config,
            BattleshipsShipDefinition.DESTROYER, BattleshipsCell(0, 0), BattleshipsOrientation.HORIZONTAL
        )
        val second = controller.placeShip(
            first.board, config,
            BattleshipsShipDefinition.CRUISER, BattleshipsCell(0, 0), BattleshipsOrientation.HORIZONTAL
        )
        assertFalse(second.accepted)
    }

    @Test
    fun fire_onShip_reportsHit() {
        val board = controller.placeShip(
            controller.emptyBoard(config), config,
            BattleshipsShipDefinition.DESTROYER, BattleshipsCell(2, 2), BattleshipsOrientation.HORIZONTAL
        ).board
        val step = controller.fire(board, BattleshipsCell(2, 2))
        assertEquals(BattleshipsFireOutcome.HIT, step.report.outcome)
    }

    @Test
    fun fire_onEmptyCell_reportsMiss() {
        val step = controller.fire(controller.emptyBoard(config), BattleshipsCell(5, 5))
        assertEquals(BattleshipsFireOutcome.MISS, step.report.outcome)
    }

    @Test
    fun fire_lastShipCell_reportsSunk() {
        val board = controller.placeShip(
            controller.emptyBoard(config), config,
            BattleshipsShipDefinition.DESTROYER, BattleshipsCell(2, 2), BattleshipsOrientation.HORIZONTAL
        ).board
        val afterFirst = controller.fire(board, BattleshipsCell(2, 2)).board
        val sunkStep = controller.fire(afterFirst, BattleshipsCell(2, 3))
        assertEquals(BattleshipsFireOutcome.SUNK, sunkStep.report.outcome)
    }

    @Test
    fun fire_repeatCell_reportsAlreadyTargeted() {
        val once = controller.fire(controller.emptyBoard(config), BattleshipsCell(5, 5)).board
        val again = controller.fire(once, BattleshipsCell(5, 5))
        assertEquals(BattleshipsFireOutcome.ALREADY_TARGETED, again.report.outcome)
    }

    @Test
    fun allShipsSunk_singleShipFullyHit_isTrue() {
        var board = controller.placeShip(
            controller.emptyBoard(config), config,
            BattleshipsShipDefinition.DESTROYER, BattleshipsCell(2, 2), BattleshipsOrientation.HORIZONTAL
        ).board
        board = controller.fire(board, BattleshipsCell(2, 2)).board
        board = controller.fire(board, BattleshipsCell(2, 3)).board
        assertTrue(controller.allShipsSunk(board))
    }

    @Test
    fun randomFleetBoard_placesEntireFleet() {
        val board = controller.randomFleetBoard(config, Random(7))
        assertEquals(config.fleet.size, board.fleet.size)
    }

    @Test
    fun aiTarget_returnsUnfiredCell() {
        val board = controller.randomFleetBoard(config, Random(3))
        val target = controller.aiTarget(board, GameDifficulty.EASY, Random(3))
        assertEquals(BattleshipsCellMarker.UNKNOWN, board.cellAt(target!!)?.marker)
    }
}
