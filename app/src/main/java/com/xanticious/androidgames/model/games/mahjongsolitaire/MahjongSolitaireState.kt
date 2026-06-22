package com.xanticious.androidgames.model.games.mahjongsolitaire

/**
 * Suits of the standard Mahjong tile set.
 *
 * DOTS/BAMBOO/CHARACTERS: ranks 1–9, 4 copies each (36 tiles per suit).
 * WIND: ranks 1–4 (East/South/West/North), 4 copies each (16 tiles).
 * DRAGON: ranks 1–3 (Red/Green/White), 4 copies each (12 tiles).
 * FLOWER/SEASON: ranks 1–4, 1 copy each (4 tiles apiece, match any-within-group).
 */
enum class TileSuit { DOTS, BAMBOO, CHARACTERS, WIND, DRAGON, FLOWER, SEASON }

/** Which stacked layout to use. */
enum class MahjongLayout { TURTLE, PYRAMID, FORTRESS, DRAGON }

/** Standard glyphs vs. high-contrast alternative (both drawn procedurally). */
enum class TileFaceSet { TRADITIONAL, HIGH_CONTRAST }

/**
 * The face (identity) on a tile. Tiles share a [matchGroup] if they can be
 * removed as a pair: FLOWER tiles all share "flower", SEASON tiles share
 * "season", and all others must match suit + rank exactly.
 */
data class TileFace(val suit: TileSuit, val rank: Int) {
    val matchGroup: String = when (suit) {
        TileSuit.FLOWER -> "flower"
        TileSuit.SEASON -> "season"
        else -> "${suit.ordinal}_$rank"
    }
}

/**
 * A physical slot in the board layout.
 *
 * [x] and [y] are integer grid coordinates; each tile occupies a 2×2 footprint
 * (adjacent same-layer tiles are at x±2 or y±2). [layer] is the vertical level,
 * 0 = ground. [face] is `null` when the tile has been removed.
 */
data class TileSlot(
    val id: Int,
    val x: Int,
    val y: Int,
    val layer: Int,
    val face: TileFace? = null
)

/**
 * Full immutable game state for Mahjong Solitaire.
 *
 * [history] stores pairs of removed tiles (with their faces) so [canUndo] is
 * possible without any extra bookkeeping.
 */
data class MahjongSolitaireState(
    val slots: List<TileSlot>,
    val selectedId: Int? = null,
    val hintIds: Pair<Int, Int>? = null,
    val history: List<Pair<TileSlot, TileSlot>> = emptyList(),
    val shufflesUsed: Int = 0,
    val layout: MahjongLayout = MahjongLayout.TURTLE,
    val faceSet: TileFaceSet = TileFaceSet.TRADITIONAL,
    val hintsEnabled: Boolean = true,
    val guaranteedSolvable: Boolean = true,
    /** The slot-ID pairs to remove in order to solve the board (populated when
     *  [guaranteedSolvable] is true; each entry is the forward solution step). */
    val solutionPath: List<Pair<Int, Int>> = emptyList()
) {
    val activeTiles: List<TileSlot> get() = slots.filter { it.face != null }
    val pairsRemoved: Int get() = (slots.size - activeTiles.size) / 2
    val totalPairs: Int get() = slots.size / 2
    val canUndo: Boolean get() = history.isNotEmpty()
}
