# Word Chain — Design Document

## Overview
- Word Chain is a single-player word game where each new word must **begin with the last
  letter** of the previous word.
- The player builds the longest chain possible without repeating a word or stalling; it
  plays best with a physical keyboard but supports the on-screen keyboard too.
- Every entry must be a valid **Wordnik** word; see `design/common/word-data-sources.md`.
- The run ends when the player can no longer add a valid word in time (or passes).
  Victory/defeat presentation follows `design/common/victory-defeat.md`.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- The growing chain is shown as connected pills; the required starting letter for the next
  word is emphasized in `Aqua3`. Accepted words land in `Aqua4`; rejects flash the
  Material 3 error role.
- No hex values outside `ui/theme/Color.kt`; transitions are instantaneous.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Word Chain      Chain 6   00:08 [⚙ ?] │  ← Top bar (optional per-word timer)
├─────────────────────────────────────┤
│  apple → eagle → echo → orbit →       │
│  tiger → rapids                        │  ← Chain so far (scrolls)
├─────────────────────────────────────┤
│  next word must start with:  S         │
│  input:  s▌                             │  ← Field (physical kb) or on-screen kb
│  Submit                                 │
└─────────────────────────────────────┘
```
- The chain is the anchor and scrolls as it grows; the required next letter is always
  visible above the input.

## Settings
- **Input mode**: Input field (physical keyboard) or On-screen keyboard (default Input
  field, since the game favors typing).
- **Per-word timer** (Off / 10s / 20s; default 20s): how long you have to add the next word;
  Off makes it a relaxed, untimed chain.
- **Link rule** (Last letter / Last two letters; default Last letter): how the next word
  must connect.
- **Minimum word length** (2–4; default 3).
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Start (or are given) a seed word. Each next word must **start with the last letter** of
  the previous word (or last two letters if that link rule is chosen).
- Type a valid Wordnik word that follows the link rule and has not been used yet, then
  **Submit**.
- Repeat to grow the chain. If the per-word timer expires or you cannot continue, the run
  ends.
- Score is the chain length (and total letters); aim to beat your best.

## Controls
- Type into the input field or tap the on-screen keyboard; Submit (or Enter) commits.
- Invalid, repeated, or wrong-starting words are rejected inline without ending the run
  (unless the timer runs out).
- Settings (⚙) and How to Play (?) from the top bar.

## Word Validation
- A pure controller checks each entry against the Wordnik-derived list, enforces the link
  rule and minimum length, and rejects already-used words (tracked in a set).

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Longest chain | yes |
| Most total letters in a chain | yes |
| Longest single word played | yes |
| Runs completed (lifetime) | yes |
- Score is primarily chain length; total letters is a tiebreaker.

## State Machine
- A dedicated `WordChainStateMachine` in `state/` exposes `StateFlow<WordChainState>`.
```
Idle
 └─ RunStarted → AwaitingWord
AwaitingWord
 ├─ WordSubmitted → AwaitingWord   (valid → extend chain; invalid → inline reject)
 ├─ TimeExpired → RunOver          (only when per-word timer is on)
 └─ Passed → RunOver
RunOver
 └─ NewRun / Replay / Menu → Idle
```
- The pure `WordChainRules` controller handles link-rule checking, validation against
  Wordnik, duplicate tracking, and scoring with no Android imports; unit tests cover the
  link rule, duplicate rejection, and scoring.

## HUD
- Top bar: game name, chain length, optional per-word countdown, settings, how-to-play.
- Per `hud-elements.md` for shared styling.
