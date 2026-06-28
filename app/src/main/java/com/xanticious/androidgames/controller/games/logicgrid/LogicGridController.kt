package com.xanticious.androidgames.controller.games.logicgrid

import com.xanticious.androidgames.model.games.logicgrid.CellKey
import com.xanticious.androidgames.model.games.logicgrid.CellMark
import com.xanticious.androidgames.model.games.logicgrid.LogicGridClue
import com.xanticious.androidgames.model.games.logicgrid.LogicGridState
import kotlin.random.Random

/** The supported puzzle sizes: (numCats, numItems). */
data class LogicGridSize(val numCats: Int, val numItems: Int)

/**
 * Pure Logic Grid rules: puzzle generation, mark application, auto-elimination,
 * contradiction detection and solved check.  No Android or Compose imports — the
 * entire rule set is JVM unit-testable.
 *
 * Puzzle generation (`design/puzzle-games/logic-grid/logic-grid-design.md`):
 *   1. Pick a random ground-truth assignment (category 0 is the identity reference;
 *      each other category gets a random permutation mapping items to entities).
 *   2. Enumerate all direct ("A is B") and negative ("A is not B") clues.
 *   3. Shuffle then greedily minimise: remove each clue that leaves the puzzle
 *      still uniquely solvable according to a constraint-propagation + backtracking
 *      solver.  The result is a minimal clue set that requires no guessing.
 *
 * The entity-bitmask solver:
 *   - possible[cat][item] = Int bitmask of still-viable entity assignments.
 *   - Category 0 is fixed: possible[0][i] = (1 shl i).
 *   - Propagation: singleton elimination + hidden-single + negative-clue arcs.
 *   - When propagation stalls, backtrack on the most-constrained variable.
 */
class LogicGridController {

    val sizes: List<LogicGridSize> = listOf(
        LogicGridSize(3, 4), // Easy
        LogicGridSize(4, 4), // Medium
        LogicGridSize(5, 5)  // Hard
    )

    // ---------------------------------------------------------------------------
    // Themes
    // ---------------------------------------------------------------------------

    private data class Theme(
        val categoryNames: List<String>,
        val categoryItems: List<List<String>>
    )

    private val themes: List<Theme> = listOf(
        Theme(
            categoryNames = listOf("Name", "Pet", "Color", "Hobby", "Drink"),
            categoryItems = listOf(
                listOf("Alice", "Bob", "Carol", "Dave", "Eve"),
                listOf("Cat", "Dog", "Bird", "Fish", "Rabbit"),
                listOf("Red", "Blue", "Green", "Yellow", "Purple"),
                listOf("Reading", "Cooking", "Painting", "Hiking", "Gaming"),
                listOf("Tea", "Coffee", "Juice", "Water", "Soda")
            )
        ),
        Theme(
            categoryNames = listOf("Name", "Job", "City", "Sport", "Food"),
            categoryItems = listOf(
                listOf("Ann", "Ben", "Cleo", "Don", "Eva"),
                listOf("Doctor", "Teacher", "Chef", "Artist", "Pilot"),
                listOf("Oslo", "Rome", "Lima", "Cairo", "Delhi"),
                listOf("Tennis", "Soccer", "Chess", "Swim", "Cycle"),
                listOf("Pizza", "Sushi", "Tacos", "Curry", "Pasta")
            )
        )
    )

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /** Deals a fresh puzzle of the given [size] using [random] for repeatability. */
    fun newGame(size: LogicGridSize, random: Random = Random.Default): LogicGridState {
        val (numCats, numItems) = size
        val theme = themes[random.nextInt(themes.size)]
        val catNames = theme.categoryNames.take(numCats)
        val catItems = theme.categoryItems.take(numCats).map { it.take(numItems) }
        val solution = generateSolution(numCats, numItems, random)
        val clues = generateMinimalClues(numCats, numItems, solution, random)
        return LogicGridState(
            categories = catNames,
            items = catItems,
            clues = clues,
            solution = solution
        )
    }

    /**
     * Applies a player mark to the given cell, returning a new state.
     *
     * When [LogicGridState.autoEliminate] is true and [value] is YES, all other
     * cells in the same row and column of that sub-matrix are set to NO
     * (unless already marked).
     */
    fun mark(
        state: LogicGridState,
        catA: Int,
        itemA: Int,
        catB: Int,
        itemB: Int,
        value: CellMark
    ): LogicGridState {
        val key = LogicGridState.canonicalKey(catA, itemA, catB, itemB)
        val newMarks = state.marks.toMutableMap()
        newMarks[key] = value

        if (value == CellMark.YES && state.autoEliminate) {
            // Canonical orientation: cA < cB
            val cA = minOf(catA, catB)
            val iA = if (catA < catB) itemA else itemB
            val cB = maxOf(catA, catB)
            val iB = if (catA < catB) itemB else itemA
            val n = state.numItems
            // Eliminate row: every (cA, iA) vs (cB, j≠iB) becomes NO
            for (j in 0 until n) {
                if (j != iB) {
                    val k = CellKey(cA, iA, cB, j)
                    if ((newMarks[k] ?: CellMark.BLANK) == CellMark.BLANK) newMarks[k] = CellMark.NO
                }
            }
            // Eliminate column: every (cA, i≠iA) vs (cB, iB) becomes NO
            for (i in 0 until n) {
                if (i != iA) {
                    val k = CellKey(cA, i, cB, iB)
                    if ((newMarks[k] ?: CellMark.BLANK) == CellMark.BLANK) newMarks[k] = CellMark.NO
                }
            }
        }

        return state.copy(marks = newMarks, moveCount = state.moveCount + 1)
    }

    /**
     * Returns true when the player's marks contain two YES values in the same
     * row or column of any sub-matrix (logically impossible assignment).
     */
    fun hasContradiction(state: LogicGridState): Boolean {
        val n = state.numItems
        for (cA in 0 until state.numCats) {
            for (cB in cA + 1 until state.numCats) {
                for (iA in 0 until n) {
                    if ((0 until n).count { iB -> state.getMark(cA, iA, cB, iB) == CellMark.YES } > 1) return true
                }
                for (iB in 0 until n) {
                    if ((0 until n).count { iA -> state.getMark(cA, iA, cB, iB) == CellMark.YES } > 1) return true
                }
            }
        }
        return false
    }

    /**
     * Returns true when the player has placed exactly one YES per row and column
     * in every sub-matrix and all YES marks agree with the hidden [solution].
     */
    fun isSolved(state: LogicGridState): Boolean {
        val n = state.numItems
        for (cA in 0 until state.numCats) {
            for (cB in cA + 1 until state.numCats) {
                for (iA in 0 until n) {
                    val yesCols = (0 until n).filter { iB ->
                        state.getMark(cA, iA, cB, iB) == CellMark.YES
                    }
                    if (yesCols.size != 1) return false
                    if (state.solution[cA][iA] != state.solution[cB][yesCols[0]]) return false
                }
            }
        }
        return true
    }

    // ---------------------------------------------------------------------------
    // Puzzle generation helpers
    // ---------------------------------------------------------------------------

    private fun generateSolution(numCats: Int, numItems: Int, random: Random): List<List<Int>> {
        val base = (0 until numItems).toList()
        return buildList {
            add(base) // category 0 is the identity reference
            for (c in 1 until numCats) add(base.shuffled(random))
        }
    }

    private fun generateMinimalClues(
        numCats: Int,
        numItems: Int,
        solution: List<List<Int>>,
        random: Random
    ): List<LogicGridClue> {
        val candidates = mutableListOf<LogicGridClue>()

        // All direct clues — one per (catA, catB, entity) triple.
        for (cA in 0 until numCats) {
            for (cB in cA + 1 until numCats) {
                for (entity in 0 until numItems) {
                    val iA = solution[cA].indexOf(entity)
                    val iB = solution[cB].indexOf(entity)
                    candidates.add(LogicGridClue.Direct(cA, iA, cB, iB))
                }
            }
        }

        // All negative clues — one per non-solution cell in each sub-matrix.
        for (cA in 0 until numCats) {
            for (cB in cA + 1 until numCats) {
                for (iA in 0 until numItems) {
                    for (iB in 0 until numItems) {
                        if (solution[cA][iA] != solution[cB][iB]) {
                            candidates.add(LogicGridClue.Negative(cA, iA, cB, iB))
                        }
                    }
                }
            }
        }

        candidates.shuffle(random)

        // Greedy minimisation: discard each clue that is redundant (puzzle still
        // has exactly one solution without it).
        val working = candidates.toMutableList()
        var idx = working.size - 1
        while (idx >= 0) {
            val without = ArrayList<LogicGridClue>(working.size - 1).apply {
                for (k in working.indices) if (k != idx) add(working[k])
            }
            if (countSolutions(numCats, numItems, without) == 1) working.removeAt(idx)
            idx--
        }
        return working
    }

    // ---------------------------------------------------------------------------
    // Solver (entity-bitmask, constraint propagation + backtracking)
    // ---------------------------------------------------------------------------

    /**
     * Counts the number of valid solutions for [clues], stopping at [limit].
     * Uses entity bitmasks: possible[cat][item] is the set of entities still
     * assignable to that item.  Category 0 is fixed as the identity reference.
     *
     * Internal visibility so unit tests can verify uniqueness directly.
     */
    internal fun countSolutions(
        numCats: Int,
        numItems: Int,
        clues: List<LogicGridClue>,
        limit: Int = 2
    ): Int {
        val allMask = (1 shl numItems) - 1
        val possible = Array(numCats) { cat ->
            IntArray(numItems) { item -> if (cat == 0) (1 shl item) else allMask }
        }

        // Apply direct clues as intersections before the first propagation pass.
        for (clue in clues.filterIsInstance<LogicGridClue.Direct>()) {
            val intersection = possible[clue.catA][clue.itemA] and possible[clue.catB][clue.itemB]
            if (intersection == 0) return 0
            possible[clue.catA][clue.itemA] = intersection
            possible[clue.catB][clue.itemB] = intersection
        }

        val negClues = clues.filterIsInstance<LogicGridClue.Negative>()
        if (!propagate(numCats, numItems, possible, negClues)) return 0

        val found = intArrayOf(0)
        search(numCats, numItems, possible, negClues, limit, found)
        return found[0]
    }

    private fun search(
        numCats: Int,
        numItems: Int,
        p: Array<IntArray>,
        negClues: List<LogicGridClue.Negative>,
        limit: Int,
        found: IntArray
    ) {
        if (found[0] >= limit) return

        // Check if fully solved (every cell is a singleton).
        var unsolved = false
        for (cat in 0 until numCats) {
            for (item in 0 until numItems) {
                if (Integer.bitCount(p[cat][item]) > 1) { unsolved = true; break }
            }
            if (unsolved) break
        }
        if (!unsolved) { found[0]++; return }

        // Pick the most-constrained unresolved variable.
        var bestCat = -1; var bestItem = -1; var bestBits = numItems + 1
        for (cat in 0 until numCats) {
            for (item in 0 until numItems) {
                val bc = Integer.bitCount(p[cat][item])
                if (bc > 1 && bc < bestBits) { bestCat = cat; bestItem = item; bestBits = bc }
            }
        }
        if (bestCat == -1) { found[0]++; return }

        // Try each possible entity for this variable.
        var remaining = p[bestCat][bestItem]
        while (remaining != 0 && found[0] < limit) {
            val bit = remaining and (-remaining) // isolate lowest set bit
            remaining = remaining xor bit         // remove it from the set
            val next = Array(numCats) { c -> p[c].copyOf() }
            next[bestCat][bestItem] = bit
            if (propagate(numCats, numItems, next, negClues)) {
                search(numCats, numItems, next, negClues, limit, found)
            }
        }
    }

    /**
     * Arc-consistency propagation on the entity-bitmask board.
     * Runs until no further deductions can be made or a contradiction is found.
     * Returns false on contradiction (any cell reduced to zero possibilities).
     */
    private fun propagate(
        numCats: Int,
        numItems: Int,
        possible: Array<IntArray>,
        negClues: List<LogicGridClue.Negative>
    ): Boolean {
        var changed = true
        while (changed) {
            changed = false
            for (cat in 0 until numCats) {
                // Singleton elimination: if one item is fixed to entity e, remove e
                // from all other items in this category.
                for (item in 0 until numItems) {
                    val mask = possible[cat][item]
                    if (mask == 0) return false
                    if (Integer.bitCount(mask) == 1) {
                        for (other in 0 until numItems) {
                            if (other != item && (possible[cat][other] and mask) != 0) {
                                possible[cat][other] = possible[cat][other] and mask.inv()
                                if (possible[cat][other] == 0) return false
                                changed = true
                            }
                        }
                    }
                }
                // Hidden single: if entity e appears in exactly one item's possible set,
                // that item must be e.
                for (e in 0 until numItems) {
                    val eBit = 1 shl e
                    var cnt = 0; var lastItem = -1
                    for (item in 0 until numItems) {
                        if (possible[cat][item] and eBit != 0) { cnt++; lastItem = item }
                    }
                    if (cnt == 0) return false
                    if (cnt == 1 && Integer.bitCount(possible[cat][lastItem]) > 1) {
                        possible[cat][lastItem] = eBit
                        changed = true
                    }
                }
            }
            // Negative-clue arcs: if either side is a singleton, forbid that entity
            // on the opposite side.
            for (clue in negClues) {
                val mA = possible[clue.catA][clue.itemA]
                val mB = possible[clue.catB][clue.itemB]
                if (mA == 0 || mB == 0) return false
                if (Integer.bitCount(mA) == 1 && (mB and mA) != 0) {
                    possible[clue.catB][clue.itemB] = mB and mA.inv()
                    if (possible[clue.catB][clue.itemB] == 0) return false
                    changed = true
                }
                if (Integer.bitCount(mB) == 1 && (mA and mB) != 0) {
                    possible[clue.catA][clue.itemA] = mA and mB.inv()
                    if (possible[clue.catA][clue.itemA] == 0) return false
                    changed = true
                }
            }
        }
        return true
    }
}
