package com.xanticious.androidgames.games.scrabble

import com.xanticious.androidgames.controller.games.scrabble.ScrabbleRules
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.scrabble.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ScrabbleRulesTest {

    private val testWordData = WordData(
        listOf("CAT", "DOG", "CATS", "DOGS", "AT", "TO", "GO", "THE", "CART", "ART")
    )

    @Test
    fun tileValues_matchStandard() {
        assertEquals(1, ScrabbleTile.create('A').value)
        assertEquals(3, ScrabbleTile.create('B').value)
        assertEquals(10, ScrabbleTile.create('Q').value)
        assertEquals(0, ScrabbleTile.create('*').value)
    }

    @Test
    fun createBag_has100Tiles() {
        val bag = ScrabbleTile.createBag()
        assertEquals(100, bag.size)
    }

    @Test
    fun createBag_hasCorrectDistribution() {
        val bag = ScrabbleTile.createBag()
        val eCounts = bag.count { it.letter == 'E' }
        assertEquals(12, eCounts)
        val qCounts = bag.count { it.letter == 'Q' }
        assertEquals(1, qCounts)
    }

    @Test
    fun validateMove_rejectsEmptyMove() {
        val board = ScrabbleBoard()
        val move = ScrabbleMove(emptyList(), Direction.HORIZONTAL)
        val result = ScrabbleRules.validateMove(board, move, testWordData)
        assertTrue(result is ScrabbleRules.MoveValidation.Invalid)
    }

    @Test
    fun validateMove_firstMoveRequiresCenter() {
        val board = ScrabbleBoard()
        val tiles = listOf(
            PlacedTile(Position(0, 0), ScrabbleTile.create('C')),
            PlacedTile(Position(0, 1), ScrabbleTile.create('A')),
            PlacedTile(Position(0, 2), ScrabbleTile.create('T'))
        )
        val move = ScrabbleMove(tiles, Direction.HORIZONTAL)
        val result = ScrabbleRules.validateMove(board, move, testWordData)
        assertTrue(result is ScrabbleRules.MoveValidation.Invalid)
    }

    @Test
    fun validateMove_acceptsFirstMoveThroughCenter() {
        val board = ScrabbleBoard()
        val tiles = listOf(
            PlacedTile(Position(7, 6), ScrabbleTile.create('C')),
            PlacedTile(Position(7, 7), ScrabbleTile.create('A')),
            PlacedTile(Position(7, 8), ScrabbleTile.create('T'))
        )
        val move = ScrabbleMove(tiles, Direction.HORIZONTAL)
        val result = ScrabbleRules.validateMove(board, move, testWordData)
        assertEquals(ScrabbleRules.MoveValidation.Valid, result)
    }

    @Test
    fun validateMove_rejectsInvalidWord() {
        val board = ScrabbleBoard()
        val tiles = listOf(
            PlacedTile(Position(7, 6), ScrabbleTile.create('X')),
            PlacedTile(Position(7, 7), ScrabbleTile.create('Y')),
            PlacedTile(Position(7, 8), ScrabbleTile.create('Z'))
        )
        val move = ScrabbleMove(tiles, Direction.HORIZONTAL)
        val result = ScrabbleRules.validateMove(board, move, testWordData)
        assertTrue(result is ScrabbleRules.MoveValidation.Invalid)
    }

    @Test
    fun validateMove_requiresConnection() {
        var board = ScrabbleBoard()
        board = board.withTile(Position(7, 7), ScrabbleTile.create('C'))
        board = board.withTile(Position(7, 8), ScrabbleTile.create('A'))

        val tiles = listOf(
            PlacedTile(Position(0, 0), ScrabbleTile.create('D')),
            PlacedTile(Position(0, 1), ScrabbleTile.create('O')),
            PlacedTile(Position(0, 2), ScrabbleTile.create('G'))
        )
        val move = ScrabbleMove(tiles, Direction.HORIZONTAL)
        val result = ScrabbleRules.validateMove(board, move, testWordData)
        assertTrue(result is ScrabbleRules.MoveValidation.Invalid)
    }

    @Test
    fun scoreMove_basicWord() {
        val board = ScrabbleBoard()
        val tiles = listOf(
            PlacedTile(Position(7, 7), ScrabbleTile.create('C')),  // 3
            PlacedTile(Position(7, 8), ScrabbleTile.create('A')),  // 1
            PlacedTile(Position(7, 9), ScrabbleTile.create('T'))   // 1
        )
        val move = ScrabbleMove(tiles, Direction.HORIZONTAL)
        val score = ScrabbleRules.scoreMove(board, move)
        // C=3, A=1, T=1 = 5, center (7,7) is DW, so (3+1+1)*2 = 10
        assertEquals(10, score)
    }

    @Test
    fun scoreMove_bingoBonus() {
        val board = ScrabbleBoard()
        val tiles = (0..6).map { i ->
            PlacedTile(Position(7, i + 7), ScrabbleTile.create('A'))
        }
        val move = ScrabbleMove(tiles, Direction.HORIZONTAL)
        val score = ScrabbleRules.scoreMove(board, move)
        assertTrue(score >= 50) // Should include bingo bonus
    }

    @Test
    fun scoreMove_premiumSquares() {
        val board = ScrabbleBoard()
        // Position (0,0) is TW
        val tiles = listOf(
            PlacedTile(Position(0, 0), ScrabbleTile.create('C')),
            PlacedTile(Position(0, 1), ScrabbleTile.create('A'))
        )
        val move = ScrabbleMove(tiles, Direction.HORIZONTAL)
        val score = ScrabbleRules.scoreMove(board, move)
        assertEquals((3 + 1) * 3, score) // TW multiplier
    }

    @Test
    fun generateMoves_findsValidMoves() {
        val board = ScrabbleBoard()
        val rack = listOf(
            ScrabbleTile.create('C'),
            ScrabbleTile.create('A'),
            ScrabbleTile.create('T')
        )
        val moves = ScrabbleRules.generateMoves(board, rack, testWordData)
        assertTrue(moves.isNotEmpty())
        assertTrue(moves.any { move -> 
            move.tiles.size == 3 && 
            move.primaryWord(board.withTiles(move.tiles)) == "CAT"
        })
    }

    @Test
    fun generateMoves_sortsByScore() {
        val board = ScrabbleBoard()
        val rack = listOf(
            ScrabbleTile.create('C'),
            ScrabbleTile.create('A'),
            ScrabbleTile.create('T'),
            ScrabbleTile.create('S')
        )
        val moves = ScrabbleRules.generateMoves(board, rack, testWordData)
        assertTrue(moves.isNotEmpty())
        // Moves should be sorted descending by score
        for (i in 0 until moves.size - 1) {
            assertTrue(moves[i].score >= moves[i + 1].score)
        }
    }

    @Test
    fun drawTiles_fillsRackToSeven() {
        val rack = listOf(ScrabbleTile.create('A'))
        val bag = ScrabbleTile.createBag()
        val (newRack, newBag) = ScrabbleRules.drawTiles(rack, bag, Random(42))
        assertEquals(7, newRack.size)
        assertEquals(100 - 6, newBag.size)
    }

    @Test
    fun drawTiles_handlesEmptyBag() {
        val rack = listOf(ScrabbleTile.create('A'))
        val bag = emptyList<ScrabbleTile>()
        val (newRack, newBag) = ScrabbleRules.drawTiles(rack, bag, Random(42))
        assertEquals(1, newRack.size)
        assertTrue(newBag.isEmpty())
    }

    @Test
    fun premiumSquares_centerIsDoubleWord() {
        val board = ScrabbleBoard()
        assertEquals(PremiumSquare.DOUBLE_WORD, board.getPremium(Position(7, 7)))
    }

    @Test
    fun premiumSquares_cornersAreTripleWord() {
        val board = ScrabbleBoard()
        assertEquals(PremiumSquare.TRIPLE_WORD, board.getPremium(Position(0, 0)))
        assertEquals(PremiumSquare.TRIPLE_WORD, board.getPremium(Position(0, 14)))
        assertEquals(PremiumSquare.TRIPLE_WORD, board.getPremium(Position(14, 0)))
        assertEquals(PremiumSquare.TRIPLE_WORD, board.getPremium(Position(14, 14)))
    }

    @Test
    fun premiumSquares_doubleLetterPositions() {
        val board = ScrabbleBoard()
        assertEquals(PremiumSquare.DOUBLE_LETTER, board.getPremium(Position(0, 3)))
        assertEquals(PremiumSquare.DOUBLE_LETTER, board.getPremium(Position(7, 3)))
    }
}
