package com.xanticious.androidgames.model.games.wordsearch

data class GridPosition(val row: Int, val col: Int)

enum class Direction(val dr: Int, val dc: Int) {
    HORIZONTAL(0, 1),
    VERTICAL(1, 0),
    DIAGONAL_DOWN(1, 1),
    DIAGONAL_UP(-1, 1),
    HORIZONTAL_REV(0, -1),
    VERTICAL_REV(-1, 0),
    DIAGONAL_DOWN_REV(-1, -1),
    DIAGONAL_UP_REV(1, -1);

    fun isReverse(): Boolean = dc < 0 || (dc == 0 && dr < 0)
}

data class PlacedWord(
    val word: String,
    val startPos: GridPosition,
    val direction: Direction
)

data class WordSearchGrid(
    val size: Int,
    val cells: List<List<Char>>,
    val placedWords: List<PlacedWord>
) {
    companion object {
        fun empty(size: Int): WordSearchGrid = WordSearchGrid(
            size = size,
            cells = List(size) { List(size) { ' ' } },
            placedWords = emptyList()
        )
    }
}

data class SelectionState(
    val start: GridPosition?,
    val current: GridPosition?
) {
    val isEmpty: Boolean get() = start == null
}

data class WordSearchState(
    val grid: WordSearchGrid,
    val targetWords: List<String>,
    val foundWords: Set<String>,
    val currentSelection: SelectionState,
    val timeRemainingSeconds: Int
) {
    val allWordsFound: Boolean get() = foundWords.size == targetWords.size
}
