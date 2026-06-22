package com.xanticious.androidgames.controller.games.mahjongsolitaire

import com.xanticious.androidgames.model.games.mahjongsolitaire.MahjongLayout
import com.xanticious.androidgames.model.games.mahjongsolitaire.MahjongSolitaireState
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileFace
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileFaceSet
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileSlot
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileSuit
import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure Mahjong Solitaire rules: tile set generation, board layouts, freedom
 * checks, matching, dealing (with guaranteed-solvable reverse-solve), undo,
 * shuffle, and solve detection.
 *
 * No Android or Compose imports — the entire rule set is JVM unit-testable.
 *
 * Coordinate model: every [TileSlot] stores integer (x, y, layer).
 * Each tile occupies a 2×2 footprint: adjacent same-layer tiles differ by 2 in
 * x or y. The freedom conditions are:
 *   • Not covered: no active tile at layer+1 with |Δx|<2 AND |Δy|<2.
 *   • At least one side open: no tile at same layer with x+2==this.x AND |Δy|<2
 *     (left blocked), OR no tile at x==this.x+2 AND |Δy|<2 (right blocked).
 */
class MahjongSolitaireController {

    // ── Tile set ────────────────────────────────────────────────────────────

    /**
     * Returns all 144 tiles in the standard Mahjong set (72 matching pairs).
     *
     * DOTS/BAMBOO/CHARACTERS ranks 1–9 × 4 copies = 108 tiles.
     * WIND ranks 1–4 × 4 copies = 16 tiles.
     * DRAGON ranks 1–3 × 4 copies = 12 tiles.
     * FLOWER ranks 1–4 × 1 copy = 4 tiles.
     * SEASON ranks 1–4 × 1 copy = 4 tiles.
     */
    fun standardTileSet(): List<TileFace> = buildList {
        for (suit in listOf(TileSuit.DOTS, TileSuit.BAMBOO, TileSuit.CHARACTERS))
            for (rank in 1..9) repeat(4) { add(TileFace(suit, rank)) }
        for (rank in 1..4) repeat(4) { add(TileFace(TileSuit.WIND, rank)) }
        for (rank in 1..3) repeat(4) { add(TileFace(TileSuit.DRAGON, rank)) }
        for (rank in 1..4) add(TileFace(TileSuit.FLOWER, rank))
        for (rank in 1..4) add(TileFace(TileSuit.SEASON, rank))
    }

    // ── Layout positions ─────────────────────────────────────────────────────

    /**
     * Returns the 144 (x, y, layer) positions for the requested [layout].
     *
     * Layers are nested, so layer N is always a strict subset of layer N-1
     * positions, which guarantees physical support and valid freedom checks.
     */
    fun layoutPositions(layout: MahjongLayout): List<Triple<Int, Int, Int>> = when (layout) {
        MahjongLayout.TURTLE -> turtlePositions()
        MahjongLayout.PYRAMID -> pyramidPositions()
        MahjongLayout.FORTRESS -> fortressPositions()
        MahjongLayout.DRAGON -> dragonPositions()
    }

    // TURTLE — dome-shaped mound: 56+44+28+12+4 = 144
    private fun turtlePositions(): List<Triple<Int, Int, Int>> = buildList {
        // Layer 0: 10×6 grid minus 4 outer corners = 56
        for (x in 0..18 step 2) for (y in 0..10 step 2) {
            if (!((x == 0 || x == 18) && (y == 0 || y == 10)))
                add(Triple(x, y, 0))
        }
        // Layer 1: 8×6 grid minus 4 outer corners = 44
        for (x in 2..16 step 2) for (y in 0..10 step 2) {
            if (!((x == 2 || x == 16) && (y == 0 || y == 10)))
                add(Triple(x, y, 1))
        }
        // Layer 2: 6×4 inner rect + 4 side extensions = 28
        for (x in 4..14 step 2) for (y in 2..8 step 2) add(Triple(x, y, 2))
        for ((ex, ey) in listOf(2 to 4, 2 to 6, 16 to 4, 16 to 6)) add(Triple(ex, ey, 2))
        // Layer 3: 4×3 = 12
        for (x in 6..12 step 2) for (y in 4..8 step 2) add(Triple(x, y, 3))
        // Layer 4: 2×2 = 4
        for (x in 8..10 step 2) for (y in 4..6 step 2) add(Triple(x, y, 4))
    }

    // PYRAMID — stepped pyramid widest at base: 56+40+24+12+8+4 = 144
    private fun pyramidPositions(): List<Triple<Int, Int, Int>> = buildList {
        for (x in 0..26 step 2) for (y in 2..8 step 2) add(Triple(x, y, 0))   // 14×4=56
        for (x in 4..22 step 2) for (y in 2..8 step 2) add(Triple(x, y, 1))   // 10×4=40
        for (x in 6..20 step 2) for (y in 2..6 step 2) add(Triple(x, y, 2))   //  8×3=24
        for (x in 8..18 step 2) for (y in 4..6 step 2) add(Triple(x, y, 3))   //  6×2=12
        for (x in 10..16 step 2) for (y in 4..6 step 2) add(Triple(x, y, 4))  //  4×2= 8
        for (x in 12..14 step 2) for (y in 4..6 step 2) add(Triple(x, y, 5))  //  2×2= 4
    }

    // FORTRESS — rectangular prism (castle block): 48+40+32+24 = 144
    private fun fortressPositions(): List<Triple<Int, Int, Int>> = buildList {
        for (x in 0..22 step 2) for (y in 2..8 step 2) add(Triple(x, y, 0))  // 12×4=48
        for (x in 2..20 step 2) for (y in 2..8 step 2) add(Triple(x, y, 1))  // 10×4=40
        for (x in 4..18 step 2) for (y in 2..8 step 2) add(Triple(x, y, 2))  //  8×4=32
        for (x in 6..16 step 2) for (y in 2..8 step 2) add(Triple(x, y, 3))  //  6×4=24
    }

    // DRAGON — long horizontal body with head and tail: 44+34+30+22+14 = 144
    private fun dragonPositions(): List<Triple<Int, Int, Int>> = buildList {
        // Layer 0: body (18×2=36) + tail at (0,2),(0,8),(2,2),(2,8) + head at (34,2),(34,8),(36,2),(36,8) = 44
        for (x in 0..34 step 2) for (y in listOf(4, 6)) add(Triple(x, y, 0))
        for (x in listOf(0, 2)) for (y in listOf(2, 8)) add(Triple(x, y, 0))
        for (x in listOf(34, 36)) for (y in listOf(2, 8)) add(Triple(x, y, 0))
        // Layer 1: x=2..34, y=4,6 → 17×2=34
        for (x in 2..34 step 2) for (y in listOf(4, 6)) add(Triple(x, y, 1))
        // Layer 2: x=4..32, y=4,6 → 15×2=30
        for (x in 4..32 step 2) for (y in listOf(4, 6)) add(Triple(x, y, 2))
        // Layer 3: x=8..28, y=4,6 → 11×2=22
        for (x in 8..28 step 2) for (y in listOf(4, 6)) add(Triple(x, y, 3))
        // Layer 4: x=12..24, y=4,6 → 7×2=14
        for (x in 12..24 step 2) for (y in listOf(4, 6)) add(Triple(x, y, 4))
    }

    // ── Freedom check ────────────────────────────────────────────────────────

    /**
     * A tile is free if:
     * 1. No tile in [present] sits directly on top (layer+1 with |Δx|<2 AND |Δy|<2).
     * 2. At least one of its long (left/right) sides is open.
     *
     * [present] should be the list of all physically present tiles: active tiles
     * for gameplay, or all layout slots (regardless of face) for the reverse-solve.
     */
    fun isFree(slot: TileSlot, present: List<TileSlot>): Boolean {
        val others = present.filter { it.id != slot.id }
        val covered = others.any { o ->
            o.layer == slot.layer + 1 && abs(o.x - slot.x) < 2 && abs(o.y - slot.y) < 2
        }
        if (covered) return false
        val leftBlocked = others.any { o ->
            o.layer == slot.layer && o.x + 2 == slot.x && abs(o.y - slot.y) < 2
        }
        val rightBlocked = others.any { o ->
            o.layer == slot.layer && o.x == slot.x + 2 && abs(o.y - slot.y) < 2
        }
        return !leftBlocked || !rightBlocked
    }

    // ── Match logic ──────────────────────────────────────────────────────────

    /** Two faces match iff they share the same [TileFace.matchGroup]. */
    fun canMatch(a: TileFace, b: TileFace): Boolean = a.matchGroup == b.matchGroup

    /** Returns every pair of free tiles in [state] whose faces can be matched. */
    fun availableMatches(state: MahjongSolitaireState): List<Pair<TileSlot, TileSlot>> {
        val active = state.activeTiles
        val free = active.filter { isFree(it, active) }
        return buildList {
            for (i in free.indices) for (j in i + 1 until free.size) {
                if (canMatch(free[i].face!!, free[j].face!!)) add(free[i] to free[j])
            }
        }
    }

    fun isStuck(state: MahjongSolitaireState): Boolean =
        state.activeTiles.isNotEmpty() && availableMatches(state).isEmpty()

    fun isSolved(state: MahjongSolitaireState): Boolean = state.activeTiles.isEmpty()

    // ── New game ─────────────────────────────────────────────────────────────

    /**
     * Deals a fresh game for [layout].
     *
     * When [guaranteedSolvable] is `true` the board is built via a reverse-solve:
     * iterate over the physical slot set, find free slots, assign a matching face
     * pair to 2 of them, remove them from the physical set, and repeat. This
     * mirrors forward play in reverse, guaranteeing at least one solution path.
     *
     * Falls back to random assignment for any slots that the reverse-solve cannot
     * place (should be rare with the given layouts; those tiles may not be solvable).
     */
    fun newGame(
        layout: MahjongLayout = MahjongLayout.TURTLE,
        random: Random = Random.Default,
        guaranteedSolvable: Boolean = true
    ): MahjongSolitaireState {
        val positions = layoutPositions(layout)

        if (!guaranteedSolvable) {
            val faces = standardTileSet().shuffled(random)
            val slots = positions.mapIndexed { i, (x, y, layer) ->
                TileSlot(i, x, y, layer, faces[i])
            }
            return MahjongSolitaireState(slots = slots, layout = layout)
        }

        // Build 72 shuffled matching pairs from the standard tile set.
        val facePairs: List<Pair<TileFace, TileFace>> = buildList {
            standardTileSet()
                .groupBy { it.matchGroup }
                .values
                .forEach { group ->
                    val s = group.shuffled(random)
                    for (i in s.indices step 2) if (i + 1 < s.size) add(s[i] to s[i + 1])
                }
        }.shuffled(random)

        // Physical slots — all start without a face.
        var remaining: List<TileSlot> = positions.mapIndexed { i, (x, y, layer) ->
            TileSlot(i, x, y, layer, null)
        }
        val result = remaining.toMutableList()

        // Reverse-solve: assign matching faces to free slot pairs.
        for ((faceA, faceB) in facePairs) {
            val free = remaining.filter { isFree(it, remaining) }
            if (free.size < 2) break
            val shuffled = free.shuffled(random)
            val slotA = shuffled[0]
            val slotB = shuffled[1]
            remaining = remaining.filter { it.id != slotA.id && it.id != slotB.id }
            val ia = result.indexOfFirst { it.id == slotA.id }
            val ib = result.indexOfFirst { it.id == slotB.id }
            if (ia >= 0) result[ia] = result[ia].copy(face = faceA)
            if (ib >= 0) result[ib] = result[ib].copy(face = faceB)
        }

        // Fill any unfilled slots (fallback) with a random subset of remaining faces.
        val usedFaces = result.mapNotNull { it.face }.toMutableList()
        val pool = standardTileSet().toMutableList()
        usedFaces.forEach { pool.remove(it) }
        pool.shuffle(random)
        var fillIdx = 0
        val finalSlots = result.map { slot ->
            if (slot.face != null) slot
            else slot.copy(face = pool.getOrElse(fillIdx++) { TileFace(TileSuit.DOTS, 1) })
        }

        return MahjongSolitaireState(slots = finalSlots, layout = layout)
    }

    // ── Interaction ──────────────────────────────────────────────────────────

    /**
     * Selects [slotId] if it is a free active tile, deselects it if already
     * selected, or does nothing if the tile is blocked.
     */
    fun selectTile(state: MahjongSolitaireState, slotId: Int): MahjongSolitaireState {
        val active = state.activeTiles
        val slot = active.firstOrNull { it.id == slotId } ?: return state
        if (!isFree(slot, active)) return state
        return if (state.selectedId == slotId) {
            state.copy(selectedId = null)
        } else {
            state.copy(selectedId = slotId, hintIds = null)
        }
    }

    /**
     * Tries to match [slotId] with the currently selected tile.
     *
     * • If no tile is selected, behaves like [selectTile].
     * • If [slotId] matches the selection, removes both and records the pair in
     *   history for [undo].
     * • On a mismatch, clears the selection.
     */
    fun tryMatch(state: MahjongSolitaireState, slotId: Int): MahjongSolitaireState {
        val selectedId = state.selectedId
            ?: return selectTile(state, slotId)

        if (selectedId == slotId) return state.copy(selectedId = null)

        val active = state.activeTiles
        val sel = active.firstOrNull { it.id == selectedId }
            ?: return selectTile(state.copy(selectedId = null), slotId)
        val tapped = active.firstOrNull { it.id == slotId }
            ?: return state.copy(selectedId = null)

        if (!isFree(sel, active) || !isFree(tapped, active)) {
            return state.copy(selectedId = null)
        }
        if (!canMatch(sel.face!!, tapped.face!!)) {
            return state.copy(selectedId = null)
        }

        val newSlots = state.slots.map { s ->
            when (s.id) {
                sel.id -> s.copy(face = null)
                tapped.id -> s.copy(face = null)
                else -> s
            }
        }
        return state.copy(
            slots = newSlots,
            selectedId = null,
            hintIds = null,
            history = state.history + (sel to tapped)
        )
    }

    /** Highlights one available matching pair as a hint. */
    fun showHint(state: MahjongSolitaireState): MahjongSolitaireState {
        val pair = availableMatches(state).firstOrNull() ?: return state
        return state.copy(selectedId = null, hintIds = pair.first.id to pair.second.id)
    }

    /**
     * Reassigns faces randomly among all currently active tiles.
     * Records one shuffle in [MahjongSolitaireState.shufflesUsed].
     */
    fun shuffle(state: MahjongSolitaireState, random: Random = Random.Default): MahjongSolitaireState {
        val active = state.activeTiles
        val faces = active.mapNotNull { it.face }.shuffled(random)
        val newSlots = state.slots.map { slot ->
            if (slot.face == null) slot
            else {
                val idx = active.indexOfFirst { it.id == slot.id }
                slot.copy(face = faces.getOrElse(idx) { slot.face })
            }
        }
        return state.copy(
            slots = newSlots,
            selectedId = null,
            hintIds = null,
            shufflesUsed = state.shufflesUsed + 1
        )
    }

    /** Restores the most recently removed tile pair, if any. */
    fun undo(state: MahjongSolitaireState): MahjongSolitaireState {
        val (slotA, slotB) = state.history.lastOrNull() ?: return state
        val newSlots = state.slots.map { s ->
            when (s.id) {
                slotA.id -> s.copy(face = slotA.face)
                slotB.id -> s.copy(face = slotB.face)
                else -> s
            }
        }
        return state.copy(
            slots = newSlots,
            selectedId = null,
            hintIds = null,
            history = state.history.dropLast(1)
        )
    }
}
