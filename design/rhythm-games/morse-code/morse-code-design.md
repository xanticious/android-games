# Morse Code — Design Document

## Overview
A single-button rhythm game about **sending** International Morse code. The player is given a short phrase and keys it out **one word at a time** with a single tap button: short presses are **dits**, long presses are **dahs**. The currently active word is highlighted; finishing it advances to the next. Mistakes cost time — a buzz and a brief delay — and you restart the current word. When the phrase is done, a **stats** panel breaks down your performance (WPM, letters practiced, easiest/hardest letter, fastest/slowest word), and you can jump into **re-training your hardest five words**.

All Morse alphabet, dit/dah timing, single-button input classification, and audio rules are shared in [../../common/morse-code.md](../../common/morse-code.md). This document covers only what is specific to Morse Code.

## Core Loop
1. A phrase is shown as a row of words; the **first word is highlighted** (active), the rest dimmed.
2. The player keys the active word's letters with the single button (press = dit/dah by hold length; gaps separate letters — see common doc).
3. As each letter is correctly completed it locks in beneath/within the active word.
4. **Correct word** → advance highlight to the next word.
5. **Mistake** → error sound + short delay, the active word **resets**, and the player retries it from the start.
6. Phrase complete → **Stats** screen → optionally **Re-train hardest 5**.

## Input
- One large tap button (≥ 48dp). Hold length classifies dit vs. dah; release length classifies element / letter boundaries — all per [../../common/morse-code.md](../../common/morse-code.md).
- The decoder controller turns the press/gap stream into letters; a letter is checked against the expected letter of the active word as soon as a letter boundary is detected.
- A **mistake** = a decoded letter that does not match the expected letter at that position (wrong symbols, or a dit/dah misjudged by the player). On a mistake the whole **current word** restarts (not the whole phrase).

## Training Mode (Setting)
- **Off (default-able per taste):** only the English word is shown; the player must recall the code.
- **On:** under each letter of the active word, the shared **code strip** renders its dit/dah pattern (e.g. `S = ...`), and the English letters stay visible — a guided, learn-the-code experience.
- Training Mode is a Settings toggle and changes presentation only; timing/scoring rules are identical.

## Highlighting & Layout
```
+-----------------------------------------------+
|   HELLO     WORLD     FROM     MORSE           |   words; active = highlighted, done = dim
|   ^^^^^                                        |
|   . / .-.. / .-.. / ---   (code strip if Training)|
+-----------------------------------------------+
|        progress within word:  H E _ _ _        |
+-----------------------------------------------+
|              (   TAP / KEY BUTTON   )          |   <- single button
+-----------------------------------------------+
|   Word 1/4     Elapsed 0:08     WPM 6.2        |   <- HUD (below)
+-----------------------------------------------+
```
- Completed words dim; the active word is highlighted; upcoming words use default text color. Colors reference `ui/theme/Color.kt` only.
- The active word shows per-letter progress so the player sees how far through the word they are.

## Mistakes & Feedback
- On a wrong letter: play the shared **error sound**, freeze input for a short **penalty delay**, then clear the active word's progress so it can be retyped from its first letter.
- Correct letters give a soft confirming tick; correct word gives a brief success cue and advances the highlight.
- Per the project rules, no failure overlay covers the board — feedback is inline.

## Stats (end of phrase)
Computed by a pure controller from the per-letter/per-word timing log:

| Stat | Definition |
|------|------------|
| **Words per minute** | Standard PARIS-based WPM over the whole phrase (correct keying time). |
| **Letters typed** | Count of **distinct** letters practiced across the phrase. |
| **Easiest letter** | Distinct letter with the best average keying time / fewest mistakes. |
| **Hardest letter** | Distinct letter with the worst average keying time / most mistakes. |
| **Fastest word** | Word with the shortest successful completion time (and that time). |
| **Slowest word** | Word with the longest completion time, retries included (and that time). |

- Timing for a word counts from first key to the word being accepted, including retry delays (so error-prone words read as "slow").
- The Stats panel appears **below** the (now-dim) play area, per [../../common/victory-defeat.md](../../common/victory-defeat.md).

### Re-train Hardest 5
- From Stats, **"Re-train the hardest 5 words"** starts a focused round containing the five slowest/most-errored words from the just-finished phrase.
- It records new times for those words so the player can see improvement (best time per word is tracked locally and highlighted on a new record).

## Difficulty / WPM
- The main lever is **target WPM**, which sets the dit unit `U` and therefore the dit/dah and gap thresholds (slower = larger, more forgiving margins — see common doc).
- A Settings difficulty picks a WPM band (e.g. Easy/Slow, Normal, Hard/Fast) and the **phrase length/vocabulary** (shorter, common words → longer, mixed words).

## Phrase Source
- Phrases come from a local word/phrase bank (no network). A **seed** selects the phrase set so a session can be replayed; the seed is shown on the Stats screen.
- Generation/selection is a **pure controller** (`controller/`) seeded by a `Long`; no Android imports, fully unit-testable.

## Screen Flow
Standard per-game flow: **Settings → How to Play → Gameplay → Stats**.
- **Settings**: Training Mode toggle, difficulty/WPM, and Random vs. saved seed.
- **How to Play**: explains dit/dah by hold length, letter/word gaps, the active-word highlight, the mistake-resets-the-word rule, and Training Mode.

## State Machine
`MorseCodeState` (KStateMachine, state exposed via `StateFlow`):
```
Idle
 └─ PhraseLoaded → Keying
Keying
 ├─ LetterCommitted → Keying      (correct letter locks in; word progresses)
 ├─ WordCompleted → Keying        (advance highlight to next word)
 ├─ MistakeMade → Penalty         (error sound + reset active word)
 └─ PhraseCompleted → Stats
Penalty
 └─ PenaltyElapsed → Keying       (resume on the reset word)
Stats
 ├─ RetrainHardest → Keying       (focused 5-word round)
 └─ (menu) → Idle
```
- States: `Idle`, `Keying`, `Penalty`, `Stats`.
- Events: `PhraseLoaded`, `LetterCommitted`, `WordCompleted`, `MistakeMade`, `PenaltyElapsed`, `PhraseCompleted`, `RetrainHardest`.

## Data Model (game-specific)
Builds on the shared `Symbol` / `MORSE` / `MorseTiming` types.
```
data class Phrase(val words: List<String>)
data class KeyStroke(val pressMs: Long, val gapMs: Long)   // raw single-button input
data class LetterAttempt(val expected: Char, val timeMs: Long, val mistakes: Int)
data class WordAttempt(val word: String, val totalMs: Long, val retries: Int, val letters: List<LetterAttempt>)
data class PhraseStats(
    val wpm: Float,
    val distinctLetters: Int,
    val easiestLetter: Char,
    val hardestLetter: Char,
    val fastestWord: Pair<String, Long>,
    val slowestWord: Pair<String, Long>
)
// Controllers (pure):
//   MorseDecoder.decode(strokes, timing) -> List<Char>
//   StatsCalculator.summarize(words: List<WordAttempt>) -> PhraseStats
//   HardestWords.pick(words, n = 5) -> List<String>
```
Controllers are pure and unit-testable (e.g. *known stroke stream decodes to the expected letters*, *a word with retries is slower than one without*, *re-train returns exactly the 5 worst words*).

## Out of Scope (v1)
- Digits and punctuation (letters + word space only).
- Iambic/paddle keying (single straight-key button only).
- Sending vs. another player; online stats.
- Saving full phrase history beyond per-word best times.
