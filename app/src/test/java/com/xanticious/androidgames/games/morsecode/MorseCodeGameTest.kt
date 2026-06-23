package com.xanticious.androidgames.games.morsecode

import com.xanticious.androidgames.controller.games.morsecode.HardestWords
import com.xanticious.androidgames.controller.games.morsecode.MorseKeyer
import com.xanticious.androidgames.controller.games.morsecode.PhraseBank
import com.xanticious.androidgames.controller.games.morsecode.StatsCalculator
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.morse.MorseTiming
import com.xanticious.androidgames.model.games.morsecode.KeyStroke
import com.xanticious.androidgames.model.games.morsecode.LetterAttempt
import com.xanticious.androidgames.model.games.morsecode.WordAttempt
import com.xanticious.androidgames.state.games.morsecode.MorseCodePhase
import com.xanticious.androidgames.state.games.morsecode.MorseCodeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Morse Code game.
 * Naming convention: <subject>_<condition>_<expectation>
 */
class MorseCodeGameTest {

    // ── Timing at 20 WPM: U = 60 ms ─────────────────────────────────────────
    // dit press  < 2U = 120 ms  → use 50 ms
    // dah press  ≥ 2U = 120 ms  → use 200 ms
    // element gap < 2U          → use 50 ms  (same letter)
    // letter gap  2U..5U        → use 150 ms (letter boundary)
    // word gap    ≥ 5U = 300 ms → use 400 ms (word boundary)
    private val t20 = MorseTiming(60)

    // ── MorseKeyer.decode ────────────────────────────────────────────────────

    @Test
    fun decode_singleDit_decodesE() {
        val result = MorseKeyer.decode(listOf(KeyStroke(50, 0)), t20)
        assertEquals(listOf('E'), result)
    }

    @Test
    fun decode_singleDah_decodesT() {
        val result = MorseKeyer.decode(listOf(KeyStroke(200, 0)), t20)
        assertEquals(listOf('T'), result)
    }

    @Test
    fun decode_ditThenDahSameLetter_decodesA() {
        // dit (50ms), element-gap (50ms), then dah (200ms) — last gapMs=0 → commit at end
        val result = MorseKeyer.decode(listOf(KeyStroke(50, 50), KeyStroke(200, 0)), t20)
        assertEquals(listOf('A'), result)
    }

    @Test
    fun decode_sosPattern_decodesCorrectly() {
        // S = dit dit dit | O = dah dah dah | S = dit dit dit
        val s1 = listOf(KeyStroke(50, 50), KeyStroke(50, 50), KeyStroke(50, 150)) // S + letter gap
        val o  = listOf(KeyStroke(200, 50), KeyStroke(200, 50), KeyStroke(200, 150)) // O + letter gap
        val s2 = listOf(KeyStroke(50, 50), KeyStroke(50, 50), KeyStroke(50, 0))   // S, no trailing gap
        val result = MorseKeyer.decode(s1 + o + s2, t20)
        assertEquals(listOf('S', 'O', 'S'), result)
    }

    @Test
    fun decode_wordBoundaryGap_commitsLetterAndContinues() {
        // E (dit), then word-gap (400 ms), then T (dah)
        val result = MorseKeyer.decode(listOf(KeyStroke(50, 400), KeyStroke(200, 0)), t20)
        assertEquals(listOf('E', 'T'), result)
    }

    @Test
    fun decode_pressExactly2U_classifiedAsDah() {
        // pressMs == 120 == 2U → NOT a dit (condition is < 2U for dit)
        val result = MorseKeyer.decode(listOf(KeyStroke(120, 0)), t20)
        assertEquals(listOf('T'), result)  // T = DAH
    }

    @Test
    fun decode_pressJustBelow2U_classifiedAsDit() {
        val result = MorseKeyer.decode(listOf(KeyStroke(119, 0)), t20)
        assertEquals(listOf('E'), result)  // E = DIT
    }

    @Test
    fun decode_unknownSymbolPattern_producesNoLetter() {
        // Five dahs = not a valid Morse letter; result should be empty
        val five = List(5) { KeyStroke(200, if (it < 4) 50 else 0) }
        val result = MorseKeyer.decode(five, t20)
        assertTrue(result.isEmpty())
    }

    @Test
    fun decode_emptyStrokes_returnsEmptyList() {
        assertTrue(MorseKeyer.decode(emptyList(), t20).isEmpty())
    }

    @Test
    fun decode_multipleWords_decodesSequentially() {
        // H = dit dit dit dit, I = dit dit
        val h = listOf(
            KeyStroke(50, 50), KeyStroke(50, 50), KeyStroke(50, 50), KeyStroke(50, 150)
        )
        val i = listOf(KeyStroke(50, 50), KeyStroke(50, 0))
        val result = MorseKeyer.decode(h + i, t20)
        assertEquals(listOf('H', 'I'), result)
    }

    // ── HardestWords.pick ────────────────────────────────────────────────────

    @Test
    fun hardestWords_returnsExactlyNWords() {
        val words = (1..10).map { i -> WordAttempt("W$i", i * 100L, i, emptyList()) }
        assertEquals(5, HardestWords.pick(words, 5).size)
    }

    @Test
    fun hardestWords_highRetryWord_ranksFirst() {
        val easy = WordAttempt("EASY", 500L, 0, emptyList())
        val hard = WordAttempt("HARD", 500L, 5, emptyList())
        assertEquals("HARD", HardestWords.pick(listOf(easy, hard), 1).first())
    }

    @Test
    fun hardestWords_fewerWordsThanN_returnsAll() {
        val words = listOf(WordAttempt("ONE", 1000L, 0, emptyList()))
        assertEquals(1, HardestWords.pick(words, 5).size)
    }

    @Test
    fun hardestWords_sameRetries_longerTimeRanksFirst() {
        val faster = WordAttempt("AA", 500L, 1, emptyList())
        val slower = WordAttempt("BB", 900L, 1, emptyList())
        assertEquals("BB", HardestWords.pick(listOf(faster, slower), 1).first())
    }

    // ── PhraseBank.pick ──────────────────────────────────────────────────────

    @Test
    fun phraseBank_sameSeedAndDifficulty_returnsSamePhrase() {
        val p1 = PhraseBank.pick(42L, GameDifficulty.EASY)
        val p2 = PhraseBank.pick(42L, GameDifficulty.EASY)
        assertEquals(p1, p2)
    }

    @Test
    fun phraseBank_differentSeeds_returnDifferentPhrases() {
        // With 30 easy words there are ≥ 900 3-word combinations; collision probability ≈ 0
        val p1 = PhraseBank.pick(1L, GameDifficulty.EASY)
        val p2 = PhraseBank.pick(99_999L, GameDifficulty.EASY)
        assertNotEquals(p1, p2)
    }

    @Test
    fun phraseBank_easyDifficulty_phraseHasThreeWords() {
        assertEquals(3, PhraseBank.pick(0L, GameDifficulty.EASY).words.size)
    }

    @Test
    fun phraseBank_mediumDifficulty_phraseHasFourWords() {
        assertEquals(4, PhraseBank.pick(0L, GameDifficulty.MEDIUM).words.size)
    }

    @Test
    fun phraseBank_hardDifficulty_phraseHasFiveWords() {
        assertEquals(5, PhraseBank.pick(0L, GameDifficulty.HARD).words.size)
    }

    @Test
    fun phraseBank_allWordsAreUppercaseLettersOnly() {
        val phrase = PhraseBank.pick(7L, GameDifficulty.MEDIUM)
        phrase.words.forEach { word ->
            assertTrue("'$word' must be uppercase letters only", word.all { it.isLetter() && it.isUpperCase() })
        }
    }

    // ── StatsCalculator.summarize ────────────────────────────────────────────

    @Test
    fun statsCalculator_wpm_positiveForNonEmptyWords() {
        val words = listOf(WordAttempt("HI", 5_000L, 0, listOf(
            LetterAttempt('H', 2_000L, 0),
            LetterAttempt('I', 3_000L, 0)
        )))
        assertTrue(StatsCalculator.summarize(words).wpm > 0f)
    }

    @Test
    fun statsCalculator_wpm_zeroForEmptyInput() {
        assertEquals(0f, StatsCalculator.summarize(emptyList()).wpm, 0.001f)
    }

    @Test
    fun statsCalculator_distinctLetters_countsUniqueLetters() {
        val words = listOf(
            WordAttempt("AB", 1_000L, 0, listOf(LetterAttempt('A', 500L, 0), LetterAttempt('B', 500L, 0))),
            WordAttempt("AC", 1_000L, 0, listOf(LetterAttempt('A', 500L, 0), LetterAttempt('C', 500L, 0)))
        )
        assertEquals(3, StatsCalculator.summarize(words).distinctLetters) // A, B, C
    }

    @Test
    fun statsCalculator_hardestLetter_hasMostMistakes() {
        val words = listOf(WordAttempt("AB", 2_000L, 0, listOf(
            LetterAttempt('A', 500L, 0),
            LetterAttempt('B', 1_500L, 3)
        )))
        assertEquals('B', StatsCalculator.summarize(words).hardestLetter)
    }

    @Test
    fun statsCalculator_easiestLetter_hasFewestMistakes() {
        val words = listOf(WordAttempt("AB", 2_000L, 0, listOf(
            LetterAttempt('A', 500L, 0),
            LetterAttempt('B', 1_500L, 3)
        )))
        assertEquals('A', StatsCalculator.summarize(words).easiestLetter)
    }

    @Test
    fun statsCalculator_fastestWord_hasMinTotalMs() {
        val words = listOf(
            WordAttempt("FAST", 1_000L, 0, emptyList()),
            WordAttempt("SLOW", 5_000L, 0, emptyList())
        )
        assertEquals("FAST", StatsCalculator.summarize(words).fastestWord.first)
    }

    @Test
    fun statsCalculator_slowestWord_hasMaxTotalMs() {
        val words = listOf(
            WordAttempt("FAST", 1_000L, 0, emptyList()),
            WordAttempt("SLOW", 5_000L, 0, emptyList())
        )
        assertEquals("SLOW", StatsCalculator.summarize(words).slowestWord.first)
    }

    @Test
    fun statsCalculator_wordWithRetries_slowerThanWordWithout() {
        val noRetry = WordAttempt("CAT", 1_000L, 0, emptyList())
        val withRetry = WordAttempt("CAT", 4_000L, 3, emptyList())
        assertTrue(withRetry.totalMs > noRetry.totalMs)
    }

    // ── MorseCodeStateMachine ────────────────────────────────────────────────

    private fun machine() = MorseCodeStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun stateMachine_initialPhase_isIdle() {
        assertEquals(MorseCodePhase.IDLE, machine().phase.value)
    }

    @Test
    fun stateMachine_gameStarted_transitionsToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(MorseCodePhase.SETUP, m.phase.value)
    }

    @Test
    fun stateMachine_configConfirmed_transitionsToHowToPlay() {
        val m = machine()
        m.startGame(); m.confirmConfig()
        assertEquals(MorseCodePhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun stateMachine_openHowToPlay_fromSetup_transitionsToHowToPlay() {
        val m = machine()
        m.startGame(); m.openHowToPlay()
        assertEquals(MorseCodePhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun stateMachine_backToSetup_fromHowToPlay_transitionsToSetup() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.backToSetup()
        assertEquals(MorseCodePhase.SETUP, m.phase.value)
    }

    @Test
    fun stateMachine_phraseLoaded_transitionsToKeying() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded()
        assertEquals(MorseCodePhase.KEYING, m.phase.value)
    }

    @Test
    fun stateMachine_letterCommitted_remainsKeying() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded(); m.letterCommitted()
        assertEquals(MorseCodePhase.KEYING, m.phase.value)
    }

    @Test
    fun stateMachine_wordCompleted_remainsKeying() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded(); m.wordCompleted()
        assertEquals(MorseCodePhase.KEYING, m.phase.value)
    }

    @Test
    fun stateMachine_mistakeMade_transitionsToPenalty() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded(); m.mistakeMade()
        assertEquals(MorseCodePhase.PENALTY, m.phase.value)
    }

    @Test
    fun stateMachine_penaltyElapsed_transitionsToKeying() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded(); m.mistakeMade(); m.penaltyElapsed()
        assertEquals(MorseCodePhase.KEYING, m.phase.value)
    }

    @Test
    fun stateMachine_phraseCompleted_transitionsToStats() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded(); m.phraseCompleted()
        assertEquals(MorseCodePhase.STATS, m.phase.value)
    }

    @Test
    fun stateMachine_retrainHardest_transitionsToKeying() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded(); m.phraseCompleted(); m.retrainHardest()
        assertEquals(MorseCodePhase.KEYING, m.phase.value)
    }

    @Test
    fun stateMachine_returnToIdle_transitionsToIdle() {
        val m = machine()
        m.startGame(); m.confirmConfig(); m.phraseLoaded(); m.phraseCompleted(); m.returnToIdle()
        assertEquals(MorseCodePhase.IDLE, m.phase.value)
    }
}
