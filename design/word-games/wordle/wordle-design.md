# Wordle — Design Document

## Overview
- Wordle is a single-player word-deduction game: guess a hidden five-letter target word in a
  limited number of tries, with color clues after each guess (green = right letter in the
  right place, yellow = right letter in the wrong place, gray = not in the word).
- The player can play **as many rounds as they want**, back to back.
- Target words are drawn from the **Wordnik** list; guesses are validated against the
  Wordnik-derived allowed-word set. See `design/common/word-data-sources.md`.
- Two rules make this variant distinctive:
  - **Hard-mode by default:** every guess must be **consistent with all clues revealed so
    far** (greens fixed in place, yellows reused, grays excluded).
  - **Carried-over first guess:** the **first guess of each round is automatically the
    previous round's target word** (or a random valid word if no round has been played yet).
- A **Help** button plays a **random valid word** that satisfies the current clues.
- After the game ends, the player can review **all words** (their guesses and the target)
  and tap any of them to see its **Wiktionary definition**. Victory/defeat presentation
  follows `design/common/victory-defeat.md`.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- The guess grid uses tokens from `ui/theme/Color.kt` for the three clue states (a "correct"
  accent, a "present" accent, and an "absent"/muted role) — no raw hex; these map to the
  green/yellow/gray semantics while staying on-palette.
- The on-screen keyboard tints used keys with their best-known clue state.
- Transitions are instantaneous; full-screen celebration is reserved for a win and never
  covers the grid.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Wordle              Round 3    [⚙ ?]  │  ← Top bar
├─────────────────────────────────────┤
│   [C][R][A][N][E]   ← prev target as  │
│   [S][T][O][R][Y]      first guess    │  ← Guess grid (clue colors)
│   [ ][ ][ ][ ][ ]                      │
│   …                                   │
├─────────────────────────────────────┤
│  Q W E R T Y U I O P                   │
│   A S D F G H J K L                    │  ← On-screen keyboard (clue tints)
│   Help  Z X C V B N M  ⌫  Enter        │  ← Help button plays a valid word
└─────────────────────────────────────┘
```
- The guess grid is the anchor; keyboard and Help below it. Status text never overlays the
  grid.

## Settings
- **Number of guesses** (5 / 6 / 7; default 6).
- **Enforce clue-consistency** (on/off, default **on**): when on, guesses must match all
  prior clues (the signature rule). Off relaxes to standard Wordle for casual play.
- **Carry first guess** (on/off, default on): seed each round's first guess with the prior
  target.
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Guess the hidden five-letter word. After each guess, tiles color in: correct (right spot),
  present (wrong spot), absent.
- Your **first guess each round is pre-filled** with the previous round's target word (or a
  random word the first time you play); submit it or edit it.
- With clue-consistency on, each new guess **must be compatible with every clue so far** —
  reuse greens in place, include yellows, and avoid grays; inconsistent guesses are rejected
  inline.
- Tap **Help** to auto-play a random valid word that fits the current clues.
- Win by guessing the target; otherwise the round ends when guesses run out. Then start
  another round whenever you like.

## Controls
- Type with the on-screen keyboard; ⌫ deletes, **Enter** submits the current row.
- **Help** fills and submits a random clue-consistent valid word.
- After the round, tap any word in the review list to open its definition.
- Settings (⚙) and How to Play (?) from the top bar.

## Clue-Consistency Enforcement
- A pure controller derives the constraint set from all prior guesses' clues and validates
  each new guess against it (plus membership in the allowed-word set). Rejected guesses are
  not consumed.
- The **Help** action enumerates valid words satisfying the current constraints and returns
  a random one.

## Definitions
- The target word and every guess are looked up in the bundled Wiktionary data; tapping a
  word in the post-game review shows its definition, with a graceful fallback when none
  exists.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Rounds won / played | yes |
| Guess distribution (wins by guess count) | yes |
| Current and best win streak | yes |
| Help uses (lifetime) | yes |
| Previous round's target (for the carried first guess) | yes |
- The previous target is persisted locally so the next round can seed its first guess.

## State Machine
- A dedicated `WordleStateMachine` in `state/` exposes `StateFlow<WordleState>`.
```
Idle
 └─ RoundStarted → Guessing        (first guess pre-seeded from previous target)
Guessing
 ├─ GuessSubmitted → Guessing      (valid+consistent → clue; else inline reject)
 ├─ HelpRequested → Guessing       (auto-plays a valid clue-consistent word)
 ├─ TargetGuessed → Won
 └─ GuessesExhausted → Lost
Won
 └─ NextRound / Review / Menu → Idle   (target stored for next round's first guess)
Lost
 └─ NextRound / Review / Menu → Idle
```
- The pure `WordleRules` controller handles clue computation, constraint derivation and
  enforcement, allowed-word validation, Help word selection, target selection, and
  carried-first-guess seeding with no Android imports; unit tests cover clue colors,
  clue-consistency rejection, Help validity, and the carried first guess.

## Victory / Defeat & Review
- Per `design/common/victory-defeat.md`: the grid stays visible; the summary below shows the
  result and a **review list of all words** (guesses + target), each tappable for its
  **Wiktionary definition**. Offers Next Round, Review, and Menu.
