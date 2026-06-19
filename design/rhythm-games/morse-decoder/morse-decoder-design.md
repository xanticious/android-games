# Morse Decoder — Design Document

## Overview
A listening rhythm game about **decoding** International Morse code. The game plays a single letter as Morse **beeps**; the player taps which letter they heard. Letters are revealed **one at a time** to spell out a target sentence. Because 26 letters + space is too busy to show at once, each letter is **multiple choice — 5 options** (the correct letter plus four random distractors). The current letter's beeps **repeat** until the player gets it; a wrong guess **replays** the letter and removes that option, so the player narrows down from the remaining choices.

All Morse alphabet, dit/dah timing, and beep-audio rules are shared in [../../common/morse-code.md](../../common/morse-code.md). This document covers only what is specific to Morse Decoder.

## Core Loop
1. A target **sentence** is chosen; its letters are revealed left-to-right, one active letter at a time (already-solved letters shown, the rest hidden/placeholder).
2. The active letter's Morse pattern **plays as beeps** (per common audio rules), then repeats on a loop with a gap.
3. The player is shown **5 choice buttons** (the answer + 4 random letters) and taps one.
   - **Correct** → the letter is revealed in the sentence, a confirm cue plays, and the game advances to the next letter (with a fresh set of 5 choices).
   - **Wrong** → an error sound plays, the **beeps replay**, the chosen wrong option is **disabled**, and the player guesses again from the remaining options.
4. Sentence fully revealed → **Results** (accuracy, time, etc.).

## Audio-First Presentation
- The **beep** is the primary information channel; visuals support it. Tone pitch is constant; durations/gaps carry the letter (see common doc).
- The active letter loops with a clear inter-repeat gap so the player can re-listen freely; there is no penalty for *listening*, only for *guessing wrong*.
- A **Replay** affordance lets the player trigger the beeps on demand in addition to the automatic loop.

## Multiple Choice (5 options)
- Each prompt shows exactly **5** options: the correct letter and **4 distractors** drawn at random from the remaining 25 letters (no duplicates).
- Distractor selection is a **pure controller** seeded by the run seed (so a run is reproducible and testable): given the answer and a seed, return 5 shuffled options containing the answer.
- On a wrong guess, that option is **greyed out/disabled** for the current letter; the correct answer always remains selectable. This guarantees the player can always finish a letter by elimination after re-listening.

## Spelling the Sentence
```
+-----------------------------------------------+
|   T H E   _ _ _ _   F O X                      |   solved letters shown; active = '_' highlighted
+-----------------------------------------------+
|        ♪ ▄ ▄▄▄ ▄  (now playing)   [Replay]     |   <- beeps loop for the active letter
+-----------------------------------------------+
|   [ Q ]   [ U ]   [ A ]   [ I ]   [ M ]        |   <- 5 choices (one correct)
+-----------------------------------------------+
|   Letter 4/11   Accuracy 86%   Time 0:31       |   <- HUD (below)
+-----------------------------------------------+
```
- **Spaces** between words are rendered directly (not quizzed) so the player only decodes letters.
- The active letter slot is highlighted; solved letters fill in. Colors reference `ui/theme/Color.kt` only.

## Scoring & Results
- Track per-letter **first-try correct**, total **wrong guesses**, and **time**.
- **Accuracy** = letters solved on the first guess ÷ total letters.
- Results panel (below the dimmed board, per [../../common/victory-defeat.md](../../common/victory-defeat.md)): accuracy %, total time, longest correct streak, the hardest letter (most wrong guesses), and the run **seed**, with a local best for the difficulty highlighted on a new record.

## Difficulty
A Settings difficulty scales the listening challenge via shared levers (see common doc) plus sentence choice:

| Difficulty | WPM (dit unit `U`) | Sentence | Notes |
|------------|--------------------|----------|-------|
| Easy   | slow (large `U`)   | short, common words | distractors biased toward dissimilar codes |
| Normal | medium             | medium sentences | random distractors |
| Hard   | fast (small `U`)   | longer sentences | distractors biased toward **similar** codes (e.g. E/I/S/H) |

- The main lever is **WPM → `U`** (faster beeps are harder to parse). Hard additionally makes distractors *confusable* (codes that differ by one element) to stress careful listening.

## Sentence Source
- Sentences come from a local bank (no network). A **seed** selects the sentence and drives distractor randomness; shown on Results for replay.
- Selection + option generation are **pure controllers** (`controller/`) seeded by a `Long`; no Android imports, fully unit-testable.

## Screen Flow
Standard per-game flow: **Settings → How to Play → Gameplay → Results**.
- **Settings**: difficulty/WPM and Random vs. saved seed.
- **How to Play**: explains that you listen to beeps and pick the letter, beeps repeat until solved, 5 choices, and a wrong guess removes that option and replays the letter.

## State Machine
`MorseDecoderState` (KStateMachine, state exposed via `StateFlow`):
```
Idle
 └─ SentenceLoaded → Listening
Listening
 ├─ BeepsFinished → Listening      (loop the active letter after a gap)
 ├─ GuessedCorrect → Listening     (reveal letter; advance; new 5 options)  [or → Results if last]
 └─ GuessedWrong → Listening       (error sound; disable option; replay beeps)
Results
 └─ (replay / menu) → Idle
```
- States: `Idle`, `Listening`, `Results`.
- Events: `SentenceLoaded`, `BeepsFinished`, `GuessedCorrect`, `GuessedWrong`, `SentenceCompleted`.

## Data Model (game-specific)
Builds on the shared `Symbol` / `MORSE` / `MorseTiming` types.
```
data class DecodePrompt(
    val answer: Char,
    val options: List<Char>,        // size 5, contains answer
    val disabled: Set<Char>         // wrong guesses removed this letter
)
data class LetterOutcome(val answer: Char, val wrongGuesses: Int, val timeMs: Long)
data class DecoderResult(
    val accuracy: Float,
    val totalMs: Long,
    val longestStreak: Int,
    val hardestLetter: Char,
    val seed: Long
)
// Controllers (pure):
//   OptionPicker.build(answer, seed) -> List<Char>          // 5 unique, shuffled, contains answer
//   Beeper.schedule(letter, timing) -> List<BeepEvent>      // dit/dah durations + gaps
//   ResultCalculator.summarize(outcomes) -> DecoderResult
```
Controllers are pure and unit-testable (e.g. *options always contain the answer and are size 5 with no duplicates*, *same seed ⇒ same options*, *accuracy counts only first-try-correct letters*).

## Out of Scope (v1)
- Decoding whole words/letters by ear without multiple choice (free text).
- Digits and punctuation (letters + spaces only).
- Adjustable tone pitch / Farnsworth spacing options (fixed per common doc).
- Online stats or leaderboards.
