package com.xanticious.androidgames.games.boggle

import com.xanticious.androidgames.controller.games.boggle.BoggleController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.boggle.BoggleConfig
import com.xanticious.androidgames.model.games.boggle.GridSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class BoggleControllerTest {

    private lateinit var wordData: WordData
    private lateinit var controller: BoggleController

    @Before
    fun setup() {
        wordData = WordData(listOf(
            "cat", "act", "tac",
            "rate", "tear", "tare",
            "trace", "crate", "react", "cater"
        ))
        controller = BoggleController(wordData)
    }

    @Test
    fun generateRound_3x3_returnsCorrectGridSize() {
        val config = BoggleConfig(gridSize = GridSize.QUICK_3X3)
        val (grid, _) = controller.generateRound(config, Random(42))
        
        assertEquals(3, grid.size)
        assertEquals(3, grid[0].size)
    }

    @Test
    fun generateRound_4x4_returnsCorrectGridSize() {
        val config = BoggleConfig(gridSize = GridSize.CLASSIC_4X4)
        val (grid, _) = controller.generateRound(config, Random(42))
        
        assertEquals(4, grid.size)
        assertEquals(4, grid[0].size)
    }

    @Test
    fun isAdjacent_horizontalNeighbors_returnsTrue() {
        assertTrue(controller.isAdjacent(0 to 0, 0 to 1))
    }

    @Test
    fun isAdjacent_verticalNeighbors_returnsTrue() {
        assertTrue(controller.isAdjacent(0 to 0, 1 to 0))
    }

    @Test
    fun isAdjacent_diagonalNeighbors_returnsTrue() {
        assertTrue(controller.isAdjacent(0 to 0, 1 to 1))
    }

    @Test
    fun isAdjacent_nonNeighbors_returnsFalse() {
        assertFalse(controller.isAdjacent(0 to 0, 0 to 2))
    }

    @Test
    fun isAdjacent_sameCell_returnsFalse() {
        assertFalse(controller.isAdjacent(0 to 0, 0 to 0))
    }

    @Test
    fun isValidPath_adjacentCells_returnsTrue() {
        val path = listOf(0 to 0, 0 to 1, 1 to 1)
        assertTrue(controller.isValidPath(path))
    }

    @Test
    fun isValidPath_nonAdjacentCells_returnsFalse() {
        val path = listOf(0 to 0, 0 to 2)
        assertFalse(controller.isValidPath(path))
    }

    @Test
    fun scoreWord_threeLetters_returnsOne() {
        assertEquals(1, controller.scoreWord("cat"))
    }

    @Test
    fun scoreWord_fourLetters_returnsOne() {
        assertEquals(1, controller.scoreWord("rate"))
    }

    @Test
    fun scoreWord_fiveLetters_returnsTwo() {
        assertEquals(2, controller.scoreWord("trace"))
    }

    @Test
    fun scoreWord_sixLetters_returnsThree() {
        assertEquals(3, controller.scoreWord("create"))
    }

    @Test
    fun scoreWord_sevenLetters_returnsFive() {
        assertEquals(5, controller.scoreWord("created"))
    }
}
