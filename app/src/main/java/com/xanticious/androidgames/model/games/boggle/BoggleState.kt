package com.xanticious.androidgames.model.games.boggle

data class BoggleState(
    val grid: List<List<Char>> = emptyList(),
    val foundWords: Set<String> = emptySet(),
    val currentEntry: String = "",
    val currentPath: List<Pair<Int, Int>> = emptyList(),
    val score: Int = 0,
    val minLength: Int = 3,
    val message: String = "",
    val timeRemaining: Int = 120,
    val allPossibleWords: List<String> = emptyList()
)

data class BoggleConfig(
    val gridSize: GridSize = GridSize.CLASSIC_4X4,
    val roundDuration: Int = 120,
    val minLength: Int = 3
)

enum class GridSize(val size: Int, val label: String) {
    QUICK_3X3(3, "3×3 Quick"),
    CLASSIC_4X4(4, "4×4 Classic")
}
