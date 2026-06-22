package com.xanticious.androidgames.games.treasuremapper

import com.xanticious.androidgames.controller.games.treasuremapper.TreasureMapperController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mapcommon.GridCell
import com.xanticious.androidgames.model.games.mapcommon.GridDirection
import com.xanticious.androidgames.model.games.treasuremapper.TreasureClueStep
import com.xanticious.androidgames.model.games.treasuremapper.TreasureMapperRound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TreasureMapperControllerTest {
    private val controller = TreasureMapperController()

    @Test
    fun generateWorld_mediumDifficulty_usesTwoStepClue() {
        val world = controller.generateWorld(controller.configFor(GameDifficulty.MEDIUM), Random(7))
        assertEquals(2, world.clue.steps.size)
    }

    @Test
    fun generateWorld_generatedClue_resolvesToTreasureCell() {
        val world = controller.generateWorld(controller.configFor(GameDifficulty.HARD), Random(11))
        assertEquals(world.treasure, controller.resolvePath(world.clue.landmark.cell, world.clue.steps))
    }

    @Test
    fun generateWorld_seededRandom_placesTreasureInBounds() {
        val world = controller.generateWorld(controller.configFor(GameDifficulty.EASY), Random(19))
        assertTrue(world.size.contains(world.treasure))
    }

    @Test
    fun resolvePath_cardinalSteps_returnsExpectedCell() {
        val steps = listOf(TreasureClueStep(GridDirection.EAST, 3), TreasureClueStep(GridDirection.SOUTH, 2))
        assertEquals(GridCell(4, 3), controller.resolvePath(GridCell(1, 1), steps))
    }

    @Test
    fun submitDig_correctGuess_marksRoundSolved() {
        val config = controller.configFor(GameDifficulty.EASY)
        val world = controller.generateWorld(config, Random(23))
        val round = controller.selectCell(TreasureMapperRound.initial(world, config.maxTries), world.treasure)
        assertTrue(controller.submitDig(round).solved)
    }

    @Test
    fun submitDig_wrongGuess_consumesOneTry() {
        val config = controller.configFor(GameDifficulty.EASY)
        val world = controller.generateWorld(config, Random(29))
        val wrong = world.size.cells.first { it != world.treasure }
        val round = controller.selectCell(TreasureMapperRound.initial(world, config.maxTries), wrong)
        assertEquals(2, controller.submitDig(round).triesRemaining)
    }
}
