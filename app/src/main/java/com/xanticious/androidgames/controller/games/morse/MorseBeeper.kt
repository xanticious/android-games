package com.xanticious.androidgames.controller.games.morse

import com.xanticious.androidgames.model.games.morse.MORSE
import com.xanticious.androidgames.model.games.morse.MorseTiming
import com.xanticious.androidgames.model.games.morse.Symbol

/**
 * A single scheduled segment of Morse audio: a tone of [durationMs] when [on],
 * otherwise a silent gap. The view layer turns a tone into an actual beep; this
 * controller only computes the schedule. Pure JVM, fully unit-testable.
 */
data class BeepEvent(val on: Boolean, val durationMs: Int)

/**
 * Builds evenly-spaced beep schedules from the shared Morse timing, used by
 * Morse Decoder (playback) and for Morse Code audio feedback.
 */
object MorseBeeper {

    /**
     * Schedule for a single [letter]: alternating tone/gap segments with no
     * trailing gap. Unknown letters yield an empty schedule.
     */
    fun schedule(letter: Char, timing: MorseTiming): List<BeepEvent> {
        val symbols = MORSE[letter.uppercaseChar()] ?: return emptyList()
        val events = mutableListOf<BeepEvent>()
        symbols.forEachIndexed { index, symbol ->
            if (index > 0) events += BeepEvent(on = false, durationMs = timing.elementGapMs)
            val toneMs = if (symbol == Symbol.DIT) timing.ditMs else timing.dahMs
            events += BeepEvent(on = true, durationMs = toneMs)
        }
        return events
    }

    /** Total wall-clock time (ms) to play a letter's [schedule]. */
    fun durationMs(schedule: List<BeepEvent>): Int = schedule.sumOf { it.durationMs }
}
