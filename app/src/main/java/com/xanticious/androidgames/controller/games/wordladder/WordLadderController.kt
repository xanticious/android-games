package com.xanticious.androidgames.controller.games.wordladder

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordladder.WordLadderPuzzle
import com.xanticious.androidgames.model.games.wordladder.WordLadderValidationResult
import kotlin.random.Random

class WordLadderController {

    fun differsByOneLetter(word1: String, word2: String): Boolean {
        if (word1.length != word2.length) return false
        var differences = 0
        for (i in word1.indices) {
            if (word1[i] != word2[i]) differences++
            if (differences > 1) return false
        }
        return differences == 1
    }

    fun validateStep(
        currentWord: String,
        newWord: String,
        usedWords: Set<String>,
        wordData: WordData
    ): WordLadderValidationResult {
        val normalized = newWord.lowercase()
        
        if (!differsByOneLetter(currentWord.lowercase(), normalized)) {
            return WordLadderValidationResult.Invalid("Must change exactly one letter")
        }
        
        if (!wordData.isValidWord(normalized)) {
            return WordLadderValidationResult.Invalid("Not a valid word")
        }
        
        if (usedWords.contains(normalized)) {
            return WordLadderValidationResult.Invalid("Word already used")
        }
        
        return WordLadderValidationResult.Valid
    }

    fun findShortestPath(startWord: String, targetWord: String, wordData: WordData): List<String>? {
        if (startWord.length != targetWord.length) return null
        if (!wordData.isValidWord(startWord) || !wordData.isValidWord(targetWord)) return null
        if (startWord == targetWord) return listOf(startWord)

        val start = startWord.lowercase()
        val target = targetWord.lowercase()
        val words = wordData.wordsOfLength(start.length).toSet()
        
        if (start !in words || target !in words) return null

        val queue = ArrayDeque<Pair<String, List<String>>>()
        val visited = mutableSetOf<String>()
        
        queue.add(start to listOf(start))
        visited.add(start)
        
        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            
            if (current == target) {
                return path
            }
            
            for (neighbor in getNeighbors(current, words)) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor to path + neighbor)
                }
            }
        }
        
        return null
    }

    private fun getNeighbors(word: String, validWords: Set<String>): List<String> {
        val neighbors = mutableListOf<String>()
        val chars = word.toCharArray()
        
        for (i in chars.indices) {
            val original = chars[i]
            for (c in 'a'..'z') {
                if (c != original) {
                    chars[i] = c
                    val candidate = String(chars)
                    if (candidate in validWords) {
                        neighbors.add(candidate)
                    }
                }
            }
            chars[i] = original
        }
        
        return neighbors
    }

    fun generatePuzzle(
        wordLength: Int,
        difficulty: GameDifficulty,
        wordData: WordData,
        random: Random = Random.Default
    ): WordLadderPuzzle? {
        val words = wordData.wordsOfLength(wordLength)
        if (words.size < 2) return null

        val minPathLength = when (difficulty) {
            GameDifficulty.EASY -> 3
            GameDifficulty.MEDIUM -> 4
            GameDifficulty.HARD -> 5
        }

        val maxAttempts = 50
        for (attempt in 0 until maxAttempts) {
            val startWord = words[random.nextInt(words.size)]
            val targetWord = words[random.nextInt(words.size)]
            
            if (startWord == targetWord) continue
            
            val path = findShortestPath(startWord, targetWord, wordData)
            if (path != null && path.size >= minPathLength) {
                return WordLadderPuzzle(
                    startWord = startWord,
                    targetWord = targetWord,
                    shortestPathLength = path.size - 1
                )
            }
        }
        
        return null
    }
}
