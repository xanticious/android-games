package com.xanticious.androidgames.view.games.jigsaw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueGreen
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHuePink
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow

/**
 * Procedural "images" for the Jigsaw puzzle.
 *
 * DEVIATION FROM DESIGN DOC: The design specified 50 bundled Unsplash photos. This
 * repo ships no binary image assets and the sandbox cannot download or bundle
 * external photos, so all Jigsaw pictures are instead generated procedurally via
 * Compose Canvas using ui/theme/Color.kt tokens only (no hex literals). Eight
 * distinct scenic / geometric motifs (ocean, reef, mountains, etc.) are rendered
 * deterministically from the draw function keyed by each image's [ProceduralImage.id].
 * The jigsaw mechanic — piece bank, snap-only-correct placement, board crop
 * rendering — is otherwise fully faithful to the design.
 */
data class ProceduralImage(
    val id: Int,
    val title: String,
    /** Draws the full picture into the current DrawScope from (0,0) to (w,h). */
    val draw: DrawScope.(w: Float, h: Float) -> Unit
)

/** The eight procedural images available for selection. */
val proceduralImages: List<ProceduralImage> = listOf(

    // 0 — Ocean Sunset
    ProceduralImage(0, "Ocean Sunset") { w, h ->
        drawRect(
            brush = Brush.verticalGradient(
                listOf(PuzzleHueViolet, PuzzleHueOrange, PuzzleHueYellow),
                startY = 0f, endY = h * 0.6f
            ),
            size = Size(w, h * 0.6f)
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(PuzzleHueBlue, Aqua3),
                startY = h * 0.6f, endY = h
            ),
            topLeft = Offset(0f, h * 0.6f),
            size = Size(w, h * 0.4f)
        )
        // Sun
        drawCircle(color = GameAccent, radius = h * 0.11f, center = Offset(w * 0.5f, h * 0.32f))
        // Sun glow
        drawCircle(color = PuzzleHueYellow.copy(alpha = 0.3f), radius = h * 0.18f, center = Offset(w * 0.5f, h * 0.32f))
        // Horizon reflection
        drawLine(GameAccent.copy(alpha = 0.6f), Offset(w * 0.35f, h * 0.62f), Offset(w * 0.65f, h * 0.62f), strokeWidth = h * 0.015f)
        // Waves
        for (i in 0..4) {
            val y = h * 0.65f + i * h * 0.055f
            val waveH = h * 0.008f
            drawArc(
                color = Aqua1.copy(alpha = 0.5f),
                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * 0.05f + i * w * 0.06f, y - waveH),
                size = Size(w * 0.2f, waveH * 2f),
                style = Stroke(width = h * 0.008f)
            )
        }
    },

    // 1 — Coral Reef
    ProceduralImage(1, "Coral Reef") { w, h ->
        drawRect(
            brush = Brush.verticalGradient(listOf(Aqua4, Aqua3, Dark1), startY = 0f, endY = h),
            size = Size(w, h)
        )
        // Sandy floor
        drawRect(
            brush = Brush.verticalGradient(
                listOf(PuzzleHueYellow.copy(alpha = 0.6f), PuzzleHueOrange.copy(alpha = 0.4f)),
                startY = h * 0.75f, endY = h
            ),
            topLeft = Offset(0f, h * 0.75f),
            size = Size(w, h * 0.25f)
        )
        // Coral branches (arcs from floor)
        val coralPositions = listOf(0.15f, 0.35f, 0.55f, 0.75f, 0.9f)
        val coralColors = listOf(PuzzleHueRed, PuzzleHuePink, PuzzleHueOrange, PuzzleHuePink, PuzzleHueRed)
        coralPositions.forEachIndexed { i, x ->
            val baseY = h * 0.75f
            val stemH = h * (0.15f + i * 0.02f)
            val color = coralColors[i]
            drawLine(color, Offset(w * x, baseY), Offset(w * x, baseY - stemH), strokeWidth = w * 0.025f)
            drawLine(color, Offset(w * x, baseY - stemH * 0.5f), Offset(w * x - w * 0.04f, baseY - stemH * 0.75f), strokeWidth = w * 0.015f)
            drawLine(color, Offset(w * x, baseY - stemH * 0.5f), Offset(w * x + w * 0.04f, baseY - stemH * 0.75f), strokeWidth = w * 0.015f)
            drawCircle(color, radius = w * 0.018f, center = Offset(w * x, baseY - stemH))
        }
        // Fish
        drawOval(PuzzleHueOrange, topLeft = Offset(w * 0.25f, h * 0.38f), size = Size(w * 0.1f, h * 0.06f))
        drawOval(Aqua2, topLeft = Offset(w * 0.6f, h * 0.28f), size = Size(w * 0.08f, h * 0.05f))
        // Bubbles
        for (i in 0..5) {
            drawCircle(Aqua0.copy(alpha = 0.4f), radius = w * 0.012f, center = Offset(w * (0.2f + i * 0.13f), h * (0.55f - i * 0.05f)))
        }
    },

    // 2 — Mountain Peaks
    ProceduralImage(2, "Mountain Peaks") { w, h ->
        // Sky
        drawRect(
            brush = Brush.verticalGradient(listOf(PuzzleHueBlue, Aqua1), startY = 0f, endY = h * 0.65f),
            size = Size(w, h * 0.65f)
        )
        // Distant mountains (lighter)
        val farPath = Path().apply {
            moveTo(0f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.28f)
            lineTo(w * 0.4f, h * 0.55f)
            lineTo(w * 0.6f, h * 0.22f)
            lineTo(w * 0.8f, h * 0.48f)
            lineTo(w, h * 0.3f)
            lineTo(w, h * 0.65f)
            close()
        }
        drawPath(farPath, color = Dark2.copy(alpha = 0.6f))
        // Snow caps on far mountains
        drawCircle(Aqua0.copy(alpha = 0.8f), radius = w * 0.04f, center = Offset(w * 0.2f, h * 0.28f))
        drawCircle(Aqua0.copy(alpha = 0.8f), radius = w * 0.05f, center = Offset(w * 0.6f, h * 0.22f))
        drawCircle(Aqua0.copy(alpha = 0.7f), radius = w * 0.03f, center = Offset(w, h * 0.3f))
        // Near mountains (darker)
        val nearPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.6f)
            lineTo(w * 0.25f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.72f, h * 0.25f)
            lineTo(w, h * 0.5f)
            lineTo(w, h)
            close()
        }
        drawPath(nearPath, color = Dark2)
        // Snow caps on near mountains
        drawCircle(Aqua0, radius = w * 0.045f, center = Offset(w * 0.25f, h * 0.3f))
        drawCircle(Aqua0, radius = w * 0.055f, center = Offset(w * 0.72f, h * 0.25f))
        // Ground / valley
        drawRect(
            brush = Brush.verticalGradient(listOf(GameSuccess.copy(alpha = 0.7f), GameSuccess), startY = h * 0.75f, endY = h),
            topLeft = Offset(0f, h * 0.75f),
            size = Size(w, h * 0.25f)
        )
    },

    // 3 — Deep Sea
    ProceduralImage(3, "Deep Sea") { w, h ->
        drawRect(
            brush = Brush.verticalGradient(listOf(Dark1, Dark0), startY = 0f, endY = h),
            size = Size(w, h)
        )
        // Glowing jellyfish
        val jellyfishCenters = listOf(Offset(w * 0.2f, h * 0.25f), Offset(w * 0.7f, h * 0.45f), Offset(w * 0.45f, h * 0.7f))
        val jellyfishColors = listOf(PuzzleHuePink, PuzzleHueTeal, PuzzleHueViolet)
        jellyfishCenters.forEachIndexed { i, c ->
            val col = jellyfishColors[i]
            drawCircle(col.copy(alpha = 0.15f), radius = w * 0.1f, center = c)
            drawCircle(col.copy(alpha = 0.4f), radius = w * 0.05f, center = c)
            drawCircle(col, radius = w * 0.02f, center = c)
            // Tentacles
            for (t in 0..3) {
                val tx = c.x + (t - 1.5f) * w * 0.02f
                drawLine(col.copy(alpha = 0.5f), Offset(tx, c.y + w * 0.04f), Offset(tx + (t - 2f) * w * 0.01f, c.y + w * 0.12f), strokeWidth = 2f)
            }
        }
        // Small glowing fish
        for (i in 0..5) {
            val fx = w * (0.1f + i * 0.15f)
            val fy = h * (0.15f + (i % 3) * 0.25f)
            drawOval(Aqua2.copy(alpha = 0.7f), topLeft = Offset(fx, fy), size = Size(w * 0.06f, h * 0.025f))
        }
        // Scattered particles
        for (i in 0..20) {
            val px = w * ((i * 47 % 100) / 100f)
            val py = h * ((i * 31 % 100) / 100f)
            drawCircle(Aqua0.copy(alpha = 0.3f), radius = w * 0.004f, center = Offset(px, py))
        }
        // Seabed silhouette
        val bedPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.88f)
            lineTo(w * 0.15f, h * 0.82f)
            lineTo(w * 0.3f, h * 0.9f)
            lineTo(w * 0.5f, h * 0.8f)
            lineTo(w * 0.7f, h * 0.87f)
            lineTo(w * 0.85f, h * 0.78f)
            lineTo(w, h * 0.85f)
            lineTo(w, h)
            close()
        }
        drawPath(bedPath, Dark0)
    },

    // 4 — Tropical Beach
    ProceduralImage(4, "Tropical Beach") { w, h ->
        // Sky
        drawRect(
            brush = Brush.verticalGradient(
                listOf(PuzzleHueBlue, Aqua2, PuzzleHueTeal),
                startY = 0f, endY = h * 0.55f
            ),
            size = Size(w, h * 0.55f)
        )
        // Fluffy clouds
        listOf(Offset(w * 0.15f, h * 0.15f), Offset(w * 0.65f, h * 0.1f)).forEach { c ->
            drawCircle(Aqua0.copy(alpha = 0.85f), radius = w * 0.09f, center = c)
            drawCircle(Aqua0.copy(alpha = 0.85f), radius = w * 0.07f, center = Offset(c.x + w * 0.08f, c.y + h * 0.01f))
            drawCircle(Aqua0.copy(alpha = 0.85f), radius = w * 0.06f, center = Offset(c.x - w * 0.07f, c.y + h * 0.015f))
        }
        // Sea
        drawRect(
            brush = Brush.verticalGradient(listOf(Aqua3, PuzzleHueBlue), startY = h * 0.55f, endY = h * 0.72f),
            topLeft = Offset(0f, h * 0.55f),
            size = Size(w, h * 0.17f)
        )
        // Sand
        drawRect(
            brush = Brush.verticalGradient(
                listOf(PuzzleHueYellow.copy(alpha = 0.9f), PuzzleHueOrange.copy(alpha = 0.6f)),
                startY = h * 0.72f, endY = h
            ),
            topLeft = Offset(0f, h * 0.72f),
            size = Size(w, h * 0.28f)
        )
        // Palm trunk
        drawLine(PuzzleHueOrange.copy(alpha = 0.7f), Offset(w * 0.75f, h * 0.9f), Offset(w * 0.65f, h * 0.4f), strokeWidth = w * 0.025f)
        // Palm leaves
        listOf(-60f, -30f, 0f, 30f, 60f).forEachIndexed { i, angle ->
            val leafX = w * 0.65f + (angle / 15f) * w * 0.1f
            val leafY = h * 0.38f
            drawLine(GameSuccess, Offset(w * 0.65f, leafY), Offset(leafX, leafY - h * 0.1f), strokeWidth = w * 0.018f)
        }
        // Waves
        for (i in 0..2) {
            drawArc(
                color = Aqua0.copy(alpha = 0.5f),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * (0.05f + i * 0.28f), h * 0.7f),
                size = Size(w * 0.25f, h * 0.03f),
                style = Stroke(width = h * 0.01f)
            )
        }
    },

    // 5 — Starry Night
    ProceduralImage(5, "Starry Night") { w, h ->
        drawRect(
            brush = Brush.verticalGradient(listOf(Dark0, Dark1), startY = 0f, endY = h),
            size = Size(w, h)
        )
        // Milky way band
        drawRect(
            brush = Brush.linearGradient(
                listOf(PuzzleHueViolet.copy(alpha = 0f), PuzzleHueViolet.copy(alpha = 0.15f), PuzzleHueBlue.copy(alpha = 0.12f), PuzzleHueViolet.copy(alpha = 0f)),
                start = Offset(0f, h * 0.3f), end = Offset(w, h * 0.7f)
            ),
            size = Size(w, h)
        )
        // Moon
        drawCircle(Aqua0, radius = w * 0.09f, center = Offset(w * 0.8f, h * 0.15f))
        drawCircle(Dark1, radius = w * 0.075f, center = Offset(w * 0.83f, h * 0.13f))
        // Stars (deterministic positions from modular arithmetic)
        for (i in 0 until 60) {
            val sx = w * ((i * 53 + 7) % 97 / 97f)
            val sy = h * ((i * 37 + 13) % 89 / 89f)
            val sr = w * if (i % 5 == 0) 0.007f else 0.003f
            val alpha = if (i % 3 == 0) 1.0f else 0.6f
            drawCircle(Aqua0.copy(alpha = alpha), radius = sr, center = Offset(sx, sy))
        }
        // Hills silhouette
        val hillPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.8f)
            lineTo(w * 0.18f, h * 0.65f)
            lineTo(w * 0.38f, h * 0.78f)
            lineTo(w * 0.6f, h * 0.6f)
            lineTo(w * 0.8f, h * 0.72f)
            lineTo(w, h * 0.62f)
            lineTo(w, h)
            close()
        }
        drawPath(hillPath, Dark0)
        // Village lights
        for (i in 0..4) {
            drawCircle(PuzzleHueYellow.copy(alpha = 0.6f), radius = w * 0.008f, center = Offset(w * (0.1f + i * 0.18f), h * 0.83f))
        }
    },

    // 6 — Underwater Garden
    ProceduralImage(6, "Underwater Garden") { w, h ->
        drawRect(
            brush = Brush.verticalGradient(listOf(Aqua3, Aqua4, Dark1), startY = 0f, endY = h),
            size = Size(w, h)
        )
        // Sunbeams from above
        for (i in 0..4) {
            val bx = w * (0.1f + i * 0.2f)
            drawLine(
                Aqua0.copy(alpha = 0.1f),
                Offset(bx, 0f),
                Offset(bx + w * 0.05f, h * 0.6f),
                strokeWidth = w * 0.04f
            )
        }
        // Seabed
        drawRect(
            brush = Brush.verticalGradient(
                listOf(PuzzleHueYellow.copy(alpha = 0.5f), PuzzleHueOrange.copy(alpha = 0.3f)),
                startY = h * 0.8f, endY = h
            ),
            topLeft = Offset(0f, h * 0.8f),
            size = Size(w, h * 0.2f)
        )
        // Seaweed stalks
        for (i in 0..6) {
            val sx = w * (0.08f + i * 0.13f)
            val sh = h * (0.25f + (i % 3) * 0.1f)
            val swayX = if (i % 2 == 0) w * 0.03f else -w * 0.03f
            val path = Path().apply {
                moveTo(sx, h * 0.8f)
                cubicTo(sx + swayX, h * 0.8f - sh * 0.4f, sx - swayX, h * 0.8f - sh * 0.7f, sx + swayX * 0.5f, h * 0.8f - sh)
            }
            drawPath(path, color = PuzzleHueGreen, style = Stroke(width = w * 0.018f))
        }
        // Sea anemones
        for (i in 0..2) {
            val ax = w * (0.2f + i * 0.3f)
            val aColor = listOf(PuzzleHuePink, PuzzleHueTeal, PuzzleHueViolet)[i]
            for (t in 0..7) {
                val angle = (t / 8f) * 2f * Math.PI.toFloat()
                drawLine(aColor, Offset(ax, h * 0.82f), Offset(ax + w * 0.03f * kotlin.math.cos(angle), h * 0.82f - h * 0.06f * kotlin.math.sin(angle.coerceAtLeast(0f))), strokeWidth = w * 0.01f)
            }
            drawCircle(aColor, radius = w * 0.015f, center = Offset(ax, h * 0.82f))
        }
        // Small fish
        for (i in 0..3) {
            val fx = w * (0.12f + i * 0.22f)
            val fy = h * (0.3f + (i % 2) * 0.2f)
            val fColor = listOf(PuzzleHueYellow, PuzzleHueOrange, Aqua2, PuzzleHuePink)[i]
            drawOval(fColor, topLeft = Offset(fx, fy), size = Size(w * 0.08f, h * 0.035f))
            // tail
            val tailPath = Path().apply {
                moveTo(fx, fy + h * 0.017f)
                lineTo(fx - w * 0.03f, fy)
                lineTo(fx - w * 0.03f, fy + h * 0.035f)
                close()
            }
            drawPath(tailPath, fColor)
        }
    },

    // 7 — Geometric Mosaic
    ProceduralImage(7, "Geometric Mosaic") { w, h ->
        val colors = listOf(
            PuzzleHueRed, PuzzleHueOrange, PuzzleHueYellow, PuzzleHueGreen,
            PuzzleHueTeal, PuzzleHueBlue, PuzzleHueViolet, PuzzleHuePink
        )
        // Background
        drawRect(color = Dark1, size = Size(w, h))
        // Grid of rotated squares / diamonds
        val cols = 6
        val rows = 6
        val cw = w / cols
        val ch = h / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = cw * (c + 0.5f)
                val cy = ch * (r + 0.5f)
                val color = colors[(r * cols + c + r) % colors.size]
                val halfW = cw * 0.42f
                val halfH = ch * 0.42f
                // Diamond shape
                val diamondPath = Path().apply {
                    moveTo(cx, cy - halfH)
                    lineTo(cx + halfW, cy)
                    lineTo(cx, cy + halfH)
                    lineTo(cx - halfW, cy)
                    close()
                }
                drawPath(diamondPath, color = color.copy(alpha = 0.7f), style = Fill)
                drawPath(diamondPath, color = color, style = Stroke(width = 2f))
                // Inner dot
                drawCircle(color = Aqua0.copy(alpha = 0.5f), radius = cw * 0.08f, center = Offset(cx, cy))
            }
        }
    }
)

/** Returns the [ProceduralImage] for [imageId], wrapping around if out of range. */
fun imageForId(imageId: Int): ProceduralImage =
    proceduralImages[imageId.coerceIn(0, proceduralImages.lastIndex)]
