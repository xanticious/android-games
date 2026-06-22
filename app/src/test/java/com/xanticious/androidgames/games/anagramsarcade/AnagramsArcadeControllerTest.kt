package com.xanticious.androidgames.games.anagramsarcade

import com.xanticious.androidgames.controller.games.anagramsarcade.AnagramsArcadeController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.anagramsarcade.AnagramsArcadeConfig
import com.xanticious.androidgames.model.games.anagramsarcade.LetterSetBias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class AnagramsArcadeControllerTest {

    private lateinit var wordData: WordData
    private lateinit var controller: AnagramsArcadeController

    @Before
    fun setup() {
        wordData = WordData(listOf(
            "cat", "act", "tac",
            "rate", "tear", "tare",
            "trace", "crate", "react", "cater",
            "create", "recat"
        ))
        controller = AnagramsArcadeController(wordData)
    }

    @Test
    fun generateRound_returnsLettersAndTargets() {
        val config = AnagramsArcadeConfig(minLength = 3, roundDuration = 90)
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
    fun scoreWord_threeLetters_noBonus_returnsOne() {
        assertEquals(1, controller.scoreWord("cat", 30))
    }

    @Test
    fun scoreWord_threeLetters_withBonus_returnsOnePointFive() {
        assertEquals(1, controller.scoreWord("cat", 70))
    }

    @Test
    fun scoreWord_sixLetters_returnsEight() {
        assertEquals(8, controller.scoreWord("create", 30))
    }

    @Test
    fun scoreWord_sixLetters_withBonus_returnsTwelve() {
        assertEquals(12, controller.scoreWord("create", 70))
    }
}
