package com.xanticious.androidgames.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Aqua4,
    onPrimary = Aqua0,
    secondary = Aqua3,
    onSecondary = Aqua0,
    tertiary = Aqua2,
    background = Aqua0,
    surface = Aqua0
)

private val DarkColors = darkColorScheme(
    primary = Aqua2,
    secondary = Aqua3,
    tertiary = Aqua1,
    background = Dark0,
    surface = Dark1
)

@Composable
fun AndroidGamesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
