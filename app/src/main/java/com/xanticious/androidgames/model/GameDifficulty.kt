package com.xanticious.androidgames.model

/**
 * Shared difficulty levels for all single-player / vs-AI games.
 *
 * Each game interprets these in its own [com.xanticious.androidgames.controller]
 * layer (AI reaction time, spawn rates, speeds, etc.).
 */
enum class GameDifficulty {
    EASY,
    MEDIUM,
    HARD;

    val label: String
        get() = when (this) {
            EASY -> "Easy"
            MEDIUM -> "Medium"
            HARD -> "Hard"
        }
}
