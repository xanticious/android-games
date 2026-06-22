package com.xanticious.androidgames.games.battleships

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.battleships.BattleshipsCellMarker
import com.xanticious.androidgames.state.games.battleships.BattleshipsPhase
import com.xanticious.androidgames.state.games.battleships.BattleshipsStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BattleshipsStateMachineTest {
    private fun machine() = BattleshipsStateMachine(
        difficulty = GameDifficulty.EASY,
        random = Random(1),
        scope = CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun initialPhase_startsWithPlayerTurn() {
        assertEquals(BattleshipsPhase.PLAYER_TURN, machine().phase.value)
    }

    @Test
    fun initialState_placesBothFleets() {
        val m = machine()
        assertTrue(m.state.value.playerBoard.fleet.isNotEmpty())
        assertTrue(m.state.value.aiBoard.fleet.isNotEmpty())
    }

    @Test
    fun playerFireSelected_emptyWater_passesToAi() {
        val m = machine()
        val target = firstUnoccupiedEnemyCell(m)
        m.playerFireSelected(target.first, target.second)
        assertEquals(BattleshipsPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun performAiTurn_afterPlayerShot_returnsToPlayer() {
        val m = machine()
        val target = firstUnoccupiedEnemyCell(m)
        m.playerFireSelected(target.first, target.second)
        m.performAiTurn()
        assertEquals(BattleshipsPhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun reset_afterShot_restoresPlayerTurn() {
        val m = machine()
        val target = firstUnoccupiedEnemyCell(m)
        m.playerFireSelected(target.first, target.second)
        m.reset()
        assertEquals(BattleshipsPhase.PLAYER_TURN, m.phase.value)
    }

    private fun firstUnoccupiedEnemyCell(m: BattleshipsStateMachine): Pair<Int, Int> {
        val board = m.state.value.aiBoard
        for (row in board.cells) {
            for (cell in row) {
                if (!cell.hasShip && cell.marker == BattleshipsCellMarker.UNKNOWN) {
                    return cell.coordinate.row to cell.coordinate.column
                }
            }
        }
        return 0 to 0
    }
}
