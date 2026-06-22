package com.xanticious.androidgames.model.games.dominoes

/** Unordered double-six domino tile. Use [of] to normalize the pip pair. */
data class DominoTile(val low: Int, val high: Int) {
    init {
        require(low in 0..6 && high in 0..6) { "Domino pips must be in 0..6." }
        require(low <= high) { "Domino tiles are stored normalized." }
    }

    val pipTotal: Int get() = low + high
    val isDouble: Boolean get() = low == high
    fun matches(pips: Int): Boolean = low == pips || high == pips
    fun otherSide(matchingPips: Int): Int = when (matchingPips) {
        low -> high
        high -> low
        else -> error("Tile $this does not contain $matchingPips")
    }

    override fun toString(): String = "$low|$high"

    companion object {
        fun of(a: Int, b: Int): DominoTile = if (a <= b) DominoTile(a, b) else DominoTile(b, a)
    }
}

data class PlayedDomino(
    val tile: DominoTile,
    val leftPips: Int,
    val rightPips: Int
)

enum class DominoesRuleset { DRAW, FIVES }
enum class DominoesPlayer { PLAYER, AI }
enum class DominoesEnd { LEFT, RIGHT }
enum class DominoesResult { NONE, PLAYER_WIN, AI_WIN }

data class DominoesConfig(
    val handSize: Int = 7,
    val targetScore: Int = 100,
    val ruleset: DominoesRuleset = DominoesRuleset.FIVES,
    val highestDoubleStarts: Boolean = true
)

data class DominoesMove(
    val tile: DominoTile,
    val end: DominoesEnd
)

data class DominoesAiDecision(
    val move: DominoesMove?,
    val drewTiles: Int = 0
)

data class DominoesState(
    val boneyard: List<DominoTile>,
    val playerHand: List<DominoTile>,
    val aiHand: List<DominoTile>,
    val line: List<PlayedDomino>,
    val leftOpen: Int?,
    val rightOpen: Int?,
    val currentPlayer: DominoesPlayer,
    val playerScore: Int,
    val aiScore: Int,
    val config: DominoesConfig,
    val result: DominoesResult,
    val roundOver: Boolean,
    val lastMessage: String,
    val moveCount: Int = 0
) {
    val isGameOver: Boolean get() = result != DominoesResult.NONE
    val openEnds: List<Int> get() = listOfNotNull(leftOpen, rightOpen)

    companion object {
        fun empty(config: DominoesConfig = DominoesConfig()): DominoesState = DominoesState(
            boneyard = emptyList(),
            playerHand = emptyList(),
            aiHand = emptyList(),
            line = emptyList(),
            leftOpen = null,
            rightOpen = null,
            currentPlayer = DominoesPlayer.PLAYER,
            playerScore = 0,
            aiScore = 0,
            config = config,
            result = DominoesResult.NONE,
            roundOver = false,
            lastMessage = "Deal a new round."
        )
    }
}
