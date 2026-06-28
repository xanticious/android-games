package com.xanticious.androidgames.games.scrabble

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.scrabble.*
import com.xanticious.androidgames.state.games.scrabblechallenge.ChallengePhase
import com.xanticious.androidgames.state.games.scrabblechallenge.ScrabbleChallengeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ScrabbleChallengeStateMachineTest {

    // A broad set of common short words so the bounded board/rack generator
    // reliably finds at least one legal play (and a non-empty top-moves list)
    // with the fixed test seed.
    private val testWordData = WordData(
        buildList {
            // Every two-letter combination that is a common Scrabble word, plus a
            // spread of short words, gives the generator plenty of legal plays.
            addAll(
                listOf(
                    "aa", "ab", "ad", "ae", "ag", "ai", "al", "am", "an", "ar", "as", "at", "aw", "ax", "ay",
                    "ba", "be", "bi", "bo", "by", "da", "de", "do", "ed", "ef", "eh", "el", "em", "en", "er",
                    "es", "et", "ex", "fa", "fe", "go", "ha", "he", "hi", "hm", "ho", "id", "if", "in", "is",
                    "it", "jo", "ka", "ki", "la", "li", "lo", "ma", "me", "mi", "mm", "mo", "mu", "my", "na",
                    "ne", "no", "nu", "od", "oe", "of", "oh", "oi", "om", "on", "op", "or", "os", "ow", "ox",
                    "oy", "pa", "pe", "pi", "qi", "re", "sh", "si", "so", "ta", "te", "ti", "to", "uh", "um",
                    "un", "up", "us", "ut", "we", "wo", "xi", "xu", "ya", "ye", "yo", "za",
                )
            )
            addAll(
                listOf(
                    "cat", "cats", "dog", "dogs", "cot", "cog", "the", "cart", "car", "bar", "bat", "rat",
                    "tar", "art", "ear", "eat", "tea", "ate", "are", "era", "sea", "set", "sit", "tin",
                    "ten", "net", "not", "ton", "one", "eon", "ore", "roe", "toe", "ant", "tan", "nat",
                )
            )
        }
    )

    private fun machine() = ScrabbleChallengeStateMachine(
        CoroutineScope(Dispatchers.Unconfined),
        Random(42)
    )

    @Test
    fun initialPhase_isSetup() {
        val m = machine()
        assertEquals(ChallengePhase.SETUP, m.phase.value)
    }

    @Test
    fun startSession_movesToPlaying() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        assertEquals(ChallengePhase.PLAYING, m.phase.value)
    }

    @Test
    fun startSession_generatesBoard() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        val state = m.state.value
        assertFalse(state.board.isEmpty())
        assertEquals(7, state.rack.size)
    }

    @Test
    fun startSession_generatesTopMoves() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        val state = m.state.value
        assertTrue(state.topMoves.isNotEmpty())
    }

    @Test
    fun startSession_startsAtRound1() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        val state = m.state.value
        assertEquals(1, state.roundNumber)
    }

    @Test
    fun placeTile_addsTentative() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        val tile = m.state.value.rack.first()
        m.userPlaceTile(Position(0, 0), tile)
        val state = m.state.value
        assertEquals(1, state.tentativeTiles.size)
        assertEquals(6, state.rack.size)
    }

    @Test
    fun recallTiles_returnsTentativeToRack() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        val tile = m.state.value.rack.first()
        m.userPlaceTile(Position(0, 0), tile)
        m.userRecallTiles()
        val state = m.state.value
        assertTrue(state.tentativeTiles.isEmpty())
        assertEquals(7, state.rack.size)
    }

    @Test
    fun skipRound_movesToRoundResult() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        m.userSkipRound()
        assertEquals(ChallengePhase.ROUND_RESULT, m.phase.value)
    }

    @Test
    fun skipRound_scoresZero() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        m.userSkipRound()
        val state = m.state.value
        assertEquals(0, state.roundScore)
    }

    @Test
    fun nextRound_incrementsRound() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)
        m.userSkipRound()
        m.userNextRound()
        val state = m.state.value
        assertEquals(2, state.roundNumber)
    }

    @Test
    fun round10_movesToSummary() {
        val m = machine()
        m.startSession(BoardDensity.MEDIUM, RackDifficulty.BALANCED, testWordData, 0)

        // Advance through rounds 1..9 (each round: skip to result, then next round).
        repeat(9) {
            m.userSkipRound()
            m.userNextRound()
        }

        // Round 10: skip to the round result, then view the session summary.
        m.userSkipRound()
        m.viewSummary()
        assertEquals(ChallengePhase.SESSION_SUMMARY, m.phase.value)
    }
}
