package com.xanticious.androidgames.model.games.logicgrid

/** The player's mark on a cross-reference cell between two categories. */
enum class CellMark { BLANK, YES, NO }

/**
 * A deduction clue connecting two items across categories.
 *
 * A [Direct] clue asserts that [itemA] in [catA] and [itemB] in [catB] belong to
 * the same entity (the cell is YES). A [Negative] clue asserts they do not (NO).
 */
sealed interface LogicGridClue {
    val catA: Int
    val itemA: Int
    val catB: Int
    val itemB: Int

    data class Direct(
        override val catA: Int,
        override val itemA: Int,
        override val catB: Int,
        override val itemB: Int
    ) : LogicGridClue

    data class Negative(
        override val catA: Int,
        override val itemA: Int,
        override val catB: Int,
        override val itemB: Int
    ) : LogicGridClue
}

/**
 * Canonical key for a cell in the cross-reference matrix between two categories.
 * Always stored with [catA] < [catB] to avoid duplicate keys.
 */
data class CellKey(val catA: Int, val itemA: Int, val catB: Int, val itemB: Int)

/**
 * Immutable snapshot of a Logic Grid puzzle ("Einstein/Zebra" style).
 *
 * The puzzle has [numCats] categories, each with [numItems] unique items.
 * [solution][cat][item] = entity index (0..[numItems]-1).  Category 0 is the
 * reference so solution[0][i] == i always.  All other categories hold random
 * permutations generated at puzzle creation.
 *
 * [marks] stores player-entered YES/NO marks keyed by canonical [CellKey]
 * (catA < catB).  Missing keys are treated as BLANK.
 */
data class LogicGridState(
    val categories: List<String>,
    val items: List<List<String>>,
    val clues: List<LogicGridClue>,
    val solution: List<List<Int>>,
    val marks: Map<CellKey, CellMark> = emptyMap(),
    val struckClues: Set<Int> = emptySet(),
    val autoEliminate: Boolean = true,
    val moveCount: Int = 0
) {
    val numCats: Int get() = categories.size
    val numItems: Int get() = items.firstOrNull()?.size ?: 0

    /** Returns the player's mark for a pair, defaulting to BLANK. */
    fun getMark(catA: Int, itemA: Int, catB: Int, itemB: Int): CellMark =
        marks[canonicalKey(catA, itemA, catB, itemB)] ?: CellMark.BLANK

    companion object {
        /** Normalises a cell reference so catA is always less than catB. */
        fun canonicalKey(catA: Int, itemA: Int, catB: Int, itemB: Int): CellKey =
            if (catA <= catB) CellKey(catA, itemA, catB, itemB)
            else CellKey(catB, itemB, catA, itemA)
    }
}
