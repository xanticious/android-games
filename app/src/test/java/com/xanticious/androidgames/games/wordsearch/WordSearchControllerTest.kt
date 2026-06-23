package com.xanticious.androidgames.games.wordsearch

import com.xanticious.androidgames.controller.games.wordsearch.WordSearchController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordsearch.GridPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchControllerTest {
    private val controller = WordSearchController()
    private val wordData = WordData(
        listOf(
            "cat", "cot", "cog", "dog", "fog", "log", "bog",
            "test", "best", "rest", "nest",
            "apple", "grape", "peach", "plum",
            "hello", "world", "kotlin", "android"
        )
    )

    @Test
    fun configFor_easy_hasSmallGridAndNoComplexDirections() {
        val config = controller.configFor(GameDifficulty.EASY)
        assertEquals(10, config.gridSize)
        assertFalse(config.allowDiagonals)
        assertFalse(config.allowBackwards)
    }

    @Test
    fun configFor_medium_allowsDiagonals() {
        val config = controller.configFor(GameDifficulty.MEDIUM)
        assertTrue(config.allowDiagonals)
        assertFalse(config.allowBackwards)
    }

    @Test
    fun configFor_hard_allowsAllDirections() {
        val config = controller.configFor(GameDifficulty.HARD)
        assertTrue(config.allowDiagonals)
        assertTrue(config.allowBackwards)
    }

    @Test
    fun generateGrid_producesCorrectSize() {
        val config = controller.configFor(GameDifficulty.EASY)
        val grid = controller.generateGrid(wordData, config, seed = 42)
        assertEquals(config.gridSize, grid.size)
        assertEquals(config.gridSize, grid.cells.size)
        assertEquals(config.gridSize, grid.cells[0].size)
    }

    @Test
    fun generateGrid_placesWords() {
        val config = controller.configFor(GameDifficulty.MEDIUM)
        val grid = controller.generateGrid(wordData, config, seed = 123)
        assertTrue("Should place at least one word", grid.placedWords.isNotEmpty())
    }

    @Test
    fun generateGrid_deterministicWithSameSeed() {
        val config = controller.configFor(GameDifficulty.EASY)
        val grid1 = controller.generateGrid(wordData, config, seed = 999)
        val grid2 = controller.generateGrid(wordData, config, seed = 999)
        assertEquals(grid1.placedWords.size, grid2.placedWords.size)
        assertEquals(grid1.placedWords.map { it.word }, grid2.placedWords.map { it.word })
    }

    @Test
    fun generateGrid_firstLettersAreUnique() {
        val config = controller.configFor(GameDifficulty.MEDIUM).copy(wordCount = 5)
        val grid = controller.generateGrid(wordData, config, seed = 555)
        
        val firstLetterPositions = grid.placedWords.map { it.startPos }
        val uniquePositions = firstLetterPositions.toSet()
        
        assertEquals(
            "Each word should start on a unique cell",
            firstLetterPositions.size,
            uniquePositions.size
        )
    }

    @Test
    fun generateGrid_fillsEmptyCellsWithRandomLetters() {
        val config = controller.configFor(GameDifficulty.EASY).copy(wordCount = 2)
        val grid = controller.generateGrid(wordData, config, seed = 777)
        
        var hasNonWordLetters = false
        for (row in grid.cells) {
            for (cell in row) {
                if (cell != ' ') {
                    hasNonWordLetters = true
                    assertTrue("Cell should contain A-Z", cell in 'A'..'Z')
                }
            }
        }
        assertTrue("Grid should have letters in all cells", hasNonWordLetters)
    }

    @Test
    fun validateSelection_horizontalWord_matches() {
        val config = controller.configFor(GameDifficulty.EASY).copy(wordCount = 1)
        val grid = controller.generateGrid(wordData, config, seed = 100)
        
        if (grid.placedWords.isNotEmpty()) {
            val placed = grid.placedWords[0]
            val endRow = placed.startPos.row + placed.direction.dr * (placed.word.length - 1)
            val endCol = placed.startPos.col + placed.direction.dc * (placed.word.length - 1)
            val end = GridPosition(endRow, endCol)
            
            val result = controller.validateSelection(
                grid,
                placed.startPos,
                end,
                listOf(placed.word)
            )
            
            assertTrue("Should match the placed word", result.isValid)
            assertEquals(placed.word, result.matchedWord)
        }
    }

    @Test
    fun validateSelection_wrongWord_doesNotMatch() {
        val config = controller.configFor(GameDifficulty.EASY)
        val grid = controller.generateGrid(wordData, config, seed = 200)
        
        val result = controller.validateSelection(
            grid,
            GridPosition(0, 0),
            GridPosition(0, 2),
            listOf("xyz")
        )
        
        assertFalse(result.isValid)
        assertNull(result.matchedWord)
    }

    @Test
    fun validateSelection_selectionTooShort_doesNotMatch() {
        val config = controller.configFor(GameDifficulty.EASY)
        val grid = controller.generateGrid(wordData, config, seed = 300)
        
        val result = controller.validateSelection(
            grid,
            GridPosition(0, 0),
            GridPosition(0, 1),
            listOf("cat")
        )
        
        assertFalse(result.isValid)
    }

    @Test
    fun validateSelection_diagonalSelection_infersDirection() {
        val config = controller.configFor(GameDifficulty.MEDIUM).copy(allowDiagonals = true)
        val grid = controller.generateGrid(wordData, config, seed = 400)
        
        val diagonalPlaced = grid.placedWords.firstOrNull { 
            it.direction.dr != 0 && it.direction.dc != 0 
        }
        
        if (diagonalPlaced != null) {
            val endRow = diagonalPlaced.startPos.row + 
                diagonalPlaced.direction.dr * (diagonalPlaced.word.length - 1)
            val endCol = diagonalPlaced.startPos.col + 
                diagonalPlaced.direction.dc * (diagonalPlaced.word.length - 1)
            val end = GridPosition(endRow, endCol)
            
            val result = controller.validateSelection(
                grid,
                diagonalPlaced.startPos,
                end,
                listOf(diagonalPlaced.word)
            )
            
            assertTrue("Should match diagonal word", result.isValid)
            assertEquals(diagonalPlaced.word, result.matchedWord)
        }
    }

    @Test
    fun validateSelection_singleCell_doesNotMatch() {
        val config = controller.configFor(GameDifficulty.EASY)
        val grid = controller.generateGrid(wordData, config, seed = 500)
        
        val result = controller.validateSelection(
            grid,
            GridPosition(0, 0),
            GridPosition(0, 0),
            listOf("a")
        )
        
        assertFalse(result.isValid)
    }
}
