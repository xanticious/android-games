package com.xanticious.androidgames.games.mastermind

import com.xanticious.androidgames.controller.games.mastermind.MastermindController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MastermindControllerTest {

    private val controller = MastermindController()

    // ── scoring ─────────────────────────────────────────────────────────────

    @Test
    fun score_exactMatch_allBlackZeroWhite() {
        val fb = controller.score(listOf(1, 2, 3, 4), listOf(1, 2, 3, 4))
        assertEquals(4, fb.black)
        assertEquals(0, fb.white)
    }

    @Test
    fun score_noOverlap_zeroPegs() {
        // Colors 0-3 vs 4-7: no colour in common
        val fb = controller.score(listOf(0, 1, 2, 3), listOf(4, 5, 6, 7))
        assertEquals(0, fb.black)
        assertEquals(0, fb.white)
    }

    @Test
    fun score_allWhite_noBlack() {
        // [1,2,3,4] vs [4,3,2,1]: every colour present, all wrong position
        val fb = controller.score(listOf(1, 2, 3, 4), listOf(4, 3, 2, 1))
        assertEquals(0, fb.black)
        assertEquals(4, fb.white)
    }

    @Test
    fun score_duplicateInSecret_correctBlackWhite() {
        // Classic edge case: secret=[1,1,2,3], guess=[1,2,1,1]
        // black: pos 0 (1==1) → 1
        // white: colour 1: min(2,3)-1=1, colour 2: min(1,1)-0=1, colour 3: 0 → 2
        val fb = controller.score(listOf(1, 1, 2, 3), listOf(1, 2, 1, 1))
        assertEquals(1, fb.black)
        assertEquals(2, fb.white)
    }

    @Test
    fun score_duplicateInGuess_noDoubleCountingWhite() {
        // secret=[1,2,3,4], guess=[1,1,1,1]: only one 1 in secret → 1 black, 0 white
        val fb = controller.score(listOf(1, 2, 3, 4), listOf(1, 1, 1, 1))
        assertEquals(1, fb.black)
        assertEquals(0, fb.white)
    }

    @Test
    fun score_partialOverlap_mixedResult() {
        // secret=[0,1,2,3], guess=[0,3,1,5]
        // black: pos 0 (0==0) → 1
        // white: colour 1: min(1,1)-0=1, colour 3: min(1,1)-0=1 → 2
        val fb = controller.score(listOf(0, 1, 2, 3), listOf(0, 3, 1, 5))
        assertEquals(1, fb.black)
        assertEquals(2, fb.white)
    }

    // ── submit ───────────────────────────────────────────────────────────────

    @Test
    fun submit_appendsRow() {
        val state = controller.newGame(4, 6, true, 10, Random(42))
        val guess = listOf(0, 1, 2, 3)
        val next = controller.submit(state, guess)
        assertEquals(1, next.guesses.size)
        assertEquals(guess, next.guesses[0].guess)
    }

    @Test
    fun submit_clearsCurrentGuess() {
        val state = controller.newGame(4, 6, true, 10, Random(42))
        val next = controller.submit(state, listOf(0, 1, 2, 3))
        assertEquals(List(4) { null }, next.currentGuess)
    }

    // ── isSolved / isLost ───────────────────────────────────────────────────

    @Test
    fun isSolved_whenLastGuessMatchesSecret() {
        val state = controller.newGame(4, 6, true, 10, Random(1))
        val winning = controller.submit(state, state.secret)
        assertTrue(controller.isSolved(winning))
    }

    @Test
    fun isSolved_wrongGuess_returnsFalse() {
        val state = controller.newGame(4, 6, true, 10, Random(1))
        val wrong = List(4) { (state.secret[it] + 1) % 6 }
        val next = controller.submit(state, wrong)
        assertFalse(controller.isSolved(next))
    }

    @Test
    fun isLost_whenGuessLimitReachedWithoutSolving() {
        var state = controller.newGame(4, 6, true, 2, Random(1))
        val wrong = List(4) { (state.secret[it] + 1) % 6 }
        state = controller.submit(state, wrong)
        state = controller.submit(state, wrong)
        assertTrue(controller.isLost(state))
    }

    @Test
    fun isLost_beforeLimitReached_returnsFalse() {
        val state = controller.newGame(4, 6, true, 10, Random(1))
        assertFalse(controller.isLost(state))
    }

    @Test
    fun isLost_whenSolvedOnLastGuess_returnsFalse() {
        var state = controller.newGame(4, 6, true, 2, Random(1))
        val wrong = List(4) { (state.secret[it] + 1) % 6 }
        state = controller.submit(state, wrong)
        state = controller.submit(state, state.secret)
        assertFalse(controller.isLost(state))
    }

    // ── newGame ──────────────────────────────────────────────────────────────

    @Test
    fun newGame_secretColorsInRange() {
        val state = controller.newGame(4, 6, true, 10, Random(7))
        assertTrue(state.secret.all { it in 0 until 6 })
    }

    @Test
    fun newGame_noDuplicates_uniqueColors() {
        val state = controller.newGame(4, 6, false, 10, Random(99))
        assertEquals(4, state.secret.toSet().size)
    }

    @Test
    fun newGame_noDuplicates_lengthIsCorrect() {
        val state = controller.newGame(5, 7, false, 10, Random(3))
        assertEquals(5, state.secret.size)
    }

    @Test
    fun newGame_duplicatesAllowed_lengthIsCorrect() {
        val state = controller.newGame(6, 6, true, 8, Random(5))
        assertEquals(6, state.secret.size)
    }

    // ── setSlot / clearCurrentGuess ──────────────────────────────────────────

    @Test
    fun setSlot_placesColor() {
        val state = controller.newGame(4, 6, true, 10, Random(1))
        val next = controller.setSlot(state, 2, 5)
        assertEquals(5, next.currentGuess[2])
    }

    @Test
    fun setSlot_doesNotAffectOtherSlots() {
        val state = controller.newGame(4, 6, true, 10, Random(1))
        val next = controller.setSlot(state, 2, 5)
        assertEquals(null, next.currentGuess[0])
        assertEquals(null, next.currentGuess[1])
        assertEquals(null, next.currentGuess[3])
    }

    @Test
    fun setSlot_nullClearsSlot() {
        val state = controller.newGame(4, 6, true, 10, Random(1))
        val filled = controller.setSlot(state, 1, 3)
        val cleared = controller.setSlot(filled, 1, null)
        assertEquals(null, cleared.currentGuess[1])
    }

    @Test
    fun clearCurrentGuess_resetsAllSlots() {
        val state = controller.newGame(4, 6, true, 10, Random(1))
        val partial = controller.setSlot(controller.setSlot(state, 0, 1), 1, 2)
        val cleared = controller.clearCurrentGuess(partial)
        assertEquals(List(4) { null }, cleared.currentGuess)
    }
}
