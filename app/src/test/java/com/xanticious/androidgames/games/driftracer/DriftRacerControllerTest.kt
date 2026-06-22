package com.xanticious.androidgames.games.driftracer

import com.xanticious.androidgames.controller.games.driftracer.DriftRacerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.driftracer.DriftRacerEvent
import com.xanticious.androidgames.model.games.driftracer.DriftRacerInput
import com.xanticious.androidgames.model.games.driftracer.DriftRacerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriftRacerControllerTest {

    private val controller = DriftRacerController()
    private val config     = controller.configFor(GameDifficulty.MEDIUM)

    private fun noInput()      = DriftRacerInput(steering = 0f,  throttle = false, brake = false)
    private fun throttleInput()= DriftRacerInput(steering = 0f,  throttle = true,  brake = false)
    private fun brakeInput()   = DriftRacerInput(steering = 0f,  throttle = false, brake = true)

    // ── Speed ──────────────────────────────────────────────────────────────────

    @Test
    fun throttle_increasesSpeed() {
        val state = DriftRacerState.initial(0)
        val step  = controller.step(state, config, 0.1f, throttleInput())
        assertTrue("speed should increase with throttle", step.state.speed > 0f)
    }

    @Test
    fun brake_decreasesSpeed() {
        val state = DriftRacerState.initial(0).copy(speed = 20f, velocity = Vec2(20f, 0f))
        val step  = controller.step(state, config, 0.1f, brakeInput())
        assertTrue("speed should decrease with brake", step.state.speed < 20f)
    }

    // ── Steering ───────────────────────────────────────────────────────────────

    @Test
    fun steering_rotatesHeading() {
        val state = DriftRacerState.initial(0).copy(
            position = Vec2(85f, 50f), velocity = Vec2(20f, 0f), speed = 20f
        )
        val step = controller.step(state, config, 0.1f, DriftRacerInput(1f, true, false))
        assertTrue("hard steering should change heading", step.state.heading != 0f)
    }

    // ── Off-track ──────────────────────────────────────────────────────────────

    @Test
    fun offTrack_capsSpeed() {
        // Position (0, 0) is far from every Oval Circuit segment → off-track.
        val state = DriftRacerState.initial(0).copy(
            position = Vec2(0f, 0f),
            velocity = Vec2(30f, 0f),
            speed    = 30f,
            lastFinishLineSide = 1f
        )
        val step         = controller.step(state, config, 0.1f, noInput())
        val maxOffTrack  = config.maxSpeed * config.offTrackSpeedMultiplier
        assertTrue("off-track speed ${step.state.speed} should be ≤ $maxOffTrack",
                   step.state.speed <= maxOffTrack)
        assertFalse("isOnTrack should be false", step.state.isOnTrack)
    }

    // ── Drift ──────────────────────────────────────────────────────────────────

    @Test
    fun driftState_triggeredAtHighSpeedWithHardSteering() {
        // centerline[3] = Vec2(85, 50) — on the Oval Circuit's right edge.
        val state = DriftRacerState.initial(0).copy(
            position  = Vec2(85f, 50f),
            velocity  = Vec2(20f, 0f),
            speed     = 20f,
            heading   = 0f,
            isDrifting = false,
            isOnTrack  = true,
            lastFinishLineSide = 1f
        )
        val step = controller.step(state, config, 0.1f, DriftRacerInput(1f, true, false))
        assertTrue("isDrifting should be true at high speed with full steering", step.state.isDrifting)
    }

    @Test
    fun brake_exitsDriftState() {
        val state = DriftRacerState.initial(0).copy(
            position   = Vec2(85f, 50f),
            velocity   = Vec2(20f, 0f),
            speed      = 20f,
            isDrifting = true,
            lastFinishLineSide = 1f
        )
        val step = controller.step(state, config, 0.1f, brakeInput())
        assertFalse("braking should exit drift", step.state.isDrifting)
    }

    // ── Grip ───────────────────────────────────────────────────────────────────

    @Test
    fun grip_velocityAlignsWithHeading() {
        // centerline[6] = Vec2(50, 85) — bottom centre of Oval Circuit, on track.
        // Velocity points south (+y) but heading is east (0); grip should steer
        // the stored velocity toward east, making x-component positive.
        val state = DriftRacerState.initial(0).copy(
            position  = Vec2(50f, 85f),
            velocity  = Vec2(0f, 20f),
            speed     = 20f,
            heading   = 0f,
            isDrifting = false,
            lastFinishLineSide = 1f
        )
        val step = controller.step(state, config, 0.1f, noInput())
        assertTrue("grip should blend velocity toward heading (+x)", step.state.velocity.x > 0f)
    }

    // ── Lap / race completion ──────────────────────────────────────────────────

    @Test
    fun lapCompleted_whenCrossingFinishLineForward() {
        // Car is behind the finish line (lastFinishLineSide = -1) and will cross
        // it in the forward direction after one step at speed 25, dt = 0.2.
        // Finish-line normal ≈ Vec2(0.989, 0.148); starting at x=46, y=15
        // gives a signed distance ≈ –4, which becomes ≈ +0.7 after the step.
        val state = DriftRacerState.initial(0).copy(
            position  = Vec2(46f, 15f),
            velocity  = Vec2(25f, 0f),
            speed     = 25f,
            heading   = 0f,
            lastFinishLineSide = -1f,
            lap       = 1,
            isOnTrack = true
        )
        val step = controller.step(state, config, 0.2f, noInput())
        assertEquals(DriftRacerEvent.LAP_COMPLETED, step.event)
        assertEquals(2, step.state.lap)
        assertEquals(1, step.state.lapTimes.size)
    }

    @Test
    fun raceFinished_whenCrossingOnFinalLap() {
        val state = DriftRacerState.initial(0).copy(
            position  = Vec2(46f, 15f),
            velocity  = Vec2(25f, 0f),
            speed     = 25f,
            heading   = 0f,
            lastFinishLineSide = -1f,
            lap       = config.totalLaps,   // already on the last lap
            isOnTrack = true
        )
        val step = controller.step(state, config, 0.2f, noInput())
        assertEquals(DriftRacerEvent.RACE_FINISHED, step.event)
    }

    @Test
    fun noEvent_whenMovingAwayFromFinishLine() {
        // Car is behind the line and moves further away; no crossing should fire.
        val state = DriftRacerState.initial(0).copy(
            position  = Vec2(44f, 15f),
            velocity  = Vec2(-5f, 0f),
            speed     = 5f,
            heading   = 3.14159f,           // facing left (away from finish)
            lastFinishLineSide = -1f,
            lap       = 1,
            isOnTrack = true
        )
        val step = controller.step(state, config, 0.1f, noInput())
        assertEquals(DriftRacerEvent.NONE, step.event)
    }
}
