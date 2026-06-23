package com.xanticious.androidgames.controller.games.anagrams

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.anagrams.AnagramsConfig
import com.xanticious.androidgames.model.games.anagrams.LetterSetBias
import kotlin.random.Random

/**
 * Pure controller for Anagrams game logic. No Android imports.
 * Per `design/word-games/anagrams/anagrams-design.md`.
 */
class AnagramsController(private val wordData: WordData) {

    fun generateRound(
        config: AnagramsConfig,
        random: Random = Random.Default
    ): Pair<List<Char>, List<String>> {
        val targetLength = 6
        val minTargets = 10
        var attempts = 0
        
        while (attempts < 100) {
            val word = if (config.letterSetBias == LetterSetBias.VOWEL_HEAVY) {
                pickVowelHeavyWord(targetLength, random)
            } else {
                wordData.randomWordOfLength(targetLength, random)
            }
            
            if (word != null) {
                val letters = word.toList()
                val targets = wordData.anagramSolutions(word, config.minLength)
                if (targets.size >= minTargets) {
                    return Pair(letters, targets)
                }
            }
            attempts++
        }
        
        // Fallback: use a known good set
        val fallback = "CREATE"
        return Pair(fallback.toList(), wordData.anagramSolutions(fallback, config.minLength))
    }

    private fun pickVowelHeavyWord(length: Int, random: Random): String? {
        val candidates = wordData.wordsOfLength(length).filter { word ->
            val vowelCount = word.count { it in "aeiou" }
            vowelCount >= length / 2
        }
        return if (candidates.isEmpty()) null else candidates[random.nextInt(candidates.size)]
    }

    fun isValidWord(word: String): Boolean = wordData.isValidWord(word)

    fun scoreWord(word: String): Int = when (word.length) {
        0, 1, 2 -> 0
        3 -> 1
        4 -> 2
        5 -> 4
        6 -> 8
        else -> word.length * 2
    }
}
