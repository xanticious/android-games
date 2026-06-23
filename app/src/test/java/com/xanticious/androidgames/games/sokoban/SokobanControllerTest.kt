package com.xanticious.androidgames.games.sokoban

import com.xanticious.androidgames.controller.games.sokoban.SokobanController
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SokobanControllerTest {
    private val controller = SokobanController()

    // Level 0 helpers — "#####\n#@$.#\n#####"
    // Player(1,1) Box(1,2) Goal(1,3)
    private val level0 = controller.allLevels[0]
    private val state0 = controller.startLevel(level0, 0)

    // ── parseLevel ────────────────────────────────────────────────────────────

    @Test
    fun parseLevel_playerPosition_matchesAtSign() {
        assertEquals(GridPos(1, 1), state0.player)
    }

    @Test
    fun parseLevel_boxPosition_matchesDollarSign() {
        assertTrue(GridPos(1, 2) in state0.boxes)
    }

    @Test
    fun parseLevel_goalPosition_matchesDot() {
        assertTrue(GridPos(1, 3) in state0.level.goals)
    }

    @Test
    fun parseLevel_wallCell_isWall() {
        assertEquals(
            com.xanticious.androidgames.model.games.sokoban.SokobanCell.WALL,
            state0.cell(GridPos(0, 0))
        )
    }

    // ── move: walking ─────────────────────────────────────────────────────────

    @Test
    fun move_intoFloor_movesPlayer() {
        // Level 0: player(1,1). Moving RIGHT hits the box, so move DOWN first.
        // Use a level with open floor. Level 1 has open space below player.
        val s = controller.startLevel(controller.allLevels[1], 1) // player(1,1)
        val moved = controller.move(s, Direction.RIGHT)
        assertEquals(GridPos(1, 2), moved.player)
    }

    @Test
    fun move_intoFloor_incrementsMoves() {
        val s = controller.startLevel(controller.allLevels[1], 1)
        val moved = controller.move(s, Direction.RIGHT)
        assertEquals(1, moved.moves)
    }

    @Test
    fun move_intoFloor_doesNotIncrementPushes() {
        val s = controller.startLevel(controller.allLevels[1], 1)
        val moved = controller.move(s, Direction.RIGHT)
        assertEquals(0, moved.pushes)
    }

    // ── move: pushing ─────────────────────────────────────────────────────────

    @Test
    fun move_pushBoxIntoFloor_movesBoxAndPlayer() {
        // Level 0: player(1,1), box(1,2), move RIGHT → box→(1,3), player→(1,2).
        val moved = controller.move(state0, Direction.RIGHT)
        assertEquals(GridPos(1, 2), moved.player)
        assertTrue(GridPos(1, 3) in moved.boxes)
        assertFalse(GridPos(1, 2) in moved.boxes)
    }

    @Test
    fun move_pushBoxIntoFloor_incrementsPushes() {
        val moved = controller.move(state0, Direction.RIGHT)
        assertEquals(1, moved.pushes)
    }

    @Test
    fun move_pushBoxIntoWall_isNoOp() {
        // Level 0: player(1,1), box(1,2). Move LEFT from start pushes nothing
        // (player is at (1,1), moving LEFT hits wall (1,0)).
        val moved = controller.move(state0, Direction.LEFT)
        assertEquals(state0.player, moved.player)
        assertEquals(state0.boxes, moved.boxes)
    }

    @Test
    fun move_pushBoxIntoAnotherBox_isNoOp() {
        // Construct a state where two boxes are adjacent.
        // Level 2 starts with boxes at (2,2) and (2,3).  Moving DOWN from (3,3)
        // pushes box(2,3) into box(2,2) — blocked.
        val s = controller.startLevel(controller.allLevels[2], 0) // player(3,3)
        // Move player to (3,4) first so we can push from the right side of box(2,3).
        val s2 = controller.move(s, Direction.RIGHT) // player→(3,4)
        val s3 = controller.move(s2, Direction.UP)   // player→(2,4)
        // Now push box(2,3) LEFT into box(2,2) — should be blocked.
        val s4 = controller.move(s3, Direction.LEFT)
        assertEquals(GridPos(2, 4), s3.player)
        assertEquals(s3.boxes, s4.boxes)
    }

    @Test
    fun move_intoWall_isNoOp() {
        // Level 0: player(1,1), moving UP hits wall(0,1).
        val moved = controller.move(state0, Direction.UP)
        assertEquals(state0.player, moved.player)
    }

    // ── isSolved ──────────────────────────────────────────────────────────────

    @Test
    fun isSolved_whenBoxOnGoal_returnsTrue() {
        // One RIGHT move places the box on the goal in level 0.
        val moved = controller.move(state0, Direction.RIGHT)
        assertTrue(controller.isSolved(moved))
    }

    @Test
    fun isSolved_whenBoxNotOnGoal_returnsFalse() {
        assertFalse(controller.isSolved(state0))
    }

    // ── boxesOnGoals ──────────────────────────────────────────────────────────

    @Test
    fun boxesOnGoals_initialState_isZero() {
        assertEquals(0, controller.boxesOnGoals(state0))
    }

    @Test
    fun boxesOnGoals_afterPush_isOne() {
        val moved = controller.move(state0, Direction.RIGHT)
        assertEquals(1, controller.boxesOnGoals(moved))
    }

    // ── undo ──────────────────────────────────────────────────────────────────

    @Test
    fun undo_afterMove_restoresPlayerPosition() {
        val moved = controller.move(state0, Direction.RIGHT)
        val undone = controller.undo(moved)
        assertEquals(state0.player, undone.player)
    }

    @Test
    fun undo_afterPush_restoresBoxPosition() {
        val moved = controller.move(state0, Direction.RIGHT)
        val undone = controller.undo(moved)
        assertEquals(state0.boxes, undone.boxes)
    }

    @Test
    fun undo_afterPush_restoresMoveCount() {
        val moved = controller.move(state0, Direction.RIGHT)
        val undone = controller.undo(moved)
        assertEquals(0, undone.moves)
    }

    @Test
    fun undo_withNoHistory_isNoOp() {
        val undone = controller.undo(state0)
        assertEquals(state0, undone)
    }

    @Test
    fun undo_setsUsedUndoFlag() {
        val moved = controller.move(state0, Direction.RIGHT)
        assertFalse(moved.usedUndo)
        val undone = controller.undo(moved)
        assertTrue(undone.usedUndo)
    }

    // ── complete solution sequence ────────────────────────────────────────────

    @Test
    fun level0_solutionR_solves() {
        // Solution for level 0: R
        val s1 = controller.move(state0, Direction.RIGHT)
        assertTrue(controller.isSolved(s1))
    }

    @Test
    fun level1_solution_RRD_solves() {
        // Level 1: player(1,1), box(2,3), goal(3,3). Solution: R, R, D.
        var s = controller.startLevel(controller.allLevels[1], 1)
        s = controller.move(s, Direction.RIGHT)
        s = controller.move(s, Direction.RIGHT)
        s = controller.move(s, Direction.DOWN)
        assertTrue(controller.isSolved(s))
    }

    @Test
    fun level2_solution_LUDR_U_solves() {
        // Level 2: player(3,3), 2 boxes. Solution: L U D R U.
        var s = controller.startLevel(controller.allLevels[2], 0)
        for (d in listOf(Direction.LEFT, Direction.UP, Direction.DOWN, Direction.RIGHT, Direction.UP)) {
            s = controller.move(s, d)
        }
        assertTrue(controller.isSolved(s))
    }

    // ── allLevels ─────────────────────────────────────────────────────────────

    @Test
    fun allLevels_hasExpectedCount() {
        assertEquals(6, controller.allLevels.size)
    }

    @Test
    fun allLevels_eachLevelHasMatchingBoxAndGoalCounts() {
        controller.allLevels.forEach { level ->
            assertEquals(level.goals.size, level.initialBoxes.size)
        }
    }

    @Test
    fun allLevels_eachStartStateIsUnsolved() {
        controller.allLevels.forEachIndexed { i, level ->
            val s = controller.startLevel(level, i)
            assertFalse("Level $i should not start solved", controller.isSolved(s))
        }
    }
}
