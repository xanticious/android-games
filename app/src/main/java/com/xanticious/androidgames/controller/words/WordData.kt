package com.xanticious.androidgames.controller.words

import kotlin.random.Random

/**
 * Shared, pure word-data engine for every word game (see
 * `design/common/word-data-sources.md`).
 *
 * Built once from the bundled, already-normalized word list (lowercase, A–Z
 * only, offensive words removed) and exposes fast read-only lookups. Contains no
 * Android imports so it can be unit-tested on the JVM; the Android asset loading
 * lives in `data/WordDataProvider`.
 *
 * The input list is treated as the source of truth for validity. All public
 * lookups lowercase their argument so callers need not pre-normalize.
 */
class WordData(words: Collection<String>) {

    /** Sorted, de-duplicated word array — enables binary-search prefix queries. */
    private val sorted: List<String> = words.asSequence()
        .map { it.lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()
        .toList()

    private val all: HashSet<String> = HashSet(sorted)

    private val byLength: Map<Int, List<String>> = sorted.groupBy { it.length }

    /** Total number of words available. */
    val size: Int get() = sorted.size

    /** True when [word] is in the list (case-insensitive). */
    fun isValidWord(word: String): Boolean = all.contains(word.lowercase())

    /** Words of exactly [length] letters, in alphabetical order (never null). */
    fun wordsOfLength(length: Int): List<String> = byLength[length] ?: emptyList()

    /**
     * True when at least one word starts with [prefix]. Uses binary search over
     * the sorted list so callers (e.g. Boggle path search) can prune dead ends.
     */
    fun isValidPrefix(prefix: String): Boolean {
        if (prefix.isEmpty()) return sorted.isNotEmpty()
        val p = prefix.lowercase()
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] < p) lo = mid + 1 else hi = mid
        }
        return lo < sorted.size && sorted[lo].startsWith(p)
    }

    /** A random word with length in [minLength]..[maxLength] (inclusive). */
    fun randomWord(
        minLength: Int = 1,
        maxLength: Int = Int.MAX_VALUE,
        random: Random = Random.Default,
    ): String? {
        val pool = (minLength..minOf(maxLength, longestWordLength())).flatMap { wordsOfLength(it) }
        return if (pool.isEmpty()) null else pool[random.nextInt(pool.size)]
    }

    /** A random word of exactly [length] letters, or null if none exist. */
    fun randomWordOfLength(length: Int, random: Random = Random.Default): String? {
        val pool = wordsOfLength(length)
        return if (pool.isEmpty()) null else pool[random.nextInt(pool.size)]
    }

    /**
     * Every valid word (length >= [minLength]) that can be spelled using a subset
     * of [letters], respecting letter multiplicity. Used by the word-builder
     * games (Anagrams, Boggle results) to compute the full solution set.
     */
    fun anagramSolutions(letters: String, minLength: Int = 3): List<String> {
        val available = letterCounts(letters.lowercase())
        val maxLen = letters.count { it.isLetter() }
        if (maxLen < minLength) return emptyList()
        val result = ArrayList<String>()
        for (len in minLength..maxLen) {
            for (word in wordsOfLength(len)) {
                if (canForm(word, available)) result.add(word)
            }
        }
        return result
    }

    private fun longestWordLength(): Int = byLength.keys.maxOrNull() ?: 0

    private fun letterCounts(s: String): IntArray {
        val counts = IntArray(26)
        for (c in s) if (c in 'a'..'z') counts[c - 'a']++
        return counts
    }

    private fun canForm(word: String, available: IntArray): Boolean {
        val need = IntArray(26)
        for (c in word) {
            if (c !in 'a'..'z') return false
            val i = c - 'a'
            if (++need[i] > available[i]) return false
        }
        return true
    }

    /**
     * Short definition for [word] from the bundled definition data, or null when
     * none is available. Definitions are not bundled in this pass, so callers
     * should fall back to [DEFINITION_UNAVAILABLE].
     */
    fun definitionOf(word: String): String? = null

    companion object {
        const val DEFINITION_UNAVAILABLE = "No definition available."
    }
}
