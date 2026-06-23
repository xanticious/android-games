package com.xanticious.androidgames.games.wordle

import com.xanticious.androidgames.controller.games.wordle.WordleController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.wordle.ConstraintState
import com.xanticious.androidgames.model.games.wordle.GuessResult
import com.xanticious.androidgames.model.games.wordle.LetterHint
import com.xanticious.androidgames.model.games.wordle.WordleSettings
import org.junit.Assert.*
import org.junit.Test

class WordleControllerTest {
    private val controller = WordleController()
    private val testWords = listOf("crane", "story", "slate", "plumb", "dodge", "aahed")
    private val wordData = WordData(testWords)
    private val settings = WordleSettings(wordLength = 5, maxGuesses = 6)

    @Test
    fun computeHints_exactMatch_allCorrect() {
        val hints = controller.computeHints("crane", "crane")
        assertEquals(5, hints.size)
        assertTrue(hints.all { it == LetterHint.CORRECT })
    }

    @Test
    fun computeHints_noMatch_allAbsent() {
        val hints = controller.computeHints("plumb", "story")
        assertEquals(5, hints.size)
        assertTrue(hints.all { it == LetterHint.ABSENT })
    }

    @Test
    fun computeHints_partialMatch_mixedHints() {
        val hints = controller.computeHints("crane", "slate")
        assertEquals(LetterHint.ABSENT, hints[0]) // c (not in slate)
        assertEquals(LetterHint.ABSENT, hints[1]) // r (not in slate)
        assertEquals(LetterHint.CORRECT, hints[2]) // a (correct position)
        assertEquals(LetterHint.ABSENT, hints[3]) // n (not in slate)
        assertEquals(LetterHint.CORRECT, hints[4]) // e (correct position)
    }

    @Test
    fun computeHints_duplicateLetters_correctlyHandlesCounts() {
        // Target: "aahed", Guess: "ahead"
        // a -> CORRECT (pos 0, target pos 0)
        // h -> PRESENT (pos 1, target has h at pos 2)
        // e -> PRESENT (pos 2, target has e at pos 3)
        // a -> PRESENT (pos 3, target has 2nd 'a' at pos 1)
        // d -> CORRECT (pos 4, target pos 4)
        val hints = controller.computeHints("ahead", "aahed")
        assertEquals(LetterHint.CORRECT, hints[0]) // a (correct position)
        assertEquals(LetterHint.PRESENT, hints[1]) // h (wrong position)
        assertEquals(LetterHint.PRESENT, hints[2]) // e (wrong position)
        assertEquals(LetterHint.PRESENT, hints[3]) // a (another a exists at pos 1)
        assertEquals(LetterHint.CORRECT, hints[4]) // d (correct position)
    }

    @Test
    fun computeHints_duplicateLetters_onlyOneInTarget() {
        // Target "slate", Guess "eerie"
        // slate has one 'e' at position 4
        // eerie: e(0)->present, e(1)->absent, r(2)->absent, i(3)->absent, e(4)->correct
        val hints = controller.computeHints("eerie", "slate")
        assertEquals(LetterHint.ABSENT, hints[0]) // e (correct one used at pos 4)
        assertEquals(LetterHint.ABSENT, hints[1]) // e (no more e's in target)
        assertEquals(LetterHint.ABSENT, hints[2]) // r (not in slate)
        assertEquals(LetterHint.ABSENT, hints[3]) // i (not in slate)
        assertEquals(LetterHint.CORRECT, hints[4]) // e (correct position)
    }

    @Test
    fun deriveConstraints_singleGuess_extractsCorrectly() {
        val guess = GuessResult(
            "crane",
            listOf(LetterHint.ABSENT, LetterHint.PRESENT, LetterHint.CORRECT, 
                   LetterHint.ABSENT, LetterHint.CORRECT)
        )
        val constraints = controller.deriveConstraints(listOf(guess))
        
        assertEquals(2, constraints.correctPositions.size)
        assertEquals('a', constraints.correctPositions[2])
        assertEquals('e', constraints.correctPositions[4])
        assertEquals(setOf('r'), constraints.presentLetters)
        assertEquals(setOf('c', 'n'), constraints.absentLetters)
    }

    @Test
    fun deriveConstraints_multipleGuesses_accumulates() {
        val guesses = listOf(
            GuessResult("crane", listOf(LetterHint.ABSENT, LetterHint.PRESENT, 
                                        LetterHint.CORRECT, LetterHint.ABSENT, LetterHint.CORRECT)),
            GuessResult("brake", listOf(LetterHint.ABSENT, LetterHint.CORRECT, 
                                        LetterHint.CORRECT, LetterHint.ABSENT, LetterHint.CORRECT))
        )
        val constraints = controller.deriveConstraints(guesses)
        
        assertEquals(3, constraints.correctPositions.size)
        assertEquals('r', constraints.correctPositions[1])
        assertEquals('a', constraints.correctPositions[2])
        assertEquals('e', constraints.correctPositions[4])
        assertTrue(constraints.absentLetters.contains('c'))
        assertTrue(constraints.absentLetters.contains('n'))
        assertTrue(constraints.absentLetters.contains('b'))
        assertTrue(constraints.absentLetters.contains('k'))
    }

    @Test
    fun isConsistentWithConstraints_validWord_returnsTrue() {
        val constraints = ConstraintState(
            correctPositions = mapOf(2 to 'a', 4 to 'e'),
            presentLetters = setOf('r'),
            absentLetters = setOf('c', 'n')
        )
        assertTrue(controller.isConsistentWithConstraints("brake", constraints))
    }

    @Test
    fun isConsistentWithConstraints_missingCorrectPosition_returnsFalse() {
        val constraints = ConstraintState(
            correctPositions = mapOf(2 to 'a', 4 to 'e'),
            presentLetters = setOf(),
            absentLetters = setOf()
        )
        assertFalse(controller.isConsistentWithConstraints("bring", constraints))
    }

    @Test
    fun isConsistentWithConstraints_missingPresentLetter_returnsFalse() {
        val constraints = ConstraintState(
            correctPositions = mapOf(),
            presentLetters = setOf('r'),
            absentLetters = setOf()
        )
        assertFalse(controller.isConsistentWithConstraints("slate", constraints))
    }

    @Test
    fun isConsistentWithConstraints_containsAbsentLetter_returnsFalse() {
        val constraints = ConstraintState(
            correctPositions = mapOf(),
            presentLetters = setOf(),
            absentLetters = setOf('c', 'n')
        )
        assertFalse(controller.isConsistentWithConstraints("crane", constraints))
    }

    @Test
    fun validateGuess_validWord_returnsTrue() {
        assertTrue(controller.validateGuess("crane", wordData, settings, null))
    }

    @Test
    fun validateGuess_invalidWord_returnsFalse() {
        assertFalse(controller.validateGuess("xxxxx", wordData, settings, null))
    }

    @Test
    fun validateGuess_wrongLength_returnsFalse() {
        assertFalse(controller.validateGuess("cat", wordData, settings, null))
    }

    @Test
    fun validateGuess_enforcesConsistency_whenEnabled() {
        val constraints = ConstraintState(
            correctPositions = mapOf(2 to 'a'),
            presentLetters = setOf(),
            absentLetters = setOf()
        )
        val enforcedSettings = settings.copy(enforceConsistency = true)
        
        assertTrue(controller.validateGuess("crane", wordData, enforcedSettings, constraints))
        assertFalse(controller.validateGuess("story", wordData, enforcedSettings, constraints))
    }

    @Test
    fun validateGuess_ignoresConsistency_whenDisabled() {
        val constraints = ConstraintState(
            correctPositions = mapOf(2 to 'a'),
            presentLetters = setOf(),
            absentLetters = setOf()
        )
        val relaxedSettings = settings.copy(enforceConsistency = false)
        
        assertTrue(controller.validateGuess("crane", wordData, relaxedSettings, constraints))
        assertTrue(controller.validateGuess("story", wordData, relaxedSettings, constraints))
    }

    @Test
    fun findValidGuess_noConstraints_findsWord() {
        val constraints = ConstraintState(mapOf(), setOf(), setOf())
        val result = controller.findValidGuess(wordData, constraints, 5)
        assertNotNull(result)
        assertEquals(5, result?.length)
    }

    @Test
    fun findValidGuess_withConstraints_findsConsistentWord() {
        // crane has 'a' at pos 2, no excluded letters
        val constraints = ConstraintState(
            correctPositions = mapOf(2 to 'a'),
            presentLetters = setOf(),
            absentLetters = setOf('x', 'z', 'q', 'j', 'k')
        )
        val result = controller.findValidGuess(wordData, constraints, 5)
        assertNotNull(result)
        assertEquals('a', result?.get(2))
        assertFalse(result?.contains('x') == true)
    }

    @Test
    fun getFirstGuess_carryEnabled_returnsPreviousTarget() {
        val settingsWithCarry = settings.copy(carryFirstGuess = true)
        val result = controller.getFirstGuess("crane", settingsWithCarry, wordData)
        assertEquals("crane", result)
    }

    @Test
    fun getFirstGuess_carryDisabled_returnsRandomWord() {
        val settingsWithoutCarry = settings.copy(carryFirstGuess = false)
        val result = controller.getFirstGuess("crane", settingsWithoutCarry, wordData)
        assertNotNull(result)
        assertEquals(5, result.length)
        assertTrue(wordData.isValidWord(result))
    }

    @Test
    fun getFirstGuess_noPreviousTarget_returnsRandomWord() {
        val settingsWithCarry = settings.copy(carryFirstGuess = true)
        val result = controller.getFirstGuess(null, settingsWithCarry, wordData)
        assertNotNull(result)
        assertEquals(5, result.length)
        assertTrue(wordData.isValidWord(result))
    }
}
