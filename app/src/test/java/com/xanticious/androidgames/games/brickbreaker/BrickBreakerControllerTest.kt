package com.xanticious.androidgames.games.brickbreaker

import com.xanticious.androidgames.controller.games.brickbreaker.BrickBreakerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.brickbreaker.ActivePowerUp
import com.xanticious.androidgames.model.games.brickbreaker.Ball
import com.xanticious.androidgames.model.games.brickbreaker.Brick
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerInput
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerState
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerVariant
import com.xanticious.androidgames.model.games.brickbreaker.BrickField
import com.xanticious.androidgames.model.games.brickbreaker.BrickType
import com.xanticious.androidgames.model.games.brickbreaker.PowerUpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrickBreakerControllerTest {

    private val controller = BrickBreakerController()

    // ---- configFor ----

    @Test
    fun configFor_classicEasy_hasExpectedSpeed() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        assertTrue(config.ballSpeed > 0f)
    }

    @Test
    fun configFor_classicHard_fasterThanEasy() {
        val easy = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val hard = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.HARD)
        assertTrue(hard.ballSpeed > easy.ballSpeed)
    }

    @Test
    fun configFor_cannonVariant_hasPositiveGravity() {
        val config = controller.configFor(BrickBreakerVariant.CANNON, GameDifficulty.MEDIUM)
        assertTrue(config.gravity > 0f)
    }

    @Test
    fun configFor_classicVariant_hasZeroGravity() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        assertEquals(0f, config.gravity, 0f)
    }

    @Test
    fun configFor_arcadeEasy_hasAutoFireInterval() {
        val config = controller.configFor(BrickBreakerVariant.ARCADE, GameDifficulty.EASY)
        assertTrue(config.autoFireInterval > 0f)
    }

    @Test
    fun configFor_cannonArcade_hasShotCooldown() {
        val config = controller.configFor(BrickBreakerVariant.CANNON_ARCADE, GameDifficulty.MEDIUM)
        assertTrue(config.shotCooldown > 0f)
    }

    // ---- generateLevel ----

    @Test
    fun generateLevel_classic_producesBricks() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        val state = controller.generateLevel(config, 1)
        assertTrue(state.bricks.isNotEmpty())
    }

    @Test
    fun generateLevel_cannon_producesTargetBricks() {
        val config = controller.configFor(BrickBreakerVariant.CANNON, GameDifficulty.MEDIUM)
        val state = controller.generateLevel(config, 1)
        assertTrue(state.bricks.any { it.type == BrickType.TARGET })
    }

    @Test
    fun generateLevel_level1_bricksHavePositiveHp() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val state = controller.generateLevel(config, 1)
        assertTrue(state.bricks.all { it.hp > 0 })
    }

    @Test
    fun generateLevel_setsCorrectLevel() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val state = controller.generateLevel(config, 3)
        assertEquals(3, state.level)
    }

    // ---- beginVolley ----

    @Test
    fun beginVolley_resetsToFullVolleySize() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        val state = BrickBreakerState()
        val started = controller.beginVolley(state, config)
        assertEquals(config.volleySize, started.ballsToFire)
    }

    @Test
    fun beginVolley_clearsPreviousBalls() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        val state = BrickBreakerState(balls = listOf(Ball(Vec2(0.5f, 0.5f), Vec2(0f, -0.5f))))
        val started = controller.beginVolley(state, config)
        assertTrue(started.balls.isEmpty())
    }

    // ---- launchClassicBall ----

    @Test
    fun launchClassicBall_decrementsBallsToFire() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        val state = BrickBreakerState(ballsToFire = 20)
        val after = controller.launchClassicBall(state, config)
        assertEquals(19, after.ballsToFire)
    }

    @Test
    fun launchClassicBall_addsBallToList() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        val state = BrickBreakerState(ballsToFire = 5, cannonAngleDeg = 90f)
        val after = controller.launchClassicBall(state, config)
        assertTrue(after.balls.isNotEmpty())
    }

    @Test
    fun launchClassicBall_angle90_ballMovesUpward() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        val state = BrickBreakerState(ballsToFire = 1, cannonAngleDeg = 90f)
        val after = controller.launchClassicBall(state, config)
        assertTrue(after.balls.first().vel.y < 0f)
    }

    @Test
    fun launchClassicBall_whenNoBallsLeft_returnsUnchanged() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.MEDIUM)
        val state = BrickBreakerState(ballsToFire = 0)
        val after = controller.launchClassicBall(state, config)
        assertEquals(0, after.balls.size)
    }

    // ---- launchCannonBall ----

    @Test
    fun launchCannonBall_addsBallMovingRight() {
        val config = controller.configFor(BrickBreakerVariant.CANNON, GameDifficulty.MEDIUM)
        val state = BrickBreakerState(cannonAngleDeg = 45f)
        val after = controller.launchCannonBall(state, config)
        assertTrue(after.balls.isNotEmpty())
        assertTrue(after.balls.first().vel.x > 0f)
    }

    @Test
    fun launchCannonBall_setsCooldown() {
        val config = controller.configFor(BrickBreakerVariant.CANNON_ARCADE, GameDifficulty.MEDIUM)
        val state = BrickBreakerState()
        val after = controller.launchCannonBall(state, config)
        assertTrue(after.fireCooldown > 0f)
    }

    // ---- step: ball + brick collision ----

    @Test
    fun step_ballHitsBrick_reducesBrickHp() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        // Place a brick at row 0, col 4 and a ball aimed straight at it.
        val brick = Brick(col = 4, row = 0, hp = 3, maxHp = 3, type = BrickType.STANDARD)
        // Ball position: center of the brick.
        val brickCx = (brick.col + 0.5f) / BrickField.COLS.toFloat()
        val brickCy = BrickField.TOP_MARGIN + brick.row * BrickField.ROW_HEIGHT + BrickField.ROW_HEIGHT / 2f
        val ball = Ball(pos = Vec2(brickCx, brickCy - 0.05f), vel = Vec2(0f, 0.5f))
        val state = BrickBreakerState(bricks = listOf(brick), balls = listOf(ball), ballsToFire = 0)
        val result = controller.step(state, config, 0.1f, BrickBreakerInput())
        val remainingBrick = result.state.bricks.firstOrNull { it.col == 4 && it.row == 0 }
        // Either destroyed (null) or reduced HP.
        if (remainingBrick != null) {
            assertTrue(remainingBrick.hp < brick.hp)
        }
    }

    @Test
    fun step_ballExitsBottom_removedFromList() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val ball = Ball(pos = Vec2(0.5f, 0.99f), vel = Vec2(0f, 1.0f))
        val state = BrickBreakerState(balls = listOf(ball), ballsToFire = 0)
        val result = controller.step(state, config, 0.1f, BrickBreakerInput())
        assertTrue(result.state.balls.isEmpty())
    }

    @Test
    fun step_ballHitsTopWall_bouncesDown() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val ball = Ball(pos = Vec2(0.5f, 0.02f), vel = Vec2(0f, -0.5f))
        val state = BrickBreakerState(balls = listOf(ball), ballsToFire = 0)
        val result = controller.step(state, config, 0.1f, BrickBreakerInput())
        val newBall = result.state.balls.firstOrNull()
        assertNotNull(newBall)
        assertTrue(newBall!!.vel.y > 0f)
    }

    @Test
    fun step_ballHitsLeftWall_bouncesRight() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val ball = Ball(pos = Vec2(0.02f, 0.5f), vel = Vec2(-0.5f, 0f))
        val state = BrickBreakerState(balls = listOf(ball), ballsToFire = 0)
        val result = controller.step(state, config, 0.1f, BrickBreakerInput())
        val newBall = result.state.balls.firstOrNull()
        assertNotNull(newBall)
        assertTrue(newBall!!.vel.x > 0f)
    }

    @Test
    fun step_ballHitsRightWall_bouncesLeft() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val ball = Ball(pos = Vec2(0.98f, 0.5f), vel = Vec2(0.5f, 0f))
        val state = BrickBreakerState(balls = listOf(ball), ballsToFire = 0)
        val result = controller.step(state, config, 0.1f, BrickBreakerInput())
        val newBall = result.state.balls.firstOrNull()
        assertNotNull(newBall)
        assertTrue(newBall!!.vel.x < 0f)
    }

    @Test
    fun step_destroyedBrick_addsScore() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val brick = Brick(col = 4, row = 0, hp = 1, maxHp = 1, type = BrickType.STANDARD)
        val brickCx = (brick.col + 0.5f) / BrickField.COLS.toFloat()
        val brickCy = BrickField.TOP_MARGIN + brick.row * BrickField.ROW_HEIGHT + BrickField.ROW_HEIGHT / 2f
        val ball = Ball(pos = Vec2(brickCx, brickCy - 0.03f), vel = Vec2(0f, 0.5f))
        val state = BrickBreakerState(bricks = listOf(brick), balls = listOf(ball), ballsToFire = 0)
        val result = controller.step(state, config, 0.15f, BrickBreakerInput())
        assertTrue(result.state.score > 0)
    }

    @Test
    fun step_allBallsDone_reportedWhenNoBallsOrQueue() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val state = BrickBreakerState(balls = emptyList(), ballsToFire = 0)
        val result = controller.step(state, config, 0.1f, BrickBreakerInput())
        assertTrue(result.allBallsDone)
    }

    // ---- dropBricks ----

    @Test
    fun dropBricks_incrementsAllBrickRows() {
        val bricks = listOf(Brick(0, 0, 1, 1, BrickType.STANDARD), Brick(1, 2, 1, 1, BrickType.STANDARD))
        val state = BrickBreakerState(bricks = bricks)
        val (dropped, _) = controller.dropBricks(state)
        assertEquals(1, dropped.bricks[0].row)
        assertEquals(3, dropped.bricks[1].row)
    }

    @Test
    fun dropBricks_bricksAtBottomRow_returnsAtBottomTrue() {
        val bricks = listOf(Brick(0, BrickField.ROWS - 1, 1, 1, BrickType.STANDARD))
        val state = BrickBreakerState(bricks = bricks)
        val (_, atBottom) = controller.dropBricks(state)
        assertTrue(atBottom)
    }

    @Test
    fun dropBricks_noBricksAtBottom_returnsFalse() {
        val bricks = listOf(Brick(0, 0, 1, 1, BrickType.STANDARD))
        val state = BrickBreakerState(bricks = bricks)
        val (_, atBottom) = controller.dropBricks(state)
        assertFalse(atBottom)
    }

    // ---- bricksNearBoundary ----

    @Test
    fun bricksNearBoundary_brickOnLastRow_returnsTrue() {
        val state = BrickBreakerState(bricks = listOf(Brick(0, BrickField.ROWS - 1, 1, 1, BrickType.STANDARD)))
        assertTrue(controller.bricksNearBoundary(state))
    }

    @Test
    fun bricksNearBoundary_brickWellAbove_returnsFalse() {
        val state = BrickBreakerState(bricks = listOf(Brick(0, 0, 1, 1, BrickType.STANDARD)))
        assertFalse(controller.bricksNearBoundary(state))
    }

    @Test
    fun bricksNearBoundary_noBricks_returnsFalse() {
        assertFalse(controller.bricksNearBoundary(BrickBreakerState(bricks = emptyList())))
    }

    @Test
    fun bricksNearBoundary_descentOffsetPushesBrickIntoDanger_returnsTrue() {
        val state = BrickBreakerState(bricks = listOf(Brick(0, BrickField.ROWS - 2, 1, 1, BrickType.STANDARD)))
        assertTrue(controller.bricksNearBoundary(state, descentOffset = 1f))
    }

    // ---- allBricksGone ----

    @Test
    fun allBricksGone_emptyList_returnsTrue() {
        assertTrue(controller.allBricksGone(BrickBreakerState(bricks = emptyList())))
    }

    @Test
    fun allBricksGone_withBricks_returnsFalse() {
        val state = BrickBreakerState(bricks = listOf(Brick(0, 0, 1, 1, BrickType.STANDARD)))
        assertFalse(controller.allBricksGone(state))
    }

    // ---- allTargetsDestroyed ----

    @Test
    fun allTargetsDestroyed_noTargets_returnsTrue() {
        val state = BrickBreakerState(bricks = listOf(Brick(0, 0, 1, 1, BrickType.STANDARD)))
        assertTrue(controller.allTargetsDestroyed(state))
    }

    @Test
    fun allTargetsDestroyed_withTarget_returnsFalse() {
        val state = BrickBreakerState(bricks = listOf(Brick(0, 0, 1, 1, BrickType.TARGET)))
        assertFalse(controller.allTargetsDestroyed(state))
    }

    // ---- applyPowerUp ----

    @Test
    fun applyPowerUp_explode_destroysAdjacentBricks() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val brick = Brick(col = 4, row = 2, hp = 1, maxHp = 1, type = BrickType.STANDARD)
        val state = BrickBreakerState(bricks = listOf(brick))
        val brickCx = (brick.col + 0.5f) / BrickField.COLS.toFloat()
        val brickCy = BrickField.TOP_MARGIN + brick.row * BrickField.ROW_HEIGHT + BrickField.ROW_HEIGHT / 2f
        val after = controller.applyPowerUp(state, config, PowerUpType.EXPLODE, brickCx, brickCy)
        assertTrue(after.bricks.isEmpty())
    }

    @Test
    fun applyPowerUp_clearScreen_removesAllStandardBricks() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val bricks = listOf(
            Brick(0, 0, 1, 1, BrickType.STANDARD),
            Brick(1, 0, 1, 1, BrickType.STEEL),
        )
        val state = BrickBreakerState(bricks = bricks)
        val after = controller.applyPowerUp(state, config, PowerUpType.CLEAR_SCREEN, 0.5f, 0.5f)
        assertFalse(after.bricks.any { it.type == BrickType.STANDARD })
        assertTrue(after.bricks.any { it.type == BrickType.STEEL })
    }

    @Test
    fun applyPowerUp_powerShot_addsActivePowerUp() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val state = BrickBreakerState()
        val after = controller.applyPowerUp(state, config, PowerUpType.POWER_SHOT, 0.5f, 0.5f)
        assertTrue(after.activePowerUps.any { it.type == PowerUpType.POWER_SHOT })
    }

    @Test
    fun applyPowerUp_multiShot_refreshesExistingDuration() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val existing = ActivePowerUp(PowerUpType.MULTI_SHOT, 5f)
        val state = BrickBreakerState(activePowerUps = listOf(existing))
        val after = controller.applyPowerUp(state, config, PowerUpType.MULTI_SHOT, 0.5f, 0.5f)
        val refreshed = after.activePowerUps.first { it.type == PowerUpType.MULTI_SHOT }
        assertTrue(refreshed.remainingSeconds > existing.remainingSeconds)
    }

    @Test
    fun applyPowerUp_timeBonus_addsToTimer() {
        val config = controller.configFor(BrickBreakerVariant.CANNON_ARCADE, GameDifficulty.EASY)
        val state = BrickBreakerState(timerSeconds = 30f)
        val after = controller.applyPowerUp(state, config, PowerUpType.TIME_BONUS, 0f, 0f)
        assertTrue(after.timerSeconds > 30f)
    }

    // ---- trajectoryPreview ----

    @Test
    fun trajectoryPreview_cannon_returnsNonEmptyPath() {
        val config = controller.configFor(BrickBreakerVariant.CANNON, GameDifficulty.MEDIUM)
        val points = controller.trajectoryPreview(config, 45f)
        assertTrue(points.isNotEmpty())
    }

    @Test
    fun trajectoryPreview_cannon_firstPointNearCannon() {
        val config = controller.configFor(BrickBreakerVariant.CANNON, GameDifficulty.MEDIUM)
        val points = controller.trajectoryPreview(config, 45f)
        assertTrue(points.first().x < 0.2f)
    }

    @Test
    fun classicTrajectoryPreview_returnsNonEmptyPath() {
        val points = controller.classicTrajectoryPreview(0.5f, 90f)
        assertTrue(points.isNotEmpty())
    }

    // ---- scoring helpers ----

    @Test
    fun levelCompleteBonus_scalesWithLevel() {
        val config = controller.configFor(BrickBreakerVariant.CLASSIC, GameDifficulty.EASY)
        val state1 = BrickBreakerState(level = 1)
        val state2 = BrickBreakerState(level = 2)
        assertTrue(controller.levelCompleteBonus(state2, config) > controller.levelCompleteBonus(state1, config))
    }

    @Test
    fun turnsRemainingBonus_scalesWithTurnsLeft() {
        val state = BrickBreakerState(turnsLeft = 5)
        assertEquals(1000, controller.turnsRemainingBonus(state))
    }

    @Test
    fun timeRemainingBonus_scalesWithSeconds() {
        val state = BrickBreakerState(timerSeconds = 20f)
        assertEquals(2000, controller.timeRemainingBonus(state))
    }
}
