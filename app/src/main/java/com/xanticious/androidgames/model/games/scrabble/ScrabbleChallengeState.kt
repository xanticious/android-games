package com.xanticious.androidgames.model.games.scrabble

/**
 * Game state for Scrabble Single Player Challenge (10 rounds, beat your best).
 */
data class ScrabbleChallengeState(
    val roundNumber: Int = 1,
    val board: ScrabbleBoard = ScrabbleBoard(),
    val rack: List<ScrabbleTile> = emptyList(),
    val tentativeTiles: List<PlacedTile> = emptyList(),
    val roundScore: Int = 0,
    val totalScore: Int = 0,
    val topMoves: List<ScrabbleMove> = emptyList(),
    val personalBest: Int = 0,
    val completed: Boolean = false
)

/** Board density for challenge mode. */
enum class BoardDensity {
    SPARSE,
    MEDIUM,
    DENSE
}

/** Rack difficulty for challenge mode. */
enum class RackDifficulty {
    BALANCED,
    SPICY
}
