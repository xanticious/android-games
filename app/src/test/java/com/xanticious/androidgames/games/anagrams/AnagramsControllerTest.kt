package com.xanticious.androidgames.games.anagrams

import com.xanticious.androidgames.controller.games.anagrams.AnagramsController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.anagrams.AnagramsConfig
import com.xanticious.androidgames.model.games.anagrams.LetterSetBias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class AnagramsControllerTest {

    private lateinit var wordData: WordData
    private lateinit var controller: AnagramsController

    @Before
    fun setup() {
        wordData = WordData(listOf(
            "cat", "act", "tac",
            "rate", "tear", "tare",
            "trace", "crate", "react", "cater",
            "create", "recat"
        ))
        controller = AnagramsController(wordData)
    }

    @Test
    fun generateRound_returnsLettersAndTargets() {
        val config = AnagramsConfig(minLength = 3)
        val (letters, targets) = controller.generateRound(config, Random(42))
        
        assertEquals(6, letters.size)
        assertTrue(targets.isNotEmpty())
    }

    @Test
    fun isValidWord_validWord_returnsTrue() {
        assertTrue(controller.isValidWord("cat"))
    }

    @Test
    fun isValidWord_invalidWord_returnsFalse() {
        assertFalse(controller.isValidWord("xyz"))
    }

    @Test
    fun scoreWord_threeLetters_returnsOne() {
        assertEquals(1, controller.scoreWord("cat"))
    }

    @Test
    fun scoreWord_fourLetters_returnsTwo() {
        assertEquals(2, controller.scoreWord("rate"))
    }

    @Test
    fun scoreWord_fiveLetters_returnsFour() {
        assertEquals(4, controller.scoreWord("trace"))
    }

    @Test
    fun scoreWord_sixLetters_returnsEight() {
        assertEquals(8, controller.scoreWord("create"))
    }
}
