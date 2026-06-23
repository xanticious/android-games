package com.xanticious.androidgames.view.common.puzzle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.model.GameDifficulty

/**
 * Shared chrome for self-configured puzzle games. These composables implement the
 * common screens described in `design/common/puzzle-flow.md` and
 * `design/common/puzzle-controls.md`, so each game only supplies its
 * game-specific option widgets and rules text.
 */

/**
 * The opening Settings screen for a puzzle. Renders the game's [options] above the
 * standard "How to Play" / "Start" action row.
 */
@Composable
fun PuzzleSettingsScreen(
    title: String,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    options: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        options()
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text("Start Game") }
        }
    }
}

/** The How to Play screen: a title, intro paragraph(s) and game-specific [body]. */
@Composable
fun PuzzleHowToPlayScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        body()
        Button(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Text("Back to Settings")
        }
    }
}

/** A titled section within a How to Play screen. */
@Composable
fun HowToPlaySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
    content()
}

/** A labelled difficulty chip row used by most puzzle settings screens. */
@Composable
fun DifficultyChips(selected: GameDifficulty, onSelect: (GameDifficulty) -> Unit) {
    Text("Difficulty", fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameDifficulty.entries.forEach { level ->
            FilterChip(
                selected = selected == level,
                onClick = { onSelect(level) },
                label = { Text(level.label) }
            )
        }
    }
}

/** A labelled single-select chip row for any small set of [options]. */
@Composable
fun <T> OptionChips(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String
) {
    Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(labelOf(option)) }
            )
        }
    }
}

/**
 * The bottom action + status row used during play (`design/common/puzzle-flow.md`).
 * Each control is optional; pass a non-null lambda to show that button. The
 * trailing [status] text is right-aligned (move count, hint count, etc.).
 */
@Composable
fun PuzzleActionBar(
    modifier: Modifier = Modifier,
    status: String = "",
    onUndo: (() -> Unit)? = null,
    undoEnabled: Boolean = true,
    onHint: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    onNew: (() -> Unit)? = null,
    extras: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        onUndo?.let { TextButton(onClick = it, enabled = undoEnabled) { Text("Undo") } }
        onHint?.let { TextButton(onClick = it) { Text("Hint") } }
        onReset?.let { TextButton(onClick = it) { Text("Reset") } }
        onNew?.let { TextButton(onClick = it) { Text("New") } }
        extras?.invoke()
        if (status.isNotEmpty()) {
            Text(
                status,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
