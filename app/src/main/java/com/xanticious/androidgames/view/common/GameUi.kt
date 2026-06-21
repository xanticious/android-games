package com.xanticious.androidgames.view.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Standard chrome for an in-game screen.
 *
 * Layout (top to bottom): app bar with an Exit action, an optional [hud] strip,
 * the [board] (which fills all remaining vertical space), and an optional
 * [status] strip below the board.
 *
 * Per `design/common/victory-defeat.md`, victory/defeat content must live in the
 * [status] slot — never overlaid on the [board].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScaffold(
    title: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    hud: @Composable () -> Unit = {},
    status: @Composable () -> Unit = {},
    board: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Clear, contentDescription = "Exit game")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            hud()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                content = board
            )
            status()
        }
    }
}

/**
 * Victory panel shown in the [GameScaffold] status slot once play has stopped.
 * Never overlays the board.
 */
@Composable
fun VictoryPanel(
    score: Int,
    bestScore: Int,
    stars: Int,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    headline: String = "You Win!",
    primaryLabel: String = "Replay"
) {
    ResultPanel(
        heading = headline,
        score = score,
        bestScore = bestScore,
        stars = stars,
        primaryLabel = primaryLabel,
        onPrimary = onReplay,
        onMenu = onMenu
    )
}

/**
 * Defeat panel shown in the [GameScaffold] status slot once play has stopped.
 * Never overlays the board.
 */
@Composable
fun DefeatPanel(
    score: Int,
    bestScore: Int,
    onTryAgain: () -> Unit,
    onMenu: () -> Unit,
    headline: String = "Game Over"
) {
    ResultPanel(
        heading = headline,
        score = score,
        bestScore = bestScore,
        stars = 0,
        primaryLabel = "Try Again",
        onPrimary = onTryAgain,
        onMenu = onMenu
    )
}

@Composable
private fun ResultPanel(
    heading: String,
    score: Int,
    bestScore: Int,
    stars: Int,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onMenu: () -> Unit
) {
    val isNewBest = score >= bestScore && score > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (stars > 0) {
            Text(text = "★".repeat(stars) + "☆".repeat((3 - stars).coerceAtLeast(0)))
        }
        Text(text = "Score: $score")
        Text(
            text = if (isNewBest) "New Best!  $bestScore" else "Best: $bestScore",
            color = if (isNewBest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isNewBest) FontWeight.Bold else FontWeight.Normal
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPrimary) { Text(primaryLabel) }
            OutlinedButton(onClick = onMenu) { Text("Menu") }
        }
    }
}

/** A single-line HUD strip; pass score, lives/health, and timer text. */
@Composable
fun GameHud(
    left: String,
    center: String,
    right: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = left, fontWeight = FontWeight.Bold)
        Text(text = center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(text = right, fontWeight = FontWeight.Bold)
    }
}
