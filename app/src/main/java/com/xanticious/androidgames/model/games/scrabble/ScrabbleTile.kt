package com.xanticious.androidgames.model.games.scrabble

/**
 * A single Scrabble letter tile with its point value. Blank tiles are
 * represented as letter = '*' and can be assigned any letter when played.
 */
data class ScrabbleTile(
    val letter: Char,
    val value: Int,
    val isBlank: Boolean = false,
    val assignedLetter: Char? = null
) {
    companion object {
        /** Standard Scrabble tile letter values. */
        val TILE_VALUES = mapOf(
            'A' to 1, 'B' to 3, 'C' to 3, 'D' to 2, 'E' to 1,
            'F' to 4, 'G' to 2, 'H' to 4, 'I' to 1, 'J' to 8,
            'K' to 5, 'L' to 1, 'M' to 3, 'N' to 1, 'O' to 1,
            'P' to 3, 'Q' to 10, 'R' to 1, 'S' to 1, 'T' to 1,
            'U' to 1, 'V' to 4, 'W' to 4, 'X' to 8, 'Y' to 4, 'Z' to 10,
            '*' to 0  // blank
        )

        /** Standard Scrabble tile distribution (count per letter). */
        val TILE_DISTRIBUTION = mapOf(
            'A' to 9, 'B' to 2, 'C' to 2, 'D' to 4, 'E' to 12,
            'F' to 2, 'G' to 3, 'H' to 2, 'I' to 9, 'J' to 1,
            'K' to 1, 'L' to 4, 'M' to 2, 'N' to 6, 'O' to 8,
            'P' to 2, 'Q' to 1, 'R' to 6, 'S' to 4, 'T' to 6,
            'U' to 4, 'V' to 2, 'W' to 2, 'X' to 1, 'Y' to 2, 'Z' to 1,
            '*' to 2  // blanks
        )

        fun create(letter: Char): ScrabbleTile {
            val upperLetter = letter.uppercaseChar()
            val value = TILE_VALUES[upperLetter] ?: 0
            return ScrabbleTile(upperLetter, value, isBlank = upperLetter == '*')
        }

        /** Generate the full bag of 100 tiles per standard distribution. */
        fun createBag(): List<ScrabbleTile> {
            return buildList {
                TILE_DISTRIBUTION.forEach { (letter, count) ->
                    repeat(count) { add(create(letter)) }
                }
            }
        }
    }
}

/** Premium square type on the Scrabble board. */
enum class PremiumSquare {
    NONE,
    DOUBLE_LETTER,
    TRIPLE_LETTER,
    DOUBLE_WORD,
    TRIPLE_WORD
}

/** A position on the 15x15 Scrabble board. */
data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..14 && col in 0..14
    fun adjacentPositions(): List<Position> = listOf(
        Position(row - 1, col), Position(row + 1, col),
        Position(row, col - 1), Position(row, col + 1)
    )
}

/** Direction of word placement on the board. */
enum class Direction { HORIZONTAL, VERTICAL }

/** A placed tile on the board. */
data class PlacedTile(
    val position: Position,
    val tile: ScrabbleTile
)

/** A complete move: a list of newly-placed tiles forming one line. */
data class ScrabbleMove(
    val tiles: List<PlacedTile>,
    val direction: Direction,
    val score: Int = 0
) {
    /** The primary word formed by this move. */
    fun primaryWord(board: ScrabbleBoard): String {
        if (tiles.isEmpty()) return ""
        val start = tiles.minBy { if (direction == Direction.HORIZONTAL) it.position.col else it.position.row }
        val end = tiles.maxBy { if (direction == Direction.HORIZONTAL) it.position.col else it.position.row }
        
        return if (direction == Direction.HORIZONTAL) {
            (start.position.col..end.position.col).map { col ->
                board.getTile(Position(start.position.row, col))?.letter ?: 
                tiles.find { it.position == Position(start.position.row, col) }?.tile?.let {
                    it.assignedLetter ?: it.letter
                } ?: '?'
            }.joinToString("")
        } else {
            (start.position.row..end.position.row).map { row ->
                board.getTile(Position(row, start.position.col))?.letter ?:
                tiles.find { it.position == Position(row, start.position.col) }?.tile?.let {
                    it.assignedLetter ?: it.letter
                } ?: '?'
            }.joinToString("")
        }
    }
}
