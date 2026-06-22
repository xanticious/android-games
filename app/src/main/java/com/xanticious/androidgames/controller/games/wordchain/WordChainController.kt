package com.xanticious.androidgames.controller.games.wordchain

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.wordchain.LinkRule
import com.xanticious.androidgames.model.games.wordchain.WordChainValidationResult

class WordChainController {

    fun validateWord(
        word: String,
        previousWord: String?,
        linkRule: LinkRule,
        minLength: Int,
        usedWords: Set<String>,
        wordData: WordData
    ): WordChainValidationResult {
        val normalized = word.lowercase()
        
        if (normalized.length < minLength) {
            return WordChainValidationResult.Invalid("Word too short (minimum $minLength letters)")
        }
        
        if (!wordData.isValidWord(normalized)) {
            return WordChainValidationResult.Invalid("Not a valid word")
        }
        
        if (usedWords.contains(normalized)) {
            return WordChainValidationResult.Invalid("Word already used")
        }
        
        if (previousWord != null) {
            val prev = previousWord.lowercase()
            val requiredStart = when (linkRule) {
                LinkRule.LAST_LETTER -> prev.takeLast(1)
                LinkRule.LAST_TWO_LETTERS -> prev.takeLast(2)
            }
            
            if (!normalized.startsWith(requiredStart, ignoreCase = true)) {
                return WordChainValidationResult.Invalid("Must start with '$requiredStart'")
            }
        }
        
        return WordChainValidationResult.Valid
    }

    fun getRequiredStart(previousWord: String?, linkRule: LinkRule): String {
        if (previousWord == null) return ""
        return when (linkRule) {
            LinkRule.LAST_LETTER -> previousWord.takeLast(1).uppercase()
            LinkRule.LAST_TWO_LETTERS -> previousWord.takeLast(2).uppercase()
        }
    }

    fun calculateScore(chainLength: Int, totalLetters: Int): Int {
        return chainLength * 10 + totalLetters
    }
}
