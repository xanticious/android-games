package com.xanticious.androidgames.controller.games.mastermind

import com.xanticious.androidgames.model.games.mastermind.MastermindFeedback
import com.xanticious.androidgames.model.games.mastermind.MastermindRow
import com.xanticious.androidgames.model.games.mastermind.MastermindState
import kotlin.random.Random

/**
 * Pure Mastermind rules: code generation, scoring, and state transitions.
 *
 * No Android or Compose imports — fully JVM unit-testable.
 *
 * Scoring uses the standard non-double-counting Mastermind rule:
 *   black = positions where guess[i] == secret[i]
 *   white = Σ min(freq(c, secret), freq(c, guess)) − black
 */
class MastermindController {

    val codeLengths: List<Int> = listOf(4, 5, 6)
    val colorCounts: List<Int> = listOf(6, 7, 8)
    val guessLimits: List<Int> = listOf(8, 10, 12)

    /**
     * Generates a fresh game. When [allowDuplicates] is false the secret is a
     * permutation sample from [colors] and requires colors >= length.
     */
    fun newGame(
        length: Int,
        colors: Int,
        allowDuplicates: Boolean,
        maxGuesses: Int,
        random: Random = Random.Default
    ): MastermindState {
        require(colors >= 1)
        require(length >= 1)
        require(!allowDuplicates || colors >= 1)
        require(allowDuplicates || colors >= length) {
            "Need at least $length colors for a no-duplicate code"
        }
        val secret = if (allowDuplicates) {
            List(length) { random.nextInt(colors) }
        } else {
            (0 until colors).shuffled(random).take(length)
        }
        return MastermindState(
            codeLength = length,
            colorCount = colors,
            maxGuesses = maxGuesses,
            allowDuplicates = allowDuplicates,
            secret = secret
        )
    }

    /**
     * Scores [guess] against [secret].
     *
     * - [MastermindFeedback.black]: exact-position matches.
     * - [MastermindFeedback.white]: color-present-but-wrong-position matches,
     *   using min-frequency counting so duplicates are never counted twice.
     */
    fun score(secret: List<Int>, guess: List<Int>): MastermindFeedback {
        require(secret.size == guess.size) { "secret and guess must be the same length" }
        val black = secret.indices.count { secret[it] == guess[it] }
        val maxColor = maxOf(secret.maxOrNull() ?: 0, guess.maxOrNull() ?: 0)
        val white = (0..maxColor).sumOf { color ->
            minOf(secret.count { it == color }, guess.count { it == color })
        } - black
        return MastermindFeedback(black = black, white = white)
    }

    /**
     * Submits [guess] against the current [state]: appends a scored row and
     * resets [MastermindState.currentGuess] to all-null.
     */
    fun submit(state: MastermindState, guess: List<Int>): MastermindState {
        val feedback = score(state.secret, guess)
        return state.copy(
            guesses = state.guesses + MastermindRow(guess = guess, feedback = feedback),
            currentGuess = List(state.codeLength) { null }
        )
    }

    /** True when the last submitted row earned a full-black result. */
    fun isSolved(state: MastermindState): Boolean =
        state.guesses.lastOrNull()?.feedback?.black == state.codeLength

    /** True when the guess limit was reached without solving. */
    fun isLost(state: MastermindState): Boolean =
        !isSolved(state) && state.guesses.size >= state.maxGuesses

    /** Places [color] (or null to clear) at [slot] in [MastermindState.currentGuess]. */
    fun setSlot(state: MastermindState, slot: Int, color: Int?): MastermindState {
        val updated = state.currentGuess.toMutableList().also { it[slot] = color }
        return state.copy(currentGuess = updated)
    }

    /** Clears every slot in [MastermindState.currentGuess]. */
    fun clearCurrentGuess(state: MastermindState): MastermindState =
        state.copy(currentGuess = List(state.codeLength) { null })
}
