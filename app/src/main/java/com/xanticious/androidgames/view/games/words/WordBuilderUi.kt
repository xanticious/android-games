package com.xanticious.androidgames.view.games.words

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark1

/**
 * Shared UI components for word-builder games (Anagrams, Anagrams Arcade, Boggle).
 * Per `design/common/word-builder-controls.md`.
 */

@Composable
fun LetterBank(
    letters: List<Char>,
    usedIndices: Set<Int>,
    onLetterTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightedIndices: Set<Int> = emptySet()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        letters.forEachIndexed { index, letter ->
            val isUsed = index in usedIndices
            val isHighlighted = index in highlightedIndices
            LetterTile(
                letter = letter,
                isUsed = isUsed,
                isHighlighted = isHighlighted,
                onClick = { if (!isUsed) onLetterTap(index) }
            )
        }
    }
}

@Composable
fun LetterTile(
    letter: Char,
    isUsed: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = if (isUsed) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = if (isHighlighted) Aqua3 else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
            .clickable(enabled = !isUsed, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isUsed) Dark1.copy(alpha = 0.3f) else Dark1
        )
    }
}

@Composable
fun CurrentEntry(
    entry: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (entry.isEmpty()) "—" else entry.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WordBuilderActions(
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSubmitAndKeep: () -> Unit,
    onGiveUp: () -> Unit,
    canBackspace: Boolean,
    canSubmit: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBackspace,
                enabled = canBackspace,
                modifier = Modifier.weight(1f)
            ) {
                Text("Backspace")
            }
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Submit")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSubmitAndKeep,
                enabled = canSubmit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Submit & Keep")
            }
            OutlinedButton(
                onClick = onGiveUp,
                modifier = Modifier.weight(1f)
            ) {
                Text("Give Up")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetWordsDisplay(
    targetWords: List<String>,
    foundWords: Set<String>,
    modifier: Modifier = Modifier,
    groupByLength: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (groupByLength) {
            val grouped = targetWords.groupBy { it.length }.toSortedMap()
            grouped.forEach { (length, words) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$length:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        words.sorted().forEach { word ->
                            WordBlank(word = word, found = word in foundWords)
                        }
                    }
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                targetWords.sorted().forEach { word ->
                    WordBlank(word = word, found = word in foundWords)
                }
            }
        }
    }
}

@Composable
fun WordBlank(
    word: String,
    found: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = if (found) word.uppercase() else "_".repeat(word.length),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (found) FontWeight.Bold else FontWeight.Normal,
        color = if (found) Aqua4 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoundWordsList(
    foundWords: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Found Words:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            foundWords.sorted().forEach { word ->
                Text(
                    text = word.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Aqua4,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProgressStrip(
    left: String,
    right: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = right,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CountdownTimer(
    timeRemaining: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatTime(timeRemaining),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = if (timeRemaining <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
