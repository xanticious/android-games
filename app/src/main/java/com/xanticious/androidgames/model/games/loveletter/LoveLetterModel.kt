package com.xanticious.androidgames.model.games.loveletter

import com.xanticious.androidgames.model.GameDifficulty

data class LoveLetterPlayer(
    val name: String,
    val isHuman: Boolean,
    val hand: List<LoveLetterCard> = emptyList(),
    val discards: List<LoveLetterCard> = emptyList(),
    val isEliminated: Boolean = false,
    /** Protected by Handmaid until this player's next turn. */
    val isProtected: Boolean = false,
    val tokens: Int = 0
)

/**
 * Full immutable snapshot of a Love Letter game.
 *
 * [pendingCardPlay] and [pendingTargetIndex] track the human's in-progress choice:
 * - null/null  → waiting for card selection
 * - card/null  → card selected, waiting for target (if required)
 * - card/idx   → card+target selected, waiting for Guard guess
 */
data class LoveLetterGame(
    val players: List<LoveLetterPlayer>,
    val deck: List<LoveLetterCard>,
    /** One card set aside face-down at the start of each round. */
    val burnedCard: LoveLetterCard?,
    /** In 2-player games, three additional cards are revealed face-up. */
    val revealedBurnCards: List<LoveLetterCard>,
    val currentPlayerIndex: Int,
    val roundNumber: Int,
    val tokensToWin: Int,
    val difficulty: GameDifficulty,
    val pendingCardPlay: LoveLetterCard? = null,
    val pendingTargetIndex: Int? = null,
    /** Human-readable description of the most recent card effect. */
    val lastEffect: String = ""
) {
    val playerCount: Int get() = players.size
    val currentPlayer: LoveLetterPlayer get() = players[currentPlayerIndex]
    val activePlayers: List<LoveLetterPlayer> get() = players.filter { !it.isEliminated }
    val activePlayerIndices: List<Int> get() = players.indices.filter { !players[it].isEliminated }
}
