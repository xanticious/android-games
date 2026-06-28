package com.xanticious.androidgames.controller.games.driftracer

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.driftracer.DriftRacerConfig
import com.xanticious.androidgames.model.games.driftracer.DriftRacerEvent
import com.xanticious.androidgames.model.games.driftracer.DriftRacerInput
import com.xanticious.androidgames.model.games.driftracer.DriftRacerState
import com.xanticious.androidgames.model.games.driftracer.DriftRacerStep
import com.xanticious.androidgames.model.games.driftracer.TrackDefinition
import kotlin.math.abs

/**
 * Pure Drift Racer physics and lap-timing logic.
 * No Android or Compose imports — fully JVM unit-testable.
 *
 * Car model (arcade, top-down):
 *   - Velocity blends toward `forward * speed` every frame; the blend rate controls grip.
 *   - Drift is triggered by hard steering at speed; while drifting the blend rate is
 *     much lower, so the rear continues sliding and must be counter-steered to exit.
 *   - Off-track (grass) hard-caps speed to [DriftRacerConfig.offTrackSpeedMultiplier] × max.
 *   - Lap counting: a signed-distance crossing plane at the start/finish line fires
 *     [DriftRacerEvent.LAP_COMPLETED] or [DriftRacerEvent.RACE_FINISHED].
 */
class DriftRacerController {

    /** Difficulty is intentionally ignored — the design has no difficulty setting. */
    fun configFor(@Suppress("UNUSED_PARAMETER") difficulty: GameDifficulty): DriftRacerConfig =
        DriftRacerConfig()

    /**
     * Advances the simulation by [dt] seconds and returns the updated state plus
     * any lap-related event that occurred in this step.
     */
    fun step(
        state: DriftRacerState,
        config: DriftRacerConfig,
        dt: Float,
        input: DriftRacerInput
    ): DriftRacerStep {
        val track = DriftRacerState.TRACKS.getOrElse(state.courseIndex) { DriftRacerState.TRACKS[0] }

        // ── 1. Speed: throttle / brake / drag ──────────────────────────────────
        var speed = state.speed
        if (input.throttle) speed += config.acceleration * dt
        if (input.brake)    speed -= config.brakeForce    * dt
        speed = speed.coerceIn(0f, config.maxSpeed)
        speed *= (1f - config.dragCoefficient * dt)
        speed = speed.coerceAtLeast(0f)

        // ── 2. Steering: rotate heading; turn rate is proportional to speed ────
        val speedFactor = (speed / config.maxSpeed).coerceIn(0.1f, 1f)
        val newHeading = state.heading + input.steering * config.turnRateBase * speedFactor * dt

        // ── 3. Drift state ─────────────────────────────────────────────────────
        // Enter drift: fast + hard steering. Exit drift: slow down or brake.
        val isDrifting = when {
            state.isDrifting -> speed > config.driftSpeedThreshold * 0.7f && !input.brake
            else             -> speed > config.driftSpeedThreshold &&
                                abs(input.steering) > config.driftSteeringThreshold
        }

        // ── 4. Velocity blend toward forward direction ─────────────────────────
        val forward       = Vec2.fromAngle(newHeading)
        val targetVelocity = forward * speed
        val gripRate       = if (isDrifting) config.driftGripFactor else config.gripFactor
        val blend          = minOf(1f, gripRate * dt)
        val blendedVelocity = Vec2(
            state.velocity.x + (targetVelocity.x - state.velocity.x) * blend,
            state.velocity.y + (targetVelocity.y - state.velocity.y) * blend
        )

        // ── 5. Integrate position ──────────────────────────────────────────────
        val newPosition = state.position + blendedVelocity * dt

        // ── 6. Off-track check and speed cap ───────────────────────────────────
        val isOnTrack   = isOnTrack(newPosition, track)
        val cappedSpeed = if (isOnTrack) speed
                          else minOf(speed, config.maxSpeed * config.offTrackSpeedMultiplier)
        // Scale velocity proportionally so stored speed and velocity stay consistent.
        val finalVelocity = if (!isOnTrack && speed > 0.001f && cappedSpeed < speed)
            blendedVelocity * (cappedSpeed / speed)
        else
            blendedVelocity

        // ── 7. Time ────────────────────────────────────────────────────────────
        val newTotalTime = state.totalTime + dt

        // ── 8. Lap crossing detection ──────────────────────────────────────────
        // The finish line is the plane through centerline[0] with normal pointing
        // toward centerline[1]. A negative→positive sign change while the car moves
        // in the forward direction registers a completed lap.
        val finishNormal = finishLineNormal(track)
        val newSignedDist = (newPosition - track.centerline[0]).dot(finishNormal)
        val newSide       = if (newSignedDist >= 0f) 1f else -1f
        val crossedForward = state.lastFinishLineSide < 0f &&
                             newSide >= 0f &&
                             finalVelocity.dot(finishNormal) > 0f &&
                             state.lap > 0

        val event: DriftRacerEvent
        val newLap: Int
        val newLapTimes: List<Float>
        val newBestLap: Float
        val newLapStartTime: Float

        if (crossedForward) {
            val lapTime   = newTotalTime - state.lapStartTime
            newLapTimes   = state.lapTimes + lapTime
            newBestLap    = minOf(state.bestLap, lapTime)
            if (state.lap >= config.totalLaps) {
                event           = DriftRacerEvent.RACE_FINISHED
                newLap          = state.lap
                newLapStartTime = state.lapStartTime
            } else {
                event           = DriftRacerEvent.LAP_COMPLETED
                newLap          = state.lap + 1
                newLapStartTime = newTotalTime
            }
        } else {
            event           = DriftRacerEvent.NONE
            newLap          = state.lap
            newLapTimes     = state.lapTimes
            newBestLap      = state.bestLap
            newLapStartTime = state.lapStartTime
        }

        return DriftRacerStep(
            state = state.copy(
                position          = newPosition,
                velocity          = finalVelocity,
                heading           = newHeading,
                speed             = cappedSpeed,
                isDrifting        = isDrifting,
                isOnTrack         = isOnTrack,
                lap               = newLap,
                lapStartTime      = newLapStartTime,
                lapTimes          = newLapTimes,
                bestLap           = newBestLap,
                totalTime         = newTotalTime,
                lastFinishLineSide = newSide
            ),
            event = event
        )
    }

    /** True if [position] is within [TrackDefinition.halfWidth] of any centerline segment. */
    fun isOnTrack(position: Vec2, track: TrackDefinition): Boolean {
        val n = track.centerline.size
        for (i in 0 until n) {
            val a = track.centerline[i]
            val b = track.centerline[(i + 1) % n]
            if (distanceToSegment(position, a, b) < track.halfWidth) return true
        }
        return false
    }

    /** Unit vector pointing in the track's forward direction at the start/finish line. */
    fun finishLineNormal(track: TrackDefinition): Vec2 =
        (track.centerline[1] - track.centerline[0]).normalized()

    private fun distanceToSegment(point: Vec2, a: Vec2, b: Vec2): Float {
        val ab     = b - a
        val lenSq  = ab.lengthSquared
        if (lenSq < 1e-10f) return point.distanceTo(a)
        val t      = ((point - a).dot(ab) / lenSq).coerceIn(0f, 1f)
        val closest = a + ab * t
        return point.distanceTo(closest)
    }
}
