package com.xanticious.androidgames.view.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.model.JoystickInput
import kotlin.math.hypot

/**
 * Left-thumb analog joystick (`design/common/virtual-joystick.md`).
 *
 * Emits a normalized [JoystickInput] in `[-1, 1]` per axis while dragging and
 * `JoystickInput.NONE` on release. A small dead zone suppresses jitter.
 */
@Composable
fun VirtualJoystick(
    onInput: (JoystickInput) -> Unit,
    modifier: Modifier = Modifier,
    ringDiameter: Dp = 120.dp,
    knobDiameter: Dp = 50.dp,
    accent: Color = Color(0xFF20C997)
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { ringDiameter.toPx() } / 2f
    val knobRadiusPx = with(density) { knobDiameter.toPx() } / 2f
    val deadZonePx = with(density) { 8.dp.toPx() }

    var knob by remember { mutableStateOf(Offset.Zero) }
    val latestInput = rememberUpdatedState(onInput)

    Canvas(
        modifier = modifier
            .size(ringDiameter)
            .pointerInput(radiusPx, deadZonePx) {
                val center = Offset(size.width / 2f, size.height / 2f)
                detectDragGestures(
                    onDragStart = { pos -> knob = clampToRing(pos - center, radiusPx) },
                    onDrag = { change, _ ->
                        change.consume()
                        knob = clampToRing(change.position - center, radiusPx)
                        latestInput.value(toInput(knob, radiusPx, deadZonePx))
                    },
                    onDragEnd = {
                        knob = Offset.Zero
                        latestInput.value(JoystickInput.NONE)
                    },
                    onDragCancel = {
                        knob = Offset.Zero
                        latestInput.value(JoystickInput.NONE)
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = accent.copy(alpha = 0.18f), radius = radiusPx, center = center)
        drawCircle(color = accent.copy(alpha = 0.4f), radius = radiusPx, center = center, style = Stroke(width = 4f))
        drawCircle(color = accent, radius = knobRadiusPx, center = center + knob)
    }
}

/**
 * Floating analog joystick: it has no fixed home. The ring appears wherever the
 * player first touches inside [modifier]'s bounds and the knob tracks the finger
 * from there, so any finger anywhere can drive it. Emits [JoystickInput.NONE] on
 * release.
 */
@Composable
fun FloatingJoystick(
    onInput: (JoystickInput) -> Unit,
    modifier: Modifier = Modifier,
    ringDiameter: Dp = 240.dp,
    knobDiameter: Dp = 100.dp,
    accent: Color = Color(0xFF20C997)
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { ringDiameter.toPx() } / 2f
    val knobRadiusPx = with(density) { knobDiameter.toPx() } / 2f
    val deadZonePx = with(density) { 8.dp.toPx() }

    var origin by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    val latestInput = rememberUpdatedState(onInput)

    Canvas(
        modifier = modifier.pointerInput(radiusPx, deadZonePx) {
            detectDragGestures(
                onDragStart = { pos ->
                    origin = pos
                    knob = Offset.Zero
                },
                onDrag = { change, _ ->
                    change.consume()
                    val o = origin ?: change.position.also { origin = it }
                    knob = clampToRing(change.position - o, radiusPx)
                    latestInput.value(toInput(knob, radiusPx, deadZonePx))
                },
                onDragEnd = {
                    origin = null
                    knob = Offset.Zero
                    latestInput.value(JoystickInput.NONE)
                },
                onDragCancel = {
                    origin = null
                    knob = Offset.Zero
                    latestInput.value(JoystickInput.NONE)
                }
            )
        }
    ) {
        origin?.let { o ->
            drawCircle(color = accent.copy(alpha = 0.18f), radius = radiusPx, center = o)
            drawCircle(color = accent.copy(alpha = 0.4f), radius = radiusPx, center = o, style = Stroke(width = 4f))
            drawCircle(color = accent, radius = knobRadiusPx, center = o + knob)
        }
    }
}

private fun clampToRing(raw: Offset, radiusPx: Float): Offset {
    val dist = hypot(raw.x, raw.y)
    if (dist <= radiusPx || dist == 0f) return raw
    val scale = radiusPx / dist
    return Offset(raw.x * scale, raw.y * scale)
}

private fun toInput(knob: Offset, radiusPx: Float, deadZonePx: Float): JoystickInput {
    val dist = hypot(knob.x, knob.y)
    if (dist < deadZonePx || radiusPx == 0f) return JoystickInput.NONE
    return JoystickInput(
        dx = (knob.x / radiusPx).coerceIn(-1f, 1f),
        dy = (knob.y / radiusPx).coerceIn(-1f, 1f)
    )
}
