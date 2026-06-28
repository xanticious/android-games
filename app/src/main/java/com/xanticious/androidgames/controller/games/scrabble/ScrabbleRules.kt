package com.xanticious.androidgames.controller.games.scrabble

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.scrabble.*
import kotlin.random.Random

/**
 * Pure Scrabble game rules engine. Validates moves, computes scores with
 * premium squares and bingo bonus, generates legal moves for AI and challenge
 * modes. No Android imports; unit-testable on the JVM.
 *
 * Per word-data-sources.md, ideally uses a separate Scrabble players dictionary,
 * but falls back to the shared WordData list for now.
 */
object ScrabbleRules {
    const val RACK_SIZE = 7
    const val BINGO_BONUS = 50

    /**
     * Validation result for a proposed move.
     */
    sealed class MoveValidation {
        data object Valid : MoveValidation()
        data class Invalid(val reason: String) : MoveValidation()
    }

    /**
     * Validates a proposed move against the current board and dictionary.
     * Checks: tiles form one line, connect to existing tiles (or cover center),
     * all formed words are valid.
     */
    fun validateMove(
        board: ScrabbleBoard,
        move: ScrabbleMove,
        wordData: WordData
    ): MoveValidation {
        if (move.tiles.isEmpty()) {
            return MoveValidation.Invalid("No tiles placed")
        }

        // Check all positions are empty
        for (placed in move.tiles) {
            if (board.getTile(placed.position) != null) {
                return MoveValidation.Invalid("Position already occupied")
            }
        }

        // Check tiles form a single line
        if (!formsLine(move.tiles)) {
            return MoveValidation.Invalid("Tiles must form a single line")
        }

        // Check connection to existing tiles or center
        if (board.isEmpty()) {
            val coversCenter = move.tiles.any { it.position == Position(7, 7) }
            if (!coversCenter) {
                return MoveValidation.Invalid("First move must cover center square")
            }
        } else {
            val connects = move.tiles.any { placed ->
                board.hasAdjacentTile(placed.position)
            }
            if (!connects) {
                return MoveValidation.Invalid("Move must connect to existing tiles")
            }
        }

        // Extract all words formed
        val tempBoard = board.withTiles(move.tiles)
        val words = extractFormedWords(board, move, tempBoard)

        // Validate all words
        for (word in words) {
            if (word.length == 1) continue  // Single letters don't need validation
            if (!wordData.isValidWord(word)) {
                return MoveValidation.Invalid("Invalid word: $word")
            }
        }

        return MoveValidation.Valid
    }

    /**
     * Scores a validated move. Includes letter/word multipliers and bingo bonus.
     */
    fun scoreMove(board: ScrabbleBoard, move: ScrabbleMove): Int {
        if (move.tiles.isEmpty()) return 0

        val tempBoard = board.withTiles(move.tiles)
        var totalScore = 0

        // Score main word
        totalScore += scoreWord(board, move.tiles, move.direction, tempBoard)

        // Score cross-words
        move.tiles.forEach { placed ->
            val crossDirection = if (move.direction == Direction.HORIZONTAL) Direction.VERTICAL else Direction.HORIZONTAL
            val crossWord = extractWordAt(placed.position, crossDirection, tempBoard)
            if (crossWord.size > 1) {
                totalScore += scoreWord(board, listOf(placed), crossDirection, tempBoard)
            }
        }

        // Bingo bonus
        if (move.tiles.size == RACK_SIZE) {
            totalScore += BINGO_BONUS
        }

        return totalScore
    }

    /**
     * Generates all legal moves for the given rack on the given board.
     * Returns list sorted by score descending.
     */
    fun generateMoves(
        board: ScrabbleBoard,
        rack: List<ScrabbleTile>,
        wordData: WordData,
        maxMoves: Int = Int.MAX_VALUE
    ): List<ScrabbleMove> {
        if (rack.isEmpty()) return emptyList()

        val moves = mutableListOf<ScrabbleMove>()

        if (board.isEmpty()) {
            // First move: words through center
            moves.addAll(generateFirstMoves(rack, wordData))
        } else {
            // Generate moves at all anchor points
            val anchors = findAnchorSquares(board)
            for (anchor in anchors) {
                moves.addAll(generateMovesAt(anchor, board, rack, wordData))
            }
        }

        return moves
            .distinctBy { move -> 
                // Dedup by positions + letters
                move.tiles.map { it.position to (it.tile.assignedLetter ?: it.tile.letter) }.toSet()
            }
            .sortedByDescending { it.score }
            .take(maxMoves)
    }

    /**
     * Returns true as soon as a single legal move is found, without enumerating
     * or scoring every possibility. Much cheaper than `generateMoves(...).isNotEmpty()`
     * for callers that only need to know whether a playable layout exists.
     */
    fun hasAnyMove(
        board: ScrabbleBoard,
        rack: List<ScrabbleTile>,
        wordData: WordData
    ): Boolean {
        if (rack.isEmpty()) return false

        if (board.isEmpty()) {
            for (length in 2..rack.size.coerceAtMost(7)) {
                for (tiles in combinations(rack, length)) {
                    for (perm in permutations(tiles)) {
                        val word = perm.joinToString("") { it.letter.toString() }
                        if (wordData.isValidWord(word)) return true
                    }
                }
            }
            return false
        }

        val anchors = findAnchorSquares(board)
        for (anchor in anchors) {
            for (direction in listOf(Direction.HORIZONTAL, Direction.VERTICAL)) {
                for (length in 1..rack.size.coerceAtMost(7)) {
                    for (tiles in combinations(rack, length)) {
                        for (perm in permutations(tiles)) {
                            for (offset in -length..0) {
                                val placed = perm.mapIndexed { i, tile ->
                                    val pos = if (direction == Direction.HORIZONTAL) {
                                        Position(anchor.row, anchor.col + offset + i)
                                    } else {
                                        Position(anchor.row + offset + i, anchor.col)
                                    }
                                    PlacedTile(pos, tile)
                                }
                                if (placed.all { it.position.isValid() } &&
                                    placed.any { it.position == anchor } &&
                                    validateMove(board, ScrabbleMove(placed, direction), wordData) is MoveValidation.Valid
                                ) {
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * Draws tiles from the bag to fill a rack up to RACK_SIZE.
     */
    fun drawTiles(
        rack: List<ScrabbleTile>,
        bag: List<ScrabbleTile>,
        random: Random
    ): Pair<List<ScrabbleTile>, List<ScrabbleTile>> {
        val needed = RACK_SIZE - rack.size
        if (needed <= 0 || bag.isEmpty()) return rack to bag

        val shuffled = bag.shuffled(random)
        val drawn = shuffled.take(needed.coerceAtMost(bag.size))
        val remaining = shuffled.drop(drawn.size)

        return (rack + drawn) to remaining
    }

    // Helper: check if tiles form a contiguous line
    private fun formsLine(tiles: List<PlacedTile>): Boolean {
        if (tiles.size == 1) return true

        val rows = tiles.map { it.position.row }.distinct()
        val cols = tiles.map { it.position.col }.distinct()

        return if (rows.size == 1) {
            // Horizontal
            val sorted = tiles.sortedBy { it.position.col }
            sorted.zipWithNext().all { (a, b) -> 
                b.position.col == a.position.col + 1 
            }
        } else if (cols.size == 1) {
            // Vertical
            val sorted = tiles.sortedBy { it.position.row }
            sorted.zipWithNext().all { (a, b) -> 
                b.position.row == a.position.row + 1 
            }
        } else {
            false
        }
    }

    // Helper: extract all words formed by this move
    private fun extractFormedWords(
        board: ScrabbleBoard,
        move: ScrabbleMove,
        tempBoard: ScrabbleBoard
    ): List<String> {
        val words = mutableListOf<String>()

        // Main word
        val mainWord = extractWordFromMove(move, tempBoard)
        if (mainWord.length > 1) {
            words.add(mainWord)
        }

        // Cross words
        move.tiles.forEach { placed ->
            val crossDirection = if (move.direction == Direction.HORIZONTAL) Direction.VERTICAL else Direction.HORIZONTAL
            val crossWord = extractWordAt(placed.position, crossDirection, tempBoard)
            if (crossWord.size > 1) {
                words.add(crossWord.joinToString("") { tile ->
                    (tile.assignedLetter ?: tile.letter).toString()
                })
            }
        }

        return words
    }

    private fun extractWordFromMove(move: ScrabbleMove, board: ScrabbleBoard): String {
        if (move.tiles.isEmpty()) return ""
        
        val start = move.tiles.minBy { 
            if (move.direction == Direction.HORIZONTAL) it.position.col else it.position.row 
        }.position
        val end = move.tiles.maxBy { 
            if (move.direction == Direction.HORIZONTAL) it.position.col else it.position.row 
        }.position

        return if (move.direction == Direction.HORIZONTAL) {
            // Extend to include adjacent tiles
            var minCol = start.col
            var maxCol = end.col
            while (minCol > 0 && board.getTile(Position(start.row, minCol - 1)) != null) minCol--
            while (maxCol < 14 && board.getTile(Position(start.row, maxCol + 1)) != null) maxCol++

            (minCol..maxCol).map { col ->
                board.getTile(Position(start.row, col))?.let {
                    (it.assignedLetter ?: it.letter).toString()
                } ?: ""
            }.joinToString("")
        } else {
            var minRow = start.row
            var maxRow = end.row
            while (minRow > 0 && board.getTile(Position(minRow - 1, start.col)) != null) minRow--
            while (maxRow < 14 && board.getTile(Position(maxRow + 1, start.col)) != null) maxRow++

            (minRow..maxRow).map { row ->
                board.getTile(Position(row, start.col))?.let {
                    (it.assignedLetter ?: it.letter).toString()
                } ?: ""
            }.joinToString("")
        }
    }

    private fun extractWordAt(pos: Position, direction: Direction, board: ScrabbleBoard): List<ScrabbleTile> {
        val tiles = mutableListOf<ScrabbleTile>()

        if (direction == Direction.HORIZONTAL) {
            var col = pos.col
            while (col > 0 && board.getTile(Position(pos.row, col - 1)) != null) col--
            while (col <= 14) {
                val tile = board.getTile(Position(pos.row, col)) ?: break
                tiles.add(tile)
                col++
            }
        } else {
            var row = pos.row
            while (row > 0 && board.getTile(Position(row - 1, pos.col)) != null) row--
            while (row <= 14) {
                val tile = board.getTile(Position(row, pos.col)) ?: break
                tiles.add(tile)
                row++
            }
        }

        return tiles
    }

    private fun scoreWord(
        originalBoard: ScrabbleBoard,
        newTiles: List<PlacedTile>,
        direction: Direction,
        finalBoard: ScrabbleBoard
    ): Int {
        if (newTiles.isEmpty()) return 0

        // Find word bounds
        val positions = newTiles.map { it.position }
        val start = if (direction == Direction.HORIZONTAL) {
            var col = positions.minOf { it.col }
            val row = positions.first().row
            while (col > 0 && finalBoard.getTile(Position(row, col - 1)) != null) col--
            Position(row, col)
        } else {
            var row = positions.minOf { it.row }
            val col = positions.first().col
            while (row > 0 && finalBoard.getTile(Position(row - 1, col)) != null) row--
            Position(row, col)
        }

        var wordScore = 0
        var wordMultiplier = 1
        var pos = start

        while (true) {
            val tile = finalBoard.getTile(pos) ?: break
            val isNewTile = newTiles.any { it.position == pos }
            
            var letterScore = tile.value
            if (isNewTile) {
                val premium = originalBoard.getPremium(pos)
                when (premium) {
                    PremiumSquare.DOUBLE_LETTER -> letterScore *= 2
                    PremiumSquare.TRIPLE_LETTER -> letterScore *= 3
                    PremiumSquare.DOUBLE_WORD -> wordMultiplier *= 2
                    PremiumSquare.TRIPLE_WORD -> wordMultiplier *= 3
                    else -> {}
                }
            }
            
            wordScore += letterScore

            pos = if (direction == Direction.HORIZONTAL) {
                Position(pos.row, pos.col + 1)
            } else {
                Position(pos.row + 1, pos.col)
            }

            if (!pos.isValid()) break
        }

        return wordScore * wordMultiplier
    }

    private fun generateFirstMoves(rack: List<ScrabbleTile>, wordData: WordData): List<ScrabbleMove> {
        val moves = mutableListOf<ScrabbleMove>()
        val center = Position(7, 7)

        // Try all subsets of rack letters
        for (length in 2..rack.size.coerceAtMost(7)) {
            combinations(rack, length).forEach { tiles ->
                permutations(tiles).forEach { perm ->
                    val word = perm.joinToString("") { it.letter.toString() }
                    if (wordData.isValidWord(word)) {
                        // Horizontal through center
                        val startCol = 7 - (length / 2)
                        val placed = perm.mapIndexed { i, tile ->
                            PlacedTile(Position(7, startCol + i), tile)
                        }
                        if (placed.any { it.position == center }) {
                            val board = ScrabbleBoard()
                            val move = ScrabbleMove(placed, Direction.HORIZONTAL)
                            val score = scoreMove(board, move)
                            moves.add(move.copy(score = score))
                        }

                        // Vertical through center
                        val startRow = 7 - (length / 2)
                        val placedV = perm.mapIndexed { i, tile ->
                            PlacedTile(Position(startRow + i, 7), tile)
                        }
                        if (placedV.any { it.position == center }) {
                            val board = ScrabbleBoard()
                            val moveV = ScrabbleMove(placedV, Direction.VERTICAL)
                            val scoreV = scoreMove(board, moveV)
                            moves.add(moveV.copy(score = scoreV))
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun findAnchorSquares(board: ScrabbleBoard): List<Position> {
        val anchors = mutableListOf<Position>()
        for (row in 0..14) {
            for (col in 0..14) {
                val pos = Position(row, col)
                if (board.getTile(pos) == null && board.hasAdjacentTile(pos)) {
                    anchors.add(pos)
                }
            }
        }
        return anchors
    }

    private fun generateMovesAt(
        anchor: Position,
        board: ScrabbleBoard,
        rack: List<ScrabbleTile>,
        wordData: WordData
    ): List<ScrabbleMove> {
        val moves = mutableListOf<ScrabbleMove>()

        // Try horizontal and vertical
        for (direction in listOf(Direction.HORIZONTAL, Direction.VERTICAL)) {
            for (length in 1..rack.size.coerceAtMost(7)) {
                combinations(rack, length).forEach { tiles ->
                    permutations(tiles).forEach { perm ->
                        // Try different starting positions relative to anchor
                        for (offset in -length..0) {
                            val placed = perm.mapIndexed { i, tile ->
                                val pos = if (direction == Direction.HORIZONTAL) {
                                    Position(anchor.row, anchor.col + offset + i)
                                } else {
                                    Position(anchor.row + offset + i, anchor.col)
                                }
                                PlacedTile(pos, tile)
                            }

                            if (placed.all { it.position.isValid() } &&
                                placed.any { it.position == anchor }) {
                                val move = ScrabbleMove(placed, direction)
                                val validation = validateMove(board, move, wordData)
                                if (validation is MoveValidation.Valid) {
                                    val score = scoreMove(board, move)
                                    moves.add(move.copy(score = score))
                                }
                            }
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun <T> combinations(items: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (items.isEmpty()) return emptyList()
        
        val result = mutableListOf<List<T>>()
        
        fun combine(start: Int, current: List<T>) {
            if (current.size == k) {
                result.add(current)
                return
            }
            for (i in start until items.size) {
                combine(i + 1, current + items[i])
            }
        }
        
        combine(0, emptyList())
        return result
    }

    private fun <T> permutations(items: List<T>): List<List<T>> {
        if (items.size <= 1) return listOf(items)
        
        val result = mutableListOf<List<T>>()
        for (i in items.indices) {
            val rest = items.filterIndexed { index, _ -> index != i }
            permutations(rest).forEach { perm ->
                result.add(listOf(items[i]) + perm)
            }
        }
        return result
    }
}
