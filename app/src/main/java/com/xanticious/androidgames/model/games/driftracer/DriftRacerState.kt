package com.xanticious.androidgames.model.games.driftracer

import com.xanticious.androidgames.model.Vec2

/**
 * A handcrafted course: a closed centerline polyline with a uniform half-width.
 * All coordinates are in world units (roughly meters). The start/finish line is
 * the perpendicular crossing at [centerline][0].
 */
data class TrackDefinition(
    val name: String,
    val centerline: List<Vec2>,
    val halfWidth: Float,
    val startPosition: Vec2,
    val startHeading: Float          // radians; 0 = east (+x), increases clockwise
)

/**
 * Full physics and timing state for a single time-trial run.
 *
 * - Position/velocity use world-space coordinates.
 * - [heading] is the car's forward angle in radians (0 = right/+x, π/2 = down/+y).
 * - [lastFinishLineSide] is the sign of the car's signed distance from the
 *   start/finish crossing plane; a transition from negative → positive while
 *   the car is moving forward completes a lap.
 * - [bestLap] is [Float.MAX_VALUE] until the first lap is completed.
 */
data class DriftRacerState(
    val position: Vec2,
    val velocity: Vec2,
    val heading: Float,
    val speed: Float,
    val isDrifting: Boolean,
    val isOnTrack: Boolean,
    val lap: Int,                        // 1-based; current lap being driven
    val lapStartTime: Float,             // totalTime when this lap began
    val lapTimes: List<Float>,           // finished lap durations in seconds
    val bestLap: Float,                  // best finished lap this session
    val totalTime: Float,                // cumulative race time in seconds
    val lastFinishLineSide: Float,       // +1f or -1f; used for crossing detection
    val courseIndex: Int
) {
    /** Elapsed time for the lap currently in progress. */
    val currentLapTime: Float get() = totalTime - lapStartTime

    companion object {
        val TRACKS: List<TrackDefinition> = listOf(
            // Oval Circuit — a rounded rectangle loop; clockwise, y-down.
            TrackDefinition(
                name = "Oval Circuit",
                centerline = listOf(
                    Vec2(50f, 15f), Vec2(70f, 18f), Vec2(82f, 30f), Vec2(85f, 50f),
                    Vec2(82f, 70f), Vec2(70f, 82f), Vec2(50f, 85f), Vec2(30f, 82f),
                    Vec2(18f, 70f), Vec2(15f, 50f), Vec2(18f, 30f), Vec2(30f, 18f)
                ),
                halfWidth = 8f,
                startPosition = Vec2(50f, 15f),
                startHeading = 0f
            ),
            // City Circuit — a tighter technical layout with varied corners.
            TrackDefinition(
                name = "City Circuit",
                centerline = listOf(
                    Vec2(50f, 20f), Vec2(75f, 20f), Vec2(85f, 30f), Vec2(85f, 45f),
                    Vec2(75f, 55f), Vec2(60f, 55f), Vec2(60f, 75f), Vec2(50f, 85f),
                    Vec2(30f, 85f), Vec2(20f, 75f), Vec2(20f, 45f), Vec2(30f, 35f),
                    Vec2(40f, 35f), Vec2(40f, 20f)
                ),
                halfWidth = 7f,
                startPosition = Vec2(50f, 20f),
                startHeading = 0f
            )
        )

        fun initial(courseIndex: Int = 0): DriftRacerState {
            val track = TRACKS.getOrElse(courseIndex) { TRACKS[0] }
            return DriftRacerState(
                position = track.startPosition,
                velocity = Vec2.ZERO,
                heading = track.startHeading,
                speed = 0f,
                isDrifting = false,
                isOnTrack = true,
                lap = 1,
                lapStartTime = 0f,
                lapTimes = emptyList(),
                bestLap = Float.MAX_VALUE,
                totalTime = 0f,
                // +1f = car has "just crossed" the line; first real detection needs a
                // full lap around so the side flips to -1 first.
                lastFinishLineSide = 1f,
                courseIndex = courseIndex
            )
        }
    }
}

/** Tuning constants derived from the selected difficulty (difficulty is ignored per design). */
data class DriftRacerConfig(
    val maxSpeed: Float = 35f,
    val acceleration: Float = 18f,
    val brakeForce: Float = 30f,
    val dragCoefficient: Float = 0.3f,
    val turnRateBase: Float = 2.2f,          // rad/s at full speed
    val gripFactor: Float = 8f,              // velocity→heading blend rate (grip)
    val driftGripFactor: Float = 2f,         // lower rate while drifting (rear slides)
    val driftSpeedThreshold: Float = 15f,    // minimum speed to enter drift
    val driftSteeringThreshold: Float = 0.6f,// minimum |steering| to enter drift
    val offTrackSpeedMultiplier: Float = 0.5f,
    val totalLaps: Int = 3
)

/** Per-frame input fed from the view into the controller. */
data class DriftRacerInput(
    val steering: Float,    // [-1, 1]; negative = left turn
    val throttle: Boolean,
    val brake: Boolean
)

/** Outcome events produced by a single physics step. */
enum class DriftRacerEvent { NONE, LAP_COMPLETED, RACE_FINISHED }

/** Result bundle returned by [com.xanticious.androidgames.controller.games.driftracer.DriftRacerController.step]. */
data class DriftRacerStep(
    val state: DriftRacerState,
    val event: DriftRacerEvent
)
