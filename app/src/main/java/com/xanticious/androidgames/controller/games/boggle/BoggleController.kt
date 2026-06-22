package com.xanticious.androidgames.controller.games.boggle

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.boggle.BoggleConfig
import com.xanticious.androidgames.model.games.boggle.GridSize
import kotlin.random.Random

class BoggleController(private val wordData: WordData) {

    private val boggleDice3x3 = listOf(
        "AAEEGN", "ABBJOO", "ACHOPS",
        "AFFKPS", "AOOTTW", "CIMOTU",
        "DEILRX", "DELRVY", "DISTTY"
    )

    private val boggleDice4x4 = listOf(
        "AAEEGN", "ABBJOO", "ACHOPS", "AFFKPS",
        "AOOTTW", "CIMOTU", "DEILRX", "DELRVY",
        "DISTTY", "EEGHNW", "EEINSU", "EHRTVW",
        "EIOSST", "ELRTTY", "HIMNUQ", "HLNNRZ"
    )

    fun generateRound(
        config: BoggleConfig,
        random: Random = Random.Default
    ): Pair<List<List<Char>>, List<String>> {
        val dice = when (config.gridSize) {
            GridSize.QUICK_3X3 -> boggleDice3x3
            GridSize.CLASSIC_4X4 -> boggleDice4x4
        }

        var attempts = 0
        val minWords = if (config.gridSize == GridSize.QUICK_3X3) 15 else 30

        while (attempts < 50) {
            val grid = generateGrid(dice.shuffled(random), config.gridSize.size, random)
            val possibleWords = findAllWords(grid, config.minLength)
            if (possibleWords.size >= minWords) {
                return Pair(grid, possibleWords)
            }
            attempts++
        }

        val grid = generateGrid(dice.shuffled(random), config.gridSize.size, random)
        return Pair(grid, findAllWords(grid, config.minLength))
    }

    private fun generateGrid(dice: List<String>, size: Int, random: Random): List<List<Char>> {
        val letters = dice.take(size * size).map { die ->
            die[random.nextInt(die.length)]
        }
        return letters.chunked(size)
    }

    fun findAllWords(grid: List<List<Char>>, minLength: Int): List<String> {
        val found = mutableSetOf<String>()
        val size = grid.size

        for (row in 0 until size) {
            for (col in 0 until size) {
                val visited = Array(size) { BooleanArray(size) }
                dfs(grid, row, col, "", visited, minLength, found)
            }
        }

        return found.sorted()
    }

    private fun dfs(
        grid: List<List<Char>>,
        row: Int,
        col: Int,
        prefix: String,
        visited: Array<BooleanArray>,
        minLength: Int,
        found: MutableSet<String>
    ) {
        if (row < 0 || row >= grid.size || col < 0 || col >= grid[0].size || visited[row][col]) {
            return
        }

        val newPrefix = prefix + grid[row][col].lowercaseChar()
        
        if (!wordData.isValidPrefix(newPrefix)) {
            return
        }

        visited[row][col] = true

        if (newPrefix.length >= minLength && wordData.isValidWord(newPrefix)) {
            found.add(newPrefix)
        }

        val directions = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1,           0 to 1,
            1 to -1,  1 to 0,  1 to 1
        )

        for ((dr, dc) in directions) {
            dfs(grid, row + dr, col + dc, newPrefix, visited, minLength, found)
        }

        visited[row][col] = false
    }

    fun isValidPath(path: List<Pair<Int, Int>>): Boolean {
        if (path.size < 2) return true

        for (i in 1 until path.size) {
            val (r1, c1) = path[i - 1]
            val (r2, c2) = path[i]
            val rowDiff = kotlin.math.abs(r1 - r2)
            val colDiff = kotlin.math.abs(c1 - c2)
            
            if (rowDiff > 1 || colDiff > 1 || (rowDiff == 0 && colDiff == 0)) {
                return false
            }
        }
        return true
    }

    fun isAdjacent(pos1: Pair<Int, Int>, pos2: Pair<Int, Int>): Boolean {
        val (r1, c1) = pos1
        val (r2, c2) = pos2
        val rowDiff = kotlin.math.abs(r1 - r2)
        val colDiff = kotlin.math.abs(c1 - c2)
        return rowDiff <= 1 && colDiff <= 1 && !(rowDiff == 0 && colDiff == 0)
    }

    fun scoreWord(word: String): Int = when (word.length) {
        0, 1, 2 -> 0
        3, 4 -> 1
        5 -> 2
        6 -> 3
        7 -> 5
        else -> 11
    }
}
