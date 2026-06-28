package com.xanticious.androidgames.view.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.model.GameDifficulty

/**
 * Shared settings/how-to-play building blocks for the word games.
 *
 * Word games are self-configured (`selfConfigured = true`): each owns its own
 * Settings screen and How to Play screen before gameplay, launched directly from
 * the lobby. These helpers keep that flow consistent across every word game so
 * the look and muscle-memory transfer (see `design/common`). They are pure
 * presentation — no game logic.
 */

/** A row of difficulty chips bound to the shared [GameDifficulty] enum. */
@Composable
fun DifficultyChips(
    selected: GameDifficulty,
    onSelect: (GameDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameDifficulty.entries.forEach { level ->
            FilterChip(
                selected = selected == level,
                onClick = { onSelect(level) },
                label = { Text(level.label) }
            )
        }
    }
}

/**
 * Standard word-game settings screen: a scrollable column with a heading, a
 * difficulty selector, a slot for game-specific [options], and the How to Play /
 * Start actions.
 */
@Composable
fun WordGameSetup(
    title: String,
    difficulty: GameDifficulty,
    onDifficulty: (GameDifficulty) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    startLabel: String = "Start Game",
    options: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Difficulty", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        DifficultyChips(selected = difficulty, onSelect = onDifficulty)
        options()
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text(startLabel) }
        }
    }
}

/**
 * Standard word-game How to Play screen: a scrollable column with a heading, an
 * intro paragraph, the body [content] (typically several [HowToPlaySection]s),
 * and a Back button.
 */
@Composable
fun WordGameHowToPlay(
    title: String,
    intro: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(intro)
        content()
        Button(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Text("Back to Settings")
        }
    }
}

/** A titled section inside a How to Play screen. */
@Composable
fun HowToPlaySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}
