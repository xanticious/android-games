package com.xanticious.androidgames.view.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos

/**
 * Drives a fixed-callback game loop using Compose frame callbacks.
 *
 * While [running] is true, [onFrame] is invoked once per display frame with the
 * elapsed time in seconds since the previous frame (clamped to avoid large
 * jumps after the app is backgrounded). The loop is cancelled automatically when
 * [running] becomes false or the composable leaves the composition.
 */
@Composable
fun GameLoop(running: Boolean, onFrame: (dtSeconds: Float) -> Unit) {
    val latest = rememberUpdatedState(onFrame)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, MAX_FRAME_SECONDS)
            last = now
            latest.value(dt)
        }
    }
}

/** Caps the per-frame delta so physics never integrates a huge step after a stall. */
private const val MAX_FRAME_SECONDS = 0.05f
