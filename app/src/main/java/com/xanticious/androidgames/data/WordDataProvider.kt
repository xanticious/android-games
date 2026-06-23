package com.xanticious.androidgames.data

import android.content.Context
import com.xanticious.androidgames.controller.words.WordData

/**
 * Android-side loader for the bundled word list. Reads
 * `assets/words/wordlist.txt` once, builds the pure [WordData] engine, and caches
 * it for the process lifetime so every word game shares one read-only instance.
 *
 * Loading is read-only and offline; building [WordData] is non-trivial for a
 * ~200k word list, so callers should invoke [get] off the main thread (e.g.
 * inside a `LaunchedEffect` on `Dispatchers.Default`).
 */
object WordDataProvider {

    private const val ASSET_PATH = "words/wordlist.txt"

    @Volatile
    private var cached: WordData? = null

    fun get(context: Context): WordData {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: load(context.applicationContext).also { cached = it }
        }
    }

    private fun load(context: Context): WordData {
        val words = context.assets.open(ASSET_PATH).bufferedReader().use { reader ->
            reader.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
        }
        return WordData(words)
    }
}
