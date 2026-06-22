package com.xanticious.androidgames.controller.games.wordsearch

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordsearch.Direction
import com.xanticious.androidgames.model.games.wordsearch.GridPosition
import com.xanticious.androidgames.model.games.wordsearch.PlacedWord
import com.xanticious.androidgames.model.games.wordsearch.WordSearchGrid
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class WordSearchController {

    data class Config(
        val gridSize: Int,
        val wordCount: Int,
        val timeLimitSeconds: Int,
        val allowDiagonals: Boolean,
        val allowBackwards: Boolean
    )

    fun configFor(difficulty: GameDifficulty): Config = when (difficulty) {
        GameDifficulty.EASY -> Config(
            gridSize = 10,
            wordCount = 6,
            timeLimitSeconds = 180,
            allowDiagonals = false,
            allowBackwards = false
        )
        GameDifficulty.MEDIUM -> Config(
            gridSize = 12,
            wordCount = 8,
            timeLimitSeconds = 240,
            allowDiagonals = true,
            allowBackwards = false
        )
        GameDifficulty.HARD -> Config(
            gridSize = 15,
            wordCount = 10,
            timeLimitSeconds = 300,
            allowDiagonals = true,
            allowBackwards = true
        )
    }

    fun generateGrid(wordData: WordData, config: Config, seed: Long): WordSearchGrid {
        val random = Random(seed)
        val directions = buildDirections(config.allowDiagonals, config.allowBackwards)
        
        val minLength = 3
        val maxLength = config.gridSize - 2
        
        val candidateWords = (minLength..maxLength).flatMap { len ->
            wordData.wordsOfLength(len)
        }.filter { it.length in minLength..maxLength }.distinct()
        
        if (candidateWords.isEmpty()) {
            return WordSearchGrid.empty(config.gridSize)
        }
        
        val shuffled = candidateWords.shuffled(random)
        val targetWords = shuffled.take(config.wordCount * 3)
        
        val cells = MutableList(config.gridSize) { MutableList(config.gridSize) { ' ' } }
        val placed = mutableListOf<PlacedWord>()
        val usedFirstLetters = mutableSetOf<GridPosition>()
        
        for (word in targetWords) {
            if (placed.size >= config.wordCount) break
            
            val positions = directions.asSequence()
                .flatMap { dir -> allStartPositions(config.gridSize, word.length, dir) }
                .shuffled(random)
                .filter { (pos, _) -> pos !in usedFirstLetters }
                .toList()
            
            for ((start, dir) in positions) {
                if (canPlaceWord(cells, word, start, dir)) {
                    placeWord(cells, word, start, dir)
                    placed.add(PlacedWord(word, start, dir))
                    usedFirstLetters.add(start)
                    break
                }
            }
        }
        
        if (placed.isEmpty()) {
            return WordSearchGrid.empty(config.gridSize)
        }
        
        for (r in cells.indices) {
            for (c in cells[r].indices) {
                if (cells[r][c] == ' ') {
                    cells[r][c] = ('A'..'Z').random(random)
                }
            }
        }
        
        return WordSearchGrid(
            size = config.gridSize,
            cells = cells.map { it.toList() },
            placedWords = placed
        )
    }

    private fun buildDirections(diagonals: Boolean, backwards: Boolean): List<Direction> {
        val base = listOf(Direction.HORIZONTAL, Direction.VERTICAL)
        val withDiag = if (diagonals) {
            base + listOf(Direction.DIAGONAL_DOWN, Direction.DIAGONAL_UP)
        } else base
        
        return if (backwards) {
            withDiag + withDiag.map { reverseDirection(it) }
        } else withDiag
    }

    private fun reverseDirection(dir: Direction): Direction = when (dir) {
        Direction.HORIZONTAL -> Direction.HORIZONTAL_REV
        Direction.VERTICAL -> Direction.VERTICAL_REV
        Direction.DIAGONAL_DOWN -> Direction.DIAGONAL_DOWN_REV
        Direction.DIAGONAL_UP -> Direction.DIAGONAL_UP_REV
        Direction.HORIZONTAL_REV -> Direction.HORIZONTAL
        Direction.VERTICAL_REV -> Direction.VERTICAL
        Direction.DIAGONAL_DOWN_REV -> Direction.DIAGONAL_DOWN
        Direction.DIAGONAL_UP_REV -> Direction.DIAGONAL_UP
    }

    private fun allStartPositions(gridSize: Int, wordLen: Int, dir: Direction): List<Pair<GridPosition, Direction>> {
        val result = mutableListOf<Pair<GridPosition, Direction>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val pos = GridPosition(r, c)
                if (fitsInGrid(gridSize, pos, wordLen, dir)) {
                    result.add(pos to dir)
                }
            }
        }
        return result
    }

    private fun fitsInGrid(gridSize: Int, start: GridPosition, wordLen: Int, dir: Direction): Boolean {
        val endRow = start.row + dir.dr * (wordLen - 1)
        val endCol = start.col + dir.dc * (wordLen - 1)
        return endRow in 0 until gridSize && endCol in 0 until gridSize
    }

    private fun canPlaceWord(
        cells: MutableList<MutableList<Char>>,
        word: String,
        start: GridPosition,
        dir: Direction
    ): Boolean {
        for (i in word.indices) {
            val r = start.row + i * dir.dr
            val c = start.col + i * dir.dc
            val existing = cells[r][c]
            if (existing != ' ' && existing != word[i].uppercaseChar()) {
                return false
            }
        }
        return true
    }

    private fun placeWord(
        cells: MutableList<MutableList<Char>>,
        word: String,
        start: GridPosition,
        dir: Direction
    ) {
        for (i in word.indices) {
            val r = start.row + i * dir.dr
            val c = start.col + i * dir.dc
            cells[r][c] = word[i].uppercaseChar()
        }
    }

    data class SelectionResult(
        val isValid: Boolean,
        val matchedWord: String?
    )

    fun validateSelection(
        grid: WordSearchGrid,
        start: GridPosition,
        end: GridPosition,
        remainingWords: List<String>
    ): SelectionResult {
        val direction = inferDirection(start, end) ?: return SelectionResult(false, null)
        val length = selectionLength(start, end, direction)
        
        if (length < 3) return SelectionResult(false, null)
        
        val word = extractWord(grid, start, direction, length)
        
        val matched = remainingWords.firstOrNull { it.equals(word, ignoreCase = true) }
        return SelectionResult(matched != null, matched)
    }

    private fun inferDirection(start: GridPosition, end: GridPosition): Direction? {
        val dr = end.row - start.row
        val dc = end.col - start.col
        
        if (dr == 0 && dc == 0) return null
        
        val absDr = abs(dr)
        val absDc = abs(dc)
        
        return when {
            dr == 0 && dc > 0 -> Direction.HORIZONTAL
            dr == 0 && dc < 0 -> Direction.HORIZONTAL_REV
            dc == 0 && dr > 0 -> Direction.VERTICAL
            dc == 0 && dr < 0 -> Direction.VERTICAL_REV
            absDr == absDc && dr > 0 && dc > 0 -> Direction.DIAGONAL_DOWN
            absDr == absDc && dr > 0 && dc < 0 -> Direction.DIAGONAL_UP_REV
            absDr == absDc && dr < 0 && dc > 0 -> Direction.DIAGONAL_UP
            absDr == absDc && dr < 0 && dc < 0 -> Direction.DIAGONAL_DOWN_REV
            else -> null
        }
    }

    private fun selectionLength(start: GridPosition, end: GridPosition, dir: Direction): Int {
        return max(abs(end.row - start.row), abs(end.col - start.col)) + 1
    }

    private fun extractWord(
        grid: WordSearchGrid,
        start: GridPosition,
        dir: Direction,
        length: Int
    ): String {
        val chars = CharArray(length)
        for (i in 0 until length) {
            val r = start.row + i * dir.dr
            val c = start.col + i * dir.dc
            chars[i] = grid.cells[r][c]
        }
        return String(chars)
    }
}
