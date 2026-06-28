package com.xanticious.androidgames.view.games.brickbreaker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * HUD strip shared by the Brick Breaker (CLASSIC) and Brick Breaker Arcade
 * (ARCADE) variants.  Shows the score, level, the player's ball bank and
 * strength multiplier, plus an optional trailing chip (e.g. lives).
 *
 * The Balls and Strength chips briefly highlight when their value increases, so
 * collecting a power-up is felt in the HUD rather than via a falling animation.
 */
@Composable
fun BrickBreakerStatHud(
    score: Int,
    level: Int,
    ballCount: Int,
    strength: Int,
    modifier: Modifier = Modifier,
    showBalls: Boolean = true,
    trailingLabel: String? = null,
    trailingValue: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatChip(label = "Score", value = "$score")
        StatChip(label = "Level", value = "$level")
        if (showBalls) {
            StatChip(label = "Balls", value = "$ballCount", flashOnIncrease = true)
        }
        StatChip(label = "Strength", value = "×$strength", flashOnIncrease = true)
        if (trailingLabel != null && trailingValue != null) {
            StatChip(label = trailingLabel, value = trailingValue)
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    flashOnIncrease: Boolean = false,
) {
    val flash = remember { Animatable(0f) }
    var previous by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (flashOnIncrease && value != previous) {
            flash.snapTo(1f)
            flash.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 700))
        }
        previous = value
    }
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f * flash.value)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(highlight)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}
