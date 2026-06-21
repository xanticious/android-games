package com.xanticious.androidgames.view.games.brickbreaker

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.brickbreaker.ActivePowerUp
import com.xanticious.androidgames.model.games.brickbreaker.Ball
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerState
import com.xanticious.androidgames.model.games.brickbreaker.BrickField
import com.xanticious.androidgames.model.games.brickbreaker.BrickType
import com.xanticious.androidgames.model.games.brickbreaker.DroppingPowerUp
import com.xanticious.androidgames.model.games.brickbreaker.PowerUpType
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess

/** Color assigned to each power-up type for icons and drops. */
internal fun powerUpColor(type: PowerUpType): Color = when (type) {
    PowerUpType.EXPLODE -> GameHazard
    PowerUpType.MULTI_SHOT -> GamePlayer
    PowerUpType.POWER_SHOT -> GameEnemy
    PowerUpType.CLEAR_SCREEN -> GameAccent
    PowerUpType.WIDE_SHOT -> GameSuccess
    PowerUpType.RAPID_FIRE -> GameCourtLine
    PowerUpType.TIME_BONUS -> GameAccent
    PowerUpType.EXTRA_BALL -> GamePlayer
    PowerUpType.EXTRA_STRENGTH -> GameEnemy
}

internal fun brickColor(type: BrickType): Color = when (type) {
    BrickType.STANDARD -> GameNeutral
    BrickType.POWERUP -> GameSuccess
    BrickType.STEEL -> Color(0xFF636E72)
    BrickType.TARGET -> GameAccent
}

/** Draw the background court. */
internal fun DrawScope.drawCourt() {
    drawRect(color = GameCourt, size = size)
}

/**
 * Draw the dashed bottom boundary (death line) for the descent variants.
 *
 * The line is unobtrusive by default; when [danger] is true (a brick is one
 * step from crossing) it switches to the hazard colour and pulses via [pulse]
 * (a 0..1 animated value).
 */
internal fun DrawScope.drawBoundaryLine(danger: Boolean, pulse: Float) {
    val y = (BrickField.TOP_MARGIN + BrickField.ROWS * BrickField.ROW_HEIGHT) * size.height
    val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
    val color = if (danger) {
        GameHazard.copy(alpha = 0.35f + 0.55f * pulse.coerceIn(0f, 1f))
    } else {
        GameCourtLine.copy(alpha = 0.35f)
    }
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = if (danger) 4f else 2f,
        pathEffect = dash,
    )
}

/**
 * Draw the full brick grid.
 *
 * [descentOffset] is a fractional row offset (ARCADE real-time descent).
 */
internal fun DrawScope.drawBricks(
    state: BrickBreakerState,
    textMeasurer: TextMeasurer,
    descentOffset: Float = 0f,
) {
    val w = size.width
    val h = size.height
    val cols = BrickField.COLS.toFloat()
    val rowH = BrickField.ROW_HEIGHT
    val pad = BrickField.BRICK_PAD
    val topM = BrickField.TOP_MARGIN

    for (brick in state.bricks) {
        val left = (brick.col / cols) * w
        val right = ((brick.col + 1) / cols - pad) * w
        val top = (topM + (brick.row + descentOffset) * rowH) * h
        val bottom = (topM + (brick.row + descentOffset) * rowH + rowH - pad) * h
        val bw = right - left
        val bh = bottom - top
        if (bh <= 0 || bw <= 0) continue

        val baseColor = brickColor(brick.type)
        // HP fade: darker as HP is depleted.
        val hpFrac = (brick.hp.toFloat() / brick.maxHp.toFloat()).coerceIn(0.3f, 1f)
        val fillColor = baseColor.copy(alpha = hpFrac)

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(left, top),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(4f, 4f),
        )
        // Target bricks get a glowing border.
        if (brick.type == BrickType.TARGET) {
            drawRoundRect(
                color = GameAccent.copy(alpha = 0.8f),
                topLeft = Offset(left, top),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 3f),
            )
        }
        // HP label.
        if (brick.hp > 0 && bh > 10f) {
            val label = brick.hp.toString()
            val measured = textMeasurer.measure(
                label,
                style = TextStyle(fontSize = (bh * 0.45f).coerceAtMost(14f).sp, fontWeight = FontWeight.Bold)
            )
            val tx = left + (bw - measured.size.width) / 2f
            val ty = top + (bh - measured.size.height) / 2f
            drawText(measured, color = Color.White, topLeft = Offset(tx, ty))
        }
    }
}

/** Draw all in-flight balls. */
internal fun DrawScope.drawBalls(state: BrickBreakerState) {
    val w = size.width
    val h = size.height
    val r = if (state.hasWideShot) BrickField.BALL_RADIUS * 2f else BrickField.BALL_RADIUS
    for (ball in state.balls) {
        drawCircle(
            color = GameAccent,
            radius = r * minOf(w, h),
            center = Offset(ball.pos.x * w, ball.pos.y * h),
        )
    }
}

/** Draw the bottom paddle / cannon (CLASSIC / ARCADE). */
internal fun DrawScope.drawBottomPaddle(paddleX: Float) {
    val w = size.width
    val h = size.height
    val cx = paddleX * w
    val cy = BrickField.CANNON_Y * h
    val halfW = w * 0.06f
    val pHeight = h * 0.014f
    drawRoundRect(
        color = GamePlayer,
        topLeft = Offset(cx - halfW, cy - pHeight),
        size = Size(halfW * 2f, pHeight * 2f),
        cornerRadius = CornerRadius(pHeight, pHeight),
    )
}

/** Draw the left-side cannon (CANNON / CANNON_ARCADE). */
internal fun DrawScope.drawLeftCannon(cannonAngleDeg: Float) {
    val w = size.width
    val h = size.height
    val cx = BrickField.CANNON_X * w
    val cy = 0.5f * h
    val barrelLen = w * 0.07f
    val rad = Math.toRadians(cannonAngleDeg.toDouble()).toFloat()
    val endX = cx + kotlin.math.cos(rad) * barrelLen
    val endY = cy - kotlin.math.sin(rad) * barrelLen
    // Base circle.
    drawCircle(color = GamePlayer, radius = w * 0.025f, center = Offset(cx, cy))
    // Barrel.
    drawLine(color = GamePlayer, start = Offset(cx, cy), end = Offset(endX, endY), strokeWidth = w * 0.012f)
}

/** Draw a dotted trajectory preview. */
internal fun DrawScope.drawTrajectory(points: List<Vec2>) {
    val w = size.width
    val h = size.height
    if (points.size < 2) return
    val step = 2  // draw every 2nd point as a dot
    for (i in points.indices step step) {
        val p = points[i]
        drawCircle(
            color = GameCourtLine.copy(alpha = 0.5f),
            radius = 3f,
            center = Offset(p.x * w, p.y * h),
        )
    }
}

/** Draw floating power-up drops. */
internal fun DrawScope.drawDroppingPowerUps(state: BrickBreakerState) {
    val w = size.width
    val h = size.height
    val r = BrickField.POWERUP_RADIUS * minOf(w, h)
    for (drop in state.droppingPowerUps) {
        drawCircle(
            color = powerUpColor(drop.type),
            radius = r,
            center = Offset(drop.pos.x * w, drop.pos.y * h),
        )
        // Pulsing outline.
        val pulseAlpha = 0.5f + 0.5f * kotlin.math.sin(drop.age * 6f)
        drawCircle(
            color = Color.White.copy(alpha = pulseAlpha),
            radius = r + 2f,
            center = Offset(drop.pos.x * w, drop.pos.y * h),
            style = Stroke(width = 2f),
        )
    }
}

/** Build a HUD label for active power-ups. */
internal fun activePowerUpLabel(powerUps: List<ActivePowerUp>): String =
    powerUps.joinToString("  ") { pu ->
        val name = when (pu.type) {
            PowerUpType.EXPLODE -> "EXP"
            PowerUpType.MULTI_SHOT -> "MULTI"
            PowerUpType.POWER_SHOT -> "PWR"
            PowerUpType.CLEAR_SCREEN -> "CLR"
            PowerUpType.WIDE_SHOT -> "WIDE"
            PowerUpType.RAPID_FIRE -> "FAST"
            PowerUpType.TIME_BONUS -> "+T"
            PowerUpType.EXTRA_BALL -> "+BALL"
            PowerUpType.EXTRA_STRENGTH -> "+STR"
        }
        "$name ${pu.remainingSeconds.toInt()}s"
    }
