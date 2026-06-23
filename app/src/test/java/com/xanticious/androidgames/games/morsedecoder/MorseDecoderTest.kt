package com.xanticious.androidgames.games.morsedecoder

import com.xanticious.androidgames.controller.games.morsedecoder.OptionPicker
import com.xanticious.androidgames.controller.games.morsedecoder.ResultCalculator
import com.xanticious.androidgames.controller.games.morsedecoder.SentenceBank
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.morsedecoder.DecoderResult
import com.xanticious.androidgames.model.games.morsedecoder.LetterOutcome
import com.xanticious.androidgames.model.games.morsedecoder.decoderConfigFor
import com.xanticious.androidgames.state.games.morsedecoder.DecoderPhase
import com.xanticious.androidgames.state.games.morsedecoder.MorseDecoderStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// ═══════════════════════════════════════════════════════════════════════════
// OptionPicker tests
// ═══════════════════════════════════════════════════════════════════════════

class OptionPickerTest {

    @Test
    fun build_alwaysContainsAnswer() {
        val options = OptionPicker.build('E', seed = 42L, difficulty = GameDifficulty.MEDIUM)
        assertTrue("options must contain the answer", 'E' in options)
    }

    @Test
    fun build_alwaysSizeFive() {
        val options = OptionPicker.build('T', seed = 99L, difficulty = GameDifficulty.EASY)
        assertEquals("options must have exactly 5 elements", 5, options.size)
    }

    @Test
    fun build_noDuplicates() {
        val options = OptionPicker.build('A', seed = 1L, difficulty = GameDifficulty.HARD)
        assertEquals("options must have no duplicates", options.distinct().size, options.size)
    }

    @Test
    fun build_sameSeed_sameOptions_medium() {
        val first  = OptionPicker.build('M', seed = 7L, difficulty = GameDifficulty.MEDIUM)
        val second = OptionPicker.build('M', seed = 7L, difficulty = GameDifficulty.MEDIUM)
        assertEquals("same seed must produce same options", first, second)
    }

    @Test
    fun build_sameSeed_sameOptions_easy() {
        val first  = OptionPicker.build('S', seed = 55L, difficulty = GameDifficulty.EASY)
        val second = OptionPicker.build('S', seed = 55L, difficulty = GameDifficulty.EASY)
        assertEquals("same seed must produce same options (easy)", first, second)
    }

    @Test
    fun build_sameSeed_sameOptions_hard() {
        val first  = OptionPicker.build('H', seed = 100L, difficulty = GameDifficulty.HARD)
        val second = OptionPicker.build('H', seed = 100L, difficulty = GameDifficulty.HARD)
        assertEquals("same seed must produce same options (hard)", first, second)
    }

    @Test
    fun build_differentSeeds_typicallyDifferentOptions() {
        val first  = OptionPicker.build('X', seed = 1L, difficulty = GameDifficulty.MEDIUM)
        val second = OptionPicker.build('X', seed = 9999L, difficulty = GameDifficulty.MEDIUM)
        // With 25 possible distractors and only 4 chosen, collision is unlikely
        assertNotEquals("different seeds should typically produce different options", first, second)
    }

    @Test
    fun build_answerContained_forAllDifficulties() {
        GameDifficulty.entries.forEach { d ->
            val options = OptionPicker.build('Z', seed = 12345L, difficulty = d)
            assertTrue("options for difficulty $d must contain answer", 'Z' in options)
            assertEquals("options for difficulty $d must be size 5", 5, options.size)
            assertEquals("options for difficulty $d must have no duplicates", options.distinct().size, options.size)
        }
    }

    @Test
    fun build_easyDistractors_fourNonAnswerOptions() {
        val options = OptionPicker.build('T', seed = 42L, difficulty = GameDifficulty.EASY)
        val distractors = options.filter { it != 'T' }
        assertEquals("must have exactly 4 distractors", 4, distractors.size)
    }

    @Test
    fun build_hardDistractors_fourNonAnswerOptions() {
        val options = OptionPicker.build('E', seed = 42L, difficulty = GameDifficulty.HARD)
        val distractors = options.filter { it != 'E' }
        assertEquals("must have exactly 4 distractors", 4, distractors.size)
    }

    @Test
    fun morseDistance_identicalCode_isZero() {
        // E and E have the same code -> distance 0
        assertEquals(0, OptionPicker.morseDistance('E', 'E'))
    }

    @Test
    fun morseDistance_eAndT_areDifferentByLength() {
        // E = DIT (len 1), T = DAH (len 1) -> mismatch 1 + lenDiff 0 = 1
        assertEquals(1, OptionPicker.morseDistance('E', 'T'))
    }

    @Test
    fun morseDistance_symmetric() {
        val ab = OptionPicker.morseDistance('A', 'B')
        val ba = OptionPicker.morseDistance('B', 'A')
        assertEquals("distance must be symmetric", ab, ba)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SentenceBank tests
// ═══════════════════════════════════════════════════════════════════════════

class SentenceBankTest {

    @Test
    fun pick_deterministicBySeed_easy() {
        val first  = SentenceBank.pick(seed = 0L, difficulty = GameDifficulty.EASY)
        val second = SentenceBank.pick(seed = 0L, difficulty = GameDifficulty.EASY)
        assertEquals("same seed must produce same sentence (easy)", first, second)
    }

    @Test
    fun pick_deterministicBySeed_medium() {
        val first  = SentenceBank.pick(seed = 3L, difficulty = GameDifficulty.MEDIUM)
        val second = SentenceBank.pick(seed = 3L, difficulty = GameDifficulty.MEDIUM)
        assertEquals("same seed must produce same sentence (medium)", first, second)
    }

    @Test
    fun pick_deterministicBySeed_hard() {
        val first  = SentenceBank.pick(seed = 7L, difficulty = GameDifficulty.HARD)
        val second = SentenceBank.pick(seed = 7L, difficulty = GameDifficulty.HARD)
        assertEquals("same seed must produce same sentence (hard)", first, second)
    }

    @Test
    fun pick_negativeSeed_doesNotThrow() {
        val sentence = SentenceBank.pick(seed = -42L, difficulty = GameDifficulty.MEDIUM)
        assertTrue("sentence must not be blank", sentence.isNotBlank())
    }

    @Test
    fun pick_sentenceContainsOnlyLettersAndSpaces() {
        GameDifficulty.entries.forEach { d ->
            val s = SentenceBank.pick(seed = 1L, difficulty = d)
            assertTrue(
                "sentence for $d must contain only uppercase letters and spaces",
                s.all { it.isLetter() || it == ' ' }
            )
            assertTrue("sentence must be uppercase", s == s.uppercase())
        }
    }

    @Test
    fun pick_differentSeedsProduceDifferentSentences() {
        val s1 = SentenceBank.pick(seed = 0L, difficulty = GameDifficulty.MEDIUM)
        val s2 = SentenceBank.pick(seed = 5L, difficulty = GameDifficulty.MEDIUM)
        // With 15 sentences a difference of 5 in index guarantees different entry
        assertNotEquals("different seeds should produce different sentences", s1, s2)
    }

    @Test
    fun pick_easyBankNotEmpty() {
        val s = SentenceBank.pick(seed = 0L, difficulty = GameDifficulty.EASY)
        assertTrue(s.isNotEmpty())
    }

    @Test
    fun pick_hardBankNotEmpty() {
        val s = SentenceBank.pick(seed = 0L, difficulty = GameDifficulty.HARD)
        assertTrue(s.isNotEmpty())
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ResultCalculator tests
// ═══════════════════════════════════════════════════════════════════════════

class ResultCalculatorTest {

    private fun outcome(ch: Char, wrong: Int, ms: Long = 1000L) =
        LetterOutcome(answer = ch, wrongGuesses = wrong, timeMs = ms)

    @Test
    fun summarize_emptyOutcomes_returnsZeroAccuracy() {
        val r = ResultCalculator.summarize(emptyList(), seed = 1L)
        assertEquals(0f, r.accuracy, 0f)
    }

    @Test
    fun summarize_allFirstTry_accuracy100Percent() {
        val outcomes = listOf(outcome('A', 0), outcome('B', 0), outcome('C', 0))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(1f, r.accuracy, 0.001f)
    }

    @Test
    fun summarize_noneFirstTry_accuracyZero() {
        val outcomes = listOf(outcome('A', 1), outcome('B', 2), outcome('C', 3))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(0f, r.accuracy, 0.001f)
    }

    @Test
    fun summarize_halfFirstTry_accuracyHalf() {
        val outcomes = listOf(outcome('A', 0), outcome('B', 1), outcome('C', 0), outcome('D', 1))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(0.5f, r.accuracy, 0.001f)
    }

    @Test
    fun summarize_accuracyCounts_onlyFirstTryCorrect() {
        // 2 correct on first try out of 5 total = 0.4
        val outcomes = listOf(
            outcome('A', 0), outcome('B', 2), outcome('C', 0), outcome('D', 1), outcome('E', 3)
        )
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(2f / 5f, r.accuracy, 0.001f)
    }

    @Test
    fun summarize_totalMs_isSumOfLetterTimes() {
        val outcomes = listOf(outcome('A', 0, 100L), outcome('B', 1, 200L), outcome('C', 0, 300L))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(600L, r.totalMs)
    }

    @Test
    fun summarize_longestStreak_consecutiveFirstTry() {
        // streak: A(0) B(0) C(1) D(0) E(0) F(0) -> longest = 3
        val outcomes = listOf(
            outcome('A', 0), outcome('B', 0), outcome('C', 1),
            outcome('D', 0), outcome('E', 0), outcome('F', 0)
        )
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(3, r.longestStreak)
    }

    @Test
    fun summarize_longestStreak_allFirstTry() {
        val outcomes = listOf(outcome('A', 0), outcome('B', 0), outcome('C', 0))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(3, r.longestStreak)
    }

    @Test
    fun summarize_longestStreak_noneFirstTry_isZero() {
        val outcomes = listOf(outcome('A', 1), outcome('B', 2), outcome('C', 1))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals(0, r.longestStreak)
    }

    @Test
    fun summarize_hardestLetter_mostWrongGuesses() {
        val outcomes = listOf(outcome('A', 1), outcome('B', 3), outcome('C', 2))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals('B', r.hardestLetter)
    }

    @Test
    fun summarize_hardestLetter_tieBreakByFirstOccurrence() {
        // B and D both have 2 wrong guesses; B appears first
        val outcomes = listOf(outcome('A', 0), outcome('B', 2), outcome('C', 1), outcome('D', 2))
        val r = ResultCalculator.summarize(outcomes, seed = 0L)
        assertEquals('B', r.hardestLetter)
    }

    @Test
    fun summarize_seedEchoedToResult() {
        val outcomes = listOf(outcome('A', 0))
        val r = ResultCalculator.summarize(outcomes, seed = 999L)
        assertEquals(999L, r.seed)
    }

    @Test
    fun summarize_emptyOutcomes_hardestLetterIsPlaceholder() {
        val r = ResultCalculator.summarize(emptyList(), seed = 0L)
        assertEquals('?', r.hardestLetter)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DecoderConfig tests
// ═══════════════════════════════════════════════════════════════════════════

class DecoderConfigTest {

    @Test
    fun config_easyWpm_isSlowerThanMedium() {
        val easy   = decoderConfigFor(GameDifficulty.EASY)
        val medium = decoderConfigFor(GameDifficulty.MEDIUM)
        assertTrue("easy WPM must be lower than medium", easy.wpm < medium.wpm)
    }

    @Test
    fun config_mediumWpm_isSlowerThanHard() {
        val medium = decoderConfigFor(GameDifficulty.MEDIUM)
        val hard   = decoderConfigFor(GameDifficulty.HARD)
        assertTrue("medium WPM must be lower than hard", medium.wpm < hard.wpm)
    }

    @Test
    fun config_easyDitUnit_isLargerThanHard() {
        val easy = decoderConfigFor(GameDifficulty.EASY)
        val hard = decoderConfigFor(GameDifficulty.HARD)
        assertTrue("easy dit unit must be larger (slower) than hard", easy.ditUnitMs > hard.ditUnitMs)
    }

    @Test
    fun config_ditUnitMs_matchesParisFormula() {
        val config = decoderConfigFor(GameDifficulty.MEDIUM)
        assertEquals(1200 / config.wpm, config.ditUnitMs)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MorseDecoderStateMachine tests
// ═══════════════════════════════════════════════════════════════════════════

class MorseDecoderStateMachineTest {

    private fun machine() = MorseDecoderStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(DecoderPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_transitionsToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(DecoderPhase.SETUP, m.phase.value)
    }

    @Test
    fun openHowToPlay_fromSetup_transitionsToHowToPlay() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        assertEquals(DecoderPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun backToSetup_fromHowToPlay_transitionsToSetup() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        m.backToSetup()
        assertEquals(DecoderPhase.SETUP, m.phase.value)
    }

    @Test
    fun sentenceLoaded_fromSetup_transitionsToListening() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        assertEquals(DecoderPhase.LISTENING, m.phase.value)
    }

    @Test
    fun guessedWrong_staysInListening() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        m.guessedWrong()
        assertEquals(DecoderPhase.LISTENING, m.phase.value)
    }

    @Test
    fun guessedCorrect_staysInListening() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        m.guessedCorrect()
        assertEquals(DecoderPhase.LISTENING, m.phase.value)
    }

    @Test
    fun beepsFinished_staysInListening() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        m.beepsFinished()
        assertEquals(DecoderPhase.LISTENING, m.phase.value)
    }

    @Test
    fun sentenceCompleted_fromListening_transitionsToResults() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        m.sentenceCompleted()
        assertEquals(DecoderPhase.RESULTS, m.phase.value)
    }

    @Test
    fun replay_fromResults_transitionsToSetup() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        m.sentenceCompleted()
        m.replay()
        assertEquals(DecoderPhase.SETUP, m.phase.value)
    }

    @Test
    fun menu_fromResults_transitionsToIdle() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        m.sentenceCompleted()
        m.menu()
        assertEquals(DecoderPhase.IDLE, m.phase.value)
    }

    @Test
    fun fullFlow_setupToResults_reachesResults() {
        val m = machine()
        m.startGame()
        m.openHowToPlay()
        m.backToSetup()
        m.sentenceLoaded()
        m.guessedWrong()
        m.guessedCorrect()
        m.beepsFinished()
        m.sentenceCompleted()
        assertEquals(DecoderPhase.RESULTS, m.phase.value)
    }

    @Test
    fun replay_thenSentenceLoaded_returnsToListening() {
        val m = machine()
        m.startGame()
        m.sentenceLoaded()
        m.sentenceCompleted()
        m.replay()
        m.sentenceLoaded()
        assertEquals(DecoderPhase.LISTENING, m.phase.value)
    }
}
