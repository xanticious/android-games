package com.xanticious.androidgames.model.games.scrabble

/**
 * Immutable 15×15 Scrabble board with premium squares. Holds tiles that have
 * been played and exposes queries about board state.
 */
data class ScrabbleBoard(
    private val tiles: Map<Position, ScrabbleTile> = emptyMap()
) {
    companion object {
        const val SIZE = 15
        private val CENTER = Position(7, 7)

        /** Standard Scrabble premium square layout. */
        private val PREMIUM_SQUARES: Map<Position, PremiumSquare> = buildMap {
            // Triple Word (corners)
            listOf(
                Position(0, 0), Position(0, 7), Position(0, 14),
                Position(7, 0), Position(7, 14),
                Position(14, 0), Position(14, 7), Position(14, 14)
            ).forEach { put(it, PremiumSquare.TRIPLE_WORD) }

            // Double Word (diagonal pattern including center)
            listOf(
                Position(1, 1), Position(2, 2), Position(3, 3), Position(4, 4),
                Position(7, 7), // Center is DW
                Position(10, 10), Position(11, 11), Position(12, 12), Position(13, 13),
                Position(1, 13), Position(2, 12), Position(3, 11), Position(4, 10),
                Position(10, 4), Position(11, 3), Position(12, 2), Position(13, 1)
            ).forEach { put(it, PremiumSquare.DOUBLE_WORD) }

            // Triple Letter
            listOf(
                Position(1, 5), Position(1, 9),
                Position(5, 1), Position(5, 5), Position(5, 9), Position(5, 13),
                Position(9, 1), Position(9, 5), Position(9, 9), Position(9, 13),
                Position(13, 5), Position(13, 9)
            ).forEach { put(it, PremiumSquare.TRIPLE_LETTER) }

            // Double Letter
            listOf(
                Position(0, 3), Position(0, 11),
                Position(2, 6), Position(2, 8),
                Position(3, 0), Position(3, 7), Position(3, 14),
                Position(6, 2), Position(6, 6), Position(6, 8), Position(6, 12),
                Position(7, 3), Position(7, 11),
                Position(8, 2), Position(8, 6), Position(8, 8), Position(8, 12),
                Position(11, 0), Position(11, 7), Position(11, 14),
                Position(12, 6), Position(12, 8),
                Position(14, 3), Position(14, 11)
            ).forEach { put(it, PremiumSquare.DOUBLE_LETTER) }
        }
    }

    fun isEmpty(): Boolean = tiles.isEmpty()

    fun getTile(pos: Position): ScrabbleTile? = tiles[pos]

    fun getPremium(pos: Position): PremiumSquare = PREMIUM_SQUARES[pos] ?: PremiumSquare.NONE

    /** Returns a new board with the given tile placed. */
    fun withTile(pos: Position, tile: ScrabbleTile): ScrabbleBoard {
        require(pos.isValid()) { "Position out of bounds: $pos" }
        require(tiles[pos] == null) { "Position already occupied: $pos" }
        return ScrabbleBoard(tiles + (pos to tile))
    }

    /** Returns a new board with multiple tiles placed. */
    fun withTiles(placed: List<PlacedTile>): ScrabbleBoard {
        var board = this
        placed.forEach { board = board.withTile(it.position, it.tile) }
        return board
    }

    /** All positions that currently have tiles. */
    fun occupiedPositions(): Set<Position> = tiles.keys

    /** Whether the center square is occupied (required for first move). */
    fun centerOccupied(): Boolean = tiles.containsKey(CENTER)

    /** Whether the given position is adjacent to an occupied square. */
    fun hasAdjacentTile(pos: Position): Boolean {
        return pos.adjacentPositions().any { tiles.containsKey(it) }
    }
}
