package com.xanticious.androidgames.controller.games.anagramsarcade

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.anagramsarcade.AnagramsArcadeConfig
import com.xanticious.androidgames.model.games.anagramsarcade.LetterSetBias
import kotlin.random.Random

class AnagramsArcadeController(private val wordData: WordData) {

    fun generateRound(
        config: AnagramsArcadeConfig,
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

    fun scoreWord(word: String, timeRemaining: Int): Int {
        val baseScore = when (word.length) {
            0, 1, 2 -> 0
            3 -> 1
            4 -> 2
            5 -> 4
            6 -> 8
            else -> word.length * 2
        }
        val timeBonus = if (timeRemaining > 60) (baseScore * 0.5).toInt() else 0
        return baseScore + timeBonus
    }
}
