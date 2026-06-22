package com.xanticious.androidgames.controller.games.morsedecoder

import com.xanticious.androidgames.model.GameDifficulty

/**
 * Provides target sentences for Morse Decoder runs.
 *
 * All sentences use only uppercase letters and spaces; digits and punctuation
 * are excluded per the v1 scope.  Spaces are rendered directly in the UI and
 * are not quizzed — only letter positions are decoded.
 *
 * Selection is deterministic: the same [seed] always yields the same sentence
 * for a given [difficulty], making runs fully reproducible.
 */
object SentenceBank {

    private val easySentences: List<String> = listOf(
        "THE FOX",
        "BIG CAT",
        "RED SKY",
        "OLD MAP",
        "HOT SUN",
        "WET DOG",
        "FLY HIGH",
        "NICE DAY",
        "COLD AIR",
        "JUMP NOW",
        "BLUE JAY",
        "DARK OAK",
        "FAST CAR",
        "SOFT RAIN",
        "WIDE SEA"
    )

    private val mediumSentences: List<String> = listOf(
        "QUICK BROWN FOX",
        "STARS AND MOON",
        "BRAVE NEW WORLD",
        "SOFT SUMMER RAIN",
        "THE OPEN ROAD",
        "WAVES ON SHORE",
        "DEEP BLUE SEA",
        "WILD RIVER RUNS",
        "CLOUD NINE DREAM",
        "BRIGHT CLEAR SKY",
        "SAIL THE OCEAN",
        "WARM SPRING BREEZE",
        "HIGH MOUNTAIN PEAK",
        "LONG SILENT NIGHT",
        "CALM STILL WATERS"
    )

    private val hardSentences: List<String> = listOf(
        "THE QUICK BROWN FOX JUMPS",
        "SOFT WINDS BLOW OVER GREEN HILLS",
        "STARLIGHT FADES AT DAWN",
        "RIVERS FLOW TO THE SEA",
        "EVERY WORD HAS MEANING",
        "BRAVE HEARTS NEVER FALTER",
        "LIGHT TRAVELS VERY FAST",
        "SIGNALS CROSS THE WIDE OCEAN",
        "MORSE CODE LINKS THE WORLD",
        "LISTEN WELL AND DECODE RIGHT",
        "PATIENCE SHARPENS THE MIND",
        "EACH LETTER TELLS A STORY",
        "STARS GUIDE THE NIGHT SAILOR",
        "STILL WATERS RUN VERY DEEP",
        "KNOW THE CODE KNOW THE WAY"
    )

    /**
     * Pick a sentence from the bank deterministically by [seed] and [difficulty].
     * The index is computed as `(seed mod bankSize)` using a positive-safe modulo
     * so negative seeds are handled correctly.
     */
    fun pick(seed: Long, difficulty: GameDifficulty): String {
        val bank = when (difficulty) {
            GameDifficulty.EASY   -> easySentences
            GameDifficulty.MEDIUM -> mediumSentences
            GameDifficulty.HARD   -> hardSentences
        }
        val index = ((seed % bank.size) + bank.size).toInt() % bank.size
        return bank[index]
    }
}
