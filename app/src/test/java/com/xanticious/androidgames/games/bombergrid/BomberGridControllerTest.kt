package com.xanticious.androidgames.games.bombergrid

import com.xanticious.androidgames.controller.games.bombergrid.BomberGridController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.bombergrid.BomberCharacter
import com.xanticious.androidgames.model.games.bombergrid.BomberGridConfig
import com.xanticious.androidgames.model.games.bombergrid.BomberGridState
import com.xanticious.androidgames.model.games.bombergrid.BomberTeam
import com.xanticious.androidgames.model.games.bombergrid.CharacterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BomberGridControllerTest {

    private val controller = BomberGridController()

    // ── Shared helpers ───────────────────────────────────────────────────────

    private val flatTerrain = List(60) { 5 }
    private val terrainWithGap = List(60) { col -> if (col in 28..30) 0 else 5 }

    private fun defaultConfig() = BomberGridConfig(
        terrainCols = 60,
        terrainMinHeight = 4,
        terrainMaxHeight = 18,
        gravity = 9.8f,
        wind = 0f,
        moveRange = 6,
        blastRadius = 3.5f,
        projectileSpeed = 22f,
        aiAimNoise = 0.30f
    )

    /** 6-character state on flat terrain; player chars at cols 2,6,10; AI at 57,53,49. */
    private fun makeState(
        terrain: List<Int> = flatTerrain,
        chars: List<BomberCharacter> = defaultCharacters(terrain)
    ): BomberGridState = BomberGridState(
        terrain = terrain,
        characters = chars,
        activeTeam = BomberTeam.PLAYER,
        activeCharIndex = 0,
        playerCharCursor = 0,
        aiCharCursor = -1,
        round = 1,
        wind = 0f,
        moveBudget = 6,
        aimAngleDeg = 45f,
        aimPower = 0.5f,
        activeProjectile = null,
        projectileVelocity = Vec2.ZERO,
        explosionCenter = null,
        explosionRadius = 0f,
        winner = null
    )

    private fun defaultCharacters(terrain: List<Int>): List<BomberCharacter> {
        val playerCols = listOf(2, 6, 10)
        val aiCols = listOf(57, 53, 49)
        val players = playerCols.mapIndexed { i, col ->
            BomberCharacter(id = i, team = BomberTeam.PLAYER, col = col, row = terrain[col], name = "P$i")
        }
        val ai = aiCols.mapIndexed { i, col ->
            BomberCharacter(id = i + 3, team = BomberTeam.AI, col = col, row = terrain[col], name = "A$i")
        }
        return players + ai
    }

    // ── configFor ────────────────────────────────────────────────────────────

    @Test
    fun configFor_easy_hasHigherAiNoiseThanHard() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(easy.aiAimNoise > hard.aiAimNoise)
    }

    @Test
    fun configFor_medium_noiseIsBetweenEasyAndHard() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val medium = controller.configFor(GameDifficulty.MEDIUM)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(medium.aiAimNoise < easy.aiAimNoise)
        assertTrue(medium.aiAimNoise > hard.aiAimNoise)
    }

    @Test
    fun configFor_allDifficulties_haveSameMoveRange() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val medium = controller.configFor(GameDifficulty.MEDIUM)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertEquals(easy.moveRange, medium.moveRange)
        assertEquals(medium.moveRange, hard.moveRange)
    }

    // ── generateTerrain ──────────────────────────────────────────────────────

    @Test
    fun generateTerrain_returnsCorrectColumnCount() {
        val config = defaultConfig()
        val terrain = controller.generateTerrain(config)
        assertEquals(config.terrainCols, terrain.size)
    }

    @Test
    fun generateTerrain_allHeightsNonNegative() {
        val config = defaultConfig()
        val terrain = controller.generateTerrain(config)
        assertTrue(terrain.all { it >= 0 })
    }

    @Test
    fun generateTerrain_allHeightsAtMostMax() {
        val config = defaultConfig()
        val terrain = controller.generateTerrain(config)
        assertTrue(terrain.all { it <= config.terrainMaxHeight })
    }

    @Test
    fun generateTerrain_spawnAreasAreSolid() {
        val config = defaultConfig()
        // Run multiple times to reduce flakiness from randomness.
        repeat(5) {
            val terrain = controller.generateTerrain(config)
            // Player spawn cols: 2, 6, 10
            assertTrue(terrain[2] > 0)
            assertTrue(terrain[6] > 0)
            assertTrue(terrain[10] > 0)
            // AI spawn cols: 57, 53, 49
            assertTrue(terrain[57] > 0)
            assertTrue(terrain[53] > 0)
            assertTrue(terrain[49] > 0)
        }
    }

    // ── moveCharacter ────────────────────────────────────────────────────────

    @Test
    fun moveCharacter_decreasesMoveBudgetByOne() {
        val state = makeState()
        val config = defaultConfig()
        val result = controller.moveCharacter(state, config, direction = 1)
        assertEquals(state.moveBudget - 1, result.moveBudget)
    }

    @Test
    fun moveCharacter_rightIncreasesCol() {
        val state = makeState()
        val config = defaultConfig()
        val result = controller.moveCharacter(state, config, direction = 1)
        val newCol = result.characters[0].col
        assertEquals(state.characters[0].col + 1, newCol)
    }

    @Test
    fun moveCharacter_leftDecreasesCol() {
        val state = makeState()
        val config = defaultConfig()
        val result = controller.moveCharacter(state, config, direction = -1)
        val newCol = result.characters[0].col
        assertEquals(state.characters[0].col - 1, newCol)
    }

    @Test
    fun moveCharacter_doesNotMoveWhenBudgetExhausted() {
        val state = makeState().copy(moveBudget = 0)
        val config = defaultConfig()
        val result = controller.moveCharacter(state, config, direction = 1)
        assertEquals(state.characters[0].col, result.characters[0].col)
    }

    @Test
    fun moveCharacter_staysInBoundsAtLeftEdge() {
        val leftChar = defaultCharacters(flatTerrain)[0].copy(col = 0)
        val state = makeState(chars = defaultCharacters(flatTerrain).mapIndexed { i, c -> if (i == 0) leftChar else c })
        val config = defaultConfig()
        val result = controller.moveCharacter(state, config, direction = -1)
        assertEquals(0, result.characters[0].col)
    }

    @Test
    fun moveCharacter_intoAbyssColumn_eliminatesCharacter() {
        val terrain = terrainWithGap   // gap at cols 28..30
        // Place character just left of the gap
        val chars = defaultCharacters(terrain).mapIndexed { i, c ->
            if (i == 0) c.copy(col = 27, row = terrain[27]) else c
        }
        val state = makeState(terrain, chars)
        val config = defaultConfig()
        val result = controller.moveCharacter(state, config, direction = 1)
        assertEquals(CharacterStatus.ELIMINATED, result.characters[0].status)
    }

    @Test
    fun moveCharacter_updatesRowToNewTerrainHeight() {
        // Place char next to a column with different height
        val terrain = List(60) { col -> if (col == 3) 8 else 5 }
        val chars = defaultCharacters(terrain).mapIndexed { i, c ->
            if (i == 0) c.copy(col = 2, row = terrain[2]) else c
        }
        val state = makeState(terrain, chars)
        val config = defaultConfig()
        val result = controller.moveCharacter(state, config, direction = 1)
        assertEquals(8, result.characters[0].row)  // stepped up to col 3 height 8
    }

    // ── adjustAim / adjustPower ───────────────────────────────────────────────

    @Test
    fun adjustAim_increasesAngleByDelta() {
        val state = makeState().copy(aimAngleDeg = 45f)
        val result = controller.adjustAim(state, 10f)
        assertEquals(55f, result.aimAngleDeg, 0.001f)
    }

    @Test
    fun adjustAim_clampsToMinimum() {
        val state = makeState().copy(aimAngleDeg = 5f)
        val result = controller.adjustAim(state, -90f)
        assertEquals(5f, result.aimAngleDeg, 0.001f)
    }

    @Test
    fun adjustAim_clampsToMaximum() {
        val state = makeState().copy(aimAngleDeg = 175f)
        val result = controller.adjustAim(state, 90f)
        assertEquals(175f, result.aimAngleDeg, 0.001f)
    }

    @Test
    fun adjustPower_increasesPower() {
        val state = makeState().copy(aimPower = 0.5f)
        val result = controller.adjustPower(state, 0.2f)
        assertEquals(0.7f, result.aimPower, 0.001f)
    }

    @Test
    fun adjustPower_clampsToMaximum() {
        val state = makeState().copy(aimPower = 1.0f)
        val result = controller.adjustPower(state, 0.5f)
        assertEquals(1.0f, result.aimPower, 0.001f)
    }

    // ── launchProjectile ─────────────────────────────────────────────────────

    @Test
    fun launchProjectile_setsActiveProjectileNearCharacter() {
        val state = makeState().copy(aimAngleDeg = 45f, aimPower = 1.0f)
        val config = defaultConfig()
        val result = controller.launchProjectile(state, config)
        val char = state.characters[0]
        assertNotNull(result.activeProjectile)
        val proj = result.activeProjectile!!
        assertEquals(char.col.toFloat(), proj.x, 0.01f)
    }

    @Test
    fun launchProjectile_horizontalRightShot_hasPositiveVx() {
        val state = makeState().copy(aimAngleDeg = 0f, aimPower = 1.0f)
        val config = defaultConfig()
        val result = controller.launchProjectile(state, config)
        assertTrue(result.projectileVelocity.x > 0f)
        assertEquals(0f, result.projectileVelocity.y, 0.01f)
    }

    @Test
    fun launchProjectile_verticalUpShot_hasPositiveVyZeroVx() {
        val state = makeState().copy(aimAngleDeg = 90f, aimPower = 1.0f)
        val config = defaultConfig()
        val result = controller.launchProjectile(state, config)
        assertEquals(0f, result.projectileVelocity.x, 0.01f)
        assertTrue(result.projectileVelocity.y > 0f)
    }

    // ── computeTrajectory ────────────────────────────────────────────────────

    @Test
    fun computeTrajectory_zeroGravityZeroWind_isApproximatelyStraight() {
        val config = defaultConfig().copy(gravity = 0f, wind = 0f)
        val start = Vec2(5f, 5f)
        val path = controller.computeTrajectory(start, 45f, 1.0f, config, flatTerrain)
        assertTrue(path.size >= 2)
        // With zero gravity, x and y increments should be equal (45° angle).
        val dx = path[1].x - path[0].x
        val dy = path[1].y - path[0].y
        assertEquals(dx, dy, 0.001f)
    }

    @Test
    fun computeTrajectory_withGravity_projectileCurvesDown() {
        val config = defaultConfig().copy(gravity = 9.8f, wind = 0f)
        val start = Vec2(5f, 10f)
        // Deep terrain so projectile can fly far before impact
        val deepTerrain = List(60) { 0 }
        val path = controller.computeTrajectory(start, 45f, 1.0f, config, deepTerrain, maxSteps = 200)
        assertTrue(path.size >= 5)
        // Last point should be lower than the peak
        val peakY = path.maxOf { it.y }
        assertTrue(path.last().y < peakY)
    }

    @Test
    fun computeTrajectory_returnsAtLeastStartPoint() {
        val config = defaultConfig()
        val start = Vec2(2f, 5f)
        val path = controller.computeTrajectory(start, 45f, 0.5f, config, flatTerrain)
        assertTrue(path.isNotEmpty())
        assertEquals(start.x, path[0].x, 0.001f)
        assertEquals(start.y, path[0].y, 0.001f)
    }

    // ── resolveExplosion ─────────────────────────────────────────────────────

    @Test
    fun resolveExplosion_carvesTerrainAtImpactColumn() {
        val state = makeState()
        val config = defaultConfig()
        val center = Vec2(30f, 5f)  // top of terrain at col 30
        val result = controller.resolveExplosion(state, config, center)
        // Terrain at col 30 should be reduced (or become 0)
        assertTrue(result.terrain[30] < state.terrain[30])
    }

    @Test
    fun resolveExplosion_healthyCharacterInRadius_becomesStunned() {
        val terrain = List(60) { 5 }
        // Place player char 0 directly at the blast centre
        val chars = defaultCharacters(terrain).mapIndexed { i, c ->
            if (i == 0) c.copy(col = 30, row = 5, status = CharacterStatus.HEALTHY) else c
        }
        val state = makeState(terrain, chars)
        val config = defaultConfig()
        val center = Vec2(30f, 5f)
        val result = controller.resolveExplosion(state, config, center)
        assertEquals(CharacterStatus.STUNNED, result.characters[0].status)
    }

    @Test
    fun resolveExplosion_stunnedCharacterInRadius_becomesEliminated() {
        val terrain = List(60) { 5 }
        val chars = defaultCharacters(terrain).mapIndexed { i, c ->
            if (i == 0) c.copy(col = 30, row = 5, status = CharacterStatus.STUNNED) else c
        }
        val state = makeState(terrain, chars)
        val config = defaultConfig()
        val center = Vec2(30f, 5f)
        val result = controller.resolveExplosion(state, config, center)
        assertEquals(CharacterStatus.ELIMINATED, result.characters[0].status)
    }

    @Test
    fun resolveExplosion_characterFarFromBlast_isUnharmed() {
        val state = makeState()
        val config = defaultConfig()
        // Blast at col 30; player chars are at cols 2, 6, 10 — all safely outside radius 3.5
        val center = Vec2(30f, 5f)
        val result = controller.resolveExplosion(state, config, center)
        assertEquals(CharacterStatus.HEALTHY, result.characters[0].status)
        assertEquals(CharacterStatus.HEALTHY, result.characters[1].status)
        assertEquals(CharacterStatus.HEALTHY, result.characters[2].status)
    }

    @Test
    fun resolveExplosion_clearsProjectile() {
        val state = makeState().copy(activeProjectile = Vec2(30f, 5f), projectileVelocity = Vec2(1f, 0f))
        val config = defaultConfig()
        val result = controller.resolveExplosion(state, config, Vec2(30f, 5f))
        assertNull(result.activeProjectile)
    }

    // ── resolveFalls ─────────────────────────────────────────────────────────

    @Test
    fun resolveFalls_characterAboveTerrain_snapsToSurface() {
        // Terrain carved below player 0: was height 5, now height 3
        val lowTerrain = List(60) { col -> if (col == 2) 3 else 5 }
        val chars = defaultCharacters(flatTerrain)  // player 0 row=5 but terrain now 3
        val state = makeState(lowTerrain, chars)
        val result = controller.resolveFalls(state)
        assertEquals(3, result.characters[0].row)
    }

    @Test
    fun resolveFalls_characterInAbyssColumn_isEliminated() {
        val abyssTerrain = List(60) { col -> if (col == 2) 0 else 5 }
        val chars = defaultCharacters(flatTerrain)  // player 0 still thinks col 2 is terrain 5
        val state = makeState(abyssTerrain, chars)
        val result = controller.resolveFalls(state)
        assertEquals(CharacterStatus.ELIMINATED, result.characters[0].status)
    }

    @Test
    fun resolveFalls_doesNotAffectAlreadyEliminatedCharacters() {
        val chars = defaultCharacters(flatTerrain).mapIndexed { i, c ->
            if (i == 0) c.copy(status = CharacterStatus.ELIMINATED, row = 0) else c
        }
        val state = makeState(chars = chars)
        val result = controller.resolveFalls(state)
        assertEquals(CharacterStatus.ELIMINATED, result.characters[0].status)
    }

    // ── winner ───────────────────────────────────────────────────────────────

    @Test
    fun winner_returnsPlayerWhenAllAiEliminated() {
        val chars = defaultCharacters(flatTerrain).map { c ->
            if (c.team == BomberTeam.AI) c.copy(status = CharacterStatus.ELIMINATED) else c
        }
        val state = makeState(chars = chars)
        assertEquals(BomberTeam.PLAYER, controller.winner(state))
    }

    @Test
    fun winner_returnsAiWhenAllPlayerEliminated() {
        val chars = defaultCharacters(flatTerrain).map { c ->
            if (c.team == BomberTeam.PLAYER) c.copy(status = CharacterStatus.ELIMINATED) else c
        }
        val state = makeState(chars = chars)
        assertEquals(BomberTeam.AI, controller.winner(state))
    }

    @Test
    fun winner_returnsNullWhenBothTeamsAlive() {
        val state = makeState()
        assertNull(controller.winner(state))
    }

    // ── advanceTurn ──────────────────────────────────────────────────────────

    @Test
    fun advanceTurn_switchesTeamFromPlayerToAi() {
        val state = makeState()
        val config = defaultConfig()
        val next = controller.advanceTurn(state, config)
        assertEquals(BomberTeam.AI, next.activeTeam)
    }

    @Test
    fun advanceTurn_switchesTeamFromAiToPlayer() {
        val state = makeState().copy(activeTeam = BomberTeam.AI, activeCharIndex = 3, aiCharCursor = 3)
        val config = defaultConfig()
        val next = controller.advanceTurn(state, config)
        assertEquals(BomberTeam.PLAYER, next.activeTeam)
    }

    @Test
    fun advanceTurn_resetsMovebudget() {
        val state = makeState().copy(moveBudget = 0)
        val config = defaultConfig()
        val next = controller.advanceTurn(state, config)
        assertEquals(config.moveRange, next.moveBudget)
    }

    @Test
    fun advanceTurn_rotatesCharactersOnSameTeam() {
        val state = makeState()
        val config = defaultConfig()
        // Two full round-trips to get back to player team, second character
        val afterAi = controller.advanceTurn(state, config)            // AI turn
        val afterPlayer = controller.advanceTurn(afterAi, config)      // back to player
        // Should now point to player char id=1 (index 1)
        assertEquals(BomberTeam.PLAYER, afterPlayer.activeTeam)
        val activeChar = afterPlayer.activeCharacter
        assertNotNull(activeChar)
        assertEquals(1, activeChar!!.id)
    }

    @Test
    fun advanceTurn_skipsEliminatedCharactersInRotation() {
        val config = defaultConfig()
        // Eliminate player char 1 (id=1)
        val chars = defaultCharacters(flatTerrain).mapIndexed { i, c ->
            if (i == 1) c.copy(status = CharacterStatus.ELIMINATED) else c
        }
        val state = makeState(chars = chars)
        // Advance: Player(0) -> AI, then AI -> Player should skip char 1 and pick char 2
        val afterAi = controller.advanceTurn(state, config)        // Player→AI
        val afterAi2 = controller.advanceTurn(afterAi, config)     // AI→Player (picks char 0 or 2, not 1)
        val activeId = afterAi2.activeCharacter?.id
        assertTrue(activeId != 1)
    }

    @Test
    fun advanceTurn_incrementsRoundWhenReturningToPlayer() {
        val state = makeState().copy(round = 1)
        val config = defaultConfig()
        val afterAi = controller.advanceTurn(state, config)         // Player→AI: round stays 1
        assertEquals(1, afterAi.round)
        val afterPlayer = controller.advanceTurn(afterAi, config)   // AI→Player: round→2
        assertEquals(2, afterPlayer.round)
    }

    // ── hasImpacted ──────────────────────────────────────────────────────────

    @Test
    fun hasImpacted_aboveTerrainAndInBounds_returnsFalse() {
        val state = makeState().copy(activeProjectile = Vec2(30f, 10f))
        // flatTerrain height is 5; projectile at height 10 is above
        assertTrue(!controller.hasImpacted(state))
    }

    @Test
    fun hasImpacted_belowTerrainHeight_returnsTrue() {
        val state = makeState().copy(activeProjectile = Vec2(30f, 3f))
        // flatTerrain height is 5; projectile at height 3 is inside terrain
        assertTrue(controller.hasImpacted(state))
    }

    @Test
    fun hasImpacted_outOfBoundsLeft_returnsTrue() {
        val state = makeState().copy(activeProjectile = Vec2(-1f, 8f))
        assertTrue(controller.hasImpacted(state))
    }

    @Test
    fun hasImpacted_belowZero_returnsTrue() {
        val state = makeState().copy(activeProjectile = Vec2(30f, -1f))
        assertTrue(controller.hasImpacted(state))
    }
}
