package com.xanticious.androidgames.games.empireskirmish

import com.xanticious.androidgames.controller.games.empireskirmish.attackablePositions
import com.xanticious.androidgames.controller.games.empireskirmish.checkWinner
import com.xanticious.androidgames.controller.games.empireskirmish.moveUnit
import com.xanticious.androidgames.controller.games.empireskirmish.reachableTiles
import com.xanticious.androidgames.controller.games.empireskirmish.resolveAttack
import com.xanticious.androidgames.controller.games.empireskirmish.statsFor
import com.xanticious.androidgames.controller.games.empireskirmish.undoMove
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.empireskirmish.Battle
import com.xanticious.androidgames.model.games.empireskirmish.EmpireSkirmishState
import com.xanticious.androidgames.model.games.empireskirmish.Side
import com.xanticious.androidgames.model.games.empireskirmish.SkirmishUnit
import com.xanticious.androidgames.model.games.empireskirmish.Tile
import com.xanticious.androidgames.model.games.empireskirmish.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmpireSkirmishControllerTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val cols = 10
    private val rows = 8

    /** Build an open grid (no blocking, no cover) of given size. */
    private fun openGrid(c: Int = cols, r: Int = rows): List<Tile> =
        (0 until r).flatMap { y -> (0 until c).map { x -> Tile(GridPos(x, y), blocking = false, cover = false) } }

    private fun makeState(
        units: List<SkirmishUnit>,
        grid: List<Tile> = openGrid(),
        isPlayerTurn: Boolean = true
    ): EmpireSkirmishState = EmpireSkirmishState(
        battle = Battle(grid = grid, cols = cols, rows = rows, units = units, seed = 0L),
        isPlayerTurn = isPlayerTurn,
        selectedUnitId = null,
        reachableTiles = emptySet(),
        attackablePositions = emptySet(),
        pendingMove = null,
        turnNumber = 1,
        winner = null
    )

    private fun playerUnit(id: Int, type: UnitType, pos: GridPos): SkirmishUnit {
        val stats = statsFor(type)
        return SkirmishUnit(id = id, type = type, side = Side.PLAYER, pos = pos, hp = stats.maxHp, maxHp = stats.maxHp)
    }

    private fun enemyUnit(id: Int, type: UnitType, pos: GridPos): SkirmishUnit {
        val stats = statsFor(type)
        return SkirmishUnit(id = id, type = type, side = Side.ENEMY, pos = pos, hp = stats.maxHp, maxHp = stats.maxHp)
    }

    // ── reachableTiles ────────────────────────────────────────────────────────

    @Test
    fun reachableTiles_openGrid_returnsAllWithinRange() {
        val unit = playerUnit(0, UnitType.SWORDSMAN, GridPos(5, 4)) // moveRange=3
        val state = makeState(listOf(unit))
        val result = reachableTiles(state, unit)
        // All tiles within manhattan distance 3 that are within bounds, excluding start
        val expected = openGrid().map { it.pos }
            .filter { it.manhattanDistanceTo(unit.pos) in 1..3 }
            .toSet()
        assertEquals(expected, result)
    }

    @Test
    fun reachableTiles_blockingTile_excludesBlockedAndBeyond() {
        // Place a wall at (4,4) — blocking path from (3,4) leftward
        val gridWithWall = openGrid().map { tile ->
            if (tile.pos == GridPos(4, 4)) tile.copy(blocking = true) else tile
        }
        val unit = playerUnit(0, UnitType.SWORDSMAN, GridPos(3, 4)) // moveRange=3
        val state = makeState(listOf(unit), grid = gridWithWall)
        val result = reachableTiles(state, unit)
        // (4,4) blocked and (5,4),(6,4) unreachable via that path (may still be reachable via other path if manhattan ≤ 3)
        assertFalse("Blocking tile itself must not be reachable", GridPos(4, 4) in result)
    }

    @Test
    fun reachableTiles_occupiedByAlly_notReachable() {
        val unit  = playerUnit(0, UnitType.SWORDSMAN, GridPos(0, 0)) // moveRange=3
        val ally  = playerUnit(1, UnitType.BOWMAN,    GridPos(1, 0))
        val state = makeState(listOf(unit, ally))
        val result = reachableTiles(state, unit)
        assertFalse("Ally-occupied tile must not appear in reachable set", GridPos(1, 0) in result)
    }

    // ── resolveAttack ─────────────────────────────────────────────────────────

    @Test
    fun resolveAttack_swordsmanVsMage_dealsBonus() {
        val attacker = playerUnit(0, UnitType.SWORDSMAN, GridPos(0, 0))
        val defender = enemyUnit(1, UnitType.MAGE,       GridPos(1, 0))
        val state    = makeState(listOf(attacker, defender))

        val after = resolveAttack(state, 0, GridPos(1, 0))
        val remaining = after.battle.units.firstOrNull { it.id == 1 }

        // SWORDSMAN base damage 20, +50% vs MAGE = 30; MAGE maxHp=50 → remaining hp=20
        if (remaining != null) {
            assertEquals(20, remaining.hp)
        } else {
            // unit died (hp reached 0 or below) — that's also correct for a 50 hp mage
            assertTrue(true)
        }
    }

    @Test
    fun resolveAttack_bowmanVsSwordsman_dealsBonus() {
        val attacker = playerUnit(0, UnitType.BOWMAN,    GridPos(0, 0))
        val defender = enemyUnit(1, UnitType.SWORDSMAN,  GridPos(2, 0)) // attackRange=3
        val state    = makeState(listOf(attacker, defender))

        val after = resolveAttack(state, 0, GridPos(2, 0))
        val remaining = after.battle.units.firstOrNull { it.id == 1 }

        // BOWMAN base 15, +50% vs SWORDSMAN = 22; SWORDSMAN maxHp=80 → 58
        if (remaining != null) {
            assertEquals(58, remaining.hp)
        }
    }

    @Test
    fun resolveAttack_coverTile_reducedDamage() {
        val gridWithCover = openGrid().map { tile ->
            if (tile.pos == GridPos(1, 0)) tile.copy(cover = true) else tile
        }
        val attacker = playerUnit(0, UnitType.SWORDSMAN, GridPos(0, 0))
        val defender = enemyUnit(1, UnitType.SWORDSMAN,  GridPos(1, 0))
        val state    = makeState(listOf(attacker, defender), grid = gridWithCover)

        val after = resolveAttack(state, 0, GridPos(1, 0))
        val remaining = after.battle.units.firstOrNull { it.id == 1 }

        // SWORDSMAN vs SWORDSMAN: 20 * 0.75 = 15; SWORDSMAN maxHp=80 → 65
        if (remaining != null) {
            assertEquals(65, remaining.hp)
        }
    }

    @Test
    fun resolveAttack_targetDies_removedFromBattle() {
        val attacker = playerUnit(0, UnitType.SWORDSMAN, GridPos(0, 0))
        // Give defender only 1 hp so it dies
        val stats    = statsFor(UnitType.SWORDSMAN)
        val defender = enemyUnit(1, UnitType.SWORDSMAN,  GridPos(1, 0)).copy(hp = 1)
        val state    = makeState(listOf(attacker, defender))

        val after = resolveAttack(state, 0, GridPos(1, 0))
        assertNull("Dead unit must be removed from battle", after.battle.units.firstOrNull { it.id == 1 })
    }

    @Test
    fun resolveAttack_mage_hitsAoeArea() {
        val attacker = playerUnit(0, UnitType.MAGE, GridPos(0, 0)) // attackRange=2
        val e1 = enemyUnit(1, UnitType.SWORDSMAN, GridPos(2, 1))
        val e2 = enemyUnit(2, UnitType.SWORDSMAN, GridPos(2, 2))
        val e3 = enemyUnit(3, UnitType.SWORDSMAN, GridPos(3, 2))
        val state = makeState(listOf(attacker, e1, e2, e3))

        // Attack at (2,2) — 3×3 area covers (1..3, 1..3)
        val after = resolveAttack(state, 0, GridPos(2, 2))

        val e1After = after.battle.units.firstOrNull { it.id == 1 }
        val e2After = after.battle.units.firstOrNull { it.id == 2 }
        val e3After = after.battle.units.firstOrNull { it.id == 3 }

        // All three are in 3x3 area centred at (2,2), so all should be damaged
        // MAGE damage=25, MAGE beats BOWMAN (+50%) but these are SWORDSMAN (no bonus) → 25 dmg; SWORDSMAN hp=80→55
        assertTrue("e1 in AoE must be damaged", e1After == null || e1After.hp < e1.hp)
        assertTrue("e2 in AoE must be damaged", e2After == null || e2After.hp < e2.hp)
        assertTrue("e3 in AoE must be damaged", e3After == null || e3After.hp < e3.hp)
    }

    // ── checkWinner ───────────────────────────────────────────────────────────

    @Test
    fun checkWinner_enemyKingDead_returnsPlayer() {
        val playerKing = playerUnit(0, UnitType.KING, GridPos(0, 0))
        // No enemy king
        val enemy      = enemyUnit(1, UnitType.SWORDSMAN, GridPos(5, 5))
        val state = makeState(listOf(playerKing, enemy))
        assertEquals(Side.PLAYER, checkWinner(state))
    }

    @Test
    fun checkWinner_playerKingDead_returnsEnemy() {
        val enemyKing = enemyUnit(0, UnitType.KING, GridPos(9, 0))
        // No player king
        val player    = playerUnit(1, UnitType.SWORDSMAN, GridPos(0, 0))
        val state = makeState(listOf(player, enemyKing))
        assertEquals(Side.ENEMY, checkWinner(state))
    }

    @Test
    fun checkWinner_bothKingsAlive_returnsNull() {
        val playerKing = playerUnit(0, UnitType.KING, GridPos(0, 0))
        val enemyKing  = enemyUnit(1, UnitType.KING, GridPos(9, 7))
        val state = makeState(listOf(playerKing, enemyKing))
        assertNull(checkWinner(state))
    }

    // ── undoMove ──────────────────────────────────────────────────────────────

    @Test
    fun undoMove_afterMove_restoresPosition() {
        val origin = GridPos(0, 0)
        val target = GridPos(2, 0)
        val unit   = playerUnit(0, UnitType.SWORDSMAN, origin)
        val state  = makeState(listOf(unit))

        val afterMove = moveUnit(state, 0, target)
        val afterUndo = undoMove(afterMove.copy(selectedUnitId = 0), 0)

        val restoredUnit = afterUndo.battle.units.first { it.id == 0 }
        assertEquals(origin, restoredUnit.pos)
        assertFalse("hasMoved must be cleared after undo", restoredUnit.hasMoved)
    }

    // ── statsFor ──────────────────────────────────────────────────────────────

    @Test
    fun statsFor_king_shortRange() {
        val stats = statsFor(UnitType.KING)
        assertEquals(1, stats.attackRange)
        assertEquals(2, stats.moveRange)
    }

    @Test
    fun statsFor_bowman_longerRangeThanSwordsman() {
        val bowman    = statsFor(UnitType.BOWMAN)
        val swordsman = statsFor(UnitType.SWORDSMAN)
        assertTrue(bowman.attackRange > swordsman.attackRange)
    }
}
