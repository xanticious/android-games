package com.xanticious.androidgames.model.games.mastermind

/**
 * Feedback for one submitted guess row.
 *
 * [black] = pegs in the exact right position.
 * [white] = pegs with the right color but wrong position (standard non-double-counting rule).
 */
data class MastermindFeedback(val black: Int, val white: Int)

/** One submitted guess together with its scored feedback. */
data class MastermindRow(
    val guess: List<Int>,
    val feedback: MastermindFeedback
)

/**
 * Full Mastermind game state.
 *
 * [codeLength] pegs per row; [colorCount] distinct colors (0-indexed);
 * [maxGuesses] rows before the player loses; [allowDuplicates] controls
 * whether the secret may repeat colors.
 *
 * [secret] is always present but the view hides it until the game ends.
 * [guesses] is the immutable list of rows already submitted.
 * [currentGuess] is the in-progress row; null slots are unfilled.
 */
data class MastermindState(
    val codeLength: Int,
    val colorCount: Int,
    val maxGuesses: Int,
    val allowDuplicates: Boolean,
    val secret: List<Int>,
    val guesses: List<MastermindRow> = emptyList(),
    val currentGuess: List<Int?> = List(codeLength) { null }
) {
    val guessCount: Int get() = guesses.size
    val isCurrentGuessFull: Boolean get() = currentGuess.none { it == null }
}
