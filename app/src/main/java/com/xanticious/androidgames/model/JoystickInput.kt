package com.xanticious.androidgames.model

/**
 * Normalized analog input produced by the shared virtual joystick component
 * (`design/common/virtual-joystick.md`). Each axis is in the range `[-1, 1]`.
 *
 * - dx: -1.0 (left) .. +1.0 (right)
 * - dy: -1.0 (up)   .. +1.0 (down)
 */
data class JoystickInput(val dx: Float, val dy: Float) {
    val isActive: Boolean get() = dx != 0f || dy != 0f

    companion object {
        val NONE = JoystickInput(0f, 0f)
    }
}
