package com.xanticious.androidgames.words

import com.xanticious.androidgames.controller.words.WordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDataTest {

    private val data = WordData(listOf("cat", "act", "car", "scar", "care", "races", "dog", "AT"))

    @Test
    fun isValidWord_knownWord_returnsTrue() {
        assertTrue(data.isValidWord("cat"))
    }

    @Test
    fun isValidWord_isCaseInsensitive() {
        assertTrue(data.isValidWord("CAT"))
    }

    @Test
    fun isValidWord_unknownWord_returnsFalse() {
        assertFalse(data.isValidWord("zzz"))
    }

    @Test
    fun wordsOfLength_returnsOnlyMatchingLength() {
        assertEquals(listOf("act", "car", "cat", "dog"), data.wordsOfLength(3))
    }

    @Test
    fun isValidPrefix_existingPrefix_returnsTrue() {
        assertTrue(data.isValidPrefix("ca"))
    }

    @Test
    fun isValidPrefix_missingPrefix_returnsFalse() {
        assertFalse(data.isValidPrefix("zoo"))
    }

    @Test
    fun anagramSolutions_respectsLetterMultiplicity() {
        val solutions = WordData(listOf("cat", "act", "aa", "aaa")).anagramSolutions("act", minLength = 2)
        assertEquals(listOf("act", "cat"), solutions.sorted())
    }

    @Test
    fun anagramSolutions_excludesWordsNeedingMissingLetters() {
        assertFalse(data.anagramSolutions("cat", minLength = 3).contains("dog"))
    }

    @Test
    fun randomWordOfLength_returnsWordOfThatLength() {
        val word = data.randomWordOfLength(4)
        assertTrue(word != null && word.length == 4)
    }

    @Test
    fun definitionOf_returnsNullWhenUnavailable() {
        assertEquals(null, data.definitionOf("cat"))
    }

    @Test
    fun size_countsDistinctLowercasedWords() {
        assertEquals(8, data.size)
    }
}
