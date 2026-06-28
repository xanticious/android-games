package com.xanticious.androidgames.model.games.scrabble

/** Game state for regular Scrabble (player vs AI or solitaire). */
data class ScrabbleGameState(
    val board: ScrabbleBoard = ScrabbleBoard(),
    val playerRack: List<ScrabbleTile> = emptyList(),
    val aiRack: List<ScrabbleTile> = emptyList(),
    val bag: List<ScrabbleTile> = ScrabbleTile.createBag(),
    val playerScore: Int = 0,
    val aiScore: Int = 0,
    val currentTurn: Player = Player.HUMAN,
    val tentativeTiles: List<PlacedTile> = emptyList(),
    val lastMove: ScrabbleMove? = null,
    val gameOver: Boolean = false,
    val winner: Player? = null
) {
    enum class Player { HUMAN, AI }
}

/** Difficulty level for Scrabble AI opponent. */
enum class ScrabbleDifficulty {
    EASY,    // Plays first legal move found
    MEDIUM,  // Evaluates moves and picks good scoring play
    HARD     // Near-optimal play with full move generation
}
