package com.xanticious.androidgames.games.morse

import com.xanticious.androidgames.controller.games.morse.MorseBeeper
import com.xanticious.androidgames.model.games.morse.MORSE
import com.xanticious.androidgames.model.games.morse.MorseTiming
import com.xanticious.androidgames.model.games.morse.Symbol
import com.xanticious.androidgames.model.games.morse.glyphs
import com.xanticious.androidgames.model.games.morse.letterFor
import com.xanticious.androidgames.model.games.morse.wpmToUnitMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseCodeTest {

    @Test
    fun morse_coversAll26Letters() {
        assertEquals(26, MORSE.size)
    }

    @Test
    fun morse_sosPattern() {
        assertEquals("...---...", (MORSE.getValue('S') + MORSE.getValue('O') + MORSE.getValue('S')).glyphs())
    }

    @Test
    fun letterFor_roundTripsEveryLetter() {
        MORSE.forEach { (letter, symbols) -> assertEquals(letter, letterFor(symbols)) }
    }

    @Test
    fun letterFor_unknownPattern_isNull() {
        assertEquals(null, letterFor(listOf(Symbol.DAH, Symbol.DAH, Symbol.DAH, Symbol.DAH, Symbol.DAH)))
    }

    @Test
    fun wpmToUnitMs_standardTwentyWpm() {
        assertEquals(60, wpmToUnitMs(20))
    }

    @Test
    fun wpmToUnitMs_slowerIsLargerUnit() {
        assertTrue(wpmToUnitMs(10) > wpmToUnitMs(20))
    }

    @Test
    fun timing_dahIsThreeDits() {
        val t = MorseTiming(60)
        assertEquals(t.ditMs * 3, t.dahMs)
    }

    @Test
    fun timing_wordGapIsSevenUnits() {
        val t = MorseTiming(50)
        assertEquals(350, t.wordGapMs)
    }

    @Test
    fun beeper_scheduleForE_isSingleDitTone() {
        val schedule = MorseBeeper.schedule('E', MorseTiming(60))
        assertEquals(1, schedule.size)
        assertTrue(schedule[0].on)
        assertEquals(60, schedule[0].durationMs)
    }

    @Test
    fun beeper_scheduleForA_hasGapBetweenElements() {
        val schedule = MorseBeeper.schedule('A', MorseTiming(60))
        // tone(dit) gap tone(dah)
        assertEquals(3, schedule.size)
        assertTrue(schedule[0].on)
        assertTrue(!schedule[1].on)
        assertTrue(schedule[2].on)
    }

    @Test
    fun beeper_unknownLetter_isEmpty() {
        assertTrue(MorseBeeper.schedule('1', MorseTiming(60)).isEmpty())
    }
}
